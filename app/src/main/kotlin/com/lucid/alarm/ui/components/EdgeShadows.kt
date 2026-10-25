package com.lucid.alarm.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Erases the scrolling content itself at the bottom edge (and optionally top edge) instead of
 * painting a coloured band over it. Because the gradient is applied through [BlendMode.DstIn],
 * the app background stays untouched while cards dissolve cleanly into it.
 *
 * By default, [topFade] is false so that screen headers sitting at the top are 100% immune to
 * status bar shadows and render at full 100% brightness and contrast.
 */
fun Modifier.edgeFadeMask(
    topFade: Boolean = false,
    bottomFade: Boolean = true
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        if (size.height <= 0f) return@drawWithContent
        val stops = mutableListOf<Pair<Float, Color>>()
        if (topFade) {
            stops.add(0.0f to Color.Transparent)
            stops.add(0.04f to Color.Transparent)
            stops.add(0.11f to Color.Black)
        } else {
            stops.add(0.0f to Color.Black)
        }
        if (bottomFade) {
            stops.add(0.88f to Color.Black)
            stops.add(1.0f to Color.Transparent)
        } else {
            stops.add(1.0f to Color.Black)
        }
        drawRect(
            brush = Brush.verticalGradient(*stops.toTypedArray()),
            blendMode = BlendMode.DstIn
        )
    }
