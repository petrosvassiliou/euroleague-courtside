package com.euroleague.courtside.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.euroleague.courtside.data.repository.EuroLeagueTeam

/**
 * Unterstützte Erscheinungsbilder für EuroLeague Courtside.
 * Standardmäßig startet die App im hellen Daylight-Design (`LIGHT`).
 */
enum class AppThemeMode(val code: String) {
    LIGHT("light"),
    DARK("dark");

    val isLight: Boolean get() = this == LIGHT

    fun toggled(): AppThemeMode = if (this == LIGHT) DARK else LIGHT

    companion object {
        val DEFAULT: AppThemeMode = LIGHT

        fun fromCode(code: String?): AppThemeMode =
            if (code?.trim()?.equals("dark", ignoreCase = true) == true) DARK else LIGHT
    }
}

data class CourtsidePalette(
    val mode: AppThemeMode,
    val shellBackground: Color,
    val sidebarSurface: Color,
    val centerPaneSurface: Color,
    val detailPaneSurface: Color,
    val cardSurface: Color,
    val cardSelectedBase: Color,
    val elevatedSurface: Color,
    val pillSurface: Color,
    val subtleGlassBorder: Color,
    val dividerSubtle: Color,
    val zebraRowSurface: Color,
    val chartGridStrong: Color,
    val chartGridSubtle: Color,
    val euroLeagueOrange: Color,
    val liveBadgeOrangeRed: Color,
    val neonMadeGreen: Color,
    val neonMissedRed: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val onAccentText: Color,
    val bannerCenterSurface: Color,
    val bannerClubAlpha: Float
)

val LightCourtsidePalette = CourtsidePalette(
    mode = AppThemeMode.LIGHT,
    shellBackground = Color(0xFFF4F6FA),
    sidebarSurface = Color(0xFFFFFFFF),
    centerPaneSurface = Color(0xFFF8F9FC),
    detailPaneSurface = Color(0xFFF4F6FA),
    cardSurface = Color(0xFFFFFFFF),
    cardSelectedBase = Color(0xFFFFF5EE),
    elevatedSurface = Color(0xFFF0F3F9),
    pillSurface = Color(0xFFE8ECF4),
    subtleGlassBorder = Color(0xFFDCE2EE),
    dividerSubtle = Color(0xFFE5E9F2),
    zebraRowSurface = Color(0x090F172A),
    chartGridStrong = Color(0xFF94A3B8),
    chartGridSubtle = Color(0xFFCBD5E1),
    euroLeagueOrange = Color(0xFFEA580C),
    liveBadgeOrangeRed = Color(0xFFE03E00),
    neonMadeGreen = Color(0xFF059669),
    neonMissedRed = Color(0xFFE11D48),
    primaryText = Color(0xFF0F172A),
    secondaryText = Color(0xFF334155),
    mutedText = Color(0xFF64748B),
    onAccentText = Color(0xFFFFFFFF),
    bannerCenterSurface = Color(0xFFFFFFFF),
    bannerClubAlpha = 0.16f
)

val DarkCourtsidePalette = CourtsidePalette(
    mode = AppThemeMode.DARK,
    shellBackground = Color(0xFF0E0E11),
    sidebarSurface = Color(0xFF0B0B0E),
    centerPaneSurface = Color(0xFF111115),
    detailPaneSurface = Color(0xFF0E0E12),
    cardSurface = Color(0xFF17171C),
    cardSelectedBase = Color(0xFF1D1716),
    elevatedSurface = Color(0xFF151519),
    pillSurface = Color(0xFF1B1B21),
    subtleGlassBorder = Color(0xFF26262F),
    dividerSubtle = Color(0x14FFFFFF),
    zebraRowSurface = Color(0x06FFFFFF),
    chartGridStrong = Color(0x55FFFFFF),
    chartGridSubtle = Color(0x28FFFFFF),
    euroLeagueOrange = Color(0xFFFF6600),
    liveBadgeOrangeRed = Color(0xFFFF4500),
    neonMadeGreen = Color(0xFF00E676),
    neonMissedRed = Color(0xFFFF3B5C),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFFD1D5DB),
    mutedText = Color(0xFF8E8E93),
    onAccentText = Color(0xFF000000),
    bannerCenterSurface = Color(0xFF17171C),
    bannerClubAlpha = 0.46f
)

fun paletteForThemeMode(mode: AppThemeMode): CourtsidePalette =
    if (mode == AppThemeMode.DARK) DarkCourtsidePalette else LightCourtsidePalette

