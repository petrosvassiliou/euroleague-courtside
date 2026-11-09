package com.euroleague.courtside.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.GameStatus
import com.euroleague.courtside.data.repository.PlayerBoxscoreStat
import com.euroleague.courtside.data.repository.ShotAttempt
import com.euroleague.courtside.i18n.LocalCourtsideStrings
import com.euroleague.courtside.notifications.EuroLeagueNotificationManager
import com.euroleague.courtside.util.CalendarExportHelper
import kotlinx.coroutines.launch

enum class BoxscoreSortColumn {
    PTS, REB, AST, STL, BLK, PIR
}

/**
 * Courtside UI Design Split-View (`ListDetailPaneScaffold`):
 * - Links: "EuroLeagues" Header mit Search-Pill + Broadcast-Spielkarten (`[LIVE | Q4 01:42]`, Vereins-Wappen links/rechts, `Q1 Q2 Q3 Q4 PIR`-Matrix in der Mitte).
 * - Rechts (gleichzeitig sichtbar ohne versteckte Tabs):
 *   1. Dual-Tone Gradient Scoreboard (`FC Bayern Munich 84 vs 82 Panathinaikos Athens`)
 *   2. Glowing Half-Court Shot-Chart (`Made` grüne Leuchtkreise, `Missed` rote Kreuze)
 *   3. `Player boxscore ⌄` mit leuchtend orangener `PIR`-Spalte (`#FF6600`).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MatchCenterScreen(
    games: List<EuroLeagueGame>,
    selectedRound: Int,
    syncStatus: String = "",
    filterOnlyFavorites: Boolean,
    favoriteTeamIds: Set<String>,
    hingeInBookPosture: Boolean,
    notificationManager: EuroLeagueNotificationManager,
    onPreviousRound: () -> Unit,
    onNextRound: () -> Unit,
    onSelectRound: (Int) -> Unit = {},
    onToggleFavoriteFilter: () -> Unit,
    onTriggerLiveRefresh: () -> Unit,
    onRefreshSelectedGameStats: (EuroLeagueGame) -> Unit = {},
    onOpenTeamStats: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = LocalCourtsideStrings.current
    val scope = rememberCoroutineScope()
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showRoundDropdown by remember { mutableStateOf(false) }

    val availableRounds = remember(games) {
        val roundsFromGames = games.map { it.round }.distinct().sorted()
        if (roundsFromGames.size >= 10) roundsFromGames else (1..38).toList()
    }

    val filteredGames = remember(games, selectedRound, filterOnlyFavorites, favoriteTeamIds, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val roundFiltered = if (q.isEmpty()) {
            games.filter { it.round == selectedRound }
        } else {
            games
        }
        val favAndSearchFiltered = roundFiltered.filter { game ->
            val matchesFav = !filterOnlyFavorites ||
                    favoriteTeamIds.contains(game.homeTeam.id) ||
                    favoriteTeamIds.contains(game.awayTeam.id) ||
                    favoriteTeamIds.contains(game.homeTeam.code) ||
                    favoriteTeamIds.contains(game.awayTeam.code)
            val matchesSearch = q.isEmpty() ||
                    game.homeTeam.name.lowercase().contains(q) ||
                    game.awayTeam.name.lowercase().contains(q) ||
                    game.homeTeam.code.lowercase().contains(q) ||
                    game.awayTeam.code.lowercase().contains(q) ||
                    game.homeTeam.city.lowercase().contains(q) ||
                    game.awayTeam.city.lowercase().contains(q)
            matchesFav && matchesSearch
        }
        if (q.isEmpty() && filterOnlyFavorites && favAndSearchFiltered.isEmpty()) {
            roundFiltered
        } else {
            favAndSearchFiltered
        }
    }

    var activeGameId by rememberSaveable {
        mutableStateOf(filteredGames.firstOrNull()?.id ?: games.firstOrNull()?.id.orEmpty())
    }

    androidx.compose.runtime.LaunchedEffect(selectedRound, filteredGames.size) {
        if (filteredGames.isNotEmpty() && filteredGames.none { it.id == activeGameId }) {
            activeGameId = filteredGames.first().id
        }
    }

    val selectedGame = filteredGames.find { it.id == activeGameId }
        ?: filteredGames.firstOrNull()
        ?: games.firstOrNull()

    androidx.compose.runtime.LaunchedEffect(selectedGame?.id, selectedGame?.boxscoreHome?.isEmpty()) {
        if (selectedGame != null) {
            onRefreshSelectedGameStats(selectedGame)
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E11))
            .padding(16.dp),
        listPane = {
            AnimatedPane {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp)
                ) {
                    // Clean Header without "LIVE API..." clutter + Wide Search Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.matchCenterTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "${strings.searchPlaceholder} (Team, Code, City)...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8E8E93),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = strings.searchPlaceholder,
                                    tint = Color(0xFFFF6600),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    Text(
                                        text = "✕",
                                        color = Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .clickable { searchQuery = "" }
                                            .padding(8.dp)
                                    )
                                }
                            } else null,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1B1B21),
                                unfocusedContainerColor = Color(0xFF1B1B21),
                                focusedBorderColor = Color(0xFFFF6600),
                                unfocusedBorderColor = Color(0xFF2C2C34),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        )
                    }

                    // Round Switcher Bar (1..38) with Direct Dropdown Picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF1B1B21),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF2C2C34), RoundedCornerShape(8.dp))
                                    .clickable { onPreviousRound() }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = "◀",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Box {
                                Surface(
                                    color = Color(0x26FF6600),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .border(1.dp, Color(0x88FF6600), RoundedCornerShape(8.dp))
                                        .clickable { showRoundDropdown = true }
                                        .pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${strings.roundPrefix} $selectedRound",
                                            color = Color(0xFFFF6600),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6600),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showRoundDropdown,
                                    onDismissRequest = { showRoundDropdown = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1B1B21))
                                        .height(360.dp)
                                ) {
                                    availableRounds.forEach { rnd ->
                                        val roundGames = games.filter { it.round == rnd }
                                        val isPlayedRound = roundGames.any { it.status == GameStatus.FINAL }
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${strings.roundPrefix} $rnd",
                                                        color = if (rnd == selectedRound) Color(0xFFFF6600) else Color.White,
                                                        fontWeight = if (rnd == selectedRound) FontWeight.ExtraBold else FontWeight.Medium,
                                                        fontSize = 13.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = if (isPlayedRound) strings.statusFinal else strings.statusUpcoming,
                                                        color = if (isPlayedRound) Color(0xFF00E676) else Color(0xFF8E8E93),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            },
                                            onClick = {
                                                showRoundDropdown = false
                                                onSelectRound(rnd)
                                            }
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0xFF1B1B21),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF2C2C34), RoundedCornerShape(8.dp))
                                    .clickable { onNextRound() }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = "▶",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            color = if (filterOnlyFavorites) Color(0x33FF6600) else Color(0xFF1B1B21),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (filterOnlyFavorites) Color(0xFFFF6600) else Color(0xFF2C2C34),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onToggleFavoriteFilter() }
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = if (filterOnlyFavorites) strings.filterFavorites else strings.filterAllGames,
                                color = if (filterOnlyFavorites) Color(0xFFFF6600) else Color(0xFFD1D5DB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredGames, key = { it.id }) { game ->
                            MatchSummaryCard(
                                game = game,
                                isSelected = game.id == selectedGame?.id,
                                onSelect = {
                                    activeGameId = game.id
                                    onRefreshSelectedGameStats(game)
                                    scope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, game.id)
                                    }
                                },
                                onExportCalendar = {
                                    CalendarExportHelper.exportSingleGameToNativeCalendar(context, game)
                                },
                                onScheduleTipOffAlarm = {
                                    notificationManager.scheduleTipOffAlarm(game, strings.language)
                                },
                                onOpenTeamStats = onOpenTeamStats
                            )
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                if (selectedGame != null) {
                    MatchDetailPane(
                        game = selectedGame,
                        onExportCalendar = {
                            CalendarExportHelper.exportSingleGameToNativeCalendar(context, selectedGame)
                        },
                        onTriggerCloseGameAlert = {
                            when (selectedGame.status) {
                                GameStatus.FINAL -> notificationManager.sendHalftimeOrFinalNotification(
                                    game = selectedGame,
                                    isFinal = true,
                                    language = strings.language
                                )
                                GameStatus.UPCOMING -> notificationManager.scheduleTipOffAlarm(
                                    game = selectedGame,
                                    language = strings.language
                                )
                                GameStatus.LIVE -> {
                                    if (selectedGame.isCloseGameFourthQuarter) {
                                        notificationManager.evaluateAndSendCloseGameAlert(selectedGame, strings.language)
                                    } else {
                                        notificationManager.sendInstantTestPushNotification(selectedGame, strings.language)
                                    }
                                }
                            }
                        },
                        onOpenTeamStats = onOpenTeamStats
                    )
                }
            }
        }
    )
}

private fun formatTipOffDateTime(epochMillis: Long): String {
    return runCatching {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy • HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(java.time.Instant.ofEpochMilli(epochMillis))
    }.getOrDefault("")
}

/**
 * Broadcast-Spielkarte im Courtside UI Design:
 * - Oben links: `[ENDE | R10 • 05.11.2026]` bzw. `[GEPLANT | R11 • 12.11.2026 • 19:30]`
 * - Mitte:
 *   - Bei gespielten/laufenden Spielen: Großes Endergebnis (`112 : 115`) + echte Viertelwerte (`Q1 Q2 Q3 Q4`)
 *   - Bei geplanten Spielen (`UPCOMING`): Tip-Off Uhrzeit, Arena & aktuelle Saisonbilanzen (`S-N`) statt künstlicher `0 0 0 0` Werte!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MatchSummaryCard(
    game: EuroLeagueGame,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExportCalendar: () -> Unit,
    onScheduleTipOffAlarm: () -> Unit,
    onOpenTeamStats: (String) -> Unit
) {
    val strings = LocalCourtsideStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var showContextMenu by remember { mutableStateOf(false) }

    val borderColor = when {
        isSelected -> Color(0xFFFF6600)
        isHovered -> Color(0xFFFF9E40)
        else -> Color(0xFF26262E)
    }

    val formattedDate = remember(game.tipOffEpochMillis) { formatTipOffDateTime(game.tipOffEpochMillis) }

    Box {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF1C1716) else Color(0xFF17171C)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected || isHovered) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .pointerInput(game.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.buttons.isSecondaryPressed) {
                                showContextMenu = true
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick = { showContextMenu = true }
                )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Row: Status + Round + Date Pill + Three Dots Menu Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF121216), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x99FF5E00), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        val statusBadgeText = when (game.status) {
                            GameStatus.LIVE -> strings.statusLive
                            GameStatus.FINAL -> strings.statusFinal
                            GameStatus.UPCOMING -> strings.statusUpcoming
                        }
                        val badgeColor = when (game.status) {
                            GameStatus.LIVE -> Color(0xFFFF4500)
                            GameStatus.FINAL -> Color(0xFF00875A)
                            GameStatus.UPCOMING -> Color(0xFF3A3A42)
                        }
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusBadgeText,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        val metaDetail = when (game.status) {
                            GameStatus.LIVE -> "R${game.round} • Q${game.currentQuarter} ${game.remainingClock}"
                            else -> "R${game.round} • $formattedDate"
                        }
                        Text(
                            text = metaDetail,
                            color = Color(0xFFE5E5EA),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "Menu",
                            tint = Color(0xFF8E8E93)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3-Column Broadcast Layout: Home Crest | Score & Quarters / Tip-Off Info | Away Crest
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ClubCrestColumn(
                        team = game.homeTeam,
                        modifier = Modifier.weight(3.3f)
                    )

                    val q = game.quarters
                    Column(
                        modifier = Modifier
                            .weight(4.4f)
                            .padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (game.status == GameStatus.UPCOMING) {
                            // Unplayed scheduled game: Show VS, Season Records & Arena instead of 0-0-0-0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${game.homeTeam.wins}${strings.winLetter}-${game.homeTeam.losses}${strings.lossLetter}",
                                    color = Color(0xFFD1D5DB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "VS",
                                    color = Color(0xFFFF6600),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${game.awayTeam.wins}${strings.winLetter}-${game.awayTeam.losses}${strings.lossLetter}",
                                    color = Color(0xFFD1D5DB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = game.homeTeam.arena,
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Played or Live game: Show Score prominently + Q1 Q2 Q3 Q4 breakdown
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = game.homeScore.toString(),
                                    color = if (game.homeScore >= game.awayScore) Color.White else Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = ":",
                                    color = Color(0xFFFF6600),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = game.awayScore.toString(),
                                    color = if (game.awayScore >= game.homeScore) Color.White else Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                            }

                            val hasQuarters = (q.q1Home + q.q2Home + q.q3Home + q.q4Home + q.q1Away + q.q2Away + q.q3Away + q.q4Away) > 0
                            if (hasQuarters) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf("Q1", "Q2", "Q3", "Q4").forEach { col ->
                                        Text(
                                            text = col,
                                            color = Color(0xFF8E8E93),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf(q.q1Home, q.q2Home, q.q3Home, q.q4Home).forEach { v ->
                                        Text(
                                            text = v.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf(q.q1Away, q.q2Away, q.q3Away, q.q4Away).forEach { v ->
                                        Text(
                                            text = v.toString(),
                                            color = Color(0xFF9CA3AF),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ClubCrestColumn(
                        team = game.awayTeam,
                        modifier = Modifier.weight(3.3f)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.contextExportCalendar) },
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                onClick = {
                    showContextMenu = false
                    onExportCalendar()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.contextScheduleTipOff) },
                leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                onClick = {
                    showContextMenu = false
                    onScheduleTipOffAlarm()
                }
            )
            DropdownMenuItem(
                text = { Text("${game.homeTeam.name} • ${strings.contextOpenTeamStatsSuffix}") },
                onClick = {
                    showContextMenu = false
                    onOpenTeamStats(game.homeTeam.id)
                }
            )
        }
    }
}

@Composable
private fun ClubCrestColumn(
    team: EuroLeagueTeam,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.euroleague.courtside.ui.theme.CourtsideClubCrest(
            team = team,
            size = 42.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = team.name,
            color = Color(0xFFE5E5EA),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Rechte Split-View-Spalte im Courtside UI Design:
 * - Bei gespielten/laufenden Spielen:
 *   1. Dual-Tone Gradient Scoreboard Header
 *   2. Glowing Half-Court Shot-Chart
 *   3. `Player boxscore ⌄` mit leuchtend orangener `PIR`-Spalte
 * - Bei geplanten Spielen (`UPCOMING`):
 *   1. Dual-Tone Matchup Banner mit Tip-Off Datum & Uhrzeit
 *   2. Direkter Team-Statistik-Vorbericht (`PPG`, `OPPG`, `OFF RTG`, `DEF RTG`, `3P%`, `2P%`, `REB%`)
 */
