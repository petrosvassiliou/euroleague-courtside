package com.euroleague.courtside.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.euroleague.courtside.BuildConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.compose.runtime.CompositionLocalProvider
import com.euroleague.courtside.data.local.CourtsidePreferencesDataStore
import com.euroleague.courtside.update.AppUpdateInfo
import com.euroleague.courtside.update.CourtsideAppUpdateManager
import com.euroleague.courtside.update.UpdateStatus
import com.euroleague.courtside.data.remote.EuroLeagueOfflineFirstRepository
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.data.repository.InjuryStatus
import com.euroleague.courtside.data.repository.PlayerProfile
import com.euroleague.courtside.data.repository.PlayoffSeries
import com.euroleague.courtside.i18n.AppLanguage
import com.euroleague.courtside.i18n.CourtsideStrings
import com.euroleague.courtside.i18n.CourtsideTranslations
import com.euroleague.courtside.i18n.LocalAppLanguage
import com.euroleague.courtside.i18n.LocalCourtsideStrings
import com.euroleague.courtside.i18n.formatTipOffCountdown
import com.euroleague.courtside.i18n.localizedLabel
import com.euroleague.courtside.notifications.EuroLeagueNotificationManager
import com.euroleague.courtside.notifications.GameReminderType
import com.euroleague.courtside.ui.screens.GameRemindersDialog
import com.euroleague.courtside.ui.screens.MatchCenterScreen
import com.euroleague.courtside.ui.screens.MyTeamDashboardScreen
import com.euroleague.courtside.ui.screens.PlayerProfileDialog
import com.euroleague.courtside.ui.screens.SeasonLeadersAndComparisonScreen
import com.euroleague.courtside.ui.screens.SettingsAndNotificationTestScreen
import com.euroleague.courtside.ui.screens.StandingsScreen
import com.euroleague.courtside.ui.screens.StandingsTeamDetailScreen
import com.euroleague.courtside.ui.screens.resolveClickedPlayerProfile
import com.euroleague.courtside.widget.CourtsideAppWidgetProvider
import com.euroleague.courtside.ui.theme.AppThemeMode
import com.euroleague.courtside.ui.theme.CourtsideClubCrest
import com.euroleague.courtside.ui.theme.CourtsideDesignTokens
import com.euroleague.courtside.ui.theme.CourtsideDualToneBanner
import com.euroleague.courtside.ui.theme.CourtsideGlassCard
import com.euroleague.courtside.ui.theme.CourtsideLivePill
import com.euroleague.courtside.ui.theme.CourtsideShortcutPill
import com.euroleague.courtside.util.CalendarExportHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

internal fun shouldUseSingleColumnMobileLayout(widthClass: WindowWidthSizeClass): Boolean =
    widthClass == WindowWidthSizeClass.COMPACT

internal fun isAndroidPhoneViewport(isCompactWidth: Boolean, smallestScreenWidthDp: Int): Boolean =
    isCompactWidth || smallestScreenWidthDp < 600

enum class CourtsideDestination(
    val title: String,
    val shortcutHint: String
) {
    MY_TEAM("Mein Team", "Ctrl+1"),
    MATCHDAY("EuroLeague", "Ctrl+2"),
    STANDINGS_PLAYOFFS("Standings", "Ctrl+3"),
    STATISTICS("Analytics", "Ctrl+4"),
    SETTINGS("Settings", "Ctrl+5");

    val icon: ImageVector
        get() = when (this) {
            MY_TEAM -> Icons.Default.Home
            MATCHDAY -> Icons.Default.SportsBasketball
            STANDINGS_PLAYOFFS -> Icons.Default.EmojiEvents
            STATISTICS -> Icons.Default.Analytics
            SETTINGS -> Icons.Default.Settings
        }

    fun localizedTitle(strings: CourtsideStrings): String = when (this) {
        MY_TEAM -> strings.navMyTeam
        MATCHDAY -> strings.navMatchday
        STANDINGS_PLAYOFFS -> strings.navStandings
        STATISTICS -> strings.navAnalytics
        SETTINGS -> strings.navSettings
    }

    companion object {
        /** Beim Start der App wird ausnahmslos immer "My Team" angezeigt. */
        val START_DESTINATION: CourtsideDestination = MY_TEAM
    }
}

/**
 * Gesamtes App-Scaffold in der einheitlichen "Courtside UI" Design-Sprache (in 6 Sprachen: EN, DE, FR, IT, ES, EL):
 * - Links auf Desktop/Foldables: Obsidian-Sidebar (`#0B0B0E`) mit leuchtendem Basketball-Brand-Icon,
 *   orangenen Glow-Icon-Buttons (ohne sichtbare Ctrl+1..5 Badges) und 6-Sprachen-Schnellumschalter.
 * - Unten auf Smartphones (`COMPACT`): Abgedunkelte Broadcast-NavigationBar.
 * - Alle 5 Screens nutzen Live-Daten der offiziellen EuroLeague-API, echte Vereins-Logos und
 *   geprüfte Kader inkl. täglichem Injury-Report.
 */
