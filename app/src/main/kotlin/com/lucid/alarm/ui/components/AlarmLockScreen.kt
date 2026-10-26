package com.lucid.alarm.ui.components

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.lucid.alarm.data.prefs.ClockDateFont
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.data.prefs.RingingBackgroundStyle
import com.lucid.alarm.data.prefs.RingingClockFont
import com.lucid.alarm.data.prefs.RingingClockStyle
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.VideoBackgroundPlayer
import com.lucid.alarm.ui.formatClockDigits
import com.lucid.alarm.ui.formatHour
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object AlarmLockPalette {
    val Content = Color.White
    val BorderStroke = Color.White.copy(alpha = 0.30f)
    val DismissButton = Color(0xFF15161C)
    val Accent = Color(0xFF8B7CF6)
    val NeonGlow = Color(0xFF22D3EE)

    val DarkMidnightGradient = listOf(
        Color(0xFF12081F),
        Color(0xFF1B1036),
        Color(0xFF090A1A)
    )

    val WarmSunriseGradient = listOf(
        Color(0xFF3B1254),
        Color(0xFFB8336A),
        Color(0xFFF7882F)
    )
}

fun ringingClockFontFamily(font: RingingClockFont): FontFamily = when (font) {
    RingingClockFont.SYSTEM_DEFAULT -> FontFamily.SansSerif
    RingingClockFont.SERIF -> FontFamily.Serif
    RingingClockFont.CURSIVE -> FontFamily.Cursive
    RingingClockFont.CUSTOM_FILE -> FontFamily.Default
}

fun resolveRingingFontFamily(font: RingingClockFont, customFontPath: String?): FontFamily =
    if (font == RingingClockFont.CUSTOM_FILE) {
        FontManager.fontFamilyFromPath(customFontPath)
    } else {
        ringingClockFontFamily(font)
    }

fun resolveDateFontFamily(
    dateFont: ClockDateFont,
    clockFont: RingingClockFont,
    clockCustomFontPath: String?,
    dateCustomFontPath: String?
): FontFamily = when (dateFont) {
    ClockDateFont.MATCH_CLOCK -> resolveRingingFontFamily(clockFont, clockCustomFontPath)
    ClockDateFont.SYSTEM_DEFAULT -> FontFamily.SansSerif
    ClockDateFont.SERIF -> FontFamily.Serif
    ClockDateFont.CURSIVE -> FontFamily.Cursive
    ClockDateFont.CUSTOM_FILE -> FontManager.fontFamilyFromPath(dateCustomFontPath)
}

fun ringingBackgroundBrush(style: RingingBackgroundStyle): Brush = when (style) {
    RingingBackgroundStyle.AMOLED_BLACK -> Brush.verticalGradient(
        listOf(Color.Black, Color.Black)
    )
    RingingBackgroundStyle.WARM_SUNRISE -> Brush.verticalGradient(
        AlarmLockPalette.WarmSunriseGradient
    )
    RingingBackgroundStyle.CUSTOM_IMAGE,
    RingingBackgroundStyle.VIDEO,
    RingingBackgroundStyle.DYNAMIC_FLUID,
    RingingBackgroundStyle.STARFIELD,
    RingingBackgroundStyle.INTERACTIVE_GRADIENT,
    RingingBackgroundStyle.AURORA,
    RingingBackgroundStyle.FIREFLIES,
    RingingBackgroundStyle.SOLID_COLOR,
    RingingBackgroundStyle.DARK_MIDNIGHT -> Brush.verticalGradient(
        AlarmLockPalette.DarkMidnightGradient
    )
}

fun clockTextBrush(
    color: Color,
    gradientEnabled: Boolean,
    endColor: Color,
    direction: GradientDirection
): Brush {
    if (!gradientEnabled) return SolidColor(color)
    val second = if (endColor.alpha == 0f) color else endColor
    return when (direction) {
        GradientDirection.VERTICAL -> Brush.verticalGradient(listOf(color, second))
        GradientDirection.HORIZONTAL -> Brush.horizontalGradient(listOf(color, second))
        GradientDirection.DIAGONAL -> Brush.linearGradient(
            colors = listOf(color, second),
            start = Offset.Zero,
            end = Offset.Infinite
        )
        GradientDirection.RADIAL -> Brush.radialGradient(listOf(color, second))
    }
}

