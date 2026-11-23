package com.euroleague.courtside.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.data.repository.InjuryStatus
import com.euroleague.courtside.data.repository.PlayerGameLogEntry
import com.euroleague.courtside.data.repository.PlayerProfile
import com.euroleague.courtside.i18n.LocalCourtsideStrings
import com.euroleague.courtside.i18n.localizedLabel
import com.euroleague.courtside.ui.theme.CourtsideClubCrest
import com.euroleague.courtside.ui.theme.CourtsideDesignTokens
import kotlin.math.roundToInt

/**
 * Löst aus einer Spieler-ID / Spielernamen + Team-Code (z. B. aus Boxscore oder Play-by-Play)
 * zuverlässig das zugehörige `PlayerProfile` auf oder erzeugt ein Fallback-Profil aus den Spieldaten.
 */
internal fun resolveClickedPlayerProfile(
    playerId: String,
    playerName: String,
    teamId: String,
    allPlayers: List<PlayerProfile>,
    allGames: List<EuroLeagueGame>
): PlayerProfile? {
    if (playerId.isBlank() && playerName.isBlank()) return null
    val canonClub = EuroLeagueMockRepository.canonicalClubId(teamId)
    val normName = playerName.trim().lowercase()
    val surname = normName.substringAfterLast(" ")

    allPlayers.firstOrNull { it.id.equals(playerId, ignoreCase = true) }?.let { return it }
    allPlayers.firstOrNull {
        EuroLeagueMockRepository.canonicalClubId(it.teamId) == canonClub &&
            it.name.trim().equals(playerName.trim(), ignoreCase = true)
    }?.let { return it }
    if (surname.length >= 3) {
        allPlayers.firstOrNull {
            EuroLeagueMockRepository.canonicalClubId(it.teamId) == canonClub &&
                it.name.lowercase().contains(surname)
        }?.let { return it }
    }

    // Fallback: Baue ein dynamisches Profil aus vorhandenen Boxscores des Spielers
    val matchingStats = allGames.flatMap { g -> g.boxscoreHome + g.boxscoreAway }.filter { st ->
        (playerId.isNotBlank() && st.playerId.equals(playerId, ignoreCase = true)) ||
            (normName.isNotBlank() && st.playerName.trim().equals(playerName.trim(), ignoreCase = true))
    }
    if (matchingStats.isEmpty() && playerName.isBlank()) return null

    val gp = matchingStats.size.coerceAtLeast(1)
    val avgPts = ((matchingStats.sumOf { it.points }.toFloat() / gp) * 10f).roundToInt() / 10f
    val avgReb = ((matchingStats.sumOf { it.totalRebounds }.toFloat() / gp) * 10f).roundToInt() / 10f
    val avgAst = ((matchingStats.sumOf { it.assists }.toFloat() / gp) * 10f).roundToInt() / 10f
    val avgPir = ((matchingStats.sumOf { it.pir }.toFloat() / gp) * 10f).roundToInt() / 10f
    val jersey = matchingStats.firstOrNull()?.jerseyNumber ?: 0

    return PlayerProfile(
        id = playerId.ifBlank { "${canonClub}_${playerName.hashCode()}" },
        teamId = canonClub.ifBlank { teamId },
        number = jersey,
        name = playerName.ifBlank { "EuroLeague Player" },
        position = "G/F",
        nationality = "EU",
        heightCm = 198,
        ppg = avgPts,
        rpg = avgReb,
        apg = avgAst,
        pir = avgPir,
        injuryStatus = InjuryStatus.FIT
    )
}

