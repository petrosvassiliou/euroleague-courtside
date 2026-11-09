package com.euroleague.courtside

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Plane periodische Hintergrund-Synchronisation (alle 6 Std. via WorkManager) für den täglichen Einsatz
        notificationManager.scheduleDailyLiveSync()

        setContent {
            val selectedTeamId by repository.selectedTeamId.collectAsState()
            val teams by repository.teamsFlow.collectAsState()
            val activeTeam = repository.findTeam(selectedTeamId) ?: teams.first()

            LaunchedEffect(Unit) {
                preferencesDataStore.userPreferencesFlow.collect { prefs ->
                    repository.selectLanguage(prefs.language)
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
                offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                var tickCount = 0
                while (true) {
                    delay(45_000L)
                    tickCount++
                    if (tickCount % 20 == 0) {
                        offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                    } else {
                        offlineFirstRepository.refreshLiveMatchdayTick()
                    }
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
}
