package com.euroleague.courtside.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.i18n.AppLanguage
import com.euroleague.courtside.i18n.CourtsideTranslations
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class LocalizedNotificationPayload(
    val title: String,
    val shortText: String,
    val bigText: String,
    val summaryText: String
)

/**
 * Verwaltet die dedizierten EuroLeague-Notification-Channels sowie WorkManager-Jobs in allen
 * 6 unterstützten Sprachen (EN, DE, FR, IT, ES, EL) für:
 * - Tip-Off-Alarme (15 Min. vor Spielbeginn)
 * - Halbzeit- & Endergebnisse
 * - Close-Game-Alerts (Q4 < 3:00 Restzeit & <= 5 Punkte Differenz)
 * - Sofortige Test-Push-Benachrichtigungen auf Googlebook OS & Android
 */
@Singleton
class EuroLeagueNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_TIP_OFF = "euroleague_tip_off_channel"
        const val CHANNEL_SCORES = "euroleague_scores_channel"
        const val CHANNEL_CLOSE_GAME = "euroleague_close_game_channel"

        private const val PREFS_NAME = "courtside_notification_prefs"
        private const val KEY_ACTIVE_LANGUAGE = "active_language_code"

        private const val NOTIFICATION_ID_TIP_OFF = 4101
        private const val NOTIFICATION_ID_SCORE = 4102
        private const val NOTIFICATION_ID_CLOSE_GAME = 4103
        private const val NOTIFICATION_ID_TEST_PUSH = 4199

        fun formatTipOffAlarm(
            matchup: String,
            arena: String,
            round: Int,
            language: AppLanguage
        ): LocalizedNotificationPayload {
            val strings = CourtsideTranslations.forLanguage(language)
            return when (language) {
                AppLanguage.EN -> LocalizedNotificationPayload(
                    title = "⏰ Tip-Off in 15 Minutes! (${strings.roundPrefix} $round)",
                    shortText = "$matchup • $arena",
                    bigText = "Get ready for ${strings.roundPrefix} $round: $matchup live from $arena. Open EuroLeague Courtside for live boxscore & shot chart!",
                    summaryText = "Live Match Center"
                )
                AppLanguage.DE -> LocalizedNotificationPayload(
                    title = "⏰ Tip-Off in 15 Minuten! (${strings.roundPrefix} $round)",
                    shortText = "$matchup • $arena",
                    bigText = "Mach dich bereit für ${strings.roundPrefix} $round: $matchup live aus der $arena. Öffne EuroLeague Courtside für Live-Boxscore & Shot-Chart!",
                    summaryText = "Live Match Center"
                )
                AppLanguage.FR -> LocalizedNotificationPayload(
                    title = "⏰ Entre-deux dans 15 minutes ! (${strings.roundPrefix} $round)",
                    shortText = "$matchup • $arena",
                    bigText = "Préparez-vous pour la ${strings.roundPrefix} $round : $matchup en direct de $arena. Ouvrez EuroLeague Courtside pour le boxscore et la carte des tirs en direct !",
                    summaryText = "Match Center en Direct"
                )
                AppLanguage.IT -> LocalizedNotificationPayload(
                    title = "⏰ Palla a due tra 15 minuti! (${strings.roundPrefix} $round)",
                    shortText = "$matchup • $arena",
                    bigText = "Preparati per la ${strings.roundPrefix} $round: $matchup in diretta da $arena. Apri EuroLeague Courtside per boxscore e shot-chart live!",
                    summaryText = "Match Center Live"
                )
                AppLanguage.ES -> LocalizedNotificationPayload(
                    title = "⏰ ¡Salto inicial en 15 minutos! (${strings.roundPrefix} $round)",
                    shortText = "$matchup • $arena",
                    bigText = "Prepárate para la ${strings.roundPrefix} $round: $matchup en directo desde $arena. ¡Abre EuroLeague Courtside para estadísticas y mapa de tiros en vivo!",
                    summaryText = "Match Center en Vivo"
                )
                AppLanguage.EL -> LocalizedNotificationPayload(
                    title = "⏰ Τζάμπολ σε 15 λεπτά! (${strings.roundPrefix} $round)",
                    shortText = "$matchup • $arena",
                    bigText = "Ετοιμαστείτε για την ${strings.roundPrefix} $round: $matchup ζωντανά από το $arena. Ανοίξτε το EuroLeague Courtside για live στατιστικά & γράφημα σουτ!",
                    summaryText = "Ζωντανό Match Center"
                )
            }
        }
    }

    @Volatile
    private var currentLanguage: AppLanguage = loadSavedLanguage()

    init {
        createNotificationChannels(currentLanguage)
    }

    private fun loadSavedLanguage(): AppLanguage {
        val code = runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_LANGUAGE, null)
        }.getOrNull()
        return AppLanguage.fromCode(code)
    }

    fun resolveActiveLanguage(): AppLanguage = loadSavedLanguage()

    /**
     * Aktualisiert die aktive Sprache für alle System-Benachrichtigungen, WorkManager-Alarme
     * und Android-Notification-Channels.
     */
    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_LANGUAGE, language.code)
                .apply()
        }
        createNotificationChannels(language)
    }

    fun createNotificationChannels(language: AppLanguage = resolveActiveLanguage()) {
        val strings = CourtsideTranslations.forLanguage(language)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tipOffChannel = NotificationChannel(
            CHANNEL_TIP_OFF,
            strings.tipOffChannelTitle,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.tipOffChannelSubtitle
            enableVibration(true)
        }

        val scoresChannel = NotificationChannel(
            CHANNEL_SCORES,
            strings.scoresChannelTitle,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = strings.scoresChannelSubtitle
        }

        val closeGameChannel = NotificationChannel(
            CHANNEL_CLOSE_GAME,
            strings.closeGameChannelTitle,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.closeGameChannelSubtitle
            enableVibration(true)
        }

        manager.createNotificationChannels(
            listOf(tipOffChannel, scoresChannel, closeGameChannel)
        )
    }

    /**
     * Plant einen WorkManager-Job exakt 15 Minuten vor Tip-Off des ausgewählten Spiels
     * in der aktuell gewählten App-Sprache.
     */
    fun scheduleTipOffAlarm(
        game: EuroLeagueGame,
        language: AppLanguage = resolveActiveLanguage()
    ) {
        setLanguage(language)
        val matchupTitle = "${game.homeTeam.name} vs. ${game.awayTeam.name}"
        val arenaLabel = "${game.homeTeam.arena} (${game.homeTeam.city})"
        val payload = buildTipOffAlarmContent(
            matchup = matchupTitle,
            arena = arenaLabel,
            round = game.round,
            language = language
        )
        dispatchNotification(
            channelId = CHANNEL_TIP_OFF,
            notificationId = NOTIFICATION_ID_TIP_OFF,
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_HIGH
        )

        val triggerAtMillis = game.tipOffEpochMillis - TimeUnit.MINUTES.toMillis(15)
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)

        val data = workDataOf(
            "GAME_ID" to game.id,
            "MATCHUP_TITLE" to matchupTitle,
            "ARENA" to arenaLabel,
            "ROUND" to game.round,
            "LANGUAGE_CODE" to language.code
        )

        val request = OneTimeWorkRequestBuilder<TipOffNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("tipoff_${game.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "tipoff_${game.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Feuert automatisch einen lokalisierten Close-Game-Alert, falls das Spiel im 4. Viertel
     * unter 3 Minuten Spielzeit <= 5 Punkte Differenz aufweist.
     */
    fun evaluateAndSendCloseGameAlert(
        game: EuroLeagueGame,
        language: AppLanguage = resolveActiveLanguage()
    ) {
        if (!game.isCloseGameFourthQuarter) return
        val payload = buildCloseGameAlertContent(game, language)

        dispatchNotification(
            channelId = CHANNEL_CLOSE_GAME,
            notificationId = NOTIFICATION_ID_CLOSE_GAME,
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Sendet eine Halbzeit- oder Endergebnis-Benachrichtigung in der gewählten App-Sprache.
     */
    fun sendHalftimeOrFinalNotification(
        game: EuroLeagueGame,
        isFinal: Boolean,
        language: AppLanguage = resolveActiveLanguage()
    ) {
        val payload = buildHalftimeOrFinalContent(game, isFinal, language)

        dispatchNotification(
            channelId = CHANNEL_SCORES,
            notificationId = NOTIFICATION_ID_SCORE,
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    /**
     * Sofortiger Test-Push-Button im Einstellungs-Screen: Feuert eine echte native System-Notification
     * mit `BigTextStyle` und Live-Score-Aufschlüsselung in der aktuell ausgewählten App-Sprache.
     */
    fun sendInstantTestPushNotification(
        game: EuroLeagueGame,
        language: AppLanguage = resolveActiveLanguage()
    ) {
        setLanguage(language)
        val payload = buildTestPushContent(game, language)

        dispatchNotification(
            channelId = CHANNEL_CLOSE_GAME,
            notificationId = NOTIFICATION_ID_TEST_PUSH,
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_MAX
        )
    }

    fun buildTipOffAlarmContent(
        matchup: String,
        arena: String,
        round: Int,
        language: AppLanguage
    ): LocalizedNotificationPayload {
        val strings = CourtsideTranslations.forLanguage(language)
        return when (language) {
            AppLanguage.EN -> LocalizedNotificationPayload(
                title = "⏰ Tip-Off in 15 Minutes! (${strings.roundPrefix} $round)",
                shortText = "$matchup • $arena",
                bigText = "Get ready for ${strings.roundPrefix} $round: $matchup live from $arena. Open EuroLeague Courtside for live boxscore & shot chart!",
                summaryText = "Live Match Center"
            )
            AppLanguage.DE -> LocalizedNotificationPayload(
                title = "⏰ Tip-Off in 15 Minuten! (${strings.roundPrefix} $round)",
                shortText = "$matchup • $arena",
                bigText = "Mach dich bereit für ${strings.roundPrefix} $round: $matchup live aus der $arena. Öffne EuroLeague Courtside für Live-Boxscore & Shot-Chart!",
                summaryText = "Live Match Center"
            )
            AppLanguage.FR -> LocalizedNotificationPayload(
                title = "⏰ Entre-deux dans 15 minutes ! (${strings.roundPrefix} $round)",
                shortText = "$matchup • $arena",
                bigText = "Préparez-vous pour la ${strings.roundPrefix} $round : $matchup en direct de $arena. Ouvrez EuroLeague Courtside pour le boxscore et la carte des tirs en direct !",
                summaryText = "Match Center en Direct"
            )
            AppLanguage.IT -> LocalizedNotificationPayload(
                title = "⏰ Palla a due tra 15 minuti! (${strings.roundPrefix} $round)",
                shortText = "$matchup • $arena",
                bigText = "Preparati per la ${strings.roundPrefix} $round: $matchup in diretta da $arena. Apri EuroLeague Courtside per boxscore e shot-chart live!",
                summaryText = "Match Center Live"
            )
            AppLanguage.ES -> LocalizedNotificationPayload(
                title = "⏰ ¡Salto inicial en 15 minutos! (${strings.roundPrefix} $round)",
                shortText = "$matchup • $arena",
                bigText = "Prepárate para la ${strings.roundPrefix} $round: $matchup en directo desde $arena. ¡Abre EuroLeague Courtside para estadísticas y mapa de tiros en vivo!",
                summaryText = "Match Center en Vivo"
            )
            AppLanguage.EL -> LocalizedNotificationPayload(
                title = "⏰ Τζάμπολ σε 15 λεπτά! (${strings.roundPrefix} $round)",
                shortText = "$matchup • $arena",
                bigText = "Ετοιμαστείτε για την ${strings.roundPrefix} $round: $matchup ζωντανά από το $arena. Ανοίξτε το EuroLeague Courtside για live στατιστικά & γράφημα σουτ!",
                summaryText = "Ζωντανό Match Center"
            )
        }
    }

    fun buildCloseGameAlertContent(
        game: EuroLeagueGame,
        language: AppLanguage
    ): LocalizedNotificationPayload {
        val diff = kotlin.math.abs(game.homeScore - game.awayScore)
        val topPlayer = (game.boxscoreHome + game.boxscoreAway).maxByOrNull { it.pir }
        val qLine = "Q1 ${game.quarters.q1Home}:${game.quarters.q1Away} | Q2 ${game.quarters.q2Home}:${game.quarters.q2Away} | Q3 ${game.quarters.q3Home}:${game.quarters.q3Away}"

        return when (language) {
            AppLanguage.EN -> {
                val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (Only $diff pts difference!)"
                val bigText = buildString {
                    appendLine("$summary at ${game.homeTeam.arena}!")
                    appendLine("Quarters: $qLine")
                    if (topPlayer != null) {
                        append("MVP Watch: ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
                    }
                }
                LocalizedNotificationPayload(
                    title = "🔥 CRUNCH-TIME ALERT (Q${game.currentQuarter} • ${game.remainingClock})",
                    shortText = summary,
                    bigText = bigText.trim(),
                    summaryText = "Live Match Center"
                )
            }
            AppLanguage.DE -> {
                val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (Nur $diff Pkt. Differenz!)"
                val bigText = buildString {
                    appendLine("$summary in der ${game.homeTeam.arena}!")
                    appendLine("Viertelstände: $qLine")
                    if (topPlayer != null) {
                        append("MVP-Watch: ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
                    }
                }
                LocalizedNotificationPayload(
                    title = "🔥 CRUNCH-TIME ALERT (Q${game.currentQuarter} • ${game.remainingClock})",
                    shortText = summary,
                    bigText = bigText.trim(),
                    summaryText = "Live Match Center"
                )
            }
            AppLanguage.FR -> {
                val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (Seulement $diff pts d'écart !)"
                val bigText = buildString {
                    appendLine("$summary à ${game.homeTeam.arena} !")
                    appendLine("Quart-temps : $qLine")
                    if (topPlayer != null) {
                        append("MVP à suivre : ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
                    }
                }
                LocalizedNotificationPayload(
                    title = "🔥 ALERTE MONEY-TIME (Q${game.currentQuarter} • ${game.remainingClock})",
                    shortText = summary,
                    bigText = bigText.trim(),
                    summaryText = "Match Center en Direct"
                )
            }
            AppLanguage.IT -> {
                val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (Solo $diff pt di scarto!)"
                val bigText = buildString {
                    appendLine("$summary alla ${game.homeTeam.arena}!")
                    appendLine("Parziali: $qLine")
                    if (topPlayer != null) {
                        append("MVP Watch: ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
                    }
                }
                LocalizedNotificationPayload(
                    title = "🔥 ALLERTA FINALE PUNTO A PUNTO (Q${game.currentQuarter} • ${game.remainingClock})",
                    shortText = summary,
                    bigText = bigText.trim(),
                    summaryText = "Match Center Live"
                )
            }
            AppLanguage.ES -> {
                val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (¡Solo $diff pts de diferencia!)"
                val bigText = buildString {
                    appendLine("¡$summary en ${game.homeTeam.arena}!")
                    appendLine("Cuartos: $qLine")
                    if (topPlayer != null) {
                        append("MVP en pista: ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
                    }
                }
                LocalizedNotificationPayload(
                    title = "🔥 ALERTA DE FINAL APRETADO (Q${game.currentQuarter} • ${game.remainingClock})",
                    shortText = summary,
                    bigText = bigText.trim(),
                    summaryText = "Match Center en Vivo"
                )
            }
            AppLanguage.EL -> {
                val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (Μόλις $diff πόντοι διαφορά!)"
                val bigText = buildString {
                    appendLine("$summary στο ${game.homeTeam.arena}!")
                    appendLine("Δεκάλεπτα: $qLine")
                    if (topPlayer != null) {
                        append("Κορυφαίος MVP: ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
                    }
                }
                LocalizedNotificationPayload(
                    title = "🔥 ΕΙΔΟΠΟΙΗΣΗ ΝΤΕΡΜΠΙ (Q${game.currentQuarter} • ${game.remainingClock})",
                    shortText = summary,
                    bigText = bigText.trim(),
                    summaryText = "Ζωντανό Match Center"
                )
            }
        }
    }

    fun buildHalftimeOrFinalContent(
        game: EuroLeagueGame,
        isFinal: Boolean,
        language: AppLanguage
    ): LocalizedNotificationPayload {
        val strings = CourtsideTranslations.forLanguage(language)
        val statusLabel = when (language) {
            AppLanguage.EN -> if (isFinal) "FINAL SCORE" else "HALFTIME SCORE"
            AppLanguage.DE -> if (isFinal) "ENDERGEBNIS" else "HALBZEIT-STAND"
            AppLanguage.FR -> if (isFinal) "SCORE FINAL" else "SCORE À LA MI-TEMPS"
            AppLanguage.IT -> if (isFinal) "RISULTATO FINALE" else "FINE PRIMO TEMPO"
            AppLanguage.ES -> if (isFinal) "RESULTADO FINAL" else "DESCANSO"
            AppLanguage.EL -> if (isFinal) "ΤΕΛΙΚΟ ΣΚΟΡ" else "ΗΜΙΧΡΟΝΟ"
        }
        val topPerformerLabel = when (language) {
            AppLanguage.EN -> "Top Performer"
            AppLanguage.DE -> "Top-Performer"
            AppLanguage.FR -> "Meilleur joueur"
            AppLanguage.IT -> "Miglior giocatore"
            AppLanguage.ES -> "Jugador destacado"
            AppLanguage.EL -> "Κορυφαίος παίκτης"
        }
        val summaryText = when (language) {
            AppLanguage.EN, AppLanguage.DE -> "Live Match Center"
            AppLanguage.FR -> "Match Center en Direct"
            AppLanguage.IT -> "Match Center Live"
            AppLanguage.ES -> "Match Center en Vivo"
            AppLanguage.EL -> "Ζωντανό Match Center"
        }

        val title = "🏀 $statusLabel • ${strings.roundPrefix} ${game.round}"
        val shortText = "${game.homeTeam.name} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.name}"
        val topHome = game.boxscoreHome.maxByOrNull { it.pir }
        val topAway = game.boxscoreAway.maxByOrNull { it.pir }

        val bigText = buildString {
            appendLine(shortText)
            if (topHome != null) {
                appendLine("${game.homeTeam.code} $topPerformerLabel: ${topHome.playerName} (${topHome.points} PTS, ${topHome.totalRebounds} REB, PIR ${topHome.pir})")
            }
            if (topAway != null) {
                append("${game.awayTeam.code} $topPerformerLabel: ${topAway.playerName} (${topAway.points} PTS, ${topAway.assists} AST, PIR ${topAway.pir})")
            }
        }

        return LocalizedNotificationPayload(
            title = title,
            shortText = shortText,
            bigText = bigText.trim(),
            summaryText = summaryText
        )
    }

    fun buildTestPushContent(
        game: EuroLeagueGame,
        language: AppLanguage
    ): LocalizedNotificationPayload {
        val diff = kotlin.math.abs(game.homeScore - game.awayScore)
        val topPlayer = (game.boxscoreHome + game.boxscoreAway).maxByOrNull { it.pir }
        val quartersStr = "Q1 ${game.quarters.q1Home}-${game.quarters.q1Away} • " +
                "Q2 ${game.quarters.q2Home}-${game.quarters.q2Away} • " +
                "Q3 ${game.quarters.q3Home}-${game.quarters.q3Away} • " +
                "Q4 ${game.quarters.q4Home}-${game.quarters.q4Away}"
        val shortText = "Q${game.currentQuarter} (${game.remainingClock}) – ${game.homeTeam.arena}"

        return when (language) {
            AppLanguage.EN -> LocalizedNotificationPayload(
                title = "🚨 LIVE CRUNCH-TIME • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}",
                shortText = shortText,
                bigText = buildString {
                    appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
                    appendLine("Status: Q${game.currentQuarter} | Remaining ${game.remainingClock} | Only $diff points difference!")
                    appendLine("Quarters: $quartersStr")
                    if (topPlayer != null) {
                        append("Top PIR on court: ${topPlayer.playerName} with ${topPlayer.points} PTS & PIR ${topPlayer.pir}")
                    }
                }.trim(),
                summaryText = "Live Match Center"
            )
            AppLanguage.DE -> LocalizedNotificationPayload(
                title = "🚨 LIVE CRUNCH-TIME • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}",
                shortText = shortText,
                bigText = buildString {
                    appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
                    appendLine("Status: Q${game.currentQuarter} | Restzeit ${game.remainingClock} | Nur $diff Punkte Differenz!")
                    appendLine("Viertel: $quartersStr")
                    if (topPlayer != null) {
                        append("Top-PIR auf dem Parkett: ${topPlayer.playerName} mit ${topPlayer.points} PTS & PIR ${topPlayer.pir}")
                    }
                }.trim(),
                summaryText = "Live Match Center"
            )
            AppLanguage.FR -> LocalizedNotificationPayload(
                title = "🚨 MONEY-TIME EN DIRECT • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}",
                shortText = shortText,
                bigText = buildString {
                    appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
                    appendLine("Statut : Q${game.currentQuarter} | Temps restant ${game.remainingClock} | Seulement $diff points d'écart !")
                    appendLine("Quart-temps : $quartersStr")
                    if (topPlayer != null) {
                        append("Meilleur PIR sur le parquet : ${topPlayer.playerName} avec ${topPlayer.points} PTS & PIR ${topPlayer.pir}")
                    }
                }.trim(),
                summaryText = "Match Center en Direct"
            )
            AppLanguage.IT -> LocalizedNotificationPayload(
                title = "🚨 FINALE PUNTO A PUNTO LIVE • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}",
                shortText = shortText,
                bigText = buildString {
                    appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
                    appendLine("Stato: Q${game.currentQuarter} | Tempo restante ${game.remainingClock} | Solo $diff punti di scarto!")
                    appendLine("Parziali: $quartersStr")
                    if (topPlayer != null) {
                        append("Miglior PIR sul parquet: ${topPlayer.playerName} con ${topPlayer.points} PTS & PIR ${topPlayer.pir}")
                    }
                }.trim(),
                summaryText = "Match Center Live"
            )
            AppLanguage.ES -> LocalizedNotificationPayload(
                title = "🚨 FINAL APRETADO EN VIVO • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}",
                shortText = shortText,
                bigText = buildString {
                    appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
                    appendLine("Estado: Q${game.currentQuarter} | Tiempo restante ${game.remainingClock} | ¡Solo $diff puntos de diferencia!")
                    appendLine("Cuartos: $quartersStr")
                    if (topPlayer != null) {
                        append("Mejor PIR en pista: ${topPlayer.playerName} con ${topPlayer.points} PTS y PIR ${topPlayer.pir}")
                    }
                }.trim(),
                summaryText = "Match Center en Vivo"
            )
            AppLanguage.EL -> LocalizedNotificationPayload(
                title = "🚨 ΖΩΝΤΑΝΟ ΝΤΕΡΜΠΙ • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}",
                shortText = shortText,
                bigText = buildString {
                    appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
                    appendLine("Κατάσταση: Q${game.currentQuarter} | Υπόλοιπο ${game.remainingClock} | Μόλις $diff πόντοι διαφορά!")
                    appendLine("Δεκάλεπτα: $quartersStr")
                    if (topPlayer != null) {
                        append("Κορυφαίο PIR στο παρκέ: ${topPlayer.playerName} με ${topPlayer.points} PTS & PIR ${topPlayer.pir}")
                    }
                }.trim(),
                summaryText = "Ζωντανό Match Center"
            )
        }
    }

    internal fun dispatchNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        shortText: String,
        bigText: String,
        summaryText: String = "Live Match Center",
        priority: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(shortText)
            .setSubText("EuroLeague Courtside")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(bigText)
                    .setSummaryText(summaryText)
            )
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Plant die tägliche Hintergrund-Synchronisation (alle 6 Stunden via WorkManager),
     * damit Kader (Rosters), Verletztenstatus, Tabellenstände und Spieltage jeden Tag aktuell sind.
     */
    fun scheduleDailyLiveSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<DailyEuroLeagueSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("euroleague_daily_live_sync")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "euroleague_daily_live_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailySyncWorkerEntryPoint {
    fun offlineFirstRepository(): EuroLeagueOfflineFirstRepository
}

/**
 * Periodischer WorkManager-Worker für den täglichen Einsatz:
 * Synchronisiert im Hintergrund alle EuroLeague-Clubs, aktuelle Kader (`people`), Verletzten-Reports,
 * Tabellenstände und Spieltage.
 */
class DailyEuroLeagueSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DailySyncWorkerEntryPoint::class.java
            )
            entryPoint.offlineFirstRepository().loadBundledLiveSnapshotFromAssets(applicationContext)
            entryPoint.offlineFirstRepository().syncSeasonDataFromRemoteOrSeedCache()
            Result.success()
        }.getOrDefault(Result.retry())
    }
}

/**
 * WorkManager-Worker für geplante Tip-Off-Alarme (15 Minuten vor Spielbeginn) in der ausgewählten Sprache.
 */
class TipOffNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val matchup = inputData.getString("MATCHUP_TITLE") ?: "EuroLeague Matchup"
        val arena = inputData.getString("ARENA") ?: "EuroLeague Arena"
        val round = inputData.getInt("ROUND", 1)

        val helper = EuroLeagueNotificationManager(applicationContext)
        val langCode = inputData.getString("LANGUAGE_CODE")
        val language = if (!langCode.isNullOrBlank()) {
            AppLanguage.fromCode(langCode)
        } else {
            helper.resolveActiveLanguage()
        }
        val payload = helper.buildTipOffAlarmContent(
            matchup = matchup,
            arena = arena,
            round = round,
            language = language
        )

        helper.dispatchNotification(
            channelId = EuroLeagueNotificationManager.CHANNEL_TIP_OFF,
            notificationId = 4101,
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_HIGH
        )
        return Result.success()
    }
}
