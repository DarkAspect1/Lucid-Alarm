package com.lucid.alarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Eight distinct 3-hour lighting phases spanning the full 24-hour cycle. [isBright] is true for the
 * phases whose sky is luminous enough that dark text reads more legibly than white text.
 */
enum class DayNightPhase(val isBright: Boolean) {
    DEEP_MIDNIGHT(false),
    PRE_DAWN(false),
    SUNRISE(true),
    LATE_MORNING(true),
    HIGH_NOON(true),
    AFTERNOON(true),
    MAGENTA_DUSK(true),
    STARLIGHT(false)
}

/** Maps an [hour] (0..23) to one of the eight 3-hour phases. [minute] is accepted for API symmetry. */
fun dayNightPhaseFor(hour: Int, minute: Int): DayNightPhase = phaseForHour(hour)

private fun phaseForHour(hour: Int): DayNightPhase = when (hour.coerceIn(0, 23) / 3) {
    0 -> DayNightPhase.DEEP_MIDNIGHT
    1 -> DayNightPhase.PRE_DAWN
    2 -> DayNightPhase.SUNRISE
    3 -> DayNightPhase.LATE_MORNING
    4 -> DayNightPhase.HIGH_NOON
    5 -> DayNightPhase.AFTERNOON
    6 -> DayNightPhase.MAGENTA_DUSK
    else -> DayNightPhase.STARLIGHT
}

/** True for phases whose sky is bright enough that dark text is more legible than white text. */
fun isDaytimeBrightPhase(hour: Int): Boolean = phaseForHour(hour).isBright

/**
 * Procedurally renders a varied natural landscape behind a card, lit according to the eight 3-hour
 * celestial phases. The [seed] picks a distinct ecosystem archetype and terrain shape so every card
 * looks different while remaining stable (no flicker) across recompositions.
 *
 * @param hour 0..23 — for World Clock cards this is the city's local hour; for alarms it is the
 *   scheduled hour. Drives the phase (sky + celestial body).
 * @param minute 0..59 — accepted for API symmetry with callers.
 * @param seed deterministic per-card seed (e.g. `alarm.id.hashCode()`).
 * @param brightText when true a dark scrim is painted for white text; when false a faint light
 *   veil is painted for dark text.
 */
@Composable
fun DayNightLandscapeBackground(
    hour: Int,
    minute: Int,
    seed: Int,
    brightText: Boolean = true,
    modifier: Modifier = Modifier
) {
    val phase = dayNightPhaseFor(hour, minute)
    val scene = remember(seed, phase) { buildLandscapeScene(seed) }
    Canvas(modifier = modifier) {
        drawLandscape(phase, scene, brightText)
    }
}

/**
 * Backwards-compatible alias so existing call sites keep compiling. Prefer
 * [DayNightLandscapeBackground] in new code.
 */
@Composable
fun DayNightCardBackground(
    hour: Int,
    minute: Int,
    seed: Int,
    brightText: Boolean = true,
    modifier: Modifier = Modifier
) = DayNightLandscapeBackground(
    hour = hour,
    minute = minute,
    seed = seed,
    brightText = brightText,
    modifier = modifier
)

// ---------------------------------------------------------------------------------------------
// Ecosystem archetypes — a seed deterministically selects one, changing terrain + palette mood.
// ---------------------------------------------------------------------------------------------

private enum class Ecosystem {
    ALPINE_PEAKS,
    DESERT_DUNES,
    REDWOOD_FOREST,
    COASTAL_CLIFFS,
    LAKE_BASIN,
    GRASSY_HILLS
}

/**
 * A single depth layer described by a smooth height profile sampled at [profile] fractions. Points
 * are (xFraction, yFraction) pairs where yFraction is measured from the top of the canvas.
 */
@Immutable
private data class TerrainLayer(
    val points: List<Offset>,
    val roughness: Float,
    val treeline: Boolean
)

@Immutable
private data class LandscapeScene(
    val ecosystem: Ecosystem,
    val distant: TerrainLayer,
    val midground: TerrainLayer,
    val foreground: TerrainLayer,
    val hasWater: Boolean,
    val waterLine: Float
)