@Composable
fun rememberWallpaperImageBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    val state by produceState<ImageBitmap?>(initialValue = null, uriString) {
        value = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    return state
}

@Composable
fun AlarmLockBackground(
    style: RingingBackgroundStyle,
    imageUri: String?,
    videoUri: String? = null,
    blur: Dp = 0.dp,
    dim: Float = -1f,
    solidColor: Color? = null,
    interactiveColors: List<Color> = emptyList(),
    dark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val effectiveDim = if (dim >= 0f) {
        dim
    } else {
        when (style) {
            RingingBackgroundStyle.CUSTOM_IMAGE -> 0.35f
            RingingBackgroundStyle.VIDEO -> 0.28f
            else -> 0f
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        when (style) {
            RingingBackgroundStyle.DYNAMIC_FLUID ->
                FluidGradientBackground(Modifier.fillMaxSize()) {}
            RingingBackgroundStyle.STARFIELD -> StarfieldBackground(
                colors = interactiveColorsOf(interactiveColors, defaultMeshColors(dark)),
                dark = dark,
                modifier = Modifier.fillMaxSize()
            ) {}
            RingingBackgroundStyle.INTERACTIVE_GRADIENT -> InteractiveBackground(
                kind = InteractiveBackgroundKind.MESH_GRADIENT,
                colors = interactiveColorsOf(interactiveColors, defaultMeshColors(dark)),
                dark = dark,
                modifier = Modifier.fillMaxSize()
            )
            RingingBackgroundStyle.AURORA -> InteractiveBackground(
                kind = InteractiveBackgroundKind.AURORA,
                colors = interactiveColorsOf(interactiveColors, defaultMeshColors(dark)),
                dark = dark,
                modifier = Modifier.fillMaxSize()
            )
            RingingBackgroundStyle.FIREFLIES -> InteractiveBackground(
                kind = InteractiveBackgroundKind.FIREFLIES,
                colors = interactiveColorsOf(interactiveColors, defaultMeshColors(dark)),
                dark = dark,
                modifier = Modifier.fillMaxSize()
            )
            RingingBackgroundStyle.AMOLED_BLACK -> AmoledGlowBackground(Modifier.fillMaxSize()) {}
            RingingBackgroundStyle.SOLID_COLOR -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(solidColor ?: Color(0xFF101014))
            )
            RingingBackgroundStyle.VIDEO -> VideoWallpaperBackground(
                videoUri = videoUri,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blur),
                content = {}
            )
            else -> {
                val image = rememberWallpaperImageBitmap(
                    if (style == RingingBackgroundStyle.CUSTOM_IMAGE) imageUri else null
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = ringingBackgroundBrush(style),
                            shape = RoundedCornerShape(0.dp)
                        )
                ) {
                    if (style == RingingBackgroundStyle.CUSTOM_IMAGE && image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(blur)
                        )
                    }
                }
            }
        }
        if (dimOverlayAlpha(style, dim) > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimOverlayAlpha(style, dim)))
            )
        }
        content()
    }
}

private fun dimOverlayAlpha(style: RingingBackgroundStyle, dim: Float): Float {
    if (dim >= 0f) return dim.coerceIn(0f, 1f)
    return when (style) {
        RingingBackgroundStyle.VIDEO -> 0.28f
        RingingBackgroundStyle.CUSTOM_IMAGE -> 0.35f
        else -> 0f
    }
}

