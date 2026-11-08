package com.euroleague.courtside.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.euroleague.courtside.data.local.CourtsidePreferencesDataStore
import com.euroleague.courtside.data.local.UserPreferencesState
import com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.notifications.EuroLeagueNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourtsideUiState(
    val primaryTeam: EuroLeagueTeam,
    val favoriteTeamIds: Set<String>,
    val allTeams: List<EuroLeagueTeam>,
    val games: List<EuroLeagueGame>,
    val selectedRound: Int = 1,
    val filterOnlyFavorites: Boolean = false,
    val tipOffAlarmEnabled: Boolean = true,
    val halftimeFinalScoreEnabled: Boolean = true,
    val closeGameAlertEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val syncStatus: String = "LIVE API • E2026"
)

sealed interface CourtsideUiEvent {
    data class SelectPrimaryTeam(val teamId: String) : CourtsideUiEvent
    data class ToggleFavoriteTeam(val teamId: String) : CourtsideUiEvent
    data class ChangeRound(val round: Int) : CourtsideUiEvent
    data object ToggleFavoriteFilter : CourtsideUiEvent
    data object TriggerLiveRefresh : CourtsideUiEvent
    data class UpdateNotificationPrefs(
        val tipOff: Boolean,
        val halftimeFinal: Boolean,
        val closeGame: Boolean
    ) : CourtsideUiEvent
}

/**
 * Unidirectional Data Flow (UDF) ViewModel für EuroLeague Courtside.
 * Kombiniert Jetpack DataStore Preferences, Live-API / Offline-First Room Sync,
 * automatische Kader-Verifikation (`/people`) und Crunch-Time-Alarme.
 */
@HiltViewModel
class CourtsideViewModel @Inject constructor(
    private val mockRepository: EuroLeagueMockRepository,
    private val offlineFirstRepository: EuroLeagueOfflineFirstRepository,
    private val preferencesDataStore: CourtsidePreferencesDataStore,
    private val notificationManager: EuroLeagueNotificationManager
) : ViewModel() {

    private val _selectedRound = MutableStateFlow(mockRepository.activeRoundFlow.value)
    private val _filterOnlyFavorites = MutableStateFlow(false)
    private val _isSyncing = MutableStateFlow(false)

    val uiState: StateFlow<CourtsideUiState> = combine(
        preferencesDataStore.userPreferencesFlow,
        mockRepository.games,
        _selectedRound,
        _filterOnlyFavorites,
        _isSyncing
    ) { prefs: UserPreferencesState, games: List<EuroLeagueGame>, round: Int, filterFav: Boolean, syncing: Boolean ->
        val teams = mockRepository.teamsFlow.value
        val primary = mockRepository.findTeam(prefs.primaryTeamId) ?: teams.first()

        CourtsideUiState(
            primaryTeam = primary,
            favoriteTeamIds = prefs.favoriteTeamIds,
            allTeams = teams,
            games = games,
            selectedRound = round,
            filterOnlyFavorites = filterFav,
            tipOffAlarmEnabled = prefs.tipOffAlarmEnabled,
            halftimeFinalScoreEnabled = prefs.halftimeFinalScoreEnabled,
            closeGameAlertEnabled = prefs.closeGameAlertEnabled,
            isSyncing = syncing,
            syncStatus = mockRepository.lastSyncStatusFlow.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CourtsideUiState(
            primaryTeam = mockRepository.findTeam("BAY") ?: mockRepository.teams.first(),
            favoriteTeamIds = setOf("BAY", "PAO"),
            allTeams = mockRepository.teams,
            games = mockRepository.games.value,
            selectedRound = mockRepository.activeRoundFlow.value,
            syncStatus = mockRepository.lastSyncStatusFlow.value
        )
    )

    init {
        syncRemoteWithLocalRoomCache()
        startLivePollingLoop()
        viewModelScope.launch {
            mockRepository.activeRoundFlow.collect { activeRound ->
                _selectedRound.value = activeRound
            }
        }
    }

    fun onEvent(event: CourtsideUiEvent) {
        when (event) {
            is CourtsideUiEvent.SelectPrimaryTeam -> viewModelScope.launch {
                mockRepository.selectPrimaryTeam(event.teamId)
                preferencesDataStore.setPrimaryTeam(event.teamId)
                offlineFirstRepository.verifyAndSyncClubRoster(event.teamId)
            }
            is CourtsideUiEvent.ToggleFavoriteTeam -> viewModelScope.launch {
                mockRepository.toggleFavoriteTeam(event.teamId)
                preferencesDataStore.toggleFavoriteTeam(event.teamId)
            }
            is CourtsideUiEvent.ChangeRound -> {
                _selectedRound.value = event.round.coerceIn(1, 38)
            }
            CourtsideUiEvent.ToggleFavoriteFilter -> {
                _filterOnlyFavorites.value = !_filterOnlyFavorites.value
            }
            CourtsideUiEvent.TriggerLiveRefresh -> {
                viewModelScope.launch {
                    _isSyncing.value = true
                    val res = offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
                    if (res.isFailure) {
                        mockRepository.simulateLiveRefresh()
                    }
                    _isSyncing.value = false
                    if (uiState.value.closeGameAlertEnabled) {
                        mockRepository.games.value
                            .filter { it.isCloseGameFourthQuarter }
                            .forEach { notificationManager.evaluateAndSendCloseGameAlert(it) }
                    }
                }
            }
            is CourtsideUiEvent.UpdateNotificationPrefs -> viewModelScope.launch {
                preferencesDataStore.updateNotificationChannels(
                    tipOff = event.tipOff,
                    halftimeFinal = event.halftimeFinal,
                    closeGame = event.closeGame
                )
            }
        }
    }

    private fun syncRemoteWithLocalRoomCache() {
        viewModelScope.launch {
            _isSyncing.value = true
            offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
            _isSyncing.value = false
        }
    }

    private fun startLivePollingLoop() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                offlineFirstRepository.syncSeasonDataFromRemoteOrSeedCache()
            }
        }
    }
}
