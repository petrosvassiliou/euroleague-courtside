package com.euroleague.courtside.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.data.repository.EuroLeagueMockRepository
import com.euroleague.courtside.data.repository.PlayByPlayEvent
import com.euroleague.courtside.data.repository.PlayEventCategory
import com.euroleague.courtside.i18n.LocalCourtsideStrings
import com.euroleague.courtside.ui.theme.CourtsideClubCrest
import com.euroleague.courtside.ui.theme.CourtsideDesignTokens
import kotlin.math.abs

@Composable
internal fun CourtVisualModeToggle(
    showMomentum: Boolean,
    onSelectMomentum: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141419))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedPillButton(
            label = strings.courtViewShotChartTab,
            selected = !showMomentum,
            onClick = { onSelectMomentum(false) },
            modifier = Modifier.weight(1f)
        )
        SegmentedPillButton(
            label = strings.courtViewMomentumTab,
            selected = showMomentum,
            onClick = { onSelectMomentum(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun BoxscorePlayByPlayModeToggle(
    showPlayByPlay: Boolean,
    playCount: Int,
    onSelectPlayByPlay: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141419))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentedPillButton(
            label = strings.playerBoxscoreHeader,
            selected = !showPlayByPlay,
            onClick = { onSelectPlayByPlay(false) },
            modifier = Modifier.weight(1f)
        )
        val pbpLabel = if (playCount > 0) "${strings.playByPlayTabLabel} ($playCount)" else strings.playByPlayTabLabel
        SegmentedPillButton(
            label = pbpLabel,
            selected = showPlayByPlay,
            onClick = { onSelectPlayByPlay(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedPillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0x33FF6600) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) CourtsideDesignTokens.EuroLeagueOrange else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF9CA3AF),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Broadcast Lead-Tracker & Momentum Canvas:
 * Zeichnet die Punkte-Differenz (`Home - Away`) über Q1..Q4 (+ OT) mit farbigem Glow-Bereich
 * für das jeweils führende Team sowie Kennzahlen zu höchster Führung und Führungswechseln.
 */
@Composable
internal fun GameFlowMomentumCard(
    game: EuroLeagueGame,
    events: List<PlayByPlayEvent>,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    val summary = remember(game.id, game.homeScore, game.awayScore, events.size) {
        EuroLeagueMockRepository.computeLeadMomentumSummary(game, events)
    }
    val homeColor = CourtsideDesignTokens.EuroLeagueOrange
    val awayColor = CourtsideDesignTokens.NeonMadeGreen

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF151519))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.leadTrackerTitle,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MomentumLegendPill(code = game.homeTeam.code, maxLead = summary.maxHomeLead, color = homeColor)
                MomentumLegendPill(code = game.awayTeam.code, maxLead = summary.maxAwayLead, color = awayColor)
                Surface(
                    color = Color(0xFF202028),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⇄ ${summary.leadChanges}",
                        color = Color(0xFFD1D5DB),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101014))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val maxAbsDiff = maxOf(summary.maxHomeLead, summary.maxAwayLead, 8).toFloat()

                // Quarter grid lines (Q1=10, Q2=20, Q3=30, Q4=40)
                val qMarks = if (summary.totalMinutes > 40f) listOf(10f, 20f, 30f, 40f) else listOf(10f, 20f, 30f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                qMarks.forEach { qm ->
                    val qx = (qm / summary.totalMinutes) * w
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = Offset(qx, 0f),
                        end = Offset(qx, h),
                        strokeWidth = 1.2f,
                        pathEffect = dash
                    )
                }

                // Zero (tie) baseline
                drawLine(
                    color = Color(0x55FFFFFF),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.6f
                )

                val pts = summary.points
                if (pts.size >= 2) {
                    val coords = pts.map { pt ->
                        val x = ((pt.gameMinute / summary.totalMinutes).coerceIn(0f, 1f)) * w
                        val normalized = (pt.diff.toFloat() / maxAbsDiff).coerceIn(-1f, 1f)
                        val y = midY - normalized * (midY * 0.84f)
                        Offset(x, y) to pt.diff
                    }

                    // Positive (Home) & Negative (Away) filled momentum polygons
                    val homeFill = Path().apply {
                        moveTo(coords.first().first.x, midY)
                        coords.forEach { (off, _) ->
                            lineTo(off.x, off.y.coerceAtMost(midY))
                        }
                        lineTo(coords.last().first.x, midY)
                        close()
                    }
                    val awayFill = Path().apply {
                        moveTo(coords.first().first.x, midY)
                        coords.forEach { (off, _) ->
                            lineTo(off.x, off.y.coerceAtLeast(midY))
                        }
                        lineTo(coords.last().first.x, midY)
                        close()
                    }
                    drawPath(homeFill, color = homeColor.copy(alpha = 0.22f))
                    drawPath(awayFill, color = awayColor.copy(alpha = 0.20f))

                    // Crisp momentum curve segments colored by leader
                    for (i in 0 until coords.lastIndex) {
                        val (p1, d1) = coords[i]
                        val (p2, d2) = coords[i + 1]
                        val segColor = if ((d1 + d2) >= 0) homeColor else awayColor
                        drawLine(
                            color = segColor,
                            start = p1,
                            end = p2,
                            strokeWidth = 3.2f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Live / Final endpoint marker
                    val (lastOffset, lastDiff) = coords.last()
                    val endColor = if (lastDiff >= 0) homeColor else awayColor
                    drawCircle(color = endColor.copy(alpha = 0.32f), radius = 9f, center = lastOffset)
                    drawCircle(color = endColor, radius = 4.5f, center = lastOffset)
                }
            }
        }

        // Quarter axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = if (summary.totalMinutes > 40f) {
                listOf("Q1", "Q2", "Q3", "Q4", "OT")
            } else {
                listOf("Q1", "Q2", "Q3", "Q4")
            }
            labels.forEach { qLabel ->
                Text(
                    text = qLabel,
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MomentumLegendPill(
    code: String,
    maxLead: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$code +$maxLead",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
internal fun PlayByPlayFilterBar(
    selectedQuarter: Int,
    hasOvertime: Boolean,
    clutchOnly: Boolean,
    onSelectQuarter: (Int) -> Unit,
    onToggleClutchOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    val quarters = buildList {
        add(0 to strings.allQuartersFilter)
        add(1 to "Q1")
        add(2 to "Q2")
        add(3 to "Q3")
        add(4 to "Q4")
        if (hasOvertime) add(5 to "OT")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        quarters.forEach { (qVal, label) ->
            val active = !clutchOnly && selectedQuarter == qVal
            Surface(
                color = if (active) Color(0x33FF6600) else Color(0xFF1A1A20),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (active) CourtsideDesignTokens.EuroLeagueOrange else Color(0x22FFFFFF),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectQuarter(qVal) }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else Color(0xFF9CA3AF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Surface(
            color = if (clutchOnly) Color(0x33FF3B5C) else Color(0xFF1A1A20),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .border(
                    1.dp,
                    if (clutchOnly) CourtsideDesignTokens.NeonMissedRed else Color(0x22FFFFFF),
                    RoundedCornerShape(8.dp)
                )
                .clickable { onToggleClutchOnly() }
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(
                text = strings.clutchFilterLabel,
                color = if (clutchOnly) CourtsideDesignTokens.NeonMissedRed else Color(0xFFD1D5DB),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
internal fun PlayByPlayEventRow(
    event: PlayByPlayEvent,
    game: EuroLeagueGame,
    onClickPlayer: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val isHomeEvent = EuroLeagueMockRepository.canonicalClubId(event.teamId) ==
        EuroLeagueMockRepository.canonicalClubId(game.homeTeam.id)
    val eventTeam = when {
        event.teamId.isBlank() -> null
        isHomeEvent -> game.homeTeam
        else -> game.awayTeam
    }
    val badgeColor = when (event.category) {
        PlayEventCategory.THREE_PT, PlayEventCategory.DUNK -> CourtsideDesignTokens.NeonMadeGreen
        PlayEventCategory.TWO_PT, PlayEventCategory.FREE_THROW -> CourtsideDesignTokens.EuroLeagueOrange
        PlayEventCategory.FOUL, PlayEventCategory.TURNOVER, PlayEventCategory.MISS -> CourtsideDesignTokens.NeonMissedRed
        PlayEventCategory.TIMEOUT -> Color(0xFFFBBF24)
        PlayEventCategory.STEAL_BLK -> Color(0xFF38BDF8)
        else -> Color(0xFF9CA3AF)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { base ->
                    if (event.playerName.isNotBlank() && event.teamId.isNotBlank()) {
                        base
                            .clickable {
                                onClickPlayer(event.playerId, event.playerName, event.teamId)
                            }
                            .pointerHoverIcon(PointerIcon.Hand)
                    } else base
                }
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quarter & Clock column
            val qTag = if (event.quarter >= 5) "OT" else "Q${event.quarter}"
            Text(
                text = "$qTag ${event.clock}",
                color = Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(54.dp)
            )

            // Club crest or neutral dot
            if (eventTeam != null) {
                CourtsideClubCrest(team = eventTeam, size = 20.dp)
            } else {
                Spacer(modifier = Modifier.size(20.dp))
            }

            // Category badge (3PT, DUNK, FOUL, TIMEOUT...)
            Surface(
                color = badgeColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            ) {
                Text(
                    text = if (event.pointsScored > 0) "+${event.pointsScored} ${event.category.badgeLabel}" else event.category.badgeLabel,
                    color = badgeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Player + Play description
            Column(modifier = Modifier.weight(1f)) {
                if (event.playerName.isNotBlank()) {
                    val numPrefix = event.jerseyNumber?.let { "#$it " }.orEmpty()
                    Text(
                        text = "$numPrefix${event.playerName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = event.description,
                    color = Color(0xFFB0B5C0),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Running Score pill
            Surface(
                color = if (event.isScoringPlay) Color(0x28FF6600) else Color(0xFF19191F),
                shape = RoundedCornerShape(7.dp)
            ) {
                Text(
                    text = "${event.homeScore}:${event.awayScore}",
                    color = if (event.isScoringPlay) CourtsideDesignTokens.EuroLeagueOrange else Color(0xFFD1D5DB),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0x0DFFFFFF))
    }
}
