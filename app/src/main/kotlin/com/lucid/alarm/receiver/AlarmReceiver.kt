package com.lucid.alarm.receiver

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lucid.alarm.AlarmRingingActivity
import com.lucid.alarm.MainActivity
import com.lucid.alarm.data.AlarmEntity
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

            ACTION_AWAKE_STAGE1 -> handleAwakeStage1(context, alarmId)

            ACTION_CONFIRM_AWAKE -> handleConfirmAwake(context, alarmId)

            ACTION_AWAKE_TIMEOUT -> handleAwakeTimeout(context, alarmId)
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
            "Lucid:AlarmReceiverWakeLock"
        ).apply { setReferenceCounted(false) }
        wakeLock.acquire(15_000L)

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(UPCOMING_NOTIFICATION_BASE_ID + alarmId)

                val is24Hour = SettingsDataStore(context).timeFormatFlow.first().is24Hour
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
                } catch (e: Exception) {
                    Log.e(TAG, "Foreground service start rejected; posting fallback notification", e)
                    postRingingFallbackNotification(context, serviceIntent)
                }

                if (alarmId != -1) {
                    val db = AppDatabase.getDatabase(context)
                    val alarmDao = db.alarmDao()
                    val alarm = alarmDao.getAlarmById(alarmId)
                    if (alarm != null) {
                        val scheduler = AlarmScheduler(context)
                        if (alarm.daysOfWeek.isEmpty()) {
                            alarmDao.update(alarm.copy(isEnabled = false))
                            scheduler.cancelAlarm(alarmId)
                        } else {
                            val settings = SettingsDataStore(context)
                            val leadMinutes = effectiveUpcomingLead(alarm, settings)
                            scheduler.scheduleAlarm(alarm, leadMinutes)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    private fun handleAwakeStage1(context: Context, alarmId: Int) {
        if (alarmId < 0) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Lucid:AwakeStage1WakeLock"
        ).apply { setReferenceCounted(false) }
        wakeLock.acquire(15_000L)

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val alarm = db.alarmDao().getAlarmById(alarmId)
                val settings = SettingsDataStore(context)
                val delayMinutes = if (alarm != null && alarm.hasCustomAreYouAwake && alarm.areYouAwakeMinutes > 0) {
                    alarm.areYouAwakeMinutes.coerceIn(1, 15)
                } else {
                    settings.awakeCheckMinutesFlow.first().coerceIn(1, 15)
                }
                AlarmAudioService.scheduleSingleAwakeCheck(context, alarmId, delayMinutes)
            } finally {
                pendingResult.finish()
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    private fun handleConfirmAwake(context: Context, alarmId: Int) {
        if (alarmId < 0) return
        AlarmAudioService.confirmAwake(context, alarmId)
    }

    private fun handleAwakeTimeout(context: Context, alarmId: Int) {
        if (alarmId < 0) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Lucid:AwakeTimeoutWakeLock"
        ).apply { setReferenceCounted(false) }
        wakeLock.acquire(15_000L)

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                AlarmAudioService.removeAwakePromptNotification(context, alarmId)
                val alarm = AppDatabase.getDatabase(context).alarmDao().getAlarmById(alarmId)
                val is24Hour = SettingsDataStore(context).timeFormatFlow.first().is24Hour
                val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
                    action = AlarmAudioService.ACTION_START_ALARM
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, alarm?.label.orEmpty().ifBlank { "Are you awake?" })
                    putExtra(EXTRA_ALARM_HOUR, alarm?.hour ?: -1)
                    putExtra(EXTRA_ALARM_MINUTE, alarm?.minute ?: -1)
                    putExtra(EXTRA_ALARM_VOLUME, 1.0f)
                    putExtra(EXTRA_RINGTONE_URI, alarm?.soundUri)
                    putExtra(EXTRA_VIBRATE, alarm?.vibrationEnabled ?: true)
                    putExtra(EXTRA_VIBE_PRESET, alarm?.vibrationPattern)
                    putExtra(EXTRA_FORCE_MAX_VOLUME, true)
                    putExtra(EXTRA_ALARM_IS_24_HOUR, is24Hour)
                    putExtra(EXTRA_IS_AWAKE_CHECK_ACTIVE, true)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Awake-check re-ring rejected; posting fallback", e)
                    postRingingFallbackNotification(context, serviceIntent)
                }

                val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, alarm?.label.orEmpty().ifBlank { "Are you awake?" })
                    putExtra(EXTRA_ALARM_HOUR, alarm?.hour ?: -1)
                    putExtra(EXTRA_ALARM_MINUTE, alarm?.minute ?: -1)
                    putExtra(EXTRA_FORCE_MAX_VOLUME, true)
                    putExtra(EXTRA_IS_AWAKE_CHECK_ACTIVE, true)
                }
                try {
                    context.startActivity(ringingIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to surface ringing screen for awake-check timeout", e)
                }
            } finally {
                pendingResult.finish()
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
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
                    alarmDao.update(alarm.copy(isEnabled = false))
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

        AlarmAudioService.initSilentAlarmChannels(context)

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_UPCOMING
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId * 10 + AlarmScheduler.OFFSET_UPCOMING,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            alarmId * 10 + AlarmScheduler.OFFSET_RING_SHOW,
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

        val notification = NotificationCompat.Builder(context, AlarmAudioService.CHANNEL_UPCOMING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss Now",
                dismissPendingIntent
            )
            .build()

        notificationManager.notify(UPCOMING_NOTIFICATION_BASE_ID + alarmId, notification)
    }

    private fun postRingingFallbackNotification(context: Context, serviceIntent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        AlarmAudioService.initSilentAlarmChannels(context)

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
            alarmId * 10 + AlarmScheduler.OFFSET_RING,
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

        val notification = NotificationCompat.Builder(context, AlarmAudioService.CHANNEL_RINGING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifBlank { "Alarm" })
            .setContentText(formattedTime)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
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

    private suspend fun effectiveUpcomingLead(
        alarm: AlarmEntity,
        settings: SettingsDataStore
    ): Int {
        val globalEnabled = settings.upcomingAlarmEnabledFlow.first()
        if (!alarm.effectiveUpcomingNotification(globalEnabled)) return 0
        if (alarm.upcomingNotificationLeadMinutes > 0) {
            return alarm.upcomingNotificationLeadMinutes
        }
        return settings.rawUpcomingAlarmMinutesFlow.first()
    }

    companion object {
        private const val TAG = "AlarmReceiver"

        const val ACTION_FIRE_ALARM = "com.lucid.alarm.ACTION_FIRE_ALARM"
        const val ACTION_UPCOMING_ALARM = "com.lucid.alarm.ACTION_UPCOMING_ALARM"
        const val ACTION_DISMISS_UPCOMING = "com.lucid.alarm.ACTION_DISMISS_UPCOMING"
        const val ACTION_AWAKE_STAGE1 = "com.lucid.alarm.ACTION_AWAKE_STAGE1"
        const val ACTION_CONFIRM_AWAKE = "com.lucid.alarm.ACTION_CONFIRM_AWAKE"
        const val ACTION_AWAKE_TIMEOUT = "com.lucid.alarm.ACTION_AWAKE_TIMEOUT"

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
        const val EXTRA_IS_AWAKE_CHECK_ACTIVE = "extra_is_awake_check_active"
        const val EXTRA_ENSURE_RINGING = "com.lucid.alarm.extra.EXTRA_ENSURE_RINGING"

        const val UPCOMING_NOTIFICATION_BASE_ID = 50_000
    }
}
