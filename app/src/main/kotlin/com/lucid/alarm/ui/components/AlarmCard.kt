package com.lucid.alarm.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.ui.amPmSuffix
import com.lucid.alarm.ui.formatHourMinute
import com.lucid.alarm.ui.theme.LocalBoxColors
import com.lucid.alarm.ui.theme.onColorFor
import java.text.SimpleDateFormat
import java.util.Calendar
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
    dayNightBackground: Boolean = false,
    showBottomDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formattedTime = formatHourMinute(alarm.hour, alarm.minute, is24Hour, removeLeadingZero)
    val repeatSummary = repeatSummary(alarm.daysOfWeek)

    val selectedNow = selectionMode && isSelected
    val dynamicBackground = dayNightBackground && !selectedNow
    val skySeed = alarm.id.hashCode()
    val brightPhase = isDaytimeBrightPhase(alarm.hour)
    val dynamicContentColor = if (brightPhase) Color(0xFF0B1220) else Color.White
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val contentColor = when {
        selectedNow -> MaterialTheme.colorScheme.onPrimary
        dynamicBackground && alarm.isEnabled -> dynamicContentColor
        dynamicBackground -> dynamicContentColor.copy(alpha = 0.40f)
        alarm.isEnabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
    }
    val secondaryColor = when {
        selectedNow -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        dynamicBackground && alarm.isEnabled -> dynamicContentColor.copy(alpha = 0.82f)
        dynamicBackground -> dynamicContentColor.copy(alpha = 0.40f)
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
        if (dynamicBackground) {
            DayNightLandscapeBackground(
                hour = alarm.hour,
                minute = alarm.minute,
                seed = skySeed,
                brightText = !brightPhase,
                modifier = Modifier.matchParentSize()
            )
        }
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
                                dynamicBackground || selectedNow -> contentColor
                                alarm.isEnabled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                            }
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
                                text = amPmSuffix(alarm.hour, is24Hour),
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
                    val subtitleText = if (alarm.label.isNotBlank()) repeatSummary else "${alarm.groupName} · $repeatSummary"
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )
                }

                if (selectionMode) {
                    SelectionCheckbox(
                        checked = isSelected,
                        onToggle = { onToggle(!isSelected) }
                    )
                } else {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle
                    )
                }
            }

            if (showReenablePill && !alarm.isEnabled && alarm.daysOfWeek.isNotEmpty()) {
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
                        text = reenableLabel(alarm.daysOfWeek, alarm.hour, alarm.minute),
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
    dayNightBackground: Boolean = false,
    cardShape: Shape = RoundedCornerShape(28.dp),
    cardPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    showBottomDivider: Boolean = false,
    connectedIndex: Int = 0,
    totalConnected: Int = 1,
    modifier: Modifier = Modifier
) {
    val selectedNow = selectionMode && isSelected
    val effectiveShape = if (selectedNow) RoundedCornerShape(24.dp) else cardShape
    val effectivePadding = if (selectedNow && totalConnected > 1) PaddingValues(horizontal = 16.dp, vertical = 3.dp) else cardPadding

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
            dayNightBackground = dayNightBackground,
            showBottomDivider = if (selectedNow) false else showBottomDivider
        )
    }
}

/** Describes the next occurrence of a repeating alarm for the restore pill. */
private fun reenableLabel(days: Set<Int>, hour: Int, minute: Int): String {
    val now = Calendar.getInstance()
    val target = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    var guard = 0
    while (guard < 8 && !days.contains(target.get(Calendar.DAY_OF_WEEK))) {
        target.add(Calendar.DAY_OF_YEAR, 1)
        guard++
    }
    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val label = when {
        target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "today"

        target.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "tomorrow"

        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(target.time)
    }
    return "Turn back on for $label"
}

private fun repeatSummary(days: Set<Int>): String = when {
    days.isEmpty() -> "Once"
    days.size == 7 -> "Every day"
    days == setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    ) -> "Weekdays"
    days == setOf(Calendar.SATURDAY, Calendar.SUNDAY) -> "Weekends"
    else -> days.sorted().joinToString(", ") { DAY_LABELS.getValue(it) }
}

private val DAY_LABELS = mapOf(
    Calendar.SUNDAY to "Sun",
    Calendar.MONDAY to "Mon",
    Calendar.TUESDAY to "Tue",
    Calendar.WEDNESDAY to "Wed",
    Calendar.THURSDAY to "Thu",
    Calendar.FRIDAY to "Fri",
    Calendar.SATURDAY to "Sat"
)
