package com.lucid.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A world-clock city that was moved to the world-clock recycle bin. Independent from the alarm
 * recycle bin; entries older than [RECYCLE_BIN_RETENTION_MILLIS] are purged automatically.
 */
@Entity(tableName = "trashed_zones")
data class TrashedZone(
    @PrimaryKey
    val zoneId: String,
    /** Epoch millis the city was trashed. */
    val deletedAt: Long
) {
    companion object {
        const val RECYCLE_BIN_RETENTION_MILLIS = 3L * 24L * 60L * 60L * 1000L
    }
}
