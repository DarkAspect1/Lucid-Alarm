package com.lucid.alarm.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class InteractiveBackgroundKind {
    MESH_GRADIENT,
    STARFIELD,
    AURORA,
    FIREFLIES
}

@JvmName("interactiveColorsOfArgb")
fun interactiveColorsOf(argb: List<Long>, fallback: List<Color>): List<Color> {
    val parsed = argb.mapNotNull { runCatching { Color(it) }.getOrNull() }
    return when {
        parsed.size >= 2 -> parsed.take(4)
        else -> fallback
    }
}

fun interactiveColorsOf(colors: List<Color>, fallback: List<Color>): List<Color> =
    if (colors.size >= 2) colors.take(4) else fallback

@Composable
fun InteractiveBackground(
    kind: InteractiveBackgroundKind,
    colors: List<Color>,
    dark: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "interactive_bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing)
        ),
        label = "phase"
    )

    val slowPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32_000, easing = LinearEasing)
        ),
        label = "slowPhase"
    )

    Box(modifier = modifier.fillMaxSize()) {
        when (kind) {
            InteractiveBackgroundKind.MESH_GRADIENT ->
                InteractiveMeshGradient(colors = colors, dark = dark, phase = phase, slowPhase = slowPhase)
            InteractiveBackgroundKind.STARFIELD ->
                StarfieldBackground(colors = colors, dark = dark)
            InteractiveBackgroundKind.AURORA ->
                InteractiveAurora(colors = colors, dark = dark, phase = phase, slowPhase = slowPhase)
            InteractiveBackgroundKind.FIREFLIES ->
                InteractiveFireflies(colors = colors, dark = dark, phase = phase, slowPhase = slowPhase)
        }
    }
}

@Composable
fun InteractiveMeshGradient(
    colors: List<Color>,
    dark: Boolean,
    phase: Float,
    slowPhase: Float,
    modifier: Modifier = Modifier
) {
    val palette = if (colors.size >= 2) colors else defaultMeshColors(dark)

    var touch by mutableStateOfTouch()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nx = (change.position.x / size.width) * 2f - 1f
                        val ny = (change.position.y / size.height) * 2f - 1f
                        touch = touch.copy(
                            x = nx.coerceIn(-1f, 1f),
                            y = ny.coerceIn(-1f, 1f),
                            active = true
                        )
                    },
                    onDragEnd = { touch = touch.copy(active = false) },
                    onDragCancel = { touch = touch.copy(active = false) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pos ->
                        val nx = (pos.x / size.width) * 2f - 1f
                        val ny = (pos.y / size.height) * 2f - 1f
                        touch = touch.copy(
                            x = nx.coerceIn(-1f, 1f),
                            y = ny.coerceIn(-1f, 1f),
                            active = true
                        )
                        tryAwaitRelease()
                        touch = touch.copy(active = false)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = lerp(palette[0], if (dark) Color.Black else Color.White, 0.55f))

        val count = palette.size

        for (i in 0 until count) {
            val seedAngle = (i.toFloat() / count) * 6.2831855f
            val orbitR = 0.28f * minOf(w, h)
            val homeX = w / 2f + cos(seedAngle + slowPhase * 6.28f) * orbitR
            val homeY = h / 2f + sin(seedAngle * 1.3f + slowPhase * 6.28f) * orbitR
            val wobbleX = sin(phase * 6.28f + i) * 0.12f * w
            val wobbleY = cos(phase * 6.28f + i * 1.7f) * 0.12f * h

            val tx = if (touch.active) (touch.x * 0.5f + 0.5f) * w else w / 2f
            val ty = if (touch.active) (touch.y * 0.5f + 0.5f) * h else h / 2f
            val pull = if (touch.active) 0.35f else 0.06f

            val cx = (homeX + wobbleX) + (tx - (homeX + wobbleX)) * pull
            val cy = (homeY + wobbleY) + (ty - (homeY + wobbleY)) * pull
            val radius = (0.42f + 0.08f * sin(phase * 6.28f + i * 2f)) * minOf(w, h) * 0.55f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette[i].copy(alpha = 0.85f), palette[i].copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val alpha: Float,
    val tint: Color = Color.White
)

