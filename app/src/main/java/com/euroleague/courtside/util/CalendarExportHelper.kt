package com.euroleague.courtside.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * Bietet zwei vollständig native Kalender-Export-Wege für EuroLeague Courtside:
 * 1. Direkter System-Intent (`CalendarContract.Events.CONTENT_URI`) für Google Kalender.
 * 2. Standardkonformer RFC 5545 `.ics`-Generator für den kompletten Saison-Spielplan via `FileProvider`.
 */
object CalendarExportHelper {

    private const val DEFAULT_GAME_DURATION_MILLIS = 2L * 60L * 60L * 1000L // 2 Stunden
    private val ICS_UTC_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    /**
     * Weg 1: Öffnet die native Google Kalender App (oder den System-Kalender) mit vorausgefüllten
     * Spieldaten, Arena-Standort, lokaler Zeitzone und 30-Minuten-Tip-Off-Erinnerung.
     */
    fun exportSingleGameToNativeCalendar(context: Context, game: EuroLeagueGame) {
        val startMillis = game.tipOffEpochMillis
        val endMillis = startMillis + DEFAULT_GAME_DURATION_MILLIS
        val localTimeZoneId = TimeZone.getDefault().id

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(
                CalendarContract.Events.TITLE,
                "🏀 EuroLeague R${game.round}: ${game.homeTeam.name} vs. ${game.awayTeam.name}"
            )
            putExtra(
                CalendarContract.Events.EVENT_LOCATION,
                "${game.homeTeam.arena}, ${game.homeTeam.city} (${game.homeTeam.country})"
            )
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                buildString {
                    appendLine("EuroLeague Regular Season – Round ${game.round}")
                    appendLine("Matchup: ${game.homeTeam.name} (${game.homeTeam.code}) vs. ${game.awayTeam.name} (${game.awayTeam.code})")
                    appendLine("Head-to-Head: ${game.headToHeadHomeWins} - ${game.headToHeadAwayWins}")
                    appendLine("Reminder: 30 Minuten vor Tip-Off")
                    append("Exportiert via EuroLeague Courtside")
                }
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.EVENT_TIMEZONE, localTimeZoneId)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            // Hinweis für Google Kalender / CalendarProvider (30-Minuten-Reminder)
            putExtra(CalendarContract.Events.HAS_ALARM, 1)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            context.startActivity(Intent.createChooser(intent, "In Kalender speichern").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    /**
     * Weg 2: Generiert eine RFC 5545 konforme `.ics`-Datei mit `VALARM` (30-Minuten-Reminder)
     * für alle Saisonspiele eines Teams und teilt sie nativ über `FileProvider`.
     */
    fun exportTeamSeasonToIcsFile(
        context: Context,
        team: EuroLeagueTeam,
        games: List<EuroLeagueGame>
    ) {
        val teamGames = games.filter { it.homeTeam.id == team.id || it.awayTeam.id == team.id }
            .ifEmpty { games }

        val icsContent = buildRfc5545IcsPayload(team, teamGames)
        val exportDir = File(context.cacheDir, "calendar").apply { mkdirs() }
        val sanitizedCode = team.code.lowercase()
        val icsFile = File(exportDir, "euroleague_${sanitizedCode}_season.ics")
        icsFile.writeText(icsContent, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            icsFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "EuroLeague Saison-Spielplan – ${team.name} (.ics)"
            )
            putExtra(
                Intent.EXTRA_TEXT,
                "Kompletter EuroLeague-Spielplan für ${team.name} inkl. 30-Minuten-Tip-Off-Alarme."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Saison-Spielplan (.ics) exportieren").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /**
     * Erstellt den RFC 5545 Standard-String mit CRLF (`\r\n`) Zeilenumbrüchen.
     */
    fun buildRfc5545IcsPayload(
        team: EuroLeagueTeam,
        games: List<EuroLeagueGame>
    ): String {
        val nowStamp = ICS_UTC_FORMATTER.format(Instant.now())
        val sb = StringBuilder()

        fun appendCrlf(line: String) {
            sb.append(line).append("\r\n")
        }

        appendCrlf("BEGIN:VCALENDAR")
        appendCrlf("VERSION:2.0")
        appendCrlf("PRODID:-//EuroLeague Courtside//Googlebook OS & Android//DE")
        appendCrlf("CALSCALE:GREGORIAN")
        appendCrlf("METHOD:PUBLISH")
        appendCrlf("X-WR-CALNAME:EuroLeague - ${escapeIcsText(team.name)}")
        appendCrlf("X-WR-TIMEZONE:${ZoneId.systemDefault().id}")

        games.forEach { game ->
            val startInstant = Instant.ofEpochMilli(game.tipOffEpochMillis)
            val endInstant = Instant.ofEpochMilli(game.tipOffEpochMillis + DEFAULT_GAME_DURATION_MILLIS)
            val dtStart = ICS_UTC_FORMATTER.format(startInstant)
            val dtEnd = ICS_UTC_FORMATTER.format(endInstant)
            val isHome = game.homeTeam.id == team.id
            val homeAwayTag = if (isHome) "HEIM" else "AUSWÄRTS"

            appendCrlf("BEGIN:VEVENT")
            appendCrlf("UID:euroleague-${game.id}-${team.code}@courtside.euroleague.com")
            appendCrlf("DTSTAMP:$nowStamp")
            appendCrlf("DTSTART:$dtStart")
            appendCrlf("DTEND:$dtEnd")
            appendCrlf(
                "SUMMARY:${escapeIcsText("[$homeAwayTag] R${game.round}: ${game.homeTeam.name} vs ${game.awayTeam.name}")}"
            )
            appendCrlf(
                "LOCATION:${escapeIcsText("${game.homeTeam.arena}, ${game.homeTeam.city}")}"
            )
            appendCrlf(
                "DESCRIPTION:${escapeIcsText("EuroLeague Round ${game.round} | H2H: ${game.headToHeadHomeWins}:${game.headToHeadAwayWins}")}"
            )
            appendCrlf("STATUS:CONFIRMED")
            // 30-Minuten-Reminder (VALARM)
            appendCrlf("BEGIN:VALARM")
            appendCrlf("TRIGGER:-PT30M")
            appendCrlf("ACTION:DISPLAY")
            appendCrlf("DESCRIPTION:Tip-Off in 30 Minuten: ${escapeIcsText(game.homeTeam.code)} vs ${escapeIcsText(game.awayTeam.code)}")
            appendCrlf("END:VALARM")
            appendCrlf("END:VEVENT")
        }

        appendCrlf("END:VCALENDAR")
        return sb.toString()
    }

    private fun escapeIcsText(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
}
