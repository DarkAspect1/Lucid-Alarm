package com.lucid.alarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * Shared frosted card surface with screen-aligned backdrop blur and zero borders:
 * Layer 1 (Screen-Aligned Blurred Wallpaper Slice): Draws the active screen background
 *   translated by (-cardOffset.x, -cardOffset.y) with 30.dp blur so it aligns 1:1 with wallpaper.
 * Layer 2 (Frosted Glass Luminance Overlay — NO BORDER):
 *   - Dark Mode: Color(0xFF22232E).copy(alpha = 0.64f) + subtle vertical sheen.
 *   - Light Mode: Color(0xFFFFFFFF).copy(alpha = 0.72f).
 * Layer 3 (Unblurred Foreground Content): Renders crisp text and icons.
 */
@Composable
fun FrostedCardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tintColor: Color? = null,
    connectedIndex: Int = 0,
    totalConnected: Int = 1,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val accentColor = MaterialTheme.colorScheme.primary
    var cardOffset by remember { mutableStateOf(Offset.Zero) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenMaxDim = max(screenWidthPx, screenHeightPx)

    Box(
        modifier = modifier
            .onGloballyPositioned { cardOffset = it.positionInRoot() }
            .clip(shape)
    ) {
        // Layer 1: Screen-Aligned Blurred Wallpaper Slice
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 30.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(-cardOffset.x, -cardOffset.y)
                if (!isLight) {
                    drawRect(
                        color = Color(0xFF020205),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            0.00f to lerp(Color(0xFF040408), accentColor, 0.25f),
                            0.28f to lerp(Color(0xFF040408), accentColor, 0.18f),
                            0.55f to lerp(Color(0xFF030306), accentColor, 0.10f),
                            0.80f to lerp(Color(0xFF020204), accentColor, 0.04f),
                            1.00f to Color(0xFF010103),
                            center = Offset(screenWidthPx * 0.50f, screenHeightPx * 0.36f),
                            radius = screenMaxDim * 0.78f
                        ),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                } else {
                    drawRect(
                        color = Color(0xFFF8F9FD),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFCE8EC), Color.Transparent),
                            center = Offset(screenWidthPx * 0.35f, screenHeightPx * 0.10f),
                            radius = screenMaxDim * 0.55f
                        ),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE2F5F4), Color.Transparent),
                            center = Offset(screenWidthPx * 0.15f, screenHeightPx * 0.45f),
                            radius = screenMaxDim * 0.50f
                        ),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFEAECFA), Color.Transparent),
                            center = Offset(screenWidthPx * 0.80f, screenHeightPx * 0.50f),
                            radius = screenMaxDim * 0.55f
                        ),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.7f to Color.Transparent,
                            1.0f to Color(0xFFF7F8FC)
                        ),
                        size = Size(screenWidthPx, screenHeightPx)
                    )
                }
                canvas.restore()
            }
        }

        // Layer 2: Frosted Glass Luminance Overlay — NO BORDER
        if (tintColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tintColor)
            )
        } else {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (!isLight) {
                    drawRect(color = Color(0xFF22232E).copy(alpha = 0.64f))
                    val topSheen = if (totalConnected > 1) {
                        0.065f - (0.045f * (connectedIndex.toFloat() / totalConnected))
                    } else {
                        0.07f
                    }
                    val bottomSheen = if (totalConnected > 1) {
                        0.065f - (0.045f * ((connectedIndex + 1).toFloat() / totalConnected))
                    } else {
                        0.02f
                    }
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = topSheen), Color.White.copy(alpha = bottomSheen))
                        )
                    )
                } else {
                    drawRect(color = Color(0xFFFFFFFF).copy(alpha = 0.72f))
                }
            }
        }

        // Layer 3: Unblurred Foreground Content
        content()
    }
}

/**
 * FrostedBlurBox delegates directly to [FrostedCardSurface] with zero border.
 */
@Composable
fun FrostedBlurBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tintColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 0.dp,
    connectedIndex: Int = 0,
    totalConnected: Int = 1,
    content: @Composable BoxScope.() -> Unit
) {
    FrostedCardSurface(
        modifier = modifier,
        shape = shape,
        tintColor = tintColor,
        connectedIndex = connectedIndex,
        totalConnected = totalConnected,
        content = content
    )
}