@Composable
fun MainAppScaffold(
    repository: EuroLeagueMockRepository,
    notificationManager: EuroLeagueNotificationManager,
    offlineFirstRepository: EuroLeagueOfflineFirstRepository? = null,
    preferencesDataStore: CourtsidePreferencesDataStore? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteIds by repository.favoriteTeamIds.collectAsState()
    val primaryTeamId by repository.selectedTeamId.collectAsState()
    val allTeams by repository.teamsFlow.collectAsState()
    val allPlayers by repository.playersFlow.collectAsState()
    val games by repository.games.collectAsState()
    val activeRound by repository.activeRoundFlow.collectAsState()
    val syncStatus by repository.lastSyncStatusFlow.collectAsState()
    val activeLanguage by repository.appLanguageFlow.collectAsState()
    val activeThemeMode by repository.themeModeFlow.collectAsState()
    val strings = remember(activeLanguage) { CourtsideTranslations.forLanguage(activeLanguage) }

    LaunchedEffect(activeLanguage) {
        notificationManager.setLanguage(activeLanguage)
    }

    val onChangeLanguage: (AppLanguage) -> Unit = { newLang ->
        repository.selectLanguage(newLang)
        notificationManager.setLanguage(newLang)
        preferencesDataStore?.saveLanguageImmediate(newLang)
        scope.launch {
            preferencesDataStore?.setLanguage(newLang)
        }
    }

    val onChangeThemeMode: (AppThemeMode) -> Unit = { newMode ->
        repository.selectThemeMode(newMode)
        preferencesDataStore?.saveThemeModeImmediate(newMode)
        scope.launch {
            preferencesDataStore?.setThemeMode(newMode)
            CourtsideAppWidgetProvider.updateAllWidgets(context)
        }
    }

    val primaryTeam = remember(primaryTeamId, allTeams) {
        repository.findTeam(primaryTeamId) ?: allTeams.first()
    }

    val currentDestination by repository.activeDestinationFlow.collectAsState()
    val pendingDeepLinkGame by repository.pendingDeepLinkGameFlow.collectAsState()
    var selectedStandingsTeamId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRound by rememberSaveable { mutableIntStateOf(activeRound) }
    var userManuallySelectedRound by rememberSaveable { mutableStateOf(false) }
    var filterOnlyFavorites by rememberSaveable { mutableStateOf(false) }
    var comparisonTeamAId by rememberSaveable { mutableStateOf(primaryTeam.id) }
    var comparisonTeamBId by rememberSaveable { mutableStateOf("PAN") }
    var activeDeepLinkGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlayerProfile by remember { mutableStateOf<PlayerProfile?>(null) }
    var reminderDialogGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var gameRemindersMap by remember {
        mutableStateOf(preferencesDataStore?.getSavedGameRemindersImmediate() ?: emptyMap())
    }

    LaunchedEffect(preferencesDataStore) {
        preferencesDataStore?.userPreferencesFlow?.collect { prefs ->
            gameRemindersMap = prefs.gameReminders
        }
    }

    var tipOffChannelEnabled by rememberSaveable {
        mutableStateOf(preferencesDataStore?.getSavedTipOffAlarmImmediate() ?: true)
    }
    var halftimeFinalChannelEnabled by rememberSaveable {
        mutableStateOf(preferencesDataStore?.getSavedHalftimeFinalImmediate() ?: true)
    }
    var closeGameChannelEnabled by rememberSaveable {
        mutableStateOf(preferencesDataStore?.getSavedCloseGameAlertImmediate() ?: true)
    }

    val updateRemindersForGame: (EuroLeagueGame, Set<GameReminderType>) -> Unit = { game, newSet ->
        val nextMap = if (newSet.isEmpty()) {
            gameRemindersMap - game.id
        } else {
            gameRemindersMap + (game.id to newSet)
        }
        gameRemindersMap = nextMap
        preferencesDataStore?.saveGameRemindersImmediate(nextMap)
        notificationManager.onGameRemindersUpdated(game, newSet)
        scope.launch {
            preferencesDataStore?.setGameReminders(nextMap)
            notificationManager.evaluateAutomaticGameNightAlerts(
                games = repository.games.value,
                favoriteTeamIds = favoriteIds + primaryTeam.id,
                tipOffEnabled = tipOffChannelEnabled,
                halftimeFinalEnabled = halftimeFinalChannelEnabled,
                closeGameEnabled = closeGameChannelEnabled,
                language = activeLanguage,
                perGameReminders = nextMap
            )
        }
    }

    LaunchedEffect(pendingDeepLinkGame) {
        val target = pendingDeepLinkGame ?: return@LaunchedEffect
        userManuallySelectedRound = true
        selectedRound = target.second.coerceIn(1, 38)
        activeDeepLinkGameId = target.first
        repository.consumePendingDeepLinkGame()
    }

    LaunchedEffect(primaryTeam.id, activeLanguage, activeThemeMode, games.size) {
        CourtsideAppWidgetProvider.updateAllWidgets(context)
    }

    val navigateToDestination: (CourtsideDestination) -> Unit = { dest ->
        if (dest == CourtsideDestination.STANDINGS_PLAYOFFS) {
            selectedStandingsTeamId = null
        }
        repository.selectDestination(dest)
    }

    BackHandler(
        enabled = currentDestination == CourtsideDestination.STANDINGS_PLAYOFFS && selectedStandingsTeamId != null
    ) {
        selectedStandingsTeamId = null
    }

    LaunchedEffect(activeRound) {
        if (!userManuallySelectedRound && activeRound in 1..38) {
            selectedRound = activeRound
        }
    }

    LaunchedEffect(primaryTeam.id) {
        comparisonTeamAId = primaryTeam.id
        if (comparisonTeamBId == primaryTeam.id) {
            comparisonTeamBId = if (primaryTeam.id == "PAN") "OLY" else "PAN"
        }
    }

    var isBookPosture by remember { mutableStateOf(false) }
    var isTableTopPosture by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        val activity = context as? Activity ?: return@LaunchedEffect
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { layoutInfo ->
            val fold = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
            isBookPosture = fold?.state == FoldingFeature.State.HALF_OPENED &&
                    fold.orientation == FoldingFeature.Orientation.VERTICAL
            isTableTopPosture = fold?.state == FoldingFeature.State.HALF_OPENED &&
                    fold.orientation == FoldingFeature.Orientation.HORIZONTAL
        }
    }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val widthClass = adaptiveInfo.windowSizeClass.windowWidthSizeClass
    val isCompact = shouldUseSingleColumnMobileLayout(widthClass)
    val configuration = LocalConfiguration.current
    val isPhoneDevice = isAndroidPhoneViewport(isCompact, configuration.smallestScreenWidthDp)

    var isLiveSyncing by remember { mutableStateOf(false) }
    var syncFeedbackBanner by remember { mutableStateOf<LiveSyncFeedbackState?>(null) }

    val updateInfo by CourtsideAppUpdateManager.updateState.collectAsState()
    val dismissedUpdateVersion by CourtsideAppUpdateManager.dismissedVersionTag.collectAsState()
    var autoUpdateCheckEnabled by rememberSaveable {
        mutableStateOf(preferencesDataStore?.getSavedAutoUpdateEnabledImmediate() ?: true)
    }
    var betaUpdateChannelEnabled by rememberSaveable {
        mutableStateOf(
            preferencesDataStore?.getSavedBetaUpdatesEnabledImmediate()
                ?: CourtsideAppUpdateManager.isPreReleaseVersion(BuildConfig.VERSION_NAME)
        )
    }

    val onToggleAutoUpdateCheck: (Boolean) -> Unit = { enabled ->
        autoUpdateCheckEnabled = enabled
        preferencesDataStore?.saveAutoUpdateEnabledImmediate(enabled)
        scope.launch {
            preferencesDataStore?.setAutoUpdateCheckEnabled(enabled)
            if (enabled) {
                CourtsideAppUpdateManager.checkForUpdates(
                    isManual = false,
                    includePrereleases = betaUpdateChannelEnabled
                )
            }
        }
    }

    val onToggleBetaUpdateChannel: (Boolean) -> Unit = { enabled ->
        betaUpdateChannelEnabled = enabled
        preferencesDataStore?.saveBetaUpdatesEnabledImmediate(enabled)
        scope.launch {
            preferencesDataStore?.setBetaUpdatesEnabled(enabled)
            CourtsideAppUpdateManager.checkForUpdates(
                isManual = true,
                includePrereleases = enabled
            )
        }
    }

    val triggerManualUpdateCheck: () -> Unit = {
        scope.launch {
            syncFeedbackBanner = LiveSyncFeedbackState(
                isRunning = true,
                isError = false,
                title = strings.checkingUpdatesLabel,
                detail = "${strings.playProtectBadge} • v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
            )
            val info = CourtsideAppUpdateManager.checkForUpdates(
                isManual = true,
                includePrereleases = betaUpdateChannelEnabled
            )
            syncFeedbackBanner = when (info.status) {
                UpdateStatus.UPDATE_AVAILABLE -> LiveSyncFeedbackState(
                    isRunning = false,
                    isError = false,
                    title = "${strings.updateAvailablePrefix} v${info.latestVersionName}",
                    detail = "${info.releaseTitle} • ${strings.playProtectBadge}"
                )
                UpdateStatus.UP_TO_DATE -> LiveSyncFeedbackState(
                    isRunning = false,
                    isError = false,
                    title = "${strings.upToDateLabel} (v${BuildConfig.VERSION_NAME})",
                    detail = "${strings.playProtectBadge} • Build ${BuildConfig.VERSION_CODE} (${info.lastCheckedTime})"
                )
                else -> LiveSyncFeedbackState(
                    isRunning = false,
                    isError = true,
                    title = "⚠️ ${info.errorMessage ?: "GitHub Releases unreachable"}",
                    detail = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                )
            }
            delay(4_500L)
            if (!isLiveSyncing) {
                syncFeedbackBanner = null
            }
        }
    }

    // Automatischer Google Play Protect-konformer Update-Check beim App-Start & alle 6 Stunden
    LaunchedEffect(autoUpdateCheckEnabled, betaUpdateChannelEnabled) {
        if (autoUpdateCheckEnabled) {
            while (true) {
                CourtsideAppUpdateManager.checkForUpdates(
                    isManual = false,
                    includePrereleases = betaUpdateChannelEnabled
                )
                delay(6 * 60 * 60 * 1_000L)
            }
        }
    }

    val triggerLiveSync: () -> Unit = {
        if (!isLiveSyncing) {
            scope.launch {
                isLiveSyncing = true
                syncFeedbackBanner = buildSyncingFeedbackState(activeLanguage)
                // 1. Sofortiger High-Speed Live-Header-Puls (<200ms), damit Spielstände augenblicklich aktualisieren
                val tickRes = offlineFirstRepository?.refreshLiveMatchdayTick(forceScheduleSync = false)
                repository.games.value
                    .filter { it.status == GameStatus.LIVE }
                    .forEach { liveGame ->
                        launch { offlineFirstRepository?.refreshSelectedGameLiveStats(liveGame) }
                    }
                // 2. Anschließend vollständiger Hintergrund-Abgleich von Tabelle & Kader
                val res = offlineFirstRepository?.syncSeasonDataFromRemoteOrSeedCache()
                offlineFirstRepository?.verifyAndSyncClubRoster(primaryTeam.id)
                val timeStr = runCatching {
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                }.getOrDefault("")
                isLiveSyncing = false
                syncFeedbackBanner = if ((res != null && res.isSuccess) || (tickRes != null && tickRes.isSuccess)) {
                    buildSyncSuccessFeedbackState(
                        language = activeLanguage,
                        timeStr = timeStr,
                        clubCount = repository.teamsFlow.value.size,
                        teamCode = primaryTeam.code
                    )
                } else {
                    buildSyncOfflineFeedbackState(activeLanguage, timeStr)
                }
                delay(4_500L)
                if (!isLiveSyncing) {
                    syncFeedbackBanner = null
                }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    CompositionLocalProvider(
        LocalAppLanguage provides activeLanguage,
        LocalCourtsideStrings provides strings
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CourtsideDesignTokens.ShellObsidian)
                .then(
                    if (isPhoneDevice) {
                        Modifier
                            .statusBarsPadding()
                            .displayCutoutPadding()
                    } else {
                        Modifier
                    }
                )
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val ctrlOrCmd = event.isCtrlPressed || event.isMetaPressed
                    when {
                        event.key == Key.Escape &&
                            currentDestination == CourtsideDestination.STANDINGS_PLAYOFFS &&
                            selectedStandingsTeamId != null -> {
                            selectedStandingsTeamId = null; true
                        }
                        ctrlOrCmd && event.key == Key.One -> {
                            navigateToDestination(CourtsideDestination.MY_TEAM); true
                        }
                        ctrlOrCmd && event.key == Key.Two -> {
                            navigateToDestination(CourtsideDestination.MATCHDAY); true
                        }
                        ctrlOrCmd && event.key == Key.Three -> {
                            navigateToDestination(CourtsideDestination.STANDINGS_PLAYOFFS); true
                        }
                        ctrlOrCmd && event.key == Key.Four -> {
                            navigateToDestination(CourtsideDestination.STATISTICS); true
                        }
                        ctrlOrCmd && event.key == Key.Five -> {
                            navigateToDestination(CourtsideDestination.SETTINGS); true
                        }
                        event.key == Key.F -> {
                            filterOnlyFavorites = !filterOnlyFavorites; true
                        }
                        event.key == Key.R -> {
                            triggerLiveSync(); true
                        }
                        event.key == Key.DirectionLeft -> {
                            userManuallySelectedRound = true
                            selectedRound = (selectedRound - 1).coerceAtLeast(1); true
                        }
                        event.key == Key.DirectionRight -> {
                            userManuallySelectedRound = true
                            selectedRound = (selectedRound + 1).coerceAtMost(38); true
                        }
                        else -> false
                    }
                }
        ) {
            if (isCompact) {
                CourtsideMobileTopBar(
                    activeLanguage = activeLanguage,
                    activeThemeMode = activeThemeMode,
                    isSyncing = isLiveSyncing,
                    updateInfo = updateInfo,
                    onToggleThemeMode = { onChangeThemeMode(activeThemeMode.toggled()) },
                    onCycleLanguage = {
                        val nextIdx = (AppLanguage.entries.indexOf(activeLanguage) + 1) % AppLanguage.entries.size
                        onChangeLanguage(AppLanguage.entries[nextIdx])
                    },
                    onLiveRefresh = triggerLiveSync,
                    onVersionClick = {
                        if (updateInfo.status == UpdateStatus.UPDATE_AVAILABLE) {
                            CourtsideAppUpdateManager.launchPlayProtectCompliantUpdate(context, updateInfo)
                        } else {
                            triggerManualUpdateCheck()
                        }
                    }
                )
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Linke Courtside-Sidebar für Desktop (Expanded) & Foldable/Tablet (Medium)
                if (!isCompact) {
                    CourtsideLeftSidebar(
                        currentDestination = currentDestination,
                        activeLanguage = activeLanguage,
                        activeThemeMode = activeThemeMode,
                        strings = strings,
                        isExpanded = widthClass == WindowWidthSizeClass.EXPANDED,
                        isSyncing = isLiveSyncing,
                        onSelectDestination = navigateToDestination,
                        onSelectLanguage = onChangeLanguage,
                        onToggleThemeMode = { onChangeThemeMode(activeThemeMode.toggled()) },
                        onLiveRefresh = triggerLiveSync
                    )
                }

                // Haupt-Arbeitsbereich inkl. Live-Sync-Status-Hinweis & permanenter Versionsnummer unten rechts
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentDestination) {
                        CourtsideDestination.MY_TEAM -> MyTeamDashboardScreen(
                            primaryTeam = primaryTeam,
                            allTeams = allTeams,
                            favoriteTeamIds = favoriteIds,
                            roster = allPlayers.filter {
                                matchesClubCode(it.teamId, primaryTeam)
                            },
                            games = games,
                            activeRound = activeRound,
                            syncStatus = syncStatus,
                            isTableTopPosture = isTableTopPosture,
                            isCompact = isCompact,
                            onSelectPrimaryTeam = { teamId ->
                                repository.selectPrimaryTeam(teamId)
                                preferencesDataStore?.savePrimaryTeamImmediate(teamId)
                                scope.launch {
                                    preferencesDataStore?.setPrimaryTeam(teamId)
                                    offlineFirstRepository?.verifyAndSyncClubRoster(teamId)
                                }
                            },
                            onVerifyRosterNow = {
                                scope.launch {
                                    isLiveSyncing = true
                                    syncFeedbackBanner = buildSyncingFeedbackState(activeLanguage)
                                    val res = offlineFirstRepository?.verifyAndSyncClubRoster(primaryTeam.id)
                                    val timeStr = runCatching {
                                        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                    }.getOrDefault("")
                                    isLiveSyncing = false
                                    syncFeedbackBanner = if (res != null && res.isSuccess) {
                                        buildRosterVerifiedFeedbackState(activeLanguage, primaryTeam.name, timeStr)
                                    } else {
                                        buildSyncOfflineFeedbackState(activeLanguage, timeStr)
                                    }
                                    delay(4_000L)
                                    if (!isLiveSyncing) {
                                        syncFeedbackBanner = null
                                    }
                                }
                            },
                            onSelectPlayer = { player -> selectedPlayerProfile = player },
                            gameRemindersMap = gameRemindersMap,
                            onOpenGameRemindersDialog = { g -> reminderDialogGameId = g.id }
                        )

                        CourtsideDestination.MATCHDAY -> MatchCenterScreen(
                            games = games,
                            selectedRound = selectedRound,
                            syncStatus = syncStatus,
                            filterOnlyFavorites = filterOnlyFavorites,
                            favoriteTeamIds = favoriteIds,
                            hingeInBookPosture = isBookPosture,
                            isCompact = isCompact,
                            notificationManager = notificationManager,
                            onPreviousRound = {
                                userManuallySelectedRound = true
                                selectedRound = (selectedRound - 1).coerceAtLeast(1)
                            },
                            onNextRound = {
                                userManuallySelectedRound = true
                                selectedRound = (selectedRound + 1).coerceAtMost(38)
                            },
                            onSelectRound = { rnd ->
                                userManuallySelectedRound = true
                                selectedRound = rnd.coerceIn(1, 38)
                            },
                            onToggleFavoriteFilter = { filterOnlyFavorites = !filterOnlyFavorites },
                            onTriggerLiveRefresh = triggerLiveSync,
                            onLiveScoreTick = {
                                scope.launch {
                                    offlineFirstRepository?.refreshLiveMatchdayTick()
                                    notificationManager.evaluateAutomaticGameNightAlerts(
                                        games = repository.games.value,
                                        favoriteTeamIds = favoriteIds + primaryTeam.id,
                                        tipOffEnabled = tipOffChannelEnabled,
                                        halftimeFinalEnabled = halftimeFinalChannelEnabled,
                                        closeGameEnabled = closeGameChannelEnabled,
                                        language = activeLanguage,
                                        perGameReminders = gameRemindersMap
                                    )
                                }
                            },
                            onRefreshSelectedGameStats = { game ->
                                scope.launch {
                                    offlineFirstRepository?.refreshSelectedGameLiveStats(game)
                                }
                            },
                            onOpenTeamStats = { teamId ->
                                comparisonTeamBId = teamId
                                repository.selectDestination(CourtsideDestination.STATISTICS)
                            },
                            deepLinkGameId = activeDeepLinkGameId,
                            onConsumeDeepLinkGame = { activeDeepLinkGameId = null },
                            onClickPlayerRef = { pId, pName, tId ->
                                selectedPlayerProfile = resolveClickedPlayerProfile(
                                    playerId = pId,
                                    playerName = pName,
                                    teamId = tId,
                                    allPlayers = allPlayers,
                                    allGames = games
                                )
                            },
                            gameRemindersMap = gameRemindersMap,
                            onUpdateGameReminders = updateRemindersForGame,
                            onOpenGameRemindersDialog = { g -> reminderDialogGameId = g.id }
                        )

                        CourtsideDestination.STANDINGS_PLAYOFFS -> {
                            val standingsList = remember(allTeams, games) { repository.getStandings() }
                            val selectedTeamForDetail = selectedStandingsTeamId?.let { id ->
                                standingsList.firstOrNull { matchesClubCode(id, it) }
                                    ?: repository.findTeam(id)
                            }
                            if (selectedTeamForDetail != null) {
                                val standingRank = standingsList.indexOfFirst {
                                    matchesClubCode(it.id, selectedTeamForDetail)
                                }.let { idx -> if (idx >= 0) idx + 1 else 1 }
                                StandingsTeamDetailScreen(
                                    team = selectedTeamForDetail,
                                    rank = standingRank,
                                    isPrimaryTeam = matchesClubCode(primaryTeam.id, selectedTeamForDetail),
                                    isCompact = isCompact,
                                    roster = allPlayers.filter {
                                        matchesClubCode(it.teamId, selectedTeamForDetail)
                                    },
                                    games = games,
                                    onBackToStandings = { selectedStandingsTeamId = null },
                                    onSetAsMyTeam = {
                                        repository.selectPrimaryTeam(selectedTeamForDetail.id)
                                        preferencesDataStore?.savePrimaryTeamImmediate(selectedTeamForDetail.id)
                                        scope.launch {
                                            preferencesDataStore?.setPrimaryTeam(selectedTeamForDetail.id)
                                            offlineFirstRepository?.verifyAndSyncClubRoster(selectedTeamForDetail.id)
                                        }
                                    },
                                    onOpenGameInMatchCenter = { round ->
                                        userManuallySelectedRound = true
                                        selectedRound = round.coerceIn(1, 38)
                                        repository.selectDestination(CourtsideDestination.MATCHDAY)
                                    },
                                    onOpenExactGameInMatchCenter = { gId, round ->
                                        userManuallySelectedRound = true
                                        selectedRound = round.coerceIn(1, 38)
                                        activeDeepLinkGameId = gId
                                        repository.selectDestination(CourtsideDestination.MATCHDAY)
                                    },
                                    onSelectPlayer = { player -> selectedPlayerProfile = player }
                                )
                            } else {
                                StandingsScreen(
                                    standings = standingsList,
                                    primaryTeam = primaryTeam,
                                    favoriteIds = favoriteIds,
                                    isCompact = isCompact,
                                    onSelectTeam = { clickedTeam ->
                                        selectedStandingsTeamId = clickedTeam.id
                                        scope.launch {
                                            offlineFirstRepository?.verifyAndSyncClubRoster(clickedTeam.id)
                                        }
                                    }
                                )
                            }
                        }

                        CourtsideDestination.STATISTICS -> {
                            val resolvedTeamA = repository.findTeam(comparisonTeamAId) ?: primaryTeam
                            val resolvedTeamB = repository.findTeam(comparisonTeamBId)
                                ?: allTeams.firstOrNull { it.id != resolvedTeamA.id }
                                ?: allTeams.first()
                            SeasonLeadersAndComparisonScreen(
                                players = allPlayers,
                                teams = allTeams,
                                games = games,
                                teamA = resolvedTeamA,
                                teamB = resolvedTeamB,
                                isCompact = isCompact,
                                onSelectTeamA = { newA ->
                                    comparisonTeamAId = newA
                                    if (comparisonTeamBId == newA) {
                                        comparisonTeamBId = allTeams.firstOrNull { it.id != newA }?.id ?: "OLY"
                                    }
                                },
                                onSelectTeamB = { newB ->
                                    comparisonTeamBId = newB
                                    if (comparisonTeamAId == newB) {
                                        comparisonTeamAId = allTeams.firstOrNull { it.id != newB }?.id ?: "PAN"
                                    }
                                },
                                onSelectPlayer = { player -> selectedPlayerProfile = player }
                            )
                        }

                        CourtsideDestination.SETTINGS -> SettingsAndNotificationTestScreen(
                            primaryTeam = primaryTeam,
                            games = games,
                            activeLanguage = activeLanguage,
                            activeThemeMode = activeThemeMode,
                            onSelectThemeMode = onChangeThemeMode,
                            isCompact = isCompact,
                            onSelectLanguage = onChangeLanguage,
                            notificationManager = notificationManager,
                            tipOffEnabled = tipOffChannelEnabled,
                            halftimeFinalEnabled = halftimeFinalChannelEnabled,
                            closeGameAlertEnabled = closeGameChannelEnabled,
                            onUpdateNotificationChannels = { tipOff, htFinal, closeGame ->
                                tipOffChannelEnabled = tipOff
                                halftimeFinalChannelEnabled = htFinal
                                closeGameChannelEnabled = closeGame
                                preferencesDataStore?.saveNotificationChannelsImmediate(tipOff, htFinal, closeGame)
                                scope.launch {
                                    preferencesDataStore?.updateNotificationChannels(tipOff, htFinal, closeGame)
                                    notificationManager.evaluateAutomaticGameNightAlerts(
                                        games = games,
                                        favoriteTeamIds = favoriteIds + primaryTeam.id,
                                        tipOffEnabled = tipOff,
                                        halftimeFinalEnabled = htFinal,
                                        closeGameEnabled = closeGame,
                                        language = activeLanguage,
                                        perGameReminders = gameRemindersMap
                                    )
                                }
                            },
                            updateInfo = updateInfo,
                            autoUpdateCheckEnabled = autoUpdateCheckEnabled,
                            onToggleAutoUpdateCheck = onToggleAutoUpdateCheck,
                            betaUpdateChannelEnabled = betaUpdateChannelEnabled,
                            onToggleBetaUpdateChannel = onToggleBetaUpdateChannel,
                            onCheckForUpdatesManual = triggerManualUpdateCheck,
                            onInstallUpdate = {
                                CourtsideAppUpdateManager.launchPlayProtectCompliantUpdate(context, updateInfo)
                            },
                            onOpenReleaseNotes = {
                                CourtsideAppUpdateManager.openReleaseNotesInBrowser(context, updateInfo)
                            }
                        )
                    }

                    // Interaktive Spieler-Scouting-Karte & Spiel-Logbuch (Modal über allen Tabs)
                    val activePlayer = selectedPlayerProfile
                    if (activePlayer != null) {
                        val playerTeam = remember(activePlayer.teamId, allTeams) {
                            repository.findTeam(activePlayer.teamId)
                        }
                        PlayerProfileDialog(
                            player = activePlayer,
                            team = playerTeam,
                            games = games,
                            isCompact = isCompact,
                            onDismiss = { selectedPlayerProfile = null },
                            onOpenGameInMatchCenter = { gId, rnd ->
                                userManuallySelectedRound = true
                                selectedRound = rnd.coerceIn(1, 38)
                                activeDeepLinkGameId = gId
                                repository.selectDestination(CourtsideDestination.MATCHDAY)
                            }
                        )
                    }

                    // Spielspezifischer Erinnerungs-Dialog (Q1, Halbzeit, Q3, Crunch-Time, Endergebnis)
                    val activeReminderGame = reminderDialogGameId?.let { id -> games.find { it.id == id } }
                    if (activeReminderGame != null) {
                        GameRemindersDialog(
                            game = activeReminderGame,
                            activeReminders = gameRemindersMap[activeReminderGame.id].orEmpty(),
                            isCompact = isCompact,
                            onUpdateReminders = { nextSet ->
                                updateRemindersForGame(activeReminderGame, nextSet)
                            },
                            onDismiss = { reminderDialogGameId = null }
                        )
                    }

                    // Google Play Protect-konformer Auto-Update-Banner bei verfügbarer neuer Version
                    val showUpdateBanner = updateInfo.status == UpdateStatus.UPDATE_AVAILABLE &&
                        dismissedUpdateVersion != CourtsideAppUpdateManager.normalizeVersionName(updateInfo.latestVersionName)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showUpdateBanner,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 14.dp, end = 16.dp, start = 16.dp)
                    ) {
                        Surface(
                            color = CourtsideDesignTokens.CardSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .widthIn(max = 460.dp)
                                .border(1.5.dp, CourtsideDesignTokens.NeonMadeGreen, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${strings.updateAvailablePrefix} v${updateInfo.latestVersionName}",
                                        color = CourtsideDesignTokens.NeonMadeGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        color = Color(0x2600E676),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = strings.playProtectBadge,
                                            color = CourtsideDesignTokens.NeonMadeGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME} → v${updateInfo.latestVersionName} • ${updateInfo.releaseTitle}",
                                    color = CourtsideDesignTokens.SecondaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            CourtsideAppUpdateManager.launchPlayProtectCompliantUpdate(context, updateInfo)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.NeonMadeGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                                    ) {
                                        Text(
                                            text = strings.downloadInstallUpdateButton,
                                            color = CourtsideDesignTokens.OnAccentText,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Surface(
                                        color = CourtsideDesignTokens.PillSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .border(1.dp, CourtsideDesignTokens.SubtleGlassBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                CourtsideAppUpdateManager.dismissUpdateBanner(updateInfo.latestVersionName)
                                            }
                                            .pointerHoverIcon(PointerIcon.Hand)
                                    ) {
                                        Text(
                                            text = strings.laterButton,
                                            color = CourtsideDesignTokens.SecondaryText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sichtbarer Live-Sync-Status-Hinweis unten links im Arbeitsbereich (neben dem Live-Sync-Button)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = syncFeedbackBanner != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = if (isCompact) 12.dp else 16.dp,
                                bottom = 12.dp,
                                end = if (isCompact) 12.dp else 96.dp
                            )
                    ) {
                        val banner = syncFeedbackBanner
                        if (banner != null) {
                            val accentColor = when {
                                banner.isRunning -> CourtsideDesignTokens.EuroLeagueOrange
                                banner.isError -> CourtsideDesignTokens.NeonMissedRed
                                else -> CourtsideDesignTokens.NeonMadeGreen
                            }
                            Surface(
                                color = CourtsideDesignTokens.CardSurface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .border(1.5.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    if (banner.isRunning) {
                                        CircularProgressIndicator(
                                            color = CourtsideDesignTokens.EuroLeagueOrange,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = banner.title,
                                            color = accentColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = banner.detail,
                                            color = CourtsideDesignTokens.SecondaryText,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Permanente Versionsnummer unten rechts auf Tablet/Laptop (auf Smartphones sitzt sie oben in CourtsideMobileTopBar)
                    if (!isCompact) {
                        val hasNewUpdate = updateInfo.status == UpdateStatus.UPDATE_AVAILABLE
                        Surface(
                            color = if (hasNewUpdate) Color(0x26059669) else CourtsideDesignTokens.CardSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 8.dp)
                                .border(
                                    1.dp,
                                    if (hasNewUpdate) CourtsideDesignTokens.NeonMadeGreen else Color(0x38FF6600),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (hasNewUpdate) {
                                        CourtsideAppUpdateManager.launchPlayProtectCompliantUpdate(context, updateInfo)
                                    } else {
                                        triggerManualUpdateCheck()
                                    }
                                }
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = if (hasNewUpdate) {
                                    "🚀 v${BuildConfig.VERSION_NAME} → v${updateInfo.latestVersionName}"
                                } else {
                                    "v${BuildConfig.VERSION_NAME}"
                                },
                                color = if (hasNewUpdate) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.SecondaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Untere NavigationBar auf Smartphones / zugeklappten Foldables (COMPACT)
            if (isCompact) {
                NavigationBar(
                    containerColor = CourtsideDesignTokens.SidebarObsidian,
                    tonalElevation = 0.dp
                ) {
                    CourtsideDestination.entries.forEach { dest ->
                        val selected = currentDestination == dest
                        val navTitle = dest.localizedTitle(strings)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToDestination(dest) },
                            icon = { Icon(dest.icon, contentDescription = navTitle) },
                            alwaysShowLabel = true,
                            label = {
                                Text(
                                    text = navTitle,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CourtsideDesignTokens.EuroLeagueOrange,
                                selectedTextColor = CourtsideDesignTokens.EuroLeagueOrange,
                                indicatorColor = Color(0x33FF6600),
                                unselectedIconColor = CourtsideDesignTokens.MutedText,
                                unselectedTextColor = CourtsideDesignTokens.MutedText
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourtsideMobileTopBar(
    activeLanguage: AppLanguage,
    activeThemeMode: AppThemeMode,
    isSyncing: Boolean,
    updateInfo: AppUpdateInfo,
    onToggleThemeMode: () -> Unit,
    onCycleLanguage: () -> Unit,
    onLiveRefresh: () -> Unit,
    onVersionClick: () -> Unit
) {
    val hasNewUpdate = updateInfo.status == UpdateStatus.UPDATE_AVAILABLE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CourtsideDesignTokens.SidebarObsidian)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CourtsideDesignTokens.EuroLeagueOrange, Color(0xFFFF3D00))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsBasketball,
                    contentDescription = null,
                    tint = CourtsideDesignTokens.OnAccentText,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                text = "EuroLeague Courtside",
                color = CourtsideDesignTokens.PrimaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = if (hasNewUpdate) Color(0x3300E676) else CourtsideDesignTokens.ElevatedSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (hasNewUpdate) CourtsideDesignTokens.NeonMadeGreen else Color(0x38FF6600),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onVersionClick() }
            ) {
                Text(
                    text = if (hasNewUpdate) "🚀 v${updateInfo.latestVersionName}" else "v${BuildConfig.VERSION_NAME}",
                    color = if (hasNewUpdate) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.SecondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = CourtsideDesignTokens.ElevatedSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, Color(0x55FF6600), RoundedCornerShape(8.dp))
                    .clickable { onToggleThemeMode() }
            ) {
                Text(
                    text = if (activeThemeMode.isLight) "☀️" else "🌙",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = CourtsideDesignTokens.ElevatedSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(1.dp, Color(0x55FF6600), RoundedCornerShape(8.dp))
                    .clickable { onCycleLanguage() }
            ) {
                Text(
                    text = "${activeLanguage.flagEmoji} ${activeLanguage.shortLabel}",
                    color = CourtsideDesignTokens.PrimaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Surface(
                color = if (isSyncing) Color(0x40FF6600) else Color(0x24FF6600),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isSyncing) CourtsideDesignTokens.EuroLeagueOrange else Color(0x88FF6600),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = !isSyncing) { onLiveRefresh() }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Text(
                            text = "⚡ Sync",
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private data class LiveSyncFeedbackState(
    val isRunning: Boolean,
    val isError: Boolean,
    val title: String,
    val detail: String
)

private fun buildSyncingFeedbackState(language: AppLanguage): LiveSyncFeedbackState = when (language) {
    AppLanguage.EN -> LiveSyncFeedbackState(
        isRunning = true,
        isError = false,
        title = "⚡ EuroLeague Live Sync in progress…",
        detail = "Updating live scores, all 38 rounds, standings, club rosters & season stats from api-live.euroleague.net…"
    )
    AppLanguage.DE -> LiveSyncFeedbackState(
        isRunning = true,
        isError = false,
        title = "⚡ EuroLeague Live-Sync läuft…",
        detail = "Aktualisiere Spielstände, alle 38 Spieltage, Tabelle, Kader & Saisonstatistiken live von api-live.euroleague.net…"
    )
    AppLanguage.FR -> LiveSyncFeedbackState(
        isRunning = true,
        isError = false,
        title = "⚡ Synchronisation EuroLeague en cours…",
        detail = "Mise à jour des scores en direct, des 38 journées, du classement, des effectifs et des statistiques…"
    )
    AppLanguage.IT -> LiveSyncFeedbackState(
        isRunning = true,
        isError = false,
        title = "⚡ Sincronizzazione live EuroLeague in corso…",
        detail = "Aggiornamento punteggi live, 38 giornate, classifica, roster e statistiche stagionali…"
    )
    AppLanguage.ES -> LiveSyncFeedbackState(
        isRunning = true,
        isError = false,
        title = "⚡ Sincronización en vivo de EuroLeague en curso…",
        detail = "Actualizando marcadores en vivo, 38 jornadas, clasificación, plantillas y estadísticas…"
    )
    AppLanguage.EL -> LiveSyncFeedbackState(
        isRunning = true,
        isError = false,
        title = "⚡ Ζωντανός συγχρονισμός EuroLeague σε εξέλιξη…",
        detail = "Ενημέρωση ζωντανών σκορ, 38 αγωνιστικών, βαθμολογίας, ρόστερ & στατιστικών σεζόν…"
    )
}

private fun buildSyncSuccessFeedbackState(
    language: AppLanguage,
    timeStr: String,
    clubCount: Int,
    teamCode: String
): LiveSyncFeedbackState = when (language) {
    AppLanguage.EN -> LiveSyncFeedbackState(
        isRunning = false,
        isError = false,
        title = "✅ Live Sync completed ($timeStr)",
        detail = "$clubCount clubs, 38 rounds, standings, $teamCode roster & season statistics are up to date."
    )
    AppLanguage.DE -> LiveSyncFeedbackState(
        isRunning = false,
        isError = false,
        title = "✅ Live-Sync abgeschlossen ($timeStr)",
        detail = "$clubCount Teams, 38 Spieltage, Tabelle, $teamCode-Kader & Saisonstatistiken sind auf dem neuesten Stand."
    )
    AppLanguage.FR -> LiveSyncFeedbackState(
        isRunning = false,
        isError = false,
        title = "✅ Synchronisation terminée ($timeStr)",
        detail = "$clubCount clubs, 38 journées, classement, effectif $teamCode et statistiques à jour."
    )
    AppLanguage.IT -> LiveSyncFeedbackState(
        isRunning = false,
        isError = false,
        title = "✅ Sincronizzazione completata ($timeStr)",
        detail = "$clubCount club, 38 giornate, classifica, roster $teamCode e statistiche aggiornati."
    )
    AppLanguage.ES -> LiveSyncFeedbackState(
        isRunning = false,
        isError = false,
        title = "✅ Sincronización completada ($timeStr)",
        detail = "$clubCount clubes, 38 jornadas, clasificación, plantilla de $teamCode y estadísticas al día."
    )
    AppLanguage.EL -> LiveSyncFeedbackState(
        isRunning = false,
        isError = false,
        title = "✅ Ο συγχρονισμός ολοκληρώθηκε ($timeStr)",
        detail = "$clubCount ομάδες, 38 αγωνιστικές, βαθμολογία, ρόστερ $teamCode & στατιστικά ενημερώθηκαν."
    )
}

private fun buildRosterVerifiedFeedbackState(
    language: AppLanguage,
    teamName: String,
    timeStr: String
): LiveSyncFeedbackState = when (language) {
    AppLanguage.EN -> LiveSyncFeedbackState(false, false, "✅ Roster verified ($timeStr)", "Active squad & injury report updated for $teamName.")
    AppLanguage.DE -> LiveSyncFeedbackState(false, false, "✅ Kader geprüft ($timeStr)", "Aktiver Kader & Verletztenstatus für $teamName erfolgreich abgeglichen.")
    AppLanguage.FR -> LiveSyncFeedbackState(false, false, "✅ Effectif vérifié ($timeStr)", "Effectif actif et rapport médical mis à jour pour $teamName.")
    AppLanguage.IT -> LiveSyncFeedbackState(false, false, "✅ Roster verificato ($timeStr)", "Roster attivo e report infortuni aggiornati per $teamName.")
    AppLanguage.ES -> LiveSyncFeedbackState(false, false, "✅ Plantilla verificada ($timeStr)", "Plantilla activa y parte médico actualizados para $teamName.")
    AppLanguage.EL -> LiveSyncFeedbackState(false, false, "✅ Το ρόστερ επαληθεύτηκε ($timeStr)", "Ενεργό ρόστερ & αναφορά τραυματισμών ενημερώθηκαν για $teamName.")
}

private fun buildSyncOfflineFeedbackState(language: AppLanguage, timeStr: String): LiveSyncFeedbackState = when (language) {
    AppLanguage.EN -> LiveSyncFeedbackState(
        isRunning = false,
        isError = true,
        title = "⚠️ Live API unreachable ($timeStr)",
        detail = "No connection or server busy – keeping latest saved EuroLeague data active."
    )
    AppLanguage.DE -> LiveSyncFeedbackState(
        isRunning = false,
        isError = true,
        title = "⚠️ Live-API momentan nicht erreichbar ($timeStr)",
        detail = "Keine Verbindung oder Server ausgelastet – der zuletzt gespeicherte Datenstand bleibt aktiv."
    )
    AppLanguage.FR -> LiveSyncFeedbackState(
        isRunning = false,
        isError = true,
        title = "⚠️ API en direct indisponible ($timeStr)",
        detail = "Aucune connexion ou serveur occupé – les dernières données enregistrées restent actives."
    )
    AppLanguage.IT -> LiveSyncFeedbackState(
        isRunning = false,
        isError = true,
        title = "⚠️ API live non raggiungibile ($timeStr)",
        detail = "Nessuna connessione o server occupato – restano attivi gli ultimi dati salvati."
    )
    AppLanguage.ES -> LiveSyncFeedbackState(
        isRunning = false,
        isError = true,
        title = "⚠️ API en vivo no disponible ($timeStr)",
        detail = "Sin conexión o servidor ocupado – se mantienen activos los últimos datos guardados."
    )
    AppLanguage.EL -> LiveSyncFeedbackState(
        isRunning = false,
        isError = true,
        title = "⚠️ Το Live API δεν είναι διαθέσιμο ($timeStr)",
        detail = "Δεν υπάρχει σύνδεση ή ο διακομιστής είναι απασχολημένος – διατηρούνται τα τελευταία αποθηκευμένα δεδομένα."
    )
}

/**
 * Linke Courtside-Sidebar ohne sichtbare Ctrl+1..5 Badges (Tastatur-Shortcuts laufen unsichtbar im Hintergrund):
 * - Oben: Leuchtendes EuroLeague-Basketball-Logo
 * - Mitte: Clean zentrierte Icon-Buttons mit orangenem Radial-Glow bei aktivem Tab
 * - Unten: 6-Sprachen-Schnellumschalter (EN, DE, FR, IT, ES, EL) + Live-Refresh-Trigger mit aktivem Ladeindikator
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourtsideLeftSidebar(
    currentDestination: CourtsideDestination,
    activeLanguage: AppLanguage,
    activeThemeMode: AppThemeMode,
    strings: CourtsideStrings,
    isExpanded: Boolean,
    isSyncing: Boolean = false,
    onSelectDestination: (CourtsideDestination) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleThemeMode: () -> Unit = {},
    onLiveRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(if (isExpanded) 172.dp else 76.dp)
            .fillMaxHeight()
            .background(CourtsideDesignTokens.SidebarObsidian)
            .border(width = 1.dp, color = CourtsideDesignTokens.SubtleGlassBorder)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally) {
            // Brand Logo Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 26.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CourtsideDesignTokens.EuroLeagueOrange, Color(0xFFFF3D00))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = CourtsideDesignTokens.OnAccentText,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isExpanded) {
                    Column {
                        Text(
                            text = "EuroLeague",
                            color = CourtsideDesignTokens.PrimaryText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Courtside",
                            color = CourtsideDesignTokens.SecondaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Clean Navigation Items (ohne Ctrl+1..5 Pills)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CourtsideDestination.entries.forEach { dest ->
                    val isSelected = currentDestination == dest
                    val navTitle = dest.localizedTitle(strings)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectDestination(dest) }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.radialGradient(
                                            colors = listOf(Color(0x4DFF6600), Color(0x12FF6600))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(Color.Transparent, Color.Transparent)
                                        )
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xAAFF6600) else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = navTitle,
                                tint = if (isSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.MutedText,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        if (isExpanded) {
                            Text(
                                text = navTitle,
                                color = if (isSelected) CourtsideDesignTokens.PrimaryText else CourtsideDesignTokens.MutedText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 6-Language Quick Switcher (EN, DE, FR, IT, ES, EL)
            if (isExpanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isLangSelected = lang == activeLanguage
                        Surface(
                            color = if (isLangSelected) Color(0x33FF6600) else CourtsideDesignTokens.ElevatedSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isLangSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SubtleGlassBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectLanguage(lang) }
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = "${lang.flagEmoji} ${lang.shortLabel}",
                                color = if (isLangSelected) CourtsideDesignTokens.PrimaryText else CourtsideDesignTokens.MutedText,
                                fontWeight = if (isLangSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                // Compact Rail Mode: Click cycles through the 6 languages
                Surface(
                    color = CourtsideDesignTokens.ElevatedSurface,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x66FF6600), RoundedCornerShape(10.dp))
                        .clickable {
                            val nextIdx = (AppLanguage.entries.indexOf(activeLanguage) + 1) % AppLanguage.entries.size
                            onSelectLanguage(AppLanguage.entries[nextIdx])
                        }
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        text = "${activeLanguage.flagEmoji} ${activeLanguage.shortLabel}",
                        color = CourtsideDesignTokens.PrimaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            // Quick Light / Dark Mode Switcher in Sidebar
            Surface(
                color = CourtsideDesignTokens.ElevatedSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CourtsideDesignTokens.SubtleGlassBorder, RoundedCornerShape(10.dp))
                    .clickable { onToggleThemeMode() }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(
                    text = if (isExpanded) {
                        if (activeThemeMode.isLight) "☀️ Light • 🌙" else "🌙 Dark • ☀️"
                    } else {
                        if (activeThemeMode.isLight) "☀️" else "🌙"
                    },
                    color = CourtsideDesignTokens.PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                )
            }

            // Bottom Live-Refresh Trigger mit aktivem Lade-Indikator
            Surface(
                color = if (isSyncing) Color(0x40FF6600) else Color(0x24FF6600),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSyncing) 1.5.dp else 1.dp,
                        color = if (isSyncing) CourtsideDesignTokens.EuroLeagueOrange else Color(0x66FF6600),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isSyncing) { onLiveRefresh() }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val syncingLabel = when (activeLanguage) {
                                AppLanguage.EN -> "Syncing…"
                                AppLanguage.DE -> "Synchronisiere…"
                                AppLanguage.FR -> "Sync en cours…"
                                AppLanguage.IT -> "Sincronizzo…"
                                AppLanguage.ES -> "Sincronizando…"
                                AppLanguage.EL -> "Συγχρονισμός…"
                            }
                            Text(
                                text = syncingLabel,
                                color = CourtsideDesignTokens.PrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Text(
                            text = if (isExpanded) strings.liveSimButton else "⚡",
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


internal fun matchesClubCode(idOrCode: String, team: EuroLeagueTeam): Boolean =
    com.euroleague.courtside.ui.screens.matchesClubCode(idOrCode, team)

internal fun resolveTeamLastAndUpcomingGames(
    team: EuroLeagueTeam,
    allGames: List<EuroLeagueGame>
): Pair<List<EuroLeagueGame>, List<EuroLeagueGame>> =
    com.euroleague.courtside.ui.screens.resolveTeamLastAndUpcomingGames(team, allGames)

internal fun isWinStreakEntry(entry: String): Boolean =
    com.euroleague.courtside.ui.screens.isWinStreakEntry(entry)
