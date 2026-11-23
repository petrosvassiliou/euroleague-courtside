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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

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
            val activeTeam = repository.findTeam(selectedTeamId) ?: teams.first()

            LaunchedEffect(Unit) {
                preferencesDataStore.userPreferencesFlow.collect { prefs ->
                    repository.restoreSavedTeamSelection(prefs.primaryTeamId, prefs.favoriteTeamIds)
                    repository.selectLanguage(prefs.language)
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

            // Sofortiges Laden des gebündelten Live-Snapshots + vollständiger Live-API-Sync beim Start
            // + schonender 45s-Live-Ticker (verhindert HTTP 429) & vollständiger Kader/Statistik-Sync alle 15 Min.
            LaunchedEffect(Unit) {
                offlineFirstRepository.loadBundledLiveSnapshotFromAssets(applicationContext)
                offlineFirstRepository.refreshLiveMatchdayTick()
                offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                evaluateAlertsAndSyncWidgets()
                var tickCount = 0
                while (true) {
                    delay(45_000L)
                    tickCount++
                    if (tickCount % 20 == 0) {
                        offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                    } else {
                        offlineFirstRepository.refreshLiveMatchdayTick()
                    }
                    evaluateAlertsAndSyncWidgets()
                }
            }

            EuroLeagueCourtsideTheme(activeTeam = activeTeam) {
                MainAppScaffold(
                    repository = repository,
                    notificationManager = notificationManager,
                    offlineFirstRepository = offlineFirstRepository,
                    preferencesDataStore = preferencesDataStore
                )
            }
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
            language = lang
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
