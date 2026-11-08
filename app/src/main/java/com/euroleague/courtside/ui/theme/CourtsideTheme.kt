package com.euroleague.courtside.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.euroleague.courtside.data.repository.EuroLeagueTeam

/**
 * Einheitliche "Courtside UI" Design-Sprache für die gesamte App:
 * - Tiefes Obsidian-Shell-Layout (#0B0B0E Sidebar, #111115 Center-Pane, #0E0E12 Detail-Pane)
 * - Dunkle Broadcast-Karten (#17171C) mit feinen Glas-Rändern und warmem #FF6600 Radial-Glow bei Auswahl/Hover
 * - Dual-Tone-Verlaufs-Header (Vereinsfarbe Links -> #17171C -> Vereinsfarbe Rechts)
 * - Neon-Akzente (#00E676 Made/Playoffs, #FF3B5C Missed/Crunch, #FF6600 PIR & Primary)
 */
object CourtsideDesignTokens {
    val ShellObsidian = Color(0xFF0E0E11)
    val SidebarObsidian = Color(0xFF0B0B0E)
    val CenterPaneSurface = Color(0xFF111115)
    val DetailPaneSurface = Color(0xFF0E0E12)
    val CardSurface = Color(0xFF17171C)
    val CardSelectedBase = Color(0xFF1D1716)
    val PillSurface = Color(0xFF1B1B21)
    val SubtleGlassBorder = Color(0xFF26262F)
    val EuroLeagueOrange = Color(0xFFFF6600)
    val LiveBadgeOrangeRed = Color(0xFFFF4500)
    val NeonMadeGreen = Color(0xFF00E676)
    val NeonMissedRed = Color(0xFFFF3B5C)
    val MutedText = Color(0xFF8E8E93)
    val PrimaryText = Color(0xFFF2F2F7)

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
    content: @Composable () -> Unit
) {
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

    val colorScheme = darkColorScheme(
        primary = CourtsideDesignTokens.EuroLeagueOrange,
        onPrimary = Color.Black,
        secondary = animatedClubSecondary,
        tertiary = animatedClubPrimary,
        background = CourtsideDesignTokens.ShellObsidian,
        surface = CourtsideDesignTokens.CardSurface,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = CourtsideShapes,
        typography = CourtsideTypography,
        content = content
    )
}

/**
 * Wiederverwendbare Broadcast-Karte im Courtside UI Design mit Radial-Glow oben links,
 * 48.dp Touch-Komfort und animiertem Desktop-Maus-Hover.
 */
@Composable
fun CourtsideGlassCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    glowColor: Color = CourtsideDesignTokens.EuroLeagueOrange,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> glowColor
            isHovered -> glowColor.copy(alpha = 0.65f)
            else -> CourtsideDesignTokens.SubtleGlassBorder
        },
        label = "cardBorder"
    )

    val bgBrush = if (isSelected || isHovered) {
        Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = if (isSelected) 0.20f else 0.10f),
                CourtsideDesignTokens.CardSurface
            ),
            center = Offset(90f, 50f),
            radius = 420f
        )
    } else {
        Brush.linearGradient(
            colors = listOf(CourtsideDesignTokens.CardSurface, CourtsideDesignTokens.CardSurface)
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFF121216), RoundedCornerShape(8.dp))
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
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        Text(
            text = detailText,
            color = Color(0xFFE5E5EA),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Dual-Tone-Verlaufs-Banner (Links Farbe A -> Mitte #17171C -> Rechts Farbe B) wie im Scoreboard-Design.
 */
@Composable
fun CourtsideDualToneBanner(
    leftColor: Color,
    rightColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        leftColor.copy(alpha = 0.46f),
                        Color(0xFF17171C),
                        rightColor.copy(alpha = 0.46f)
                    )
                )
            )
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}

/**
 * Rendert das echte, offizielle Vereins-Logo (`images.crest` PNG der EuroLeague) jedes Clubs:
 * 1. Löst sofort das lokal gebündelte offizielle Wappen (`res/drawable-nodpi/club_logo_<code>.png`) auf,
 *    sodass alle 20 EuroLeague-Logos im selben Frame (und 100 % offline) gestochen scharf erscheinen.
 * 2. Lädt parallel via Coil `AsyncImage` die aktuelle Live-CDN-URL (`team.logoUrl` von `media-cdn.cortextech.io` /
 *    `media-cdn.incrowdsports.com`), falls ein Verein unter der Saison sein Wappen aktualisiert.
 * 3. Fällt nur bei unbekannten Test-IDs auf das Vektor-Wappen in den Vereinsfarben zurück.
 */
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

/**
 * Dunkles `Ctrl+1`..`Ctrl+5` Shortcut-Pill-Badge für die Desktop-Sidebar.
 */
@Composable
fun CourtsideShortcutPill(shortcut: String) {
    Box(
        modifier = Modifier
            .background(CourtsideDesignTokens.PillSurface, RoundedCornerShape(7.dp))
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortcut,
            color = CourtsideDesignTokens.MutedText,
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
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected || isHovered || isPressed) 2.dp else 1.dp,
        label = "borderWidth"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> CourtsideDesignTokens.EuroLeagueOrange
            isPressed -> accentColor
            isHovered -> accentColor.copy(alpha = 0.85f)
            else -> CourtsideDesignTokens.SubtleGlassBorder
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
