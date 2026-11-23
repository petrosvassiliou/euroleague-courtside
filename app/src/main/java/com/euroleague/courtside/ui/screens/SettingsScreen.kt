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
import com.euroleague.courtside.widget.CourtsideAppWidgetProvider
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
// TAB 5 (Ctrl+5): SETTINGS, 6-LANGUAGE SWITCHER & PUSH NOTIFICATION STUDIO
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsAndNotificationTestScreen(
    primaryTeam: EuroLeagueTeam,
    games: List<EuroLeagueGame>,
    activeLanguage: AppLanguage,
    isCompact: Boolean = false,
    onSelectLanguage: (AppLanguage) -> Unit,
    notificationManager: EuroLeagueNotificationManager,
    tipOffEnabled: Boolean = true,
    halftimeFinalEnabled: Boolean = true,
    closeGameAlertEnabled: Boolean = true,
    onUpdateNotificationChannels: (tipOff: Boolean, halftimeFinal: Boolean, closeGame: Boolean) -> Unit = { _, _, _ -> },
    updateInfo: AppUpdateInfo = AppUpdateInfo(),
    autoUpdateCheckEnabled: Boolean = true,
    onToggleAutoUpdateCheck: (Boolean) -> Unit = {},
    onCheckForUpdatesManual: () -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    onOpenReleaseNotes: () -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalCourtsideStrings.current
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val sampleLiveGame = games.firstOrNull { it.status == GameStatus.LIVE } ?: games.first()
    val hasUpdateAvailable = updateInfo.status == UpdateStatus.UPDATE_AVAILABLE
    val isCheckingUpdates = updateInfo.status == UpdateStatus.CHECKING
    val previewPayload = remember(sampleLiveGame, activeLanguage) {
        notificationManager.buildTestPushContent(sampleLiveGame, activeLanguage)
    }

    if (isCompact) {
        // Smartphone (COMPACT): Einheitlicher Vollbreiten-Feed ohne gequetschte Doppelspalten
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CourtsideDesignTokens.ShellObsidian)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 6-Language Selector Card
            CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.languageSectionTitle,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Surface(
                            color = Color(0x26FF6600),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, Color(0x66FF6600), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
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
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
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
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
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

            // 2. Google Play Protect-Compliant App Updates Card
            CourtsideGlassCard(
                isSelected = true,
                glowColor = if (hasUpdateAvailable) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.EuroLeagueOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = strings.updateSectionTitle,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Surface(
                            color = Color(0x2600E676),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, Color(0x6600E676), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = strings.playProtectBadge,
                                color = CourtsideDesignTokens.NeonMadeGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF15151B))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.autoUpdateToggleTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = strings.autoUpdateToggleSubtitle,
                                color = CourtsideDesignTokens.MutedText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoUpdateCheckEnabled,
                            onCheckedChange = onToggleAutoUpdateCheck,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CourtsideDesignTokens.NeonMadeGreen
                            )
                        )
                    }

                    val statusHeadline = when (updateInfo.status) {
                        UpdateStatus.CHECKING -> strings.checkingUpdatesLabel
                        UpdateStatus.UPDATE_AVAILABLE -> "${strings.updateAvailablePrefix} v${updateInfo.latestVersionName}" +
                            (updateInfo.latestVersionCode?.let { " (Build $it)" } ?: "")
                        UpdateStatus.ERROR -> "⚠️ ${updateInfo.errorMessage ?: "GitHub Releases unreachable"}"
                        else -> "${strings.upToDateLabel} (v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE})"
                    }
                    val statusColor = when (updateInfo.status) {
                        UpdateStatus.CHECKING -> CourtsideDesignTokens.EuroLeagueOrange
                        UpdateStatus.UPDATE_AVAILABLE -> CourtsideDesignTokens.NeonMadeGreen
                        UpdateStatus.ERROR -> CourtsideDesignTokens.NeonMissedRed
                        else -> CourtsideDesignTokens.NeonMadeGreen
                    }

                    Surface(
                        color = Color(0xFF141419),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        color = CourtsideDesignTokens.EuroLeagueOrange,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = statusHeadline,
                                    color = statusColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            val subText = buildString {
                                if (updateInfo.releaseTitle.isNotBlank()) {
                                    append(updateInfo.releaseTitle)
                                } else {
                                    append("v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                                }
                                if (updateInfo.lastCheckedTime.isNotBlank()) {
                                    append(" • ${updateInfo.lastCheckedTime}")
                                }
                                if (!updateInfo.sha256Digest.isNullOrBlank()) {
                                    append(" • SHA-256: ${updateInfo.sha256Digest.take(12)}…")
                                }
                            }
                            Text(
                                text = subText,
                                color = Color(0xFFD1D5DB),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCheckForUpdatesManual,
                            enabled = !isCheckingUpdates,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.EuroLeagueOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = strings.checkForUpdatesButton,
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onOpenReleaseNotes,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.PillSurface),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text(
                                text = strings.viewReleaseNotesButton,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (hasUpdateAvailable) {
                        Button(
                            onClick = onInstallUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.NeonMadeGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${strings.downloadInstallUpdateButton} (v${updateInfo.latestVersionName})",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Notification Channels + Auto Game-Night Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.notificationChannelsTitle, style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
                Text(
                    text = strings.autoGameAlertsBadge,
                    color = CourtsideDesignTokens.NeonMadeGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            CourtsideGlassCard(isSelected = tipOffEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "WORKMANAGER",
                    title = "${strings.tipOffChannelTitle} (${primaryTeam.code})",
                    subtitle = strings.tipOffChannelSubtitle,
                    checked = tipOffEnabled,
                    onCheckedChange = { onUpdateNotificationChannels(it, halftimeFinalEnabled, closeGameAlertEnabled) }
                )
            }
            CourtsideGlassCard(isSelected = halftimeFinalEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "SCORES",
                    title = strings.scoresChannelTitle,
                    subtitle = strings.scoresChannelSubtitle,
                    checked = halftimeFinalEnabled,
                    onCheckedChange = { onUpdateNotificationChannels(tipOffEnabled, it, closeGameAlertEnabled) }
                )
            }
            CourtsideGlassCard(isSelected = closeGameAlertEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "CRUNCH TIME",
                    title = strings.closeGameChannelTitle,
                    subtitle = strings.closeGameChannelSubtitle,
                    checked = closeGameAlertEnabled,
                    onCheckedChange = { onUpdateNotificationChannels(tipOffEnabled, halftimeFinalEnabled, it) }
                )
            }

            // Android Home-Screen Courtside Widget Card
            HomeScreenWidgetPromoCard(
                primaryTeam = primaryTeam,
                onPinWidget = {
                    val requested = CourtsideAppWidgetProvider.requestPinCourtsideWidget(context)
                    CourtsideAppWidgetProvider.updateAllWidgets(context)
                    statusBanner = if (requested) {
                        "📌 ${strings.widgetSectionTitle}"
                    } else {
                        "✓ ${strings.widgetSectionTitle} (${primaryTeam.code})"
                    }
                }
            )

            // 4. Push Preview & Instant Test Button
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
                        fontSize = 15.sp
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
                    .height(52.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.sendTestPushButton,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        return
    }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.notificationChannelsTitle, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    text = strings.autoGameAlertsBadge,
                    color = CourtsideDesignTokens.NeonMadeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            CourtsideGlassCard(isSelected = tipOffEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "WORKMANAGER",
                    title = "${strings.tipOffChannelTitle} (${primaryTeam.code})",
                    subtitle = strings.tipOffChannelSubtitle,
                    checked = tipOffEnabled,
                    onCheckedChange = { onUpdateNotificationChannels(it, halftimeFinalEnabled, closeGameAlertEnabled) }
                )
            }
            CourtsideGlassCard(isSelected = halftimeFinalEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "SCORES",
                    title = strings.scoresChannelTitle,
                    subtitle = strings.scoresChannelSubtitle,
                    checked = halftimeFinalEnabled,
                    onCheckedChange = { onUpdateNotificationChannels(tipOffEnabled, it, closeGameAlertEnabled) }
                )
            }
            CourtsideGlassCard(isSelected = closeGameAlertEnabled, modifier = Modifier.fillMaxWidth()) {
                ChannelToggleRow(
                    badge = "CRUNCH TIME",
                    title = strings.closeGameChannelTitle,
                    subtitle = strings.closeGameChannelSubtitle,
                    checked = closeGameAlertEnabled,
                    onCheckedChange = { onUpdateNotificationChannels(tipOffEnabled, halftimeFinalEnabled, it) }
                )
            }

            HomeScreenWidgetPromoCard(
                primaryTeam = primaryTeam,
                onPinWidget = {
                    val requested = CourtsideAppWidgetProvider.requestPinCourtsideWidget(context)
                    CourtsideAppWidgetProvider.updateAllWidgets(context)
                    statusBanner = if (requested) {
                        "📌 ${strings.widgetSectionTitle}"
                    } else {
                        "✓ ${strings.widgetSectionTitle} (${primaryTeam.code})"
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Google Play Protect-Compliant App Updates & Manual Update Checker Card
            CourtsideGlassCard(
                isSelected = true,
                glowColor = if (hasUpdateAvailable) CourtsideDesignTokens.NeonMadeGreen else CourtsideDesignTokens.EuroLeagueOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.updateSectionTitle,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color(0x2600E676),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.border(1.dp, Color(0x6600E676), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = strings.playProtectBadge,
                                color = CourtsideDesignTokens.NeonMadeGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Auto-Update Check Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF15151B))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.autoUpdateToggleTitle,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = strings.autoUpdateToggleSubtitle,
                                color = CourtsideDesignTokens.MutedText,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = autoUpdateCheckEnabled,
                            onCheckedChange = onToggleAutoUpdateCheck,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CourtsideDesignTokens.NeonMadeGreen
                            ),
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        )
                    }

                    // Live Update Status Box
                    val statusHeadline = when (updateInfo.status) {
                        UpdateStatus.CHECKING -> strings.checkingUpdatesLabel
                        UpdateStatus.UPDATE_AVAILABLE -> "${strings.updateAvailablePrefix} v${updateInfo.latestVersionName}" +
                            (updateInfo.latestVersionCode?.let { " (Build $it)" } ?: "")
                        UpdateStatus.ERROR -> "⚠️ ${updateInfo.errorMessage ?: "GitHub Releases unreachable"}"
                        else -> "${strings.upToDateLabel} (v${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE})"
                    }
                    val statusColor = when (updateInfo.status) {
                        UpdateStatus.CHECKING -> CourtsideDesignTokens.EuroLeagueOrange
                        UpdateStatus.UPDATE_AVAILABLE -> CourtsideDesignTokens.NeonMadeGreen
                        UpdateStatus.ERROR -> CourtsideDesignTokens.NeonMissedRed
                        else -> CourtsideDesignTokens.NeonMadeGreen
                    }

                    Surface(
                        color = Color(0xFF141419),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        color = CourtsideDesignTokens.EuroLeagueOrange,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = statusHeadline,
                                    color = statusColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            val subText = buildString {
                                if (updateInfo.releaseTitle.isNotBlank()) {
                                    append(updateInfo.releaseTitle)
                                } else {
                                    append("v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                                }
                                if (updateInfo.lastCheckedTime.isNotBlank()) {
                                    append(" • ${updateInfo.lastCheckedTime}")
                                }
                                if (!updateInfo.sha256Digest.isNullOrBlank()) {
                                    append(" • SHA-256: ${updateInfo.sha256Digest.take(12)}…")
                                }
                            }
                            Text(
                                text = subText,
                                color = Color(0xFFD1D5DB),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Manual Check for Updates + Release Notes / Install Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCheckForUpdatesManual,
                            enabled = !isCheckingUpdates,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.EuroLeagueOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = strings.checkForUpdatesButton,
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onOpenReleaseNotes,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.PillSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = strings.viewReleaseNotesButton,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (hasUpdateAvailable) {
                        Button(
                            onClick = onInstallUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = CourtsideDesignTokens.NeonMadeGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text(
                                text = "${strings.downloadInstallUpdateButton} (v${updateInfo.latestVersionName})",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
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

@Composable
private fun HomeScreenWidgetPromoCard(
    primaryTeam: EuroLeagueTeam,
    onPinWidget: () -> Unit
) {
    val strings = LocalCourtsideStrings.current
    CourtsideGlassCard(isSelected = true, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                CourtsideClubCrest(team = primaryTeam, size = 34.dp)
                Column {
                    Text(
                        text = strings.widgetSectionTitle,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = strings.widgetSectionSubtitle,
                        color = CourtsideDesignTokens.MutedText,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0x2EFF6600),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .border(1.dp, CourtsideDesignTokens.EuroLeagueOrange, RoundedCornerShape(10.dp))
                    .clickable { onPinWidget() }
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(
                    text = "📌 ${strings.widgetAddHint}",
                    color = CourtsideDesignTokens.EuroLeagueOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}
