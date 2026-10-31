package com.lucid.alarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import java.util.Collections

abstract class BaseMultiTimerSlotService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    abstract val slotIndex: Int

    override fun onCreate() {
        super.onCreate()
        activeSlots.add(slotIndex)
    }

    override fun onDestroy() {
        activeSlots.remove(slotIndex)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            activeSlots.remove(slotIndex)
            stopSelf()
            return START_NOT_STICKY
        }
        val action = intent.action
        if (action == ACTION_STOP_SLOT) {
            activeSlots.remove(slotIndex)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        activeSlots.add(slotIndex)
        val notifId = intent.getIntExtra(EXTRA_SLOT_NOTIF_ID, 0)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SLOT_NOTIFICATION, Notification::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SLOT_NOTIFICATION)
        }
        if (notifId != 0 && notification != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(notifId, notification)
                }
                NotificationRateLimiter.markPostedFromMainThread()
            } catch (_: Exception) {
                try {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(notifId, notification)
                    NotificationRateLimiter.markPostedFromMainThread()
                } catch (_: Exception) {}
            }
        } else {
            activeSlots.remove(slotIndex)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_START_SLOT = "com.lucid.alarm.service.ACTION_START_SLOT"
        const val ACTION_STOP_SLOT = "com.lucid.alarm.service.ACTION_STOP_SLOT"
        const val EXTRA_SLOT_NOTIF_ID = "extra_slot_notif_id"
        const val EXTRA_SLOT_NOTIFICATION = "extra_slot_notification"

        private val activeSlots = Collections.synchronizedSet(mutableSetOf<Int>())

        fun isSlotRunning(slotIndex: Int): Boolean = slotIndex in activeSlots

        private val slotClasses = arrayOf(
            MultiTimerSlot1Service::class.java,
            MultiTimerSlot2Service::class.java,
            MultiTimerSlot3Service::class.java,
            MultiTimerSlot4Service::class.java,
            MultiTimerSlot5Service::class.java,
            MultiTimerSlot6Service::class.java,
            MultiTimerSlot7Service::class.java
        )

        val MAX_SLOTS = slotClasses.size

        fun startSlot(context: Context, slotIndex: Int, notifId: Int, notification: Notification) {
            if (slotIndex !in slotClasses.indices) return
            activeSlots.add(slotIndex)
            val intent = Intent(context, slotClasses[slotIndex]).apply {
                action = ACTION_START_SLOT
                putExtra(EXTRA_SLOT_NOTIF_ID, notifId)
                putExtra(EXTRA_SLOT_NOTIFICATION, notification)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {}
        }

        fun stopSlot(context: Context, slotIndex: Int) {
            if (slotIndex !in slotClasses.indices) return
            activeSlots.remove(slotIndex)
            val intent = Intent(context, slotClasses[slotIndex]).apply {
                action = ACTION_STOP_SLOT
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopAllSlots(context: Context) {
            activeSlots.clear()
            for (i in slotClasses.indices) {
                stopSlot(context, i)
            }
        }
    }
}

class MultiTimerSlot1Service : BaseMultiTimerSlotService() { override val slotIndex = 0 }
class MultiTimerSlot2Service : BaseMultiTimerSlotService() { override val slotIndex = 1 }
class MultiTimerSlot3Service : BaseMultiTimerSlotService() { override val slotIndex = 2 }
class MultiTimerSlot4Service : BaseMultiTimerSlotService() { override val slotIndex = 3 }
class MultiTimerSlot5Service : BaseMultiTimerSlotService() { override val slotIndex = 4 }
class MultiTimerSlot6Service : BaseMultiTimerSlotService() { override val slotIndex = 5 }
class MultiTimerSlot7Service : BaseMultiTimerSlotService() { override val slotIndex = 6 }
