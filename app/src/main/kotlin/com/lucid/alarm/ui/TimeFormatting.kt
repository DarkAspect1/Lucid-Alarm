package com.lucid.alarm.ui

import java.util.Locale

/**
 * Centralised 12h/24h time formatting so every card, list and lock-screen honours the
 * global [com.lucid.alarm.data.prefs.TimeFormat] setting.
 */
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

/** Format the current wall-clock time of [java.util.Calendar] (seconds not shown). */
fun formatHourMinute(
    hour: Int,
    minute: Int,
    second: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String = formatHourMinute(hour, minute, is24Hour, removeLeadingZero)

/** Formats only the hour component (used by oversized clock styles). */
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

/** AM/PM suffix for the given hour (empty string in 24h mode). */
fun amPmSuffix(hour: Int, is24Hour: Boolean): String =
    if (is24Hour) "" else if (hour.coerceIn(0, 23) < 12) "AM" else "PM"

/**
 * Full time string for notifications: [formatHourMinute] plus the AM/PM suffix in 12h mode so a
 * notification can never show an ambiguous time when the app is set to 12-hour format.
 */
fun formatNotificationTime(
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String {
    val time = formatHourMinute(hour, minute, is24Hour, removeLeadingZero)
    return if (is24Hour) time else "$time ${amPmSuffix(hour, is24Hour)}"
}

/**
 * Digits-only clock string for the ringing clock face: always zero-padded `HH:mm` in 24h mode and
 * `hh:mm` in 12h mode, with **no** AM/PM suffix (the suffix is rendered as a separate element so it
 * can never be duplicated inside the time text). When [removeLeadingZero] is set (12h only) the hour
 * is rendered without its leading zero (e.g. `8:38`).
 */
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
