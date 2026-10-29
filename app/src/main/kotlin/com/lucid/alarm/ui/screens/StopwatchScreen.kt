package com.lucid.alarm.ui.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.R
import com.lucid.alarm.service.StopwatchService
import com.lucid.alarm.service.StopwatchSessionState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchScreen(
    barsVisible: Boolean = true,
    onOpenSettings: () -> Unit = {},
    stopwatchState: StopwatchSessionState = StopwatchSessionState(),
    modifier: Modifier = Modifier
) {
    val isRunning = stopwatchState.running
    val startRealtime = stopwatchState.startRealtime
    val laps = stopwatchState.laps
    val context = LocalContext.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val accent = MaterialTheme.colorScheme.primary
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val redColor = Color(0xFFEF4444)
    val neutralColor = if (dark) Color(0xFF6B7280) else Color(0xFF9CA3AF)

    var liveMillis by remember(
        isRunning,
        startRealtime,
        stopwatchState.elapsedMillis
    ) {
        mutableLongStateOf(stopwatchState.liveMillis)
    }
    LaunchedEffect(isRunning, startRealtime, stopwatchState.elapsedMillis) {
        if (isRunning && startRealtime != 0L) {
            while (true) {
                liveMillis = (SystemClock.elapsedRealtime() - startRealtime).coerceAtLeast(0L)
                withFrameNanos { }
            }
        } else {
            liveMillis = stopwatchState.liveMillis
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val started = liveMillis > 0L || laps.isNotEmpty()
    val idle = !started

    val dialSize = 264.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dialTop = (maxHeight * 0.05f).coerceAtLeast(0.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(statusBarTop + 16.dp))

            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(dialTop))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dialSize)
                        .background(Color.Transparent)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(dialSize)) {
                                if (page == 0) {
                                    StopwatchDial(
                                        millis = liveMillis,
                                        accent = accent,
                                        dark = dark,
                                        dialSize = dialSize,
                                        modifier = Modifier.size(dialSize)
                                    )
                                } else {
                                    StopwatchDigitalPage(
                                        millis = liveMillis,
                                        laps = laps,
                                        accent = accent,
                                        dark = dark,
                                        dialSize = dialSize,
                                        modifier = Modifier.size(dialSize)
                                    )
                                }
                            }
                        }
                    }
                }

                if (laps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val lapDiffs = remember(laps) {
                        laps.mapIndexed { i, lap ->
                            if (i == laps.lastIndex) lap else laps[i] - laps[i + 1]
                        }
                    }
                    val minLap = remember(lapDiffs) { if (lapDiffs.size >= 2) lapDiffs.minOrNull() else null }
                    val maxLap = remember(lapDiffs) { if (lapDiffs.size >= 2) lapDiffs.maxOrNull() else null }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .padding(horizontal = 28.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.stopwatch_lap_header),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.9f)
                                )
                                Text(
                                    text = stringResource(R.string.stopwatch_lap_times),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(R.string.stopwatch_overall_time),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.1f),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = if (dark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                            val fadeBrush = remember {
                                Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.75f to Color.Black,
                                    1.0f to Color.Transparent
                                )
                            }
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                                    },
                                contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
                            ) {
                                itemsIndexed(laps, key = { index, _ -> laps.size - index }) { index, lapTotal ->
                                    val lapNum = laps.size - index
                                    val diff = lapDiffs.getOrElse(index) { lapTotal }
                                    val rowColor = when {
                                        minLap != null && diff == minLap -> Color(0xFF6B8AFF)
                                        maxLap != null && diff == maxLap && maxLap != minLap -> Color(0xFFE55C5C)
                                        dark -> Color.White
                                        else -> Color(0xFF111318)
                                    }

                                    val dm = diff / 60000
                                    val ds = (diff % 60000) / 1000
                                    val dms = (diff % 1000) / 10

                                    val om = lapTotal / 60000
                                    val os = (lapTotal % 60000) / 1000
                                    val oms = (lapTotal % 1000) / 10

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%02d", lapNum),
                                            style = TextStyle(
                                                fontSize = 14.5.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Normal
                                            ),
                                            color = rowColor,
                                            modifier = Modifier.weight(0.9f)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d.%02d", dm, ds, dms),
                                            style = TextStyle(
                                                fontSize = 14.5.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Normal,
                                                fontFamily = FontFamily.SansSerif,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = rowColor,
                                            modifier = Modifier.weight(1.2f),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%02d:%02d.%02d", om, os, oms),
                                            style = TextStyle(
                                                fontSize = 14.5.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Normal,
                                                fontFamily = FontFamily.SansSerif,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = rowColor,
                                            modifier = Modifier.weight(1.1f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) {
                                        accent
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                            .copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 36.dp, end = 36.dp, bottom = 148.dp)
                .zIndex(2f),
            horizontalArrangement = if (idle) Arrangement.Center else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning) {
                RoundActionButton(
                    label = stringResource(R.string.action_lap),
                    color = neutralColor,
                    isLight = !dark
                ) { StopwatchService.lap(context) }
            } else if (started) {
                RoundActionButton(
                    label = stringResource(R.string.action_reset),
                    color = neutralColor,
                    isLight = !dark
                ) { StopwatchService.cancel(context) }
            }
            RoundActionButton(
                label = when {
                    isRunning -> stringResource(R.string.action_stop)
                    started -> stringResource(R.string.action_resume)
                    else -> stringResource(R.string.action_start)
                },
                color = if (isRunning) redColor else accent,
                isLight = !dark
            ) {
                when {
                    isRunning -> StopwatchService.stop(context)
                    started -> StopwatchService.resume(context)
                    else -> StopwatchService.start(context)
                }
            }
        }

        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
        AnimatedVisibility(
            visible = barsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 96.dp)
                .zIndex(4f)
        ) {
            SmallFloatingActionButton(
                onClick = onOpenSettings,
                containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF2C2D35),
                contentColor = if (isLight) Color(0xFF1E1E1E) else Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.clock_settings),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StopwatchDial(
    millis: Long,
    accent: Color,
    dark: Boolean,
    dialSize: Dp = 264.dp,
    modifier: Modifier = Modifier
) {
    val inner = if (dark) Color(0xFF23242C) else Color(0xFFF3F4F6)
    val majorTick = if (dark) Color.White else Color(0xFF111318)
    val minorTick = if (dark) Color.White.copy(alpha = 0.22f) else Color(0xFF111318).copy(alpha = 0.20f)
    val borderColor = if (dark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
    val digitalColor = if (dark) Color.White else Color(0xFF111318)

    Box(modifier = modifier.size(dialSize), contentAlignment = Alignment.Center) {
        val innerCircleRadiusDp = (dialSize / 2f) - 24.dp
        val textOffsetY = -innerCircleRadiusDp * 0.32f

        Canvas(modifier = Modifier.size(dialSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerTickOuterRadius = (size.minDimension / 2f) - 4.dp.toPx()
            val outerTickInnerRadius = outerTickOuterRadius - 7.dp.toPx()
            val innerCircleRadius = (size.minDimension / 2f) - 24.dp.toPx()
            val tickStrokeWidth = 2.dp.toPx()

            for (i in 0 until 60) {
                val angle = Math.toRadians((i * 6.0) - 90.0)
                val major = i % 5 == 0
                val start = Offset(
                    center.x + (outerTickInnerRadius * cos(angle)).toFloat(),
                    center.y + (outerTickInnerRadius * sin(angle)).toFloat()
                )
                val end = Offset(
                    center.x + (outerTickOuterRadius * cos(angle)).toFloat(),
                    center.y + (outerTickOuterRadius * sin(angle)).toFloat()
                )
                drawLine(
                    color = if (major) majorTick else minorTick,
                    start = start,
                    end = end,
                    strokeWidth = tickStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(color = inner, radius = innerCircleRadius, center = center)
            drawCircle(
                color = borderColor,
                radius = innerCircleRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            val angle = Math.toRadians((((millis % 60_000L) / 60_000.0) * 360.0) - 90.0)
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            val mainEnd = Offset(
                center.x + outerTickOuterRadius * cosA,
                center.y + outerTickOuterRadius * sinA
            )
            val tailEnd = Offset(
                center.x - 12.dp.toPx() * cosA,
                center.y - 12.dp.toPx() * sinA
            )
            drawLine(
                color = accent,
                start = tailEnd,
                end = center,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accent,
                start = center,
                end = mainEnd,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = accent, radius = 5.dp.toPx(), center = center)
        }

        val m = millis / 60000
        val s = (millis % 60000) / 1000
        val ms = (millis % 1000) / 10
        Text(
            text = String.format(Locale.US, "%02d:%02d.%02d", m, s, ms),
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "tnum"
            ),
            color = digitalColor,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = textOffsetY)
        )
    }
}

@Composable
private fun StopwatchDigitalPage(
    millis: Long,
    laps: List<Long>,
    accent: Color,
    dark: Boolean,
    dialSize: Dp = 264.dp,
    modifier: Modifier = Modifier
) {
    val primary = if (dark) MaterialTheme.colorScheme.onSurface else Color(0xFF111318)
    val secondary = primary.copy(alpha = 0.65f)
    val m = millis / 60000
    val s = (millis % 60000) / 1000
    val ms = (millis % 1000) / 10
    val lastLap = when {
        laps.size >= 2 -> laps[0] - laps[1]
        laps.size == 1 -> laps[0]
        else -> 0L
    }
    val lapM = lastLap / 60000
    val lapS = (lastLap % 60000) / 1000
    val lapMs = (lastLap % 1000) / 10

    Box(modifier = modifier.size(dialSize), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%02d:%02d.%02d", m, s, ms),
                style = TextStyle(
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    fontFeatureSettings = "tnum"
                ),
                color = primary,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
            if (laps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        stringResource(R.string.stopwatch_last_lap_pattern),
                        lapM,
                        lapS,
                        lapMs
                    ),
                    fontSize = 16.sp,
                    color = secondary,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
