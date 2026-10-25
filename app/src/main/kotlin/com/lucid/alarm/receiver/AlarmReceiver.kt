package com.lucid.alarm.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lucid.alarm.AlarmRingingActivity
import com.lucid.alarm.MainActivity
import com.lucid.alarm.data.AppDatabase
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.service.AlarmAudioService
import com.lucid.alarm.ui.formatNotificationTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/**
 * Entry point from [android.app.AlarmManager].
 *
 * Firing path launches an explicit foreground [AlarmAudioService] which owns notification,
 * audio focus and lock-screen awakening. The receiver also reschedules repeating alarms so
 * weekly alarms keep firing.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ACTION_FIRE_ALARM
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty()
        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, -1)
        val volumeLevel = intent.getFloatExtra(EXTRA_ALARM_VOLUME, 1.0f)
        val ringtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val vibePreset = intent.getStringExtra(EXTRA_VIBE_PRESET)
        val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
        val snoozeLimit = intent.getIntExtra(EXTRA_SNOOZE_LIMIT, -1)

        when (action) {
            ACTION_FIRE_ALARM -> handleFireAlarm(
                context = context,
                alarmId = alarmId,
                label = label,
                hour = hour,
                minute = minute,
                volumeLevel = volumeLevel,
                ringtoneUri = ringtoneUri,
                vibrate = vibrate,
                vibePreset = vibePreset,
                snoozeCount = snoozeCount,
                snoozeLimit = snoozeLimit
            )

            ACTION_UPCOMING_ALARM -> postUpcomingAlarmNotification(
                context = context,
                alarmId = alarmId,
                label = label,
                hour = hour,
                minute = minute
            )

            ACTION_DISMISS_UPCOMING -> handleDismissUpcoming(context, alarmId)

            ACTION_CONFIRM_AWAKE -> handleConfirmAwake(context, alarmId)

            ACTION_AWAKE_CHECK_TIMEOUT -> handleAwakeCheckTimeout(context, alarmId)
        }
    }

    /** The user confirmed they are awake: retire the prompt and its loud fallback. */
    private fun handleConfirmAwake(context: Context, alarmId: Int) {
        if (alarmId < 0) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmAudioService.awakeCheckNotificationId(alarmId))
        val timeoutIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_AWAKE_CHECK_TIMEOUT
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmAudioService.AWAKE_TIMEOUT_REQUEST_BASE + alarmId,
            timeoutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Nobody confirmed the awake prompt in time: ring the original alarm again at maximum volume
     * with a fresh notification and bring the ringing screen forward. The full-volume streak only
     * ends when the user dismisses this re-ring (a new awake prompt may follow).
     */
    private fun handleAwakeCheckTimeout(context: Context, alarmId: Int) {
        if (alarmId < 0) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(AlarmAudioService.awakeCheckNotificationId(alarmId))

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val alarm = AppDatabase.getDatabase(context).alarmDao().getAlarmById(alarmId)
                val is24Hour = runBlocking {
                    SettingsDataStore(context).timeFormatFlow.first().is24Hour
                }
                val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
                    action = AlarmAudioService.ACTION_START_ALARM
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, alarm?.label.orEmpty())
                    putExtra(EXTRA_ALARM_HOUR, alarm?.hour ?: -1)
                    putExtra(EXTRA_ALARM_MINUTE, alarm?.minute ?: -1)
                    putExtra(EXTRA_ALARM_VOLUME, 1.0f)
                    putExtra(EXTRA_RINGTONE_URI, alarm?.soundUri)
                    putExtra(EXTRA_VIBRATE, alarm?.vibrationEnabled ?: true)
                    putExtra(EXTRA_VIBE_PRESET, alarm?.vibrationPattern)
                    putExtra(EXTRA_FORCE_MAX_VOLUME, true)
                    putExtra(EXTRA_ALARM_IS_24_HOUR, is24Hour)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Awake-check re-ring rejected; posting fallback", e)
                    postRingingFallbackNotification(context, serviceIntent)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Awake-check re-ring denied; posting fallback", e)
                    postRingingFallbackNotification(context, serviceIntent)
                }

                val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, alarm?.label.orEmpty())
                    putExtra(EXTRA_ALARM_HOUR, alarm?.hour ?: -1)
                    putExtra(EXTRA_ALARM_MINUTE, alarm?.minute ?: -1)
                    putExtra(EXTRA_FORCE_MAX_VOLUME, true)
                }
                try {
                    context.startActivity(ringingIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to surface ringing screen for awake-check timeout", e)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleFireAlarm(
        context: Context,
        alarmId: Int,
        label: String,
        hour: Int,
        minute: Int,
        volumeLevel: Float,
        ringtoneUri: String?,
        vibrate: Boolean,
        vibePreset: String?,
        snoozeCount: Int,
        snoozeLimit: Int
    ) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LucidAlarm::AlarmReceiverWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(UPCOMING_NOTIFICATION_BASE_ID + alarmId)

            val is24Hour = runBlocking {
                SettingsDataStore(context).timeFormatFlow.first().is24Hour
            }
            val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
                this.action = AlarmAudioService.ACTION_START_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_ALARM_LABEL, label)
                putExtra(EXTRA_ALARM_HOUR, hour)
                putExtra(EXTRA_ALARM_MINUTE, minute)
                putExtra(EXTRA_ALARM_VOLUME, volumeLevel)
                putExtra(EXTRA_RINGTONE_URI, ringtoneUri)
                putExtra(EXTRA_VIBRATE, vibrate)
                putExtra(EXTRA_VIBE_PRESET, vibePreset)
                putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
                putExtra(EXTRA_SNOOZE_LIMIT, snoozeLimit)
                putExtra(EXTRA_ALARM_IS_24_HOUR, is24Hour)
            }
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: IllegalStateException) {
                // API 31+ rejects background foreground-service starts (Doze/OEM policies) and
                // throws ForegroundServiceStartNotAllowedException. Never crash the receiver:
                // surface a fallback notification whose tap opens the ringing screen, which can
                // legally start the service once it is visible.
                Log.e(TAG, "Foreground service start rejected; posting fallback notification", e)
                postRingingFallbackNotification(context, serviceIntent)
            } catch (e: SecurityException) {
                Log.e(TAG, "Foreground service start denied; posting fallback notification", e)
                postRingingFallbackNotification(context, serviceIntent)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }

        // Reschedule repeating alarms off the main thread without blocking onReceive.
        if (alarmId == -1) return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val alarmDao = db.alarmDao()
                val alarm = alarmDao.getAlarmById(alarmId) ?: return@launch
                val scheduler = AlarmScheduler(context)
                if (alarm.daysOfWeek.isEmpty()) {
                    // One-shot alarm: disable it after it fires.
                    alarmDao.update(alarm.copy(isEnabled = false))
                    scheduler.cancelAlarm(alarmId)
                } else {
                    val settings = SettingsDataStore(context)
                    val leadMinutes = effectiveUpcomingLead(alarm, settings)
                    scheduler.scheduleAlarm(alarm, leadMinutes)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Last-resort notification for when the OS refuses the foreground-service start. Tapping it
     * opens [AlarmRingingActivity], which is allowed to start the service from the foreground and
     * sees [EXTRA_ENSURE_RINGING] so it knows to do so.
     */
    private fun postRingingFallbackNotification(context: Context, serviceIntent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                AlarmAudioService.ALARM_CHANNEL_ID,
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm clock ring"
                setShowBadge(true)
                enableVibration(true)
            }
        )

        val alarmId = serviceIntent.getIntExtra(EXTRA_ALARM_ID, -1)
        val label = serviceIntent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty()
        val hour = serviceIntent.getIntExtra(EXTRA_ALARM_HOUR, -1)
        val minute = serviceIntent.getIntExtra(EXTRA_ALARM_MINUTE, -1)

        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ENSURE_RINGING, true)
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
        }
        val ringingPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 40_000,
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val displayHour = hour.takeIf { it in 0..23 } ?: now.get(Calendar.HOUR_OF_DAY)
        val displayMinute = minute.takeIf { it in 0..59 } ?: now.get(Calendar.MINUTE)
        val is24Hour = serviceIntent.getBooleanExtra(
            EXTRA_ALARM_IS_24_HOUR,
            DateFormat.is24HourFormat(context)
        )
        val formattedTime = formatNotificationTime(displayHour, displayMinute, is24Hour)

        val notification = NotificationCompat.Builder(context, AlarmAudioService.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText(formattedTime)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(ringingPendingIntent, true)
            .setContentIntent(ringingPendingIntent)
            .build()

        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            notificationManager.notify(
                AlarmAudioService.FOREGROUND_NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun handleDismissUpcoming(context: Context, alarmId: Int) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(UPCOMING_NOTIFICATION_BASE_ID + alarmId)

                val scheduler = AlarmScheduler(context)
                scheduler.cancelAlarm(alarmId)

                if (alarmId == -1) return@launch
                val db = AppDatabase.getDatabase(context)
                val alarmDao = db.alarmDao()
                val alarm = alarmDao.getAlarmById(alarmId) ?: return@launch

                if (alarm.daysOfWeek.isEmpty()) {
                    // One-shot: skip today entirely.
                    alarmDao.update(alarm.copy(isEnabled = false))
                } else {
                    // Repeating: schedule the next valid weekday occurrence.
                    val settings = SettingsDataStore(context)
                    val leadMinutes = effectiveUpcomingLead(alarm, settings)
                    val next = nextValidTrigger(alarm.hour, alarm.minute, alarm.daysOfWeek)
                    scheduler.scheduleAlarm(alarm, leadMinutes)
                    // next is verified for logging/consistency only.
                    android.util.Log.d(TAG, "Rescheduled repeating alarm $alarmId for $next")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Pre-alarm lead for [alarm]: 0 when neither the alarm nor the inherited global switch wants an
     * upcoming notification, the alarm's own lead when it defines one, otherwise the global lead.
     */
    private suspend fun effectiveUpcomingLead(
        alarm: com.lucid.alarm.data.AlarmEntity,
        settings: SettingsDataStore
    ): Int {
        val globalEnabled = settings.upcomingAlarmEnabledFlow.first()
        if (!alarm.effectiveUpcomingNotification(globalEnabled)) return 0
        if (alarm.upcomingNotificationLeadMinutes > 0) {
            return alarm.upcomingNotificationLeadMinutes
        }
        return settings.rawUpcomingAlarmMinutesFlow.first()
    }

    private fun nextValidTrigger(hour: Int, minute: Int, days: Set<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        var guard = 0
        while (guard < 8 && !days.contains(target.get(Calendar.DAY_OF_WEEK))) {
            target.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
        return target.timeInMillis
    }

    private fun postUpcomingAlarmNotification(
        context: Context,
        alarmId: Int,
        label: String,
        hour: Int,
        minute: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (hour !in 0..23 || minute !in 0..59) {
            Log.w(TAG, "Skipping upcoming notification for invalid time $hour:$minute")
            return
        }

        val channel = NotificationChannel(
            UPCOMING_CHANNEL_ID,
            "Upcoming Alarms",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Heads-up notification before an alarm rings"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_UPCOMING
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 300_000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 400_000,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val displayHour = hour.takeIf { it in 0..23 } ?: now.get(Calendar.HOUR_OF_DAY)
        val displayMinute = minute.takeIf { it in 0..59 } ?: now.get(Calendar.MINUTE)
        val is24Hour = runBlocking {
            SettingsDataStore(context).timeFormatFlow.first().is24Hour
        }
        val removeLeadingZero = runBlocking {
            SettingsDataStore(context).removeLeadingZeroFlow.first()
        }
        val formattedTime = formatNotificationTime(
            displayHour,
            displayMinute,
            is24Hour,
            removeLeadingZero
        )
        val contentTitle = if (label.isNotBlank()) "Upcoming Alarm: $label" else "Upcoming Alarm"
        val contentText = "Scheduled for $formattedTime"

        val notification = NotificationCompat.Builder(context, UPCOMING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss Now",
                dismissPendingIntent
            )
            .build()

        notificationManager.notify(UPCOMING_NOTIFICATION_BASE_ID + alarmId, notification)
    }

    companion object {
        private const val TAG = "AlarmReceiver"

        const val ACTION_FIRE_ALARM = "com.lucid.alarm.ACTION_FIRE_ALARM"
        const val ACTION_UPCOMING_ALARM = "com.lucid.alarm.ACTION_UPCOMING_ALARM"
        const val ACTION_DISMISS_UPCOMING = "com.lucid.alarm.ACTION_DISMISS_UPCOMING"
        const val ACTION_CONFIRM_AWAKE = "com.lucid.alarm.ACTION_CONFIRM_AWAKE"
        const val ACTION_AWAKE_CHECK_TIMEOUT = "com.lucid.alarm.ACTION_AWAKE_CHECK_TIMEOUT"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
        const val EXTRA_ALARM_VOLUME = "extra_alarm_volume"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_VIBE_PRESET = "extra_vibe_preset"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SNOOZE_COUNT = "extra_snooze_count"
        const val EXTRA_SNOOZE_LIMIT = "extra_snooze_limit"
        const val EXTRA_FORCE_MAX_VOLUME = "extra_force_max_volume"
        const val EXTRA_ALARM_IS_24_HOUR = "extra_alarm_is_24_hour"
        const val EXTRA_ENSURE_RINGING = "com.lucid.alarm.extra.EXTRA_ENSURE_RINGING"

        const val UPCOMING_CHANNEL_ID = "lucid_upcoming_alarm_channel"
        const val UPCOMING_NOTIFICATION_BASE_ID = 50_000
    }
}