@Composable
private fun MatchDetailPane(
    game: EuroLeagueGame,
    onExportCalendar: () -> Unit,
    onTriggerCloseGameAlert: () -> Unit,
    onOpenTeamStats: (String) -> Unit = {}
) {
    val strings = LocalCourtsideStrings.current
    var sortColumn by rememberSaveable { mutableStateOf(BoxscoreSortColumn.PIR) }
    var sortAscending by rememberSaveable { mutableStateOf(false) }

    val combinedPlayers = remember(game.boxscoreHome, game.boxscoreAway, sortColumn, sortAscending) {
        val all = game.boxscoreHome + game.boxscoreAway
        val comparator = when (sortColumn) {
            BoxscoreSortColumn.PTS -> compareBy<PlayerBoxscoreStat> { it.points }
            BoxscoreSortColumn.REB -> compareBy { it.totalRebounds }
            BoxscoreSortColumn.AST -> compareBy { it.assists }
            BoxscoreSortColumn.STL -> compareBy { it.steals }
            BoxscoreSortColumn.BLK -> compareBy { it.blocks }
            BoxscoreSortColumn.PIR -> compareBy { it.pir }
        }
        if (sortAscending) all.sortedWith(comparator) else all.sortedWith(comparator.reversed())
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Dual-Tone Gradient Scoreboard Banner
        DualToneScoreboardBanner(
            game = game,
            onExportCalendar = onExportCalendar,
            onTriggerCloseGameAlert = onTriggerCloseGameAlert
        )

        if (game.status == GameStatus.UPCOMING) {
            // Vorbericht & Direkter Team-Vergleich für geplante Spiele (keine erfundenen Boxscores!)
            UpcomingMatchPreviewCard(
                game = game,
                onExportCalendar = onExportCalendar,
                onOpenTeamStats = onOpenTeamStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // 2. Glowing Half-Court Shot-Chart Card
            InteractiveCourtShotChart(
                shots = game.shots,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
            )

            // 3. Sleek Player Boxscore Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = strings.playerBoxscoreHeader,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(18.dp)
                    )
                }

                SortableBoxscoreTable(
                    stats = combinedPlayers,
                    sortColumn = sortColumn,
                    sortAscending = sortAscending,
                    onSortChange = { col ->
                        if (sortColumn == col) sortAscending = !sortAscending
                        else {
                            sortColumn = col
                            sortAscending = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UpcomingMatchPreviewCard(
    game: EuroLeagueGame,
    onExportCalendar: () -> Unit,
    onOpenTeamStats: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    val home = game.homeTeam
    val away = game.awayTeam
    val gpH = (home.wins + home.losses).coerceAtLeast(1)
    val gpA = (away.wins + away.losses).coerceAtLeast(1)
    val ppgH = ((home.pointsFor.toFloat() / gpH) * 10f).toInt() / 10f
    val ppgA = ((away.pointsFor.toFloat() / gpA) * 10f).toInt() / 10f
    val oppgH = ((home.pointsAgainst.toFloat() / gpH) * 10f).toInt() / 10f
    val oppgA = ((away.pointsAgainst.toFloat() / gpA) * 10f).toInt() / 10f

    val rows = listOf(
        Triple("${strings.winsColumn}-${strings.lossesColumn}", "${home.wins}-${home.losses}", "${away.wins}-${away.losses}"),
        Triple("PPG", "$ppgH", "$ppgA"),
        Triple("OPPG", "$oppgH", "$oppgA"),
        Triple("OFF RTG", "${home.offensiveRating}", "${away.offensiveRating}"),
        Triple("DEF RTG", "${home.defensiveRating}", "${away.defensiveRating}"),
        Triple("3P%", "${home.threePointPct}%", "${away.threePointPct}%"),
        Triple("2P%", "${home.twoPointPct}%", "${away.twoPointPct}%"),
        Triple("REB%", "${home.reboundRate}%", "${away.reboundRate}%")
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF151519))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${home.arena} • ${home.city}",
                color = Color(0xFFFF6600),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = formatTipOffDateTime(game.tipOffEpochMillis),
                color = Color(0xFFD1D5DB),
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }

        HorizontalDivider(color = Color(0x1AFFFFFF))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(rows) { (label, valH, valA) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1B21), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = valH,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = label,
                        color = Color(0xFFFF6600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = valA,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DualToneScoreboardBanner(
    game: EuroLeagueGame,
    onExportCalendar: () -> Unit,
    onTriggerCloseGameAlert: () -> Unit
) {
    val strings = LocalCourtsideStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        game.homeTeam.primaryColor.copy(alpha = 0.45f),
                        Color(0xFF17171C),
                        game.awayTeam.primaryColor.copy(alpha = 0.45f)
                    )
                )
            )
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .clickable { onExportCalendar() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0x77000000),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.clickable { onTriggerCloseGameAlert() }
            ) {
                val topPillText = when (game.status) {
                    GameStatus.LIVE -> "${strings.statusLive} • R${game.round} • Q${game.currentQuarter} ${game.remainingClock}"
                    GameStatus.FINAL -> "${strings.statusFinal} • ${strings.roundPrefix.uppercase()} ${game.round} • ${formatTipOffDateTime(game.tipOffEpochMillis)}"
                    GameStatus.UPCOMING -> "${strings.statusUpcoming} • ${strings.roundPrefix.uppercase()} ${game.round} • ${formatTipOffDateTime(game.tipOffEpochMillis)}"
                }
                Text(
                    text = topPillText,
                    color = Color(0xFFE5E5EA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    ClubCrestBadgeSmall(game.homeTeam)
                    Text(
                        text = game.homeTeam.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                }

                if (game.status == GameStatus.UPCOMING) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VS",
                            color = Color(0xFFFF6600),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${game.homeTeam.wins}-${game.homeTeam.losses}  |  ${game.awayTeam.wins}-${game.awayTeam.losses}",
                            color = Color(0xFFD1D5DB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = game.homeScore.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp
                        )
                        Text(
                            text = ":",
                            color = Color(0xFFFF6600),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = game.awayScore.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = game.awayTeam.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ClubCrestBadgeSmall(game.awayTeam)
                }
            }
        }
    }
}

@Composable
private fun ClubCrestBadgeSmall(team: EuroLeagueTeam) {
    com.euroleague.courtside.ui.theme.CourtsideClubCrest(
        team = team,
        size = 38.dp
    )
}

/**
 * Glowing Half-Court Shot-Chart (`Canvas`) im Courtside UI Design:
 * - Warmes orangenes Leuchten links & smaragdgrünes Leuchten rechts
 * - Leuchtend grüne Kreise (`#00E676`) für `Made` und rote Kreuze (`#FF3B5C`) für `Missed`.
 */
@Composable
private fun InteractiveCourtShotChart(
    shots: List<ShotAttempt>,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF151519))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Left Orange Ambient Glow & Right Emerald Ambient Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x38FF5E00), Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.45f),
                        radius = w * 0.45f
                    ),
                    center = Offset(w * 0.15f, h * 0.45f),
                    radius = w * 0.45f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3000D26E), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.45f),
                        radius = w * 0.45f
                    ),
                    center = Offset(w * 0.85f, h * 0.45f),
                    radius = w * 0.45f
                )

                val lineColor = Color(0x55FFFFFF)
                drawRect(color = lineColor, style = Stroke(width = 2f))

                // Outer & Inner Key
                val keyW = w * 0.28f
                val keyH = h * 0.56f
                drawRect(
                    color = lineColor,
                    topLeft = Offset((w - keyW) / 2f, 0f),
                    size = Size(keyW, keyH),
                    style = Stroke(width = 2f)
                )

                val innerW = w * 0.18f
                drawRect(
                    color = lineColor,
                    topLeft = Offset((w - innerW) / 2f, 0f),
                    size = Size(innerW, keyH),
                    style = Stroke(width = 2f)
                )

                // Free-throw circle
                drawCircle(
                    color = lineColor,
                    radius = innerW / 2f,
                    center = Offset(w / 2f, keyH),
                    style = Stroke(width = 2f)
                )

                // Orange Hoop at Top Center
                val hoopCenter = Offset(w / 2f, h * 0.10f)
                drawCircle(
                    color = Color(0xFFFF6600),
                    radius = 9f,
                    center = hoopCenter,
                    style = Stroke(width = 3.5f)
                )

                // 3-Point Arc
                val arcW = w * 0.78f
                val arcH = h * 0.78f
                drawArc(
                    color = lineColor,
                    startAngle = 12f,
                    sweepAngle = 156f,
                    useCenter = false,
                    topLeft = Offset((w - arcW) / 2f, -arcH * 0.22f),
                    size = Size(arcW, arcH),
                    style = Stroke(width = 2.2f)
                )

                // Plot Shots: Neon Green Glowing Circle (Made) vs Red Cross 'X' (Missed)
                shots.forEach { shot ->
                    val pos = Offset(shot.courtX * w, shot.courtY * h)
                    if (shot.made) {
                        drawCircle(
                            color = Color(0x4400E676),
                            radius = 16f,
                            center = pos
                        )
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = 9f,
                            center = pos,
                            style = Stroke(width = 3f)
                        )
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = 3.5f,
                            center = pos
                        )
                    } else {
                        val r = 7f
                        drawLine(
                            color = Color(0xFFFF3B5C),
                            start = Offset(pos.x - r, pos.y - r),
                            end = Offset(pos.x + r, pos.y + r),
                            strokeWidth = 3f
                        )
                        drawLine(
                            color = Color(0xFFFF3B5C),
                            start = Offset(pos.x + r, pos.y - r),
                            end = Offset(pos.x - r, pos.y + r),
                            strokeWidth = 3f
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFF00E676), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shotMade, color = Color(0xFFD1D5DB), fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFF3B5C), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.shotMissed, color = Color(0xFFD1D5DB), fontSize = 12.sp)
            }
        }
    }
}

