package com.lucid.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.service.StopwatchService
import com.lucid.alarm.service.TimerService

/**
 * Routes the Now Bar notification actions.
 *
 * The notification body opens `MainActivity` directly (see
 * [NowBarNotification.openAppPendingIntent]); only the split actions are delivered here. Each one
 * is forwarded to the owning foreground service from the notification action context, which is an
 * allowlisted foreground start.
 */
class NowBarActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val serviceClass = when (intent.getStringExtra(NowBarNotification.EXTRA_TARGET_SERVICE)) {
            NowBarNotification.TARGET_TIMER -> TimerService::class.java
            NowBarNotification.TARGET_STOPWATCH -> StopwatchService::class.java
            else -> return
        }

        val forward = Intent(context, serviceClass).apply { this.action = action }
        ContextCompat.startForegroundService(context, forward)
    }
}
