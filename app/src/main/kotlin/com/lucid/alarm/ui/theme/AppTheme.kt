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

/** Converts the persisted packed [AccentColor.argb] into a Compose [Color]. */
fun AccentColor.toColor(): Color = Color(argb)

/** Converts the persisted packed [BackgroundPalette.argb] into a Compose [Color]. */
fun BackgroundPalette.toColor(): Color = Color(argb)

/** Picks a high-contrast foreground (near-black or white) for [color]. */
fun onColorFor(color: Color): Color =
    if (color.luminance() > 0.45f) Color(0xFF14171A) else Color.White

/** Near-black primary text used across light-mode cards for maximum legibility on white. */
private val LightOnSurface = Color(0xFF111318)

/** Muted secondary text for light-mode cards. */
private val LightOnSurfaceVariant = Color(0xFF4B5563)

/**
 * Root background brush for [palette]. [BackgroundPalette.AURORA] renders its deep indigo → cyan
 * gradient; every other palette is painted as a solid Color. In light mode the background is pure
 * white (or the user's exact custom Color) — never washed toward white.
 *
 * [customArgb] supplies the packed Color for [BackgroundPalette.CUSTOM]; when it is null the
 * palette's own (fallback) [BackgroundPalette.argb] is used.
 */
fun backgroundBrushFor(
    palette: BackgroundPalette,
    dark: Boolean = true,
    customArgb: Long? = null
): Brush {
    // Light mode: pure white by default. If the user explicitly chose a custom light-mode
    // background Color, render that EXACT raw Color (alpha = 1.0f) — never washed toward white.
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

/**
 * Builds the whole-app [ColorScheme] from the selected [accent] and [background] palette.
 *
 * Dark mode derives every surface / container tone from the (deep) background Color so the accent
 * always reads clearly. Light mode auto-lightens the palette and switches to dark foregrounds.
 *
 * [customAccentArgb] / [customBackgroundArgb] supply the stored Colors for
 * [AccentColor.CUSTOM] / [BackgroundPalette.CUSTOM]; when null the enum's fallback [argb] is used.
 */
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
        // Light mode: the user's accent is used VERBATIM (byte-for-byte, no darkening/mixing) and
        // every surface is pure white with a transparent surfaceTint so Material 3 never overlays a
        // tonal grey wash on top of cards.
        lightScheme(accentColor)
    }
}

private fun darkScheme(accentColor: Color, bg: Color): ColorScheme {
    val onBg = onColorFor(bg)
    // Near-black palettes (AMOLED / Onyx) would otherwise render surface containers identical to
    // the background, hiding every card, row and box — lift the container tones so they stay visible.
    val lift = if (bg.luminance() < 0.02f) 0.10f else 0f
    // Containers tint toward the foreground so they always contrast with the background.
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
    // The user's chosen accent is used VERBATIM — never darkened, desaturated or alpha-reduced —
    // so its hex/RGB brightness and saturation are byte-for-byte identical in Light and Dark mode.
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
        // Every surface resolves to pure white: cards, lists, sheets and dialogs all share one bright
        // fill, so no surface is left dimmed or tinted grey in light mode.
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