@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = emptyList(),
    dark: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val stars = remember {
        val random = Random(42L)
        val palette = listOf(
            Color.White,
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0),
            Color(0xFFBAE6FD),
            Color(0xFFE0F2FE)
        )
        List(105) {
            val speed = 0.08f + random.nextFloat() * 0.36f
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.5f + random.nextFloat() * 1.7f,
                speed = speed,
                alpha = 0.28f + random.nextFloat() * 0.72f,
                tint = palette[random.nextInt(palette.size)]
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "starfield")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val obsidian = Color.Black

    Box(
        modifier = modifier
            .background(obsidian)
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = obsidian)
            stars.forEach { star ->
                val y = (((star.y + phase * star.speed) % 1f) * size.height)
                val twinkle = (sin((phase * 6.28318f) + star.x * 10f) + 1f) / 2f
                drawCircle(
                    color = star.tint.copy(alpha = star.alpha * (0.35f + 0.65f * twinkle)),
                    radius = star.radius.dp.toPx(),
                    center = Offset(star.x * size.width, y)
                )
            }
        }
        content()
    }
}

@Composable
fun InteractiveAurora(
    colors: List<Color>,
    dark: Boolean,
    phase: Float,
    slowPhase: Float,
    modifier: Modifier = Modifier
) {
    val base = if (colors.isNotEmpty()) colors else defaultMeshColors(dark)
    var touch by mutableStateOfTouch()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        touch = touch.copy(
                            x = (change.position.x / size.width) * 2f - 1f,
                            active = true
                        )
                    },
                    onDragEnd = { touch = touch.copy(active = false) },
                    onDragCancel = { touch = touch.copy(active = false) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pos ->
                    touch = touch.copy(
                        x = (pos.x / size.width) * 2f - 1f,
                        active = true
                    )
                    tryAwaitRelease()
                    touch = touch.copy(active = false)
                })
            }
    ) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF03040A), lerp(Color.Black, base.first(), 0.12f))
            )
        )

        base.forEachIndexed { band, c ->
            val bandTop = 0.12f + band * 0.16f
            val path = Path()
            val steps = 48
            val amp = 0.06f * h + (if (touch.active) touch.x * 0.02f * h else 0f)
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val x = t * w
                val wave = sin((t * 6.28f) * 1.5f + phase * 6.28f + band) * amp
                val y = bandTop * h + wave + sin(t * 3.14f) * 0.05f * h
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            for (s in steps downTo 0) {
                val t = s.toFloat() / steps
                val x = t * w
                val wave = sin((t * 6.28f) * 1.5f + phase * 6.28f + band) * amp
                val y = bandTop * h + wave + 0.10f * h + sin(t * 3.14f) * 0.05f * h
                path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    listOf(
                        c.copy(alpha = 0.42f),
                        lerp(c, base[(band + 1) % base.size], 0.5f).copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
        }
    }
}

