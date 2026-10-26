package com.lucid.alarm.ui

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
        String.format(Locale.getDefault(), "%02d:%02d", safeHour, safeMinute)
    } else {
        val displayHour = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        val hourText = if (removeLeadingZero) {
            displayHour.toString()
        } else {
            String.format(Locale.getDefault(), "%02d", displayHour)
        }
        "$hourText:${String.format(Locale.getDefault(), "%02d", safeMinute)}"
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
        String.format(Locale.getDefault(), "%02d", safeHour)
    } else {
        val displayHour = when {
            safeHour == 0 -> 12
            safeHour > 12 -> safeHour - 12
            else -> safeHour
        }
        if (removeLeadingZero) {
            displayHour.toString()
        } else {
            String.format(Locale.getDefault(), "%02d", displayHour)
        }
    }
}

fun amPmSuffix(hour: Int, is24Hour: Boolean): String =
    if (is24Hour) "" else if (hour.coerceIn(0, 23) < 12) "AM" else "PM"

fun formatNotificationTime(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String {
    val time = formatHourMinute(hour, minute, is24Hour, removeLeadingZero)
    return if (is24Hour) time else "$time ${amPmSuffix(hour, is24Hour)}"
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
        String.format(Locale.getDefault(), "%02d", displayHour)
    }
    return "$hourText:${String.format(Locale.getDefault(), "%02d", safeMinute)}"
}
