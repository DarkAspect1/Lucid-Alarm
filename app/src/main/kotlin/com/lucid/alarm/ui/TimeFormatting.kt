package com.lucid.alarm.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AlarmVibrationPolicy
import com.lucid.alarm.data.prefs.AlarmVibrationPreset
import com.lucid.alarm.data.prefs.ClockDateFont
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.data.prefs.RingingBackgroundStyle
import com.lucid.alarm.data.prefs.RingingClockFont
import com.lucid.alarm.data.prefs.RingingClockStyle
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.data.prefs.TimerVibrationPolicy
import java.text.DateFormatSymbols
import java.util.Locale

fun formatHourMinute(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    return if (is24Hour) {
        String.format(Locale.US, "%02d:%02d", safeHour, safeMinute)
    } else {
        val displayHour = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        val hourText = if (removeLeadingZero) {
            displayHour.toString()
        } else {
            String.format(Locale.US, "%02d", displayHour)
        }
        "$hourText:${String.format(Locale.US, "%02d", safeMinute)}"
    }
}

fun formatHourMinute(
    hour: Int,
    minute: Int,
    second: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String = formatHourMinute(hour, minute, is24Hour, removeLeadingZero)

fun formatHour(hour: Int, is24Hour: Boolean, removeLeadingZero: Boolean = false): String {
    val safeHour = hour.coerceIn(0, 23)
    return if (is24Hour) {
        String.format(Locale.US, "%02d", safeHour)
    } else {
        val displayHour = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        if (removeLeadingZero) {
            displayHour.toString()
        } else {
            String.format(Locale.US, "%02d", displayHour)
        }
    }
}

fun amPmSuffix(hour: Int, is24Hour: Boolean, context: Context? = null): String {
    if (is24Hour) return ""
    val isAm = hour.coerceIn(0, 23) < 12
    if (context != null) {
        return context.getString(if (isAm) R.string.time_am else R.string.time_pm)
    }
    val lang = Locale.getDefault().language
    if (lang == "ar") return if (isAm) "ص" else "م"
    if (lang == "tr") return if (isAm) "ÖÖ" else "ÖS"
    return runCatching {
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
        val arr = symbols.amPmStrings
        if (arr != null && arr.size >= 2) {
            if (isAm) arr[0] else arr[1]
        } else if (isAm) "AM" else "PM"
    }.getOrDefault(if (isAm) "AM" else "PM")
}

fun formatNotificationTime(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false,
    context: Context? = null
): String {
    val time = formatHourMinute(hour, minute, is24Hour, removeLeadingZero)
    return if (is24Hour) time else "$time ${amPmSuffix(hour, is24Hour, context)}"
}

fun formatClockDigits(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val displayHour = if (is24Hour) {
        safeHour
    } else {
        when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
    }
    val hourText = if (removeLeadingZero && !is24Hour) {
        displayHour.toString()
    } else {
        String.format(Locale.US, "%02d", displayHour)
    }
    return "$hourText:${String.format(Locale.US, "%02d", safeMinute)}"
}

@Composable
fun AlarmVibrationPreset.localizedName(): String = when (this) {
    AlarmVibrationPreset.HEARTBEAT -> stringResource(R.string.vibe_heartbeat)
    AlarmVibrationPreset.TICK_TICK -> stringResource(R.string.vibe_tick_tick)
    AlarmVibrationPreset.CONTINUOUS -> stringResource(R.string.vibe_continuous)
    AlarmVibrationPreset.URGENT_STACCATO -> stringResource(R.string.vibe_urgent_staccato)
}

@Composable
fun TimerVibrationPattern.localizedName(): String = when (this) {
    TimerVibrationPattern.HEARTBEAT -> stringResource(R.string.vibe_heartbeat)
    TimerVibrationPattern.TICK_TICK -> stringResource(R.string.vibe_tick_tick)
    TimerVibrationPattern.CONTINUOUS_PULSE -> stringResource(R.string.vibe_continuous_pulse)
    TimerVibrationPattern.URGENT_BUZZ -> stringResource(R.string.vibe_urgent_buzz)
}

@Composable
fun AlarmVibrationPolicy.localizedLabel(): String = when (this) {
    AlarmVibrationPolicy.UNIFIED -> stringResource(R.string.vibration_policy_unified)
    AlarmVibrationPolicy.PER_ALARM -> stringResource(R.string.vibration_policy_per_alarm)
}

@Composable
fun TimerVibrationPolicy.localizedLabel(): String = when (this) {
    TimerVibrationPolicy.UNIFIED -> stringResource(R.string.vibration_policy_unified)
    TimerVibrationPolicy.PER_TIMER -> stringResource(R.string.vibration_policy_per_timer)
}

@Composable
fun RingingBackgroundStyle.localizedLabel(): String = when (this) {
    RingingBackgroundStyle.DARK_MIDNIGHT -> stringResource(R.string.bg_style_dark_midnight)
    RingingBackgroundStyle.AMOLED_BLACK -> stringResource(R.string.bg_style_amoled_black)
    RingingBackgroundStyle.WARM_SUNRISE -> stringResource(R.string.bg_style_warm_sunrise)
    RingingBackgroundStyle.DYNAMIC_FLUID -> stringResource(R.string.bg_style_dynamic_fluid)
    RingingBackgroundStyle.STARFIELD -> stringResource(R.string.bg_style_starfield)
    RingingBackgroundStyle.INTERACTIVE_GRADIENT -> stringResource(R.string.bg_style_interactive_gradient)
    RingingBackgroundStyle.AURORA -> stringResource(R.string.bg_style_aurora)
    RingingBackgroundStyle.FIREFLIES -> stringResource(R.string.bg_style_fireflies)
    RingingBackgroundStyle.SOLID_COLOR -> stringResource(R.string.bg_style_solid_color)
    RingingBackgroundStyle.CUSTOM_IMAGE -> stringResource(R.string.bg_style_custom_image)
    RingingBackgroundStyle.VIDEO -> stringResource(R.string.bg_style_video)
}

@Composable
fun RingingClockStyle.localizedLabel(): String = when (this) {
    RingingClockStyle.HORIZONTAL_DIGITAL -> stringResource(R.string.clock_style_horizontal)
    RingingClockStyle.VERTICAL_DIGITAL -> stringResource(R.string.clock_style_vertical)
    RingingClockStyle.BOLD_HORIZONTAL -> stringResource(R.string.clock_style_bold_horizontal)
    RingingClockStyle.BOLD_VERTICAL -> stringResource(R.string.clock_style_bold_vertical)
    RingingClockStyle.COMPACT_ANALOG -> stringResource(R.string.clock_style_compact_analog)
    RingingClockStyle.NUMBERED_ANALOG -> stringResource(R.string.clock_style_numbered_analog)
}

@Composable
fun RingingClockFont.localizedLabel(): String = when (this) {
    RingingClockFont.SYSTEM_DEFAULT -> stringResource(R.string.font_system_default)
    RingingClockFont.SERIF -> stringResource(R.string.font_serif)
    RingingClockFont.CURSIVE -> stringResource(R.string.font_cursive)
    RingingClockFont.CUSTOM_FILE -> stringResource(R.string.font_custom_file)
}

@Composable
fun ClockDateFont.localizedLabel(): String = when (this) {
    ClockDateFont.MATCH_CLOCK -> stringResource(R.string.font_match_clock)
    ClockDateFont.SYSTEM_DEFAULT -> stringResource(R.string.font_system_default)
    ClockDateFont.SERIF -> stringResource(R.string.font_serif)
    ClockDateFont.CURSIVE -> stringResource(R.string.font_cursive)
    ClockDateFont.CUSTOM_FILE -> stringResource(R.string.font_custom_file)
}

@Composable
fun GradientDirection.localizedLabel(): String = when (this) {
    GradientDirection.HORIZONTAL -> stringResource(R.string.gradient_dir_horizontal)
    GradientDirection.VERTICAL -> stringResource(R.string.gradient_dir_vertical)
    GradientDirection.DIAGONAL -> stringResource(R.string.gradient_dir_diagonal)
    GradientDirection.RADIAL -> stringResource(R.string.gradient_dir_radial)
}
