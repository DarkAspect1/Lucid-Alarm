package com.lucid.alarm.receiver

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lucid.alarm.AlarmRingingActivity
import com.lucid.alarm.service.AlarmAudioService
import java.util.Calendar

class AwakeCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val minutes = intent.getIntExtra(EXTRA_DELAY_MINUTES, EMERGENCY_DELAY_MINUTES)
        when (intent.action ?: ACTION_AWAKE_CHECK) {
            ACTION_AWAKE_CHECK -> showAwakeCheck(context, alarmId, minutes)
            ACTION_MARK_AWAKE -> confirmAwake(context, alarmId)
            ACTION_AWAKE_EMERGENCY -> triggerEmergencyAlarm(context, alarmId)
        }
    }

    private fun showAwakeCheck(context: Context, alarmId: Int, minutesFromNow: Int) {
        ensureChannel(context)

        val markAwakeIntent = Intent(context, AwakeCheckReceiver::class.java).apply {
            action = ACTION_MARK_AWAKE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val markAwakePi = PendingIntent.getBroadcast(
            context,
            REQUEST_MARK_AWAKE_BASE + alarmId,
            markAwakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Are you awake?")
            .setContentText("Tap to confirm you're awake.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(markAwakePi)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "I'm Awake!",
                markAwakePi
            )
            .build()

        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(alarmId + NOTIFICATION_ID_BASE, notif)
        }

        scheduleEmergency(context, alarmId, minutesFromNow)
    }

    private fun confirmAwake(context: Context, alarmId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alarmId + NOTIFICATION_ID_BASE)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val existing = buildEmergencyPendingIntent(context, alarmId, create = false)
        if (existing != null) am.cancel(existing)
    }

    private fun triggerEmergencyAlarm(context: Context, alarmId: Int) {

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alarmId + NOTIFICATION_ID_BASE)

        val now = Calendar.getInstance()
        val serviceIntent = Intent(context, AlarmAudioService::class.java).apply {
            action = AlarmAudioService.ACTION_START_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "Are you awake?")
            putExtra(AlarmReceiver.EXTRA_ALARM_HOUR, now.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, now.get(Calendar.MINUTE))
            putExtra(AlarmReceiver.EXTRA_ALARM_VOLUME, 1.0f)
            putExtra(AlarmReceiver.EXTRA_FORCE_MAX_VOLUME, true)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, true)
        }
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
            .onFailure { e ->
                Log.e(TAG, "Emergency foreground service start rejected", e)
                postEmergencyFallbackNotification(context, alarmId)
            }
    }

    private fun postEmergencyFallbackNotification(context: Context, alarmId: Int) {
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

        val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmReceiver.EXTRA_ENSURE_RINGING, true)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, "Are you awake?")
            putExtra(AlarmReceiver.EXTRA_FORCE_MAX_VOLUME, true)
        }
        val ringingPendingIntent = PendingIntent.getActivity(
            context,
            alarmId + 40_000,
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AlarmAudioService.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Are you awake?")
            .setContentText("Tap to sound the alarm.")
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

    private fun scheduleEmergency(context: Context, alarmId: Int, minutesFromNow: Int) {
        val minutes = if (minutesFromNow > 0) minutesFromNow else EMERGENCY_DELAY_MINUTES
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val pi = buildEmergencyPendingIntent(context, alarmId, create = true) ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun buildEmergencyPendingIntent(
        context: Context,
        alarmId: Int,
        create: Boolean
    ): PendingIntent? {
        val intent = Intent(context, AwakeCheckReceiver::class.java).apply {
            action = ACTION_AWAKE_EMERGENCY
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val flags = if (create) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, REQUEST_EMERGENCY_BASE + alarmId, intent, flags)
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Awake Check",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Post-dismiss check confirming you are awake"
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "AwakeCheckReceiver"

        const val ACTION_AWAKE_CHECK = "com.lucid.alarm.ACTION_AWAKE_CHECK"
        const val ACTION_MARK_AWAKE = "com.lucid.alarm.ACTION_MARK_AWAKE"
        const val ACTION_AWAKE_EMERGENCY = "com.lucid.alarm.ACTION_AWAKE_EMERGENCY"
        const val CHANNEL_ID = "lucid_awake_check_channel"

        private const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val EXTRA_DELAY_MINUTES = "extra_delay_minutes"
        private const val NOTIFICATION_ID_BASE = 60_000
        private const val REQUEST_MARK_AWAKE_BASE = 50_000
        private const val REQUEST_EMERGENCY_BASE = 70_000

        const val EMERGENCY_DELAY_MINUTES = 3

        fun startAwakeCheck(context: Context, alarmId: Int, minutesFromNow: Int) {
            if (alarmId < 0) return
            val intent = Intent(context, AwakeCheckReceiver::class.java).apply {
                action = ACTION_AWAKE_CHECK
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_DELAY_MINUTES, minutesFromNow)
            }
            context.sendBroadcast(intent.setPackage(context.packageName))
        }

        fun cancelAwakeCheck(context: Context, alarmId: Int) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(alarmId + NOTIFICATION_ID_BASE)

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AwakeCheckReceiver::class.java).apply {
                action = ACTION_AWAKE_EMERGENCY
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_EMERGENCY_BASE + alarmId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
            am.cancel(pi)
        }
    }
}
