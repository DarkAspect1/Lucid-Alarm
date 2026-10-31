package com.lucid.alarm.ui.screens

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.service.TimerService
import com.lucid.alarm.service.TimerSessionState
import com.lucid.alarm.service.TimerStopwatchManager
import com.lucid.alarm.ui.components.AppDropdownMenu
import com.lucid.alarm.ui.components.AppMenuItem
import com.lucid.alarm.ui.components.NumberWheel
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.components.wheelScrollIndex
import com.lucid.alarm.ui.formatNotificationTime
import com.lucid.alarm.ui.viewmodel.TimerViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TimerScreen(
    settingsDataStore: SettingsDataStore,
    barsVisible: Boolean = true,
    onOpenSettings: () -> Unit = {},
    onOpenRecycleBin: () -> Unit = {},
    onOpenMultiTimer: () -> Unit = {},
    timerViewModel: TimerViewModel = viewModel(),
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

    val activeTimers by timerViewModel.activeTimers.collectAsState()

    var selectedHours by rememberSaveable { mutableIntStateOf(0) }
    var selectedMinutes by rememberSaveable { mutableIntStateOf(0) }
    var selectedSeconds by rememberSaveable { mutableIntStateOf(0) }

    var userDismissedDial by rememberSaveable { mutableStateOf(false) }

    var liveNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(100)
            liveNowMillis = System.currentTimeMillis()
        }
    }

    val isRunning = timerState.running
    val isRinging = timerState.ringing
    val initialTime = timerState.totalSeconds
    var dialSecondsLeft by remember(timerState.timerId, timerState.endRealtime, isRunning) {
        val initSec = if (isRunning && timerState.endRealtime > 0L) {
            val rem = (timerState.endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            ((rem + 999L) / 1000L).coerceIn(0L, initialTime.toLong()).toInt()
        } else {
            timerState.secondsLeft
        }
        mutableIntStateOf(initSec)
    }

    LaunchedEffect(isRunning, timerState.endRealtime, initialTime) {
        if (isRunning && timerState.endRealtime > 0L) {
            while (isActive) {
                val rem = (timerState.endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                dialSecondsLeft = ((rem + 999L) / 1000L).coerceIn(0L, initialTime.toLong()).toInt()
                delay(100)
            }
        } else {
            dialSecondsLeft = timerState.secondsLeft
        }
    }
    val timeLeft = dialSecondsLeft
    val counting = isRunning || timeLeft > 0 || isRinging

    LaunchedEffect(counting) {
        if (!counting) {
            userDismissedDial = false
        }
    }

    val showDialView = counting && !userDismissedDial

    BackHandler(enabled = showDialView && timerState.timerId != 0L) {
        onOpenMultiTimer()
    }

    val neutralColor = if (isLight) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val redColor = Color(0xFFEF4444)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(statusBarTop + 8.dp))

            if (showDialView) {
                if (timerState.timerId != 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenMultiTimer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (timerState.label.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timerState.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val dialTop = (maxHeight * 0.03f).coerceAtLeast(0.dp)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = dialTop),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                                .fillMaxWidth(0.78f)
                                .aspectRatio(1f)
                        )

                        val runningTimers = activeTimers.filter { it.isRunning || (it.id == timerState.timerId && counting) }
                        if (runningTimers.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
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
                                            text = stringResource(R.string.tab_timer),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(R.string.timer_header_time_left),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = if (!isLight) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f),
                                        thickness = 0.5.dp
                                    )
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .edgeFadeMask(topFade = false, bottomFade = true),
                                        contentPadding = PaddingValues(top = 2.dp, bottom = 8.dp)
                                    ) {
                                        items(runningTimers, key = { it.id }) { item ->
                                            val rem = if (item.isRunning) (item.endTimestamp - liveNowMillis).coerceIn(0L, item.durationMillis) else item.remainingMillis.coerceIn(0L, item.durationMillis)
                                            val isCurrent = item.id == timerState.timerId
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val now = System.currentTimeMillis()
                                                        val remMillis = if (item.isRunning) (item.endTimestamp - now).coerceIn(0L, item.durationMillis) else item.remainingMillis.coerceIn(0L, item.durationMillis)
                                                        val endRt = if (item.isRunning && remMillis > 0L) SystemClock.elapsedRealtime() + remMillis else 0L
                                                        val sec = ((remMillis + 999L) / 1000L).coerceIn(0L, item.durationMillis / 1000L).toInt()
                                                        val tot = (item.durationMillis / 1000L).toInt()
                                                        TimerStopwatchManager.setTimer(
                                                            running = item.isRunning,
                                                            totalSeconds = tot,
                                                            secondsLeft = sec,
                                                            endRealtime = endRt,
                                                            timerId = item.id,
                                                            label = item.label
                                                        )
                                                        TimerService.focusTimer(context, item.id, item.label, sec, tot, remMillis, endRt)
                                                    }
                                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.label.ifBlank { stringResource(R.string.tab_timer) },
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) accent else textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = formatLiveTime(rem, item.durationMillis),
                                                    style = TextStyle(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFeatureSettings = "tnum"
                                                    ),
                                                    color = if (isCurrent) accent else textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 36.dp, end = 36.dp, bottom = 148.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRinging) {
                            RoundActionButton(
                                label = stringResource(R.string.action_dismiss),
                                color = redColor,
                                isLight = isLight
                            ) {
                                TimerService.dismiss(context)
                                userDismissedDial = false
                            }
                            RoundActionButton(
                                label = stringResource(R.string.timer_add_1_min),
                                color = accent,
                                isLight = isLight
                            ) { TimerService.addOneMinute(context) }
                        } else {
                            RoundActionButton(
                                label = stringResource(R.string.action_reset),
                                color = neutralColor,
                                isLight = isLight
                            ) {
                                if (timerState.timerId != 0L) {
                                    val currentCard = activeTimers.firstOrNull { it.id == timerState.timerId }
                                    if (currentCard != null) {
                                        timerViewModel.resetTimerCard(currentCard)
                                    }
                                    TimerService.stopMultiTimer(context, timerState.timerId)
                                    val remainingRunning = activeTimers.filter { it.isRunning && it.id != timerState.timerId }
                                    if (remainingRunning.isNotEmpty()) {
                                        val next = remainingRunning.first()
                                        val rem = (next.endTimestamp - liveNowMillis).coerceAtLeast(0L)
                                        val sec = ((rem + 999L) / 1000L).toInt()
                                        val tot = (next.durationMillis / 1000L).toInt()
                                        TimerService.focusTimer(context, next.id, next.label, sec, tot)
                                    } else {
                                        userDismissedDial = false
                                    }
                                } else {
                                    TimerService.cancel(context)
                                    userDismissedDial = false
                                }
                            }
                            val startLabel = if (isRunning) {
                                stringResource(R.string.action_pause)
                            } else {
                                stringResource(R.string.action_resume)
                            }
                            RoundActionButton(
                                label = startLabel,
                                color = accent,
                                isLight = isLight
                            ) {
                                if (isRunning) TimerService.pause(context) else TimerService.resume(context)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onOpenMultiTimer,
                        shape = RoundedCornerShape(999.dp),
                        color = if (isLight) Color(0xFFF3F4F6) else accent.copy(alpha = 0.16f),
                        contentColor = if (isLight) textColor else accent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = stringResource(R.string.multi_timer),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(28.dp))

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

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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
                                        selectedHours = total / 3600
                                        selectedMinutes = (total % 3600) / 60
                                        selectedSeconds = total % 60
                                    }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = textColor, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 148.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundActionButton(
                            label = stringResource(R.string.action_start),
                            color = accent,
                            isLight = isLight
                        ) {
                            val total = selectedHours * 3600 + selectedMinutes * 60 + selectedSeconds
                            if (total > 0) {
                                userDismissedDial = false
                                TimerService.start(context, total)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.timer_duration_zero),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        if (!showDialView) {
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
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.clock_settings),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun formatLiveTime(millis: Long, maxMillis: Long = Long.MAX_VALUE): String {
    val maxSec = maxMillis / 1000L
    val totalSec = ((millis + 999L) / 1000L).coerceIn(0L, maxSec)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
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
internal fun TimerWheelPicker(
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
    isLight: Boolean,
    itemHeight: Dp = 68.dp,
    height: Dp = 200.dp
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
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
                itemHeight = itemHeight,
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
                itemHeight = itemHeight,
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
                itemHeight = itemHeight,
                modifier = Modifier.width(96.dp),
                listState = secondsState
            )
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
        h > 0 -> String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        m > 0 -> String.format(Locale.US, "%d:%02d", m, s)
        else -> String.format(Locale.US, "%d", s)
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
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.timer_times_up),
                        fontSize = 18.sp,
                        color = secondary,
                        maxLines = 1
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
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

@Composable
private fun formatDurationWords(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val hrStr = stringResource(R.string.unit_hr_short)
    val minStr = stringResource(R.string.unit_min_short)
    val secStr = stringResource(R.string.unit_sec_short)
    val parts = buildList {
        if (h > 0) add("$h $hrStr")
        if (m > 0) add("$m $minStr")
        if (h == 0 && m == 0) add("$s $secStr")
    }
    return parts.joinToString(" ")
}