private fun buildLandscapeScene(seed: Int): LandscapeScene {
    val random = Random(seed)
    val ecosystem = Ecosystem.entries[random.nextInt(Ecosystem.entries.size)]

    // Each layer is a heightfield across the full width; lower yFraction = taller terrain.
    val distant = buildTerrainLayer(
        random = random,
        pointCount = 14,
        baseHeight = 0.42f,
        amplitude = 0.16f,
        roughness = 0.5f,
        treeline = false,
        ecosystem = ecosystem,
        layerIndex = 0
    )
    val midground = buildTerrainLayer(
        random = random,
        pointCount = 16,
        baseHeight = 0.62f,
        amplitude = 0.2f,
        roughness = 0.7f,
        treeline = ecosystem == Ecosystem.REDWOOD_FOREST,
        ecosystem = ecosystem,
        layerIndex = 1
    )
    val foreground = buildTerrainLayer(
        random = random,
        pointCount = 18,
        baseHeight = 0.82f,
        amplitude = 0.16f,
        roughness = 1f,
        treeline = ecosystem == Ecosystem.REDWOOD_FOREST || ecosystem == Ecosystem.ALPINE_PEAKS,
        ecosystem = ecosystem,
        layerIndex = 2
    )

    val hasWater = ecosystem == Ecosystem.LAKE_BASIN || ecosystem == Ecosystem.COASTAL_CLIFFS
    val waterLine = 0.78f

    return LandscapeScene(
        ecosystem = ecosystem,
        distant = distant,
        midground = midground,
        foreground = foreground,
        hasWater = hasWater,
        waterLine = waterLine
    )
}

private fun buildTerrainLayer(
    random: Random,
    pointCount: Int,
    baseHeight: Float,
    amplitude: Float,
    roughness: Float,
    treeline: Boolean,
    ecosystem: Ecosystem,
    layerIndex: Int
): TerrainLayer {
    val points = ArrayList<Offset>(pointCount)
    // Random phase/offset so layers start at different elevations.
    val phase = random.nextFloat() * 6.2831855f
    val frequencyJitter = 0.8f + random.nextFloat() * 0.8f
    for (i in 0 until pointCount) {
        val x = i / (pointCount - 1).toFloat()
        // Layered sinusoids create smooth, natural ridges without harsh zig-zags.
        val waveA = kotlin.math.sin((x * 3.1f * frequencyJitter) + phase)
        val waveB = kotlin.math.sin((x * 7.7f * frequencyJitter) + phase * 1.7f) * 0.45f
        val waveC = kotlin.math.sin((x * 15.3f * frequencyJitter) + phase * 2.3f) * 0.22f
        val wave = (waveA + waveB + waveC) / 1.67f
        val shaped = when (ecosystem) {
            Ecosystem.ALPINE_PEAKS -> {
                // Sharpen ridges into crags.
                val crag = kotlin.math.abs(wave)
                (crag * 1.35f - 0.15f)
            }

            Ecosystem.DESERT_DUNES -> wave * 0.7f
            Ecosystem.REDWOOD_FOREST -> wave * 0.9f
            Ecosystem.COASTAL_CLIFFS -> if (layerIndex == 2) wave * 1.2f else wave * 0.6f
            Ecosystem.LAKE_BASIN -> wave * 0.55f
            Ecosystem.GRASSY_HILLS -> wave * 0.8f
        }
        val h = baseHeight - shaped * amplitude * roughness
        points += Offset(x, h.coerceIn(0.06f, 0.96f))
    }
    return TerrainLayer(points = points, roughness = roughness, treeline = treeline)
}

// ---------------------------------------------------------------------------------------------
// Celestial bodies — position/radius follow the phase arc.
// ---------------------------------------------------------------------------------------------

private enum class CelestialLayer { BACK, MID, FRONT }

@Immutable
private data class CelestialBody(
    val xFraction: Float,
    val yFraction: Float,
    val radiusFraction: Float,
    val coreColor: Color,
    val glowColor: Color,
    val isMoon: Boolean,
    val isRisingOrSetting: Boolean
)

