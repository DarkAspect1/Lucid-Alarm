package com.lucid.alarm.util

/**
 * Shared day/hour/minute humaniser for countdowns and alarm toasts.
 *
 * Only non-zero components are emitted; days only appear once the remaining time exceeds
 * 23:59 (i.e. a full day or more).
 */
object TimeLeftFormatter {

    private const val MINUTES_PER_HOUR = 60L
    private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR

    /** Remaining whole minutes, rounded up so "under a minute" collapses to 0. */
    fun remainingMinutes(diffMillis: Long): Long {
        val safe = diffMillis.coerceAtLeast(0L)
        return (safe + 59_999L) / 60_000L
    }

    /**
     * Humanised duration such as "2 days, 3 hours and 15 minutes", omitting any zero unit, or
     * "less than a minute" when under one minute remains.
     */
    fun humanized(diffMillis: Long): String {
        val totalMinutes = remainingMinutes(diffMillis)
        if (totalMinutes <= 0L) return "less than a minute"

        val days = totalMinutes / MINUTES_PER_DAY
        val hours = (totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR

        val parts = buildList {
            if (days > 0L) add("$days ${unit(days, "day")}")
            if (hours > 0L) add("$hours ${unit(hours, "hour")}")
            if (minutes > 0L) add("$minutes ${unit(minutes, "minute")}")
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