val LocalAppThemeMode = compositionLocalOf { AppThemeMode.LIGHT }
val LocalCourtsidePalette = compositionLocalOf { LightCourtsidePalette }

/**
 * Einheitliche "Courtside UI" Design-Tokens für Light Mode (Standard) & Dark Mode (Obsidian).
 */
object CourtsideDesignTokens {
    @Volatile
    var activeMode: AppThemeMode = AppThemeMode.DEFAULT

    val currentPalette: CourtsidePalette
        get() = paletteForThemeMode(activeMode)

    val isLightMode: Boolean
        get() = activeMode == AppThemeMode.LIGHT

    val ShellObsidian: Color get() = currentPalette.shellBackground
    val SidebarObsidian: Color get() = currentPalette.sidebarSurface
    val CenterPaneSurface: Color get() = currentPalette.centerPaneSurface
    val DetailPaneSurface: Color get() = currentPalette.detailPaneSurface
    val CardSurface: Color get() = currentPalette.cardSurface
    val CardSelectedBase: Color get() = currentPalette.cardSelectedBase
    val ElevatedSurface: Color get() = currentPalette.elevatedSurface
    val PillSurface: Color get() = currentPalette.pillSurface
    val SubtleGlassBorder: Color get() = currentPalette.subtleGlassBorder
    val DividerSubtle: Color get() = currentPalette.dividerSubtle
    val ZebraRowSurface: Color get() = currentPalette.zebraRowSurface
    val ChartGridStrong: Color get() = currentPalette.chartGridStrong
    val ChartGridSubtle: Color get() = currentPalette.chartGridSubtle
    val EuroLeagueOrange: Color get() = currentPalette.euroLeagueOrange
    val LiveBadgeOrangeRed: Color get() = currentPalette.liveBadgeOrangeRed
    val NeonMadeGreen: Color get() = currentPalette.neonMadeGreen
    val NeonMissedRed: Color get() = currentPalette.neonMissedRed
    val PrimaryText: Color get() = currentPalette.primaryText
    val SecondaryText: Color get() = currentPalette.secondaryText
    val MutedText: Color get() = currentPalette.mutedText
    val OnAccentText: Color get() = currentPalette.onAccentText
    val BannerCenterSurface: Color get() = currentPalette.bannerCenterSurface
    val BannerClubAlpha: Float get() = currentPalette.bannerClubAlpha

    val MinTouchTargetSize: Dp = 48.dp
    val CardCornerRadius: Dp = 18.dp
}

val CourtsideShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

val CourtsideTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.3.sp
    )
)

@Composable
fun EuroLeagueCourtsideTheme(
    activeTeam: EuroLeagueTeam,
    themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    content: @Composable () -> Unit
) {
    CourtsideDesignTokens.activeMode = themeMode
    val palette = paletteForThemeMode(themeMode)

    val animatedClubPrimary by animateColorAsState(
        targetValue = activeTeam.primaryColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "clubPrimaryColor"
    )
    val animatedClubSecondary by animateColorAsState(
        targetValue = activeTeam.secondaryColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "clubSecondaryColor"
    )

    val colorScheme = if (themeMode == AppThemeMode.LIGHT) {
        lightColorScheme(
            primary = palette.euroLeagueOrange,
            onPrimary = Color.White,
            secondary = animatedClubSecondary,
            tertiary = animatedClubPrimary,
            background = palette.shellBackground,
            surface = palette.cardSurface,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText
        )
    } else {
        darkColorScheme(
            primary = palette.euroLeagueOrange,
            onPrimary = Color.Black,
            secondary = animatedClubSecondary,
            tertiary = animatedClubPrimary,
            background = palette.shellBackground,
            surface = palette.cardSurface,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText
        )
    }

    CompositionLocalProvider(
        LocalAppThemeMode provides themeMode,
        LocalCourtsidePalette provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = CourtsideShapes,
            typography = CourtsideTypography,
            content = content
        )
    }
}

/**
 * Wiederverwendbare Broadcast-Karte im Courtside UI Design mit Radial-Glow oben links,
 * 48.dp Touch-Komfort und animiertem Desktop-Maus-Hover (adaptiv für Light & Dark Mode).
 */
