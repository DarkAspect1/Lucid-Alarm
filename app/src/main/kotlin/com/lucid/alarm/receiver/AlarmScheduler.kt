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
import com.lucid.alarm.service.AlarmAudioService
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
    }

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
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
    }

    fun calculateTriggerTimeMillis(
        hour24: Int,
        minute: Int,
        daysOfWeek: Set<Int> = emptySet(),
        weekInterval: Int = 1,
        anchorWeekStartEpochDay: Long = 0L
    ): Long {
        return calculateTriggerTimeMillisStatic(
            hour24 = hour24,
            minute = minute,
            daysOfWeek = daysOfWeek,
            weekInterval = weekInterval,
            anchorWeekStartEpochDay = anchorWeekStartEpochDay
        )
    }

    fun scheduleAlarm(alarm: AlarmEntity, upcomingLeadMinutes: Int = 0) {
        val isSkipped = !alarm.isEnabled && alarm.skipUntilEpochMillis > System.currentTimeMillis()
        if (!alarm.isEnabled && !isSkipped) {
            cancelAlarm(alarm.id)
            return
        }

        runCatching {
            val triggerAtMillis = if (isSkipped) {
                alarm.skipUntilEpochMillis
            } else {
                calculateTriggerTimeMillis(
                    hour24 = alarm.hour,
                    minute = alarm.minute,
                    daysOfWeek = alarm.daysOfWeek,
                    weekInterval = alarm.weekInterval,
                    anchorWeekStartEpochDay = alarm.anchorWeekStartEpochDay
                )
            }

            setAlarmClockExact(
                requestCode = alarm.id * 10 + OFFSET_RING,
                showRequestCode = alarm.id * 10 + OFFSET_RING_SHOW,
                pendingIntent = buildRingPendingIntent(alarm),
                triggerAtMillis = triggerAtMillis
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

    fun scheduleSnooze(
        alarmId: Int,
        label: String,
        hour: Int,
        minute: Int,
        volumeLevel: Float,
        ringtoneUri: String?,
        vibrate: Boolean,
        snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
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
            requestCode = alarmId * 10 + OFFSET_SNOOZE,
            showRequestCode = alarmId * 10 + OFFSET_SNOOZE_SHOW,
            pendingIntent = buildSnoozePendingIntent(alarm, snoozeCount),
            triggerAtMillis = triggerAtMillis
        )
    }

    fun scheduleAwakeCheckStage1(alarmId: Int, triggerAtMillis: Long) {
        scheduleAwakeCheckTimeout(alarmId, triggerAtMillis)
    }

    fun scheduleAwakeCheck(alarmId: Int, checkDelayMinutes: Int) {
        val triggerAtMillis = System.currentTimeMillis() + checkDelayMinutes.toLong() * 60_000L
        scheduleAwakeCheckTimeout(alarmId, triggerAtMillis)
    }

    fun scheduleAwakeCheckTimeout(alarmId: Int, triggerAtMillis: Long) {
        val pendingIntent = buildAwakeTimeoutPendingIntent(alarmId)
        setExact(alarmId * 10 + OFFSET_AWAKE_TIMEOUT, pendingIntent, triggerAtMillis)
    }

    private fun scheduleUpcoming(
        alarm: AlarmEntity,
        triggerAtMillis: Long,
        leadMinutes: Int
    ) {
        val upcomingAt = triggerAtMillis - leadMinutes * 60_000L
        if (upcomingAt <= System.currentTimeMillis()) {
            cancelUpcoming(alarm.id)
            return
        }
        val pendingIntent = buildUpcomingPendingIntent(alarm)
        setExact(alarm.id * 10 + OFFSET_UPCOMING, pendingIntent, upcomingAt)
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

    private fun setAlarmClockExact(
        requestCode: Int,
        showRequestCode: Int,
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
                showRequestCode,
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
        cancelPendingIntent(alarmId * 10 + OFFSET_RING, AlarmReceiver.ACTION_FIRE_ALARM)
        cancelPendingIntent(alarmId * 10 + OFFSET_RING_SHOW)
        cancelUpcoming(alarmId)
        cancelAllAuxiliaryTimers(alarmId)
    }

    fun cancelAllAuxiliaryTimers(alarmId: Int) {
        cancelPendingIntent(snoozeRequestCode(alarmId), AlarmReceiver.ACTION_FIRE_ALARM)
        cancelPendingIntent(alarmId * 10 + OFFSET_SNOOZE_SHOW)
        cancelPendingIntent(awakeDelayRequestCode(alarmId), AlarmReceiver.ACTION_AWAKE_STAGE1)
        cancelPendingIntent(awakeTimeoutRequestCode(alarmId), AlarmReceiver.ACTION_AWAKE_TIMEOUT)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmAudioService.awakeCheckNotificationId(alarmId))
        notificationManager.cancel(AlarmAudioService.FOREGROUND_NOTIFICATION_ID)

        AlarmAudioService.cancelAuxiliary(context, alarmId)
    }

    fun cancelSnooze(alarmId: Int) {
        cancelPendingIntent(alarmId * 10 + OFFSET_SNOOZE, AlarmReceiver.ACTION_FIRE_ALARM)
        cancelPendingIntent(alarmId * 10 + OFFSET_SNOOZE_SHOW)
    }

    fun cancelUpcoming(alarmId: Int) {
        cancelPendingIntent(alarmId * 10 + OFFSET_UPCOMING, AlarmReceiver.ACTION_UPCOMING_ALARM)
    }

    fun cancelAwakeCheck(alarmId: Int) {
        cancelPendingIntent(alarmId * 10 + OFFSET_AWAKE_DELAY, AlarmReceiver.ACTION_AWAKE_STAGE1)
        cancelPendingIntent(alarmId * 10 + OFFSET_AWAKE_TIMEOUT, AlarmReceiver.ACTION_AWAKE_TIMEOUT)
    }

    fun cancelAwakeTimeout(alarmId: Int) {
        cancelPendingIntent(alarmId * 10 + OFFSET_AWAKE_TIMEOUT, AlarmReceiver.ACTION_AWAKE_TIMEOUT)
    }

    private fun cancelPendingIntent(requestCode: Int, action: String? = null) {
        val intent = if (action != null) {
            Intent(context, AlarmReceiver::class.java).apply { this.action = action }
        } else {
            Intent(context, MainActivity::class.java)
        }
        val pi = if (action != null) {
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildRingPendingIntent(alarm: AlarmEntity): PendingIntent {
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
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, 0)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, alarm.snoozeLimit)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id * 10 + OFFSET_RING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePendingIntent(alarm: AlarmEntity, snoozeCount: Int): PendingIntent {
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
            alarm.id * 10 + OFFSET_SNOOZE,
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
            alarm.id * 10 + OFFSET_UPCOMING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildAwakeStage1PendingIntent(alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_AWAKE_STAGE1
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId * 10 + OFFSET_AWAKE_DELAY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildAwakeTimeoutPendingIntent(alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_AWAKE_TIMEOUT
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, true)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId * 10 + OFFSET_AWAKE_TIMEOUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"

        const val DEFAULT_SNOOZE_MINUTES = 5

        const val OFFSET_RING = 1
        const val OFFSET_SNOOZE = 2
        const val OFFSET_UPCOMING = 3
        const val OFFSET_AWAKE_DELAY = 4
        const val OFFSET_AWAKE_TIMEOUT = 5
        const val OFFSET_RING_SHOW = 6
        const val OFFSET_SNOOZE_SHOW = 7

        fun ringRequestCode(alarmId: Int): Int = alarmId * 10 + OFFSET_RING
        fun snoozeRequestCode(alarmId: Int): Int = alarmId * 10 + OFFSET_SNOOZE
        fun upcomingRequestCode(alarmId: Int): Int = alarmId * 10 + OFFSET_UPCOMING
        fun awakeDelayRequestCode(alarmId: Int): Int = alarmId * 10 + OFFSET_AWAKE_DELAY
        fun awakeTimeoutRequestCode(alarmId: Int): Int = alarmId * 10 + OFFSET_AWAKE_TIMEOUT

        @JvmStatic
        fun calculateTriggerTimeMillisStatic(
            hour24: Int,
            minute: Int,
            daysOfWeek: Set<Int> = emptySet(),
            weekInterval: Int = 1,
            anchorWeekStartEpochDay: Long = 0L,
            afterMillis: Long = System.currentTimeMillis()
        ): Long {
            val now = Instant.ofEpochMilli(afterMillis).atZone(ZoneId.systemDefault())
            val zone = now.zone

            if (daysOfWeek.isEmpty()) {
                var target = now.with(LocalTime.of(hour24.coerceIn(0, 23), minute.coerceIn(0, 59), 0, 0))
                if (!target.isAfter(now)) {
                    target = target.plusDays(1)
                }
                return target.toInstant().toEpochMilli()
            }

            val effectiveInterval = weekInterval.coerceAtLeast(1)
            val maxSearchDays = (7L * effectiveInterval + 14L)
            val candidateDate = now.toLocalDate()
            val targetTime = LocalTime.of(hour24.coerceIn(0, 23), minute.coerceIn(0, 59), 0, 0)

            val anchorDate = if (anchorWeekStartEpochDay > 0L) {
                LocalDate.ofEpochDay(anchorWeekStartEpochDay)
            } else {
                LocalDate.now()
            }
            val anchorMonday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

            for (dayOffset in 0..maxSearchDays) {
                val dateToCheck = candidateDate.plusDays(dayOffset)
                val candidateZdt = dateToCheck.atTime(targetTime).atZone(zone)

                if (!candidateZdt.isAfter(now)) {
                    continue
                }

                val calendarDay = if (dateToCheck.dayOfWeek == DayOfWeek.SUNDAY) {
                    Calendar.SUNDAY
                } else {
                    dateToCheck.dayOfWeek.value + 1
                }

                if (!daysOfWeek.contains(calendarDay)) {
                    continue
                }

                if (effectiveInterval <= 1) {
                    return candidateZdt.toInstant().toEpochMilli()
                }

                val candidateMonday = dateToCheck.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weeksBetween = ChronoUnit.WEEKS.between(anchorMonday, candidateMonday)
                val isActiveWeek = weeksBetween >= 0L && (weeksBetween % effectiveInterval.toLong() == 0L)

                if (isActiveWeek) {
                    return candidateZdt.toInstant().toEpochMilli()
                }
            }

            val fallback = now.plusDays(1).with(targetTime)
            return fallback.toInstant().toEpochMilli()
        }

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