private fun celestialFor(phase: DayNightPhase, seed: Int): CelestialBody {
    // Seed-randomized horizontal/vertical jitter keeps each card's moon/sun unique.
    val random = Random(seed * 2654435761.toInt())
    val jitterX = (random.nextFloat() - 0.5f) * 0.12f
    val jitterY = (random.nextFloat() - 0.5f) * 0.08f
    val base = when (phase) {
        DayNightPhase.DEEP_MIDNIGHT -> CelestialBody(
            xFraction = 0.74f,
            yFraction = 0.2f,
            radiusFraction = 0.115f,
            coreColor = Color(0xFFEAF0FF),
            glowColor = Color(0xFF9DB4E8),
            isMoon = true,
            isRisingOrSetting = false
        )

        DayNightPhase.PRE_DAWN -> CelestialBody(
            xFraction = 0.88f,
            yFraction = 0.44f,
            radiusFraction = 0.09f,
            coreColor = Color(0xFFE3D8F0),
            glowColor = Color(0xFFA98BC8),
            isMoon = true,
            isRisingOrSetting = true
        )

        DayNightPhase.SUNRISE -> CelestialBody(
            xFraction = 0.84f,
            yFraction = 0.52f,
            radiusFraction = 0.13f,
            coreColor = Color(0xFFFFE29B),
            glowColor = Color(0xFFF7A95C),
            isMoon = false,
            isRisingOrSetting = true
        )

        DayNightPhase.LATE_MORNING -> CelestialBody(
            xFraction = 0.82f,
            yFraction = 0.22f,
            radiusFraction = 0.115f,
            coreColor = Color(0xFFFFF6D2),
            glowColor = Color(0xFFFFD980),
            isMoon = false,
            isRisingOrSetting = false
        )

        DayNightPhase.HIGH_NOON -> CelestialBody(
            xFraction = 0.5f,
            yFraction = 0.13f,
            radiusFraction = 0.13f,
            coreColor = Color(0xFFFFFDF0),
            glowColor = Color(0xFFFFE9A3),
            isMoon = false,
            isRisingOrSetting = false
        )

        DayNightPhase.AFTERNOON -> CelestialBody(
            xFraction = 0.2f,
            yFraction = 0.26f,
            radiusFraction = 0.12f,
            coreColor = Color(0xFFFFE0A0),
            glowColor = Color(0xFFE79A4E),
            isMoon = false,
            isRisingOrSetting = false
        )

        DayNightPhase.MAGENTA_DUSK -> CelestialBody(
            xFraction = 0.2f,
            yFraction = 0.5f,
            radiusFraction = 0.13f,
            coreColor = Color(0xFFFFC07A),
            glowColor = Color(0xFFE1614B),
            isMoon = false,
            isRisingOrSetting = true
        )

        DayNightPhase.STARLIGHT -> CelestialBody(
            xFraction = 0.7f,
            yFraction = 0.2f,
            radiusFraction = 0.105f,
            coreColor = Color(0xFFF2F4FF),
            glowColor = Color(0xFFAFC3F0),
            isMoon = true,
            isRisingOrSetting = false
        )
    }
    return base.copy(
        xFraction = (base.xFraction + jitterX).coerceIn(0.12f, 0.9f),
        yFraction = (base.yFraction + jitterY).coerceIn(0.1f, 0.56f)
    )
}

// ---------------------------------------------------------------------------------------------
// Palettes
// ---------------------------------------------------------------------------------------------

private fun skyColors(phase: DayNightPhase): List<Color> = when (phase) {
    DayNightPhase.DEEP_MIDNIGHT -> listOf(
        Color(0xFF070B19),
        Color(0xFF0C1230),
        Color(0xFF111A3A)
    )

    DayNightPhase.PRE_DAWN -> listOf(
        Color(0xFF0F172A),
        Color(0xFF2E1F47),
        Color(0xFF5C3C58)
    )

    DayNightPhase.SUNRISE -> listOf(
        Color(0xFF4A7C9B),
        Color(0xFFE88D67),
        Color(0xFFF7C578)
    )

    DayNightPhase.LATE_MORNING -> listOf(
        Color(0xFF2B6CB0),
        Color(0xFF76B5E3),
        Color(0xFFF3F6F8)
    )

    DayNightPhase.HIGH_NOON -> listOf(
        Color(0xFF1E88E5),
        Color(0xFF64B5F6),
        Color(0xFF90CAF9)
    )

    DayNightPhase.AFTERNOON -> listOf(
        Color(0xFF2C5E8A),
        Color(0xFFD99B52),
        Color(0xFFF2B866)
    )

    DayNightPhase.MAGENTA_DUSK -> listOf(
        Color(0xFF1F1B44),
        Color(0xFF8E2D56),
        Color(0xFFE76F51)
    )

    DayNightPhase.STARLIGHT -> listOf(
        Color(0xFF0B132B),
        Color(0xFF141C36),
        Color(0xFF1C2541)
    )
}