@Composable
fun CourtsideGlassCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    glowColor: Color = CourtsideDesignTokens.EuroLeagueOrange,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalCourtsidePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> glowColor
            isHovered -> glowColor.copy(alpha = 0.65f)
            else -> palette.subtleGlassBorder
        },
        label = "cardBorder"
    )

    val selectedAlpha = if (palette.mode.isLight) 0.12f else 0.20f
    val hoverAlpha = if (palette.mode.isLight) 0.06f else 0.10f

    val bgBrush = if (isSelected || isHovered) {
        Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = if (isSelected) selectedAlpha else hoverAlpha),
                palette.cardSurface
            ),
            center = Offset(90f, 50f),
            radius = 420f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(palette.cardSurface, palette.cardSurface)
        )
    }

    val clickMod = if (onClick != null) {
        Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CourtsideDesignTokens.CardCornerRadius))
            .background(bgBrush)
            .border(
                width = if (isSelected || isHovered) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(CourtsideDesignTokens.CardCornerRadius)
            )
            .hoverable(interactionSource)
            .then(clickMod),
        content = content
    )
}

/**
 * Zweigeteiltes `[LIVE | Q4 01:42]` bzw. `[STATUS | SUBTITLE]` Badge mit orangenem Glow-Rahmen.
 */
@Composable
fun CourtsideLivePill(
    badgeText: String,
    detailText: String,
    badgeColor: Color = CourtsideDesignTokens.LiveBadgeOrangeRed
) {
    val palette = LocalCourtsidePalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(palette.elevatedSurface, RoundedCornerShape(8.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        Surface(
            color = badgeColor,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        Text(
            text = detailText,
            color = palette.primaryText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Dual-Tone-Verlaufs-Banner (adaptiv für Light & Dark Mode).
 */
@Composable
fun CourtsideDualToneBanner(
    leftColor: Color,
    rightColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalCourtsidePalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        leftColor.copy(alpha = palette.bannerClubAlpha),
                        palette.bannerCenterSurface,
                        rightColor.copy(alpha = palette.bannerClubAlpha)
                    )
                )
            )
            .border(1.dp, palette.subtleGlassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

@Composable
fun CourtsideClubCrest(
    team: EuroLeagueTeam,
    size: Dp = 42.dp
) {
    val context = LocalContext.current
    val localDrawableId = remember(team.id, team.code, team.localLogoResName) {
        val candidates = listOf(
            team.localLogoResName,
            "club_logo_${team.id.lowercase()}",
            "club_logo_${team.code.lowercase()}"
        ).filter { it.isNotBlank() }.distinct()
        candidates.firstNotNullOfOrNull { resName ->
            val id = context.resources.getIdentifier(resName, "drawable", context.packageName)
            id.takeIf { it != 0 }
        } ?: 0
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            team.logoUrl.isNotBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(team.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${team.name} Official Logo",
                    placeholder = if (localDrawableId != 0) painterResource(id = localDrawableId) else null,
                    error = if (localDrawableId != 0) painterResource(id = localDrawableId) else null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
            }
            localDrawableId != 0 -> {
                Image(
                    painter = painterResource(id = localDrawableId),
                    contentDescription = "${team.name} Official Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
            }
            else -> {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val r = (w.coerceAtMost(h) / 2f) * 0.90f

                    drawCircle(color = team.primaryColor, radius = r, center = Offset(cx, cy))
                    drawCircle(color = team.secondaryColor, radius = r * 0.58f, center = Offset(cx, cy))
                    drawCircle(color = team.primaryColor, radius = r * 0.34f, center = Offset(cx, cy))
                    drawCircle(
                        color = team.accentColor,
                        radius = r * 0.92f,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.10f)
                    )
                }
            }
        }
    }
}

@Composable
fun CourtsideShortcutPill(shortcut: String) {
    val palette = LocalCourtsidePalette.current
    Box(
        modifier = Modifier
            .background(palette.pillSurface, RoundedCornerShape(7.dp))
            .border(1.dp, palette.subtleGlassBorder, RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortcut,
            color = palette.mutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun Modifier.courtsideTouchAndDesktopInteractive(
    isSelected: Boolean = false,
    accentColor: Color = CourtsideDesignTokens.EuroLeagueOrange,
    cornerRadius: Dp = CourtsideDesignTokens.CardCornerRadius
): Modifier = composed {
    val palette = LocalCourtsidePalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected || isHovered || isPressed) 2.dp else 1.dp,
        label = "borderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> palette.euroLeagueOrange
            isPressed -> accentColor
            isHovered -> accentColor.copy(alpha = 0.85f)
            else -> palette.subtleGlassBorder
        },
        label = "borderColor"
    )

    this
        .defaultMinSize(
            minWidth = CourtsideDesignTokens.MinTouchTargetSize,
            minHeight = CourtsideDesignTokens.MinTouchTargetSize
        )
        .clip(RoundedCornerShape(cornerRadius))
        .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
        .hoverable(interactionSource)
        .pointerHoverIcon(PointerIcon.Hand)
}
