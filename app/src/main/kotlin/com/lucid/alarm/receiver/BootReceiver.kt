package com.lucid.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lucid.alarm.AlarmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d("BootReceiver", "Boot completed. Rescheduling alarms.")
            
            val app = context.applicationContext as AlarmApplication
            val repository = app.repository
            
            CoroutineScope(Dispatchers.IO).launch {
                val activeAlarms = repository.getActiveAlarmsSync()
                for (alarm in activeAlarms) {
                    runCatching { AlarmScheduler.scheduleAlarm(context, alarm) }
                        .onFailure { e ->
                            Log.e(
                                "BootReceiver",
                                "Unable to reschedule alarm ${alarm.id} after boot",
                                e
                            )
                        }
                }
            }
        }
    }
}
