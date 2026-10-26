package com.lucid.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trashed_zones")
data class TrashedZone(
    @PrimaryKey
    val zoneId: String,

    val deletedAt: Long
) {
    companion object {
        const val RECYCLE_BIN_RETENTION_MILLIS = 3L * 24L * 60L * 60L * 1000L
    }
}
