package com.euroleague.courtside.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.euroleague.courtside.R
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.i18n.AppLanguage
import com.euroleague.courtside.i18n.CourtsideTranslations
import com.euroleague.courtside.widget.CourtsideAppWidgetProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * v2.0 Lock-Screen Live Game Tracker (`ForegroundService`):
 * - Hält während aktiv verfolgter EuroLeague-Spiele (Per-Game-Reminders oder Favoriten-Club im Live-Fenster)
 *   ein schlankes, lautlos aktualisierendes Live-Scoreboard auf dem Android-Sperrbildschirm.
 * - Verhindert zuverlässig Android-Doze-Verzögerungen, sodass `Q1`-, `Halbzeit`-, `Q3`-, `Crunch-Time`-
 *   und `Endergebnis`-Alarme sekundengenau auch bei ausgeschaltetem Display ausgelöst werden.
 * - Beendet sich vollautomatisch nach Spielende (oder spätestens nach 3,5 Std. Sicherheits-Timeout).
 */
class CourtsideLiveTrackerService : Service() {

    companion object {
        const val CHANNEL_LIVE_TRACKER = "euroleague_live_lockscreen_tracker"
        const val NOTIFICATION_ID_LIVE_TRACKER = 4150

        const val ACTION_START_TRACKER = "com.euroleague.courtside.ACTION_START_LIVE_TRACKER"
        const val ACTION_PREVIEW_TRACKER = "com.euroleague.courtside.ACTION_PREVIEW_LIVE_TRACKER"
        const val ACTION_STOP_TRACKER = "com.euroleague.courtside.ACTION_STOP_LIVE_TRACKER"
        const val EXTRA_FOCUS_GAME_ID = "extra_focus_game_id"

        private const val MAX_TRACKER_DURATION_MS = 210 * 60_000L // 3.5h cap

        /**
         * Reine Hilfsfunktion (auch für Unit-Tests): Ermittelt alle Spiele, die aktuell im
         * Sperrbildschirm-Tracker verfolgt werden sollen (LIVE oder Tip-Off in <= 30 Min.).
         */
        fun selectActiveTrackedGames(
            games: List<EuroLeagueGame>,
            favoriteTeamIds: Set<String>,
            perGameReminders: Map<String, Set<GameReminderType>>,
            nowMillis: Long = System.currentTimeMillis()
        ): List<EuroLeagueGame> {
            val favCanon = favoriteTeamIds.map { EuroLeagueMockRepository.canonicalClubId(it) }.toSet()
            return games.filter { g ->
                val hasPerGame = !perGameReminders[g.id].isNullOrEmpty()
                val involvesFav = EuroLeagueMockRepository.canonicalClubId(g.homeTeam.id) in favCanon ||
                    EuroLeagueMockRepository.canonicalClubId(g.awayTeam.id) in favCanon
                if (!hasPerGame && !involvesFav) return@filter false

                when (g.status) {
                    GameStatus.LIVE -> true
                    GameStatus.UPCOMING -> {
                        val delta = g.tipOffEpochMillis - nowMillis
                        delta in (-3 * 3_600_000L)..(30 * 60_000L)
                    }
                    GameStatus.FINAL -> false
                }
            }.sortedWith(
                compareByDescending<EuroLeagueGame> { it.status == GameStatus.LIVE }
                    .thenByDescending { !perGameReminders[it.id].isNullOrEmpty() }
                    .thenByDescending { it.isCloseGameFourthQuarter }
                    .thenBy { it.tipOffEpochMillis }
            )
        }

        fun formatLockScreenScorePayload(
            trackedGames: List<EuroLeagueGame>,
            fallbackGame: EuroLeagueGame?,
            perGameReminders: Map<String, Set<GameReminderType>>,
            language: AppLanguage
        ): LocalizedNotificationPayload {
            val strings = CourtsideTranslations.forLanguage(language)
            val primary = trackedGames.firstOrNull() ?: fallbackGame
            if (primary == null) {
                return LocalizedNotificationPayload(
                    title = "🏀 EuroLeague Courtside • Live Tracker",
                    shortText = strings.lockScreenTrackerTitle,
                    bigText = strings.lockScreenTrackerSubtitle,
                    summaryText = "Live Scoreboard"
                )
            }

            val armedBadges = perGameReminders[primary.id].orEmpty()
                .sortedBy { it.ordinal }
                .joinToString(" • ") { it.shortBadge }

            val title = when (primary.status) {
                GameStatus.LIVE -> {
                    val qLabel = if (primary.currentQuarter >= 5) "OT" else "Q${primary.currentQuarter}"
                    val extraCount = (trackedGames.count { it.status == GameStatus.LIVE } - 1).coerceAtLeast(0)
                    val suffix = if (extraCount > 0) " (+$extraCount)" else ""
                    "🔴 $qLabel ${primary.remainingClock} • ${primary.homeTeam.code} ${primary.homeScore} : ${primary.awayScore} ${primary.awayTeam.code}$suffix"
                }
                GameStatus.FINAL -> {
                    "🏁 ${strings.statusFinal} • ${primary.homeTeam.code} ${primary.homeScore} : ${primary.awayScore} ${primary.awayTeam.code}"
                }
                GameStatus.UPCOMING -> {
                    val remMs = (primary.tipOffEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                    val mins = (remMs / 60_000L).coerceAtLeast(1L)
                    "⏰ ${primary.homeTeam.code} vs ${primary.awayTeam.code} • ${mins}m"
                }
            }

            val q = primary.quarters
            val qSplits = if (q.q1Home + q.q1Away > 0) {
                buildString {
                    append("Q1 ${q.q1Home}:${q.q1Away} • Q2 ${q.q2Home}:${q.q2Away} • Q3 ${q.q3Home}:${q.q3Away} • Q4 ${q.q4Home}:${q.q4Away}")
                    if (q.otHome != null && q.otAway != null) append(" • OT ${q.otHome}:${q.otAway}")
                }
            } else {
                "${primary.homeTeam.arena} • ${strings.roundPrefix} ${primary.round}"
            }

            val topPerformer = (primary.boxscoreHome + primary.boxscoreAway).maxByOrNull { it.pir }
            val bigBody = buildString {
                appendLine("${primary.homeTeam.name} vs ${primary.awayTeam.name}")
                appendLine(qSplits)
                if (trackedGames.size > 1) {
                    val others = trackedGames.drop(1).take(3).joinToString("  |  ") { g ->
                        if (g.status == GameStatus.LIVE) {
                            "${g.homeTeam.code} ${g.homeScore}:${g.awayScore} ${g.awayTeam.code} (Q${g.currentQuarter})"
                        } else {
                            "${g.homeTeam.code} vs ${g.awayTeam.code}"
                        }
                    }
                    appendLine(others)
                } else if (topPerformer != null) {
                    appendLine("MVP: ${topPerformer.playerName} (${topPerformer.points} PTS, PIR ${topPerformer.pir})")
                }
                if (armedBadges.isNotEmpty()) {
                    append("🔔 $armedBadges")
                }
            }.trim()

            return LocalizedNotificationPayload(
                title = title,
                shortText = qSplits,
                bigText = bigBody,
                summaryText = "Courtside Live Tracker"
            )
        }

        fun syncLiveTrackerIfEligible(
            context: Context,
            games: List<EuroLeagueGame>,
            favoriteTeamIds: Set<String>,
            perGameReminders: Map<String, Set<GameReminderType>>,
            trackerEnabled: Boolean
        ) {
            if (!trackerEnabled) {
                stopTracker(context)
                return
            }
            val activeTracked = selectActiveTrackedGames(games, favoriteTeamIds, perGameReminders)
            if (activeTracked.isNotEmpty()) {
                runCatching {
                    val intent = Intent(context, CourtsideLiveTrackerService::class.java).apply {
                        action = ACTION_START_TRACKER
                        putExtra(EXTRA_FOCUS_GAME_ID, activeTracked.first().id)
                    }
                    ContextCompat.startForegroundService(context, intent)
                }
            }
        }

        fun launchPreviewTracker(context: Context, focusGameId: String? = null) {
            runCatching {
                val intent = Intent(context, CourtsideLiveTrackerService::class.java).apply {
                    action = ACTION_PREVIEW_TRACKER
                    if (!focusGameId.isNullOrBlank()) putExtra(EXTRA_FOCUS_GAME_ID, focusGameId)
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stopTracker(context: Context) {
            runCatching {
                context.stopService(Intent(context, CourtsideLiveTrackerService::class.java))
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isPreviewMode: Boolean = false
    private var focusGameId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_TRACKER
        if (action == ACTION_STOP_TRACKER) {
            shutdownTracker()
            return START_NOT_STICKY
        }

        if (action == ACTION_PREVIEW_TRACKER) {
            isPreviewMode = true
        }
        intent?.getStringExtra(EXTRA_FOCUS_GAME_ID)?.takeIf { it.isNotBlank() }?.let {
            focusGameId = it
        }

        ensureTrackerChannel()
        val initialNotification = buildOngoingTrackerNotification()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID_LIVE_TRACKER, initialNotification, fgsType)
        }.onFailure {
            stopSelf()
            return START_NOT_STICKY
        }

        startWakefulLiveLoop()
        return START_STICKY
    }

    private fun startWakefulLiveLoop() {
        if (tickerJob?.isActive == true) {
            refreshNotificationDisplay()
            return
        }
        acquireSafeWakeLock()
        val startedAt = System.currentTimeMillis()

        tickerJob = serviceScope.launch {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DailySyncWorkerEntryPoint::class.java
            )
            val offlineRepo = entryPoint.offlineFirstRepository()
            val mockRepo = entryPoint.mockRepository()
            val prefs = entryPoint.preferencesDataStore()
            val notifManager = entryPoint.notificationManager()

            if (mockRepo.games.value.isEmpty()) {
                offlineRepo.loadBundledLiveSnapshotFromAssets(applicationContext)
                offlineRepo.restoreFromRoomCacheIfAvailable()
            }

            while (isActive) {
                val now = System.currentTimeMillis()
                if (now - startedAt > MAX_TRACKER_DURATION_MS) {
                    shutdownTracker()
                    break
                }
                if (!prefs.getSavedLockScreenTrackerImmediate() && !isPreviewMode) {
                    shutdownTracker()
                    break
                }

                offlineRepo.refreshLiveMatchdayTick()

                val primaryId = prefs.getSavedPrimaryTeamImmediate() ?: "MUN"
                val favIds = (prefs.getSavedFavoriteTeamsImmediate() ?: setOf(primaryId)) + primaryId
                val perGame = prefs.getSavedGameRemindersImmediate()
                val lang = prefs.getSavedLanguageImmediate() ?: notifManager.resolveActiveLanguage()
                val allGames = mockRepo.games.value

                val activeTracked = selectActiveTrackedGames(allGames, favIds, perGame, now)
                activeTracked.firstOrNull { it.status == GameStatus.LIVE }?.let { liveFocus ->
                    offlineRepo.refreshSelectedGameLiveStats(liveFocus)
                }

                notifManager.evaluateAutomaticGameNightAlerts(
                    games = mockRepo.games.value,
                    favoriteTeamIds = favIds,
                    tipOffEnabled = prefs.getSavedTipOffAlarmImmediate(),
                    halftimeFinalEnabled = prefs.getSavedHalftimeFinalImmediate(),
                    closeGameEnabled = prefs.getSavedCloseGameAlertImmediate(),
                    language = lang,
                    perGameReminders = perGame
                )

                CourtsideAppWidgetProvider.updateAllWidgets(applicationContext)
                refreshNotificationDisplay()

                if (activeTracked.isEmpty() && !isPreviewMode) {
                    shutdownTracker()
                    break
                }
                delay(8_000L)
            }
        }
    }

    private fun refreshNotificationDisplay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notification = buildOngoingTrackerNotification()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_LIVE_TRACKER, notification)
    }

    private fun buildOngoingTrackerNotification(): Notification {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DailySyncWorkerEntryPoint::class.java
        )
        val mockRepo = entryPoint.mockRepository()
        val prefs = entryPoint.preferencesDataStore()
        val lang = prefs.getSavedLanguageImmediate() ?: AppLanguage.DE
        val strings = CourtsideTranslations.forLanguage(lang)
        val primaryId = prefs.getSavedPrimaryTeamImmediate() ?: "MUN"
        val favIds = (prefs.getSavedFavoriteTeamsImmediate() ?: setOf(primaryId)) + primaryId
        val perGame = prefs.getSavedGameRemindersImmediate()
        val games = mockRepo.games.value

        val tracked = selectActiveTrackedGames(games, favIds, perGame)
        val fallback = focusGameId?.let { id -> games.find { it.id == id } }
            ?: games.firstOrNull { it.status == GameStatus.LIVE }
            ?: games.firstOrNull {
                EuroLeagueMockRepository.canonicalClubId(it.homeTeam.id) == EuroLeagueMockRepository.canonicalClubId(primaryId) ||
                    EuroLeagueMockRepository.canonicalClubId(it.awayTeam.id) == EuroLeagueMockRepository.canonicalClubId(primaryId)
            }
            ?: games.firstOrNull()

        val payload = formatLockScreenScorePayload(tracked, fallback, perGame, lang)
        val targetGame = tracked.firstOrNull() ?: fallback

        val openIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetGame != null) {
                putExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_GAME_ID, targetGame.id)
                putExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_ROUND, targetGame.round)
            }
        }
        val contentPending = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID_LIVE_TRACKER,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CourtsideLiveTrackerService::class.java).apply {
            action = ACTION_STOP_TRACKER
        }
        val stopPending = PendingIntent.getService(
            this,
            NOTIFICATION_ID_LIVE_TRACKER + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_LIVE_TRACKER)
            .setSmallIcon(R.drawable.ic_stat_courtside_ball)
            .setColor(0xFFFF6600.toInt())
            .setContentTitle(payload.title)
            .setContentText(payload.shortText)
            .setSubText("EuroLeague Courtside")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(payload.title)
                    .bigText(payload.bigText)
                    .setSummaryText(payload.summaryText)
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPending)
            .addAction(0, "✕ ${strings.stopLiveTrackerLabel}", stopPending)
            .build()
    }

    private fun ensureTrackerChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_LIVE_TRACKER,
            "Courtside Live Lock-Screen Scoreboard",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Silent ongoing lock-screen scoreboard during tracked live EuroLeague games"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun acquireSafeWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) return
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EuroLeagueCourtside::LiveGameTracker").apply {
                setReferenceCounted(false)
                acquire(MAX_TRACKER_DURATION_MS)
            }
        }
    }

    private fun shutdownTracker() {
        tickerJob?.cancel()
        tickerJob = null
        isPreviewMode = false
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        super.onDestroy()
    }
}
