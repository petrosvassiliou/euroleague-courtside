package com.euroleague.courtside

import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.data.repository.PlayerBoxscoreStat
import com.euroleague.courtside.i18n.AppLanguage
import com.euroleague.courtside.i18n.CourtsideTranslations
import com.euroleague.courtside.util.CalendarExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests für:
 * 1. Offizielle EuroLeague PIR-Formel (Performance Index Rating)
 * 2. RFC 5545 Standard-Konformität des `.ics`-Saison-Exporters (`CalendarExportHelper`)
 * 3. Crunch-Time Close-Game-Alert-Erkennung (Q4, < 3:00 Restzeit, <= 5 Punkte Differenz)
 * 4. Mehrsprachige Lokalisierung in allen 6 Sprachen (EN, DE, FR, IT, ES, EL)
 */
class EuroLeagueCourtsideUnitTest {

    private val repository = EuroLeagueMockRepository()

    @Test
    fun `official EuroLeague PIR calculation matches formula`() {
        // Carsen Edwards Beispiel: 26 PTS, 4 REB (1 Off + 3 Def), 5 AST, 2 STL, 0 BLK, 2 TO,
        // 2P: 5/9 (4 missed), 3P: 4/9 (5 missed), FT: 4/4 (0 missed), 5 Fouls Drawn, 2 Fouls Committed
        val stat = PlayerBoxscoreStat(
            playerId = "p5",
            playerName = "Carsen Edwards",
            jerseyNumber = 3,
            teamId = "BAY",
            minutes = "31:42",
            points = 26,
            offRebounds = 1,
            defRebounds = 3,
            assists = 5,
            steals = 2,
            blocks = 0,
            turnovers = 2,
            twoPtMade = 5,
            twoPtAttempted = 9,
            threePtMade = 4,
            threePtAttempted = 9,
            ftMade = 4,
            ftAttempted = 4,
            foulsDrawn = 5,
            foulsCommitted = 2
        )

        // Positive: 26 + 4 + 5 + 2 + 0 + 5 = 42
        // Negative: (4 + 5) missed FG + 0 missed FT + 2 TO + 2 Fouls Committed = 13
        // PIR = 42 - 13 = 29
        assertEquals(29, stat.pir)
        assertEquals(4, stat.totalRebounds)
    }