/** (fill, rim) for a terrain depth layer under the given phase. */
private fun layerColors(phase: DayNightPhase, layer: CelestialLayer): Pair<Color, Color> = when (phase) {
    DayNightPhase.DEEP_MIDNIGHT -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF0E1630) to Color(0xFF3B5687)
        CelestialLayer.MID -> Color(0xFF0A1024) to Color(0xFF48699F)
        CelestialLayer.FRONT -> Color(0xFF050912) to Color(0xFF6E8FC4)
    }

    DayNightPhase.PRE_DAWN -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF241733) to Color(0xFF7A4E7E)
        CelestialLayer.MID -> Color(0xFF1B1027) to Color(0xFF8D5A86)
        CelestialLayer.FRONT -> Color(0xFF130A1B) to Color(0xFFA56A8F)
    }

    DayNightPhase.SUNRISE -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF6B6157) to Color(0xFFFFC97A)
        CelestialLayer.MID -> Color(0xFF514A48) to Color(0xFFFFD48C)
        CelestialLayer.FRONT -> Color(0xFF3A3130) to Color(0xFFFFE0A8)
    }

    DayNightPhase.LATE_MORNING -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF8FB4CC) to Color(0xFFFFF4DA)
        CelestialLayer.MID -> Color(0xFF6E94B2) to Color(0xFFFFFBF0)
        CelestialLayer.FRONT -> Color(0xFF4E718F) to Color(0xFFFFFFFF)
    }

    DayNightPhase.HIGH_NOON -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF9FC4DD) to Color(0xFFFFFDF2)
        CelestialLayer.MID -> Color(0xFF7CA3C4) to Color(0xFFFFFFFF)
        CelestialLayer.FRONT -> Color(0xFF587C9C) to Color(0xFFFFFFFF)
    }

    DayNightPhase.AFTERNOON -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF8B7A66) to Color(0xFFFFCE84)
        CelestialLayer.MID -> Color(0xFF6B5A4C) to Color(0xFFFFD992)
        CelestialLayer.FRONT -> Color(0xFF4A3B33) to Color(0xFFFFE6AC)
    }

    DayNightPhase.MAGENTA_DUSK -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF3A2340) to Color(0xFFE96B4C)
        CelestialLayer.MID -> Color(0xFF251529) to Color(0xFFF07A50)
        CelestialLayer.FRONT -> Color(0xFF120A14) to Color(0xFFFF9A5C)
    }

    DayNightPhase.STARLIGHT -> when (layer) {
        CelestialLayer.BACK -> Color(0xFF101731) to Color(0xFF3F5688)
        CelestialLayer.MID -> Color(0xFF0A1024) to Color(0xFF4E6CA0)
        CelestialLayer.FRONT -> Color(0xFF05080F) to Color(0xFF7A94C4)
    }
}

// ---------------------------------------------------------------------------------------------
// Master draw
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawLandscape(
    phase: DayNightPhase,
    scene: LandscapeScene,
    brightText: Boolean
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    val body = celestialFor(phase, scene.seedHash())
    val lightCenter = Offset(width * body.xFraction, height * body.yFraction)

    drawSky(phase)
    drawCelestial(phase, body, width, height)
    drawAtmosphere(phase, body, width, height)
    // Radial glow behind terrain, centered at the sun/moon position.
    drawDirectionalGlow(phase, body, lightCenter, width, height)
    drawTerrainLayer(phase, scene.distant, width, height, CelestialLayer.BACK, body, lightCenter)
    drawTerrainLayer(phase, scene.midground, width, height, CelestialLayer.MID, body, lightCenter)
    if (scene.hasWater) {
        drawWater(phase, scene, body, width, height)
    }
    drawTerrainLayer(phase, scene.foreground, width, height, CelestialLayer.FRONT, body, lightCenter)
    drawReadabilityScrim(phase, width, height, brightText)
}

private fun LandscapeScene.seedHash(): Int =
    ecosystem.ordinal * 101 + distant.points.size * 7 + foreground.points.size

private fun DrawScope.drawSky(phase: DayNightPhase) {
    drawRect(brush = Brush.verticalGradient(skyColors(phase)))
}

