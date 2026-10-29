package com.lucid.alarm.util

import android.content.Context
import com.lucid.alarm.R
import java.util.Locale

object TimeLeftFormatter {

    private const val MINUTES_PER_HOUR = 60L
    private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR

    fun remainingMinutes(diffMillis: Long): Long {
        val safe = diffMillis.coerceAtLeast(0L)
        return (safe + 59_999L) / 60_000L
    }

    fun humanized(context: Context, diffMillis: Long): String {
        val totalMinutes = remainingMinutes(diffMillis)
        if (totalMinutes <= 0L) return context.getString(R.string.time_less_than_minute)

        val totalDays = totalMinutes / MINUTES_PER_DAY
        val hours = (totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR

        val weeks = totalDays / 7L
        val remDays = totalDays % 7L

        val parts = buildList {
            if (weeks > 0L) {
                val weekUnit = if (weeks == 1L) context.getString(R.string.unit_week) else context.getString(R.string.unit_weeks)
                add("${String.format(Locale.US, "%d", weeks)} $weekUnit")
                if (remDays > 0L) {
                    val dayUnit = if (remDays == 1L) context.getString(R.string.unit_day) else context.getString(R.string.unit_days)
                    add("${String.format(Locale.US, "%d", remDays)} $dayUnit")
                }
            } else if (totalDays > 0L) {
                val dayUnit = if (totalDays == 1L) context.getString(R.string.unit_day) else context.getString(R.string.unit_days)
                add("${String.format(Locale.US, "%d", totalDays)} $dayUnit")
            } else {
                if (hours > 0L) {
                    val hourUnit = if (hours == 1L) context.getString(R.string.unit_hour) else context.getString(R.string.unit_hours)
                    add("${String.format(Locale.US, "%d", hours)} $hourUnit")
                }
                if (minutes > 0L) {
                    val minUnit = if (minutes == 1L) context.getString(R.string.unit_minute) else context.getString(R.string.unit_minutes)
                    add("${String.format(Locale.US, "%d", minutes)} $minUnit")
                }
            }
        }
        val andSep = context.getString(R.string.and_separator)
        return when (parts.size) {
            0 -> ""
            1 -> parts[0]
            2 -> "${parts[0]}$andSep${parts[1]}"
            else -> parts.dropLast(1).joinToString(", ") + andSep + parts.last()
        }
    }

    fun humanized(diffMillis: Long): String {
        val totalMinutes = remainingMinutes(diffMillis)
        if (totalMinutes <= 0L) return "less than a minute"

        val totalDays = totalMinutes / MINUTES_PER_DAY
        val hours = (totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR

        val weeks = totalDays / 7L
        val remDays = totalDays % 7L

        val parts = buildList {
            if (weeks > 0L) {
                add("${String.format(Locale.US, "%d", weeks)} ${unit(weeks, "week")}")
                if (remDays > 0L) {
                    add("${String.format(Locale.US, "%d", remDays)} ${unit(remDays, "day")}")
                }
            } else if (totalDays > 0L) {
                add("${String.format(Locale.US, "%d", totalDays)} ${unit(totalDays, "day")}")
            } else {
                if (hours > 0L) add("${String.format(Locale.US, "%d", hours)} ${unit(hours, "hour")}")
                if (minutes > 0L) add("${String.format(Locale.US, "%d", minutes)} ${unit(minutes, "minute")}")
            }
        }
        return joinNatural(parts)
    }

    private fun unit(value: Long, name: String): String = if (value == 1L) name else "${name}s"

    private fun joinNatural(parts: List<String>): String = when (parts.size) {
        0 -> ""
        1 -> parts[0]
        2 -> "${parts[0]} and ${parts[1]}"
        else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }
}
