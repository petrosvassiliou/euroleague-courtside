package com.euroleague.courtside.ui.navigation

import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.HorizontalDivider
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
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.data.repository.InjuryStatus
import com.euroleague.courtside.data.repository.PlayerProfile
import com.euroleague.courtside.data.repository.PlayoffSeries
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
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

enum class CourtsideDestination(
    val title: String,
    val shortcutHint: String,
    val icon: ImageVector
) {
    MY_TEAM("Mein Team", "Ctrl+1", Icons.Default.Home),
    MATCHDAY("EuroLeagues", "Ctrl+2", Icons.Default.SportsBasketball),
    STANDINGS_PLAYOFFS("Standings", "Ctrl+3", Icons.Default.EmojiEvents),
    STATISTICS("Analytics", "Ctrl+4", Icons.Default.Analytics),
    SETTINGS("Settings", "Ctrl+5", Icons.Default.Settings)
}

/**
 * Gesamtes App-Scaffold in der einheitlichen "Courtside UI" Design-Sprache:
 * - Links auf Desktop/Foldables: Obsidian-Sidebar (`#0B0B0E`) mit leuchtendem Basketball-Brand-Icon,
 *   orangenen Glow-Icon-Buttons und `Ctrl+1`..`Ctrl+5` Shortcut-Pills.
 * - Unten auf Smartphones (`COMPACT`): Abgedunkelte Broadcast-NavigationBar.
 * - Alle 5 Screens nutzen das 2-Spalten-Broadcast-Layout (Links Liste/Karten + Rechts Dual-Tone-Banner,
 *   leuchtendes Canvas-Chart und dunkle Datentabelle mit orangener PIR-Spalte).
 */
