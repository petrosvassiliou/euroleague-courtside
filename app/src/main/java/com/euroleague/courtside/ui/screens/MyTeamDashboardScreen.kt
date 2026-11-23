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
// TAB 1 (Ctrl+1): MEIN TEAM HUB – GLEICHE COURTSIDE SPLIT-VIEW DESIGN-SPRACHE
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MyTeamDashboardScreen(
    primaryTeam: EuroLeagueTeam,
    allTeams: List<EuroLeagueTeam>,
    favoriteTeamIds: Set<String>,
    roster: List<PlayerProfile>,
    games: List<EuroLeagueGame>,
    activeRound: Int = 11,
    syncStatus: String = "LIVE API • E2026",
    isTableTopPosture: Boolean,
    isCompact: Boolean = false,
    onSelectPrimaryTeam: (String) -> Unit,
    onVerifyRosterNow: () -> Unit = {},
    onSelectPlayer: (PlayerProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalCourtsideStrings.current
    val teamGames = remember(games, primaryTeam.id, primaryTeam.code) {
        games.filter {
            it.homeTeam.id == primaryTeam.id || it.awayTeam.id == primaryTeam.id ||
                it.homeTeam.code == primaryTeam.code || it.awayTeam.code == primaryTeam.code
        }.sortedWith(
            compareBy<EuroLeagueGame> { it.round }
                .thenBy { it.tipOffEpochMillis }
                .thenBy { it.gameCode }
        )
    }
    val nextGame = remember(teamGames, games, activeRound) {
        teamGames.firstOrNull { it.round == activeRound }
            ?: teamGames.firstOrNull { it.round >= activeRound && (it.status == GameStatus.UPCOMING || it.status == GameStatus.LIVE) }
            ?: teamGames.firstOrNull { it.status == GameStatus.UPCOMING || it.status == GameStatus.LIVE }
            ?: teamGames.lastOrNull()
            ?: games.minByOrNull { it.round }
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

    val countdownLabel = strings.formatTipOffCountdown(remainingMillis)
    val isLiveGame = nextGame.status == GameStatus.LIVE
    val isPlayedFinal = nextGame.status == GameStatus.FINAL && (nextGame.homeScore > 0 || nextGame.awayScore > 0)
    val pillBadgeText = when {
        isLiveGame -> "● ${strings.statusLive}"
        isPlayedFinal -> strings.statusFinal
        else -> strings.nextTipOffBadge
    }
    val pillDetailText = when {
        isLiveGame -> "Q${nextGame.currentQuarter.coerceAtMost(4)} • ${nextGame.remainingClock}"
        isPlayedFinal -> "${nextGame.homeScore} : ${nextGame.awayScore}"
        else -> countdownLabel
    }
    val pillBadgeColor = if (isLiveGame) {
        CourtsideDesignTokens.NeonMissedRed
    } else {
        CourtsideDesignTokens.EuroLeagueOrange
    }

    if (isCompact) {
        // Smartphone (COMPACT): Einheitlicher vertikaler Feed mit Club-Banner, horizontalem Club-Umschalter, Spieltagskarte & Kader
        val clubListState = rememberLazyListState()
        LaunchedEffect(primaryTeam.id, allTeams.size) {
            val selectedIdx = allTeams.indexOfFirst { it.id == primaryTeam.id || it.code == primaryTeam.code }
            if (selectedIdx >= 0) {
                clubListState.animateScrollToItem((selectedIdx - 1).coerceAtLeast(0))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CourtsideDesignTokens.ShellObsidian),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Club Hero Banner (mit Bilanz & 5-Spiele-Formkurve immer sichtbar rechts)
            item {
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CourtsideClubCrest(primaryTeam, size = 42.dp)
                            Column {
                                Text(
                                    text = primaryTeam.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    lineHeight = 19.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${primaryTeam.arena} • ${primaryTeam.city}",
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${primaryTeam.wins}${strings.winLetter} - ${primaryTeam.losses}${strings.lossLetter}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                primaryTeam.streak.takeLast(5).forEach { res ->
                                    val isWin = isWinStreakEntry(res)
                                    Box(
                                        modifier = Modifier
                                            .size(17.dp)
                                            .clip(CircleShape)
                                            .background(if (isWin) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.NeonMissedRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isWin) strings.winLetter else strings.lossLetter,
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Kompakter horizontaler Verein-Umschalter für alle 20 EuroLeague-Clubs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.myTeamHubTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
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
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LazyRow(
                        state = clubListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allTeams, key = { "m_club_${it.id}" }) { team ->
                            val isSelected = team.id == primaryTeam.id || team.code == primaryTeam.code
                            CourtsideGlassCard(
                                isSelected = isSelected,
                                glowColor = team.primaryColor,
                                onClick = { onSelectPrimaryTeam(team.id) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    CourtsideClubCrest(team = team, size = 22.dp)
                                    Text(
                                        text = team.code,
                                        color = if (isSelected) Color.White else CourtsideDesignTokens.MutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Vollbreite Spieltags- / Countdown-Karte
            item {
                CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                CourtsideLivePill(
                                    badgeText = pillBadgeText,
                                    detailText = pillDetailText,
                                    badgeColor = pillBadgeColor
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.roundPrefix} ${nextGame.round}",
                                color = CourtsideDesignTokens.MutedText,
                                fontSize = 12.sp,
                                maxLines = 1
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                CourtsideClubCrest(nextGame.homeTeam, size = 34.dp)
                                Column {
                                    Text(
                                        text = nextGame.homeTeam.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = nextGame.homeTeam.arena,
                                        color = CourtsideDesignTokens.MutedText,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(
                                text = if (isLiveGame || isPlayedFinal) "${nextGame.homeScore} : ${nextGame.awayScore}" else "VS",
                                color = pillBadgeColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isLiveGame || isPlayedFinal) 15.sp else 13.sp,
                                maxLines = 1,
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
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = nextGame.awayTeam.city,
                                        color = CourtsideDesignTokens.MutedText,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.End
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                CourtsideClubCrest(nextGame.awayTeam, size = 34.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { CalendarExportHelper.exportSingleGameToNativeCalendar(context, nextGame) },
                                colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.EuroLeagueOrange),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.googleCalendarButton,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { CalendarExportHelper.exportTeamSeasonToIcsFile(context, primaryTeam, games) },
                                colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.PillSurface),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.seasonIcsButton,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 4. Aktiver Kader im Vollbreiten-Kartenblock
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.activeRosterHeader} (${roster.size} ${strings.playersWord})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0x2200E676),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .border(1.dp, CourtsideDesignTokens.NeonMadeGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { onVerifyRosterNow() }
                    ) {
                        Text(
                            text = strings.rosterVerifiedBadge,
                            color = CourtsideDesignTokens.NeonMadeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                CourtsideGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(strings.playerColumn, Modifier.weight(2.6f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp)
                            Text("POS", Modifier.weight(0.75f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("PPG", Modifier.weight(0.95f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("RPG", Modifier.weight(0.95f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("APG", Modifier.weight(0.95f), color = CourtsideDesignTokens.MutedText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            Text("PIR ⌄", Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                        roster.forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlayer(p) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2.6f).padding(end = 4.dp)) {
                                    Text(
                                        text = "${idx + 1}. #${p.number} ${p.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
                                    Text(
                                        text = injText,
                                        color = injColor,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(p.position, Modifier.weight(0.75f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text(p.ppg.toString(), Modifier.weight(0.95f), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text(p.rpg.toString(), Modifier.weight(0.95f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text(p.apg.toString(), Modifier.weight(0.95f), color = Color(0xFFD1D5DB), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                Text(p.pir.toString(), Modifier.weight(1f), color = CourtsideDesignTokens.EuroLeagueOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End, maxLines = 1)
                            }
                            if (idx < roster.lastIndex) {
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

            // Matchday / Countdown Broadcast Card für den aktuellen Spieltag (z. B. Round 11)
            CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CourtsideLivePill(
                            badgeText = pillBadgeText,
                            detailText = pillDetailText,
                            badgeColor = pillBadgeColor
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
                            text = if (isLiveGame || isPlayedFinal) "${nextGame.homeScore} : ${nextGame.awayScore}" else "VS",
                            color = pillBadgeColor,
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
                            primaryTeam.streak.takeLast(5).forEach { res ->
                                val isWin = isWinStreakEntry(res)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlayer(p) }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(vertical = 8.dp),
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
