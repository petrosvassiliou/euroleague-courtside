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
// TAB 3 (Ctrl+3): EUROLEAGUE REGULAR SEASON STANDINGS & CLICKABLE TEAM PAGE
// =========================================================================

internal fun matchesClubCode(idOrCode: String, team: EuroLeagueTeam): Boolean {
    if (team.id.equals(idOrCode, ignoreCase = true) || team.code.equals(idOrCode, ignoreCase = true)) {
        return true
    }
    val upper = idOrCode.uppercase()
    val tId = team.id.uppercase()
    val tCode = team.code.uppercase()
    val aliasPairs = listOf(
        "BAY" to "MUN",
        "PAO" to "PAN",
        "RMB" to "MAD",
        "FCB" to "BAR",
        "EFS" to "IST",
        "FBB" to "ULK",
        "ASM" to "MCO",
        "CZV" to "RED",
        "EA7" to "MIL",
        "BKN" to "BAS",
        "MTA" to "TEL"
    )
    return aliasPairs.any { (a, b) ->
        (upper == a && (tId == b || tCode == b)) || (upper == b && (tId == a || tCode == a))
    }
}

/**
 * Ermittelt für ein ausgewähltes Team aus allen Saisonspielen:
 * - `first`: Die letzten 3 gespielten (bzw. laufenden) Spiele, neuestes zuerst (`FINAL` / `LIVE`)
 * - `second`: Die nächsten 3 anstehenden Spiele in chronologischer Reihenfolge (`UPCOMING`)
 */
internal fun resolveTeamLastAndUpcomingGames(
    team: EuroLeagueTeam,
    allGames: List<EuroLeagueGame>
): Pair<List<EuroLeagueGame>, List<EuroLeagueGame>> {
    val teamGames = allGames.filter { game ->
        matchesClubCode(game.homeTeam.id, team) ||
            matchesClubCode(game.homeTeam.code, team) ||
            matchesClubCode(game.awayTeam.id, team) ||
            matchesClubCode(game.awayTeam.code, team)
    }.sortedWith(
        compareBy<EuroLeagueGame> { it.round }
            .thenBy { it.tipOffEpochMillis }
            .thenBy { it.gameCode }
    )

    val last3Games = teamGames
        .filter { it.status == GameStatus.FINAL || it.status == GameStatus.LIVE }
        .takeLast(3)
        .reversed()

    val upcoming3Games = teamGames
        .filter { it.status == GameStatus.UPCOMING }
        .take(3)

    return last3Games to upcoming3Games
}

private fun formatStandingsGameDate(epochMillis: Long, includeTime: Boolean): String {
    if (epochMillis <= 0L) return ""
    return runCatching {
        val pattern = if (includeTime) "dd.MM.yyyy • HH:mm" else "dd.MM.yyyy"
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern))
    }.getOrDefault("")
}

