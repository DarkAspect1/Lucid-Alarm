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
    val repeatDays: String = "", // Comma-separated 1..7 (Calendar.DAY_OF_WEEK)
    val groupName: String = DEFAULT_GROUP,
    val soundUri: String = "",
    val soundTitle: String = "Default",
    val vibrationPattern: String = "HEARTBEAT",
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    /** Snooze interval in minutes; 0 means "use the global ringing snooze setting". */
    val snoozeMinutes: Int = 0,
    /** How many times an alarm may be snoozed before it re-rings at full volume; -1 = continuous. */
    val snoozeLimit: Int = -1,
    /** Whether this alarm shows the "Are you awake?" follow-up prompt while ringing. */
    val areYouAwakeEnabled: Boolean = false,
    /** Per-alarm "Are you awake?" delay in minutes; 0 means "use the global awake-check delay". */
    val areYouAwakeMinutes: Int = 0,
    /** Whether this alarm posts an "upcoming alarm" heads-up notification before it rings. */
    val upcomingNotificationEnabled: Boolean = false,
    /** Per-alarm upcoming-notification lead in minutes; 0 means "use the global lead time". */
    val upcomingNotificationLeadMinutes: Int = 0,
    /**
     * True once the user explicitly flips this alarm's "Are you awake?" switch. While false the
     * alarm follows the global setting and keeps tracking it when the global value changes.
     */
    val hasCustomAreYouAwake: Boolean = false,
    /**
     * True once the user explicitly flips this alarm's "Upcoming notification" switch. While false
     * the alarm follows the global setting.
     */
    val hasCustomUpcomingNotification: Boolean = false,
    /** Enables the per-alarm appearance override applied on the ringing/lock screen. */
    val customAppearanceEnabled: Boolean = false,
    /** Persisted URI of a per-alarm wallpaper (empty = use the global ringing background). */
    val customBackgroundUri: String = "",
    /** Per-alarm background style (name of [com.lucid.alarm.data.prefs.RingingBackgroundStyle]; "" = inherit). */
    val customBackgroundStyle: String = "",
    /** Per-alarm solid background Color (0 = inherit the global ringing solid Color). */
    val customBackgroundSolidColor: Long = 0L,
    /**
     * Per-alarm interactive-background colour stops (comma-separated packed ARGB; "" = inherit the
     * global ringing interactive colours). Used by the Interactive Mesh Gradient style.
     */
    val customBackgroundInteractiveColors: String = "",
    /** Per-alarm looping video wallpaper URI (empty = inherit the global ringing video). */
    val customVideoUri: String = "",
    /** ARGB Color for the clock digits; 0 means "inherit the ringing screen Color". */
    val customClockColor: Long = 0L,
    val customClockOffsetX: Float = 0f,
    val customClockOffsetY: Float = 0f,
    val customBoxWidth: Float = 0f,
    val customBoxHeight: Float = 0f,
    /** Draws a translucent backing box behind this alarm's clock digits. */
    val customClockBoxEnabled: Boolean = false,
    /** ARGB Color of the clock backing box; 0 = default near-black. */
    val customClockBoxColor: Long = 0L,
    /** Opacity (0..1) of the clock backing box. */
    val customClockBoxOpacity: Float = 0.35f,
    /** Per-alarm clock face (name of [com.lucid.alarm.data.prefs.RingingClockStyle]; "" = inherit). */
    val customClockStyle: String = "",
    /** Per-alarm clock font (name of [com.lucid.alarm.data.prefs.RingingClockFont]; "" = inherit). */
    val customClockFont: String = "",
    /** On-disk path for a per-alarm custom clock font file (used when the font is CUSTOM_FILE). */
    val customClockFontPath: String = "",
    /** Per-alarm date font (name of [com.lucid.alarm.data.prefs.ClockDateFont]; "" = inherit). */
    val customDateFont: String = "",
    /** On-disk path for a per-alarm custom date font file (used when the font is CUSTOM_FILE). */
    val customDateFontPath: String = "",
    /** Per-alarm date visibility override. */
    val customDateVisible: Boolean = true,
    val customDateOffsetX: Float = 0f,
    val customDateOffsetY: Float = 0f,
    val customDateScale: Float = 1f,
    /** Per-alarm override that strips the leading zero from the hour in 12-hour mode. */
    val customRemoveLeadingZero: Boolean = false,
    /** Per-alarm background blur radius (dp); negative = inherit the global value. */
    val customBlur: Float = -1f,
    /** Per-alarm darkening overlay opacity; negative = inherit the global value. */
    val customDim: Float = -1f,
    /** Second stop of the per-alarm clock gradient (0 = default). */
    val customClockColorEnd: Long = 0L,
    /** Enables the per-alarm clock gradient (solid Color when false). */
    val customClockGradient: Boolean = false,
    /** Per-alarm clock gradient direction (name of [com.lucid.alarm.data.prefs.GradientDirection]; "" = inherit). */
    val customClockGradientDir: String = "",
    /** Per-alarm date text Color (0 = inherit). */
    val customDateColor: Long = 0L,
    /** Second stop of the per-alarm date gradient (0 = default). */
    val customDateColorEnd: Long = 0L,
    /** Enables the per-alarm date gradient (solid Color when false). */
    val customDateGradient: Boolean = false,
    /** Per-alarm date gradient direction (name of [com.lucid.alarm.data.prefs.GradientDirection]; "" = inherit). */
    val customDateGradientDir: String = "",
    /** Per-alarm AM/PM visibility; null = inherit the global ringing setting. */
    val customShowAmPm: Boolean? = null,
    /** Per-alarm AM/PM font (name of [com.lucid.alarm.data.prefs.RingingClockFont]; "" = inherit). */
    val customAmPmFont: String = "",
    /** On-disk path for a per-alarm custom AM/PM font file. */
    val customAmPmFontPath: String = "",
    /** Per-alarm AM/PM Color (0 = inherit). */
    val customAmPmColor: Long = 0L,
    /** Per-alarm "Alarm" label visibility; null = inherit. */
    val customAlarmLabelVisible: Boolean? = null,
    /** Per-alarm label font (name of [com.lucid.alarm.data.prefs.RingingClockFont]; "" = inherit). */
    val customAlarmLabelFont: String = "",
    /** On-disk path for a per-alarm custom label font file. */
    val customAlarmLabelFontPath: String = "",
    /** Per-alarm label Color (0 = inherit). */
    val customAlarmLabelColor: Long = 0L,
    /** Per-alarm snooze button font (name of [com.lucid.alarm.data.prefs.RingingClockFont]; "" = inherit). */
    val customSnoozeFont: String = "",
    /** On-disk path for a per-alarm custom snooze font file. */
    val customSnoozeFontPath: String = "",
    /** Per-alarm snooze button container Color (0 = inherit). */
    val customSnoozeContainerColor: Long = 0L,
    /** Per-alarm snooze button text Color (0 = inherit). */
    val customSnoozeTextColor: Long = 0L,
    /** Per-alarm dismiss button font (name of [com.lucid.alarm.data.prefs.RingingClockFont]; "" = inherit). */
    val customDismissFont: String = "",
    /** On-disk path for a per-alarm custom dismiss font file. */
    val customDismissFontPath: String = "",
    /** Per-alarm dismiss button container Color (0 = inherit). */
    val customDismissContainerColor: Long = 0L,
    /** Per-alarm dismiss button text Color (0 = inherit). */
    val customDismissTextColor: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val volumeLevel: Float = 0.85f,
    /** Manual ordering index used by [com.lucid.alarm.data.prefs.AlarmSortMode.CUSTOM]. */
    val sortOrder: Int = 0,
    /** 0 means "not in the recycle bin"; otherwise the epoch millis it was trashed. */
    val deletedAt: Long = 0L,
    /**
     * When an entire group is deleted, this holds the group name so the recycle bin can present the
     * trashed alarms as a single restorable group. Empty for individually-trashed alarms.
     */
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

    /** Resolves the per-alarm awake-check switch against the global default. */
    fun effectiveAreYouAwake(globalEnabled: Boolean): Boolean =
        if (hasCustomAreYouAwake) areYouAwakeEnabled else globalEnabled

    /** Resolves the per-alarm upcoming-notification switch against the global default. */
    fun effectiveUpcomingNotification(globalEnabled: Boolean): Boolean =
        if (hasCustomUpcomingNotification) upcomingNotificationEnabled else globalEnabled

    companion object {
        const val DEFAULT_GROUP = "General"
        const val RECYCLE_BIN_RETENTION_MILLIS = 3L * 24L * 60L * 60L * 1000L
    }
}