@Composable
fun FluidGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "fluidGradient")
    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t2"
    )

    Box(
        modifier = modifier
            .background(Color(0xFF0B0718))
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val blobs = listOf(
                Triple(Color(0xFF6D28D9), Offset(size.width * (0.25f + 0.4f * t1), size.height * (0.3f + 0.2f * t2)), 0.72f),
                Triple(Color(0xFFDB2777), Offset(size.width * (0.75f - 0.35f * t2), size.height * (0.35f + 0.3f * t1)), 0.68f),
                Triple(Color(0xFF0EA5E9), Offset(size.width * (0.5f + 0.3f * t2), size.height * (0.8f - 0.25f * t1)), 0.8f)
            )
            blobs.forEach { (color, center, scale) ->
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.85f), Color.Transparent),
                        center = center,
                        radius = size.maxDimension * scale
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    blendMode = BlendMode.Plus
                )
            }
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.75f
                )
            )
        }
        content()
    }
}

@Composable
fun AmoledGlowBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "amoledGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AlarmLockPalette.NeonGlow.copy(alpha = 0.16f * pulse),
                        AlarmLockPalette.Accent.copy(alpha = 0.08f * pulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.55f * pulse
                ),
                topLeft = Offset.Zero,
                size = size
            )
        }
        content()
    }
}

@Composable
fun VideoWallpaperBackground(
    videoUri: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.background(Color.Black).fillMaxSize()) {
        if (!videoUri.isNullOrBlank()) {
            VideoBackgroundPlayer(
                videoUri = videoUri,
                muted = true,
                loop = true,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }
        content()
    }
}

@Composable
fun AlarmClockBox(
    calendar: java.util.Calendar,
    style: RingingClockStyle,
    fontFamily: FontFamily,
    is24Hour: Boolean,
    showAmPm: Boolean = true,
    contentColor: Color = AlarmLockPalette.Content,
    textBrush: Brush? = null,
    amPmFontFamily: FontFamily? = null,
    amPmBrush: Brush? = null,
    removeLeadingZero: Boolean = false,
    fontSize: TextUnit = 72.sp,
    autoFit: Boolean = false,
    showContainerBorder: Boolean = true,
    clockScale: Float = 1f,

    clockFont: RingingClockFont = RingingClockFont.SYSTEM_DEFAULT,

    customFontPath: String? = null,
    modifier: Modifier = Modifier
) {
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scale = clockScale.coerceAtLeast(0.1f)
    val boxShape = RoundedCornerShape((22f * scale).dp)

    val appFont = com.lucid.alarm.ui.LocalAppFont.current
    val resolvedAmPmFontFamily = amPmFontFamily ?: appFont

    BoxWithConstraints(
        modifier = modifier
            .wrapContentSize(Alignment.Center, unbounded = true)
            .padding(
                horizontal = (18f * scale).dp,
                vertical = (10f * scale).dp
            ),
        contentAlignment = Alignment.Center
    ) {
        val isAnalog = style == RingingClockStyle.COMPACT_ANALOG ||
            style == RingingClockStyle.NUMBERED_ANALOG
        val dialDiameter = min(maxWidth, maxHeight)

        val resolvedFontSize = if (!isAnalog && autoFit &&
            constraints.hasBoundedWidth && constraints.hasBoundedHeight
        ) {
            remember(hour, minute, is24Hour, showAmPm, fontFamily, style, maxWidth, maxHeight, density) {
                autoFitFontSize(
                    measurer = measurer,
                    style = style,
                    fontFamily = fontFamily,
                    is24Hour = is24Hour,
                    showAmPm = showAmPm,
                    maxWidthPx = constraints.maxWidth.toFloat(),
                    maxHeightPx = constraints.maxHeight.toFloat(),
                    density = density
                )
            }
        } else {
            fontSize
        }

        RingingClockFace(
            hour = hour,
            minute = minute,
            second = calendar.get(java.util.Calendar.SECOND),
            style = style,
            fontFamily = fontFamily,
            is24Hour = is24Hour,
            showAmPm = showAmPm,
            contentColor = contentColor,
            textBrush = textBrush,
            amPmFontFamily = resolvedAmPmFontFamily,
            amPmBrush = amPmBrush,
            removeLeadingZero = removeLeadingZero,
            fontSize = resolvedFontSize,
            dialDiameter = if (isAnalog && autoFit) dialDiameter * 0.94f else (150f * scale).dp,
            clockFont = clockFont,
            customFontPath = customFontPath
        )
    }
}

