package com.lucid.alarm.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.BackgroundPalette

fun AccentColor.toColor(): Color = Color(argb)

fun BackgroundPalette.toColor(): Color = Color(argb)

fun onColorFor(color: Color): Color =
    if (color.luminance() > 0.40f) Color(0xFF14171A) else Color.White

private val LightOnSurface = Color(0xFF111318)

private val LightOnSurfaceVariant = Color(0xFF4B5563)

fun backgroundBrushFor(
    palette: BackgroundPalette,
    dark: Boolean = true,
    customArgb: Long? = null
): Brush {

    if (!dark) {
        return if (palette == BackgroundPalette.CUSTOM && customArgb != null) {
            SolidColor(Color(customArgb))
        } else {
            SolidColor(Color(0xFFFFFFFF))
        }
    }
    if (palette == BackgroundPalette.AURORA) {
        return Brush.verticalGradient(
            listOf(
                Color(0xFF0B1E3A),
                Color(0xFF0E2C44),
                Color(0xFF0A2733),
                Color(0xFF07242B)
            )
        )
    }
    val custom = palette == BackgroundPalette.CUSTOM && customArgb != null
    val solid =
        if (palette == BackgroundPalette.CUSTOM && customArgb != null) Color(customArgb)
        else palette.toColor()
    if (palette.isGradient && !custom) {
        return Brush.verticalGradient(
            listOf(lerp(solid, Color.Black, 0.20f), solid, lerp(solid, Color.White, 0.12f))
        )
    }
    return SolidColor(solid)
}

fun appColorSchemeFor(
    accent: AccentColor,
    background: BackgroundPalette,
    dark: Boolean = true,
    customAccentArgb: Long? = null,
    customBackgroundArgb: Long? = null
): ColorScheme {
    val accentColor =
        if (accent == AccentColor.CUSTOM && customAccentArgb != null) {
            Color(customAccentArgb)
        } else {
            accent.toColor()
        }
    val bgColor =
        if (background == BackgroundPalette.CUSTOM && customBackgroundArgb != null) {
            Color(customBackgroundArgb)
        } else {
            background.toColor()
        }
    return if (dark) {
        darkScheme(accentColor, bgColor)
    } else {

        lightScheme(accentColor)
    }
}

private fun darkScheme(accentColor: Color, bg: Color): ColorScheme {
    val onBg = onColorFor(bg)

    val lift = if (bg.luminance() < 0.02f) 0.10f else 0f

    val containerTint = onBg

    val primaryContainer = lerp(bg, accentColor, 0.28f)
    val secondaryContainer = lerp(bg, accentColor, 0.16f)
    val tertiaryContainer = lerp(bg, accentColor, 0.34f)
    val secondary = lerp(Color.White, accentColor, 0.55f)
    val tertiary = lerp(Color.White, accentColor, 0.35f)

    return darkColorScheme(
        primary = accentColor,
        onPrimary = onColorFor(accentColor),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onColorFor(primaryContainer),
        inversePrimary = lerp(Color.White, accentColor, 0.6f),
        secondary = secondary,
        onSecondary = onColorFor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onColorFor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = onColorFor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onColorFor(tertiaryContainer),
        background = bg,
        onBackground = onBg,
        surface = bg,
        onSurface = onBg,
        surfaceVariant = lerp(bg, containerTint, 0.07f + lift),
        onSurfaceVariant = lerp(onBg, bg, 0.32f),
        surfaceContainerLowest = lerp(bg, Color.Black, 0.18f),
        surfaceContainerLow = lerp(bg, containerTint, 0.04f + lift),
        surfaceContainer = lerp(bg, containerTint, 0.07f + lift),
        surfaceContainerHigh = lerp(bg, containerTint, 0.10f + lift),
        surfaceContainerHighest = lerp(bg, containerTint, 0.14f + lift),
        outline = lerp(bg, onBg, 0.35f),
        outlineVariant = lerp(bg, onBg, 0.18f + (if (lift > 0f) 0.12f else 0f)),
        scrim = Color.Black,
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC)
    )
}

private fun lightScheme(accentColor: Color): ColorScheme {

    val onAccent = if (accentColor.luminance() > 0.6f) Color(0xFF111318) else Color.White
    return androidx.compose.material3.lightColorScheme(
        primary = accentColor,
        onPrimary = onAccent,
        primaryContainer = accentColor,
        onPrimaryContainer = onAccent,
        inversePrimary = accentColor,
        secondary = accentColor,
        onSecondary = onAccent,
        secondaryContainer = accentColor,
        onSecondaryContainer = onAccent,
        tertiary = accentColor,
        onTertiary = onAccent,
        tertiaryContainer = accentColor,
        onTertiaryContainer = onAccent,
        background = Color(0xFFFFFFFF),
        onBackground = LightOnSurface,

        surface = Color(0xFFFFFFFF),
        onSurface = LightOnSurface,
        surfaceVariant = Color(0xFFFFFFFF),
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFFFFFFF),
        surfaceContainerHighest = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFFFFFFF),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFF111318),
        inverseOnSurface = Color(0xFFF9FAFB),
        outline = Color(0xFFE5E7EB),
        outlineVariant = Color(0xFFF3F4F6),
        scrim = Color.Black,
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF8C1D18)
    )
}
