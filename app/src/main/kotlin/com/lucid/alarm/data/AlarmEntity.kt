package com.lucid.alarm.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: String = "",
    val groupName: String = DEFAULT_GROUP,
    val soundUri: String = "",
    val soundTitle: String = "Default",
    val vibrationPattern: String = "HEARTBEAT",
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,

    val snoozeMinutes: Int = 0,

    val snoozeLimit: Int = -1,

    val areYouAwakeEnabled: Boolean = false,

    val areYouAwakeMinutes: Int = 0,

    val upcomingNotificationEnabled: Boolean = false,

    val upcomingNotificationLeadMinutes: Int = 0,

    val hasCustomAreYouAwake: Boolean = false,

    val hasCustomUpcomingNotification: Boolean = false,

    val customAppearanceEnabled: Boolean = false,

    val customBackgroundUri: String = "",

    val customBackgroundStyle: String = "",

    val customBackgroundSolidColor: Long = 0L,

    val customBackgroundInteractiveColors: String = "",

    val customVideoUri: String = "",

    val customClockColor: Long = 0L,
    val customClockOffsetX: Float = 0f,
    val customClockOffsetY: Float = 0f,
    val customBoxWidth: Float = 0f,
    val customBoxHeight: Float = 0f,

    val customClockBoxEnabled: Boolean = false,

    val customClockBoxColor: Long = 0L,

    val customClockBoxOpacity: Float = 0.35f,

    val customClockStyle: String = "",

    val customClockFont: String = "",

    val customClockFontPath: String = "",

    val customDateFont: String = "",

    val customDateFontPath: String = "",

    val customDateVisible: Boolean = true,
    val customDateOffsetX: Float = 0f,
    val customDateOffsetY: Float = 0f,
    val customDateScale: Float = 1f,

    val customRemoveLeadingZero: Boolean = false,

    val customBlur: Float = -1f,

    val customDim: Float = -1f,

    val customClockColorEnd: Long = 0L,

    val customClockGradient: Boolean = false,

    val customClockGradientDir: String = "",

    val customDateColor: Long = 0L,

    val customDateColorEnd: Long = 0L,

    val customDateGradient: Boolean = false,

    val customDateGradientDir: String = "",

    val customShowAmPm: Boolean? = null,

    val customAmPmFont: String = "",

    val customAmPmFontPath: String = "",

    val customAmPmColor: Long = 0L,

    val customAlarmLabelVisible: Boolean? = null,

    val customAlarmLabelFont: String = "",

    val customAlarmLabelFontPath: String = "",

    val customAlarmLabelColor: Long = 0L,

    val customSnoozeFont: String = "",

    val customSnoozeFontPath: String = "",

    val customSnoozeContainerColor: Long = 0L,

    val customSnoozeTextColor: Long = 0L,

    val customDismissFont: String = "",

    val customDismissFontPath: String = "",

    val customDismissContainerColor: Long = 0L,

    val customDismissTextColor: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val volumeLevel: Float = 0.85f,

    val sortOrder: Int = 0,

    val deletedAt: Long = 0L,

    val trashedGroup: String = ""
) {
    @get:Ignore
    val daysOfWeek: Set<Int>
        get() = if (repeatDays.isBlank()) {
            emptySet()
        } else {
            repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        }

    @get:Ignore
    val ringtoneUri: String?
        get() = soundUri.ifBlank { null }

    @get:Ignore
    val vibrate: Boolean
        get() = vibrationEnabled

    @get:Ignore
    val isTrashed: Boolean
        get() = deletedAt > 0L

    @get:Ignore
    val clockBoxEnabled: Boolean
        get() = customClockBoxEnabled

    @get:Ignore
    val clockBoxColor: Int
        get() = customClockBoxColor.toInt()

    @get:Ignore
    val clockBoxAlpha: Float
        get() = customClockBoxOpacity

    fun effectiveAreYouAwake(globalEnabled: Boolean): Boolean =
        if (hasCustomAreYouAwake) areYouAwakeEnabled else globalEnabled

    fun effectiveUpcomingNotification(globalEnabled: Boolean): Boolean =
        if (hasCustomUpcomingNotification) upcomingNotificationEnabled else globalEnabled

    companion object {
        const val DEFAULT_GROUP = "General"
        const val RECYCLE_BIN_RETENTION_MILLIS = 3L * 24L * 60L * 60L * 1000L
    }
}