/**
 * Minimalistische, dunkle Boxscore-Tabelle mit orangener PIR-Spalte rechts (`#FF6600`).
 */
@Composable
private fun SortableBoxscoreTable(
    stats: List<PlayerBoxscoreStat>,
    sortColumn: BoxscoreSortColumn,
    sortAscending: Boolean,
    onSortChange: (BoxscoreSortColumn) -> Unit
) {
    val strings = LocalCourtsideStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.playerColumn,
                modifier = Modifier.weight(2.8f),
                color = Color(0xFF8E8E93),
                fontSize = 12.sp
            )
            BoxscoreSortColumn.entries.forEach { col ->
                val isPir = col == BoxscoreSortColumn.PIR
                Text(
                    text = if (isPir) "PIR ⌄" else col.name,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSortChange(col) }
                        .pointerHoverIcon(PointerIcon.Hand),
                    textAlign = if (isPir) TextAlign.End else TextAlign.Center,
                    color = if (isPir || sortColumn == col) Color(0xFFFF6600) else Color(0xFF8E8E93),
                    fontWeight = if (isPir || sortColumn == col) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = Color(0x1AFFFFFF))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(stats, key = { index, item -> "${item.teamId}_${item.playerId}_$index" }) { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. #${row.jerseyNumber} ${row.playerName}",
                        modifier = Modifier.weight(2.8f),
                        color = Color(0xFFE5E5EA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatValueCell(row.points.toString(), Modifier.weight(1f))
                    StatValueCell(row.totalRebounds.toString(), Modifier.weight(1f))
                    StatValueCell(row.assists.toString(), Modifier.weight(1f))
                    StatValueCell(row.steals.toString(), Modifier.weight(1f))
                    StatValueCell(row.blocks.toString(), Modifier.weight(1f))
                    Text(
                        text = "${row.pir}.0",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = Color(0xFFFF6600),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
                HorizontalDivider(color = Color(0x0DFFFFFF))
            }
        }
    }
}

@Composable
private fun StatValueCell(value: String, modifier: Modifier = Modifier) {
    Text(
        text = value,
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = Color(0xFFD1D5DB),
        fontSize = 12.sp
    )
}
