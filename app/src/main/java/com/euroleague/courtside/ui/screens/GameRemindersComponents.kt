package com.euroleague.courtside.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euroleague.courtside.data.repository.EuroLeagueGame
import com.euroleague.courtside.i18n.CourtsideStrings
import com.euroleague.courtside.i18n.LocalCourtsideStrings
import com.euroleague.courtside.notifications.GameReminderType
import com.euroleague.courtside.ui.theme.CourtsideClubCrest
import com.euroleague.courtside.ui.theme.CourtsideDesignTokens

internal fun GameReminderType.localizedTitle(strings: CourtsideStrings): String = when (this) {
    GameReminderType.Q1_RESULT -> strings.reminderQ1Label
    GameReminderType.HALFTIME_RESULT -> strings.reminderHalftimeLabel
    GameReminderType.Q3_RESULT -> strings.reminderQ3Label
    GameReminderType.CRUNCH_TIME -> strings.reminderCrunchTimeLabel
    GameReminderType.FINAL_SCORE -> strings.reminderFinalScoreLabel
}

/**
 * Kompakter Glocken-Button auf jeder Spielkarte (Match Center & My Team Hub):
 * Zeigt auf einen Blick, ob und wie viele spielspezifische Erinnerungen (Q1, HT, Q3, Crunch, FT) aktiv sind.
 */
@Composable
internal fun GameReminderBellPill(
    activeReminders: Set<GameReminderType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActive = activeReminders.isNotEmpty()
    val isAll = activeReminders.size == GameReminderType.entries.size
    Surface(
        color = if (hasActive) Color(0x2EFF6600) else CourtsideDesignTokens.PillSurface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (hasActive) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SubtleGlassBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = if (hasActive) 7.dp else 6.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (hasActive) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                contentDescription = "Game Reminders",
                tint = if (hasActive) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.MutedText,
                modifier = Modifier.size(14.dp)
            )
            if (hasActive) {
                Text(
                    text = if (isAll) "ALL" else "${activeReminders.size}",
                    color = CourtsideDesignTokens.EuroLeagueOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 1-Tap Schnellwahl-Leiste direkt unter dem Scoreboard im Match Center für spielspezifische Alarme:
 * `[🔔]` `[Q1]` `[HT]` `[Q3]` `[🔥 Q4]` `[FT]` `[All]`
 */
@Composable
internal fun GameRemindersQuickStrip(
    activeReminders: Set<GameReminderType>,
    onToggleReminder: (GameReminderType) -> Unit,
    onToggleAll: () -> Unit,
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalCourtsideStrings.current
    val allSelected = activeReminders.size == GameReminderType.entries.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CourtsideDesignTokens.ElevatedSurface)
            .border(1.dp, CourtsideDesignTokens.SubtleGlassBorder, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onOpenDialog() }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (activeReminders.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                contentDescription = strings.gameRemindersTitle,
                tint = if (activeReminders.isNotEmpty()) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SecondaryText,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = strings.gameRemindersTitle.removePrefix("🔔").trim(),
                color = CourtsideDesignTokens.SecondaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        GameReminderType.entries.forEach { type ->
            val selected = type in activeReminders
            val activeAccent = if (type == GameReminderType.CRUNCH_TIME) {
                CourtsideDesignTokens.NeonMissedRed
            } else {
                CourtsideDesignTokens.EuroLeagueOrange
            }
            Surface(
                color = if (selected) activeAccent.copy(alpha = 0.20f) else CourtsideDesignTokens.PillSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .border(
                        width = if (selected) 1.2.dp else 1.dp,
                        color = if (selected) activeAccent else CourtsideDesignTokens.SubtleGlassBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleReminder(type) }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(
                    text = type.shortBadge,
                    color = if (selected) activeAccent else CourtsideDesignTokens.MutedText,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            color = if (allSelected) CourtsideDesignTokens.NeonMadeGreen.copy(alpha = 0.20f) else CourtsideDesignTokens.PillSurface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (allSelected) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.SubtleGlassBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onToggleAll() }
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(
                text = if (allSelected) strings.clearRemindersLabel else strings.enableAllRemindersLabel,
                color = if (allSelected) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.SecondaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Vollständiger Spiel-Erinnerungs-Dialog für ein einzelnes Spiel:
 * Erlaubt das individuelle Aktivieren von Q1-Ergebnis, Halbzeit-Ergebnis, Q3-Ergebnis,
 * Crunch-Time und Endergebnis inklusive Schnell-Buttons ("Alle aktivieren" / "Alle löschen").
 */
@Composable
internal fun GameRemindersDialog(
    game: EuroLeagueGame,
    activeReminders: Set<GameReminderType>,
    isCompact: Boolean,
    onUpdateReminders: (Set<GameReminderType>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalCourtsideStrings.current
    val allTypes = GameReminderType.entries.toSet()
    val allEnabled = activeReminders.size == allTypes.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CourtsideDesignTokens.CardSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.94f else 0.62f)
                .widthIn(max = 500.dp)
                .border(1.5.dp, Color(0x55FF6600), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Matchup Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    game.homeTeam.primaryColor.copy(alpha = CourtsideDesignTokens.BannerClubAlpha),
                                    CourtsideDesignTokens.BannerCenterSurface,
                                    game.awayTeam.primaryColor.copy(alpha = CourtsideDesignTokens.BannerClubAlpha)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CourtsideClubCrest(team = game.homeTeam, size = 26.dp)
                            Text(
                                text = "${game.homeTeam.code} vs ${game.awayTeam.code}",
                                color = CourtsideDesignTokens.PrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            CourtsideClubCrest(team = game.awayTeam, size = 26.dp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "• ${strings.roundPrefix} ${game.round}",
                                color = CourtsideDesignTokens.MutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.closeButtonLabel,
                                tint = CourtsideDesignTokens.PrimaryText
                            )
                        }
                    }
                }

                HorizontalDivider(color = CourtsideDesignTokens.SubtleGlassBorder)

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.gameRemindersTitle,
                        color = CourtsideDesignTokens.PrimaryText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = strings.gameRemindersSubtitle,
                        color = CourtsideDesignTokens.MutedText,
                        fontSize = 12.sp
                    )

                    GameReminderType.entries.forEach { type ->
                        val checked = type in activeReminders
                        val accent = if (type == GameReminderType.CRUNCH_TIME) {
                            CourtsideDesignTokens.NeonMissedRed
                        } else {
                            CourtsideDesignTokens.EuroLeagueOrange
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CourtsideDesignTokens.PillSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (checked) accent.copy(alpha = 0.65f) else CourtsideDesignTokens.SubtleGlassBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val next = if (checked) activeReminders - type else activeReminders + type
                                    onUpdateReminders(next)
                                }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = accent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = type.shortBadge,
                                        color = accent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = type.localizedTitle(strings),
                                    color = CourtsideDesignTokens.PrimaryText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = checked,
                                onCheckedChange = { isOn ->
                                    val next = if (isOn) activeReminders + type else activeReminders - type
                                    onUpdateReminders(next)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CourtsideDesignTokens.OnAccentText,
                                    checkedTrackColor = accent
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onUpdateReminders(if (allEnabled) emptySet() else allTypes)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allEnabled) CourtsideDesignTokens.PillSurface else CourtsideDesignTokens.EuroLeagueOrange
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = if (allEnabled) strings.clearRemindersLabel else strings.enableAllRemindersLabel,
                                color = if (allEnabled) CourtsideDesignTokens.PrimaryText else CourtsideDesignTokens.OnAccentText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.NeonMadeGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.75f).pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = "✓ OK",
                                color = CourtsideDesignTokens.OnAccentText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
