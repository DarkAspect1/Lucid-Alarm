package com.lucid.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.ui.NowBarFormatting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that owns the running Timer.
 *
 * The service is the single source of truth for the countdown: it ticks, publishes the remaining
 * seconds through [TimerStopwatchManager] and renders the ongoing "Now Bar" notification.
 *
 * When the countdown reaches zero the service enters an explicit **ringing** state: it plays the
 * configured timer ringtone (looping) plus the configured vibration pattern and replaces the Now
 * Bar with a high-importance alert that can always be stopped, either through its "Dismiss" action,
 * its "+1 min" action or by swiping it away.
 *
 * It is deliberately **not** a media service: there is no `MediaSession`, no `MediaStyle` and no
 * transport controls, so Android/One UI can never present the countdown as a music or video
 * playback.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private var isForeground = false

    private lateinit var nowBar: NowBarNotification
    private lateinit var settings: SettingsDataStore

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private val vibrator: Vibrator? by lazy { resolveVibrator() }

    /**
     * Last known appearance, owned by *this* service.
     *
     * The accent is refreshed from the persisted preference on every tick instead of living in a
     * shared/static holder, so a simultaneously running Stopwatch cannot overwrite it.
     */
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
            getString(R.string.now_bar_channel_timer),
            getString(R.string.now_bar_channel_description)
        )
        ensureRingingChannel()
        // Seed the appearance early. The first notification may briefly use the default accent and
        // is corrected by the first tick a moment later.
        scope.launch { preferences = settings.currentNowBarPreferences() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        // Dismissing the expired timer is always the first thing that must happen: stop the sound
        // immediately and never re-post anything.
        if (action == ACTION_DISMISS_TIMER) {
            dismissRinging()
            return START_NOT_STICKY
        }

        // Promote to the foreground *immediately*, before any work, so the ongoing notification
        // (and therefore the Now Bar / Live Update) exists from the very first moment.
        ensureForeground()

        when (action) {
            ACTION_START -> {
                stopRingAudio()
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0).coerceAtLeast(0)
                startTimer(seconds)
            }

            ACTION_PAUSE -> pauseTimer()

            ACTION_RESUME -> resumeTimer()

            ACTION_ADJUST -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_SECONDS, 0)
                if (TimerStopwatchManager.timer.value.ringing) {
                    extendExpiredTimer(delta)
                } else {
                    adjustTimer(delta)
                }
            }

            ACTION_ADD_ONE_MINUTE -> extendExpiredTimer(60)

            ACTION_CANCEL -> {
                stopRingAudio()
                TimerStopwatchManager.setTimer(
                    running = false,
                    totalSeconds = 0,
                    secondsLeft = 0,
                    endRealtime = 0L
                )
            }

            else -> {
                // Restarted by the system without a usable intent: keep an existing session alive,
                // otherwise shut down instead of posting a stale notification.
                if (!TimerStopwatchManager.timer.value.hasSession) {
                    shutdown()
                    return START_NOT_STICKY
                }
            }
        }

        if (!TimerStopwatchManager.timer.value.hasSession) {
            shutdown()
            return START_NOT_STICKY
        }

        // While ringing, the high-importance alert is already posted and the ticker is not needed.
        if (TimerStopwatchManager.timer.value.ringing) return START_STICKY

        // Paint the real state right away (the notification above used the pre-action snapshot)
        // and keep it refreshed once per second for as long as the session lives.
        startTicker()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        stopRingAudio()
        scope.cancel()
        super.onDestroy()
    }

    private fun startTimer(seconds: Int) {
        val safe = seconds.coerceAtLeast(0)
        if (safe <= 0) return
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = safe,
            secondsLeft = safe,
            endRealtime = SystemClock.elapsedRealtime() + safe * 1000L
        )
        // Replace a still-visible expired alert with the fresh countdown immediately.
        promoteToForeground(buildNotification(preferences))
    }

    private fun pauseTimer() {
        val state = TimerStopwatchManager.timer.value
        if (!state.hasSession) return
        val remaining = if (state.running) {
            ((state.endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L) + 999L) / 1000L
        } else {
            state.secondsLeft.toLong()
        }
        TimerStopwatchManager.setTimer(
            running = false,
            totalSeconds = state.totalSeconds,
            secondsLeft = remaining.toInt(),
            endRealtime = 0L
        )
    }

    private fun resumeTimer() {
        val state = TimerStopwatchManager.timer.value
        if (state.secondsLeft <= 0) return
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = state.totalSeconds,
            secondsLeft = state.secondsLeft,
            endRealtime = SystemClock.elapsedRealtime() + state.secondsLeft * 1000L
        )
    }

    private fun adjustTimer(deltaSeconds: Int) {
        val state = TimerStopwatchManager.timer.value
        if (deltaSeconds == 0) return
        if (!state.hasSession) {
            // Nothing is running yet: arm a paused countdown so the Now Bar shows the new session.
            val armed = deltaSeconds.coerceAtLeast(0)
            if (armed == 0) return
            TimerStopwatchManager.setTimer(
                running = false,
                totalSeconds = armed,
                secondsLeft = armed,
                endRealtime = 0L
            )
            return
        }
        val newLeft = (state.secondsLeft + deltaSeconds).coerceAtLeast(0)
        val newTotal = (state.totalSeconds + deltaSeconds).coerceAtLeast(0)
        TimerStopwatchManager.setTimer(
            running = state.running,
            totalSeconds = newTotal,
            secondsLeft = newLeft,
            endRealtime = if (state.running) {
                SystemClock.elapsedRealtime() + newLeft * 1000L
            } else {
                0L
            }
        )
    }

    /**
     * Turns a ringing timer into a fresh countdown of [deltaSeconds] (the "+1 min" action).
     *
     * The alert is silenced immediately; the ticker then resumes so the notification goes back to
     * its normal ongoing state.
     */
    private fun extendExpiredTimer(deltaSeconds: Int) {
        stopRingAudio()
        val state = TimerStopwatchManager.timer.value
        val added = deltaSeconds.coerceAtLeast(1)
        val newLeft = if (state.ringing) added else (state.secondsLeft + added).coerceAtLeast(1)
        val newTotal = maxOf(state.totalSeconds, newLeft)
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = newTotal,
            secondsLeft = newLeft,
            endRealtime = SystemClock.elapsedRealtime() + newLeft * 1000L
        )
        // Re-post on the low-importance ongoing channel so the alert is replaced by the countdown.
        promoteToForeground(buildNotification(preferences))
        startTicker()
    }

    /**
     * Drives the countdown and the ongoing notification.
     *
     * The loop refreshes the notification once per second, so the readout, subtitle, accent and
     * One UI extras stay live.
     */
    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                if (!tick()) return@launch
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    /** @return false when the session ended (or started ringing) and the ticker should stop. */
    private suspend fun tick(): Boolean {
        val state = TimerStopwatchManager.timer.value
        if (!state.hasSession) return false
        if (state.ringing) return false

        if (state.running) {
            val remainingMillis = state.endRealtime - SystemClock.elapsedRealtime()
            val secondsLeft = if (remainingMillis <= 0L) {
                0
            } else {
                ((remainingMillis + 999L) / 1000L).toInt()
            }
            TimerStopwatchManager.updateTimerSecondsLeft(secondsLeft)
            if (secondsLeft <= 0) {
                enterRinging()
                return false
            }
        }

        postNotification()
        return true
    }

    private suspend fun postNotification() {
        if (!TimerStopwatchManager.timer.value.hasSession) return
        preferences = settings.currentNowBarPreferences()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, buildNotification(preferences))
        } catch (e: Exception) {
            Log.e(TAG, "Unable to update the ongoing notification", e)
        }
    }

    /**
     * Switches the session to the ringing state: publishes it, starts the alert and promotes the
     * high-importance "Time's up" notification (still with a Dismiss and a "+1 min" action).
     */
    private suspend fun enterRinging() {
        TimerStopwatchManager.markTimerRinging()
        preferences = settings.currentNowBarPreferences()
        startRingAudio()
        withContext(Dispatchers.Main) { postRingingNotification() }
    }

    private fun postRingingNotification() {
        val state = TimerStopwatchManager.timer.value
        val large = nowBar.largeIcon(NowBarNotification.Kind.TIMER, preferences.accentArgb, preferences.isDark)
        val timeText = NowBarFormatting.timerTime(0)
        val subtitle = "Time's up! (${NowBarFormatting.durationShort(state.totalSeconds * 1000L)})"

        val builder = NotificationCompat.Builder(this, RING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle(timeText)
            .setContentText(subtitle)
            .setLargeIcon(large)
            .setContentIntent(nowBar.openAppPendingIntent(NowBarNotification.INITIAL_TAB_TIMER))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(preferences.accentArgb)
            .setColorized(true)
            .addAction(
                buildNotificationAction(
                    this,
                    R.drawable.ic_action_stop,
                    "Dismiss",
                    nowBar.actionPendingIntent(
                        ACTION_DISMISS_TIMER,
                        NowBarNotification.TARGET_TIMER,
                        RING_DISMISS_REQUEST
                    )
                )
            )
            .addAction(
                buildNotificationAction(
                    this,
                    R.drawable.ic_action_add,
                    "+1 min",
                    nowBar.actionPendingIntent(
                        ACTION_ADD_ONE_MINUTE,
                        NowBarNotification.TARGET_TIMER,
                        RING_ADD_REQUEST
                    )
                )
            )
            .setDeleteIntent(
                nowBar.actionPendingIntent(
                    ACTION_DISMISS_TIMER,
                    NowBarNotification.TARGET_TIMER,
                    RING_DELETE_REQUEST
                )
            )

        builder.addExtras(
            buildNowBarExtras(timeText, subtitle, preferences.accentArgb, large)
        )

        promoteToForeground(builder.build())
    }

    /**
     * Stops the alert and ends the session.
     *
     * Called by the "Dismiss" action, by the swipe-to-dismiss delete intent and by the in-app
     * "Dismiss" button.
     */
    private fun dismissRinging() {
        stopRingAudio()
        TimerStopwatchManager.setTimer(
            running = false,
            totalSeconds = 0,
            secondsLeft = 0,
            endRealtime = 0L
        )
        tickerJob?.cancel()
        tickerJob = null
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to cancel the timer notification", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    private suspend fun startRingAudio() {
        val ringtoneUri = runCatching { settings.timerRingtoneUriFlow.first() }.getOrDefault("")
        val vibrationEnabled = runCatching {
            settings.timerVibrationEnabledFlow.first()
        }.getOrDefault(true)
        val vibrationPattern = runCatching {
            settings.timerVibrationPatternFlow.first()
        }.getOrDefault(TimerVibrationPattern.TICK_TICK)

        stopRingAudio()

        val uri: Uri? = if (ringtoneUri.isBlank()) {
            null
        } else {
            runCatching { Uri.parse(ringtoneUri) }.getOrNull()
        }
        val source = uri
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val player = MediaPlayer()
        var started = false
        try {
            player.setAudioAttributes(attributes)
            player.setDataSource(applicationContext, source)
            player.isLooping = true
            player.prepare()
            player.start()
            mediaPlayer = player
            started = true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to play the timer ringtone", e)
            runCatching { player.release() }
        }

        if (!started) {
            try {
                val fallback = RingtoneManager.getRingtone(applicationContext, source)
                fallback?.audioAttributes = attributes
                fallback?.play()
                ringtone = fallback
            } catch (e: Exception) {
                Log.e(TAG, "Unable to play the fallback ringtone", e)
            }
        }

        if (vibrationEnabled) {
            vibrate(vibrationPattern)
        }
    }

    private fun vibrate(pattern: TimerVibrationPattern) {
        val effect = VibrationEffect.createWaveform(pattern.waveform, 0)
        val vibrator = this.vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to vibrate for the expired timer", e)
        }
    }

    private fun stopRingAudio() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    private fun resolveVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun ensureRingingChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(RING_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                RING_CHANNEL_ID,
                getString(R.string.timer_ringing_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.timer_ringing_channel_description)
                setShowBadge(true)
                enableVibration(true)
                // The ringtone is played by this service, so the channel itself stays silent.
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun ensureForeground() {
        if (isForeground) return
        promoteToForeground(buildNotification(preferences))
    }

    private fun promoteToForeground(notification: Notification) {
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
            isForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to enter the foreground", e)
            stopSelf()
        }
    }

    private fun shutdown() {
        tickerJob?.cancel()
        tickerJob = null
        stopRingAudio()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    private fun buildNotification(preferences: SettingsDataStore.NowBarPreferences): Notification {
        val state = TimerStopwatchManager.timer.value
        val accent = preferences.accentArgb

        val timeText = NowBarFormatting.timerTime(state.secondsLeft)
        val subtitle = NowBarFormatting.timerSubtitle(
            totalSeconds = state.totalSeconds,
            secondsLeft = state.secondsLeft,
            running = state.running,
            is24Hour = preferences.is24Hour,
            removeLeadingZero = preferences.removeLeadingZero
        )

        val badgeIcon = nowBar.largeIcon(NowBarNotification.Kind.TIMER, accent, preferences.isDark)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle("Timer")
            .setContentText(timeText)
            .setLargeIcon(badgeIcon)
            .setContentIntent(nowBar.openAppPendingIntent(NowBarNotification.INITIAL_TAB_TIMER))
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
                    R.drawable.ic_action_close,
                    "Cancel",
                    nowBar.actionPendingIntent(
                        ACTION_CANCEL,
                        NowBarNotification.TARGET_TIMER,
                        NowBarNotification.LEFT_TIMER_REQUEST
                    )
                )
            )
            .addAction(
                buildNotificationAction(
                    this,
                    if (state.running) R.drawable.ic_action_pause else R.drawable.ic_action_play,
                    if (state.running) "Pause" else "Resume",
                    nowBar.actionPendingIntent(
                        if (state.running) ACTION_PAUSE else ACTION_RESUME,
                        NowBarNotification.TARGET_TIMER,
                        NowBarNotification.RIGHT_TIMER_REQUEST
                    )
                )
            )

        builder.addExtras(buildNowBarExtras(timeText, subtitle, accent, badgeIcon))
        return builder.build()
    }

    /**
     * Builds a notification action with a real icon.
     *
     * One UI's SystemUI drops any action whose [IconCompat] is null (i.e. one added with
     * `addAction(0, ...)`), so an icon resource is mandatory here.
     */
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

    /** Extras that promote the notification into the Now Bar and label its buttons. */
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
        // Must be 1 with actionPrimarySet = 0, otherwise One UI hides every Now Bar button.
        putInt("android.ongoingActivityNoti.actionType", 1)
        putInt("android.ongoingActivityNoti.actionPrimarySet", 0)
    }

    companion object {
        private const val TAG = "TimerService"

        const val NOTIFICATION_ID = 42001
        const val CHANNEL_ID = "lucid_timer_ongoing"
        const val RING_CHANNEL_ID = "lucid_timer_ringing"

        const val ACTION_START = "com.lucid.alarm.timer.START"
        const val ACTION_PAUSE = "com.lucid.alarm.timer.PAUSE"
        const val ACTION_RESUME = "com.lucid.alarm.timer.RESUME"
        const val ACTION_ADJUST = "com.lucid.alarm.timer.ADJUST"
        const val ACTION_CANCEL = "com.lucid.alarm.timer.CANCEL"
        const val ACTION_DISMISS_TIMER = "com.lucid.alarm.timer.DISMISS"
        const val ACTION_ADD_ONE_MINUTE = "com.lucid.alarm.timer.ADD_ONE_MINUTE"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_DELTA_SECONDS = "extra_delta_seconds"

        private const val TICK_INTERVAL_MS = 1_000L

        private const val RING_DISMISS_REQUEST = 9201
        private const val RING_ADD_REQUEST = 9202
        private const val RING_DELETE_REQUEST = 9203

        /** Starts (or restarts) a countdown of [seconds] seconds. */
        fun start(context: Context, seconds: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, seconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Pauses the running countdown. */
        fun pause(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_PAUSE }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Resumes a paused countdown. */
        fun resume(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_RESUME }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Extends (or shortens) the countdown by [deltaSeconds]. */
        fun adjust(context: Context, deltaSeconds: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_ADJUST
                putExtra(EXTRA_DELTA_SECONDS, deltaSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Cancels the countdown and dismisses the ongoing notification. */
        fun cancel(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_CANCEL }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Silences the expired timer and clears its notification. */
        fun dismiss(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_DISMISS_TIMER
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Silences the expired timer and restarts it with one extra minute. */
        fun addOneMinute(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_ADD_ONE_MINUTE
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
