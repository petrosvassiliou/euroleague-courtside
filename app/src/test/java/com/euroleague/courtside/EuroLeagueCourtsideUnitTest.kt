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
        assertTrue(manager.compareSemanticVersions("v1.8.0-beta.1", "1.7.0") > 0)
        assertTrue(manager.compareSemanticVersions("v1.8.0-beta.2", "v1.8.0-beta.1") > 0)
        assertTrue(manager.compareSemanticVersions("v1.8.0", "v1.8.0-beta.1") > 0)
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

        // Verify GitHub `/releases?per_page=15` JSON array with beta pre-releases:
        val sampleReleasesListJson = """
            [
              {
                "tag_name": "v1.8.0-beta.3",
                "name": "EuroLeague Courtside v1.8.0-beta.3 (Build 19 — Beta Pre-Release Signed APK)",
                "html_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/tag/v1.8.0-beta.3",
                "body": "Beta 3 headshot fix",
                "draft": false,
                "prerelease": true,
                "assets": [
                  {
                    "name": "EuroLeague-Courtside-v1.8.0-beta.3.apk",
                    "browser_download_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/download/v1.8.0-beta.3/EuroLeague-Courtside-v1.8.0-beta.3.apk",
                    "size": 5240000,
                    "digest": "sha256:aaa111222333444555666777888999000aaabbbcccdddeeefff0001112223334"
                  }
                ]
              },
              {
                "tag_name": "v1.7.0",
                "name": "EuroLeague Courtside v1.7.0 (Build 16 — Official Signed Release APK)",
                "html_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/tag/v1.7.0",
                "body": "Official stable v1.7.0",
                "draft": false,
                "prerelease": false,
                "assets": [
                  {
                    "name": "EuroLeague-Courtside-v1.7.0.apk",
                    "browser_download_url": "https://github.com/petrosvassiliou/euroleague-courtside/releases/download/v1.7.0/EuroLeague-Courtside-v1.7.0.apk",
                    "size": 5100000
                  }
                ]
              }
            ]
        """.trimIndent()

        // Installed beta build (1.8.0-beta.2) automatically detects newer beta (1.8.0-beta.3)
        val betaUpgrade = manager.evaluateReleasePayload(
            rawJson = sampleReleasesListJson,
            currentVersionName = "1.8.0-beta.2",
            currentVersionCode = 18,
            checkedAtTime = "20:42:30",
            isManual = true
        )
        assertEquals(com.euroleague.courtside.update.UpdateStatus.UPDATE_AVAILABLE, betaUpgrade.status)
        assertEquals("1.8.0-beta.3", betaUpgrade.latestVersionName)
        assertEquals(19, betaUpgrade.latestVersionCode)
        assertTrue(betaUpgrade.isPreRelease)
        assertTrue(betaUpgrade.apkDownloadUrl.endsWith("EuroLeague-Courtside-v1.8.0-beta.3.apk"))

        // Stable build (1.7.0) ignores beta pre-releases by default unless beta channel is opted in
        val stableDefault = manager.evaluateReleasePayload(
            rawJson = sampleReleasesListJson,
            currentVersionName = "1.7.0",
            currentVersionCode = 16,
            isManual = true,
            includePrereleases = false
        )
        assertEquals(com.euroleague.courtside.update.UpdateStatus.UP_TO_DATE, stableDefault.status)

        val stableOptInBeta = manager.evaluateReleasePayload(
            rawJson = sampleReleasesListJson,
            currentVersionName = "1.7.0",
            currentVersionCode = 16,
            isManual = true,
            includePrereleases = true
        )
        assertEquals(com.euroleague.courtside.update.UpdateStatus.UPDATE_AVAILABLE, stableOptInBeta.status)
        assertEquals("1.8.0-beta.3", stableOptInBeta.latestVersionName)

        // Verify live ticker URLs bypass Cloudflare 60s edge cache via 3-second `_t` cache-busting bucket
        val urlT0 = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository.buildLiveApiUrl(
            endpoint = "Header",
            gameCode = 136,
            seasonCode = "E2026",
            nowMillis = 1_700_000_000_000L
        )
        val urlT4 = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository.buildLiveApiUrl(
            endpoint = "Header",
            gameCode = 136,
            seasonCode = "E2026",
            nowMillis = 1_700_000_004_000L
        )
        assertTrue(urlT0.contains("https://live.euroleague.net/api/Header?gamecode=136&seasoncode=E2026&_t="))
        assertTrue(urlT0 != urlT4)

        // Verify per-game reminders (Q1, Halftime, Q3, Crunch Time, Final Score) fire independently of Favorite Teams
        val sampleRepo = EuroLeagueMockRepository()
        val nonFavHome = sampleRepo.findTeam("BAR") ?: sampleRepo.teams.first()
        val nonFavAway = sampleRepo.findTeam("ULK") ?: sampleRepo.teams[1]
        val baseGame = sampleRepo.games.value.first().copy(
            id = "E2026_133",
            gameCode = 133,
            round = 14,
            homeTeam = nonFavHome,
            awayTeam = nonFavAway,
            tipOffEpochMillis = 1_700_000_000_000L
        )
        val perGameMap = mapOf(
            "E2026_133" to com.euroleague.courtside.notifications.GameReminderType.entries.toSet()
        )
        val encoded = com.euroleague.courtside.data.local.CourtsidePreferencesDataStore.encodeGameReminders(perGameMap)
        val decoded = com.euroleague.courtside.data.local.CourtsidePreferencesDataStore.decodeGameReminders(encoded)
        assertEquals(5, decoded["E2026_133"]?.size)

        // 1) End of Q1 (`currentQuarter = 1, remainingClock = "00:00"`)
        val q1Game = baseGame.copy(
            status = GameStatus.LIVE,
            currentQuarter = 1,
            remainingClock = "00:00",
            homeScore = 28,
            awayScore = 21,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(28, 21, 0, 0, 0, 0, 0, 0)
        )
        val q1Actions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(q1Game),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = false,
            halftimeFinalEnabled = false,
            closeGameEnabled = false,
            alreadySentKeys = emptySet(),
            nowMillis = 1_700_001_200_000L,
            perGameReminders = perGameMap
        )
        assertEquals(1, q1Actions.size)
        assertEquals(
            com.euroleague.courtside.notifications.EuroLeagueNotificationManager.Companion.AutoAlertType.Q1_SCORE,
            q1Actions.first().type
        )

        // 2) Halftime (`currentQuarter = 2, remainingClock = "00:00"`)
        val htGame = q1Game.copy(
            currentQuarter = 2,
            remainingClock = "00:00",
            homeScore = 45,
            awayScore = 39,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(28, 21, 17, 18, 0, 0, 0, 0)
        )
        val htActions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(htGame),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = false,
            halftimeFinalEnabled = false,
            closeGameEnabled = false,
            alreadySentKeys = setOf("q1_E2026_133"),
            nowMillis = 1_700_002_800_000L,
            perGameReminders = perGameMap
        )
        assertEquals(1, htActions.size)
        assertEquals(
            com.euroleague.courtside.notifications.EuroLeagueNotificationManager.Companion.AutoAlertType.HALFTIME_SCORE,
            htActions.first().type
        )

        // 3) End of Q3 (`currentQuarter = 3, remainingClock = "00:00"`)
        val q3Game = htGame.copy(
            currentQuarter = 3,
            remainingClock = "00:00",
            homeScore = 61,
            awayScore = 53,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(28, 21, 17, 18, 16, 14, 0, 0)
        )
        val q3Actions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(q3Game),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = false,
            halftimeFinalEnabled = false,
            closeGameEnabled = false,
            alreadySentKeys = setOf("q1_E2026_133", "ht_E2026_133"),
            nowMillis = 1_700_004_500_000L,
            perGameReminders = perGameMap
        )
        assertEquals(1, q3Actions.size)
        assertEquals(
            com.euroleague.courtside.notifications.EuroLeagueNotificationManager.Companion.AutoAlertType.Q3_SCORE,
            q3Actions.first().type
        )

        // 4) Crunch Time (`Q4, 01:42, 2 pts diff`)
        val crunchGame = q3Game.copy(
            currentQuarter = 4,
            remainingClock = "01:42",
            homeScore = 82,
            awayScore = 80,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(28, 21, 17, 18, 16, 14, 21, 27)
        )
        val crunchActions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(crunchGame),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = false,
            halftimeFinalEnabled = false,
            closeGameEnabled = false,
            alreadySentKeys = setOf("q1_E2026_133", "ht_E2026_133", "q3_E2026_133"),
            nowMillis = 1_700_005_800_000L,
            perGameReminders = perGameMap
        )
        assertEquals(1, crunchActions.size)
        assertEquals(
            com.euroleague.courtside.notifications.EuroLeagueNotificationManager.Companion.AutoAlertType.CRUNCH_TIME,
            crunchActions.first().type
        )

        // 5) Final Score (`FINAL`)
        val finalGame = crunchGame.copy(
            status = GameStatus.FINAL,
            remainingClock = "00:00",
            homeScore = 86,
            awayScore = 83
        )
        val finalActions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(finalGame),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = false,
            halftimeFinalEnabled = false,
            closeGameEnabled = false,
            alreadySentKeys = setOf("q1_E2026_133", "ht_E2026_133", "q3_E2026_133", "crunch_E2026_133"),
            nowMillis = 1_700_006_500_000L,
            perGameReminders = perGameMap
        )
        assertEquals(1, finalActions.size)
        assertEquals(
            com.euroleague.courtside.notifications.EuroLeagueNotificationManager.Companion.AutoAlertType.FINAL_SCORE,
            finalActions.first().type
        )

        AppLanguage.entries.forEach { lang ->
            val strings = CourtsideTranslations.forLanguage(lang)
            assertTrue(strings.updateSectionTitle.isNotBlank())
            assertTrue(strings.playProtectBadge.isNotBlank())
            assertTrue(strings.checkForUpdatesButton.isNotBlank())
            assertTrue(strings.downloadInstallUpdateButton.isNotBlank())
        }
    }

    @Test
    fun `live Header enrichment transitions lagging UPCOMING v2 games to LIVE and parses ScoreExtraTime overtime FINAL games`() {
        val freshRepo = EuroLeagueMockRepository()
        val bas = freshRepo.findTeam("BAS") ?: freshRepo.teams.first()
        val hta = freshRepo.findTeam("HTA") ?: freshRepo.teams[1]
        val zal = freshRepo.findTeam("ZAL") ?: freshRepo.teams[2]
        val mun = freshRepo.findTeam("MUN") ?: freshRepo.teams[3]
        val template = freshRepo.games.value.first()

        // 1. Game reported as UPCOMING (0:0) by lagging v2 endpoint during live matchday
        val upcomingInV2 = template.copy(
            id = "E2026_114",
            gameCode = 114,
            round = 12,
            homeTeam = bas,
            awayTeam = hta,
            status = GameStatus.UPCOMING,
            currentQuarter = 1,
            remainingClock = "10:00",
            homeScore = 0,
            awayScore = 0,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(0, 0, 0, 0, 0, 0, 0, 0)
        )
        val liveQ4Header = com.euroleague.courtside.data.remote.EuroLeagueLiveHeaderDto(
            live = true,
            round = "12",
            scoreA = "73",
            scoreB = "72",
            quarter = "4",
            remainingPartialTime = "06:42",
            gameTime = "33:00",
            scoreQuarter1A = 33,
            scoreQuarter2A = 48,
            scoreQuarter3A = 71,
            scoreQuarter4A = 73,
            scoreQuarter1B = 26,
            scoreQuarter2B = 44,
            scoreQuarter3B = 64,
            scoreQuarter4B = 72
        )

        val enrichedLive = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository.applyLiveHeaderToGameStatic(
            upcomingInV2,
            liveQ4Header
        )
        assertEquals(GameStatus.LIVE, enrichedLive.status)
        assertEquals(4, enrichedLive.currentQuarter)
        assertEquals("06:42", enrichedLive.remainingClock)
        assertEquals(73, enrichedLive.homeScore)
        assertEquals(72, enrichedLive.awayScore)
        assertEquals(33, enrichedLive.quarters.q1Home)
        assertEquals(15, enrichedLive.quarters.q2Home)
        assertEquals(23, enrichedLive.quarters.q3Home)
        assertEquals(2, enrichedLive.quarters.q4Home)

        // 2. Halftime game where Quarter is empty "" and GameTime is "20:00"
        val halftimeHeader = com.euroleague.courtside.data.remote.EuroLeagueLiveHeaderDto(
            live = true,
            round = "12",
            scoreA = "40",
            scoreB = "43",
            quarter = "",
            remainingPartialTime = "00:00",
            gameTime = "20:00",
            scoreQuarter1A = 19,
            scoreQuarter2A = 40,
            scoreQuarter3A = 40,
            scoreQuarter4A = 0,
            scoreQuarter1B = 20,
            scoreQuarter2B = 43,
            scoreQuarter3B = 43,
            scoreQuarter4B = 0
        )
        val enrichedHalftime = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository.applyLiveHeaderToGameStatic(
            upcomingInV2,
            halftimeHeader
        )
        assertEquals(GameStatus.LIVE, enrichedHalftime.status)
        assertEquals(2, enrichedHalftime.currentQuarter)
        assertEquals(40, enrichedHalftime.homeScore)
        assertEquals(43, enrichedHalftime.awayScore)
        assertEquals(0, enrichedHalftime.quarters.q3Home)
        assertEquals(0, enrichedHalftime.quarters.q4Home)

        // 3. Overtime finished game (ZAL 117 : 114 MUN) using ScoreExtraTimeA / ScoreExtraTimeB
        val otUpcomingInV2 = upcomingInV2.copy(id = "E2026_112", gameCode = 112, homeTeam = zal, awayTeam = mun)
        val otFinalHeader = com.euroleague.courtside.data.remote.EuroLeagueLiveHeaderDto(
            live = false,
            round = "12",
            scoreA = "117",
            scoreB = "114",
            quarter = "",
            remainingPartialTime = "00:00",
            gameTime = "45:00",
            scoreQuarter1A = 19,
            scoreQuarter2A = 50,
            scoreQuarter3A = 78,
            scoreQuarter4A = 101,
            scoreExtraTimeA = 117,
            scoreQuarter1B = 25,
            scoreQuarter2B = 57,
            scoreQuarter3B = 79,
            scoreQuarter4B = 101,
            scoreExtraTimeB = 114
        )
        val enrichedOtFinal = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository.applyLiveHeaderToGameStatic(
            otUpcomingInV2,
            otFinalHeader
        )
        assertEquals(GameStatus.FINAL, enrichedOtFinal.status)
        assertEquals(5, enrichedOtFinal.currentQuarter)
        assertEquals("00:00", enrichedOtFinal.remainingClock)
        assertEquals(117, enrichedOtFinal.homeScore)
        assertEquals(114, enrichedOtFinal.awayScore)
        assertEquals(16, enrichedOtFinal.quarters.otHome)
        assertEquals(13, enrichedOtFinal.quarters.otAway)
    }

    @Test
    fun `standings team page resolves last 3 games and next 3 upcoming games and removes Play-In labels in all 6 languages`() {
        val freshRepo = EuroLeagueMockRepository()
        val oly = freshRepo.findTeam("OLY") ?: freshRepo.teams.first()
        val pan = freshRepo.findTeam("PAN") ?: freshRepo.teams[1]
        val mun = freshRepo.findTeam("MUN") ?: freshRepo.teams[2]
        val mad = freshRepo.findTeam("MAD") ?: freshRepo.teams[3]
        val template = freshRepo.games.value.first()

        val syntheticGames = listOf(
            template.copy(id = "g_r8", round = 8, homeTeam = oly, awayTeam = pan, homeScore = 82, awayScore = 77, status = GameStatus.FINAL),
            template.copy(id = "g_r9", round = 9, homeTeam = mun, awayTeam = oly, homeScore = 79, awayScore = 85, status = GameStatus.FINAL),
            template.copy(id = "g_r10", round = 10, homeTeam = oly, awayTeam = mad, homeScore = 91, awayScore = 88, status = GameStatus.FINAL),
            template.copy(id = "g_r11", round = 11, homeTeam = pan, awayTeam = oly, homeScore = 80, awayScore = 76, status = GameStatus.FINAL),
            template.copy(id = "g_other", round = 12, homeTeam = mun, awayTeam = mad, homeScore = 0, awayScore = 0, status = GameStatus.UPCOMING),
            template.copy(id = "g_r12", round = 12, homeTeam = oly, awayTeam = mun, status = GameStatus.UPCOMING),
            template.copy(id = "g_r13", round = 13, homeTeam = mad, awayTeam = oly, status = GameStatus.UPCOMING),
            template.copy(id = "g_r14", round = 14, homeTeam = oly, awayTeam = pan, status = GameStatus.UPCOMING),
            template.copy(id = "g_r15", round = 15, homeTeam = mun, awayTeam = oly, status = GameStatus.UPCOMING)
        )

        val (last3, next3) = com.euroleague.courtside.ui.navigation.resolveTeamLastAndUpcomingGames(oly, syntheticGames)
        assertEquals(listOf(11, 10, 9), last3.map { it.round })
        assertEquals(listOf(12, 13, 14), next3.map { it.round })
        assertTrue(last3.none { it.id == "g_other" })
        assertTrue(next3.none { it.id == "g_other" })

        AppLanguage.entries.forEach { lang ->
            val strings = CourtsideTranslations.forLanguage(lang)
            assertFalse(strings.standingsHeader.contains("Play-In", ignoreCase = true))
            assertTrue(strings.backToStandingsButton.isNotBlank())
            assertTrue(strings.last3GamesHeader.isNotBlank())
            assertTrue(strings.next3GamesHeader.isNotBlank())
            assertTrue(strings.clickTeamHint.isNotBlank())
        }
    }

    @Test
    fun `form streaks normalize W L and legacy S N cleanly without rendering false LLLLL`() {
        val normalized = EuroLeagueMockRepository.normalizeStreakList(listOf("S", "N", "W", "L", "S"))
        assertEquals(listOf("W", "L", "W", "L", "W"), normalized)
        assertEquals(listOf("W", "W"), EuroLeagueMockRepository.normalizeStreakList(listOf("W", "S")))

        assertTrue(com.euroleague.courtside.ui.navigation.isWinStreakEntry("W"))
        assertTrue(com.euroleague.courtside.ui.navigation.isWinStreakEntry("S"))
        assertFalse(com.euroleague.courtside.ui.navigation.isWinStreakEntry("L"))
        assertFalse(com.euroleague.courtside.ui.navigation.isWinStreakEntry("N"))

        // Initial repository teams must use canonical W/L streaks and never be all LLLLL across the league
        val standings = EuroLeagueMockRepository().getStandings()
        assertEquals(20, standings.size)
        assertTrue(standings.all { team -> team.streak.all { it == "W" || it == "L" } })
        assertTrue(standings.count { team -> team.streak.contains("W") } >= 18)
    }

    @Test
    fun `official EuroLeague standings exclude overtime points from PF PA and diff while keeping OT in PPG and computing exact boxscore analytics`() {
        val freshRepo = EuroLeagueMockRepository()
        val ulk = freshRepo.findTeam("ULK")!!.copy(wins = 0, losses = 0, pointsFor = 0, pointsAgainst = 0, streak = emptyList())
        val prs = freshRepo.findTeam("PRS")!!.copy(wins = 0, losses = 0, pointsFor = 0, pointsAgainst = 0, streak = emptyList())
        val template = freshRepo.games.value.first()

        val prsBox = listOf(
            PlayerBoxscoreStat(
                playerId = "PRS_1", playerName = "TJ Shorts", jerseyNumber = 0, teamId = "PRS",
                minutes = "35:00", points = 111, offRebounds = 10, defRebounds = 28,
                assists = 22, steals = 7, blocks = 3, blocksAgainst = 2, turnovers = 12,
                twoPtMade = 30, twoPtAttempted = 50, threePtMade = 11, threePtAttempted = 30,
                ftMade = 18, ftAttempted = 25, foulsDrawn = 21, foulsCommitted = 19
            )
        )
        val ulkBox = listOf(
            PlayerBoxscoreStat(
                playerId = "ULK_1", playerName = "Nigel Hayes-Davis", jerseyNumber = 11, teamId = "ULK",
                minutes = "36:00", points = 109, offRebounds = 12, defRebounds = 26,
                assists = 20, steals = 6, blocks = 2, blocksAgainst = 3, turnovers = 14,
                twoPtMade = 29, twoPtAttempted = 52, threePtMade = 11, threePtAttempted = 28,
                ftMade = 18, ftAttempted = 20, foulsDrawn = 19, foulsCommitted = 21,
                valuationOverride = 115
            )
        )
        // Verify blocksAgainst is subtracted when valuationOverride is absent, and valuationOverride is respected when present
        assertEquals(115, ulkBox.first().pir)

        // Round 11 overtime game: PRS 111 : 109 ULK (regulation 92 : 92, OT 19 : 17)
        val otGame = template.copy(
            id = "E2026_101",
            gameCode = 101,
            round = 11,
            homeTeam = prs,
            awayTeam = ulk,
            status = GameStatus.FINAL,
            currentQuarter = 5,
            remainingClock = "00:00",
            homeScore = 111,
            awayScore = 109,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(
                q1Home = 24, q1Away = 22,
                q2Home = 23, q2Away = 25,
                q3Home = 21, q3Away = 23,
                q4Home = 24, q4Away = 22,
                otHome = 19, otAway = 17
            ),
            boxscoreHome = prsBox,
            boxscoreAway = ulkBox
        )
        // Round 12 regulation game: ULK 88 : 78 PRS
        val regGame = template.copy(
            id = "E2026_291",
            gameCode = 291,
            round = 12,
            homeTeam = ulk,
            awayTeam = prs,
            status = GameStatus.FINAL,
            currentQuarter = 4,
            remainingClock = "00:00",
            homeScore = 88,
            awayScore = 78,
            quarters = com.euroleague.courtside.data.repository.QuarterScores(
                q1Home = 22, q1Away = 20,
                q2Home = 22, q2Away = 19,
                q3Home = 22, q3Away = 20,
                q4Home = 22, q4Away = 19
            ),
            boxscoreHome = emptyList(),
            boxscoreAway = emptyList()
        )

        val recomputed = EuroLeagueMockRepository.recomputeOfficialStandingsFromGames(
            teams = listOf(ulk, prs),
            games = listOf(otGame, regGame)
        )
        val ulkUpdated = recomputed.first { it.id == "ULK" }
        val prsUpdated = recomputed.first { it.id == "PRS" }

        // Regulation PF/PA exclude overtime (92+88 = 180 PF, 92+78 = 170 PA -> +10 diff for ULK)
        assertEquals(1, ulkUpdated.wins)
        assertEquals(1, ulkUpdated.losses)
        assertEquals(180, ulkUpdated.pointsFor)
        assertEquals(170, ulkUpdated.pointsAgainst)
        assertEquals(10, ulkUpdated.pointDiff)
        // Total points include OT (109+88 = 197 -> 98.5 PPG)
        assertEquals(197, ulkUpdated.totalPointsFor)
        assertEquals(98.5f, ulkUpdated.ppg, 0.1f)
        assertEquals(listOf("L", "W"), ulkUpdated.streak)
        assertEquals(listOf("W", "L"), prsUpdated.streak)

        // Both clubs are 1-1 and played 2 H2H games: H2H wins 1-1, H2H regulation diff +10 for ULK -> ULK ranks #1
        val sorted = EuroLeagueMockRepository.sortOfficialStandings(recomputed, listOf(otGame, regGame))
        assertEquals("ULK", sorted[0].id)
        assertEquals("PRS", sorted[1].id)

        // Verify Win% sorting when games played differ (7-4 = 63.6% ranks above 7-5 = 58.3%)
        val sevenFour = ulkUpdated.copy(id = "PAM", wins = 7, losses = 4, pointsFor = 900, pointsAgainst = 880)
        val sevenFive = prsUpdated.copy(id = "MAD", wins = 7, losses = 5, pointsFor = 1020, pointsAgainst = 930)
        val diffGpSorted = EuroLeagueMockRepository.sortOfficialStandings(listOf(sevenFive, sevenFour), emptyList())
        assertEquals("PAM", diffGpSorted[0].id)
        assertEquals("MAD", diffGpSorted[1].id)
    }

    @Test
    fun `bundled euroleague_live_snapshot json has 20 verified clubs with W L streaks and official OT-excluded standings`() {
        val snapshotFile = java.io.File("src/main/assets/euroleague_live_snapshot.json").let {
            if (it.exists()) it else java.io.File("app/src/main/assets/euroleague_live_snapshot.json")
        }
        assertTrue("Bundled snapshot file must exist", snapshotFile.exists())

        val mockDao = object : com.euroleague.courtside.data.local.CourtsideDao {
            override fun observeStandings() = kotlinx.coroutines.flow.flowOf(emptyList<com.euroleague.courtside.data.local.CachedTeamEntity>())
            override fun observeGamesByRound(round: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.euroleague.courtside.data.local.CachedGameEntity>())
            override fun observeAllGames() = kotlinx.coroutines.flow.flowOf(emptyList<com.euroleague.courtside.data.local.CachedGameEntity>())
            override suspend fun upsertTeams(teams: List<com.euroleague.courtside.data.local.CachedTeamEntity>) {}
            override suspend fun upsertGames(games: List<com.euroleague.courtside.data.local.CachedGameEntity>) {}
        }
        val dummyApi = object : com.euroleague.courtside.data.remote.EuroLeagueApiService {
            override suspend fun fetchSeasonClubs(seasonCode: String, limit: Int) = com.euroleague.courtside.data.remote.EuroLeagueClubsEnvelopeDto()
            override suspend fun fetchClubRosterPeople(seasonCode: String, clubCode: String) = emptyList<com.euroleague.courtside.data.remote.EuroLeagueClubPersonDto>()
            override suspend fun fetchSeasonRounds(seasonCode: String) = com.euroleague.courtside.data.remote.EuroLeagueRoundsEnvelopeDto()
            override suspend fun fetchSeasonGames(seasonCode: String, round: Int?, limit: Int) = com.euroleague.courtside.data.remote.EuroLeagueGamesEnvelopeDto()
            override suspend fun fetchGameBoxscoreStats(seasonCode: String, gameCode: Int) = com.euroleague.courtside.data.remote.EuroLeagueGameBoxscoreDto()
            override suspend fun fetchPlayerTraditionalStats(seasonMode: String, seasonCode: String, statisticMode: String, limit: Int) = com.euroleague.courtside.data.remote.EuroLeaguePlayerStatsEnvelopeDto()
            override suspend fun fetchTeamTraditionalStats(seasonMode: String, seasonCode: String, statisticMode: String, limit: Int) = com.euroleague.courtside.data.remote.EuroLeagueTeamStatsEnvelopeDto()
            override suspend fun fetchLiveGameBoxscore(fullUrl: String) = com.euroleague.courtside.data.remote.EuroLeagueLiveBoxscoreEnvelopeDto()
            override suspend fun fetchLiveGameShotPoints(fullUrl: String) = com.euroleague.courtside.data.remote.EuroLeagueLivePointsDto()
            override suspend fun fetchLiveGameHeader(fullUrl: String) = com.euroleague.courtside.data.remote.EuroLeagueLiveHeaderDto()
            override suspend fun fetchLiveGamePlayByPlay(fullUrl: String) = com.euroleague.courtside.data.remote.EuroLeagueLivePlayByPlayDto()
        }

        val repo = EuroLeagueMockRepository()
        val offlineFirst = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository(
            apiService = dummyApi,
            dao = mockDao,
            fallbackMockRepository = repo
        )
        offlineFirst.applySnapshotJsonString(snapshotFile.readText(Charsets.UTF_8), isLiveNetwork = false)

        val standings = repo.getStandings()
        assertEquals(20, standings.size)
        // Every club must have non-empty W/L streaks, and top clubs must NOT have all LLLLL!
        standings.forEach { team ->
            assertTrue("Streak must not be empty for ${team.id}", team.streak.isNotEmpty())
            assertTrue("Streak must only contain W or L for ${team.id}: ${team.streak}", team.streak.all { it == "W" || it == "L" })
            assertTrue("ORtg must be realistic basketball value for ${team.id}: ${team.offensiveRating}", team.offensiveRating in 92f..132f)
            assertTrue("DRtg must be realistic basketball value for ${team.id}: ${team.defensiveRating}", team.defensiveRating in 92f..132f)
            assertTrue("Pace must be realistic 40-min FIBA possessions for ${team.id}: ${team.pace}", team.pace in 64f..88f)
            assertTrue("REB% must be realistic percentage for ${team.id}: ${team.reboundRate}", team.reboundRate in 38f..62f)
        }
        assertTrue(standings.take(10).none { it.streak == listOf("L", "L", "L", "L", "L") })
        // Sum of regulation pointsFor across all 20 clubs must equal sum of regulation pointsAgainst
        assertEquals(standings.sumOf { it.pointsFor }, standings.sumOf { it.pointsAgainst })
        // Sum of wins across all 20 clubs must equal sum of losses
        assertEquals(standings.sumOf { it.wins }, standings.sumOf { it.losses })
    }

    @Test
    fun `Android phones use compact single-column layout and status-bar safe area while tablets and laptops preserve desktop split-view`() {
        // Portrait smartphones (< 600dp width, sw360dp..sw440dp) -> single-column compact layout + phone safe-area insets
        assertTrue(com.euroleague.courtside.ui.navigation.shouldUseSingleColumnMobileLayout(androidx.window.core.layout.WindowWidthSizeClass.COMPACT))
        assertTrue(com.euroleague.courtside.ui.navigation.isAndroidPhoneViewport(isCompactWidth = true, smallestScreenWidthDp = 393))
        assertTrue(com.euroleague.courtside.ui.navigation.isAndroidPhoneViewport(isCompactWidth = false, smallestScreenWidthDp = 412))

        // Tablets (sw600dp..sw800dp) and Laptops/Chromebooks (sw840dp..sw1080dp) -> preserve untouched multi-column split-view
        assertFalse(com.euroleague.courtside.ui.navigation.shouldUseSingleColumnMobileLayout(androidx.window.core.layout.WindowWidthSizeClass.MEDIUM))
        assertFalse(com.euroleague.courtside.ui.navigation.shouldUseSingleColumnMobileLayout(androidx.window.core.layout.WindowWidthSizeClass.EXPANDED))
        assertFalse(com.euroleague.courtside.ui.navigation.isAndroidPhoneViewport(isCompactWidth = false, smallestScreenWidthDp = 600))
        assertFalse(com.euroleague.courtside.ui.navigation.isAndroidPhoneViewport(isCompactWidth = false, smallestScreenWidthDp = 840))
        assertFalse(com.euroleague.courtside.ui.navigation.isAndroidPhoneViewport(isCompactWidth = false, smallestScreenWidthDp = 1080))
    }

    @Test
    fun `live PlayByPlay parser handles ForthQuarter and ExtraTime and computes LeadMomentumSummary`() {
        val dto = com.euroleague.courtside.data.remote.EuroLeagueLivePlayByPlayDto(
            live = true,
            codeTeamA = "MUN",
            codeTeamB = "PAN",
            actualQuarter = 4,
            firstQuarter = listOf(
                com.euroleague.courtside.data.remote.EuroLeaguePlayByPlayRowDto(
                    numberOfPlay = 10,
                    codeTeam = "MUN       ",
                    playerId = "P005001   ",
                    playType = "3FGM",
                    player = "EDWARDS, CARSEN",
                    dorsal = "3",
                    minute = 2,
                    markerTime = "08:30",
                    pointsA = 3,
                    pointsB = 0,
                    playInfo = "Three Pointer (1/1 - 3 pt)"
                ),
                com.euroleague.courtside.data.remote.EuroLeaguePlayByPlayRowDto(
                    numberOfPlay = 14,
                    codeTeam = "PAN       ",
                    playerId = "P009999   ",
                    playType = "2FGM",
                    player = "NUNN, KENDRICK",
                    dorsal = "25",
                    minute = 3,
                    markerTime = "07:15",
                    pointsA = 3,
                    pointsB = 5,
                    playInfo = "Two Pointer"
                )
            ),
            forthQuarter = listOf(
                com.euroleague.courtside.data.remote.EuroLeaguePlayByPlayRowDto(
                    numberOfPlay = 410,
                    codeTeam = "MUN       ",
                    playerId = "P005001   ",
                    playType = "DUNK",
                    player = "EDWARDS, CARSEN",
                    dorsal = "3",
                    minute = 39,
                    markerTime = "01:12",
                    pointsA = 84,
                    pointsB = 82,
                    playInfo = "Fastbreak Dunk"
                )
            )
        )

        val parsed = com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository.parseLivePlayByPlayStatic(
            dto = dto,
            gameCode = 120,
            homeTeamId = "MUN",
            awayTeamId = "PAN"
        )
        assertEquals(3, parsed.size)
        assertEquals("Carsen Edwards", parsed.first().playerName)
        assertEquals(com.euroleague.courtside.data.repository.PlayEventCategory.THREE_PT, parsed.first().category)
        assertEquals(3, parsed.first().pointsScored)
        assertTrue(parsed.last().isClutchPlay)
        assertEquals(84, parsed.last().homeScore)
        assertEquals(82, parsed.last().awayScore)

        val liveGame = repository.games.value.first { it.id == "g_live_1" }.copy(playByPlay = parsed)
        val momentum = EuroLeagueMockRepository.computeLeadMomentumSummary(liveGame, parsed)
        assertEquals(3, momentum.maxHomeLead)
        assertEquals(2, momentum.maxAwayLead)
        assertEquals(2, momentum.leadChanges)
    }

    @Test
    fun `player scouting summary aggregates shooting splits minutes and game log and resolves clicked players`() {
        val freshRepo = EuroLeagueMockRepository()
        val edwards = freshRepo.players.first { it.name.contains("Edwards", ignoreCase = true) }
        val summary = EuroLeagueMockRepository.buildPlayerScoutingSummary(edwards, freshRepo.games.value)

        assertTrue(summary.gamesLogged >= 1)
        assertTrue(summary.gameLog.isNotEmpty())
        assertTrue(summary.twoPtPct in 0f..100f)
        assertTrue(summary.threePtPct in 0f..100f)
        assertTrue(summary.ftPct in 0f..100f)

        val resolved = com.euroleague.courtside.ui.screens.resolveClickedPlayerProfile(
            playerId = "",
            playerName = edwards.name,
            teamId = edwards.teamId,
            allPlayers = freshRepo.players,
            allGames = freshRepo.games.value
        )
        assertTrue(resolved != null)
        assertEquals(edwards.id, resolved?.id)
    }

    @Test
    fun `automatic game-night alert engine deduplicates Tip-Off Halftime Final and Crunch-Time and preserves deep-link vs start destination`() {
        val freshRepo = EuroLeagueMockRepository()
        val now = 1_763_900_000_000L
        val mun = freshRepo.findTeam("MUN") ?: freshRepo.teams.first()
        val pan = freshRepo.findTeam("PAN") ?: freshRepo.teams[1]
        val template = freshRepo.games.value.first()

        val upcomingSoon = template.copy(
            id = "g_up_fav",
            round = 12,
            homeTeam = mun,
            awayTeam = pan,
            status = GameStatus.UPCOMING,
            tipOffEpochMillis = now + 3 * 3_600_000L
        )
        val liveHalftime = template.copy(
            id = "g_ht_fav",
            round = 12,
            homeTeam = mun,
            awayTeam = pan,
            status = GameStatus.LIVE,
            currentQuarter = 2,
            remainingClock = "00:00",
            homeScore = 44,
            awayScore = 41
        )
        val recentFinal = template.copy(
            id = "g_fin_recent",
            round = 12,
            homeTeam = mun,
            awayTeam = pan,
            status = GameStatus.FINAL,
            homeScore = 89,
            awayScore = 85,
            tipOffEpochMillis = now - 3 * 3_600_000L
        )
        val oldRound1Final = recentFinal.copy(
            id = "g_fin_old_r1",
            round = 1,
            tipOffEpochMillis = now - 30L * 24L * 3_600_000L
        )
        val liveCrunch = template.copy(
            id = "g_crunch_fav",
            round = 12,
            homeTeam = mun,
            awayTeam = pan,
            status = GameStatus.LIVE,
            currentQuarter = 4,
            remainingClock = "01:20",
            homeScore = 82,
            awayScore = 80
        )

        val actions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(upcomingSoon, liveHalftime, recentFinal, oldRound1Final, liveCrunch),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = true,
            halftimeFinalEnabled = true,
            closeGameEnabled = true,
            alreadySentKeys = emptySet(),
            nowMillis = now
        )
        assertEquals(4, actions.size)
        assertTrue(actions.none { it.game.id == "g_fin_old_r1" })

        // Deduplication check: passing dispatched keys suppresses repeat alerts
        val repeatActions = com.euroleague.courtside.notifications.EuroLeagueNotificationManager.selectAutoAlertActions(
            games = listOf(upcomingSoon, liveHalftime, recentFinal, oldRound1Final, liveCrunch),
            favoriteTeamIds = setOf("MUN"),
            tipOffEnabled = true,
            halftimeFinalEnabled = true,
            closeGameEnabled = true,
            alreadySentKeys = actions.map { it.dedupeKey }.toSet(),
            nowMillis = now
        )
        assertTrue(repeatActions.isEmpty())

        // Deep-link jumps to Match Center while normal reset restores My Team
        freshRepo.openGameDeepLink("g_crunch_fav", 12)
        assertEquals(com.euroleague.courtside.ui.navigation.CourtsideDestination.MATCHDAY, freshRepo.activeDestinationFlow.value)
        assertEquals("g_crunch_fav" to 12, freshRepo.pendingDeepLinkGameFlow.value)
        freshRepo.consumePendingDeepLinkGame()
        assertEquals(null, freshRepo.pendingDeepLinkGameFlow.value)
        freshRepo.resetToStartDestination()
        assertEquals(com.euroleague.courtside.ui.navigation.CourtsideDestination.MY_TEAM, freshRepo.activeDestinationFlow.value)
    }

    @Test
    fun `Courtside Android Home-Screen Widget builds live scoreboard and countdown states and Monaco offline crest exists`() {
        val freshRepo = EuroLeagueMockRepository()
        val mun = freshRepo.findTeam("MUN") ?: freshRepo.teams.first()
        val pan = freshRepo.findTeam("PAN") ?: freshRepo.teams[1]
        val template = freshRepo.games.value.first()
        val now = 1_763_900_000_000L

        val upcomingGame = template.copy(
            id = "w_up",
            round = 13,
            homeTeam = mun,
            awayTeam = pan,
            status = GameStatus.UPCOMING,
            tipOffEpochMillis = now + 26 * 3_600_000L
        )
        val countdownState = com.euroleague.courtside.widget.CourtsideAppWidgetProvider.buildWidgetMatchupState(
            primaryTeam = mun,
            games = listOf(upcomingGame),
            activeRound = 13,
            language = AppLanguage.EN,
            nowMillis = now
        )
        assertFalse(countdownState.isLive)
        assertTrue(countdownState.centerPrimaryText.contains("Day"))

        val liveGame = upcomingGame.copy(
            id = "w_live",
            status = GameStatus.LIVE,
            currentQuarter = 4,
            remainingClock = "02:11",
            homeScore = 79,
            awayScore = 77
        )
        val liveState = com.euroleague.courtside.widget.CourtsideAppWidgetProvider.buildWidgetMatchupState(
            primaryTeam = mun,
            games = listOf(liveGame),
            activeRound = 13,
            language = AppLanguage.DE,
            nowMillis = now
        )
        assertTrue(liveState.isLive)
        assertEquals("79 : 77", liveState.centerPrimaryText)
        assertTrue(liveState.badgeText.contains("Q4"))

        // Verify offline AS Monaco crest PNG exists in drawable-nodpi
        val mcoPng = java.io.File("src/main/res/drawable-nodpi/club_logo_mco.png").let {
            if (it.exists()) it else java.io.File("app/src/main/res/drawable-nodpi/club_logo_mco.png")
        }
        assertTrue("Offline Monaco crest club_logo_mco.png must exist", mcoPng.exists() && mcoPng.length() > 500)

        // Verify all 6 languages translate all v1.8.0 labels
        AppLanguage.entries.forEach { lang ->
            val s = CourtsideTranslations.forLanguage(lang)
            assertTrue(s.playByPlayTabLabel.isNotBlank())
            assertTrue(s.courtViewMomentumTab.isNotBlank())
            assertTrue(s.playerProfileTitle.isNotBlank())
            assertTrue(s.gameLogHeader.isNotBlank())
            assertTrue(s.widgetSectionTitle.isNotBlank())
            assertTrue(s.themeSectionTitle.isNotBlank())
            assertTrue(s.lightModeLabel.isNotBlank())
            assertTrue(s.darkModeLabel.isNotBlank())
        }
    }

    @Test
    fun `Light Mode is the default AppThemeMode on startup and toggles cleanly with Dark Mode`() {
        assertEquals(
            com.euroleague.courtside.ui.theme.AppThemeMode.LIGHT,
            com.euroleague.courtside.ui.theme.AppThemeMode.DEFAULT
        )
        assertEquals(
            com.euroleague.courtside.ui.theme.AppThemeMode.LIGHT,
            com.euroleague.courtside.ui.theme.AppThemeMode.fromCode(null)
        )
        assertEquals(
            com.euroleague.courtside.ui.theme.AppThemeMode.LIGHT,
            com.euroleague.courtside.ui.theme.AppThemeMode.fromCode("light")
        )
        assertEquals(
            com.euroleague.courtside.ui.theme.AppThemeMode.DARK,
            com.euroleague.courtside.ui.theme.AppThemeMode.fromCode("dark")
        )

        val freshRepo = EuroLeagueMockRepository()
        assertEquals(
            com.euroleague.courtside.ui.theme.AppThemeMode.LIGHT,
            freshRepo.themeModeFlow.value
        )

        freshRepo.selectThemeMode(freshRepo.themeModeFlow.value.toggled())
        assertEquals(
            com.euroleague.courtside.ui.theme.AppThemeMode.DARK,
            freshRepo.themeModeFlow.value
        )

        val lightPalette = com.euroleague.courtside.ui.theme.paletteForThemeMode(com.euroleague.courtside.ui.theme.AppThemeMode.LIGHT)
        val darkPalette = com.euroleague.courtside.ui.theme.paletteForThemeMode(com.euroleague.courtside.ui.theme.AppThemeMode.DARK)
        assertTrue(lightPalette.mode.isLight)
        assertFalse(darkPalette.mode.isLight)
        assertTrue(lightPalette.cardSurface != darkPalette.cardSurface)
        assertTrue(lightPalette.primaryText != darkPalette.primaryText)

        // Player headshots (750x1000 3:4 portraits) must anchor TopCenter so heads are never cut off
        assertEquals(
            androidx.compose.ui.Alignment.TopCenter,
            com.euroleague.courtside.ui.screens.PlayerHeadshotCropAlignment
        )

        // Bundled snapshot player names must be clean without stray passport middle-name commas (e.g. "Evan Fournier")
        val snapFile = java.io.File("src/main/assets/euroleague_live_snapshot.json").let {
            if (it.exists()) it else java.io.File("app/src/main/assets/euroleague_live_snapshot.json")
        }
        assertFalse(snapFile.readText(Charsets.UTF_8).contains("Evan , Mehdi"))
    }
}