@Composable
fun InteractiveFireflies(
    colors: List<Color>,
    dark: Boolean,
    phase: Float,
    slowPhase: Float,
    modifier: Modifier = Modifier
) {
    val base = if (colors.isNotEmpty()) colors else defaultMeshColors(dark)
    val seeds = remember { List(48) { Random.nextFloat() } }
    var touch by mutableStateOfTouch()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        touch = touch.copy(
                            x = (change.position.x / size.width) * 2f - 1f,
                            y = (change.position.y / size.height) * 2f - 1f,
                            active = true
                        )
                    },
                    onDragEnd = { touch = touch.copy(active = false) },
                    onDragCancel = { touch = touch.copy(active = false) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pos ->
                    touch = touch.copy(
                        x = (pos.x / size.width) * 2f - 1f,
                        y = (pos.y / size.height) * 2f - 1f,
                        active = true
                    )
                    tryAwaitRelease()
                    touch = touch.copy(active = false)
                })
            }
    ) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(lerp(Color.Black, base.first(), 0.14f), Color.Black)
            )
        )
        val tx = if (touch.active) (touch.x * 0.5f + 0.5f) * w else null
        val ty = if (touch.active) (touch.y * 0.5f + 0.5f) * h else null
        for (i in seeds.indices) {
            val s = seeds[i]
            val speed = 0.4f + s * 0.8f

            val y = h - (((slowPhase * speed + s) % 1f) * h)
            val swayX = sin((y / h) * 6.28f * 2f + phase * 6.28f + i) * 0.05f * w
            var x = (s * 5.13f % 1f) * w + swayX
            var yy = y

            if (tx != null && ty != null) {
                val dx = x - tx
                val dy = yy - ty
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val influence = (1f - (dist / (0.35f * minOf(w, h)))).coerceIn(0f, 1f)
                x -= dx * 0.25f * influence
                yy -= dy * 0.25f * influence
            }
            val c = base[i % base.size]
            val glow = 0.5f + 0.5f * sin(phase * 6.28f * 3f + i)
            val radius = (3f + s * 5f) * (0.7f + 0.5f * glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.copy(alpha = 0.85f), c.copy(alpha = 0f)),
                    center = Offset(x, yy),
                    radius = radius * 3f
                ),
                radius = radius * 3f,
                center = Offset(x, yy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.6f * glow),
                radius = radius * 0.4f,
                center = Offset(x, yy)
            )
        }
    }
}

fun defaultMeshColors(dark: Boolean): List<Color> =
    if (dark) {
        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF14B8A6))
    } else {
        listOf(Color(0xFF93C5FD), Color(0xFFC4B5FD), Color(0xFF99F6E4))
    }

private data class TouchState(
    val x: Float = 0f,
    val y: Float = 0f,
    val active: Boolean = false
)

@Composable
private fun mutableStateOfTouch() = remember { mutableStateOf(TouchState()) }

val INTERACTIVE_PRESET_COLORS: List<Color> = listOf(
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6),
    Color(0xFF14B8A6),
    Color(0xFF22C55E),
    Color(0xFFEAB308),
    Color(0xFFD9B478),
    Color(0xFFEF4444),
    Color(0xFFEC4899),
    Color(0xFF06B6D4),
    Color(0xFF64748B)
)

@Composable
fun InteractiveColorPickerRow(
    colors: List<Color>,
    maxColors: Int = 4,
    onColorsChange: (List<Color>) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = if (colors.size >= 2) colors.take(maxColors) else defaultMeshColors(dark = true)
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var addingNew by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        active.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .pointerInput(active.size, index) {
                        detectTapGestures(
                            onTap = { editingIndex = index },
                            onLongPress = {
                                if (active.size > 2 && index < active.size) {
                                    onColorsChange(active.toMutableList().apply { removeAt(index) })
                                }
                            }
                        )
                    }
            )
        }
        if (active.size < maxColors) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { addingNew = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }

    val index = editingIndex
    if (index != null && index < active.size) {
        CustomColorPickerDialog(
            initialColor = active[index].toArgb().toLong(),
            onDismiss = { editingIndex = null },
            onConfirm = { argb ->
                val updated = active.toMutableList()
                updated[index] = Color(argb)
                onColorsChange(updated)
                editingIndex = null
            }
        )
    }
    if (addingNew) {
        val suggested = INTERACTIVE_PRESET_COLORS.firstOrNull { it !in active }
            ?: INTERACTIVE_PRESET_COLORS.first()
        CustomColorPickerDialog(
            initialColor = suggested.toArgb().toLong(),
            onDismiss = { addingNew = false },
            onConfirm = { argb ->
                onColorsChange((active + Color(argb)).take(maxColors))
                addingNew = false
            }
        )
    }
}
