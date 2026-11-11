package com.lucid.alarm.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

val FloatingBarEnterTransition: EnterTransition = slideInVertically(
    initialOffsetY = { it + 48 },
    animationSpec = tween(durationMillis = 180)
)

val FloatingBarExitTransition: ExitTransition = slideOutVertically(
    targetOffsetY = { it + 48 },
    animationSpec = tween(durationMillis = 150)
)

val FloatingButtonEnterTransition: EnterTransition = scaleIn(
    initialScale = 0f,
    animationSpec = tween(durationMillis = 180)
)

val FloatingButtonExitTransition: ExitTransition = scaleOut(
    targetScale = 0f,
    animationSpec = tween(durationMillis = 150)
)

val FloatingTooltipEnterTransition: EnterTransition = scaleIn(
    initialScale = 0.85f,
    animationSpec = tween(durationMillis = 120)
)

val FloatingTooltipExitTransition: ExitTransition = scaleOut(
    targetScale = 0.85f,
    animationSpec = tween(durationMillis = 100)
)
