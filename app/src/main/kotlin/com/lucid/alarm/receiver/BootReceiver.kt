package com.lucid.alarm.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.data.prefs.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.TIME_SET",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
        if (action !in validActions) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Lucid:BootReceiverWakeLock"
        ).apply { setReferenceCounted(false) }
        wakeLock.acquire(30_000L)

        val pendingResult = goAsync()
        val app = context.applicationContext as AlarmApplication
        val repository = app.repository
        val settings = SettingsDataStore(context.applicationContext)
        val scheduler = AlarmScheduler(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val globalUpcomingEnabled = runCatching { settings.upcomingAlarmEnabledFlow.first() }.getOrDefault(false)
                val rawUpcomingMinutes = runCatching { settings.rawUpcomingAlarmMinutesFlow.first() }.getOrDefault(0)

                val activeAlarms = repository.getActiveAlarmsSync()
                for (alarm in activeAlarms) {
                    val lead = if (!alarm.effectiveUpcomingNotification(globalUpcomingEnabled)) {
                        0
                    } else if (alarm.upcomingNotificationLeadMinutes > 0) {
                        alarm.upcomingNotificationLeadMinutes
                    } else {
                        rawUpcomingMinutes
                    }
                    runCatching { scheduler.scheduleAlarm(alarm, lead) }
                        .onFailure { e ->
                            Log.e("BootReceiver", "Unable to reschedule alarm ${alarm.id} for $action", e)
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
}