private fun DrawScope.drawCelestial(
    phase: DayNightPhase,
    body: CelestialBody,
    width: Float,
    height: Float
) {
    val center = Offset(width * body.xFraction, height * body.yFraction)
    val radius = min(width, height).coerceAtLeast(1f) * body.radiusFraction
    val glowRadius = radius * if (body.isMoon) 3.6f else 4.6f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                body.glowColor.copy(alpha = 0.52f),
                body.glowColor.copy(alpha = 0.2f),
                Color.Transparent
            ),
            center = center,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = center
    )

    if (body.isMoon) {
        // Crescent for the twilight/rising phases, full moon otherwise — seed picks the style.
        val crescent = body.isRisingOrSetting ||
            phase == DayNightPhase.PRE_DAWN ||
            phase == DayNightPhase.STARLIGHT
        drawCircle(color = body.coreColor, radius = radius, center = center)
        if (crescent) {
            drawCircle(
                color = skyColors(phase).first(),
                radius = radius * 0.96f,
                center = center + Offset(radius * 0.42f, -radius * 0.18f)
            )
        }
        // Crisp crater shading.
        drawCircle(
            color = Color(0xFFAEB8D6).copy(alpha = 0.34f),
            radius = radius * 0.2f,
            center = center + Offset(-radius * 0.24f, -radius * 0.16f)
        )
        drawCircle(
            color = Color(0xFFAEB8D6).copy(alpha = 0.26f),
            radius = radius * 0.13f,
            center = center + Offset(radius * 0.22f, radius * 0.2f)
        )
        drawCircle(
            color = Color(0xFFAEB8D6).copy(alpha = 0.2f),
            radius = radius * 0.1f,
            center = center + Offset(radius * 0.05f, -radius * 0.4f)
        )
    } else {
        drawCircle(color = body.coreColor, radius = radius, center = center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.42f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.7f
            ),
            radius = radius * 1.7f,
            center = center
        )
        // Blazing zenith rays at high noon.
        if (phase == DayNightPhase.HIGH_NOON) {
            val rayCount = 12
            for (i in 0 until rayCount) {
                val angle = (i / rayCount.toFloat()) * 6.2831855f
                val startR = radius * 1.35f
                val endR = radius * 2.6f
                drawLine(
                    color = body.coreColor.copy(alpha = 0.35f),
                    start = center + Offset(
                        kotlin.math.cos(angle) * startR,
                        kotlin.math.sin(angle) * startR
                    ),
                    end = center + Offset(
                        kotlin.math.cos(angle) * endR,
                        kotlin.math.sin(angle) * endR
                    ),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawAtmosphere(
    phase: DayNightPhase,
    body: CelestialBody,
    width: Float,
    height: Float
) {
    when (phase) {
        DayNightPhase.DEEP_MIDNIGHT ->
            drawStarfield(width, height, count = 46, maxY = 0.9f, varied = true)

        DayNightPhase.PRE_DAWN -> {
            drawStarfield(width, height, count = 14, maxY = 0.34f, varied = true)
            drawMist(width, height, Color(0xFF6A4A72).copy(alpha = 0.16f), yFraction = 0.7f)
            drawMist(width, height, Color(0xFF5C3C58).copy(alpha = 0.12f), yFraction = 0.82f)
        }

        DayNightPhase.SUNRISE -> {
            drawClouds(width, height, Color(0xFFFFE1B8).copy(alpha = 0.34f))
            drawMist(width, height, Color(0xFFFFE1B8).copy(alpha = 0.2f), yFraction = 0.6f)
            drawMist(width, height, Color(0xFFFFF1D8).copy(alpha = 0.14f), yFraction = 0.78f)
        }

        DayNightPhase.LATE_MORNING, DayNightPhase.HIGH_NOON ->
            drawClouds(width, height, Color.White.copy(alpha = 0.5f))

        DayNightPhase.AFTERNOON ->
            drawClouds(width, height, Color(0xFFFFE0B0).copy(alpha = 0.45f))

        DayNightPhase.MAGENTA_DUSK -> {
            drawClouds(width, height, Color(0xFFF7A4B8).copy(alpha = 0.4f))
            drawMist(width, height, Color(0xFFE4795C).copy(alpha = 0.2f), yFraction = 0.72f)
        }

        DayNightPhase.STARLIGHT ->
            drawStarfield(width, height, count = 34, maxY = 0.82f, varied = true)
    }

    if (body.isRisingOrSetting && !body.isMoon) {
        val center = Offset(width * body.xFraction, height * body.yFraction)
        val radius = min(width, height) * body.radiusFraction
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    body.glowColor.copy(alpha = 0.34f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 6.5f
            ),
            radius = radius * 6.5f,
            center = center
        )
    }
}

private fun DrawScope.drawStarfield(
    width: Float,
    height: Float,
    count: Int,
    maxY: Float,
    varied: Boolean
) {
    val random = Random(0x5EED + count * 97)
    repeat(count) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height * maxY
        val radius = if (varied) {
            (0.4f + random.nextFloat() * 1.5f).dp.toPx()
        } else {
            (0.5f + random.nextFloat() * 1.1f).dp.toPx()
        }
        val alpha = 0.3f + random.nextFloat() * 0.6f
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawMist(width: Float, height: Float, color: Color, yFraction: Float) {
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color, Color.Transparent)
        ),
        topLeft = Offset(-width * 0.2f, height * yFraction - height * 0.1f),
        size = Size(width * 1.4f, height * 0.2f)
    )
}

