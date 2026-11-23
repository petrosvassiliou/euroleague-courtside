package com.euroleague.courtside.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.euroleague.courtside.MainActivity
import com.euroleague.courtside.R
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.i18n.AppLanguage
import com.euroleague.courtside.i18n.CourtsideTranslations
import com.euroleague.courtside.i18n.formatTipOffCountdown
import com.euroleague.courtside.notifications.DailyEuroLeagueSyncWorker
import com.euroleague.courtside.notifications.DailySyncWorkerEntryPoint
import com.euroleague.courtside.notifications.EuroLeagueNotificationManager
import dagger.hilt.android.EntryPointAccessors

data class WidgetMatchupState(
    val gameId: String,
    val round: Int,
    val isLive: Boolean,
    val isFinal: Boolean,
    val badgeText: String,
    val homeTeam: EuroLeagueTeam,
    val awayTeam: EuroLeagueTeam,
    val centerPrimaryText: String,
    val centerSecondaryText: String,
    val footerArenaText: String
)

/**
 * Natives Android Startbildschirm-Widget im Obsidian/Orange Courtside Broadcast-Design:
 * Zeigt das nächste Spiel des gewählten "My Team" inkl. Countdown oder – an Spieltagen –
 * das Echtzeit-Live-Scoreboard mit Vereinswappen, Viertel/Uhrzeit und Deep-Link ins Match-Center.
 */
class CourtsideAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            renderWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val req = OneTimeWorkRequestBuilder<DailyEuroLeagueSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(req)
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.euroleague.courtside.ACTION_REFRESH_WIDGET"

        fun buildWidgetMatchupState(
            primaryTeam: EuroLeagueTeam,
            games: List<EuroLeagueGame>,
            activeRound: Int,
            language: AppLanguage,
            nowMillis: Long = System.currentTimeMillis()
        ): WidgetMatchupState {
            val strings = CourtsideTranslations.forLanguage(language)
            val primaryCanon = EuroLeagueMockRepository.canonicalClubId(primaryTeam.id)
            val teamGames = games.filter { g ->
                EuroLeagueMockRepository.canonicalClubId(g.homeTeam.id) == primaryCanon ||
                    EuroLeagueMockRepository.canonicalClubId(g.awayTeam.id) == primaryCanon
            }.sortedWith(
                compareBy<EuroLeagueGame> { it.round }
                    .thenBy { it.tipOffEpochMillis }
                    .thenBy { it.gameCode }
            )

            val targetGame = teamGames.firstOrNull { it.status == GameStatus.LIVE }
                ?: teamGames.firstOrNull { it.round >= activeRound && it.status == GameStatus.UPCOMING }
                ?: teamGames.firstOrNull { it.round == activeRound }
                ?: teamGames.firstOrNull { it.status == GameStatus.UPCOMING }
                ?: teamGames.lastOrNull()
                ?: games.firstOrNull { it.status == GameStatus.LIVE }
                ?: games.first()

            val isLive = targetGame.status == GameStatus.LIVE
            val isFinal = targetGame.status == GameStatus.FINAL
            val qTag = if (targetGame.currentQuarter >= 5) "OT" else "Q${targetGame.currentQuarter}"

            val badge = when {
                isLive -> "● ${strings.statusLive} • $qTag ${targetGame.remainingClock}"
                isFinal -> strings.statusFinal
                else -> strings.nextTipOffBadge
            }

            val centerPrimary = when {
                isLive || isFinal -> "${targetGame.homeScore} : ${targetGame.awayScore}"
                else -> {
                    val rem = (targetGame.tipOffEpochMillis - nowMillis).coerceAtLeast(0L)
                    strings.formatTipOffCountdown(rem)
                }
            }

            val centerSecondary = "${strings.roundPrefix} ${targetGame.round}"
            val footerArena = "${targetGame.homeTeam.arena} • ${targetGame.homeTeam.city}"

            return WidgetMatchupState(
                gameId = targetGame.id,
                round = targetGame.round,
                isLive = isLive,
                isFinal = isFinal,
                badgeText = badge,
                homeTeam = targetGame.homeTeam,
                awayTeam = targetGame.awayTeam,
                centerPrimaryText = centerPrimary,
                centerSecondaryText = centerSecondary,
                footerArenaText = footerArena
            )
        }

        fun updateAllWidgets(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, CourtsideAppWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    ids.forEach { id -> renderWidget(context, manager, id) }
                }
            }
        }

        fun requestPinCourtsideWidget(context: Context): Boolean {
            return runCatching {
                val manager = AppWidgetManager.getInstance(context)
                if (!manager.isRequestPinAppWidgetSupported) return false
                val provider = ComponentName(context, CourtsideAppWidgetProvider::class.java)
                manager.requestPinAppWidget(provider, null, null)
            }.getOrDefault(false)
        }

        private fun renderWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            runCatching {
                val appCtx = context.applicationContext
                val entryPoint = EntryPointAccessors.fromApplication(
                    appCtx,
                    DailySyncWorkerEntryPoint::class.java
                )
                val mockRepo = entryPoint.mockRepository()
                val prefs = entryPoint.preferencesDataStore()

                // Falls Prozess kalt gestartet wurde, Snapshot synchron laden
                if (mockRepo.games.value.size < 20) {
                    runCatching {
                        val raw = appCtx.assets.open("euroleague_live_snapshot.json")
                            .bufferedReader()
                            .use { it.readText() }
                        entryPoint.offlineFirstRepository().applySnapshotJsonString(raw, isLiveNetwork = false)
                    }
                }

                val savedTeamId = prefs.getSavedPrimaryTeamImmediate() ?: mockRepo.selectedTeamId.value
                val language = prefs.getSavedLanguageImmediate() ?: mockRepo.appLanguageFlow.value
                val primaryTeam = mockRepo.findTeam(savedTeamId) ?: mockRepo.teams.first()

                val state = buildWidgetMatchupState(
                    primaryTeam = primaryTeam,
                    games = mockRepo.games.value,
                    activeRound = mockRepo.activeRoundFlow.value,
                    language = language
                )

                val views = RemoteViews(context.packageName, R.layout.widget_courtside_hub)
                views.setTextViewText(R.id.widget_brand_title, "${primaryTeam.name.uppercase()}")
                views.setTextViewText(R.id.widget_status_badge, state.badgeText)
                views.setInt(
                    R.id.widget_status_badge,
                    "setBackgroundResource",
                    if (state.isLive) R.drawable.widget_badge_live else R.drawable.widget_badge_orange
                )
                views.setTextColor(
                    R.id.widget_status_badge,
                    if (state.isLive) 0xFFFF3B5C.toInt() else 0xFFFF6600.toInt()
                )

                views.setImageViewResource(R.id.widget_home_logo, resolveClubDrawable(context, state.homeTeam))
                views.setImageViewResource(R.id.widget_away_logo, resolveClubDrawable(context, state.awayTeam))
                views.setTextViewText(R.id.widget_home_code, state.homeTeam.code)
                views.setTextViewText(R.id.widget_home_record, "${state.homeTeam.wins}-${state.homeTeam.losses}")
                views.setTextViewText(R.id.widget_away_code, state.awayTeam.code)
                views.setTextViewText(R.id.widget_away_record, "${state.awayTeam.wins}-${state.awayTeam.losses}")

                views.setTextViewText(R.id.widget_center_primary, state.centerPrimaryText)
                views.setTextViewText(R.id.widget_center_secondary, state.centerSecondaryText)
                views.setTextViewText(R.id.widget_footer_arena, state.footerArenaText)

                // Klick auf das Widget öffnet bei Live-Spielen direkt das Live-Spiel im Match-Center, sonst My Team
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (state.isLive) {
                        putExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_GAME_ID, state.gameId)
                        putExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_ROUND, state.round)
                    }
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 8000,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

                val refreshIntent = Intent(context, CourtsideAppWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                val refreshPending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 9000,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun resolveClubDrawable(context: Context, team: EuroLeagueTeam): Int {
            val candidates = listOf(
                team.localLogoResName,
                "club_logo_${team.id.lowercase()}",
                "club_logo_${team.code.lowercase()}"
            ).filter { it.isNotBlank() }.distinct()
            for (name in candidates) {
                val id = context.resources.getIdentifier(name, "drawable", context.packageName)
                if (id != 0) return id
            }
            return R.drawable.club_logo_mun
        }
    }
}
