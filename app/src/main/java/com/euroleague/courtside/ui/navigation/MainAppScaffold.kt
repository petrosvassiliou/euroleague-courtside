package com.euroleague.courtside.ui.navigation

import android.app.Activity
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.euroleague.courtside.BuildConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
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
import java.time.LocalTime
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.compose.runtime.CompositionLocalProvider
import com.euroleague.courtside.data.local.CourtsidePreferencesDataStore
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
import com.euroleague.courtside.ui.screens.MatchCenterScreen
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

enum class CourtsideDestination(
    val title: String,
    val shortcutHint: String,
    val icon: ImageVector
) {
    MY_TEAM("Mein Team", "Ctrl+1", Icons.Default.Home),
    MATCHDAY("EuroLeague", "Ctrl+2", Icons.Default.SportsBasketball),
    STANDINGS_PLAYOFFS("Standings", "Ctrl+3", Icons.Default.EmojiEvents),
    STATISTICS("Analytics", "Ctrl+4", Icons.Default.Analytics),
    SETTINGS("Settings", "Ctrl+5", Icons.Default.Settings);

    fun localizedTitle(strings: CourtsideStrings): String = when (this) {
        MY_TEAM -> strings.navMyTeam
        MATCHDAY -> strings.navMatchday
        STANDINGS_PLAYOFFS -> strings.navStandings
        STATISTICS -> strings.navAnalytics
        SETTINGS -> strings.navSettings
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

    val primaryTeam = remember(primaryTeamId, allTeams) {
        repository.findTeam(primaryTeamId) ?: allTeams.first()
    }

    var currentDestination by rememberSaveable { mutableStateOf(CourtsideDestination.MATCHDAY) }
    var selectedRound by rememberSaveable { mutableIntStateOf(activeRound) }
    var userManuallySelectedRound by rememberSaveable { mutableStateOf(false) }
    var filterOnlyFavorites by rememberSaveable { mutableStateOf(false) }
    var comparisonTeamAId by rememberSaveable { mutableStateOf(primaryTeam.id) }
    var comparisonTeamBId by rememberSaveable { mutableStateOf("PAN") }

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
    val isCompact = widthClass == WindowWidthSizeClass.COMPACT

    var isLiveSyncing by remember { mutableStateOf(false) }
    var syncFeedbackBanner by remember { mutableStateOf<LiveSyncFeedbackState?>(null) }

    val triggerLiveSync: () -> Unit = {
        if (!isLiveSyncing) {
            scope.launch {
                isLiveSyncing = true
                syncFeedbackBanner = buildSyncingFeedbackState(activeLanguage)
                val res = offlineFirstRepository?.syncSeasonDataFromRemoteOrSeedCache()
                offlineFirstRepository?.verifyAndSyncClubRoster(primaryTeam.id)
                offlineFirstRepository?.refreshLiveMatchdayTick()
                val timeStr = runCatching {
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                }.getOrDefault("")
                isLiveSyncing = false
                syncFeedbackBanner = if (res != null && res.isSuccess) {
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
                .focusRequester(focusRequester)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val ctrlOrCmd = event.isCtrlPressed || event.isMetaPressed
                    when {
                        ctrlOrCmd && event.key == Key.One -> {
                            currentDestination = CourtsideDestination.MY_TEAM; true
                        }
                        ctrlOrCmd && event.key == Key.Two -> {
                            currentDestination = CourtsideDestination.MATCHDAY; true
                        }
                        ctrlOrCmd && event.key == Key.Three -> {
                            currentDestination = CourtsideDestination.STANDINGS_PLAYOFFS; true
                        }
                        ctrlOrCmd && event.key == Key.Four -> {
                            currentDestination = CourtsideDestination.STATISTICS; true
                        }
                        ctrlOrCmd && event.key == Key.Five -> {
                            currentDestination = CourtsideDestination.SETTINGS; true
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
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Linke Courtside-Sidebar für Desktop (Expanded) & Foldable/Tablet (Medium)
                if (!isCompact) {
                    CourtsideLeftSidebar(
                        currentDestination = currentDestination,
                        activeLanguage = activeLanguage,
                        strings = strings,
                        isExpanded = widthClass == WindowWidthSizeClass.EXPANDED,
                        isSyncing = isLiveSyncing,
                        onSelectDestination = { currentDestination = it },
                        onSelectLanguage = onChangeLanguage,
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
                                it.teamId == primaryTeam.id || it.teamId == primaryTeam.code
                            },
                            games = games,
                            syncStatus = syncStatus,
                            isTableTopPosture = isTableTopPosture,
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
                            }
                        )

                        CourtsideDestination.MATCHDAY -> MatchCenterScreen(
                            games = games,
                            selectedRound = selectedRound,
                            syncStatus = syncStatus,
                            filterOnlyFavorites = filterOnlyFavorites,
                            favoriteTeamIds = favoriteIds,
                            hingeInBookPosture = isBookPosture,
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
                            onRefreshSelectedGameStats = { game ->
                                scope.launch {
                                    offlineFirstRepository?.refreshSelectedGameLiveStats(game)
                                }
                            },
                            onOpenTeamStats = { teamId ->
                                comparisonTeamBId = teamId
                                currentDestination = CourtsideDestination.STATISTICS
                            }
                        )

                        CourtsideDestination.STANDINGS_PLAYOFFS -> StandingsAndBracketScreen(
                            standings = repository.getStandings(),
                            bracketSeries = repository.getPlayoffBracket(),
                            favoriteIds = favoriteIds
                        )

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
                                }
                            )
                        }

                        CourtsideDestination.SETTINGS -> SettingsAndNotificationTestScreen(
                            primaryTeam = primaryTeam,
                            games = games,
                            activeLanguage = activeLanguage,
                            onSelectLanguage = onChangeLanguage,
                            notificationManager = notificationManager
                        )
                    }

                    // Sichtbarer Live-Sync-Status-Hinweis unten links im Arbeitsbereich (neben dem Live-Sync-Button)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = syncFeedbackBanner != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 12.dp, end = 96.dp)
                    ) {
                        val banner = syncFeedbackBanner
                        if (banner != null) {
                            val accentColor = when {
                                banner.isRunning -> CourtsideDesignTokens.EuroLeagueOrange
                                banner.isError -> CourtsideDesignTokens.NeonMissedRed
                                else -> CourtsideDesignTokens.NeonMadeGreen
                            }
                            Surface(
                                color = Color(0xEB141419),
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
                                            color = Color(0xFFE5E7EB),
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Permanente Versionsnummer unten rechts
                    Surface(
                        color = Color(0xCC121216),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 8.dp)
                            .border(1.dp, Color(0x38FF6600), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = Color(0xFFD1D5DB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
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
                            onClick = { currentDestination = dest },
                            icon = { Icon(dest.icon, contentDescription = navTitle) },
                            label = { Text(navTitle, fontSize = 11.sp) },
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
    strings: CourtsideStrings,
    isExpanded: Boolean,
    isSyncing: Boolean = false,
    onSelectDestination: (CourtsideDestination) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onLiveRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(if (isExpanded) 172.dp else 76.dp)
            .fillMaxHeight()
            .background(CourtsideDesignTokens.SidebarObsidian)
            .border(width = 1.dp, color = Color(0x14FFFFFF))
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
                                colors = listOf(Color(0xFFFF6600), Color(0xFFFF3D00))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsBasketball,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isExpanded) {
                    Column {
                        Text(
                            text = "EuroLeague",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Courtside",
                            color = Color(0xFFD1D5DB),
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
                                color = if (isSelected) Color.White else CourtsideDesignTokens.MutedText,
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
                            color = if (isLangSelected) Color(0x33FF6600) else Color(0xFF16161B),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isLangSelected) Color(0xFFFF6600) else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectLanguage(lang) }
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = "${lang.flagEmoji} ${lang.shortLabel}",
                                color = if (isLangSelected) Color.White else CourtsideDesignTokens.MutedText,
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
                    color = Color(0xFF16161B),
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
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
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
                                color = Color.White,
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

// =========================================================================
// TAB 1 (Ctrl+1): MEIN TEAM HUB – GLEICHE COURTSIDE SPLIT-VIEW DESIGN-SPRACHE
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyTeamDashboardScreen(
    primaryTeam: EuroLeagueTeam,
    allTeams: List<EuroLeagueTeam>,
    favoriteTeamIds: Set<String>,
    roster: List<PlayerProfile>,
    games: List<EuroLeagueGame>,
    syncStatus: String = "LIVE API • E2026",
    isTableTopPosture: Boolean,
    onSelectPrimaryTeam: (String) -> Unit,
    onVerifyRosterNow: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalCourtsideStrings.current
    val teamGames = remember(games, primaryTeam.id, primaryTeam.code) {
        games.filter {
            it.homeTeam.id == primaryTeam.id || it.awayTeam.id == primaryTeam.id ||
                it.homeTeam.code == primaryTeam.code || it.awayTeam.code == primaryTeam.code
        }
    }
    val nextGame = remember(teamGames, games) {
        teamGames.firstOrNull { it.status == GameStatus.UPCOMING || it.status == GameStatus.LIVE }
            ?: teamGames.lastOrNull()
            ?: games.first()
    }

    var remainingMillis by remember(nextGame.id) {
        mutableStateOf((nextGame.tipOffEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }
    LaunchedEffect(nextGame.id) {
        while (true) {
            remainingMillis = (nextGame.tipOffEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(1_000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Linke Spalte: Team-Selector + Letzte/Nächste Spiele im Broadcast-Card-Stil
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.myTeamHubTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Surface(
                    color = Color(0xFF1B1B21),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF2C2C34), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "${allTeams.size} ${strings.clubsSuffix}",
                        color = Color(0xFFD1D5DB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Verein-Umschalter im Pill-Design mit echten offiziellen Club-Logos
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTeams.forEach { team ->
                    val isSelected = team.id == primaryTeam.id || team.code == primaryTeam.code
                    CourtsideGlassCard(
                        isSelected = isSelected,
                        glowColor = team.primaryColor,
                        onClick = { onSelectPrimaryTeam(team.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            CourtsideClubCrest(team = team, size = 22.dp)
                            Text(
                                text = team.code,
                                color = if (isSelected) Color.White else CourtsideDesignTokens.MutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Countdown Broadcast Card ("3 Tage 15 Stunden" statt reiner Stundenanzeige + beide Mannschaftsnamen komplett)
            val countdownLabel = strings.formatTipOffCountdown(remainingMillis)

            CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CourtsideLivePill(
                            badgeText = strings.nextTipOffBadge,
                            detailText = countdownLabel,
                            badgeColor = CourtsideDesignTokens.EuroLeagueOrange
                        )
                        Text(
                            text = "${strings.roundPrefix} ${nextGame.round}",
                            color = CourtsideDesignTokens.MutedText,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CourtsideClubCrest(nextGame.homeTeam)
                            Column {
                                Text(
                                    text = nextGame.homeTeam.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = nextGame.homeTeam.arena,
                                    color = CourtsideDesignTokens.MutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            text = "VS",
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = nextGame.awayTeam.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = nextGame.awayTeam.city,
                                    color = CourtsideDesignTokens.MutedText,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            CourtsideClubCrest(nextGame.awayTeam)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { CalendarExportHelper.exportSingleGameToNativeCalendar(context, nextGame) },
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.EuroLeagueOrange),
                            modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.googleCalendarButton, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { CalendarExportHelper.exportTeamSeasonToIcsFile(context, primaryTeam, games) },
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.PillSurface),
                            modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.seasonIcsButton, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Rechte Spalte: Dual-Tone Club Header + Glowing Form/Ratings Card + Roster Table mit orangenem PIR
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CourtsideDualToneBanner(
                leftColor = primaryTeam.primaryColor,
                rightColor = primaryTeam.secondaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CourtsideClubCrest(primaryTeam, size = 48.dp)
                        Column {
                            Text(primaryTeam.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("${primaryTeam.arena} • ${primaryTeam.city}", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${primaryTeam.wins}${strings.winLetter} - ${primaryTeam.losses}${strings.lossLetter}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            primaryTeam.streak.forEach { res ->
                                val isWin = res == "S" || res == "W"
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isWin) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.NeonMissedRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isWin) strings.winLetter else strings.lossLetter,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Roster Header inkl. Live-Prüfstatus & Sofort-Check-Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${strings.activeRosterHeader} (${roster.size} ${strings.playersWord})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CourtsideDesignTokens.MutedText)
                }
                Surface(
                    color = Color(0x2200E676),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, CourtsideDesignTokens.NeonMadeGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onVerifyRosterNow() }
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        text = strings.rosterVerifiedBadge,
                        color = CourtsideDesignTokens.NeonMadeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(strings.playerColumn, Modifier.weight(2.6f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp)
                        Text("POS", Modifier.weight(0.8f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("PPG", Modifier.weight(1f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("RPG", Modifier.weight(1f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("APG", Modifier.weight(1f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("PIR ⌄", Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0x1AFFFFFF))
                }
                itemsIndexed(roster, key = { _, p -> p.id }) { idx, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(2.6f)) {
                            Text("${idx + 1}. #${p.number} ${p.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            val injColor = when (p.injuryStatus) {
                                InjuryStatus.FIT -> CourtsideDesignTokens.NeonMadeGreen
                                InjuryStatus.GAME_TIME_DECISION, InjuryStatus.QUESTIONABLE -> CourtsideDesignTokens.EuroLeagueOrange
                                InjuryStatus.OUT -> CourtsideDesignTokens.NeonMissedRed
                            }
                            val localizedInjuryLabel = p.injuryStatus.localizedLabel(strings)
                            val injText = if (p.injuryDetail.isNotBlank()) {
                                "$localizedInjuryLabel • ${p.injuryDetail}"
                            } else {
                                "$localizedInjuryLabel • ${p.nationality} • ${p.heightCm}cm"
                            }
                            Text(injText, color = injColor, fontSize = 10.sp)
                        }
                        Text(p.position, Modifier.weight(0.8f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(p.ppg.toString(), Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(p.rpg.toString(), Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(p.apg.toString(), Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(p.pir.toString(), Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                }
            }
        }
    }
}

// =========================================================================
// TAB 3 (Ctrl+3): STANDINGS & PLAYOFF BRACKET IM COURTSIDE SPLIT-VIEW DESIGN
// =========================================================================

@Composable
private fun StandingsAndBracketScreen(
    standings: List<EuroLeagueTeam>,
    bracketSeries: List<PlayoffSeries>,
    favoriteIds: Set<String>
) {
    val strings = LocalCourtsideStrings.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Links: Playoff & Play-In Series Cards im Broadcast-Karten-Stil
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(
                text = strings.playoffBracketTitle,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bracketSeries, key = { it.id }) { series ->
                    CourtsideGlassCard(isSelected = series.stage == "FINAL_FOUR_SEMI", modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            CourtsideLivePill(
                                badgeText = series.stage.replace("_", " "),
                                detailText = series.label,
                                badgeColor = if (series.stage == "PLAY_IN") CourtsideDesignTokens.EuroLeagueOrange else Color(0xFF00A859)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CourtsideClubCrest(series.homeTeam, size = 34.dp)
                                    Text(
                                        text = "#${series.homeSeed} ${series.homeTeam.name}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                                Text(
                                    text = "${series.homeWins} : ${series.awayWins}",
                                    color = CourtsideDesignTokens.EuroLeagueOrange,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "#${series.awaySeed} ${series.awayTeam.name}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CourtsideClubCrest(series.awayTeam, size = 34.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rechts: Dual-Tone Leader Banner + Sleek Standings Table mit orangener Diff/PIR-Spalte
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val top1 = standings.first()
            val top2 = standings[1]
            CourtsideDualToneBanner(
                leftColor = top1.primaryColor,
                rightColor = top2.primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CourtsideClubCrest(top1, size = 40.dp)
                        Column {
                            Text("#1 ${top1.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("${top1.wins}${strings.winLetter} - ${top1.losses}${strings.lossLetter}", color = CourtsideDesignTokens.NeonMadeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(strings.regularSeasonLabel, color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("#2 ${top2.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("${top2.wins}${strings.winLetter} - ${top2.losses}${strings.lossLetter}", color = CourtsideDesignTokens.NeonMadeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        CourtsideClubCrest(top2, size = 40.dp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.standingsHeader, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CourtsideDesignTokens.MutedText)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(strings.clubColumn, Modifier.weight(3f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp)
                        Text(strings.winsColumn, Modifier.weight(0.8f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(strings.lossesColumn, Modifier.weight(0.8f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(strings.offRatingColumn, Modifier.weight(1f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("+/- ⌄", Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0x1AFFFFFF))
                }
                itemsIndexed(standings, key = { _, t -> t.id }) { index, team ->
                    val rank = index + 1
                    val dotColor = when (rank) {
                        in 1..6 -> CourtsideDesignTokens.NeonMadeGreen
                        in 7..10 -> CourtsideDesignTokens.EuroLeagueOrange
                        else -> Color(0xFF48484A)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(3f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                            CourtsideClubCrest(team = team, size = 22.dp)
                            Text("$rank. ${team.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(team.wins.toString(), Modifier.weight(0.8f), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(team.losses.toString(), Modifier.weight(0.8f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(team.offensiveRating.toString(), Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(
                            text = if (team.pointDiff >= 0) "+${team.pointDiff}" else "${team.pointDiff}",
                            modifier = Modifier.weight(1f),
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                }
            }
        }
    }
}

// =========================================================================
// TAB 4 (Ctrl+4): ANALYTICS, RADAR-CHART & SEASON LEADERS IM COURTSIDE DESIGN
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonLeadersAndComparisonScreen(
    players: List<PlayerProfile>,
    teams: List<EuroLeagueTeam>,
    games: List<EuroLeagueGame> = emptyList(),
    teamA: EuroLeagueTeam,
    teamB: EuroLeagueTeam,
    onSelectTeamA: (String) -> Unit = {},
    onSelectTeamB: (String) -> Unit
) {
    val strings = LocalCourtsideStrings.current

    val h2hGames = remember(games, teamA.id, teamB.id) {
        games.filter { g ->
            val aHome = g.homeTeam.id == teamA.id || g.homeTeam.code == teamA.code
            val bAway = g.awayTeam.id == teamB.id || g.awayTeam.code == teamB.code
            val bHome = g.homeTeam.id == teamB.id || g.homeTeam.code == teamB.code
            val aAway = g.awayTeam.id == teamA.id || g.awayTeam.code == teamA.code
            (aHome && bAway) || (bHome && aAway)
        }.sortedBy { it.round }
    }

    val gpA = (teamA.wins + teamA.losses).coerceAtLeast(1)
    val gpB = (teamB.wins + teamB.losses).coerceAtLeast(1)
    val ppgA = ((teamA.pointsFor.toFloat() / gpA) * 10f).toInt() / 10f
    val ppgB = ((teamB.pointsFor.toFloat() / gpB) * 10f).toInt() / 10f
    val oppgA = ((teamA.pointsAgainst.toFloat() / gpA) * 10f).toInt() / 10f
    val oppgB = ((teamB.pointsAgainst.toFloat() / gpB) * 10f).toInt() / 10f

    data class ComparisonMetric(
        val label: String,
        val displayA: String,
        val displayB: String,
        val rawA: Float,
        val rawB: Float,
        val lowerIsBetter: Boolean = false
    )

    val comparisonMetrics = remember(teamA, teamB, strings) {
        listOf(
            ComparisonMetric(
                label = "${strings.winsColumn}-${strings.lossesColumn}",
                displayA = "${teamA.wins}-${teamA.losses}",
                displayB = "${teamB.wins}-${teamB.losses}",
                rawA = teamA.wins.toFloat().coerceAtLeast(0.5f),
                rawB = teamB.wins.toFloat().coerceAtLeast(0.5f)
            ),
            ComparisonMetric(
                label = "PPG (PTS / Game)",
                displayA = "$ppgA",
                displayB = "$ppgB",
                rawA = ppgA,
                rawB = ppgB
            ),
            ComparisonMetric(
                label = "OPPG (Opp PTS / Game)",
                displayA = "$oppgA",
                displayB = "$oppgB",
                rawA = oppgA,
                rawB = oppgB,
                lowerIsBetter = true
            ),
            ComparisonMetric(
                label = "+/- Diff",
                displayA = if (teamA.pointDiff >= 0) "+${teamA.pointDiff}" else "${teamA.pointDiff}",
                displayB = if (teamB.pointDiff >= 0) "+${teamB.pointDiff}" else "${teamB.pointDiff}",
                rawA = (teamA.pointDiff + 150).toFloat().coerceAtLeast(10f),
                rawB = (teamB.pointDiff + 150).toFloat().coerceAtLeast(10f)
            ),
            ComparisonMetric(
                label = "OFF RTG",
                displayA = "${teamA.offensiveRating}",
                displayB = "${teamB.offensiveRating}",
                rawA = teamA.offensiveRating,
                rawB = teamB.offensiveRating
            ),
            ComparisonMetric(
                label = "DEF RTG",
                displayA = "${teamA.defensiveRating}",
                displayB = "${teamB.defensiveRating}",
                rawA = teamA.defensiveRating,
                rawB = teamB.defensiveRating,
                lowerIsBetter = true
            ),
            ComparisonMetric(
                label = "3P%",
                displayA = "${teamA.threePointPct}%",
                displayB = "${teamB.threePointPct}%",
                rawA = teamA.threePointPct,
                rawB = teamB.threePointPct
            ),
            ComparisonMetric(
                label = "2P%  •  REB%",
                displayA = "${teamA.twoPointPct}% • ${teamA.reboundRate}%",
                displayB = "${teamB.twoPointPct}% • ${teamB.reboundRate}%",
                rawA = teamA.twoPointPct + teamA.reboundRate,
                rawB = teamB.twoPointPct + teamB.reboundRate
            )
        )
    }

    val matchupAndLeaguePlayers = remember(players, teamA.id, teamB.id) {
        val teamABStars = players.filter {
            it.teamId == teamA.id || it.teamId == teamA.code ||
                it.teamId == teamB.id || it.teamId == teamB.code
        }.sortedByDescending { it.pir }.take(8)
        val others = players.sortedByDescending { it.pir }.filterNot { p -> teamABStars.any { it.id == p.id } }
        teamABStars + others
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Linke Spalte: Freie Team-A & Team-B Auswahl + Direkte Saison-Duelle + 8 Head-to-Head Vergleichs-Balken
        Column(
            modifier = Modifier
                .weight(1.08f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.teamAnalyticsTitle,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            // Team A & Team B Auswahl-Karte
            CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TEAM A (ORANGE): ${teamA.name}",
                        color = CourtsideDesignTokens.EuroLeagueOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(teams, key = { "teamA_${it.id}" }) { candidate ->
                            val selected = candidate.id == teamA.id
                            Surface(
                                color = if (selected) Color(0x38FF6600) else Color(0xFF1B1B21),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) Color(0xFFFF6600) else Color(0xFF2C2C34),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectTeamA(candidate.id) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    CourtsideClubCrest(team = candidate, size = 20.dp)
                                    Text(
                                        text = candidate.code,
                                        color = if (selected) Color.White else Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0x1AFFFFFF))

                    Text(
                        text = "TEAM B (GRÜN): ${teamB.name}",
                        color = CourtsideDesignTokens.NeonMadeGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(teams, key = { "teamB_${it.id}" }) { candidate ->
                            val selected = candidate.id == teamB.id
                            Surface(
                                color = if (selected) Color(0x3300E676) else Color(0xFF1B1B21),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) CourtsideDesignTokens.NeonMadeGreen else Color(0xFF2C2C34),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectTeamB(candidate.id) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    CourtsideClubCrest(team = candidate, size = 20.dp)
                                    Text(
                                        text = candidate.code,
                                        color = if (selected) Color.White else Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Direkte Saison-Duelle + 8 Visuelle Statistik-Vergleichsbalken
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (h2hGames.isNotEmpty()) {
                    item {
                        CourtsideGlassCard(isSelected = false, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "H2H • ${teamA.code} vs ${teamB.code}",
                                    color = CourtsideDesignTokens.EuroLeagueOrange,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                                h2hGames.forEach { g ->
                                    val statusText = when (g.status) {
                                        GameStatus.FINAL -> "${g.homeTeam.code} ${g.homeScore} : ${g.awayScore} ${g.awayTeam.code} (${strings.statusFinal})"
                                        GameStatus.LIVE -> "${g.homeTeam.code} ${g.homeScore} : ${g.awayScore} ${g.awayTeam.code} (${strings.statusLive})"
                                        GameStatus.UPCOMING -> "${g.homeTeam.code} vs ${g.awayTeam.code} (${strings.statusUpcoming})"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${strings.roundPrefix} ${g.round}",
                                            color = CourtsideDesignTokens.MutedText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = statusText,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(comparisonMetrics, key = { it.label }) { metric ->
                    val aLeads = if (metric.lowerIsBetter) metric.rawA <= metric.rawB else metric.rawA >= metric.rawB
                    val bLeads = if (metric.lowerIsBetter) metric.rawB <= metric.rawA else metric.rawB >= metric.rawA
                    val total = (metric.rawA + metric.rawB).coerceAtLeast(1f)
                    val fractionA = if (metric.lowerIsBetter) {
                        (metric.rawB / total).coerceIn(0.15f, 0.85f)
                    } else {
                        (metric.rawA / total).coerceIn(0.15f, 0.85f)
                    }

                    Surface(
                        color = Color(0xFF17171C),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = metric.displayA,
                                    color = if (aLeads) CourtsideDesignTokens.EuroLeagueOrange else Color(0xFFD1D5DB),
                                    fontWeight = if (aLeads) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = metric.label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.4f)
                                )
                                Text(
                                    text = metric.displayB,
                                    color = if (bLeads) CourtsideDesignTokens.NeonMadeGreen else Color(0xFFD1D5DB),
                                    fontWeight = if (bLeads) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF26262E))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(fractionA)
                                        .fillMaxHeight()
                                        .background(CourtsideDesignTokens.EuroLeagueOrange)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .weight((1f - fractionA).coerceAtLeast(0.1f))
                                        .fillMaxHeight()
                                        .background(CourtsideDesignTokens.NeonMadeGreen)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rechte Spalte: Dual-Tone Banner + Beschriftetes 5-Achsen-Radar + Top-Performer
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CourtsideDualToneBanner(
                leftColor = teamA.primaryColor,
                rightColor = teamB.primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CourtsideClubCrest(teamA, size = 38.dp)
                        Column {
                            Text(teamA.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp)
                            Text("${teamA.wins}${strings.winLetter}-${teamA.losses}${strings.lossLetter} • OFF ${teamA.offensiveRating}", color = CourtsideDesignTokens.EuroLeagueOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("VS", color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f, fill = false)) {
                            Text(teamB.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.End)
                            Text("${teamB.wins}${strings.winLetter}-${teamB.losses}${strings.lossLetter} • OFF ${teamB.offensiveRating}", color = CourtsideDesignTokens.NeonMadeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        CourtsideClubCrest(teamB, size = 38.dp)
                    }
                }
            }

            // Beschriftetes 5-Achsen-Radar-Diagramm mit Legende
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF151519))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TeamComparisonRadarCanvas(teamA = teamA, teamB = teamB)

                    // Top Vertex Label: OFF RTG
                    RadarAxisBadge(
                        title = "OFF",
                        valA = "${teamA.offensiveRating}",
                        valB = "${teamB.offensiveRating}",
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    // Right Vertex Label: DEF RTG
                    RadarAxisBadge(
                        title = "DEF",
                        valA = "${teamA.defensiveRating}",
                        valB = "${teamB.defensiveRating}",
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    // Bottom-Right Vertex Label: 3P%
                    RadarAxisBadge(
                        title = "3P%",
                        valA = "${teamA.threePointPct}%",
                        valB = "${teamB.threePointPct}%",
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                    // Bottom-Left Vertex Label: 2P%
                    RadarAxisBadge(
                        title = "2P%",
                        valA = "${teamA.twoPointPct}%",
                        valB = "${teamB.twoPointPct}%",
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                    // Left Vertex Label: REB%
                    RadarAxisBadge(
                        title = "REB%",
                        valA = "${teamA.reboundRate}%",
                        valB = "${teamB.reboundRate}%",
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CourtsideDesignTokens.EuroLeagueOrange))
                        Text("${teamA.code} (${teamA.name})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CourtsideDesignTokens.NeonMadeGreen))
                        Text("${teamB.code} (${teamB.name})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${teamA.code} vs ${teamB.code} • ${strings.seasonLeadersHeader}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CourtsideDesignTokens.MutedText)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(matchupAndLeaguePlayers, key = { _, p -> p.id }) { idx, p ->
                    val isTeamA = p.teamId == teamA.id || p.teamId == teamA.code
                    val isTeamB = p.teamId == teamB.id || p.teamId == teamB.code
                    val teamColor = when {
                        isTeamA -> CourtsideDesignTokens.EuroLeagueOrange
                        isTeamB -> CourtsideDesignTokens.NeonMadeGreen
                        else -> Color(0xFF9CA3AF)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(2.6f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${idx + 1}.",
                                color = CourtsideDesignTokens.MutedText,
                                fontSize = 11.sp
                            )
                            Text(
                                text = p.teamId,
                                color = teamColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = p.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text("PPG ${p.ppg}", Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text("RPG ${p.rpg}", Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text("APG ${p.apg}", Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text("PIR ${p.pir}", Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                }
            }
        }
    }
}

@Composable
private fun RadarAxisBadge(
    title: String,
    valA: String,
    valB: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xCC1B1B21),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            Text(text = valA, color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text(text = "|", color = Color(0xFF6B7280), fontSize = 10.sp)
            Text(text = valB, color = CourtsideDesignTokens.NeonMadeGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TeamComparisonRadarCanvas(teamA: EuroLeagueTeam, teamB: EuroLeagueTeam) {
    // 5 Radar Axes matching the 5 RadarAxisBadges:
    // 0 (Top): OFF RTG, 1 (Right): DEF Efficiency (215 - defRtg), 2 (Bottom-Right): 3P%, 3 (Bottom-Left): 2P%, 4 (Left): REB%
    val metricsA = listOf(
        ((teamA.offensiveRating - 95f) / 30f).coerceIn(0.25f, 0.96f),
        ((125f - teamA.defensiveRating) / 25f).coerceIn(0.25f, 0.96f),
        ((teamA.threePointPct - 28f) / 16f).coerceIn(0.25f, 0.96f),
        ((teamA.twoPointPct - 45f) / 18f).coerceIn(0.25f, 0.96f),
        ((teamA.reboundRate - 40f) / 20f).coerceIn(0.25f, 0.96f)
    )
    val metricsB = listOf(
        ((teamB.offensiveRating - 95f) / 30f).coerceIn(0.25f, 0.96f),
        ((125f - teamB.defensiveRating) / 25f).coerceIn(0.25f, 0.96f),
        ((teamB.threePointPct - 28f) / 16f).coerceIn(0.25f, 0.96f),
        ((teamB.twoPointPct - 45f) / 18f).coerceIn(0.25f, 0.96f),
        ((teamB.reboundRate - 40f) / 20f).coerceIn(0.25f, 0.96f)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.75f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x38FF5E00), Color.Transparent),
                center = Offset(size.width * 0.25f, size.height * 0.5f),
                radius = size.width * 0.35f
            ),
            center = Offset(size.width * 0.25f, size.height * 0.5f),
            radius = size.width * 0.35f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3000D26E), Color.Transparent),
                center = Offset(size.width * 0.75f, size.height * 0.5f),
                radius = size.width * 0.35f
            ),
            center = Offset(size.width * 0.75f, size.height * 0.5f),
            radius = size.width * 0.35f
        )

        for (level in 1..4) {
            val r = radius * (level / 4f)
            val webPath = Path()
            for (i in 0 until 5) {
                val angle = Math.toRadians((i * 72.0) - 90.0)
                val pt = Offset(
                    center.x + (r * cos(angle)).toFloat(),
                    center.y + (r * sin(angle)).toFloat()
                )
                if (i == 0) webPath.moveTo(pt.x, pt.y) else webPath.lineTo(pt.x, pt.y)
            }
            webPath.close()
            drawPath(webPath, color = Color(0x38FFFFFF), style = Stroke(width = 1.5f))
        }

        // Draw 5 spoke lines from center to vertices
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72.0) - 90.0)
            val vertex = Offset(
                center.x + (radius * cos(angle)).toFloat(),
                center.y + (radius * sin(angle)).toFloat()
            )
            drawLine(color = Color(0x28FFFFFF), start = center, end = vertex, strokeWidth = 1.2f)
        }

        fun buildPolygon(values: List<Float>): Path {
            val p = Path()
            values.forEachIndexed { i, v ->
                val angle = Math.toRadians((i * 72.0) - 90.0)
                val pt = Offset(
                    center.x + (radius * v * cos(angle)).toFloat(),
                    center.y + (radius * v * sin(angle)).toFloat()
                )
                if (i == 0) p.moveTo(pt.x, pt.y) else p.lineTo(pt.x, pt.y)
            }
            p.close()
            return p
        }

        val pathA = buildPolygon(metricsA)
        val pathB = buildPolygon(metricsB)

        drawPath(pathA, color = Color(0x44FF6600))
        drawPath(pathA, color = Color(0xFFFF6600), style = Stroke(width = 3.5f))

        drawPath(pathB, color = Color(0x4400E676))
        drawPath(pathB, color = Color(0xFF00E676), style = Stroke(width = 3.5f))
    }
}

// =========================================================================
// TAB 5 (Ctrl+5): SETTINGS, 6-LANGUAGE SWITCHER & PUSH NOTIFICATION STUDIO
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsAndNotificationTestScreen(
    primaryTeam: EuroLeagueTeam,
    games: List<EuroLeagueGame>,
    activeLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    notificationManager: EuroLeagueNotificationManager
) {
    val strings = LocalCourtsideStrings.current
    var tipOffEnabled by rememberSaveable { mutableStateOf(true) }
    var halftimeFinalEnabled by rememberSaveable { mutableStateOf(true) }
    var closeGameAlertEnabled by rememberSaveable { mutableStateOf(true) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val sampleLiveGame = games.firstOrNull { it.status == GameStatus.LIVE } ?: games.first()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 6-Language Selector Card (EN, DE, FR, IT, ES, EL)
            CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.languageSectionTitle,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color(0x26FF6600),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, Color(0x66FF6600), RoundedCornerShape(8.dp))
                        ) {
                            val versionLabel = when (activeLanguage) {
                                AppLanguage.EN -> "Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                AppLanguage.DE -> "Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                AppLanguage.FR -> "Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                AppLanguage.IT -> "Versione v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                AppLanguage.ES -> "Versión v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                AppLanguage.EL -> "Έκδοση v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                            }
                            Text(
                                text = versionLabel,
                                color = CourtsideDesignTokens.EuroLeagueOrange,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = strings.languageSectionSubtitle,
                        color = CourtsideDesignTokens.MutedText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = lang == activeLanguage
                            Surface(
                                color = if (isSelected) Color(0x33FF6600) else Color(0xFF1B1B21),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFFF6600) else Color(0xFF2C2C34),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectLanguage(lang) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(text = lang.flagEmoji, fontSize = 14.sp)
                                    Text(
                                        text = lang.nativeName,
                                        color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(strings.notificationChannelsTitle, style = MaterialTheme.typography.titleLarge, color = Color.White)

            CourtsideGlassCard(isSelected = tipOffEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "WORKMANAGER",
                    title = "${strings.tipOffChannelTitle} (${primaryTeam.code})",
                    subtitle = strings.tipOffChannelSubtitle,
                    checked = tipOffEnabled,
                    onCheckedChange = { tipOffEnabled = it }
                )
            }
            CourtsideGlassCard(isSelected = halftimeFinalEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "SCORES",
                    title = strings.scoresChannelTitle,
                    subtitle = strings.scoresChannelSubtitle,
                    checked = halftimeFinalEnabled,
                    onCheckedChange = { halftimeFinalEnabled = it }
                )
            }
            CourtsideGlassCard(isSelected = closeGameAlertEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "CRUNCH TIME",
                    title = strings.closeGameChannelTitle,
                    subtitle = strings.closeGameChannelSubtitle,
                    checked = closeGameAlertEnabled,
                    onCheckedChange = { closeGameAlertEnabled = it }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val previewPayload = remember(sampleLiveGame, activeLanguage) {
                notificationManager.buildTestPushContent(sampleLiveGame, activeLanguage)
            }
            CourtsideDualToneBanner(
                leftColor = sampleLiveGame.homeTeam.primaryColor,
                rightColor = sampleLiveGame.awayTeam.primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    CourtsideLivePill(
                        badgeText = strings.livePushPreviewBadge,
                        detailText = "Q${sampleLiveGame.currentQuarter} ${sampleLiveGame.remainingClock}"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = previewPayload.title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = previewPayload.bigText,
                        color = CourtsideDesignTokens.MutedText,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = {
                    notificationManager.sendInstantTestPushNotification(sampleLiveGame, activeLanguage)
                    statusBanner = "${strings.testPushSentPrefix} '${previewPayload.title}'"
                },
                colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.EuroLeagueOrange),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.sendTestPushButton,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            if (statusBanner != null) {
                CourtsideGlassCard(
                    isSelected = true,
                    glowColor = CourtsideDesignTokens.NeonMadeGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusBanner!!,
                        color = CourtsideDesignTokens.NeonMadeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelToggleRow(
    badge: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CourtsideLivePill(badgeText = badge, detailText = if (checked) "ACTIVE" else "MUTED")
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = CourtsideDesignTokens.MutedText, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CourtsideDesignTokens.EuroLeagueOrange
            ),
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        )
    }
}
