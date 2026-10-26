package com.lucid.alarm.ui

import java.util.Calendar
import java.util.Locale

object NowBarFormatting {

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

    fun lapTime(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val m = safe / 60000L
        val s = (safe % 60000L) / 1000L
        val cs = (safe % 1000L) / 10L
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", m, s, cs)
    }

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

    fun stopwatchSubtitle(lapCount: Int, latestLapSplit: Long): String = if (lapCount <= 0) {
        "No laps completed"
    } else {
        "Lap $lapCount • ${lapTime(latestLapSplit)}"
    }
}
