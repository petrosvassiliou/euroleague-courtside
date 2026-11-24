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
import com.euroleague.courtside.R
import com.euroleague.courtside.data.local.CourtsidePreferencesDataStore
import com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.GameStatus
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

enum class GameReminderType(val code: String, val shortBadge: String) {
    Q1_RESULT("Q1", "Q1"),
    HALFTIME_RESULT("HT", "HT"),
    Q3_RESULT("Q3", "Q3"),
    CRUNCH_TIME("CRUNCH", "🔥 Q4"),
    FINAL_SCORE("FINAL", "FT");

    companion object {
        fun fromCode(code: String): GameReminderType? =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }
}

enum class ScoreMilestone {
    Q1,
    HALFTIME,
    Q3,
    FINAL
}

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

        const val EXTRA_DEEP_LINK_GAME_ID = "extra_deep_link_game_id"
        const val EXTRA_DEEP_LINK_ROUND = "extra_deep_link_round"

        private const val PREFS_NAME = "courtside_notification_prefs"
        private const val KEY_ACTIVE_LANGUAGE = "active_language_code"
        private const val KEY_DISPATCHED_EVENTS = "dispatched_alert_event_keys"

        private const val NOTIFICATION_ID_TIP_OFF = 4101
        private const val NOTIFICATION_ID_SCORE = 4102
        private const val NOTIFICATION_ID_CLOSE_GAME = 4103
        private const val NOTIFICATION_ID_TEST_PUSH = 4199

        enum class AutoAlertType {
            SCHEDULE_TIP_OFF,
            Q1_SCORE,
            HALFTIME_SCORE,
            Q3_SCORE,
            FINAL_SCORE,
            CRUNCH_TIME
        }

        data class AutoAlertAction(
            val type: AutoAlertType,
            val game: EuroLeagueGame,
            val dedupeKey: String
        )

        fun isQuarterResultReady(game: EuroLeagueGame, quarterNumber: Int): Boolean {
            if (game.status != GameStatus.LIVE) return false
            val clockTrimmed = game.remainingClock.trim()
            val isClockZero = clockTrimmed == "00:00" || clockTrimmed == "0:00"
            val q = game.quarters
            return when (quarterNumber) {
                1 -> (q.q1Home + q.q1Away) > 0 &&
                    ((game.currentQuarter == 1 && isClockZero) ||
                        (game.currentQuarter == 2 && (q.q2Home + q.q2Away) <= 6))
                2 -> (q.q1Home + q.q1Away + q.q2Home + q.q2Away) > 0 &&
                    ((game.currentQuarter == 2 && isClockZero) ||
                        (game.currentQuarter == 3 && (q.q3Home + q.q3Away) <= 6))
                3 -> (q.q3Home + q.q3Away) > 0 &&
                    ((game.currentQuarter == 3 && isClockZero) ||
                        (game.currentQuarter == 4 && (q.q4Home + q.q4Away) <= 6))
                else -> false
            }
        }

        /**
         * Reine Entscheidungslogik für automatische Spieltags-Benachrichtigungen UND spielspezifische
         * Erinnerungen (`perGameReminders`: Q1, Halbzeit, Q3, Crunch-Time, Endergebnis).
         */
        fun selectAutoAlertActions(
            games: List<EuroLeagueGame>,
            favoriteTeamIds: Set<String>,
            tipOffEnabled: Boolean,
            halftimeFinalEnabled: Boolean,
            closeGameEnabled: Boolean,
            alreadySentKeys: Set<String>,
            nowMillis: Long = System.currentTimeMillis(),
            perGameReminders: Map<String, Set<GameReminderType>> = emptyMap()
        ): List<AutoAlertAction> {
            val favCanon = favoriteTeamIds.map { EuroLeagueMockRepository.canonicalClubId(it) }.toSet()
            val actions = mutableListOf<AutoAlertAction>()

            for (game in games) {
                val homeCanon = EuroLeagueMockRepository.canonicalClubId(game.homeTeam.id)
                val awayCanon = EuroLeagueMockRepository.canonicalClubId(game.awayTeam.id)
                val involvesFavorite = favCanon.isEmpty() || homeCanon in favCanon || awayCanon in favCanon
                val custom = perGameReminders[game.id].orEmpty()

                // 1. Geplante Tip-Off-Alarme (15 Min. vorher) für Favoriten oder Spiele mit aktiver Erinnerung
                if (((tipOffEnabled && involvesFavorite) || custom.isNotEmpty()) && game.status == GameStatus.UPCOMING) {
                    val timeUntilTipOff = game.tipOffEpochMillis - nowMillis
                    val key = "tipoff_${game.id}"
                    if (tipOffEnabled && involvesFavorite &&
                        timeUntilTipOff in (16 * 60_000L)..(48 * 3_600_000L) &&
                        key !in alreadySentKeys
                    ) {
                        actions.add(AutoAlertAction(AutoAlertType.SCHEDULE_TIP_OFF, game, key))
                    }
                }

                // 2. 1. Viertel-Ergebnis (spielspezifische Erinnerung)
                if (GameReminderType.Q1_RESULT in custom && isQuarterResultReady(game, 1)) {
                    val key = "q1_${game.id}"
                    if (key !in alreadySentKeys) {
                        actions.add(AutoAlertAction(AutoAlertType.Q1_SCORE, game, key))
                    }
                }

                // 3. Halbzeit-Ergebnis (Favoriten-Kanal ODER spielspezifische Erinnerung)
                if ((GameReminderType.HALFTIME_RESULT in custom || (halftimeFinalEnabled && involvesFavorite)) &&
                    isQuarterResultReady(game, 2)
                ) {
                    val key = "ht_${game.id}"
                    if (key !in alreadySentKeys) {
                        actions.add(AutoAlertAction(AutoAlertType.HALFTIME_SCORE, game, key))
                    }
                }

                // 4. 3. Viertel-Ergebnis (spielspezifische Erinnerung)
                if (GameReminderType.Q3_RESULT in custom && isQuarterResultReady(game, 3)) {
                    val key = "q3_${game.id}"
                    if (key !in alreadySentKeys) {
                        actions.add(AutoAlertAction(AutoAlertType.Q3_SCORE, game, key))
                    }
                }

                // 5. Offizielles Endergebnis (Favoriten-Kanal ODER spielspezifische Erinnerung im 12-Std.-Fenster)
                val finishedRecently = game.status == GameStatus.FINAL &&
                    (game.homeScore > 0 || game.awayScore > 0) &&
                    (nowMillis - game.tipOffEpochMillis) in (-2 * 3_600_000L)..(12 * 3_600_000L)
                if ((GameReminderType.FINAL_SCORE in custom || (halftimeFinalEnabled && involvesFavorite)) &&
                    finishedRecently
                ) {
                    val key = "final_${game.id}"
                    if (key !in alreadySentKeys) {
                        actions.add(AutoAlertAction(AutoAlertType.FINAL_SCORE, game, key))
                    }
                }

                // 6. Crunch-Time Thriller (Q4 <= 3:00, <= 5 Pkt. Differenz)
                if ((GameReminderType.CRUNCH_TIME in custom || (closeGameEnabled && involvesFavorite)) &&
                    game.isCloseGameFourthQuarter
                ) {
                    val key = "crunch_${game.id}"
                    if (key !in alreadySentKeys) {
                        actions.add(AutoAlertAction(AutoAlertType.CRUNCH_TIME, game, key))
                    }
                }
            }
            return actions
        }

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
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = strings.scoresChannelSubtitle
            enableVibration(true)
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
            priority = NotificationCompat.PRIORITY_HIGH,
            deepLinkGameId = game.id,
            deepLinkRound = game.round
        )

        enqueueSilentTipOffWorker(game, language)
    }

    private fun enqueueSilentTipOffWorker(
        game: EuroLeagueGame,
        language: AppLanguage
    ) {
        val matchupTitle = "${game.homeTeam.name} vs. ${game.awayTeam.name}"
        val arenaLabel = "${game.homeTeam.arena} (${game.homeTeam.city})"
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
            notificationId = NOTIFICATION_ID_CLOSE_GAME + (game.gameCode % 50),
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_HIGH,
            deepLinkGameId = game.id,
            deepLinkRound = game.round
        )
    }

    /**
     * Sendet eine Viertel- (Q1, Halbzeit, Q3) oder Endergebnis-Benachrichtigung in der gewählten App-Sprache.
     */
    fun sendMilestoneScoreNotification(
        game: EuroLeagueGame,
        milestone: ScoreMilestone,
        language: AppLanguage = resolveActiveLanguage()
    ) {
        val payload = buildMilestoneScoreContent(game, milestone, language)
        val distinctId = NOTIFICATION_ID_SCORE + ((game.gameCode.coerceAtLeast(1) % 200) * 10) + milestone.ordinal

        dispatchNotification(
            channelId = CHANNEL_SCORES,
            notificationId = distinctId,
            title = payload.title,
            shortText = payload.shortText,
            bigText = payload.bigText,
            summaryText = payload.summaryText,
            priority = NotificationCompat.PRIORITY_HIGH,
            deepLinkGameId = game.id,
            deepLinkRound = game.round
        )
    }

    fun sendHalftimeOrFinalNotification(
        game: EuroLeagueGame,
        isFinal: Boolean,
        language: AppLanguage = resolveActiveLanguage()
    ) {
        sendMilestoneScoreNotification(
            game = game,
            milestone = if (isFinal) ScoreMilestone.FINAL else ScoreMilestone.HALFTIME,
            language = language
        )
    }

    /**
     * Wird aufgerufen, wenn der Nutzer Erinnerungen für ein spezifisches Spiel ändert:
     * - Markiert bereits vergangene Viertel eines laufenden Spiels als erledigt, damit nur kommende Spielphasen feuern.
     * - Plant gezielte WorkManager-Hintergrundchecks für Q1, Halbzeit, Q3, Crunch-Time & Ende dieses Spiels.
     */
    fun onGameRemindersUpdated(
        game: EuroLeagueGame,
        enabledReminders: Set<GameReminderType>
    ) {
        val workManager = WorkManager.getInstance(context)
        if (enabledReminders.isEmpty()) {
            workManager.cancelAllWorkByTag("game_rem_${game.id}")
            return
        }

        if (game.status == GameStatus.LIVE) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val sentSet = prefs.getStringSet(KEY_DISPATCHED_EVENTS, emptySet())?.toMutableSet() ?: mutableSetOf()
            val q = game.quarters
            if (game.currentQuarter > 1 && (q.q2Home + q.q2Away) > 6) {
                sentSet.add("q1_${game.id}")
            }
            if (game.currentQuarter > 2 && (q.q3Home + q.q3Away) > 6) {
                sentSet.add("ht_${game.id}")
            }
            if (game.currentQuarter > 3 && (q.q4Home + q.q4Away) > 6) {
                sentSet.add("q3_${game.id}")
            }
            prefs.edit().putStringSet(KEY_DISPATCHED_EVENTS, sentSet).apply()
        }

        if (game.status == GameStatus.FINAL) return

        val nowMillis = System.currentTimeMillis()
        val offsetsMinutes = buildList {
            if (GameReminderType.Q1_RESULT in enabledReminders) add("q1" to 22L)
            if (GameReminderType.HALFTIME_RESULT in enabledReminders) add("ht" to 48L)
            if (GameReminderType.Q3_RESULT in enabledReminders) add("q3" to 80L)
            if (GameReminderType.CRUNCH_TIME in enabledReminders) add("crunch" to 98L)
            if (GameReminderType.FINAL_SCORE in enabledReminders) add("ft" to 112L)
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        for ((slot, minsFromTipOff) in offsetsMinutes) {
            val targetMs = game.tipOffEpochMillis + TimeUnit.MINUTES.toMillis(minsFromTipOff)
            val delayMs = (targetMs - nowMillis).coerceAtLeast(2 * 60_000L)
            if (targetMs >= nowMillis - 25 * 60_000L) {
                val req = OneTimeWorkRequestBuilder<DailyEuroLeagueSyncWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag("game_rem_${game.id}")
                    .build()
                workManager.enqueueUniqueWork(
                    "game_rem_${game.id}_$slot",
                    ExistingWorkPolicy.REPLACE,
                    req
                )
            }
        }
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
            priority = NotificationCompat.PRIORITY_MAX,
            deepLinkGameId = game.id,
            deepLinkRound = game.round
        )
    }

    /**
     * Wertet bei Hintergrund-Syncs und Live-Ticks alle Spiele für die Favoriten-Teams aus,
     * plant automatisch anstehende Tip-Off-Alarme, versendet deduplizierte Halbzeit-/End-/Crunch-Time-Pushes
     * und aktiviert während laufender Spieltage einen 15-Minuten-Folge-Check.
     */
    fun evaluateAutomaticGameNightAlerts(
        games: List<EuroLeagueGame>,
        favoriteTeamIds: Set<String>,
        tipOffEnabled: Boolean,
        halftimeFinalEnabled: Boolean,
        closeGameEnabled: Boolean,
        language: AppLanguage = resolveActiveLanguage(),
        perGameReminders: Map<String, Set<GameReminderType>> = emptyMap()
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sentSet = prefs.getStringSet(KEY_DISPATCHED_EVENTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowMillis = System.currentTimeMillis()

        val actions = selectAutoAlertActions(
            games = games,
            favoriteTeamIds = favoriteTeamIds,
            tipOffEnabled = tipOffEnabled,
            halftimeFinalEnabled = halftimeFinalEnabled,
            closeGameEnabled = closeGameEnabled,
            alreadySentKeys = sentSet,
            nowMillis = nowMillis,
            perGameReminders = perGameReminders
        )

        if (actions.isNotEmpty()) {
            for (action in actions) {
                when (action.type) {
                    AutoAlertType.SCHEDULE_TIP_OFF -> enqueueSilentTipOffWorker(action.game, language)
                    AutoAlertType.Q1_SCORE -> sendMilestoneScoreNotification(action.game, ScoreMilestone.Q1, language)
                    AutoAlertType.HALFTIME_SCORE -> sendMilestoneScoreNotification(action.game, ScoreMilestone.HALFTIME, language)
                    AutoAlertType.Q3_SCORE -> sendMilestoneScoreNotification(action.game, ScoreMilestone.Q3, language)
                    AutoAlertType.FINAL_SCORE -> sendMilestoneScoreNotification(action.game, ScoreMilestone.FINAL, language)
                    AutoAlertType.CRUNCH_TIME -> evaluateAndSendCloseGameAlert(action.game, language = language)
                }
                sentSet.add(action.dedupeKey)
            }
            val trimmed = if (sentSet.size > 400) sentSet.toList().takeLast(400).toSet() else sentSet
            prefs.edit().putStringSet(KEY_DISPATCHED_EVENTS, trimmed).apply()
        }

        val hasImminentOrLiveGame = games.any { g ->
            g.status == GameStatus.LIVE ||
                (g.status == GameStatus.UPCOMING && (g.tipOffEpochMillis - nowMillis) in 0L..(90 * 60_000L))
        }
        val anyPerGameActive = perGameReminders.values.any { it.isNotEmpty() }
        if (hasImminentOrLiveGame && (tipOffEnabled || halftimeFinalEnabled || closeGameEnabled || anyPerGameActive)) {
            scheduleGameWindowFollowUpCheck()
        }
    }

    private fun scheduleGameWindowFollowUpCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DailyEuroLeagueSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .addTag("euroleague_gamenight_live_window")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "euroleague_gamenight_live_window",
            ExistingWorkPolicy.KEEP,
            request
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
    ): LocalizedNotificationPayload = buildMilestoneScoreContent(
        game = game,
        milestone = if (isFinal) ScoreMilestone.FINAL else ScoreMilestone.HALFTIME,
        language = language
    )

    fun buildMilestoneScoreContent(
        game: EuroLeagueGame,
        milestone: ScoreMilestone,
        language: AppLanguage
    ): LocalizedNotificationPayload {
        val strings = CourtsideTranslations.forLanguage(language)
        val statusLabel = when (milestone) {
            ScoreMilestone.Q1 -> when (language) {
                AppLanguage.EN -> "END OF 1ST QUARTER"
                AppLanguage.DE -> "ENDE 1. VIERTEL"
                AppLanguage.FR -> "FIN DU 1ER QUART"
                AppLanguage.IT -> "FINE 1° QUARTO"
                AppLanguage.ES -> "FINAL 1.º CUARTO"
                AppLanguage.EL -> "ΤΕΛΟΣ 1ΗΣ ΠΕΡΙΟΔΟΥ"
            }
            ScoreMilestone.HALFTIME -> when (language) {
                AppLanguage.EN -> "HALFTIME SCORE"
                AppLanguage.DE -> "HALBZEIT-STAND"
                AppLanguage.FR -> "SCORE À LA MI-TEMPS"
                AppLanguage.IT -> "FINE PRIMO TEMPO"
                AppLanguage.ES -> "DESCANSO"
                AppLanguage.EL -> "ΗΜΙΧΡΟΝΟ"
            }
            ScoreMilestone.Q3 -> when (language) {
                AppLanguage.EN -> "END OF 3RD QUARTER"
                AppLanguage.DE -> "ENDE 3. VIERTEL"
                AppLanguage.FR -> "FIN DU 3E QUART"
                AppLanguage.IT -> "FINE 3° QUARTO"
                AppLanguage.ES -> "FINAL 3.º CUARTO"
                AppLanguage.EL -> "ΤΕΛΟΣ 3ΗΣ ΠΕΡΙΟΔΟΥ"
            }
            ScoreMilestone.FINAL -> when (language) {
                AppLanguage.EN -> "FINAL SCORE"
                AppLanguage.DE -> "ENDERGEBNIS"
                AppLanguage.FR -> "SCORE FINAL"
                AppLanguage.IT -> "RISULTATO FINALE"
                AppLanguage.ES -> "RESULTADO FINAL"
                AppLanguage.EL -> "ΤΕΛΙΚΟ ΣΚΟΡ"
            }
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

        val q = game.quarters
        val (displayHome, displayAway) = when (milestone) {
            ScoreMilestone.Q1 -> if (q.q1Home + q.q1Away > 0) (q.q1Home to q.q1Away) else (game.homeScore to game.awayScore)
            ScoreMilestone.HALFTIME -> {
                val h = q.q1Home + q.q2Home
                val a = q.q1Away + q.q2Away
                if (h + a > 0) (h to a) else (game.homeScore to game.awayScore)
            }
            ScoreMilestone.Q3 -> {
                val h = q.q1Home + q.q2Home + q.q3Home
                val a = q.q1Away + q.q2Away + q.q3Away
                if (h + a > 0) (h to a) else (game.homeScore to game.awayScore)
            }
            ScoreMilestone.FINAL -> game.homeScore to game.awayScore
        }

        val partialsSummary = when (milestone) {
            ScoreMilestone.Q1 -> "Q1 ${q.q1Home}:${q.q1Away}"
            ScoreMilestone.HALFTIME -> "Q1 ${q.q1Home}:${q.q1Away} • Q2 ${q.q2Home}:${q.q2Away}"
            ScoreMilestone.Q3 -> "Q1 ${q.q1Home}:${q.q1Away} • Q2 ${q.q2Home}:${q.q2Away} • Q3 ${q.q3Home}:${q.q3Away}"
            ScoreMilestone.FINAL -> buildString {
                append("Q1 ${q.q1Home}:${q.q1Away} • Q2 ${q.q2Home}:${q.q2Away} • Q3 ${q.q3Home}:${q.q3Away} • Q4 ${q.q4Home}:${q.q4Away}")
                if (q.otHome != null && q.otAway != null) {
                    append(" • OT ${q.otHome}:${q.otAway}")
                }
            }
        }

        val title = "🏀 $statusLabel • ${strings.roundPrefix} ${game.round}"
        val shortText = "${game.homeTeam.name} $displayHome : $displayAway ${game.awayTeam.name}"
        val topHome = game.boxscoreHome.maxByOrNull { it.pir }
        val topAway = game.boxscoreAway.maxByOrNull { it.pir }

        val bigText = buildString {
            appendLine(shortText)
            if (q.q1Home + q.q1Away > 0) {
                appendLine(partialsSummary)
            }
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
        priority: Int,
        deepLinkGameId: String? = null,
        deepLinkRound: Int? = null
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
            if (!deepLinkGameId.isNullOrBlank()) {
                putExtra(EXTRA_DEEP_LINK_GAME_ID, deepLinkGameId)
                if (deepLinkRound != null) {
                    putExtra(EXTRA_DEEP_LINK_ROUND, deepLinkRound)
                }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_courtside_ball)
            .setColor(0xFFFF6600.toInt())
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
    fun mockRepository(): EuroLeagueMockRepository
    fun preferencesDataStore(): CourtsidePreferencesDataStore
    fun notificationManager(): EuroLeagueNotificationManager
}

/**
 * Periodischer & Spieltags-WorkManager-Worker:
 * Synchronisiert im Hintergrund alle EuroLeague-Clubs, aktuelle Kader (`people`), Verletzten-Reports,
 * Tabellenstände und Spieltage, wertet automatische Spieltags-Pushes (Tip-Off, Halbzeit/Ende, Crunch-Time) aus
 * und aktualisiert das Android Startbildschirm-Widget.
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
            val offlineRepo = entryPoint.offlineFirstRepository()
            val mockRepo = entryPoint.mockRepository()
            val prefs = entryPoint.preferencesDataStore()
            val notifManager = entryPoint.notificationManager()

            offlineRepo.loadBundledLiveSnapshotFromAssets(applicationContext)
            offlineRepo.refreshLiveMatchdayTick()
            offlineRepo.syncSeasonDataFromRemoteOrSeedCache()

            val primaryId = prefs.getSavedPrimaryTeamImmediate() ?: "MUN"
            val favIds = (prefs.getSavedFavoriteTeamsImmediate() ?: setOf(primaryId)) + primaryId
            val lang = prefs.getSavedLanguageImmediate() ?: notifManager.resolveActiveLanguage()

            notifManager.evaluateAutomaticGameNightAlerts(
                games = mockRepo.games.value,
                favoriteTeamIds = favIds,
                tipOffEnabled = prefs.getSavedTipOffAlarmImmediate(),
                halftimeFinalEnabled = prefs.getSavedHalftimeFinalImmediate(),
                closeGameEnabled = prefs.getSavedCloseGameAlertImmediate(),
                language = lang,
                perGameReminders = prefs.getSavedGameRemindersImmediate()
            )

            com.euroleague.courtside.widget.CourtsideAppWidgetProvider.updateAllWidgets(applicationContext)
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
        val gameId = inputData.getString("GAME_ID")
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
            priority = NotificationCompat.PRIORITY_HIGH,
            deepLinkGameId = gameId,
            deepLinkRound = round
        )
        return Result.success()
    }
}
