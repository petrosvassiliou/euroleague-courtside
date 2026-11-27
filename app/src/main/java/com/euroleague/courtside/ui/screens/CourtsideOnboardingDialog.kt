package com.euroleague.courtside.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.euroleague.courtside.data.repository.EuroLeagueTeam
import com.euroleague.courtside.i18n.AppLanguage
import com.euroleague.courtside.i18n.LocalCourtsideStrings
import com.euroleague.courtside.ui.theme.AppThemeMode
import com.euroleague.courtside.ui.theme.CourtsideClubCrest
import com.euroleague.courtside.ui.theme.CourtsideDesignTokens

/**
 * v2.0 Interaktives Erststart-Onboarding ("Welcome & Club Setup"):
 * - Erlaubt beim ersten App-Start (und jederzeit erneut über Settings) die Sofort-Auswahl von:
 *   1. Sprache (`EN`, `DE`, `FR`, `IT`, `ES`, `EL`) & Erscheinungsbild (`☀️ Light` / `🌙 Dark`)
 *   2. Lieblingsverein ("My Team" aus allen 20 EuroLeague-Clubs)
 *   3. Spieltags-Push-Alarme & Sperrbildschirm Live-Tracker (`CourtsideLiveTrackerService`)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CourtsideOnboardingDialog(
    allTeams: List<EuroLeagueTeam>,
    selectedTeam: EuroLeagueTeam,
    activeLanguage: AppLanguage,
    activeThemeMode: AppThemeMode,
    isCompact: Boolean,
    tipOffEnabled: Boolean,
    halftimeFinalEnabled: Boolean,
    closeGameAlertEnabled: Boolean,
    lockScreenTrackerEnabled: Boolean,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectPrimaryTeam: (String) -> Unit,
    onUpdateNotificationChannels: (tipOff: Boolean, halftimeFinal: Boolean, closeGame: Boolean) -> Unit,
    onToggleLockScreenTracker: (Boolean) -> Unit,
    onCompleteOnboarding: () -> Unit
) {
    val strings = LocalCourtsideStrings.current

    Dialog(
        onDismissRequest = onCompleteOnboarding,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = CourtsideDesignTokens.CardSurface,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 0.95f else 0.72f)
                .widthIn(max = 640.dp)
                .heightIn(max = if (isCompact) 680.dp else 740.dp)
                .border(1.5.dp, Color(0x66FF6600), RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hero Header mit dynamischem Club-Farbverlauf
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    CourtsideDesignTokens.EuroLeagueOrange.copy(alpha = 0.22f),
                                    CourtsideDesignTokens.BannerCenterSurface,
                                    selectedTeam.primaryColor.copy(alpha = 0.26f)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0x28FF6600),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.border(1.dp, CourtsideDesignTokens.EuroLeagueOrange, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsBasketball,
                                contentDescription = null,
                                tint = CourtsideDesignTokens.EuroLeagueOrange,
                                modifier = Modifier.padding(8.dp).size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.onboardingWelcomeTitle,
                                color = CourtsideDesignTokens.PrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (isCompact) 16.sp else 18.sp
                            )
                            Text(
                                text = strings.onboardingWelcomeSubtitle,
                                color = CourtsideDesignTokens.SecondaryText,
                                fontSize = 12.sp
                            )
                        }
                        CourtsideClubCrest(team = selectedTeam, size = 34.dp)
                    }
                }

                HorizontalDivider(color = CourtsideDesignTokens.SubtleGlassBorder)

                // Scrollbarer Setup-Inhalt
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Schnellwahl: Sprache + Light/Dark Theme
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = lang == activeLanguage
                            Surface(
                                color = if (isSelected) Color(0x2EFF6600) else CourtsideDesignTokens.PillSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SubtleGlassBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectLanguage(lang) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = "${lang.flagEmoji} ${lang.code.uppercase()}",
                                    color = if (isSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SecondaryText,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }

                        listOf(
                            AppThemeMode.LIGHT to "☀️ Light",
                            AppThemeMode.DARK to "🌙 Dark"
                        ).forEach { (mode, label) ->
                            val isSelected = mode == activeThemeMode
                            Surface(
                                color = if (isSelected) Color(0x2EFF6600) else CourtsideDesignTokens.PillSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SubtleGlassBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectThemeMode(mode) }
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SecondaryText,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // 1. Lieblingsverein ("My Team") aus allen 20 EuroLeague-Clubs wählen
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.onboardingPickClubTitle,
                                color = CourtsideDesignTokens.PrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = selectedTeam.name,
                                color = CourtsideDesignTokens.EuroLeagueOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allTeams.forEach { club ->
                                val selected = club.id == selectedTeam.id
                                Surface(
                                    color = if (selected) Color(0x28FF6600) else CourtsideDesignTokens.PillSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) CourtsideDesignTokens.EuroLeagueOrange else CourtsideDesignTokens.SubtleGlassBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectPrimaryTeam(club.id) }
                                        .pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        CourtsideClubCrest(team = club, size = 20.dp)
                                        Text(
                                            text = club.code,
                                            color = if (selected) CourtsideDesignTokens.PrimaryText else CourtsideDesignTokens.SecondaryText,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = CourtsideDesignTokens.EuroLeagueOrange,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Spieltags-Alarme & Sperrbildschirm-Tracker
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            text = strings.onboardingAlertsTitle,
                            color = CourtsideDesignTokens.PrimaryText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )

                        OnboardingToggleRow(
                            badge = "LOCK SCREEN",
                            title = strings.lockScreenTrackerTitle,
                            subtitle = strings.lockScreenTrackerSubtitle,
                            checked = lockScreenTrackerEnabled,
                            accent = CourtsideDesignTokens.NeonMadeGreen,
                            onCheckedChange = onToggleLockScreenTracker
                        )
                        OnboardingToggleRow(
                            badge = "TIP-OFF",
                            title = "${strings.tipOffChannelTitle} (${selectedTeam.code})",
                            subtitle = strings.tipOffChannelSubtitle,
                            checked = tipOffEnabled,
                            accent = CourtsideDesignTokens.EuroLeagueOrange,
                            onCheckedChange = { onUpdateNotificationChannels(it, halftimeFinalEnabled, closeGameAlertEnabled) }
                        )
                        OnboardingToggleRow(
                            badge = "SCORES",
                            title = strings.scoresChannelTitle,
                            subtitle = strings.scoresChannelSubtitle,
                            checked = halftimeFinalEnabled,
                            accent = CourtsideDesignTokens.EuroLeagueOrange,
                            onCheckedChange = { onUpdateNotificationChannels(tipOffEnabled, it, closeGameAlertEnabled) }
                        )
                        OnboardingToggleRow(
                            badge = "CRUNCH",
                            title = strings.closeGameChannelTitle,
                            subtitle = strings.closeGameChannelSubtitle,
                            checked = closeGameAlertEnabled,
                            accent = CourtsideDesignTokens.NeonMissedRed,
                            onCheckedChange = { onUpdateNotificationChannels(tipOffEnabled, halftimeFinalEnabled, it) }
                        )
                    }
                }

                HorizontalDivider(color = CourtsideDesignTokens.SubtleGlassBorder)

                // Primärer Abschluss-Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CourtsideDesignTokens.ElevatedSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onCompleteOnboarding,
                        colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.EuroLeagueOrange),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text(
                            text = strings.onboardingFinishButton,
                            color = CourtsideDesignTokens.OnAccentText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingToggleRow(
    badge: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CourtsideDesignTokens.PillSurface)
            .border(
                width = 1.dp,
                color = if (checked) accent.copy(alpha = 0.55f) else CourtsideDesignTokens.SubtleGlassBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Text(
                        text = badge,
                        color = accent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = title,
                    color = CourtsideDesignTokens.PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = CourtsideDesignTokens.MutedText,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CourtsideDesignTokens.OnAccentText,
                checkedTrackColor = accent
            )
        )
    }
}