private fun DrawScope.drawClouds(width: Float, height: Float, color: Color) {
    val random = Random(0xC10D5)
    repeat(3) { index ->
        val baseX = width * (0.16f + random.nextFloat() * 0.7f)
        val baseY = height * (0.1f + index * 0.11f + random.nextFloat() * 0.05f)
        val scale = 0.5f + random.nextFloat() * 0.7f
        drawOval(
            color = color,
            topLeft = Offset(baseX, baseY),
            size = Size(width * 0.24f * scale, height * 0.09f * scale)
        )
        drawOval(
            color = color,
            topLeft = Offset(baseX + width * 0.07f * scale, baseY - height * 0.03f * scale),
            size = Size(width * 0.16f * scale, height * 0.08f * scale)
        )
        drawOval(
            color = color,
            topLeft = Offset(baseX - width * 0.06f * scale, baseY + height * 0.015f * scale),
            size = Size(width * 0.13f * scale, height * 0.06f * scale)
        )
    }
}

/** Wide, soft radial glow centered on the celestial body, drawn behind the terrain. */
private fun DrawScope.drawDirectionalGlow(
    phase: DayNightPhase,
    body: CelestialBody,
    lightCenter: Offset,
    width: Float,
    height: Float
) {
    val glowColor = when (phase) {
        DayNightPhase.DEEP_MIDNIGHT -> Color(0xFF8FA8E0)
        DayNightPhase.PRE_DAWN -> Color(0xFF9C6FB0)
        DayNightPhase.SUNRISE -> Color(0xFFF6A860)
        DayNightPhase.MAGENTA_DUSK -> Color(0xFFE1614B)
        DayNightPhase.STARLIGHT -> Color(0xFF8FA8E0)
        else -> body.glowColor
    }
    val intensity = if (body.isMoon) 0.24f else 0.34f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = intensity),
                glowColor.copy(alpha = intensity * 0.35f),
                Color.Transparent
            ),
            center = lightCenter,
            radius = width * 0.85f
        ),
        radius = width * 0.85f,
        center = lightCenter
    )
}

// ---------------------------------------------------------------------------------------------
// Terrain rendering with directional rim light
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawTerrainLayer(
    phase: DayNightPhase,
    layer: TerrainLayer,
    width: Float,
    height: Float,
    celestialLayer: CelestialLayer,
    body: CelestialBody,
    lightCenter: Offset
) {
    val (fillColor, rimColor) = layerColors(phase, celestialLayer)
    val depthScale = when (celestialLayer) {
        CelestialLayer.BACK -> 0.94f
        CelestialLayer.MID -> 1f
        CelestialLayer.FRONT -> 1.06f
    }

    val path = Path().apply {
        moveTo(0f, height)
        layer.points.forEach { p ->
            lineTo(p.x * width, p.y * height * depthScale)
        }
        lineTo(width, height)
        close()
    }
    drawPath(path = path, color = fillColor)

    // Specular rim stroke along the sky-facing edge, brighter where it faces the light source.
    drawTerrainRimStroke(layer, width, height, depthScale, rimColor, lightCenter)

    if (layer.treeline) {
        drawTreeline(layer, width, height, depthScale, fillColor, rimColor, lightCenter)
    }
}

