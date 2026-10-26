package com.lucid.alarm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.BackgroundPalette
import com.lucid.alarm.data.prefs.RingingClockFont
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.LocalAppFont
import com.lucid.alarm.ui.LocalClockFont
import kotlinx.coroutines.flow.flowOf

@Composable
fun OneUiAppBackground(
    dark: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (dark) {

            drawRect(color = Color(0xFF020205))

            drawRect(
                brush = Brush.radialGradient(
                    0.00f to lerp(Color(0xFF040408), accentColor, 0.25f),
                    0.28f to lerp(Color(0xFF040408), accentColor, 0.18f),
                    0.55f to lerp(Color(0xFF030306), accentColor, 0.10f),
                    0.80f to lerp(Color(0xFF020204), accentColor, 0.04f),
                    1.00f to Color(0xFF010103),
                    center = Offset(size.width * 0.50f, size.height * 0.36f),
                    radius = size.maxDimension * 0.78f
                )
            )
        } else {

            drawRect(color = Color(0xFFF8F9FD))

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFCE8EC), Color.Transparent),
                    center = Offset(size.width * 0.35f, size.height * 0.10f),
                    radius = size.maxDimension * 0.55f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE2F5F4), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.45f),
                    radius = size.maxDimension * 0.50f
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEAECFA), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.50f),
                    radius = size.maxDimension * 0.55f
                )
            )

            drawRect(
                brush = Brush.verticalGradient(
                    0.7f to Color.Transparent,
                    1.0f to Color(0xFFF7F8FC)
                )
            )
        }
    }
}

@Composable
fun LucidAlarmTheme(
    accentColor: AccentColor = AccentColor.DEFAULT,
    backgroundPalette: BackgroundPalette = BackgroundPalette.OBSIDIAN,
    appFontStyle: AppFontStyle = AppFontStyle.SYSTEM_DEFAULT,
    dark: Boolean = true,
    settingsDataStore: SettingsDataStore? = null,
    content: @Composable () -> Unit
) {
    val customAppFontPath by (settingsDataStore?.appCustomFontPathFlow ?: flowOf(""))
        .collectAsState(initial = "")
    val clockFontStyle by (settingsDataStore?.alarmClockFontStyleFlow ?: flowOf(RingingClockFont.SYSTEM_DEFAULT))
        .collectAsState(initial = RingingClockFont.SYSTEM_DEFAULT)
    val clockCustomFontPath by (settingsDataStore?.alarmClockCustomFontPathFlow ?: flowOf(""))
        .collectAsState(initial = "")

    val customAccentArgb by (settingsDataStore?.accentCustomColorFlow ?: flowOf(AccentColor.DEFAULT.argb))
        .collectAsState(initial = AccentColor.DEFAULT.argb)
    val customBackgroundArgb by (settingsDataStore?.backgroundCustomColorFlow ?: flowOf(BackgroundPalette.OBSIDIAN.argb))
        .collectAsState(initial = BackgroundPalette.OBSIDIAN.argb)

    val colorScheme = appColorSchemeFor(
        accent = accentColor,
        background = backgroundPalette,
        dark = dark,
        customAccentArgb = customAccentArgb,
        customBackgroundArgb = customBackgroundArgb
    )

    val appFontFamily = when (appFontStyle) {
        AppFontStyle.SYSTEM_DEFAULT -> FontFamily.SansSerif
        AppFontStyle.SERIF -> FontFamily.Serif
        AppFontStyle.CURSIVE -> FontFamily.Cursive
        AppFontStyle.CUSTOM_FILE -> FontManager.fontFamilyFromPath(customAppFontPath)
    }

    val clockFontFamily = when (clockFontStyle) {
        RingingClockFont.SYSTEM_DEFAULT -> FontFamily.SansSerif
        RingingClockFont.SERIF -> FontFamily.Serif
        RingingClockFont.CURSIVE -> FontFamily.Cursive
        RingingClockFont.CUSTOM_FILE -> FontManager.fontFamilyFromPath(clockCustomFontPath)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    CompositionLocalProvider(
        LocalAppFont provides appFontFamily,
        LocalClockFont provides clockFontFamily
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getTypographyForAppFont(appFontFamily)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OneUiAppBackground(dark = dark)
                content()
            }
        }
    }
}
