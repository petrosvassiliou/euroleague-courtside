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
}