@Composable
internal fun PlayerProfileDialog(
    player: PlayerProfile,
    team: EuroLeagueTeam?,
    games: List<EuroLeagueGame>,
    isCompact: Boolean,
    onDismiss: () -> Unit,
    onOpenGameInMatchCenter: (gameId: String, round: Int) -> Unit
) {
    val strings = LocalCourtsideStrings.current
    val context = LocalContext.current
    val summary = remember(player.id, games.size) {
        EuroLeagueMockRepository.buildPlayerScoutingSummary(player, games)
    }

    val primaryColor = team?.primaryColor ?: CourtsideDesignTokens.EuroLeagueOrange
    val secondaryColor = team?.secondaryColor ?: Color(0xFF1A1A22)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color(0xFF121217),
            shape = RoundedCornerShape(if (isCompact) 20.dp else 22.dp),
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.95f else 0.78f)
                .widthIn(max = 760.dp)
                .heightIn(max = if (isCompact) 680.dp else 720.dp)
                .border(1.5.dp, Color(0x44FF6600), RoundedCornerShape(if (isCompact) 20.dp else 22.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Dual-Tone Club & Player Hero Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.42f),
                                    Color(0xFF16161D),
                                    secondaryColor.copy(alpha = 0.36f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Headshot / Jersey Avatar
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 56.dp else 66.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1F1F28))
                                    .border(2.dp, CourtsideDesignTokens.EuroLeagueOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${player.number}",
                                    color = CourtsideDesignTokens.EuroLeagueOrange,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                if (player.headshotUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(player.headshotUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = player.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (team != null) {
                                        CourtsideClubCrest(team = team, size = 18.dp)
                                    }
                                    Text(
                                        text = "${team?.name ?: player.teamId} • #${player.number} • ${player.position}",
                                        color = Color(0xFFD1D5DB),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = player.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isCompact) 18.sp else 21.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val injColor = when (player.injuryStatus) {
                                    InjuryStatus.FIT -> CourtsideDesignTokens.NeonMadeGreen
                                    InjuryStatus.GAME_TIME_DECISION, InjuryStatus.QUESTIONABLE -> CourtsideDesignTokens.EuroLeagueOrange
                                    InjuryStatus.OUT -> CourtsideDesignTokens.NeonMissedRed
                                }
                                val bioLine = buildString {
                                    append("${player.heightCm} cm • ${player.nationality} • ${player.injuryStatus.localizedLabel(strings)}")
                                    if (player.injuryDetail.isNotBlank()) append(" (${player.injuryDetail})")
                                }
                                Text(
                                    text = bioLine,
                                    color = injColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.closeButtonLabel,
                                tint = Color.White
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0x22FFFFFF))

                // 2. Scrollable Scouting Body: Season Averages + Shooting Splits + PIR Trend + Game Log
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Season Averages Row
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strings.seasonAveragesHeader,
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlayerStatBox("PPG", "${player.ppg}", false, Modifier.weight(1f))
                                PlayerStatBox("RPG", "${player.rpg}", false, Modifier.weight(1f))
                                PlayerStatBox("APG", "${player.apg}", false, Modifier.weight(1f))
                                PlayerStatBox(strings.minutesColumn, summary.avgMinutes, false, Modifier.weight(1f))
                                PlayerStatBox("PIR", "${player.pir}", true, Modifier.weight(1f))
                            }
                        }
                    }

                    // Shooting Splits + PIR Form Trend
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF181820))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.shootingSplitsHeader,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            ShootingSplitBar(
                                label = "2P%",
                                pct = summary.twoPtPct,
                                detail = if (summary.twoPtAttempted > 0) "${summary.twoPtMade}/${summary.twoPtAttempted}" else "",
                                color = CourtsideDesignTokens.EuroLeagueOrange
                            )
                            ShootingSplitBar(
                                label = "3P%",
                                pct = summary.threePtPct,
                                detail = if (summary.threePtAttempted > 0) "${summary.threePtMade}/${summary.threePtAttempted}" else "",
                                color = CourtsideDesignTokens.NeonMadeGreen
                            )
                            ShootingSplitBar(
                                label = "FT%",
                                pct = summary.ftPct,
                                detail = if (summary.ftAttempted > 0) "${summary.ftMade}/${summary.ftAttempted}" else "",
                                color = Color(0xFF38BDF8)
                            )

                            if (summary.gameLog.size >= 2) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.pirTrendHeader,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                PirSparklineCanvas(
                                    entriesChronological = summary.gameLog.take(10).asReversed(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                )
                            }
                        }
                    }

                    // EuroLeague Round-by-Round Game Log
                    if (summary.gameLog.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${strings.gameLogHeader} (${summary.gamesLogged})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("RND", Modifier.weight(0.9f), color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(strings.opponentColumn, Modifier.weight(1.4f), color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(strings.resultColumn, Modifier.weight(1.5f), color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("PTS", Modifier.weight(0.8f), color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("REB", Modifier.weight(0.8f), color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("AST", Modifier.weight(0.8f), color = Color(0xFF8E8E93), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text("PIR", Modifier.weight(0.9f), color = CourtsideDesignTokens.EuroLeagueOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = Color(0x1AFFFFFF))
                            }
                        }

                        items(summary.gameLog, key = { "log_${it.gameId}_${it.round}" }) { entry ->
                            GameLogRow(
                                entry = entry,
                                winLetter = strings.winLetter,
                                lossLetter = strings.lossLetter,
                                onClick = {
                                    onDismiss()
                                    onOpenGameInMatchCenter(entry.gameId, entry.round)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatBox(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) Color(0x28FF6600) else Color(0xFF1A1A22))
            .border(
                1.dp,
                if (highlight) CourtsideDesignTokens.EuroLeagueOrange else Color(0x22FFFFFF),
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (highlight) CourtsideDesignTokens.EuroLeagueOrange else Color(0xFF9CA3AF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = if (highlight) CourtsideDesignTokens.EuroLeagueOrange else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ShootingSplitBar(
    label: String,
    pct: Float,
    detail: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFFD1D5DB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val rightLabel = if (detail.isNotBlank()) "${pct}% ($detail)" else "${pct}%"
            Text(rightLabel, color = color, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFF262630))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((pct / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun PirSparklineCanvas(
    entriesChronological: List<PlayerGameLogEntry>,
    modifier: Modifier = Modifier
) {
    val orange = CourtsideDesignTokens.EuroLeagueOrange
    Canvas(modifier = modifier) {
        if (entriesChronological.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val pirs = entriesChronological.map { it.stat.pir.toFloat() }
        val minPir = (pirs.minOrNull() ?: 0f).coerceAtMost(0f)
        val maxPir = (pirs.maxOrNull() ?: 25f).coerceAtLeast(15f)
        val span = (maxPir - minPir).coerceAtLeast(1f)

        val points = pirs.mapIndexed { idx, v ->
            val x = (idx.toFloat() / (pirs.size - 1).toFloat()) * (w - 16f) + 8f
            val norm = (v - minPir) / span
            val y = h - 8f - norm * (h - 16f)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { pt -> lineTo(pt.x, pt.y) }
        }
        drawPath(
            path = linePath,
            color = orange,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
        points.forEach { pt ->
            drawCircle(color = orange.copy(alpha = 0.28f), radius = 7f, center = pt)
            drawCircle(color = orange, radius = 3.6f, center = pt)
        }
    }
}

@Composable
private fun GameLogRow(
    entry: PlayerGameLogEntry,
    winLetter: String,
    lossLetter: String,
    onClick: () -> Unit
) {
    val resColor = when {
        entry.isLive -> CourtsideDesignTokens.EuroLeagueOrange
        entry.isWin -> CourtsideDesignTokens.NeonMadeGreen
        else -> CourtsideDesignTokens.NeonMissedRed
    }
    val resPrefix = when {
        entry.isLive -> "LIVE"
        entry.isWin -> winLetter
        else -> lossLetter
    }
    val venueSymbol = if (entry.isHome) "vs" else "@"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "R${entry.round}",
                modifier = Modifier.weight(0.9f),
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$venueSymbol ${entry.opponentCode}",
                modifier = Modifier.weight(1.4f),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = "$resPrefix ${entry.teamScore}:${entry.opponentScore}",
                modifier = Modifier.weight(1.5f),
                color = resColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "${entry.stat.points}",
                modifier = Modifier.weight(0.8f),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${entry.stat.totalRebounds}",
                modifier = Modifier.weight(0.8f),
                color = Color(0xFFD1D5DB),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${entry.stat.assists}",
                modifier = Modifier.weight(0.8f),
                color = Color(0xFFD1D5DB),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${entry.stat.pir}",
                modifier = Modifier.weight(0.9f),
                color = CourtsideDesignTokens.EuroLeagueOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.End
            )
        }
        HorizontalDivider(color = Color(0x0DFFFFFF))
    }
}