@Composable
internal fun StandingsScreen(
    standings: List<EuroLeagueTeam>,
    primaryTeam: EuroLeagueTeam,
    favoriteIds: Set<String>,
    isCompact: Boolean,
    onSelectTeam: (EuroLeagueTeam) -> Unit
) {
    val strings = LocalCourtsideStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(if (isCompact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Kopfbereich: Fokus zu 100% auf die echte EuroLeague-Tabelle (ohne Playoff/Play-In-Projektionen)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.standingsHeader,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.clickTeamHint,
                    color = CourtsideDesignTokens.EuroLeagueOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!isCompact) {
                CourtsideLivePill(
                    badgeText = strings.regularSeasonLabel,
                    detailText = "${standings.size} ${strings.clubsSuffix}",
                    badgeColor = CourtsideDesignTokens.EuroLeagueOrange
                )
            }
        }

        // Vollflächige Standings-Tabelle im Courtside Broadcast-Design mit klickbaren Vereinszeilen
        CourtsideGlassCard(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 10.dp else 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        modifier = Modifier.width(if (isCompact) 28.dp else 36.dp),
                        color = CourtsideDesignTokens.MutedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = strings.clubColumn,
                        modifier = Modifier.weight(if (isCompact) 3.1f else 3.4f),
                        color = CourtsideDesignTokens.MutedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = strings.gamesPlayedColumn,
                        modifier = Modifier.weight(0.7f),
                        color = CourtsideDesignTokens.MutedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.winsColumn,
                        modifier = Modifier.weight(0.7f),
                        color = CourtsideDesignTokens.MutedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = strings.lossesColumn,
                        modifier = Modifier.weight(0.7f),
                        color = CourtsideDesignTokens.MutedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    if (!isCompact) {
                        Text(
                            text = strings.pointsForColumn,
                            modifier = Modifier.weight(0.9f),
                            color = CourtsideDesignTokens.MutedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.pointsAgainstColumn,
                            modifier = Modifier.weight(0.9f),
                            color = CourtsideDesignTokens.MutedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = strings.formColumn,
                            modifier = Modifier.weight(1.5f),
                            color = CourtsideDesignTokens.MutedText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "+/-",
                        modifier = Modifier.weight(0.9f),
                        color = CourtsideDesignTokens.EuroLeagueOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 20.dp else 28.dp))
                }
                HorizontalDivider(color = Color(0x26FFFFFF))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(standings, key = { _, t -> t.id }) { index, team ->
                        val rank = index + 1
                        val isMyTeam = matchesClubCode(primaryTeam.id, team)
                        val isFav = isMyTeam || favoriteIds.any { matchesClubCode(it, team) }
                        val gamesPlayed = team.wins + team.losses
                        val rowBgColor = when {
                            isMyTeam -> team.primaryColor.copy(alpha = 0.16f)
                            index % 2 == 1 -> Color(0x06FFFFFF)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(rowBgColor)
                                .clickable { onSelectTeam(team) }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 4.dp, vertical = if (isCompact) 9.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$rank",
                                modifier = Modifier.width(if (isCompact) 24.dp else 32.dp),
                                color = if (rank <= 3) Color.White else Color(0xFF9CA3AF),
                                fontWeight = if (rank <= 3 || isMyTeam) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Row(
                                modifier = Modifier.weight(if (isCompact) 3.1f else 3.4f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CourtsideClubCrest(team = team, size = if (isCompact) 26.dp else 30.dp)
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = team.name,
                                            color = Color.White,
                                            fontSize = if (isCompact) 13.sp else 14.sp,
                                            fontWeight = if (isMyTeam) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (isFav) {
                                            Text(
                                                text = "★",
                                                color = CourtsideDesignTokens.EuroLeagueOrange,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (!isCompact) {
                                        Text(
                                            text = "${team.city} • ${team.arena}",
                                            color = CourtsideDesignTokens.MutedText,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    } else if (team.streak.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            team.streak.takeLast(5).forEach { entry ->
                                                val isWin = isWinStreakEntry(entry)
                                                val badgeTextCol = if (isWin) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.NeonMissedRed
                                                Text(
                                                    text = if (isWin) strings.winLetter else strings.lossLetter,
                                                    color = badgeTextCol,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = gamesPlayed.toString(),
                                modifier = Modifier.weight(0.7f),
                                color = Color(0xFFD1D5DB),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = team.wins.toString(),
                                modifier = Modifier.weight(0.7f),
                                color = CourtsideDesignTokens.NeonMadeGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = team.losses.toString(),
                                modifier = Modifier.weight(0.7f),
                                color = Color(0xFFD1D5DB),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            if (!isCompact) {
                                Text(
                                    text = team.pointsFor.toString(),
                                    modifier = Modifier.weight(0.9f),
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = team.pointsAgainst.toString(),
                                    modifier = Modifier.weight(0.9f),
                                    color = CourtsideDesignTokens.MutedText,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Box(
                                    modifier = Modifier.weight(1.5f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    StandingsFormStreakRow(streak = team.streak, strings = strings)
                                }
                            }
                            Text(
                                text = if (team.pointDiff >= 0) "+${team.pointDiff}" else "${team.pointDiff}",
                                modifier = Modifier.weight(0.9f),
                                color = if (team.pointDiff >= 0) {
                                    CourtsideDesignTokens.EuroLeagueOrange
                                } else {
                                    Color(0xFF9CA3AF)
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.End
                            )
                            Box(
                                modifier = Modifier.width(if (isCompact) 20.dp else 28.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = team.name,
                                    tint = CourtsideDesignTokens.EuroLeagueOrange.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0x12FFFFFF))
                    }
                }
            }
        }
    }
}

internal fun isWinStreakEntry(entry: String): Boolean {
    return entry.equals("W", ignoreCase = true) || entry.equals("S", ignoreCase = true)
}

@Composable
private fun StandingsFormStreakRow(
    streak: List<String>,
    strings: CourtsideStrings
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        streak.takeLast(5).forEach { entry ->
            val isWin = isWinStreakEntry(entry)
            val badgeBg = if (isWin) Color(0x2600E676) else Color(0x26FF3B30)
            val badgeTextCol = if (isWin) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.NeonMissedRed
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .border(1.dp, badgeTextCol.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isWin) strings.winLetter else strings.lossLetter,
                    color = badgeTextCol,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Dedizierte Team-Seite beim Klick auf ein Team in der Standings-Tabelle:
 * Zeigt Vereins-Header, die letzten 3 Spiele (`last3Games`), die 3 kommenden Spiele (`upcoming3Games`)
 * sowie den kompletten aktiven Kader (`roster`) inkl. Verletztenstatus & Saisonstatistiken.
 */
@Composable
internal fun StandingsTeamDetailScreen(
    team: EuroLeagueTeam,
    rank: Int,
    isPrimaryTeam: Boolean,
    isCompact: Boolean,
    roster: List<PlayerProfile>,
    games: List<EuroLeagueGame>,
    onBackToStandings: () -> Unit,
    onSetAsMyTeam: () -> Unit,
    onOpenGameInMatchCenter: (Int) -> Unit,
    onOpenExactGameInMatchCenter: (String, Int) -> Unit = { _, rnd -> onOpenGameInMatchCenter(rnd) },
    onSelectPlayer: (PlayerProfile) -> Unit = {}
) {
    val strings = LocalCourtsideStrings.current
    val (last3Games, upcoming3Games) = remember(team.id, team.code, games) {
        resolveTeamLastAndUpcomingGames(team, games)
    }
    val sortedRoster = remember(roster) {
        roster.sortedByDescending { it.pir }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtsideDesignTokens.ShellObsidian)
            .padding(if (isCompact) 12.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Obere Navigationsleiste: Zurück zur Tabelle + Optional "Als Mein Team wählen"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = CourtsideDesignTokens.CardSurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .border(1.dp, CourtsideDesignTokens.EuroLeagueOrange.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .clickable { onBackToStandings() }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(
                    text = strings.backToStandingsButton,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            if (isPrimaryTeam) {
                Surface(
                    color = Color(0x2600E676),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, CourtsideDesignTokens.NeonMadeGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "★ ${strings.navMyTeam}",
                        color = CourtsideDesignTokens.NeonMadeGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            } else {
                Surface(
                    color = Color(0x26FF6600),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .border(1.dp, CourtsideDesignTokens.EuroLeagueOrange.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                        .clickable { onSetAsMyTeam() }
                        .pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(
                        text = strings.setAsMyTeamButton,
                        color = CourtsideDesignTokens.EuroLeagueOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Vereins-Banner mit Rang, Bilanz, Punktedifferenz & Formkurve
        CourtsideDualToneBanner(
            leftColor = team.primaryColor,
            rightColor = team.secondaryColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CourtsideClubCrest(team = team, size = if (isCompact) 44.dp else 54.dp)
                    Column {
                        Text(
                            text = "#$rank ${team.name}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isCompact) 16.sp else 20.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${team.arena} • ${team.city}",
                            color = Color(0xFFE5E7EB),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${team.wins}${strings.winLetter} - ${team.losses}${strings.lossLetter}",
                        color = CourtsideDesignTokens.NeonMadeGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isCompact) 15.sp else 18.sp
                    )
                    Text(
                        text = "${if (team.pointDiff >= 0) "+${team.pointDiff}" else "${team.pointDiff}"} +/-",
                        color = CourtsideDesignTokens.EuroLeagueOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (team.streak.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        StandingsFormStreakRow(streak = team.streak, strings = strings)
                    }
                }
            }
        }

        if (!isCompact) {
            // Desktop / Tablet / Foldable: Links Letzte 3 & Kommende 3 Spiele, Rechts Aktiver Kader
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = strings.last3GamesHeader,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(last3Games, key = { "last_${it.id}" }) { game ->
                        TeamPageGameCard(
                            game = game,
                            perspectiveTeam = team,
                            strings = strings,
                            onClick = { onOpenExactGameInMatchCenter(game.id, game.round) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.next3GamesHeader,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(upcoming3Games, key = { "next_${it.id}" }) { game ->
                        TeamPageGameCard(
                            game = game,
                            perspectiveTeam = team,
                            strings = strings,
                            onClick = { onOpenExactGameInMatchCenter(game.id, game.round) }
                        )
                    }
                }

                CourtsideGlassCard(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.activeRosterHeader} (${sortedRoster.size} ${strings.playersWord})",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = strings.rosterVerifiedBadge,
                                color = CourtsideDesignTokens.NeonMadeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        TeamRosterTableHeader(strings = strings)
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(sortedRoster, key = { _, p -> p.id }) { idx, player ->
                                TeamRosterPlayerRow(
                                    index = idx,
                                    player = player,
                                    strings = strings,
                                    onClick = { onSelectPlayer(player) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Smartphone (COMPACT): Einheitlicher vertikaler Feed mit Letzten 3 Spielen, Kommenden 3 Spielen & Kader
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = strings.last3GamesHeader,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                items(last3Games, key = { "m_last_${it.id}" }) { game ->
                    TeamPageGameCard(
                        game = game,
                        perspectiveTeam = team,
                        strings = strings,
                        onClick = { onOpenGameInMatchCenter(game.round) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.next3GamesHeader,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                items(upcoming3Games, key = { "m_next_${it.id}" }) { game ->
                    TeamPageGameCard(
                        game = game,
                        perspectiveTeam = team,
                        strings = strings,
                        onClick = { onOpenGameInMatchCenter(game.round) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${strings.activeRosterHeader} (${sortedRoster.size} ${strings.playersWord})",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                item {
                    CourtsideGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            TeamRosterTableHeader(strings = strings)
                            sortedRoster.forEachIndexed { idx, player ->
                                TeamRosterPlayerRow(
                                    index = idx,
                                    player = player,
                                    strings = strings,
                                    onClick = { onSelectPlayer(player) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamPageGameCard(
    game: EuroLeagueGame,
    perspectiveTeam: EuroLeagueTeam,
    strings: CourtsideStrings,
    onClick: () -> Unit
) {
    val isTeamHome = matchesClubCode(game.homeTeam.id, perspectiveTeam) ||
        matchesClubCode(game.homeTeam.code, perspectiveTeam)
    val isFinal = game.status == GameStatus.FINAL
    val isLive = game.status == GameStatus.LIVE
    val didTeamWin = if (isTeamHome) {
        game.homeScore > game.awayScore
    } else {
        game.awayScore > game.homeScore
    }

    CourtsideGlassCard(
        isSelected = isLive,
        glowColor = if (isLive) CourtsideDesignTokens.LiveBadgeOrangeRed else perspectiveTeam.primaryColor,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${strings.roundPrefix} ${game.round}",
                        color = CourtsideDesignTokens.EuroLeagueOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Surface(
                        color = Color(0xFF22222A),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = if (isTeamHome) strings.homeShortLabel else strings.awayShortLabel,
                            color = Color(0xFFD1D5DB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    val dateLabel = formatStandingsGameDate(
                        epochMillis = game.tipOffEpochMillis,
                        includeTime = game.status == GameStatus.UPCOMING
                    )
                    if (dateLabel.isNotBlank()) {
                        Text(
                            text = "• $dateLabel",
                            color = CourtsideDesignTokens.MutedText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))

                when {
                    isLive -> {
                        Surface(
                            color = CourtsideDesignTokens.LiveBadgeOrangeRed,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "● ${strings.statusLive} Q${game.currentQuarter} ${game.remainingClock}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    isFinal -> {
                        val badgeCol = if (didTeamWin) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.NeonMissedRed
                        Surface(
                            color = badgeCol.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.border(1.dp, badgeCol.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "${if (didTeamWin) strings.winLetter else strings.lossLetter} • ${strings.statusFinal}",
                                color = badgeCol,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = game.homeTeam.arena,
                            color = CourtsideDesignTokens.MutedText,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CourtsideClubCrest(team = game.homeTeam, size = 28.dp)
                    Text(
                        text = game.homeTeam.name,
                        color = if (isTeamHome) Color.White else Color(0xFFD1D5DB),
                        fontWeight = if (isTeamHome) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (isFinal || isLive) {
                        "${game.homeScore} : ${game.awayScore}"
                    } else {
                        "VS"
                    },
                    color = if (isLive) CourtsideDesignTokens.EuroLeagueOrange else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isFinal || isLive) 16.sp else 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = game.awayTeam.name,
                        color = if (!isTeamHome) Color.White else Color(0xFFD1D5DB),
                        fontWeight = if (!isTeamHome) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CourtsideClubCrest(team = game.awayTeam, size = 28.dp)
                }
            }
        }
    }
}

@Composable
private fun TeamRosterTableHeader(strings: CourtsideStrings) {
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

@Composable
private fun TeamRosterPlayerRow(
    index: Int,
    player: PlayerProfile,
    strings: CourtsideStrings,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2.6f)) {
            Text(
                text = "${index + 1}. #${player.number} ${player.name}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            val injColor = when (player.injuryStatus) {
                InjuryStatus.FIT -> CourtsideDesignTokens.NeonMadeGreen
                InjuryStatus.GAME_TIME_DECISION, InjuryStatus.QUESTIONABLE -> CourtsideDesignTokens.EuroLeagueOrange
                InjuryStatus.OUT -> CourtsideDesignTokens.NeonMissedRed
            }
            val localizedInjuryLabel = player.injuryStatus.localizedLabel(strings)
            val injText = if (player.injuryDetail.isNotBlank()) {
                "$localizedInjuryLabel • ${player.injuryDetail}"
            } else {
                "$localizedInjuryLabel • ${player.nationality} • ${player.heightCm}cm"
            }
            Text(injText, color = injColor, fontSize = 10.sp)
        }
        Text(player.position, Modifier.weight(0.8f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(player.ppg.toString(), Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(player.rpg.toString(), Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(player.apg.toString(), Modifier.weight(1f), color = Color(0xFFD1D5DB), fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(player.pir.toString(), Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End)
    }
    HorizontalDivider(color = Color(0x0DFFFFFF))
}
