package com.lucid.alarm.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

val LocalAppFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

val LocalClockFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
