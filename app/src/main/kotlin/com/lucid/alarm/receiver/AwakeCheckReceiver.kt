package com.lucid.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.lucid.alarm.service.AlarmAudioService

class AwakeCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        if (alarmId < 0) return
        when (intent.action ?: ACTION_AWAKE_CHECK) {
            ACTION_AWAKE_CHECK -> {
                val forwardIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_AWAKE_STAGE1
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                }
                context.sendBroadcast(forwardIntent)
            }
            ACTION_MARK_AWAKE -> {
                val forwardIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_CONFIRM_AWAKE
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                }
                context.sendBroadcast(forwardIntent)
            }
            ACTION_AWAKE_EMERGENCY -> {
                val forwardIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_AWAKE_TIMEOUT
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                }
                context.sendBroadcast(forwardIntent)
            }
        }
    }

    companion object {
        const val ACTION_AWAKE_CHECK = "com.lucid.alarm.ACTION_AWAKE_CHECK"
        const val ACTION_MARK_AWAKE = "com.lucid.alarm.ACTION_MARK_AWAKE"
        const val ACTION_AWAKE_EMERGENCY = "com.lucid.alarm.ACTION_AWAKE_EMERGENCY"
        const val CHANNEL_ID = "lucid_alarm_awake_v2"
        private const val EXTRA_ALARM_ID = "extra_alarm_id"

        fun startAwakeCheck(context: Context, alarmId: Int, minutesFromNow: Int) {
            AlarmAudioService.scheduleSingleAwakeCheck(context, alarmId, minutesFromNow)
        }

        fun cancelAwakeCheck(context: Context, alarmId: Int) {
            AlarmAudioService.confirmAwake(context, alarmId)
        }
    }
}
