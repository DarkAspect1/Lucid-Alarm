package com.lucid.alarm.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucid.alarm.R
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.receiver.AlarmScheduler
import com.lucid.alarm.ui.amPmSuffix
import com.lucid.alarm.ui.formatHourMinute
import com.lucid.alarm.ui.theme.onColorFor
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmRow(
    alarm: AlarmEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showReenablePill: Boolean = false,
    onReenable: () -> Unit = {},
    showBottomDivider: Boolean = false,
    showSwitch: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedTime = formatHourMinute(alarm.hour, alarm.minute, is24Hour, removeLeadingZero)
    val repeatSummary = repeatSummary(context, alarm)

    val selectedNow = selectionMode && isSelected
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val contentColor = when {
        selectedNow -> MaterialTheme.colorScheme.onPrimary
        alarm.isEnabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
    }
    val secondaryColor = when {
        selectedNow -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        alarm.isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (alarm.label.isNotBlank()) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = when {
                                selectedNow -> contentColor
                                alarm.isEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (alarm.skipUntilEpochMillis > System.currentTimeMillis()) {
                        val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(alarm.skipUntilEpochMillis))
                        Text(
                            text = stringResource(R.string.alarm_will_ring_on, dateStr),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Row {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp
                            ),
                            color = contentColor,
                            modifier = Modifier.alignByBaseline()
                        )
                        if (!is24Hour) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = amPmSuffix(alarm.hour, is24Hour, context),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = contentColor,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    val groupDisplay = if (alarm.groupName == AlarmEntity.DEFAULT_GROUP) stringResource(R.string.group_general) else alarm.groupName
                    val subtitleText = if (alarm.label.isNotBlank()) repeatSummary else "$groupDisplay \u00B7 $repeatSummary"
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selectionMode) {
                    SelectionCheckbox(
                        checked = isSelected,
                        onToggle = { onToggle(!isSelected) }
                    )
                } else if (showSwitch) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle
                    )
                }
            }

            if (showReenablePill && !alarm.isEnabled && alarm.daysOfWeek.isNotEmpty() && alarm.skipUntilEpochMillis <= System.currentTimeMillis()) {
                Surface(
                    onClick = onReenable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = onColorFor(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = reenableLabel(context, alarm),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }
            }

            if (showBottomDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: AlarmEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showReenablePill: Boolean = false,
    onReenable: () -> Unit = {},
    cardShape: Shape = RoundedCornerShape(28.dp),
    cardPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    showBottomDivider: Boolean = false,
    connectedIndex: Int = 0,
    totalConnected: Int = 1,
    showSwitch: Boolean = true,
    modifier: Modifier = Modifier
) {
    val selectedNow = selectionMode && isSelected
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val effectiveShape = cardShape
    val effectivePadding = cardPadding

    FrostedBlurBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(effectivePadding),
        shape = effectiveShape,
        tintColor = if (selectedNow) MaterialTheme.colorScheme.primary else null,
        borderColor = null,
        connectedIndex = connectedIndex,
        totalConnected = totalConnected
    ) {
        AlarmRow(
            alarm = alarm,
            selectionMode = selectionMode,
            isSelected = isSelected,
            is24Hour = is24Hour,
            removeLeadingZero = removeLeadingZero,
            onToggle = onToggle,
            onClick = onClick,
            onLongClick = onLongClick,
            showReenablePill = showReenablePill,
            onReenable = onReenable,
            showBottomDivider = if (selectedNow) false else showBottomDivider,
            showSwitch = showSwitch
        )
    }
}

private fun reenableLabel(context: Context, alarm: AlarmEntity): String {
    val o1 = AlarmScheduler.calculateTriggerTimeMillisStatic(
        hour24 = alarm.hour,
        minute = alarm.minute,
        daysOfWeek = alarm.daysOfWeek,
        weekInterval = alarm.weekInterval,
        anchorWeekStartEpochDay = alarm.anchorWeekStartEpochDay
    )
    val o2 = AlarmScheduler.calculateTriggerTimeMillisStatic(
        hour24 = alarm.hour,
        minute = alarm.minute,
        daysOfWeek = alarm.daysOfWeek,
        weekInterval = alarm.weekInterval,
        anchorWeekStartEpochDay = alarm.anchorWeekStartEpochDay,
        afterMillis = o1 + 60_000L
    )
    val now = Calendar.getInstance()
    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val target = Calendar.getInstance().apply { timeInMillis = o2 }
    val label = when {
        target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> context.getString(R.string.turn_back_on_today)

        target.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> context.getString(R.string.turn_back_on_tomorrow)

        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(target.time)
    }
    return context.getString(R.string.turn_back_on_for, label)
}

private val WEEKDAYS = setOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY
)
private val WEEKENDS = setOf(Calendar.SATURDAY, Calendar.SUNDAY)

private fun repeatSummary(context: Context, alarm: AlarmEntity): String {
    val days = alarm.daysOfWeek
    val base = when {
        days.isEmpty() -> context.getString(R.string.repeat_once)
        days.size == 7 -> context.getString(R.string.repeat_every_day)
        days == WEEKDAYS -> context.getString(R.string.repeat_weekdays)
        days == WEEKENDS -> context.getString(R.string.repeat_weekends)
        else -> days.sorted().joinToString(", ") { dayLabel(it) }
    }
    if (days.isNotEmpty() && alarm.weekInterval > 1) {
        val intervalSuffix = " \u2022 " + context.getString(R.string.every_x_weeks_short, alarm.weekInterval)
        val now = LocalDate.now()
        val anchorDate = if (alarm.anchorWeekStartEpochDay > 0L) {
            LocalDate.ofEpochDay(alarm.anchorWeekStartEpochDay)
        } else {
            now
        }
        val anchorMonday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentMonday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksBetween = ChronoUnit.WEEKS.between(anchorMonday, currentMonday)
        val isCurrentWeekActive = weeksBetween >= 0L && (weeksBetween % alarm.weekInterval.toLong() == 0L)

        if (alarm.isEnabled && !isCurrentWeekActive) {
            val nextTriggerMillis = AlarmScheduler.calculateTriggerTimeMillisStatic(
                alarm.hour,
                alarm.minute,
                alarm.daysOfWeek,
                alarm.weekInterval,
                alarm.anchorWeekStartEpochDay
            )
            val triggerDate = Instant.ofEpochMilli(nextTriggerMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
            return context.getString(R.string.skips_this_week) + " \u2022 " + context.getString(R.string.next_trigger_prefix, triggerDate.format(formatter)) + intervalSuffix
        }
        return "$base$intervalSuffix"
    }
    return base
}

private fun dayLabel(dayOfWeek: Int): String {
    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek) }
    return SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
}
