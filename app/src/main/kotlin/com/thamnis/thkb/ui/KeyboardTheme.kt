package com.thamnis.thkb.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thamnis.thkb.settings.KbSettings

data class KbColorPalette(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val bg: Color,
    val keyBg: Color,
    val keyShadow: Color,
    val keyPressed: Color,
    val actionKeyBg: Color,
    val pillKeyBg: Color,
    val returnBg: Color,
    val returnFg: Color,
    val hintText: Color,
    val toolbarText: Color,
    val keyText: Color,
    val isLight: Boolean = false,
)

val DarkGraphitePalette = KbColorPalette(
    id = KbSettings.THEME_DARK_GRAPHITE,
    displayName = "Dark Graphite",
    subtitle = "Balanced studio dark keys",
    bg = Color(0xFF1B1C1E),
    keyBg = Color(0xFF3A3C41),
    keyShadow = Color(0xFF0E0F11),
    keyPressed = Color(0xFF5A5D64),
    actionKeyBg = Color(0xFF2B2D31),
    pillKeyBg = Color(0xFF43474E),
    returnBg = Color(0xFFA8C7FA),
    returnFg = Color(0xFF062E6F),
    hintText = Color(0xFF9AA0A6),
    toolbarText = Color(0xFFE3E3E3),
    keyText = Color.White,
)

val MidnightAmoledPalette = KbColorPalette(
    id = KbSettings.THEME_MIDNIGHT_AMOLED,
    displayName = "Midnight AMOLED",
    subtitle = "True black background for OLED screens",
    bg = Color(0xFF000000),
    keyBg = Color(0xFF1B1D22),
    keyShadow = Color(0xFF000000),
    keyPressed = Color(0xFF343842),
    actionKeyBg = Color(0xFF111216),
    pillKeyBg = Color(0xFF262A32),
    returnBg = Color(0xFF8AB4F8),
    returnFg = Color(0xFF041E49),
    hintText = Color(0xFF8E9199),
    toolbarText = Color(0xFFF1F3F4),
    keyText = Color.White,
)

val WarmPaperPalette = KbColorPalette(
    id = KbSettings.THEME_WARM_PAPER,
    displayName = "Warm Paper",
    subtitle = "Clean daylight contrast",
    bg = Color(0xFFECEEF3),
    keyBg = Color(0xFFFFFFFF),
    keyShadow = Color(0xFFC5C9D3),
    keyPressed = Color(0xFFD9DEE8),
    actionKeyBg = Color(0xFFDCE0E9),
    pillKeyBg = Color(0xFFD2D7E2),
    returnBg = Color(0xFF1B5EF0),
    returnFg = Color(0xFFFFFFFF),
    hintText = Color(0xFF666D7A),
    toolbarText = Color(0xFF1C2029),
    keyText = Color(0xFF181C24),
    isLight = true,
)

val OceanSlatePalette = KbColorPalette(
    id = KbSettings.THEME_OCEAN_SLATE,
    displayName = "Ocean Slate",
    subtitle = "Deep Atlantic blues & crisp cyan",
    bg = Color(0xFF0F1724),
    keyBg = Color(0xFF1E2B3E),
    keyShadow = Color(0xFF070B12),
    keyPressed = Color(0xFF2F4360),
    actionKeyBg = Color(0xFF162030),
    pillKeyBg = Color(0xFF263750),
    returnBg = Color(0xFF5CE1E6),
    returnFg = Color(0xFF062730),
    hintText = Color(0xFF89A0BD),
    toolbarText = Color(0xFFE6F0FA),
    keyText = Color(0xFFF3F8FF),
)

