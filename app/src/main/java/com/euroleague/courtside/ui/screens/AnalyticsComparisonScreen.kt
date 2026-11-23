package com.euroleague.courtside.ui.screens

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


// =========================================================================
// TAB 4 (Ctrl+4): ANALYTICS, RADAR-CHART & SEASON LEADERS IM COURTSIDE DESIGN
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeasonLeadersAndComparisonScreen(
    players: List<PlayerProfile>,
    teams: List<EuroLeagueTeam>,
    games: List<EuroLeagueGame> = emptyList(),
    teamA: EuroLeagueTeam,
    teamB: EuroLeagueTeam,
    isCompact: Boolean = false,
    onSelectTeamA: (String) -> Unit = {},
    onSelectTeamB: (String) -> Unit,
    onSelectPlayer: (PlayerProfile) -> Unit = {}
) {
    val strings = LocalCourtsideStrings.current

    val h2hGames = remember(games, teamA.id, teamB.id) {
        games.filter { g ->
            val aHome = matchesClubCode(g.homeTeam.id, teamA) || matchesClubCode(g.homeTeam.code, teamA)
            val bAway = matchesClubCode(g.awayTeam.id, teamB) || matchesClubCode(g.awayTeam.code, teamB)
            val bHome = matchesClubCode(g.homeTeam.id, teamB) || matchesClubCode(g.homeTeam.code, teamB)
            val aAway = matchesClubCode(g.awayTeam.id, teamA) || matchesClubCode(g.awayTeam.code, teamA)
            (aHome && bAway) || (bHome && aAway)
        }.sortedBy { it.round }
    }

    val ppgA = teamA.ppg
    val ppgB = teamB.ppg
    val oppgA = teamA.oppg
    val oppgB = teamB.oppg

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
            matchesClubCode(it.teamId, teamA) || matchesClubCode(it.teamId, teamB)
        }.sortedByDescending { it.pir }.take(8)
        val others = players.sortedByDescending { it.pir }.filterNot { p -> teamABStars.any { it.id == p.id } }
        teamABStars + others
    }

    if (isCompact) {
        // Smartphone (COMPACT): Einheitlicher Vollbreiten-Feed ohne gequetschte Nebenspalten
        val teamAListState = rememberLazyListState()
        val teamBListState = rememberLazyListState()
        LaunchedEffect(teamA.id, teams.size) {
            val idxA = teams.indexOfFirst { it.id == teamA.id }
            if (idxA >= 0) teamAListState.animateScrollToItem((idxA - 1).coerceAtLeast(0))
        }
        LaunchedEffect(teamB.id, teams.size) {
            val idxB = teams.indexOfFirst { it.id == teamB.id }
            if (idxB >= 0) teamBListState.animateScrollToItem((idxB - 1).coerceAtLeast(0))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CourtsideDesignTokens.ShellObsidian),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = strings.teamAnalyticsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // 1. Vollbreite Team-A & Team-B Auswahl-Karte
            item {
                CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TEAM A: ${teamA.name}",
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LazyRow(
                            state = teamAListState,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(teams, key = { "m_teamA_${it.id}" }) { candidate ->
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
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        CourtsideClubCrest(team = candidate, size = 20.dp)
                                        Text(
                                            text = candidate.code,
                                            color = if (selected) Color.White else Color(0xFF9CA3AF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Text(
                            text = "TEAM B: ${teamB.name}",
                            color = CourtsideDesignTokens.NeonMadeGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LazyRow(
                            state = teamBListState,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(teams, key = { "m_teamB_${it.id}" }) { candidate ->
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
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        CourtsideClubCrest(team = candidate, size = 20.dp)
                                        Text(
                                            text = candidate.code,
                                            color = if (selected) Color.White else Color(0xFF9CA3AF),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Vollbreites Dual-Tone Matchup Banner (ohne 2-Buchstaben-Zeilenumbruch!)
            item {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CourtsideClubCrest(teamA, size = 34.dp)
                            Column {
                                Text(
                                    text = teamA.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${teamA.wins}${strings.winLetter}-${teamA.losses}${strings.lossLetter} • OFF ${teamA.offensiveRating}",
                                    color = CourtsideDesignTokens.EuroLeagueOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = "VS",
                            color = CourtsideDesignTokens.EuroLeagueOrange,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
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
                                    text = teamB.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "${teamB.wins}${strings.winLetter}-${teamB.losses}${strings.lossLetter} • OFF ${teamB.offensiveRating}",
                                    color = CourtsideDesignTokens.NeonMadeGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            CourtsideClubCrest(teamB, size = 34.dp)
                        }
                    }
                }
            }

            // 3. Vollbreites 5-Achsen-Radar-Diagramm mit gepolsterter Beschriftung & umbrechender Legende
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF151519))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(244.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            TeamComparisonRadarCanvas(teamA = teamA, teamB = teamB)
                        }

                        RadarAxisBadge(
                            title = "OFF",
                            valA = "${teamA.offensiveRating}",
                            valB = "${teamB.offensiveRating}",
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        RadarAxisBadge(
                            title = "DEF",
                            valA = "${teamA.defensiveRating}",
                            valB = "${teamB.defensiveRating}",
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                        RadarAxisBadge(
                            title = "3P%",
                            valA = "${teamA.threePointPct}%",
                            valB = "${teamB.threePointPct}%",
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                        RadarAxisBadge(
                            title = "2P%",
                            valA = "${teamA.twoPointPct}%",
                            valB = "${teamB.twoPointPct}%",
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                        RadarAxisBadge(
                            title = "REB%",
                            valA = "${teamA.reboundRate}%",
                            valB = "${teamB.reboundRate}%",
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(CourtsideDesignTokens.EuroLeagueOrange))
                            Text(
                                text = "${teamA.code} (${teamA.name})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(CourtsideDesignTokens.NeonMadeGreen))
                            Text(
                                text = "${teamB.code} (${teamB.name})",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 4. Direkte Saison-Duelle (H2H)
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
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Alle 8 visuellen Head-to-Head Statistik-Balken
            items(comparisonMetrics, key = { "m_metric_${it.label}" }) { metric ->
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
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = metric.label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                text = metric.displayB,
                                color = if (bLeads) CourtsideDesignTokens.NeonMadeGreen else Color(0xFFD1D5DB),
                                fontWeight = if (bLeads) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.End,
                                maxLines = 1,
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

            // 6. Saison MVP & PIR-Leader Tabelle im Vollbreiten-Design (volle Spielernamen ohne Abschneiden!)
            item {
                Text(
                    text = "${teamA.code} vs ${teamB.code} • ${strings.seasonLeadersHeader}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            item {
                CourtsideGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.playerColumn, Modifier.weight(2.7f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp)
                            Text("PPG", Modifier.weight(0.95f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("RPG", Modifier.weight(0.95f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("APG", Modifier.weight(0.95f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("PIR", Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                        matchupAndLeaguePlayers.take(20).forEachIndexed { idx, p ->
                            val isTeamA = matchesClubCode(p.teamId, teamA)
                            val isTeamB = matchesClubCode(p.teamId, teamB)
                            val teamColor = when {
                                isTeamA -> CourtsideDesignTokens.EuroLeagueOrange
                                isTeamB -> CourtsideDesignTokens.NeonMadeGreen
                                else -> Color(0xFF9CA3AF)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlayer(p) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(2.7f).padding(end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        color = CourtsideDesignTokens.MutedText,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = p.teamId,
                                        color = teamColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = p.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text("${p.ppg}", Modifier.weight(0.95f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text("${p.rpg}", Modifier.weight(0.95f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text("${p.apg}", Modifier.weight(0.95f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text("${p.pir}", Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End, maxLines = 1)
                            }
                            if (idx < minOf(matchupAndLeaguePlayers.size, 20) - 1) {
                                HorizontalDivider(color = Color(0x0DFFFFFF))
                            }
                        }
                    }
                }
            }
        }
        return
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
                    val isTeamA = matchesClubCode(p.teamId, teamA)
                    val isTeamB = matchesClubCode(p.teamId, teamB)
                    val teamColor = when {
                        isTeamA -> CourtsideDesignTokens.EuroLeagueOrange
                        isTeamB -> CourtsideDesignTokens.NeonMadeGreen
                        else -> Color(0xFF9CA3AF)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlayer(p) }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(vertical = 6.dp),
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
    // 0 (Top): OFF RTG, 1 (Right): DEF Efficiency (lower DRtg = higher polygon vertex), 2 (Bottom-Right): 3P%, 3 (Bottom-Left): 2P%, 4 (Left): REB%
    val metricsA = listOf(
        ((teamA.offensiveRating - 98f) / 26f).coerceIn(0.25f, 0.96f),
        ((126f - teamA.defensiveRating) / 26f).coerceIn(0.25f, 0.96f),
        ((teamA.threePointPct - 27f) / 16f).coerceIn(0.25f, 0.96f),
        ((teamA.twoPointPct - 47f) / 15f).coerceIn(0.25f, 0.96f),
        ((teamA.reboundRate - 42f) / 15f).coerceIn(0.25f, 0.96f)
    )
    val metricsB = listOf(
        ((teamB.offensiveRating - 98f) / 26f).coerceIn(0.25f, 0.96f),
        ((126f - teamB.defensiveRating) / 26f).coerceIn(0.25f, 0.96f),
        ((teamB.threePointPct - 27f) / 16f).coerceIn(0.25f, 0.96f),
        ((teamB.twoPointPct - 47f) / 15f).coerceIn(0.25f, 0.96f),
        ((teamB.reboundRate - 42f) / 15f).coerceIn(0.25f, 0.96f)
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
