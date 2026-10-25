package com.lucid.alarm.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * User-picked container Colors for the app's card surfaces. A `null` entry means "use the
 * Material default" so each card keeps its existing tonal surface when no override is set.
 */
data class BoxColors(
    val settingsCard: Color? = null,
    val alarmCard: Color? = null,
    val worldClockCard: Color? = null
)

val LocalBoxColors = staticCompositionLocalOf { BoxColors() }