@Composable
fun MainAppScaffold(
    repository: EuroLeagueMockRepository,
    notificationManager: EuroLeagueNotificationManager
) {
    val context = LocalContext.current
    val favoriteIds by repository.favoriteTeamIds.collectAsState()
    val primaryTeamId by repository.selectedTeamId.collectAsState()
    val games by repository.games.collectAsState()

    val primaryTeam = remember(primaryTeamId, repository.teams) {
        repository.teams.find { it.id == primaryTeamId } ?: repository.teams.first()
    }

    var currentDestination by rememberSaveable { mutableStateOf(CourtsideDestination.MATCHDAY) }
    var selectedRound by rememberSaveable { mutableIntStateOf(33) }
    var filterOnlyFavorites by rememberSaveable { mutableStateOf(false) }
    var comparisonTeamBId by rememberSaveable { mutableStateOf("PAO") }

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
    val showShortcutPills = widthClass == WindowWidthSizeClass.EXPANDED

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                        repository.simulateLiveRefresh(); true
                    }
                    event.key == Key.DirectionLeft -> {
                        selectedRound = (selectedRound - 1).coerceAtLeast(1); true
                    }
                    event.key == Key.DirectionRight -> {
                        selectedRound = (selectedRound + 1).coerceAtMost(34); true
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
                    isExpanded = widthClass == WindowWidthSizeClass.EXPANDED,
                    onSelectDestination = { currentDestination = it },
                    onLiveRefresh = { repository.simulateLiveRefresh() }
                )
            }

            // Haupt-Arbeitsbereich
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (currentDestination) {
                    CourtsideDestination.MY_TEAM -> MyTeamDashboardScreen(
                        primaryTeam = primaryTeam,
                        allTeams = repository.teams,
                        favoriteTeamIds = favoriteIds,
                        roster = repository.players.filter { it.teamId == primaryTeam.id },
                        games = games,
                        isTableTopPosture = isTableTopPosture,
                        onSelectPrimaryTeam = { repository.selectPrimaryTeam(it) }
                    )

                    CourtsideDestination.MATCHDAY -> MatchCenterScreen(
                        games = games,
                        selectedRound = selectedRound,
                        filterOnlyFavorites = filterOnlyFavorites,
                        favoriteTeamIds = favoriteIds,
                        hingeInBookPosture = isBookPosture,
                        notificationManager = notificationManager,
                        onPreviousRound = { selectedRound = (selectedRound - 1).coerceAtLeast(1) },
                        onNextRound = { selectedRound = (selectedRound + 1).coerceAtMost(34) },
                        onToggleFavoriteFilter = { filterOnlyFavorites = !filterOnlyFavorites },
                        onTriggerLiveRefresh = { repository.simulateLiveRefresh() },
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

                    CourtsideDestination.STATISTICS -> SeasonLeadersAndComparisonScreen(
                        players = repository.players,
                        teams = repository.teams,
                        teamA = primaryTeam,
                        teamB = repository.teams.find { it.id == comparisonTeamBId } ?: repository.teams.first(),
                        onSelectTeamB = { comparisonTeamBId = it }
                    )

                    CourtsideDestination.SETTINGS -> SettingsAndNotificationTestScreen(
                        primaryTeam = primaryTeam,
                        games = games,
                        notificationManager = notificationManager
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
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentDestination = dest },
                        icon = { Icon(dest.icon, contentDescription = dest.title) },
                        label = { Text(dest.title, fontSize = 11.sp) },
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

/**
 * Linke Courtside-Sidebar ohne sichtbare Ctrl+1..5 Badges (Tastatur-Shortcuts laufen unsichtbar im Hintergrund):
 * - Oben: Leuchtendes EuroLeague-Basketball-Logo
 * - Mitte: Clean zentrierte Icon-Buttons mit orangenem Radial-Glow bei aktivem Tab
 */
@Composable
private fun CourtsideLeftSidebar(
    currentDestination: CourtsideDestination,
    isExpanded: Boolean,
    onSelectDestination: (CourtsideDestination) -> Unit,
    onLiveRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(if (isExpanded) 168.dp else 76.dp)
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
                modifier = Modifier.padding(bottom = 28.dp)
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
                                contentDescription = dest.title,
                                tint = if (isSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.MutedText,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        if (isExpanded) {
                            Text(
                                text = dest.title,
                                color = if (isSelected) Color.White else CourtsideDesignTokens.MutedText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Bottom Live-Refresh Trigger
        Surface(
            color = Color(0x24FF6600),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x66FF6600), RoundedCornerShape(12.dp))
                .clickable { onLiveRefresh() }
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(
                text = if (showShortcutPills) "⚡ Live-Sim (R)" else "⚡",
                color = CourtsideDesignTokens.EuroLeagueOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 10.dp)
            )
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
    isTableTopPosture: Boolean,
    onSelectPrimaryTeam: (String) -> Unit
) {
    val context = LocalContext.current
    val nextGame = games.firstOrNull {
        it.homeTeam.id == primaryTeam.id || it.awayTeam.id == primaryTeam.id
    } ?: games.first()

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
            Text(
                text = "My Team Hub",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            // Verein-Umschalter im Pill-Design
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTeams.forEach { team ->
                    val isSelected = team.id == primaryTeam.id
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

            // Countdown Broadcast Card
            val hrs = TimeUnit.MILLISECONDS.toHours(remainingMillis)
            val mins = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
            val secs = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60

            CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CourtsideLivePill(
                            badgeText = "NEXT TIP-OFF",
                            detailText = "%02d:%02d:%02d".format(hrs, mins, secs),
                            badgeColor = CourtsideDesignTokens.EuroLeagueOrange
                        )
                        Text(
                            text = "Round ${nextGame.round}",
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CourtsideClubCrest(nextGame.homeTeam)
                            Column {
                                Text(nextGame.homeTeam.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(nextGame.homeTeam.arena, color = CourtsideDesignTokens.MutedText, fontSize = 11.sp)
                            }
                        }
                        Text("VS", color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold)
                        CourtsideClubCrest(nextGame.awayTeam)
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
                            Text("Google Kalender", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { CalendarExportHelper.exportTeamSeasonToIcsFile(context, primaryTeam, games) },
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.PillSurface),
                            modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saison .ics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        Text("${primaryTeam.wins}W - ${primaryTeam.losses}L", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            primaryTeam.streak.forEach { res ->
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (res == "S") CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.NeonMissedRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(res, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Roster Table im "Player boxscore ⌄"-Stil mit orangener PIR-Spalte
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active Club Roster", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CourtsideDesignTokens.MutedText)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Player", Modifier.weight(2.6f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp)
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
                            val injColor = if (p.injuryStatus == InjuryStatus.FIT) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.EuroLeagueOrange
                            Text(p.injuryStatus.label, color = injColor, fontSize = 10.sp)
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
                text = "Playoff & Play-In Bracket",
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CourtsideClubCrest(series.homeTeam, size = 34.dp)
                                    Text("#${series.homeSeed} ${series.homeTeam.code}", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${series.homeWins} : ${series.awayWins}",
                                    color = CourtsideDesignTokens.EuroLeagueOrange,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("#${series.awaySeed} ${series.awayTeam.code}", color = Color.White, fontWeight = FontWeight.Bold)
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
                            Text("${top1.wins}W - ${top1.losses}L", color = CourtsideDesignTokens.NeonMadeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("REGULAR SEASON", color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("#2 ${top2.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("${top2.wins}W - ${top2.losses}L", color = CourtsideDesignTokens.NeonMadeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        CourtsideClubCrest(top2, size = 40.dp)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EuroLeague Standings (1–6 Playoffs • 7–10 Play-In)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CourtsideDesignTokens.MutedText)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Club", Modifier.weight(3f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp)
                        Text("W", Modifier.weight(0.8f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("L", Modifier.weight(0.8f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("OFF", Modifier.weight(1f), color = CourtsideDesignTokens.MutedText, fontSize = 12.sp, textAlign = TextAlign.Center)
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

@Composable
private fun SeasonLeadersAndComparisonScreen(
    players: List<PlayerProfile>,
    teams: List<EuroLeagueTeam>,
    teamA: EuroLeagueTeam,
    teamB: EuroLeagueTeam,
    onSelectTeamB: (String) -> Unit
) {
    val topPirPlayers = remember(players) { players.sortedByDescending { it.pir } }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Links: Team Matchup Cards zum Auswählen des Vergleichsteams
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Team Analytics Comparison", style = MaterialTheme.typography.titleLarge, color = Color.White)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(teams.filter { it.id != teamA.id }, key = { it.id }) { candidate ->
                    val isSelected = candidate.id == teamB.id
                    CourtsideGlassCard(
                        isSelected = isSelected,
                        onClick = { onSelectTeamB(candidate.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            CourtsideLivePill(
                                badgeText = if (isSelected) "ACTIVE" else "COMPARE",
                                detailText = "${teamA.code} vs ${candidate.code}"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CourtsideClubCrest(teamA, size = 34.dp)
                                Text(
                                    text = "OFF ${teamA.offensiveRating} vs ${candidate.offensiveRating}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                CourtsideClubCrest(candidate, size = 34.dp)
                            }
                        }
                    }
                }
            }
        }

        // Rechts: Dual-Tone Banner + Glowing Radar Canvas + Season Leaders Table
        Column(
            modifier = Modifier
                .weight(1.2f)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CourtsideClubCrest(teamA, size = 38.dp)
                        Text(teamA.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("RADAR", color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(teamB.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        CourtsideClubCrest(teamB, size = 38.dp)
                    }
                }
            }

            // Glowing Radar Chart Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF151519))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                TeamComparisonRadarCanvas(teamA = teamA, teamB = teamB)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Season MVP & PIR Leaders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = CourtsideDesignTokens.MutedText)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(topPirPlayers, key = { _, p -> p.id }) { idx, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${idx + 1}. ${p.name} (${p.teamId})", Modifier.weight(2.6f), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("PPG ${p.ppg}", Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("RPG ${p.rpg}", Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text("APG ${p.apg}", Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Text(p.pir.toString(), Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0x0DFFFFFF))
                }
            }
        }
    }
}

@Composable
private fun TeamComparisonRadarCanvas(teamA: EuroLeagueTeam, teamB: EuroLeagueTeam) {
    val metricsA = listOf(
        (teamA.offensiveRating / 130f).coerceIn(0.3f, 1f),
        (teamA.defensiveRating / 130f).coerceIn(0.3f, 1f),
        (teamA.pace / 85f).coerceIn(0.3f, 1f),
        (teamA.threePointPct / 50f).coerceIn(0.3f, 1f),
        (teamA.reboundRate / 65f).coerceIn(0.3f, 1f)
    )
    val metricsB = listOf(
        (teamB.offensiveRating / 130f).coerceIn(0.3f, 1f),
        (teamB.defensiveRating / 130f).coerceIn(0.3f, 1f),
        (teamB.pace / 85f).coerceIn(0.3f, 1f),
        (teamB.threePointPct / 50f).coerceIn(0.3f, 1f),
        (teamB.reboundRate / 65f).coerceIn(0.3f, 1f)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2.2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x38FF5E00), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.5f),
                radius = size.width * 0.4f
            ),
            center = Offset(size.width * 0.2f, size.height * 0.5f),
            radius = size.width * 0.4f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3000D26E), Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.5f),
                radius = size.width * 0.4f
            ),
            center = Offset(size.width * 0.8f, size.height * 0.5f),
            radius = size.width * 0.4f
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
            drawPath(webPath, color = Color(0x44FFFFFF), style = Stroke(width = 1.5f))
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
// TAB 5 (Ctrl+5): SETTINGS & PUSH NOTIFICATION STUDIO IM COURTSIDE DESIGN
// =========================================================================

@Composable
private fun SettingsAndNotificationTestScreen(
    primaryTeam: EuroLeagueTeam,
    games: List<EuroLeagueGame>,
    notificationManager: EuroLeagueNotificationManager
) {
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
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Notification Channels", style = MaterialTheme.typography.titleLarge, color = Color.White)

            CourtsideGlassCard(isSelected = tipOffEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "WORKMANAGER",
                    title = "Tip-Off-Alarm (${primaryTeam.code})",
                    subtitle = "15 Minuten vor Spielbeginn inkl. Arena & H2H",
                    checked = tipOffEnabled,
                    onCheckedChange = { tipOffEnabled = it }
                )
            }
            CourtsideGlassCard(isSelected = halftimeFinalEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "SCORES",
                    title = "Halbzeit- & Endergebnis",
                    subtitle = "Push-Update mit Viertelständen & Top-PIR-Performer",
                    checked = halftimeFinalEnabled,
                    onCheckedChange = { halftimeFinalEnabled = it }
                )
            }
            CourtsideGlassCard(isSelected = closeGameAlertEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "CRUNCH TIME",
                    title = "Close-Game-Alert (Q4 < 3:00)",
                    subtitle = "Sofort-Alarm bei <= 5 Punkten Differenz in der Schlussphase",
                    checked = closeGameAlertEnabled,
                    onCheckedChange = { closeGameAlertEnabled = it }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1.1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CourtsideDualToneBanner(
                leftColor = sampleLiveGame.homeTeam.primaryColor,
                rightColor = sampleLiveGame.awayTeam.primaryColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    CourtsideLivePill(
                        badgeText = "LIVE PUSH PREVIEW",
                        detailText = "Q${sampleLiveGame.currentQuarter} ${sampleLiveGame.remainingClock}"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${sampleLiveGame.homeTeam.name} ${sampleLiveGame.homeScore} : ${sampleLiveGame.awayScore} ${sampleLiveGame.awayTeam.name}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "BigTextStyle Notification • Channel: euroleague_close_game_channel",
                        color = CourtsideDesignTokens.MutedText,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = {
                    notificationManager.sendInstantTestPushNotification(sampleLiveGame)
                    statusBanner = "System-Notification ('${sampleLiveGame.homeTeam.code} ${sampleLiveGame.homeScore}:${sampleLiveGame.awayScore} ${sampleLiveGame.awayTeam.code}') abgefeuert!"
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
                    text = "Test-Push-Benachrichtigung senden",
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