val ForestSagePalette = KbColorPalette(
    id = KbSettings.THEME_FOREST_SAGE,
    displayName = "Forest Sage",
    subtitle = "Earthy pine dark with mint glow",
    bg = Color(0xFF121A16),
    keyBg = Color(0xFF223029),
    keyShadow = Color(0xFF090D0B),
    keyPressed = Color(0xFF33493E),
    actionKeyBg = Color(0xFF1A251F),
    pillKeyBg = Color(0xFF2A3B32),
    returnBg = Color(0xFF8BE2B0),
    returnFg = Color(0xFF062E1B),
    hintText = Color(0xFF93AE9E),
    toolbarText = Color(0xFFE8F5ED),
    keyText = Color(0xFFF4FBF7),
)

val StaticKeyboardPalettes: List<KbColorPalette> = listOf(
    DarkGraphitePalette,
    MidnightAmoledPalette,
    WarmPaperPalette,
    OceanSlatePalette,
    ForestSagePalette,
)

fun resolveKbPalette(themeId: String, context: Context): KbColorPalette {
    if (themeId == KbSettings.THEME_MATERIAL_YOU) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
                Configuration.UI_MODE_NIGHT_NO
            val scheme = if (isNight) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            return KbColorPalette(
                id = KbSettings.THEME_MATERIAL_YOU,
                displayName = "Material You",
                subtitle = "Matches your Android wallpaper colors",
                bg = scheme.surfaceContainerLowest,
                keyBg = if (isNight) scheme.surfaceContainerHigh else scheme.surface,
                keyShadow = if (isNight) Color(0xFF0B0C0E) else scheme.outlineVariant,
                keyPressed = scheme.primaryContainer,
                actionKeyBg = scheme.surfaceContainer,
                pillKeyBg = scheme.surfaceContainerHighest,
                returnBg = scheme.primary,
                returnFg = scheme.onPrimary,
                hintText = scheme.onSurfaceVariant,
                toolbarText = scheme.onSurface,
                keyText = scheme.onSurface,
                isLight = !isNight,
            )
        }
        return DarkGraphitePalette.copy(
            id = KbSettings.THEME_MATERIAL_YOU,
            displayName = "Material You",
            subtitle = "System dynamic colors (Android 12+)",
        )
    }
    return StaticKeyboardPalettes.firstOrNull { it.id == themeId } ?: DarkGraphitePalette
}

fun allAvailablePalettes(context: Context): List<KbColorPalette> =
    listOf(
        resolveKbPalette(KbSettings.THEME_MATERIAL_YOU, context),
        DarkGraphitePalette,
        MidnightAmoledPalette,
        WarmPaperPalette,
        OceanSlatePalette,
        ForestSagePalette,
    )

val LocalKbPalette = staticCompositionLocalOf { DarkGraphitePalette }

internal val BgColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.bg

internal val KeyBgColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.keyBg

internal val KeyShadowColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.keyShadow

internal val KeyPressedColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.keyPressed

internal val ActionKeyBgColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.actionKeyBg

internal val PillKeyBgColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.pillKeyBg

internal val ReturnBgColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.returnBg

internal val ReturnFgColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.returnFg

internal val HintTextColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.hintText

internal val ToolbarTextColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.toolbarText

internal val KeyTextColor: Color
    @Composable @ReadOnlyComposable get() = LocalKbPalette.current.keyText

internal fun Color.withMutedSurface(alpha: Float, onBg: Color): Color =
    this.copy(alpha = alpha).compositeOver(onBg)

@Composable
internal fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) ReturnBgColor else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "toolbarBtnBg",
    )
    val animatedTint by animateColorAsState(
        targetValue = if (active) ReturnFgColor else ToolbarTextColor,
        animationSpec = tween(durationMillis = 180),
        label = "toolbarBtnTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (active) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toolbarBtnScale",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animatedTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
    }
}

@Composable
internal fun CategoryChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val animatedBg by animateColorAsState(
        targetValue = if (active) ReturnBgColor else ActionKeyBgColor,
        animationSpec = tween(durationMillis = 180),
        label = "chipBg",
    )
    val animatedFg by animateColorAsState(
        targetValue = if (active) ReturnFgColor else ToolbarTextColor,
        animationSpec = tween(durationMillis = 180),
        label = "chipFg",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(animatedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = animatedFg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