private fun autoFitFontSize(
    measurer: TextMeasurer,
    style: RingingClockStyle,
    fontFamily: FontFamily,
    is24Hour: Boolean,
    showAmPm: Boolean,
    maxWidthPx: Float,
    maxHeightPx: Float,
    density: Density
): TextUnit {
    if (maxWidthPx <= 0f || maxHeightPx <= 0f) return 72.sp
    val sample = when {
        style.isVertical -> "23\n59"
        is24Hour -> "23:59"
        else -> "10:38"
    }
    val weight = if (style.isBold) FontWeight.Black else FontWeight.Light
    val baseSize = 100f
    val baseStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = baseSize.sp,
        letterSpacing = 0.sp
    )
    val measured = measurer.measure(text = sample, style = baseStyle)
    val width = measured.size.width.toFloat()
    val height = measured.size.height.toFloat()
    if (width <= 0f || height <= 0f) return 72.sp
    val reserve = if (showAmPm && !is24Hour) 0.22f else 0f
    val scale = min(maxWidthPx * (1f - reserve) / width, maxHeightPx / height) * 0.96f
    return (baseSize * scale).coerceIn(10f, 500f).sp
}

private const val VERTICAL_INK_GAP_DP = 10f

private data class VerticalInkMetrics(
    val hourInkBottomPx: Float,
    val minuteInkTopPx: Float
)

private fun measureVerticalInk(
    font: RingingClockFont,
    customFontPath: String?,
    fontFamily: FontFamily,
    weight: FontWeight,
    fontSize: TextUnit,
    hourText: String,
    minuteText: String,
    density: Density
): VerticalInkMetrics {
    val sizePx = with(density) { fontSize.toPx() }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = sizePx
        this.typeface = FontManager.typefaceFor(font, customFontPath, fontFamily, weight)
    }
    val metrics = paint.fontMetrics
    val hourBounds = Rect()
    paint.getTextBounds(hourText, 0, hourText.length, hourBounds)
    val minuteBounds = Rect()
    paint.getTextBounds(minuteText, 0, minuteText.length, minuteBounds)

    val baselineFromTop = -metrics.ascent
    return VerticalInkMetrics(
        hourInkBottomPx = baselineFromTop + hourBounds.bottom,
        minuteInkTopPx = baselineFromTop + minuteBounds.top
    )
}

