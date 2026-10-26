package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.service.TimerService
import com.lucid.alarm.service.TimerSessionState
import com.lucid.alarm.ui.components.NumberWheel
import com.lucid.alarm.ui.components.wheelScrollIndex
import com.lucid.alarm.ui.formatNotificationTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun TimerScreen(
    settingsDataStore: SettingsDataStore,
    barsVisible: Boolean = true,
    onOpenSettings: () -> Unit = {},
    timerState: TimerSessionState = TimerSessionState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val accent = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val wheelUnselected = if (isLight) Color(0xFF9CA3AF) else mutedColor
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    val removeLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(initial = false)

    var selectedHours by rememberSaveable { mutableIntStateOf(0) }
    var selectedMinutes by rememberSaveable { mutableIntStateOf(0) }
    var selectedSeconds by rememberSaveable { mutableIntStateOf(0) }

    val isRunning = timerState.running
    val isRinging = timerState.ringing
    val timeLeft = timerState.secondsLeft
    val initialTime = timerState.totalSeconds
    val counting = isRunning || timeLeft > 0 || isRinging
    val idle = !counting

    val neutralColor = if (isLight) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val redColor = Color(0xFFEF4444)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(statusBarTop + 16.dp))

            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val dialTop = (maxHeight * 0.05f).coerceAtLeast(0.dp)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = dialTop),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (counting) {
                        TimerDial(
                            secondsLeft = timeLeft,
                            totalSeconds = initialTime,
                            accent = accent,
                            textColor = textColor,
                            isLight = isLight,
                            isRinging = isRinging,
                            is24Hour = timeFormat.is24Hour,
                            removeLeadingZero = removeLeadingZero,
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .aspectRatio(1f)
                        )
                    } else {
                        TimerWheelPicker(
                            hours = selectedHours,
                            minutes = selectedMinutes,
                            seconds = selectedSeconds,
                            onHours = { selectedHours = it },
                            onMinutes = { selectedMinutes = it },
                            onSeconds = { selectedSeconds = it },
                            textColor = textColor,
                            mutedColor = mutedColor,
                            unselectedColor = wheelUnselected,
                            accent = accent,
                            isLight = isLight
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 36.dp, end = 36.dp, bottom = 148.dp),
                    horizontalArrangement = if (idle) Arrangement.Center else Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRinging) {
                        RoundActionButton(
                            label = "Dismiss",
                            color = redColor,
                            isLight = isLight
                        ) { TimerService.dismiss(context) }
                        RoundActionButton(
                            label = "+1 min",
                            color = accent,
                            isLight = isLight
                        ) { TimerService.addOneMinute(context) }
                    } else {
                        if (isRunning || timeLeft > 0) {
                            RoundActionButton(
                                label = "Reset",
                                color = neutralColor,
                                isLight = isLight
                            ) { TimerService.cancel(context) }
                        }
                        val startLabel = when {
                            isRunning -> "Pause"
                            timeLeft > 0 -> "Resume"
                            else -> "Start"
                        }
                        RoundActionButton(
                            label = startLabel,
                            color = accent,
                            isLight = isLight
                        ) {
                            if (!isRunning && timeLeft == 0) {
                                val total =
                                    selectedHours * 3600 + selectedMinutes * 60 + selectedSeconds
                                if (total > 0) TimerService.start(context, total)
                                return@RoundActionButton
                            }
                            if (isRunning) TimerService.pause(context) else TimerService.resume(context)
                        }
                    }
                }
            }
        }

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
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
internal fun RoundActionButton(
    label: String,
    color: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val labelColor = if (isLight) {
        lerp(color, Color.Black, 0.38f)
    } else {
        lerp(color, Color.White, 0.42f)
    }
    Box(
        modifier = modifier
            .size(98.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.28f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimerWheelPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHours: (Int) -> Unit,
    onMinutes: (Int) -> Unit,
    onSeconds: (Int) -> Unit,
    textColor: Color,
    mutedColor: Color,
    unselectedColor: Color,
    accent: Color,
    isLight: Boolean
) {
    val hoursState = rememberLazyListState(
        initialFirstVisibleItemIndex = wheelScrollIndex(100, hours)
    )
    val minutesState = rememberLazyListState(
        initialFirstVisibleItemIndex = wheelScrollIndex(60, minutes)
    )
    val secondsState = rememberLazyListState(
        initialFirstVisibleItemIndex = wheelScrollIndex(60, seconds)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberWheel(
            values = (0..99).toList(),
            initialValue = hours,
            onValueChange = onHours,
            selectedColor = textColor,
            unselectedColor = unselectedColor,
            selectedFontSize = 48.sp,
            unselectedFontSize = 40.sp,
            unselectedAlpha = 0.28f,
            itemHeight = 68.dp,
            modifier = Modifier.width(96.dp),
            listState = hoursState
        )
        Text(
            ":",
            color = mutedColor,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        NumberWheel(
            values = (0..59).toList(),
            initialValue = minutes,
            onValueChange = onMinutes,
            selectedColor = textColor,
            unselectedColor = unselectedColor,
            selectedFontSize = 48.sp,
            unselectedFontSize = 40.sp,
            unselectedAlpha = 0.28f,
            itemHeight = 68.dp,
            modifier = Modifier.width(96.dp),
            listState = minutesState
        )
        Text(
            ":",
            color = mutedColor,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        NumberWheel(
            values = (0..59).toList(),
            initialValue = seconds,
            onValueChange = onSeconds,
            selectedColor = textColor,
            unselectedColor = unselectedColor,
            selectedFontSize = 48.sp,
            unselectedFontSize = 40.sp,
            unselectedAlpha = 0.28f,
            itemHeight = 68.dp,
            modifier = Modifier.width(96.dp),
            listState = secondsState
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        listOf(
            "00:10:00" to 600,
            "00:15:00" to 900,
            "00:30:00" to 1800
        ).forEach { (label, total) ->
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isLight) Color(0xFFF3F4F6) else accent.copy(alpha = 0.16f)
                    )
                    .clickable {
                        onHours(total / 3600)
                        onMinutes((total % 3600) / 60)
                        onSeconds(total % 60)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = textColor, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TimerDial(
    secondsLeft: Int,
    totalSeconds: Int,
    accent: Color,
    textColor: Color,
    isLight: Boolean,
    isRinging: Boolean,
    is24Hour: Boolean,
    removeLeadingZero: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) {
        (secondsLeft.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val h = secondsLeft / 3600
    val m = (secondsLeft % 3600) / 60
    val s = secondsLeft % 60
    val label = when {
        h > 0 -> String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        m > 0 -> String.format(Locale.getDefault(), "%d:%02d", m, s)
        else -> String.format(Locale.getDefault(), "%d", s)
    }
    val innerColor = if (isLight) Color(0xFFF3F4F6) else Color(0xFF23242C)
    val elapsedColor = if (isLight) Color(0xFFD1D5DB) else Color(0xFF3A3B45)
    val secondary = textColor.copy(alpha = 0.75f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 3.dp.toPx()
            val tickLength = 10.dp.toPx()
            val radius = min(size.width, size.height) / 2f - strokePx
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = innerColor,
                radius = (radius - tickLength - 6.dp.toPx()).coerceAtLeast(0f),
                center = center
            )

            for (i in 0 until 60) {
                val fraction = i / 60f
                val active = progress > 0.001f && fraction < progress
                val angle = (-90f + 360f * fraction) * (PI / 180f).toFloat()
                val outer = Offset(
                    center.x + radius * cos(angle),
                    center.y + radius * sin(angle)
                )
                val inner = Offset(
                    center.x + (radius - tickLength) * cos(angle),
                    center.y + (radius - tickLength) * sin(angle)
                )
                drawLine(
                    color = if (active) accent else elapsedColor,
                    start = inner,
                    end = outer,
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatDurationWords(totalSeconds),
                fontSize = 18.sp,
                color = secondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = textColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (isRinging) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Time's up!",
                        fontSize = 18.sp,
                        color = secondary,
                        maxLines = 1
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatFinishTime(secondsLeft, is24Hour, removeLeadingZero),
                        fontSize = 18.sp,
                        color = secondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatFinishTime(
    secondsLeft: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean
): String {
    val finish = Calendar.getInstance().apply {
        add(Calendar.SECOND, secondsLeft.coerceAtLeast(0))
    }
    return formatNotificationTime(
        finish.get(Calendar.HOUR_OF_DAY),
        finish.get(Calendar.MINUTE),
        is24Hour,
        removeLeadingZero
    )
}

private fun formatDurationWords(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val parts = buildList {
        if (h > 0) add("$h hr")
        if (m > 0) add("$m min")
        if (h == 0 && m == 0) add("$s sec")
    }
    return parts.joinToString(" ")
}
