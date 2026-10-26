package com.lucid.alarm.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.ui.NowBarFormatting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StopwatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private lateinit var nowBar: NowBarNotification
    private lateinit var settings: SettingsDataStore

    @Volatile
    private var preferences = SettingsDataStore.NowBarPreferences(
        accentArgb = AccentColor.DEFAULT.argb.toInt(),
        is24Hour = false,
        removeLeadingZero = true
    )

    override fun onCreate() {
        super.onCreate()
        nowBar = NowBarNotification(this)
        settings = SettingsDataStore(applicationContext)
        nowBar.ensureChannel(
            CHANNEL_ID,
            getString(R.string.now_bar_channel_stopwatch),
            getString(R.string.now_bar_channel_stopwatch_description)
        )

        scope.launch { preferences = settings.currentNowBarPreferences() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        ensureForeground()

        when (intent?.action) {
            ACTION_START -> startStopwatch()

            ACTION_STOP -> stopStopwatch()

            ACTION_RESUME -> resumeStopwatch()

            ACTION_LAP -> recordLap()

            ACTION_CANCEL -> {
                TimerStopwatchManager.setStopwatch(
                    running = false,
                    startRealtime = 0L,
                    elapsedMillis = 0L,
                    laps = emptyList()
                )
            }

            else -> {
                if (!TimerStopwatchManager.stopwatch.value.hasSession) {
                    shutdown()
                    return START_NOT_STICKY
                }
            }
        }

        if (!TimerStopwatchManager.stopwatch.value.hasSession) {
            shutdown()
            return START_NOT_STICKY
        }

        startTicker()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startStopwatch() {
        val state = TimerStopwatchManager.stopwatch.value
        if (state.running) return
        val base = SystemClock.elapsedRealtime() - state.elapsedMillis
        TimerStopwatchManager.setStopwatch(
            running = true,
            startRealtime = base,
            elapsedMillis = state.elapsedMillis,
            laps = state.laps
        )
    }

    private fun stopStopwatch() {
        val state = TimerStopwatchManager.stopwatch.value
        if (!state.running) return
        val elapsed = state.liveMillis
        TimerStopwatchManager.setStopwatch(
            running = false,
            startRealtime = 0L,
            elapsedMillis = elapsed,
            laps = state.laps
        )
    }

    private fun resumeStopwatch() {
        val state = TimerStopwatchManager.stopwatch.value
        if (state.running || state.elapsedMillis <= 0L) return
        TimerStopwatchManager.setStopwatch(
            running = true,
            startRealtime = SystemClock.elapsedRealtime() - state.elapsedMillis,
            elapsedMillis = state.elapsedMillis,
            laps = state.laps
        )
    }

    private fun recordLap() {
        val state = TimerStopwatchManager.stopwatch.value
        if (!state.running) return
        TimerStopwatchManager.addStopwatchLap(state.liveMillis)
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                if (!tick()) return@launch
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private suspend fun tick(): Boolean {
        if (!TimerStopwatchManager.stopwatch.value.hasSession) return false
        postNotification()
        return true
    }

    private suspend fun postNotification() {
        if (!TimerStopwatchManager.stopwatch.value.hasSession) return
        preferences = settings.currentNowBarPreferences()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, buildNotification(preferences))
        } catch (e: Exception) {
            Log.e(TAG, "Unable to update the ongoing notification", e)
        }
    }

    private fun ensureForeground() {
        val notification = buildNotification(preferences)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to enter the foreground", e)
            stopSelf()
        }
    }

    private fun shutdown() {
        tickerJob?.cancel()
        tickerJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(preferences: SettingsDataStore.NowBarPreferences): Notification {
        val state = TimerStopwatchManager.stopwatch.value
        val accent = preferences.accentArgb

        val timeText = NowBarFormatting.stopwatchTime(state.liveMillis)
        val subtitle = NowBarFormatting.stopwatchSubtitle(
            lapCount = state.laps.size,
            latestLapSplit = state.latestLapSplit
        )

        val leftIcon: Int
        val leftLabel: String
        val leftAction: String
        val rightIcon: Int
        val rightLabel: String
        val rightAction: String
        if (state.running) {
            leftIcon = R.drawable.ic_action_lap
            leftLabel = "Lap"
            leftAction = ACTION_LAP
            rightIcon = R.drawable.ic_action_pause
            rightLabel = "Stop"
            rightAction = ACTION_STOP
        } else {
            leftIcon = R.drawable.ic_action_close
            leftLabel = "Cancel"
            leftAction = ACTION_CANCEL
            rightIcon = R.drawable.ic_action_play
            rightLabel = "Resume"
            rightAction = ACTION_RESUME
        }

        val badgeIcon = nowBar.largeIcon(NowBarNotification.Kind.STOPWATCH, accent, preferences.isDark)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle("Stopwatch")
            .setContentText(timeText)
            .setLargeIcon(badgeIcon)
            .setContentIntent(nowBar.openAppPendingIntent(NowBarNotification.INITIAL_TAB_STOPWATCH))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(accent)
            .setColorized(true)
            .addAction(
                buildNotificationAction(
                    this,
                    leftIcon,
                    leftLabel,
                    nowBar.actionPendingIntent(
                        leftAction,
                        NowBarNotification.TARGET_STOPWATCH,
                        NowBarNotification.LEFT_STOPWATCH_REQUEST
                    )
                )
            )
            .addAction(
                buildNotificationAction(
                    this,
                    rightIcon,
                    rightLabel,
                    nowBar.actionPendingIntent(
                        rightAction,
                        NowBarNotification.TARGET_STOPWATCH,
                        NowBarNotification.RIGHT_STOPWATCH_REQUEST
                    )
                )
            )

        builder.addExtras(buildNowBarExtras(timeText, subtitle, accent, badgeIcon))
        return builder.build()
    }

    private fun buildNotificationAction(
        context: Context,
        @DrawableRes iconRes: Int,
        title: String,
        pendingIntent: PendingIntent
    ): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, iconRes),
            title,
            pendingIntent
        )
            .setShowsUserInterface(false)
            .build()

    private fun buildNowBarExtras(
        formattedTime: String,
        subtitleText: String,
        accentColorInt: Int,
        badgeBitmap: Bitmap
    ): Bundle = Bundle().apply {
        putBoolean("android.requestPromotedOngoing", true)
        putString("android.shortCriticalText", formattedTime)
        putInt("android.ongoingActivityNoti.style", 1)
        putString("android.ongoingActivityNoti.primaryInfo", formattedTime)
        putString("android.ongoingActivityNoti.secondaryInfo", subtitleText)
        putString("android.ongoingActivityNoti.nowbarPrimaryInfo", formattedTime)
        putString("android.ongoingActivityNoti.nowbarSecondaryInfo", subtitleText)
        putInt("android.ongoingActivityNoti.chipBgColor", accentColorInt)
        putParcelable(
            "android.ongoingActivityNoti.chipIcon",
            Icon.createWithBitmap(badgeBitmap)
        )
        putString("android.ongoingActivityNoti.chipExpandedText", formattedTime)

        putInt("android.ongoingActivityNoti.actionType", 1)
        putInt("android.ongoingActivityNoti.actionPrimarySet", 0)
    }

    companion object {
        private const val TAG = "StopwatchService"

        const val NOTIFICATION_ID = 42002
        const val CHANNEL_ID = "lucid_stopwatch_ongoing"

        const val ACTION_START = "com.lucid.alarm.stopwatch.START"
        const val ACTION_STOP = "com.lucid.alarm.stopwatch.STOP"
        const val ACTION_RESUME = "com.lucid.alarm.stopwatch.RESUME"
        const val ACTION_LAP = "com.lucid.alarm.stopwatch.LAP"
        const val ACTION_CANCEL = "com.lucid.alarm.stopwatch.CANCEL"

        private const val TICK_INTERVAL_MS = 1_000L

        fun start(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply { action = ACTION_START }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply { action = ACTION_STOP }
            ContextCompat.startForegroundService(context, intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply { action = ACTION_RESUME }
            ContextCompat.startForegroundService(context, intent)
        }

        fun lap(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply { action = ACTION_LAP }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, StopwatchService::class.java).apply { action = ACTION_CANCEL }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
