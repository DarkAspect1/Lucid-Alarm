package com.lucid.alarm.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

private val BottomFadeBrush = Brush.verticalGradient(
    0.0f to Color.Black,
    0.88f to Color.Black,
    1.0f to Color.Transparent
)

private val TopFadeBrush = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.04f to Color.Transparent,
    0.11f to Color.Black,
    1.0f to Color.Black
)

private val BothFadeBrush = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.04f to Color.Transparent,
    0.11f to Color.Black,
    0.88f to Color.Black,
    1.0f to Color.Transparent
)

fun Modifier.edgeFadeMask(
    topFade: Boolean = false,
    bottomFade: Boolean = true
): Modifier {
    if (!topFade && !bottomFade) return this

    val brush = when {
        topFade && bottomFade -> BothFadeBrush
        topFade -> TopFadeBrush
        else -> BottomFadeBrush
    }

    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            if (size.height > 0f) {
                drawRect(
                    brush = brush,
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
