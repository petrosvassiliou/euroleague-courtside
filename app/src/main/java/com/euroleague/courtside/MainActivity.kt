package com.euroleague.courtside

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.notifications.EuroLeagueNotificationManager
import com.euroleague.courtside.ui.navigation.MainAppScaffold
import com.euroleague.courtside.ui.theme.AppThemeMode
import com.euroleague.courtside.ui.theme.EuroLeagueCourtsideTheme
import com.euroleague.courtside.widget.CourtsideAppWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltAndroidApp
class EuroLeagueApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: EuroLeagueMockRepository

    @Inject
    lateinit var offlineFirstRepository: EuroLeagueOfflineFirstRepository

    @Inject
    lateinit var notificationManager: EuroLeagueNotificationManager

    @Inject
    lateinit var preferencesDataStore: com.euroleague.courtside.data.local.CourtsidePreferencesDataStore

    private var shouldResetToMyTeamOnNextStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialThemeMode = preferencesDataStore.getSavedThemeModeImmediate()
        repository.selectThemeMode(initialThemeMode)
        applySystemBarAppearance(initialThemeMode)

        // Beim regulären Start immer "My Team" anzeigen; bei Klick auf eine Spiel-Notification direkt zum Spiel springen
        if (!consumeDeepLinkIntentIfPresent(intent)) {
            repository.resetToStartDestination()
        }
        shouldResetToMyTeamOnNextStart = false

        // Stelle gespeichertes "My Team" & Sprache sofort synchron vor dem ersten UI-Frame wieder her
        preferencesDataStore.getSavedPrimaryTeamImmediate()?.let { savedTeamId ->
            repository.restoreSavedTeamSelection(
                primaryTeamId = savedTeamId,
                favoriteIds = preferencesDataStore.getSavedFavoriteTeamsImmediate().orEmpty()
            )
        }
        preferencesDataStore.getSavedLanguageImmediate()?.let { savedLang ->
            repository.selectLanguage(savedLang)
            notificationManager.setLanguage(savedLang)
        }

        // Plane periodische Hintergrund-Synchronisation (alle 6 Std. via WorkManager) für den täglichen Einsatz
        notificationManager.scheduleDailyLiveSync()

        setContent {
            val selectedTeamId by repository.selectedTeamId.collectAsState()
            val teams by repository.teamsFlow.collectAsState()
            val activeThemeMode by repository.themeModeFlow.collectAsState()
            val activeTeam = repository.findTeam(selectedTeamId) ?: teams.first()

            LaunchedEffect(activeThemeMode) {
                applySystemBarAppearance(activeThemeMode)
            }

            LaunchedEffect(Unit) {
                preferencesDataStore.userPreferencesFlow.collect { prefs ->
                    repository.restoreSavedTeamSelection(prefs.primaryTeamId, prefs.favoriteTeamIds)
                    repository.selectLanguage(prefs.language)
                    repository.selectThemeMode(prefs.themeMode)
                    notificationManager.setLanguage(prefs.language)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {}
                )
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Sofortiges Laden des gebündelten Live-Snapshots + schneller Live-Header-Puls (<200ms) beim Start
            // + adaptiver Echtzeit-Ticker (8s während laufender Live-Spiele, 30s im Ruhemodus) & Voll-Sync alle 15 Min.
            LaunchedEffect(Unit) {
                offlineFirstRepository.loadBundledLiveSnapshotFromAssets(applicationContext)
                offlineFirstRepository.refreshLiveMatchdayTick(forceScheduleSync = true)
                evaluateAlertsAndSyncWidgets()
                offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                evaluateAlertsAndSyncWidgets()
                var elapsedMs = 0L
                while (true) {
                    val nowMs = System.currentTimeMillis()
                    val hasActiveLiveWindow = repository.games.value.any { g ->
                        g.status == com.euroleague.courtside.data.repository.GameStatus.LIVE ||
                            (g.status == com.euroleague.courtside.data.repository.GameStatus.UPCOMING &&
                                g.tipOffEpochMillis in (nowMs - 4 * 3_600_000L)..(nowMs + 20 * 60_000L))
                    }
                    val pollIntervalMs = if (hasActiveLiveWindow) 8_000L else 30_000L
                    delay(pollIntervalMs)
                    elapsedMs += pollIntervalMs
                    if (elapsedMs >= 15 * 60_000L) {
                        elapsedMs = 0L
                        offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                    } else {
                        offlineFirstRepository.refreshLiveMatchdayTick()
                    }
                    evaluateAlertsAndSyncWidgets()
                }
            }

            EuroLeagueCourtsideTheme(
                activeTeam = activeTeam,
                themeMode = activeThemeMode
            ) {
                MainAppScaffold(
                    repository = repository,
                    notificationManager = notificationManager,
                    offlineFirstRepository = offlineFirstRepository,
                    preferencesDataStore = preferencesDataStore
                )
            }
        }
    }

    private fun applySystemBarAppearance(mode: AppThemeMode) {
        if (mode == AppThemeMode.LIGHT) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            )
        } else {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
        }
    }

    private fun evaluateAlertsAndSyncWidgets() {
        val primaryId = preferencesDataStore.getSavedPrimaryTeamImmediate() ?: repository.selectedTeamId.value
        val favIds = (preferencesDataStore.getSavedFavoriteTeamsImmediate() ?: repository.favoriteTeamIds.value) + primaryId
        val lang = preferencesDataStore.getSavedLanguageImmediate() ?: repository.appLanguageFlow.value
        notificationManager.evaluateAutomaticGameNightAlerts(
            games = repository.games.value,
            favoriteTeamIds = favIds,
            tipOffEnabled = preferencesDataStore.getSavedTipOffAlarmImmediate(),
            halftimeFinalEnabled = preferencesDataStore.getSavedHalftimeFinalImmediate(),
            closeGameEnabled = preferencesDataStore.getSavedCloseGameAlertImmediate(),
            language = lang,
            perGameReminders = preferencesDataStore.getSavedGameRemindersImmediate()
        )
        CourtsideAppWidgetProvider.updateAllWidgets(applicationContext)
    }

    private fun consumeDeepLinkIntentIfPresent(incomingIntent: Intent?): Boolean {
        val gameId = incomingIntent?.getStringExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_GAME_ID)
            ?.takeIf { it.isNotBlank() }
            ?: return false
        val round = incomingIntent.getIntExtra(
            EuroLeagueNotificationManager.EXTRA_DEEP_LINK_ROUND,
            repository.activeRoundFlow.value
        )
        incomingIntent.removeExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_GAME_ID)
        incomingIntent.removeExtra(EuroLeagueNotificationManager.EXTRA_DEEP_LINK_ROUND)
        repository.openGameDeepLink(gameId, round)
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::repository.isInitialized) {
            if (!consumeDeepLinkIntentIfPresent(intent)) {
                repository.resetToStartDestination()
            }
        }
        shouldResetToMyTeamOnNextStart = false
    }

    override fun onStart() {
        super.onStart()
        if (shouldResetToMyTeamOnNextStart) {
            shouldResetToMyTeamOnNextStart = false
            if (::repository.isInitialized) {
                repository.resetToStartDestination()
            }
        }
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            shouldResetToMyTeamOnNextStart = true
        }
        super.onStop()
    }
}
