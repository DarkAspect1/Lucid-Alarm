package com.lucid.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val label: String = "",
    val durationMillis: Long,
    val remainingMillis: Long,
    val endTimestamp: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val orderIndex: Int = 0,
    val volume: Float = 1.0f,
    val vibrate: Boolean = true,
    val vibrationPattern: String = "TICK_TICK"
)