private fun DrawScope.drawTerrainRimStroke(
    layer: TerrainLayer,
    width: Float,
    height: Float,
    depthScale: Float,
    rimColor: Color,
    lightCenter: Offset
) {
    val points = layer.points
    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        val ax = a.x * width
        val ay = a.y * height * depthScale
        val bx = b.x * width
        val by = b.y * height * depthScale
        val midX = (ax + bx) * 0.5f
        // Faces toward the light horizontally glow brightest.
        val dx = abs(midX - lightCenter.x)
        val facing = (1f - (dx / width).coerceIn(0f, 1f))
        val alpha = (0.18f + facing * 0.62f) * rimColor.alpha
        drawLine(
            color = rimColor.copy(alpha = alpha.coerceIn(0f, 1f)),
            start = Offset(ax, ay),
            end = Offset(bx, by),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawTreeline(
    layer: TerrainLayer,
    width: Float,
    height: Float,
    depthScale: Float,
    fillColor: Color,
    rimColor: Color,
    lightCenter: Offset
) {
    val random = Random((layer.points.size * 7919) + layer.points.hashCode())
    val points = layer.points
    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        val ax = a.x * width
        val ay = a.y * height * depthScale
        val bx = b.x * width
        val by = b.y * height * depthScale
        val span = bx - ax
        if (span <= 0f) continue
        val trees = (span / (9.dp.toPx())).toInt().coerceIn(1, 10)
        for (t in 0 until trees) {
            val fx = ax + span * ((t + 0.5f) / trees)
            val baseY = ay + (by - ay) * ((t + 0.5f) / trees)
            val treeHeight = (10.dp.toPx() + random.nextFloat() * 14.dp.toPx())
            val treeWidth = treeHeight * 0.42f
            val pine = Path().apply {
                moveTo(fx - treeWidth * 0.5f, baseY)
                lineTo(fx, baseY - treeHeight)
                lineTo(fx + treeWidth * 0.5f, baseY)
                close()
            }
            drawPath(path = pine, color = fillColor)
            val dx = abs(fx - lightCenter.x)
            val facing = (1f - (dx / width).coerceIn(0f, 1f))
            drawLine(
                color = rimColor.copy(alpha = (0.2f + facing * 0.55f).coerceIn(0f, 1f)),
                start = Offset(fx, baseY),
                end = Offset(fx, baseY - treeHeight),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawWater(
    phase: DayNightPhase,
    scene: LandscapeScene,
    body: CelestialBody,
    width: Float,
    height: Float
) {
    val waterTop = height * scene.waterLine
    val random = Random(scene.seedHash() * 31 + 17)
    // Vertical reflection band with subtle horizontal ripple streaks.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                skyColors(phase).last().copy(alpha = 0.55f),
                Color(0xFF04070E).copy(alpha = 0.85f)
            ),
            startY = waterTop,
            endY = height
        ),
        topLeft = Offset(0f, waterTop),
        size = Size(width, height - waterTop)
    )
    // Celestial reflection streak.
    val reflectX = width * body.xFraction
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                body.coreColor.copy(alpha = 0.28f),
                Color.Transparent
            ),
            startY = waterTop,
            endY = height
        ),
        topLeft = Offset((reflectX - width * 0.08f).coerceAtLeast(0f), waterTop),
        size = Size(width * 0.16f, height - waterTop)
    )
    repeat(9) {
        val y = waterTop + random.nextFloat() * (height - waterTop)
        val x = random.nextFloat() * width
        val len = width * (0.06f + random.nextFloat() * 0.18f)
        drawLine(
            color = Color.White.copy(alpha = 0.05f + random.nextFloat() * 0.08f),
            start = Offset(x, y),
            end = Offset((x + len).coerceAtMost(width), y),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Adaptive text readability
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawReadabilityScrim(
    phase: DayNightPhase,
    width: Float,
    height: Float,
    brightText: Boolean
) {
    // A left-side gradient guarantees the text column stays legible regardless of terrain/sky.
    if (!brightText) {
        // Dark text: faint light veil on the left to lift contrast, mostly transparent over terrain.
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (phase.isBright) 0.34f else 0.22f),
                    Color.White.copy(alpha = if (phase.isBright) 0.12f else 0.08f),
                    Color.Transparent
                ),
                endX = width * 0.82f
            )
        )
        return
    }
    // White text: dark scrim on the left, strongest during bright phases.
    val baseAlpha = if (phase.isBright) 0.42f else 0.28f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = baseAlpha),
                Color.Black.copy(alpha = baseAlpha * 0.5f),
                Color.Transparent
            ),
            endX = width * 0.85f
        )
    )
    // Gentle top/bottom vignette so time + labels stay crisp across the whole card.
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.14f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.2f)
            )
        )
    )
}
