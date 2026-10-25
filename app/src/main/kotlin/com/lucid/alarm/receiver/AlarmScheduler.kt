package com.lucid.alarm.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.lucid.alarm.MainActivity
import com.lucid.alarm.data.AlarmEntity
import java.util.Calendar

/**
 * Centralised AlarmManager wrapper.
 *
 * All trigger times are computed exclusively against the device's local time zone using
 * [Calendar.getInstance] with a default [java.util.TimeZone]. No manual UTC millisecond
 * arithmetic is performed anywhere, which eliminates the "one hour / one minute behind" bug.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Android 14+ lets the user revoke the full-screen-intent grant. When it is missing, a
     * notification with `setFullScreenIntent` is downgraded to a heads-up notification instead of
     * launching over the lock screen, so callers must know whether the alarm UI can appear.
     */
    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    /**
     * Settings screen where the user grants full-screen-intent access (Android 14+ only).
     * Returns null when the platform has no such screen, so callers can skip the launch safely.
     */
    fun requestFullScreenIntentPermissionIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }

    fun requestExactAlarmPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
        }
    }

    /**
     * Computes the next wall-clock trigger time in the local time zone.
     *
     * @param hour24 value in 0..23 exactly as displayed by the picker.
     * @param minute value in 0..59 exactly as displayed by the picker.
     * @param daysOfWeek Calendar.DAY_OF_WEEK constants (Sunday = 1 ... Saturday = 7).
     *                   Empty set means "next occurrence of this time, one-shot".
     */
    fun calculateTriggerTimeMillis(
        hour24: Int,
        minute: Int,
        daysOfWeek: Set<Int> = emptySet()
    ): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If today's time has already passed, roll forward to tomorrow.
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (daysOfWeek.isNotEmpty()) {
            // Walk forward at most 7 days until we land on an allowed weekday.
            var guard = 0
            while (guard < 8 && !daysOfWeek.contains(target.get(Calendar.DAY_OF_WEEK))) {
                target.add(Calendar.DAY_OF_YEAR, 1)
                guard++
            }
        }

        return target.timeInMillis
    }

    fun scheduleAlarm(alarm: AlarmEntity, upcomingLeadMinutes: Int = 0) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        runCatching {
            val triggerAtMillis = calculateTriggerTimeMillis(
                hour24 = alarm.hour,
                minute = alarm.minute,
                daysOfWeek = alarm.daysOfWeek
            )

            setAlarmClockExact(
                RETURN_CODE_FIRE_BASE + alarm.id,
                buildFirePendingIntent(alarm),
                triggerAtMillis
            )

            if (upcomingLeadMinutes > 0) {
                scheduleUpcoming(alarm, triggerAtMillis, upcomingLeadMinutes)
            } else {
                cancelUpcoming(alarm.id)
            }
        }.onFailure { e ->
            Log.e(TAG, "Unable to schedule alarm ${alarm.id}", e)
        }
    }

    private fun scheduleUpcoming(
        alarm: AlarmEntity,
        triggerAtMillis: Long,
        leadMinutes: Int
    ) {
        val upcomingAt = triggerAtMillis - leadMinutes * 60_000L
        if (upcomingAt <= System.currentTimeMillis()) {
            // Too close to the fire time to post a heads-up; skip the pre-notification.
            cancelUpcoming(alarm.id)
            return
        }
        setExact(
            RETURN_CODE_UPCOMING_BASE + alarm.id,
            buildUpcomingPendingIntent(alarm),
            upcomingAt
        )
    }

    private fun setExact(requestCode: Int, pendingIntent: PendingIntent, triggerAtMillis: Long) {
        try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                // Degrade gracefully when the user revoked exact-alarm access.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to schedule exact alarm, falling back to inexact.", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Schedules a user-visible alarm with [AlarmManager.setAlarmClock]. Unlike the plain exact
     * APIs this is treated as a first-class clock alarm by the OS: it is exempt from batching and
     * appears in the status bar, and the [AlarmManager.AlarmClockInfo] carries the intent the
     * system uses when the user taps that indicator. Falls back to [setExact] when exact-alarm
     * access was revoked.
     */
    private fun setAlarmClockExact(
        requestCode: Int,
        pendingIntent: PendingIntent,
        triggerAtMillis: Long
    ) {
        if (!canScheduleExactAlarms()) {
            setExact(requestCode, pendingIntent, triggerAtMillis)
            return
        }
        try {
            val showIntent = PendingIntent.getActivity(
                context,
                RETURN_CODE_SHOW_BASE,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "setAlarmClock rejected, falling back to exact set.", e)
            setExact(requestCode, pendingIntent, triggerAtMillis)
        }
    }

    fun cancelAlarm(alarm: AlarmEntity) = cancelAlarm(alarm.id)

    fun cancelAlarm(alarmId: Int) {
        alarmManager.cancel(buildFirePendingIntentForCancel(alarmId))
        cancelUpcoming(alarmId)
    }

    private fun cancelUpcoming(alarmId: Int) {
        alarmManager.cancel(buildUpcomingPendingIntentForCancel(alarmId))
    }

    /**
     * Schedules a one-shot snooze that fires after [snoozeMinutes]. Used by the ringing
     * screen and the foreground audio service.
     */
    fun scheduleSnooze(
        alarmId: Int,
        label: String,
        hour: Int,
        minute: Int,
        volumeLevel: Float,
        ringtoneUri: String?,
        vibrate: Boolean,
        snoozeMinutes: Int = SNOOZE_MINUTES,
        vibrationPreset: String = "",
        snoozeCount: Int = 1,
        snoozeLimit: Int = -1
    ) {
        val triggerAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }

        val alarm = AlarmEntity(
            id = alarmId,
            hour = triggerCal.get(Calendar.HOUR_OF_DAY),
            minute = triggerCal.get(Calendar.MINUTE),
            label = label,
            soundUri = ringtoneUri.orEmpty(),
            vibrationEnabled = vibrate,
            vibrationPattern = vibrationPreset.ifBlank { "HEARTBEAT" },
            snoozeLimit = snoozeLimit,
            volumeLevel = volumeLevel
        )

        setAlarmClockExact(
            SNOOZE_REQUEST_BASE + alarmId,
            buildFirePendingIntent(
                alarm,
                requestCode = SNOOZE_REQUEST_BASE + alarmId,
                snoozeCount = snoozeCount
            ),
            triggerAtMillis
        )
    }

    private fun buildFirePendingIntent(
        alarm: AlarmEntity,
        requestCode: Int = RETURN_CODE_FIRE_BASE + alarm.id,
        snoozeCount: Int = 0
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, alarm.volumeLevel)
            putExtra(AlarmReceiver.EXTRA_RINGTONE_URI, alarm.soundUri)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_VIBE_PRESET, alarm.vibrationPattern)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, snoozeCount)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, alarm.snoozeLimit)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildFirePendingIntentForCancel(alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            RETURN_CODE_FIRE_BASE + alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: PendingIntent.getBroadcast(
            context,
            RETURN_CODE_FIRE_BASE + alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildUpcomingPendingIntent(alarm: AlarmEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_UPCOMING_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, alarm.minute)
        }
        return PendingIntent.getBroadcast(
            context,
            RETURN_CODE_UPCOMING_BASE + alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildUpcomingPendingIntentForCancel(alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_UPCOMING_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            RETURN_CODE_UPCOMING_BASE + alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: PendingIntent.getBroadcast(
            context,
            RETURN_CODE_UPCOMING_BASE + alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"

        const val SNOOZE_MINUTES = 9

        // Stable, non-overlapping PendingIntent request-code ranges per alarm id.
        private const val RETURN_CODE_FIRE_BASE = 0
        private const val RETURN_CODE_UPCOMING_BASE = 100_000

        /** Base request code for a per-alarm snooze trigger; shared with the audio service. */
        const val SNOOZE_REQUEST_BASE = 200_000
        private const val RETURN_CODE_SHOW_BASE = 300_000

        @JvmStatic
        fun scheduleAlarm(context: Context, alarm: AlarmEntity, upcomingLeadMinutes: Int = 0) {
            AlarmScheduler(context).scheduleAlarm(alarm, upcomingLeadMinutes)
        }

        @JvmStatic
        fun cancelAlarm(context: Context, alarmId: Int) {
            AlarmScheduler(context).cancelAlarm(alarmId)
        }
    }
}
