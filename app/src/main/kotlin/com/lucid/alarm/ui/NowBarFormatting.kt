package com.lucid.alarm.ui

import java.util.Calendar
import java.util.Locale

/**
 * Formatting rules for the "Now Bar".
 *
 * The very same helpers render the in-app card and the system notification (which is built with
 * `RemoteViews` and therefore cannot use Compose formatting), so both surfaces always show the
 * exact same strings.
 */
object NowBarFormatting {

    /** Timer readout: `MM:SS`, or `HH:MM:SS` once the timer runs past an hour. */
    fun timerTime(secondsLeft: Int): String {
        val safe = secondsLeft.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    /**
     * Stopwatch readout for the ongoing notification: `MM:SS`, or `H:MM:SS` past an hour.
     *
     * The services re-post the notification once per second, so the readout only needs whole
     * seconds instead of centiseconds; the lap readout keeps centiseconds through [lapTime].
     */
    fun stopwatchTime(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val h = safe / 3600000L
        val m = (safe % 3600000L) / 60000L
        val s = (safe % 60000L) / 1000L
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    /** Lap readout: `MM:SS.CC` (centiseconds). */
    fun lapTime(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val m = safe / 60000L
        val s = (safe % 60000L) / 1000L
        val cs = (safe % 1000L) / 10L
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", m, s, cs)
    }

    /** Compact duration label, e.g. `45 s`, `10 m`, `1 h 5 m`. */
    fun durationShort(millis: Long): String {
        val totalSeconds = (millis / 1000L).toInt().coerceAtLeast(0)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 && m > 0 -> "$h h $m m"
            h > 0 -> "$h h"
            m > 0 -> "$m m"
            else -> "$s s"
        }
    }

    /**
     * Wall-clock time at which a timer with [remainingMillis] left will hit zero.
     * 24-hour mode renders `23:59`, 12-hour mode renders `11:18 pm` (lower-case suffix, and the
     * leading zero of the hour is dropped when [removeLeadingZero] is set).
     */
    fun estimatedEndTime(
        remainingMillis: Long,
        is24Hour: Boolean,
        removeLeadingZero: Boolean
    ): String {
        val target = System.currentTimeMillis() + remainingMillis.coerceAtLeast(0L)
        val calendar = Calendar.getInstance().apply { timeInMillis = target }
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        if (is24Hour) {
            return String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
        }
        val displayHour = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val hourText = if (removeLeadingZero) {
            displayHour.toString()
        } else {
            String.format(Locale.getDefault(), "%02d", displayHour)
        }
        val suffix = if (hour24 < 12) "am" else "pm"
        return "$hourText:${String.format(Locale.getDefault(), "%02d", minute)} $suffix"
    }

    /** Timer subtitle: `<total> / <end time>` while running, `<total> / Paused` while paused. */
    fun timerSubtitle(
        totalSeconds: Int,
        secondsLeft: Int,
        running: Boolean,
        is24Hour: Boolean,
        removeLeadingZero: Boolean
    ): String {
        val total = durationShort(totalSeconds * 1000L)
        return if (running) {
            "$total / ${estimatedEndTime(secondsLeft * 1000L, is24Hour, removeLeadingZero)}"
        } else {
            "$total / Paused"
        }
    }

    /** Stopwatch subtitle: `No laps completed` or `Lap 3 • 00:11.15`. */
    fun stopwatchSubtitle(lapCount: Int, latestLapSplit: Long): String = if (lapCount <= 0) {
        "No laps completed"
    } else {
        "Lap $lapCount • ${lapTime(latestLapSplit)}"
    }
}
