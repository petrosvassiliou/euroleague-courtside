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
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.euroleague.courtside.data.repository.EuroLeagueGame
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet die dedizierten EuroLeague-Notification-Channels sowie WorkManager-Jobs für:
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

        private const val NOTIFICATION_ID_TIP_OFF = 4101
        private const val NOTIFICATION_ID_SCORE = 4102
        private const val NOTIFICATION_ID_CLOSE_GAME = 4103
        private const val NOTIFICATION_ID_TEST_PUSH = 4199
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tipOffChannel = NotificationChannel(
            CHANNEL_TIP_OFF,
            "Tip-Off-Alarm (15 Min. vorher)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Erinnert 15 Minuten vor Spielbeginn deines Lieblingsteams inkl. Arena & H2H."
            enableVibration(true)
        }

        val scoresChannel = NotificationChannel(
            CHANNEL_SCORES,
            "Halbzeit- & Endergebnisse",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Live-Zwischenstände zur Halbzeit sowie offizielle Endergebnisse mit Top-PIR."
        }

        val closeGameChannel = NotificationChannel(
            CHANNEL_CLOSE_GAME,
            "Crunch-Time Close-Game-Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sofort-Alarm, wenn ein Spiel im 4. Viertel (< 3:00 Min.) <= 5 Punkte Differenz hat."
            enableVibration(true)
        }

        manager.createNotificationChannels(
            listOf(tipOffChannel, scoresChannel, closeGameChannel)
        )
    }

    /**
     * Plant einen WorkManager-Job exakt 15 Minuten vor Tip-Off des ausgewählten Spiels.
     */
    fun scheduleTipOffAlarm(game: EuroLeagueGame) {
        val triggerAtMillis = game.tipOffEpochMillis - TimeUnit.MINUTES.toMillis(15)
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)

        val data = workDataOf(
            "GAME_ID" to game.id,
            "MATCHUP_TITLE" to "${game.homeTeam.name} vs. ${game.awayTeam.name}",
            "ARENA" to "${game.homeTeam.arena} (${game.homeTeam.city})",
            "ROUND" to game.round
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
     * Feuert automatisch einen Close-Game-Alert, falls das Spiel im 4. Viertel unter 3 Minuten
     * Spielzeit <= 5 Punkte Differenz aufweist.
     */
    fun evaluateAndSendCloseGameAlert(game: EuroLeagueGame) {
        if (!game.isCloseGameFourthQuarter) return

        val diff = kotlin.math.abs(game.homeScore - game.awayScore)
        val title = "🔥 CRUNCH-TIME ALERT (Q${game.currentQuarter} • ${game.remainingClock})"
        val summary = "${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code} (Nur $diff Pkt. Differenz!)"
        val bigText = buildString {
            appendLine("$summary in der ${game.homeTeam.arena}!")
            appendLine("Viertelstände: Q1 ${game.quarters.q1Home}:${game.quarters.q1Away} | Q2 ${game.quarters.q2Home}:${game.quarters.q2Away} | Q3 ${game.quarters.q3Home}:${game.quarters.q3Away}")
            val topPlayer = (game.boxscoreHome + game.boxscoreAway).maxByOrNull { it.pir }
            if (topPlayer != null) {
                append("MVP-Watch: ${topPlayer.playerName} (${topPlayer.points} PTS, PIR ${topPlayer.pir})")
            }
        }

        dispatchNotification(
            channelId = CHANNEL_CLOSE_GAME,
            notificationId = NOTIFICATION_ID_CLOSE_GAME,
            title = title,
            shortText = summary,
            bigText = bigText,
            priority = NotificationCompat.PRIORITY_HIGH
        )
    }

    /**
     * Sendet eine Halbzeit- oder Endergebnis-Benachrichtigung für das Lieblingsteam.
     */
    fun sendHalftimeOrFinalNotification(game: EuroLeagueGame, isFinal: Boolean) {
        val statusLabel = if (isFinal) "ENDERGEBNIS" else "HALBZEIT-STAND"
        val title = "🏀 $statusLabel • Round ${game.round}"
        val shortText = "${game.homeTeam.name} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.name}"
        val topHome = game.boxscoreHome.maxByOrNull { it.pir }
        val topAway = game.boxscoreAway.maxByOrNull { it.pir }

        val bigText = buildString {
            appendLine(shortText)
            if (topHome != null) {
                appendLine("${game.homeTeam.code} Top-Performer: ${topHome.playerName} (${topHome.points} PTS, ${topHome.totalRebounds} REB, PIR ${topHome.pir})")
            }
            if (topAway != null) {
                append("${game.awayTeam.code} Top-Performer: ${topAway.playerName} (${topAway.points} PTS, ${topAway.assists} AST, PIR ${topAway.pir})")
            }
        }

        dispatchNotification(
            channelId = CHANNEL_SCORES,
            notificationId = NOTIFICATION_ID_SCORE,
            title = title,
            shortText = shortText,
            bigText = bigText,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    /**
     * Sofortiger Test-Push-Button im Einstellungs-Screen: Feuert eine echte native System-Notification
     * mit `BigTextStyle` und Live-Score-Aufschlüsselung auf Googlebook OS und Android.
     */
    fun sendInstantTestPushNotification(game: EuroLeagueGame) {
        val title = "🚨 LIVE CRUNCH-TIME • ${game.homeTeam.code} ${game.homeScore} : ${game.awayScore} ${game.awayTeam.code}"
        val shortText = "Q${game.currentQuarter} (${game.remainingClock}) – ${game.homeTeam.arena}"
        val topPlayer = (game.boxscoreHome + game.boxscoreAway).maxByOrNull { it.pir }

        val expandedBody = buildString {
            appendLine("${game.homeTeam.name} (${game.homeScore}) vs. ${game.awayTeam.name} (${game.awayScore})")
            appendLine("Status: Q${game.currentQuarter} | Restzeit ${game.remainingClock} | Nur ${kotlin.math.abs(game.homeScore - game.awayScore)} Punkte Differenz!")
            appendLine(
                "Quarters: Q1 ${game.quarters.q1Home}-${game.quarters.q1Away} • " +
                        "Q2 ${game.quarters.q2Home}-${game.quarters.q2Away} • " +
                        "Q3 ${game.quarters.q3Home}-${game.quarters.q3Away} • " +
                        "Q4 ${game.quarters.q4Home}-${game.quarters.q4Away}"
            )
            if (topPlayer != null) {
                append("Top-PIR auf dem Parkett: ${topPlayer.playerName} mit ${topPlayer.points} PTS & PIR ${topPlayer.pir}")
            }
        }

        dispatchNotification(
            channelId = CHANNEL_CLOSE_GAME,
            notificationId = NOTIFICATION_ID_TEST_PUSH,
            title = title,
            shortText = shortText,
            bigText = expandedBody,
            priority = NotificationCompat.PRIORITY_MAX
        )
    }

    internal fun dispatchNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        shortText: String,
        bigText: String,
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
                    .setSummaryText("Live Match Center")
            )
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

/**
 * WorkManager-Worker für geplante Tip-Off-Alarme (15 Minuten vor Spielbeginn).
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
        helper.dispatchNotification(
            channelId = EuroLeagueNotificationManager.CHANNEL_TIP_OFF,
            notificationId = 4101,
            title = "⏰ Tip-Off in 15 Minuten! (Round $round)",
            shortText = "$matchup • $arena",
            bigText = "Mach dich bereit für Round $round: $matchup live aus der $arena. Öffne EuroLeague Courtside für Live-Boxscore & Shot-Chart!",
            priority = NotificationCompat.PRIORITY_HIGH
        )
        return Result.success()
    }
}