@Composable
private fun VerticalInkClock(
    hourText: String,
    minuteText: String,
    digitStyle: TextStyle,
    hourInkBottomPx: Float,
    minuteInkTopPx: Float,
    gapPx: Float,
    amPm: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Layout(
            content = {
                Text(
                    text = hourText,
                    style = digitStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = minuteText,
                    style = digitStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        ) { measurables, constraints ->
            val hourPlaceable = measurables[0].measure(constraints)
            val minutePlaceable = measurables[1].measure(constraints)
            val gap = gapPx.coerceAtLeast(0f)

            val rawMinuteY = hourInkBottomPx + gap - minuteInkTopPx

            val shift = if (rawMinuteY < 0f) -rawMinuteY else 0f
            val hourY = shift
            val minuteY = rawMinuteY + shift

            val width = maxOf(hourPlaceable.width, minutePlaceable.width)
            val height = maxOf(
                hourY + hourPlaceable.height,
                minuteY + minutePlaceable.height
            )
            val layoutHeight = height.toInt().coerceAtLeast(0)

            layout(width = width, height = layoutHeight) {
                val hourX = ((width - hourPlaceable.width) / 2f).toInt()
                val minuteX = ((width - minutePlaceable.width) / 2f).toInt()
                hourPlaceable.place(hourX, hourY.toInt())
                minutePlaceable.place(minuteX, minuteY.toInt())
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        amPm()
    }
}

@Composable
fun ClockDateText(
    calendar: java.util.Calendar,
    fontFamily: FontFamily,
    fontSize: TextUnit = 18.sp,
    contentColor: Color = AlarmLockPalette.Content,
    textBrush: Brush? = null,
    modifier: Modifier = Modifier
) {
    val dateText = remember(calendar.get(java.util.Calendar.DAY_OF_YEAR)) {
        java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    Text(
        text = dateText,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            lineHeight = TextUnit.Unspecified,
            brush = textBrush
        ),
        color = if (textBrush != null) Color.Unspecified else contentColor.copy(alpha = 0.85f),
        maxLines = 1,
        softWrap = false,
        modifier = modifier.wrapContentSize(Alignment.Center, unbounded = true)
    )
}

private fun clockDigitStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    color: Color,
    brush: Brush?
): TextStyle = if (brush != null) {
    TextStyle(brush = brush, fontWeight = fontWeight, fontSize = fontSize, fontFamily = fontFamily)
} else {
    TextStyle(color = color, fontWeight = fontWeight, fontSize = fontSize, fontFamily = fontFamily)
}

private fun amPmTextStyle(
    fontFamily: FontFamily,
    weight: FontWeight,
    size: TextUnit,
    color: Color,
    brush: Brush?
): TextStyle = if (brush != null) {
    TextStyle(brush = brush, fontFamily = fontFamily, fontWeight = weight, fontSize = size)
} else {
    TextStyle(color = color, fontFamily = fontFamily, fontWeight = weight, fontSize = size)
}

@Composable
fun RingingClockFace(
    hour: Int,
    minute: Int,
    second: Int,
    style: RingingClockStyle,
    fontFamily: FontFamily,
    is24Hour: Boolean,
    contentColor: Color,
    showAmPm: Boolean = true,
    textBrush: Brush? = null,
    amPmFontFamily: FontFamily? = null,
    amPmBrush: Brush? = null,
    removeLeadingZero: Boolean = false,
    fontSize: TextUnit = 72.sp,
    dialDiameter: Dp = 150.dp,
    clockFont: RingingClockFont = RingingClockFont.SYSTEM_DEFAULT,
    customFontPath: String? = null
) {

    val timeText = formatClockDigits(hour, minute, is24Hour, removeLeadingZero)
    val amPm = if (showAmPm) com.lucid.alarm.ui.amPmSuffix(hour, is24Hour) else ""
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitColor = if (textBrush == null) contentColor else Color.Unspecified

    when (style) {
        RingingClockStyle.HORIZONTAL_DIGITAL -> {
            Row(modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)) {
                Text(
                    text = timeText,
                    style = clockDigitStyle(fontFamily, FontWeight.Light, fontSize, digitColor, textBrush),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
                if (amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = amPm,
                        style = amPmTextStyle(
                            amPmFontFamily ?: fontFamily,
                            FontWeight.Medium,
                            fontSize * 0.28f,
                            contentColor.copy(alpha = 0.8f),
                            amPmBrush
                        ),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }

        RingingClockStyle.BOLD_HORIZONTAL -> {
            Row(modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)) {
                Text(
                    text = timeText,
                    style = clockDigitStyle(fontFamily, FontWeight.Black, fontSize, digitColor, textBrush),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline()
                )
                if (amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = amPm,
                        style = amPmTextStyle(
                            amPmFontFamily ?: fontFamily,
                            FontWeight.Bold,
                            fontSize * 0.24f,
                            contentColor.copy(alpha = 0.75f),
                            amPmBrush
                        ),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }

        RingingClockStyle.VERTICAL_DIGITAL,
        RingingClockStyle.BOLD_VERTICAL -> {
            val weight = if (style == RingingClockStyle.BOLD_VERTICAL) {
                FontWeight.Black
            } else {
                FontWeight.Light
            }
            val hourText = formatHour(hour, is24Hour, removeLeadingZero)
            val minuteText = String.format(java.util.Locale.getDefault(), "%02d", minute)

            val ink = remember(fontFamily, clockFont, customFontPath, weight, fontSize, density) {
                measureVerticalInk(
                    font = clockFont,
                    customFontPath = customFontPath,
                    fontFamily = fontFamily,
                    weight = weight,
                    fontSize = fontSize,
                    hourText = hourText,
                    minuteText = minuteText,
                    density = density
                )
            }
            val gapPx = with(density) { VERTICAL_INK_GAP_DP.dp.toPx() }

            val digitStyle = clockDigitStyle(fontFamily, weight, fontSize, digitColor, textBrush)
                .copy(lineHeight = TextUnit.Unspecified)

            VerticalInkClock(
                hourText = hourText,
                minuteText = minuteText,
                digitStyle = digitStyle,
                hourInkBottomPx = ink.hourInkBottomPx,
                minuteInkTopPx = ink.minuteInkTopPx,
                gapPx = gapPx
            ) {
                if (amPm.isNotEmpty()) {
                    Text(
                        text = amPm,
                        style = amPmTextStyle(
                            amPmFontFamily ?: fontFamily,
                            FontWeight.Medium,
                            fontSize * 0.24f,
                            contentColor.copy(alpha = 0.8f),
                            amPmBrush
                        )
                    )
                }
            }
        }

        RingingClockStyle.COMPACT_ANALOG -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(dialDiameter)) {
                    val radius = size.minDimension / 2f
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = contentColor.copy(alpha = 0.85f),
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    for (tick in 0 until 12) {
                        val angleRad = Math.toRadians((tick * 30 - 90).toDouble())
                        val innerR = radius * 0.84f
                        val outerR = radius * 0.94f
                        drawLine(
                            color = contentColor.copy(alpha = 0.7f),
                            start = Offset(
                                x = centerOffset.x + (innerR * cos(angleRad)).toFloat(),
                                y = centerOffset.y + (innerR * sin(angleRad)).toFloat()
                            ),
                            end = Offset(
                                x = centerOffset.x + (outerR * cos(angleRad)).toFloat(),
                                y = centerOffset.y + (outerR * sin(angleRad)).toFloat()
                            ),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    drawAnalogHands(hour, minute, second, radius, centerOffset, contentColor)
                }

                if (amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = amPm,
                        style = amPmTextStyle(
                            amPmFontFamily ?: fontFamily,
                            FontWeight.Medium,
                            18.sp,
                            contentColor.copy(alpha = 0.8f),
                            amPmBrush
                        )
                    )
                }
            }
        }

        RingingClockStyle.NUMBERED_ANALOG -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(dialDiameter)) {
                    val radius = size.minDimension / 2f
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = contentColor.copy(alpha = 0.85f),
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    for (tick in 0 until 60) {
                        val isMajor = tick % 5 == 0
                        val angleRad = Math.toRadians((tick * 6 - 90).toDouble())
                        val innerR = if (isMajor) radius * 0.80f else radius * 0.88f
                        val outerR = radius * 0.93f
                        drawLine(
                            color = contentColor.copy(alpha = if (isMajor) 0.85f else 0.35f),
                            start = Offset(
                                x = centerOffset.x + (innerR * cos(angleRad)).toFloat(),
                                y = centerOffset.y + (innerR * sin(angleRad)).toFloat()
                            ),
                            end = Offset(
                                x = centerOffset.x + (outerR * cos(angleRad)).toFloat(),
                                y = centerOffset.y + (outerR * sin(angleRad)).toFloat()
                            ),
                            strokeWidth = (if (isMajor) 2.5f else 1.2f).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    val numberStyle = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (radius * 0.17f).toSp()
                    )
                    for (number in 1..12) {
                        val angleRad = Math.toRadians((number * 30 - 90).toDouble())
                        val numberRadius = radius * 0.68f
                        val numberCenterX =
                            centerOffset.x + (numberRadius * cos(angleRad)).toFloat()
                        val numberCenterY =
                            centerOffset.y + (numberRadius * sin(angleRad)).toFloat()
                        val layout = measurer.measure(
                            text = number.toString(),
                            style = numberStyle
                        )
                        drawText(
                            textLayoutResult = layout,
                            color = contentColor,
                            topLeft = Offset(
                                x = numberCenterX - layout.size.width / 2f,
                                y = numberCenterY - layout.size.height / 2f
                            )
                        )
                    }

                    drawAnalogHands(hour, minute, second, radius, centerOffset, contentColor)
                }

                if (amPm.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = amPm,
                        style = amPmTextStyle(
                            amPmFontFamily ?: fontFamily,
                            FontWeight.Medium,
                            18.sp,
                            contentColor.copy(alpha = 0.8f),
                            amPmBrush
                        )
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAnalogHands(
    hour: Int,
    minute: Int,
    second: Int,
    radius: Float,
    center: Offset,
    contentColor: Color
) {
    val hourAngle = Math.toRadians(((hour % 12 + minute / 60f) * 30f - 90f).toDouble())
    drawLine(
        color = contentColor,
        start = center,
        end = Offset(
            x = center.x + (radius * 0.5f * cos(hourAngle)).toFloat(),
            y = center.y + (radius * 0.5f * sin(hourAngle)).toFloat()
        ),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round
    )

    val minuteAngle = Math.toRadians((minute * 6f - 90f).toDouble())
    drawLine(
        color = contentColor,
        start = center,
        end = Offset(
            x = center.x + (radius * 0.72f * cos(minuteAngle)).toFloat(),
            y = center.y + (radius * 0.72f * sin(minuteAngle)).toFloat()
        ),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )

    val secondAngle = Math.toRadians((second * 6f - 90f).toDouble())
    drawLine(
        color = contentColor.copy(alpha = 0.7f),
        start = center,
        end = Offset(
            x = center.x + (radius * 0.8f * cos(secondAngle)).toFloat(),
            y = center.y + (radius * 0.8f * sin(secondAngle)).toFloat()
        ),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    drawCircle(color = contentColor, radius = 6.dp.toPx(), center = center)
}

@Composable
fun RingingActionPill(
    text: String,
    onClick: () -> Unit,
    containerColor: Color = AlarmLockPalette.DismissButton,
    contentColor: Color = Color.White,
    fontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
fun SnoozePill(
    snoozeMinutes: Int,
    onSnooze: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    containerColor: Color = Color.White.copy(alpha = 0.10f),
    contentColor: Color = Color.White,
    fontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        RepeatIconButton(
            icon = Icons.Filled.Remove,
            contentDescription = "Decrease snooze",
            tint = contentColor,
            onStep = onMinus
        )
        Text(
            text = "Snooze $snoozeMinutes mins",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            ),
            color = contentColor,
            modifier = Modifier
                .clickable(onClick = onSnooze)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        RepeatIconButton(
            icon = Icons.Filled.Add,
            contentDescription = "Increase snooze",
            tint = contentColor,
            onStep = onPlus
        )
    }
}

@Composable
private fun RepeatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onStep: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .holdToRepeat(onStep = onStep),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

fun Modifier.holdToRepeat(
    initialDelayMs: Long = 400L,
    startIntervalMs: Long = 150L,
    minIntervalMs: Long = 80L,
    stepDownMs: Long = 15L,
    onStep: () -> Unit
): Modifier = composed {
    val currentOnStep by rememberUpdatedState(onStep)
    pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                currentOnStep()
                val releasedEarly = withTimeoutOrNull(initialDelayMs) { awaitRelease() }
                if (releasedEarly == null) {
                    var interval = startIntervalMs
                    while (withTimeoutOrNull(interval) { awaitRelease() } == null) {
                        currentOnStep()
                        interval = (interval - stepDownMs).coerceAtLeast(minIntervalMs)
                    }
                }
            }
        )
    }
}
