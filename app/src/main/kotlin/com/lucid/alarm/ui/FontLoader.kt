package com.lucid.alarm.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/**
 * Reactive composition locals for the active font families.
 *
 * [com.lucid.alarm.ui.theme.LucidAlarmTheme] provides these from the persisted settings flows, so
 * any composable reading `LocalAppFont.current` / `LocalClockFont.current` recomposes the instant a
 * new `.ttf` / `.otf` is imported — no app restart required.
 */
val LocalAppFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

val LocalClockFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