    @Test
    fun `RFC 5545 ics generator creates valid VCALENDAR with VEVENT and 30min VALARM`() {
        val bayern = repository.teams.first { it.id == "BAY" }
        val games = repository.games.value.filter {
            it.homeTeam.id == "BAY" || it.awayTeam.id == "BAY"
        }

        val icsPayload = CalendarExportHelper.buildRfc5545IcsPayload(bayern, games)

        assertTrue(icsPayload.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(icsPayload.contains("VERSION:2.0\r\n"))
        assertTrue(icsPayload.contains("BEGIN:VEVENT\r\n"))
        assertTrue(icsPayload.contains("LOCATION:SAP Garden\\, Munich\r\n"))
        assertTrue(icsPayload.contains("BEGIN:VALARM\r\nTRIGGER:-PT30M\r\nACTION:DISPLAY\r\n"))
        assertTrue(icsPayload.endsWith("END:VCALENDAR\r\n"))
    }

    @Test
    fun `close game alert triggers only in Q4 under 3 minutes with diff at most 5`() {
        val liveCrunchGame = repository.games.value.first { it.id == "g_live_1" }
        // Q4, 01:42, 84:82 -> diff 2 -> true
        assertTrue(liveCrunchGame.isCloseGameFourthQuarter)

        // Blowout-Spiel (12 Punkte Differenz) darf keinen Close-Game-Alert auslösen
        val blowoutGame = liveCrunchGame.copy(homeScore = 94, awayScore = 82)
        assertFalse(blowoutGame.isCloseGameFourthQuarter)

        // Beendetes Spiel darf keinen Live-Close-Game-Alert auslösen
        val finishedGame = liveCrunchGame.copy(status = GameStatus.FINAL)
        assertFalse(finishedGame.isCloseGameFourthQuarter)
    }

    @Test
    fun `live club roster verification and official crest logo URLs update reactive state`() {
        val bayern = repository.findTeam("BAY")
        assertTrue(bayern != null)
        assertTrue(bayern!!.logoUrl.startsWith("https://"))
        assertEquals("club_logo_mun", bayern.localLogoResName)

        val updatedRoster = repository.players
            .filter { it.teamId == "BAY" }
            .map { it.copy(injuryDetail = "Verified Active Roster") }

        repository.updateClubRoster("BAY", updatedRoster)
        val syncedBayernPlayers = repository.playersFlow.value.filter { it.teamId == "BAY" }
        assertTrue(syncedBayernPlayers.isNotEmpty())
        assertTrue(syncedBayernPlayers.all { it.injuryDetail == "Verified Active Roster" })
    }

    @Test
    fun `all 6 supported languages EN DE FR IT ES EL provide complete translations and update state`() {
        assertEquals(6, AppLanguage.entries.size)
        AppLanguage.entries.forEach { lang ->
            val strings = CourtsideTranslations.forLanguage(lang)
            assertTrue(strings.navMyTeam.isNotBlank())
            assertTrue(strings.navStandings.isNotBlank())
            assertTrue(strings.navSettings.isNotBlank())
            assertTrue(strings.playerBoxscoreHeader.isNotBlank())
            assertTrue(strings.myTeamHubTitle.isNotBlank())

            repository.selectLanguage(lang)
            assertEquals(lang, repository.appLanguageFlow.value)
        }
    }

    @Test
    fun `playoff bracket projection computes real regular season H2H records`() {
        val bracket = repository.getPlayoffBracket()
        assertTrue(bracket.isNotEmpty())
        assertTrue(bracket.all { it.homeWins in 0..2 && it.awayWins in 0..2 })
    }

    @Test
    fun `notifications format in all 6 selected languages instead of hardcoded German`() {
        val enTipOff = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.formatTipOffAlarm(
            matchup = "Panathinaikos AKTOR Athens vs. Olympiacos Piraeus",
            arena = "OAKA Altion",
            round = 30,
            language = AppLanguage.EN
        )
        val elTipOff = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.formatTipOffAlarm(
            matchup = "Panathinaikos AKTOR Athens vs. Olympiacos Piraeus",
            arena = "OAKA Altion",
            round = 30,
            language = AppLanguage.EL
        )
        assertTrue(enTipOff.title.contains("Minutes"))
        assertFalse(enTipOff.title.contains("Minuten"))
        assertTrue(elTipOff.title.contains("Τζάμπολ"))

        AppLanguage.entries.forEach { lang ->
            val payload = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.formatTipOffAlarm(
                matchup = "FC Bayern Munich vs. Real Madrid",
                arena = "SAP Garden",
                round = 12,
                language = lang
            )
            assertTrue(payload.title.isNotBlank())
            assertTrue(payload.bigText.isNotBlank())
        }
    }

    @Test
    fun `saved My Team selection restores cleanly across app restarts`() {
        val freshRepo = EuroLeagueMockRepository()
        freshRepo.restoreSavedTeamSelection("OLY", setOf("OLY", "PAN"))
        assertEquals("OLY", freshRepo.selectedTeamId.value)
        assertTrue(freshRepo.favoriteTeamIds.value.contains("OLY"))
        assertEquals("Olympiacos Piraeus", freshRepo.findTeam(freshRepo.selectedTeamId.value)?.name)
    }

    @Test
    fun `app always starts on My Team destination on initial launch and every restart`() {
        assertEquals(
            com.euroleague.courtside.ui.navigation.CourtsideDestination.MY_TEAM,
            com.euroleague.courtside.ui.navigation.CourtsideDestination.START_DESTINATION
        )

        val freshRepo = EuroLeagueMockRepository()
        assertEquals(
            com.euroleague.courtside.ui.navigation.CourtsideDestination.MY_TEAM,
            freshRepo.activeDestinationFlow.value
        )

        freshRepo.selectDestination(com.euroleague.courtside.ui.navigation.CourtsideDestination.MATCHDAY)
        assertEquals(
            com.euroleague.courtside.ui.navigation.CourtsideDestination.MATCHDAY,
            freshRepo.activeDestinationFlow.value
        )
        freshRepo.selectDestination(com.euroleague.courtside.ui.navigation.CourtsideDestination.SETTINGS)
        assertEquals(
            com.euroleague.courtside.ui.navigation.CourtsideDestination.SETTINGS,
            freshRepo.activeDestinationFlow.value
        )

        freshRepo.resetToStartDestination()
        assertEquals(
            com.euroleague.courtside.ui.navigation.CourtsideDestination.MY_TEAM,
            freshRepo.activeDestinationFlow.value
        )
    }

    @Test
    fun `applyLiveDataset preserves real FINAL scores without fabricating fake LIVE games and transitions expired Q4 games to FINAL`() {
        val freshRepo = EuroLeagueMockRepository()
        val oly = freshRepo.findTeam("OLY") ?: freshRepo.teams.first()
        val dub = freshRepo.findTeam("DUB") ?: freshRepo.teams[1]
        val pan = freshRepo.findTeam("PAN") ?: freshRepo.teams[2]
        val bar = freshRepo.findTeam("BAR") ?: freshRepo.teams[3]
        val template = freshRepo.games.value.first()

        // 1. Real finished Round 11 game (OLY 101 : 72 DUB) and a live game at 00:15 in Q4 (PAN 74 : 76 BAR)
        val finishedOlyDub = template.copy(
            id = "E2026_107",
            gameCode = 107,
            round = 11,
            homeTeam = oly,
            awayTeam = dub,
            homeScore = 101,
            awayScore = 72,
            status = GameStatus.FINAL,
            currentQuarter = 4,
            remainingClock = "00:00",
            quarters = com.euroleague.courtside.data.repository.QuarterScores(
                q1Home = 25, q1Away = 18,
                q2Home = 32, q2Away = 25,
                q3Home = 31, q3Away = 13,
                q4Home = 13, q4Away = 16
            )
        )
        val expiringLiveGame = template.copy(
            id = "E2026_110",
            gameCode = 110,
            round = 11,
            homeTeam = pan,
            awayTeam = bar,
            homeScore = 74,
            awayScore = 76,
            status = GameStatus.LIVE,
            currentQuarter = 4,
            remainingClock = "00:15",
            quarters = com.euroleague.courtside.data.repository.QuarterScores(
                q1Home = 17, q1Away = 17,
                q2Home = 16, q2Away = 15,
                q3Home = 24, q3Away = 11,
                q4Home = 17, q4Away = 33
            )
        )

        freshRepo.applyLiveDataset(
            liveTeams = freshRepo.teams,
            livePlayers = freshRepo.players,
            liveGames = listOf(finishedOlyDub, expiringLiveGame),
            activeRound = 11,
            seasonCode = "E2026",
            syncLabel = "E2026 • Round 11"
        )

        // Finished game must stay FINAL with exact score 101 : 72 (never converted to fake LIVE!)
        val olyAfterApply = freshRepo.games.value.first { it.id == "E2026_107" }
        assertEquals(GameStatus.FINAL, olyAfterApply.status)
        assertEquals(101, olyAfterApply.homeScore)
        assertEquals(72, olyAfterApply.awayScore)

        // When the 00:15 Q4 live game expires (clock reaches 00:00), it transitions to FINAL and displays the final score
        freshRepo.simulateLiveRefresh()
        val endedGame = freshRepo.games.value.first { it.id == "E2026_110" }
        assertEquals(GameStatus.FINAL, endedGame.status)
        assertEquals("00:00", endedGame.remainingClock)
        assertEquals(
            endedGame.homeScore,
            endedGame.quarters.q1Home + endedGame.quarters.q2Home +
                endedGame.quarters.q3Home + endedGame.quarters.q4Home +
                (endedGame.quarters.otHome ?: 0)
        )

        // When all games in the dataset are FINAL, zero games are marked LIVE
        freshRepo.applyLiveDataset(
            liveTeams = freshRepo.teams,
            livePlayers = freshRepo.players,
            liveGames = listOf(finishedOlyDub),
            activeRound = 11,
            seasonCode = "E2026",
            syncLabel = "E2026 • Round 11"
        )
        assertTrue(freshRepo.games.value.none { it.status == GameStatus.LIVE })
        assertEquals(GameStatus.FINAL, freshRepo.games.value.first().status)

        AppLanguage.entries.forEach { lang ->
            val strings = CourtsideTranslations.forLanguage(lang)
            assertTrue(strings.liveScoresHeader.isNotBlank())
            assertTrue(strings.filterLiveOnly.isNotBlank())
            assertTrue(strings.liveTickButton.isNotBlank())
            assertTrue(strings.crunchTimeBadge.isNotBlank())
            assertTrue(strings.statusFinal.isNotBlank())
        }
    }

    @Test
    fun `My Team resolves Round 11 instead of Round 38 even when API returns games in descending round order`() {
        val freshRepo = EuroLeagueMockRepository()
        assertEquals(11, freshRepo.activeRoundFlow.value)

        val oly = freshRepo.findTeam("OLY") ?: freshRepo.teams.first()
        val valencia = freshRepo.findTeam("PAM") ?: freshRepo.teams.last()
        val dubai = freshRepo.findTeam("DUB") ?: freshRepo.teams[1]
        val template = freshRepo.games.value.first()

        // Simulate API response arriving in descending round order (Round 38 first, Round 11 middle, Round 1 last)
        val descendingGames = listOf(
            template.copy(
                id = "E2026_380",
                gameCode = 380,
                round = 38,
                homeTeam = oly,
                awayTeam = valencia,
                status = GameStatus.UPCOMING
            ),
            template.copy(
                id = "E2026_107",
                gameCode = 107,
                round = 11,
                homeTeam = oly,
                awayTeam = dubai,
                status = GameStatus.UPCOMING
            ),
            template.copy(
                id = "E2026_1",
                gameCode = 1,
                round = 1,
                homeTeam = valencia,
                awayTeam = oly,
                status = GameStatus.FINAL
            )
        )

        freshRepo.applyLiveDataset(
            liveTeams = freshRepo.teams,
            livePlayers = freshRepo.players,
            liveGames = descendingGames,
            activeRound = 11,
            seasonCode = "E2026",
            syncLabel = "E2026 • Round 11"
        )

        val storedRounds = freshRepo.games.value.map { it.round }
        assertEquals(listOf(1, 11, 38), storedRounds)

        val olyGames = freshRepo.games.value.filter {
            it.homeTeam.id == oly.id || it.awayTeam.id == oly.id
        }
        val myTeamActiveGame = olyGames.firstOrNull { it.round == freshRepo.activeRoundFlow.value }
            ?: olyGames.firstOrNull { it.status == GameStatus.UPCOMING || it.status == GameStatus.LIVE }
        assertTrue(myTeamActiveGame != null)
        assertEquals(11, myTeamActiveGame?.round)
    }

    @Test
    fun `Google Play Protect compliant AppUpdateManager compares versions and evaluates GitHub release payloads`() {
        val manager = com.euroleague.courtside.update.CourtsideAppUpdateManager
        assertTrue(manager.compareSemanticVersions("v1.5.0", "1.4.2") > 0)
        assertEquals(0, manager.compareSemanticVersions("v1.5.0", "1.5.0"))
        assertTrue(manager.compareSemanticVersions("1.4.1", "v1.5.0") < 0)
        assertEquals(12, manager.extractBuildNumber("EuroLeague Courtside v1.5.0 (Build 12 — Official Signed Release APK)"))

        val sampleReleaseJson = """
            {
              "tag_name": "v1.6.0",
              "name": "EuroLeague Courtside v1.6.0 (Build 13 — Official Signed Release APK)",
              "html_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/tag/v1.6.0",
              "body": "Official Play Protect compliant update",
              "draft": false,
              "prerelease": false,
              "assets": [
                {
                  "name": "EuroLeague-Courtside-v1.6.0-debug.apk",
                  "browser_download_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/download/v1.6.0/EuroLeague-Courtside-v1.6.0-debug.apk",
                  "size": 23425630
                },
                {
                  "name": "EuroLeague-Courtside-v1.6.0.apk",
                  "browser_download_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/download/v1.6.0/EuroLeague-Courtside-v1.6.0.apk",
                  "size": 5488857,
                  "digest": "sha256:bce303826eeffaf855d57c63205577115b6cc3e129638acc0a4af916ef46c151"
                }
              ]
            }
        """.trimIndent()

        val updateAvailableInfo = manager.evaluateReleasePayload(
            rawJson = sampleReleaseJson,
            currentVersionName = "1.5.0",
            currentVersionCode = 12,
            checkedAtTime = "19:30:00",
            isManual = true
        )
        assertEquals(com.euroleague.courtside.update.UpdateStatus.UPDATE_AVAILABLE, updateAvailableInfo.status)
        assertEquals("1.6.0", updateAvailableInfo.latestVersionName)
        assertEquals(13, updateAvailableInfo.latestVersionCode)
        assertTrue(updateAvailableInfo.apkDownloadUrl.endsWith("EuroLeague-Courtside-v1.6.0.apk"))
        assertEquals("bce303826eeffaf855d57c63205577115b6cc3e129638acc0a4af916ef46c151", updateAvailableInfo.sha256Digest)

        val upToDateInfo = manager.evaluateReleasePayload(
            rawJson = sampleReleaseJson,
            currentVersionName = "1.6.0",
            currentVersionCode = 13,
            checkedAtTime = "19:30:00",
            isManual = true
        )
        assertEquals(com.euroleague.courtside.update.UpdateStatus.UP_TO_DATE, upToDateInfo.status)

        AppLanguage.entries.forEach { lang ->
            val strings = CourtsideTranslations.forLanguage(lang)
            assertTrue(strings.updateSectionTitle.isNotBlank())
            assertTrue(strings.playProtectBadge.isNotBlank())
            assertTrue(strings.checkForUpdatesButton.isNotBlank())
            assertTrue(strings.downloadInstallUpdateButton.isNotBlank())
        }
    }
}

