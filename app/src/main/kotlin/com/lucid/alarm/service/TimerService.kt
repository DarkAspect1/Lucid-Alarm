package com.lucid.alarm.service

import android.app.AlarmManager
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
import android.os.PowerManager
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
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.R
import com.lucid.alarm.data.TimerEntity
import com.lucid.alarm.data.TimerRepository
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.data.prefs.TimerVibrationPolicy
import com.lucid.alarm.data.prefs.TimerVolumePolicy
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

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var uiTickerJob: Job? = null
    private var notifDispatcherJob: Job? = null

    private var isForeground = false
    private val postedSecondaryNotifIds = mutableSetOf<Int>()
    private val timerSlotAssignments = mutableMapOf<Long, Int>()
    private val lastDisplayedSecond = mutableMapOf<Long, Long>()
    private val lastPausedPostedTimerIds = mutableSetOf<Long>()

    private lateinit var nowBar: NowBarNotification
    private lateinit var settings: SettingsDataStore

    private val repository: TimerRepository by lazy {
        (applicationContext as AlarmApplication).timerRepository
    }

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private val vibrator: Vibrator? by lazy { resolveVibrator() }
    private var wakeLock: PowerManager.WakeLock? = null

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

        scope.launch { preferences = settings.currentNowBarPreferences() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val timerId = intent?.getLongExtra(EXTRA_TIMER_ID, 0L) ?: 0L

        if (action == ACTION_DISMISS_TIMER) {
            dismissRinging()
            if (timerId != 0L) {
                TimerStopwatchManager.clearTimerRinging(timerId)
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_TIMER_EXPIRED) {
            scope.launch { enterRinging() }
            return START_STICKY
        }

        ensureForeground()

        when (action) {
            ACTION_START -> {
                stopRingAudio()
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0).coerceAtLeast(0)
                startTimer(seconds)
            }

            ACTION_START_MULTI -> {
                stopRingAudio()
                val label = intent.getStringExtra(EXTRA_LABEL) ?: ""
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0).coerceAtLeast(0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, seconds).coerceAtLeast(seconds)
                if (timerId != 0L && seconds <= 0) {
                    resumeMultiTimerSession(timerId)
                } else {
                    startMultiTimerSession(timerId, label, seconds, totalSeconds)
                }
            }

            ACTION_FOCUS_TIMER -> {
                val label = intent.getStringExtra(EXTRA_LABEL) ?: ""
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0).coerceAtLeast(0)
                val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, seconds).coerceAtLeast(seconds)
                val remMillis = intent.getLongExtra(EXTRA_REMAINING_MILLIS, 0L).coerceAtLeast(0L)
                val endRealtime = intent.getLongExtra(EXTRA_END_REALTIME, 0L)
                val endTimestamp = intent.getLongExtra(EXTRA_END_TIMESTAMP, 0L)
                focusTimerSession(timerId, label, seconds, totalSeconds, remMillis, endRealtime, endTimestamp)
            }

            ACTION_PAUSE -> pauseTimer()

            ACTION_PAUSE_MULTI -> pauseMultiTimerSession(timerId)

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
                cancelTimerAlarm()
                stopRingAudio()
                val state = TimerStopwatchManager.timer.value
                lastDisplayedSecond.remove(state.timerId)
                lastPausedPostedTimerIds.remove(state.timerId)
                TimerStopwatchManager.setTimer(
                    running = false,
                    totalSeconds = 0,
                    secondsLeft = 0,
                    endRealtime = 0L,
                    endTimestamp = 0L
                )
                checkRemainingTimersOrShutdown()
            }

            ACTION_CANCEL_MULTI -> {
                cancelMultiTimerSession(timerId)
            }

            ACTION_SYNC_MULTI -> {
                startLoops()
            }

            else -> {
                if (!TimerStopwatchManager.timer.value.hasSession) {
                    checkRemainingTimersOrShutdown()
                    return START_NOT_STICKY
                }
            }
        }

        if (TimerStopwatchManager.timer.value.ringing) return START_STICKY

        startLoops()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLoops()
        cancelTimerAlarm()
        stopRingAudio()
        timerSlotAssignments.clear()
        try {
            BaseMultiTimerSlotService.stopAllSlots(this)
        } catch (_: Exception) {}
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            for (id in postedSecondaryNotifIds) {
                try { manager.cancel(id) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        postedSecondaryNotifIds.clear()
        lastDisplayedSecond.clear()
        lastPausedPostedTimerIds.clear()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startTimer(seconds: Int) {
        val safe = seconds.coerceAtLeast(0)
        if (safe <= 0) return
        val end = SystemClock.elapsedRealtime() + safe * 1000L
        val endTs = System.currentTimeMillis() + safe * 1000L
        lastPausedPostedTimerIds.remove(0L)
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = safe,
            secondsLeft = safe,
            endRealtime = end,
            timerId = 0L,
            label = "",
            endTimestamp = endTs
        )
        scheduleTimerAlarm(end)

        promoteToForeground(buildNotification(preferences))
        startLoops()
    }

    private fun startMultiTimerSession(timerId: Long, label: String, seconds: Int, totalSeconds: Int) {
        val safe = seconds.coerceAtLeast(0)
        if (safe <= 0) return
        val end = SystemClock.elapsedRealtime() + safe * 1000L
        val endTs = System.currentTimeMillis() + safe * 1000L
        val prevSlot = timerSlotAssignments.remove(timerId)
        if (prevSlot != null) {
            BaseMultiTimerSlotService.stopSlot(this, prevSlot)
        }
        val oldSecondaryNotifId = (NOTIFICATION_BASE_MULTI_ID + timerId).toInt()
        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(oldSecondaryNotifId)
        } catch (_: Exception) {}
        postedSecondaryNotifIds.remove(oldSecondaryNotifId)
        lastPausedPostedTimerIds.remove(timerId)
        val prevPrimaryId = TimerStopwatchManager.timer.value.timerId
        if (prevPrimaryId != 0L && prevPrimaryId != timerId) {
            lastPausedPostedTimerIds.remove(prevPrimaryId)
            lastDisplayedSecond.remove(prevPrimaryId)
        }
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = totalSeconds,
            secondsLeft = safe,
            endRealtime = end,
            timerId = timerId,
            label = label,
            endTimestamp = endTs
        )
        if (timerId != 0L) {
            scope.launch(Dispatchers.IO) {
                repository.updateTimerState(
                    id = timerId,
                    remainingMillis = safe * 1000L,
                    endTimestamp = endTs,
                    isRunning = true,
                    isPaused = false
                )
            }
        }
        scheduleTimerAlarm(end)

        promoteToForeground(buildNotification(preferences))
        startLoops()
    }

    private fun focusTimerSession(
        timerId: Long,
        label: String,
        seconds: Int,
        totalSeconds: Int,
        remMillis: Long = 0L,
        endRealtime: Long = 0L,
        endTimestamp: Long = 0L
    ) {
        val safe = seconds.coerceAtLeast(0)
        val end = if (endRealtime > 0L) {
            endRealtime
        } else if (remMillis > 0L) {
            SystemClock.elapsedRealtime() + remMillis
        } else if (safe > 0) {
            SystemClock.elapsedRealtime() + safe * 1000L
        } else {
            0L
        }
        val endTs = if (endTimestamp > 0L) {
            endTimestamp
        } else if (remMillis > 0L) {
            System.currentTimeMillis() + remMillis
        } else if (safe > 0) {
            System.currentTimeMillis() + safe * 1000L
        } else {
            0L
        }
        val prevSlot = timerSlotAssignments.remove(timerId)
        if (prevSlot != null) {
            BaseMultiTimerSlotService.stopSlot(this, prevSlot)
        }
        val oldSecondaryNotifId = (NOTIFICATION_BASE_MULTI_ID + timerId).toInt()
        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(oldSecondaryNotifId)
        } catch (_: Exception) {}
        postedSecondaryNotifIds.remove(oldSecondaryNotifId)
        lastPausedPostedTimerIds.remove(timerId)

        TimerStopwatchManager.setTimer(
            running = safe > 0,
            totalSeconds = totalSeconds,
            secondsLeft = safe,
            endRealtime = end,
            timerId = timerId,
            label = label,
            endTimestamp = endTs
        )
        if (end > 0L) {
            scheduleTimerAlarm(end)
        }
        promoteToForeground(buildNotification(preferences))
        startLoops()
    }

    private fun pauseTimer() {
        cancelTimerAlarm()
        val state = TimerStopwatchManager.timer.value
        if (!state.hasSession) return
        val remaining = if (state.running) {
            ((state.endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L) + 999L) / 1000L
        } else {
            state.secondsLeft.toLong()
        }
        lastPausedPostedTimerIds.remove(state.timerId)
        TimerStopwatchManager.setTimer(
            running = false,
            totalSeconds = state.totalSeconds,
            secondsLeft = remaining.toInt(),
            endRealtime = 0L,
            timerId = state.timerId,
            label = state.label,
            endTimestamp = 0L
        )
        if (state.timerId != 0L) {
            scope.launch {
                repository.updateTimerState(
                    id = state.timerId,
                    remainingMillis = remaining * 1000L,
                    endTimestamp = 0L,
                    isRunning = false,
                    isPaused = true
                )
                startLoops()
            }
        } else {
            startLoops()
        }
    }

    private fun pauseMultiTimerSession(timerId: Long) {
        scope.launch {
            val timer = repository.getTimerById(timerId)
            if (timer != null && timer.isRunning) {
                val now = System.currentTimeMillis()
                val rem = (timer.endTimestamp - now).coerceAtLeast(0L)
                repository.updateTimerState(
                    id = timerId,
                    remainingMillis = rem,
                    endTimestamp = 0L,
                    isRunning = false,
                    isPaused = true
                )
            }
            lastPausedPostedTimerIds.remove(timerId)
            val current = TimerStopwatchManager.timer.value
            if (current.timerId == timerId) {
                val prevSlot = timerSlotAssignments.remove(timerId)
                if (prevSlot != null) {
                    BaseMultiTimerSlotService.stopSlot(this@TimerService, prevSlot)
                }
                val secNotifId = (NOTIFICATION_BASE_MULTI_ID + timerId).toInt()
                try {
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(secNotifId)
                } catch (_: Exception) {}
                postedSecondaryNotifIds.remove(secNotifId)
                pauseTimer()
            } else {
                startLoops()
            }
        }
    }

    private fun resumeTimer() {
        val state = TimerStopwatchManager.timer.value
        if (state.secondsLeft <= 0) return
        val end = SystemClock.elapsedRealtime() + state.secondsLeft * 1000L
        val endTs = System.currentTimeMillis() + state.secondsLeft * 1000L
        lastPausedPostedTimerIds.remove(state.timerId)
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = state.totalSeconds,
            secondsLeft = state.secondsLeft,
            endRealtime = end,
            timerId = state.timerId,
            label = state.label,
            endTimestamp = endTs
        )
        if (state.timerId != 0L) {
            scope.launch {
                repository.updateTimerState(
                    id = state.timerId,
                    remainingMillis = state.secondsLeft * 1000L,
                    endTimestamp = endTs,
                    isRunning = true,
                    isPaused = false
                )
            }
        }
        scheduleTimerAlarm(end)
        startLoops()
    }

    private fun adjustTimer(deltaSeconds: Int) {
        val state = TimerStopwatchManager.timer.value
        if (deltaSeconds == 0) return
        if (!state.hasSession) {
            val armed = deltaSeconds.coerceAtLeast(0)
            if (armed == 0) return
            TimerStopwatchManager.setTimer(
                running = false,
                totalSeconds = armed,
                secondsLeft = armed,
                endRealtime = 0L,
                timerId = state.timerId,
                label = state.label,
                endTimestamp = 0L
            )
            cancelTimerAlarm()
            return
        }
        val newLeft = (state.secondsLeft + deltaSeconds).coerceAtLeast(0)
        val newTotal = (state.totalSeconds + deltaSeconds).coerceAtLeast(0)
        val end = if (state.running) {
            SystemClock.elapsedRealtime() + newLeft * 1000L
        } else {
            0L
        }
        val endTs = if (state.running) {
            System.currentTimeMillis() + newLeft * 1000L
        } else {
            0L
        }
        TimerStopwatchManager.setTimer(
            running = state.running,
            totalSeconds = newTotal,
            secondsLeft = newLeft,
            endRealtime = end,
            timerId = state.timerId,
            label = state.label,
            endTimestamp = endTs
        )
        if (state.running) {
            scheduleTimerAlarm(end)
        } else {
            cancelTimerAlarm()
        }
    }

    private fun extendExpiredTimer(deltaSeconds: Int) {
        stopRingAudio()
        val state = TimerStopwatchManager.timer.value
        val added = deltaSeconds.coerceAtLeast(1)
        val newLeft = if (state.ringing) added else (state.secondsLeft + added).coerceAtLeast(1)
        val newTotal = maxOf(state.totalSeconds, newLeft)
        val end = SystemClock.elapsedRealtime() + newLeft * 1000L
        val endTs = System.currentTimeMillis() + newLeft * 1000L
        lastPausedPostedTimerIds.remove(state.timerId)
        TimerStopwatchManager.setTimer(
            running = true,
            totalSeconds = newTotal,
            secondsLeft = newLeft,
            endRealtime = end,
            timerId = state.timerId,
            label = state.label,
            endTimestamp = endTs
        )
        scheduleTimerAlarm(end)

        promoteToForeground(buildNotification(preferences))
        startLoops()
    }

    private fun resumeMultiTimerSession(timerId: Long) {
        scope.launch {
            val timer = repository.getTimerById(timerId)
            if (timer != null) {
                val now = System.currentTimeMillis()
                val rem = if (timer.remainingMillis > 0L) timer.remainingMillis else timer.durationMillis
                val remSeconds = ((rem + 999L) / 1000L).coerceAtLeast(0L)
                val alignedMillis = remSeconds * 1000L
                val endTs = now + alignedMillis
                repository.updateTimerState(
                    id = timer.id,
                    remainingMillis = alignedMillis,
                    endTimestamp = endTs,
                    isRunning = true,
                    isPaused = false
                )
                lastPausedPostedTimerIds.remove(timerId)
                val current = TimerStopwatchManager.timer.value
                if (current.timerId == timerId) {
                    TimerStopwatchManager.setTimer(
                        running = true,
                        totalSeconds = (timer.durationMillis / 1000L).toInt(),
                        secondsLeft = remSeconds.toInt(),
                        endRealtime = SystemClock.elapsedRealtime() + alignedMillis,
                        timerId = timer.id,
                        label = timer.label,
                        endTimestamp = endTs
                    )
                    scheduleTimerAlarm(SystemClock.elapsedRealtime() + alignedMillis)
                }
                startLoops()
            }
        }
    }

    private fun cancelMultiTimerSession(timerId: Long) {
        scope.launch {
            val timer = repository.getTimerById(timerId)
            if (timer != null) {
                repository.updateTimerState(
                    id = timerId,
                    remainingMillis = timer.durationMillis,
                    endTimestamp = 0L,
                    isRunning = false,
                    isPaused = false
                )
            }
            val slot = timerSlotAssignments.remove(timerId)
            if (slot != null) {
                BaseMultiTimerSlotService.stopSlot(this@TimerService, slot)
            }
            val notifId = (NOTIFICATION_BASE_MULTI_ID + timerId).toInt()
            try {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
            } catch (_: Exception) {}
            postedSecondaryNotifIds.remove(notifId)
            lastDisplayedSecond.remove(timerId)
            lastPausedPostedTimerIds.remove(timerId)

            val current = TimerStopwatchManager.timer.value
            if (current.timerId == timerId) {
                cancelTimerAlarm()
                stopRingAudio()
                TimerStopwatchManager.setTimer(
                    running = false,
                    totalSeconds = 0,
                    secondsLeft = 0,
                    endRealtime = 0L,
                    endTimestamp = 0L
                )
            }
            checkRemainingTimersOrShutdown()
        }
    }

    private fun checkRemainingTimersOrShutdown() {
        scope.launch {
            val runningOrPaused = repository.getRunningOrPausedTimers()
            val next = runningOrPaused.firstOrNull { it.isRunning } ?: runningOrPaused.firstOrNull()
            if (next != null) {
                val now = System.currentTimeMillis()
                val rem = if (next.isRunning) (next.endTimestamp - now).coerceAtLeast(0L) else next.remainingMillis
                val secondsLeft = ((rem + 999L) / 1000L).toInt()
                val totalSeconds = (next.durationMillis / 1000L).toInt()
                focusTimerSession(
                    next.id,
                    next.label,
                    secondsLeft,
                    totalSeconds,
                    rem,
                    0L,
                    if (next.isRunning) next.endTimestamp else 0L
                )
            } else if (runningOrPaused.isEmpty() && !TimerStopwatchManager.timer.value.hasSession) {
                shutdown()
            }
        }
    }

    private fun startLoops() {
        startUiTicker()
        startNotifDispatcher()
    }

    private fun stopLoops() {
        uiTickerJob?.cancel()
        uiTickerJob = null
        notifDispatcherJob?.cancel()
        notifDispatcherJob = null
    }

    private fun startUiTicker() {
        if (uiTickerJob?.isActive == true) return
        uiTickerJob = scope.launch {
            while (isActive) {
                if (!fastUiTick()) break
                delay(200L)
            }
        }
    }

    private suspend fun fastUiTick(): Boolean {
        val state = TimerStopwatchManager.timer.value
        val runningInDb = withContext(Dispatchers.IO) { repository.getRunningOrPausedTimers() }
        val now = System.currentTimeMillis()

        var expiredTimer: TimerEntity? = null
        for (t in runningInDb) {
            if (t.isRunning && t.endTimestamp > 0L) {
                val rem = t.endTimestamp - now
                if (rem <= 0L) {
                    expiredTimer = t
                    break
                }
            }
        }

        if (expiredTimer != null) {
            val slot = timerSlotAssignments.remove(expiredTimer.id)
            if (slot != null) {
                BaseMultiTimerSlotService.stopSlot(this, slot)
            }
            val notifId = (NOTIFICATION_BASE_MULTI_ID + expiredTimer.id).toInt()
            try {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
            } catch (_: Exception) {}
            postedSecondaryNotifIds.remove(notifId)
            lastDisplayedSecond.remove(expiredTimer.id)
            lastPausedPostedTimerIds.remove(expiredTimer.id)

            withContext(Dispatchers.IO) {
                repository.updateTimerState(expiredTimer.id, expiredTimer.durationMillis, 0L, false, false)
            }
            TimerStopwatchManager.setTimer(
                running = false,
                totalSeconds = (expiredTimer.durationMillis / 1000L).toInt(),
                secondsLeft = 0,
                endRealtime = 0L,
                finished = true,
                ringing = true,
                timerId = expiredTimer.id,
                label = expiredTimer.label,
                endTimestamp = 0L
            )
            enterRinging()
            return false
        }

        if (state.hasSession && state.running) {
            val remainingMillis = state.endRealtime - SystemClock.elapsedRealtime()
            val secondsLeft = if (remainingMillis <= 0L) {
                0
            } else {
                ((remainingMillis + 999L) / 1000L).toInt()
            }
            TimerStopwatchManager.updateTimerSecondsLeft(secondsLeft)
            if (secondsLeft <= 0) {
                if (state.timerId != 0L) {
                    withContext(Dispatchers.IO) {
                        val entity = repository.getTimerById(state.timerId)
                        if (entity != null) {
                            repository.updateTimerState(entity.id, entity.durationMillis, 0L, false, false)
                        }
                    }
                }
                lastDisplayedSecond.remove(state.timerId)
                lastPausedPostedTimerIds.remove(state.timerId)
                enterRinging()
                return false
            }
        }

        val anyActive = (state.hasSession && (state.running || state.secondsLeft > 0)) || runningInDb.any { it.isRunning || it.isPaused }
        if (!anyActive && !state.ringing) {
            shutdown()
            return false
        }

        val earliestRemainingMillis = runningInDb
            .filter { it.isRunning && it.endTimestamp > now }
            .map { it.endTimestamp - now }
            .let { list ->
                if (state.hasSession && state.running && state.endRealtime > 0L) {
                    val primaryRem = state.endRealtime - SystemClock.elapsedRealtime()
                    if (primaryRem > 0L) list + primaryRem else list
                } else list
            }
            .minOrNull()
        if (earliestRemainingMillis != null && earliestRemainingMillis > 0L) {
            scheduleTimerAlarm(SystemClock.elapsedRealtime() + earliestRemainingMillis)
        }

        val activeSecondary = runningInDb.filter { it.id != state.timerId && (it.isRunning || it.isPaused) }
        val activeSecondaryIds = activeSecondary.map { it.id }.toSet()
        val stoppedTimerIds = timerSlotAssignments.keys.filter { it !in activeSecondaryIds }
        for (tid in stoppedTimerIds) {
            val slot = timerSlotAssignments.remove(tid)
            if (slot != null) {
                BaseMultiTimerSlotService.stopSlot(this, slot)
            }
        }

        for (timer in activeSecondary) {
            val assignedSlot = timerSlotAssignments.getOrPut(timer.id) {
                val usedSlots = timerSlotAssignments.values.toSet()
                (0 until BaseMultiTimerSlotService.MAX_SLOTS).firstOrNull { it !in usedSlots } ?: -1
            }
            if (assignedSlot != -1 && !BaseMultiTimerSlotService.isSlotRunning(assignedSlot)) {
                val rem = if (timer.isRunning) (timer.endTimestamp - now).coerceAtLeast(0L) else timer.remainingMillis
                val sec = ((rem + 999L) / 1000L).toInt()
                val tot = (timer.durationMillis / 1000L).toInt()
                val notifId = (NOTIFICATION_BASE_MULTI_ID + timer.id).toInt()
                preferences = settings.currentNowBarPreferences()
                val notif = buildTimerNotification(
                    timerId = timer.id,
                    label = timer.label,
                    secondsLeft = sec,
                    totalSeconds = tot,
                    running = timer.isRunning,
                    preferences = preferences,
                    isPromotedNowBar = true,
                    endTimestamp = if (timer.isRunning) timer.endTimestamp else 0L
                )
                NotificationRateLimiter.acquireSlot()
                BaseMultiTimerSlotService.startSlot(this, assignedSlot, notifId, notif)
                postedSecondaryNotifIds.add(notifId)
                if (timer.isRunning) {
                    lastDisplayedSecond[timer.id] = rem / 1000L
                } else {
                    lastPausedPostedTimerIds.add(timer.id)
                }
            }
        }

        val activeIds = (runningInDb.map { (NOTIFICATION_BASE_MULTI_ID + it.id).toInt() }).toSet()
        val toCancel = postedSecondaryNotifIds.filter { it !in activeIds }
        for (id in toCancel) {
            try {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
            } catch (_: Exception) {}
            postedSecondaryNotifIds.remove(id)
            val tid = (id - NOTIFICATION_BASE_MULTI_ID).toLong()
            lastDisplayedSecond.remove(tid)
            lastPausedPostedTimerIds.remove(tid)
        }

        return true
    }

    private fun startNotifDispatcher() {
        if (notifDispatcherJob?.isActive == true) return
        notifDispatcherJob = scope.launch {
            var rrIndex = 0
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            while (isActive) {
                val state = TimerStopwatchManager.timer.value
                val runningInDb = withContext(Dispatchers.IO) { repository.getRunningOrPausedTimers() }

                if (!state.hasSession && runningInDb.isNotEmpty()) {
                    val next = runningInDb.firstOrNull { it.isRunning } ?: runningInDb.firstOrNull()
                    if (next != null) {
                        val now = System.currentTimeMillis()
                        val rem = if (next.isRunning) (next.endTimestamp - now).coerceAtLeast(0L) else next.remainingMillis
                        val sec = ((rem + 999L) / 1000L).toInt()
                        val tot = (next.durationMillis / 1000L).toInt()
                        focusTimerSession(next.id, next.label, sec, tot, rem, 0L, next.endTimestamp)
                        delay(50L)
                        continue
                    }
                }

                if (state.hasSession && !state.running && !state.ringing && state.timerId !in lastPausedPostedTimerIds) {
                    NotificationRateLimiter.acquireSlot()
                    preferences = settings.currentNowBarPreferences()
                    val notif = buildNotification(preferences)
                    try {
                        manager.notify(NOTIFICATION_ID, notif)
                        NotificationRateLimiter.markPostedFromMainThread()
                    } catch (e: Exception) {
                        Log.e(TAG, "Unable to post primary paused notification", e)
                    }
                    lastPausedPostedTimerIds.add(state.timerId)
                }

                for (timer in runningInDb) {
                    if (timer.id == state.timerId) continue
                    if (timer.isPaused && timer.id !in lastPausedPostedTimerIds) {
                        NotificationRateLimiter.acquireSlot()
                        val rem = timer.remainingMillis
                        val sec = ((rem + 999L) / 1000L).toInt()
                        val tot = (timer.durationMillis / 1000L).toInt()
                        val notifId = (NOTIFICATION_BASE_MULTI_ID + timer.id).toInt()
                        postedSecondaryNotifIds.add(notifId)
                        preferences = settings.currentNowBarPreferences()
                        val notif = buildTimerNotification(
                            timerId = timer.id,
                            label = timer.label,
                            secondsLeft = sec,
                            totalSeconds = tot,
                            running = false,
                            preferences = preferences,
                            isPromotedNowBar = true,
                            endTimestamp = 0L
                        )
                        try {
                            manager.notify(notifId, notif)
                            NotificationRateLimiter.markPostedFromMainThread()
                        } catch (e: Exception) {
                            Log.e(TAG, "Unable to post secondary paused notification", e)
                        }
                        lastPausedPostedTimerIds.add(timer.id)
                    }
                }

                val runningTargets = mutableListOf<Long>()
                if (state.hasSession && state.running) {
                    runningTargets.add(state.timerId)
                    lastPausedPostedTimerIds.remove(state.timerId)
                }
                for (timer in runningInDb) {
                    if (timer.id != state.timerId && timer.isRunning) {
                        runningTargets.add(timer.id)
                        lastPausedPostedTimerIds.remove(timer.id)
                    }
                }

                if (runningTargets.isEmpty()) {
                    delay(200L)
                    continue
                }

                val targetSpacing = maxOf(
                    NotificationRateLimiter.MIN_GLOBAL_NOTIFY_GAP_MS,
                    1000L / runningTargets.size
                )

                rrIndex = (rrIndex + 1) % runningTargets.size
                val currentTargetId = runningTargets[rrIndex]

                NotificationRateLimiter.acquireSlot(targetSpacing)

                if (currentTargetId == state.timerId) {
                    val freshState = TimerStopwatchManager.timer.value
                    if (freshState.hasSession && freshState.running) {
                        val remMillis = (freshState.endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                        val curSec = (remMillis + 999L) / 1000L
                        if (lastDisplayedSecond[freshState.timerId] != curSec) {
                            lastDisplayedSecond[freshState.timerId] = curSec
                            preferences = settings.currentNowBarPreferences()
                            val notif = buildNotification(preferences)
                            try {
                                manager.notify(NOTIFICATION_ID, notif)
                                NotificationRateLimiter.markPostedFromMainThread()
                            } catch (e: Exception) {
                                Log.e(TAG, "Unable to update primary notification", e)
                            }
                        }
                    }
                } else {
                    val freshTimer = withContext(Dispatchers.IO) { repository.getTimerById(currentTargetId) }
                    if (freshTimer != null && freshTimer.isRunning) {
                        val now = System.currentTimeMillis()
                        val remMillis = (freshTimer.endTimestamp - now).coerceAtLeast(0L)
                        val curSec = (remMillis + 999L) / 1000L
                        if (lastDisplayedSecond[freshTimer.id] != curSec) {
                            lastDisplayedSecond[freshTimer.id] = curSec
                            val sec = ((remMillis + 999L) / 1000L).toInt()
                            val tot = (freshTimer.durationMillis / 1000L).toInt()
                            val notifId = (NOTIFICATION_BASE_MULTI_ID + freshTimer.id).toInt()
                            postedSecondaryNotifIds.add(notifId)
                            preferences = settings.currentNowBarPreferences()
                            val notif = buildTimerNotification(
                                timerId = freshTimer.id,
                                label = freshTimer.label,
                                secondsLeft = sec,
                                totalSeconds = tot,
                                running = true,
                                preferences = preferences,
                                isPromotedNowBar = true,
                                endTimestamp = freshTimer.endTimestamp
                            )
                            try {
                                manager.notify(notifId, notif)
                                NotificationRateLimiter.markPostedFromMainThread()
                            } catch (e: Exception) {
                                Log.e(TAG, "Unable to update secondary notification for timer ${freshTimer.id}", e)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun enterRinging() {
        stopLoops()
        cancelTimerAlarm()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Lucid:TimerServiceRingingWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(10 * 60_000L)
            }
        } else if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(10 * 60_000L)
        }
        TimerStopwatchManager.markTimerRinging()
        preferences = settings.currentNowBarPreferences()
        startRingAudio()
        withContext(Dispatchers.Main) { postRingingNotification() }
    }

    private fun postRingingNotification() {
        val state = TimerStopwatchManager.timer.value
        val large = nowBar.largeIcon(NowBarNotification.Kind.TIMER, preferences.accentArgb, preferences.isDark)
        val timeText = NowBarFormatting.timerTime(0)
        val durationText = NowBarFormatting.durationShort(state.totalSeconds * 1000L, this)
        val subtitle = if (state.label.isNotBlank()) {
            "${state.label} • ${getString(R.string.timer_times_up)} ($durationText)"
        } else {
            "${getString(R.string.timer_times_up)} ($durationText)"
        }

        val builder = NotificationCompat.Builder(this, RING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle(if (state.label.isNotBlank()) state.label else timeText)
            .setContentText(subtitle)
            .setLargeIcon(large)
            .setContentIntent(nowBar.openAppPendingIntent(NowBarNotification.INITIAL_TAB_TIMER))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(preferences.accentArgb)
            .setColorized(true)
            .addAction(
                buildNotificationAction(
                    this,
                    R.drawable.ic_action_stop,
                    getString(R.string.action_dismiss),
                    nowBar.actionPendingIntent(
                        ACTION_DISMISS_TIMER,
                        NowBarNotification.TARGET_TIMER,
                        RING_DISMISS_REQUEST,
                        Bundle().apply { putLong(EXTRA_TIMER_ID, state.timerId) }
                    )
                )
            )
            .addAction(
                buildNotificationAction(
                    this,
                    R.drawable.ic_action_add,
                    getString(R.string.timer_add_1_min),
                    nowBar.actionPendingIntent(
                        ACTION_ADD_ONE_MINUTE,
                        NowBarNotification.TARGET_TIMER,
                        RING_ADD_REQUEST,
                        Bundle().apply { putLong(EXTRA_TIMER_ID, state.timerId) }
                    )
                )
            )
            .setDeleteIntent(
                nowBar.actionPendingIntent(
                    ACTION_DISMISS_TIMER,
                    NowBarNotification.TARGET_TIMER,
                    RING_DELETE_REQUEST,
                    Bundle().apply { putLong(EXTRA_TIMER_ID, state.timerId) }
                )
            )

        builder.addExtras(
            buildNowBarExtras(timeText, subtitle, preferences.accentArgb, large)
        )

        promoteToForeground(builder.build())
    }

    private fun dismissRinging() {
        stopLoops()
        cancelTimerAlarm()
        stopRingAudio()
        val timerId = TimerStopwatchManager.timer.value.timerId
        TimerStopwatchManager.setTimer(
            running = false,
            totalSeconds = 0,
            secondsLeft = 0,
            endRealtime = 0L,
            endTimestamp = 0L
        )
        lastDisplayedSecond.remove(timerId)
        lastPausedPostedTimerIds.remove(timerId)
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to cancel the timer notification", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        if (timerId != 0L) {
            checkRemainingTimersOrShutdown()
        } else {
            stopSelf()
        }
    }

    private suspend fun startRingAudio() {
        val soundEnabled = runCatching { settings.timerSoundEnabledFlow.first() }.getOrDefault(true)
        val ringtoneUri = runCatching { settings.timerRingtoneUriFlow.first() }.getOrDefault("")
        val vibePolicy = runCatching {
            settings.timerVibrationPolicyFlow.first()
        }.getOrDefault(TimerVibrationPolicy.UNIFIED)

        val state = TimerStopwatchManager.timer.value
        val (vibrationEnabled, vibrationPattern) = if (vibePolicy == TimerVibrationPolicy.PER_TIMER) {
            val dbTimer = if (state.timerId != 0L) withContext(Dispatchers.IO) { repository.getTimerById(state.timerId) } else null
            val enabled = dbTimer?.vibrate ?: true
            val patternName = dbTimer?.vibrationPattern ?: TimerVibrationPattern.TICK_TICK.name
            val pattern = runCatching { TimerVibrationPattern.valueOf(patternName) }.getOrDefault(TimerVibrationPattern.TICK_TICK)
            Pair(enabled, pattern)
        } else {
            val enabled = runCatching { settings.timerVibrationEnabledFlow.first() }.getOrDefault(true)
            val pattern = runCatching { settings.timerVibrationPatternFlow.first() }.getOrDefault(TimerVibrationPattern.TICK_TICK)
            Pair(enabled, pattern)
        }

        stopRingAudio()

        val policy = runCatching {
            settings.timerVolumePolicyFlow.first()
        }.getOrDefault(TimerVolumePolicy.UNIFIED)

        val targetVol = if (policy == TimerVolumePolicy.PER_TIMER) {
            val dbTimer = if (state.timerId != 0L) withContext(Dispatchers.IO) { repository.getTimerById(state.timerId) } else null
            dbTimer?.volume ?: 1.0f
        } else {
            runCatching {
                settings.timerUnifiedVolumeFlow.first()
            }.getOrDefault(0.85f)
        }

        if (soundEnabled && targetVol > 0.001f && ringtoneUri.isNotBlank() && ringtoneUri != "silent") {
            val uri: Uri? = runCatching { Uri.parse(ringtoneUri) }.getOrNull()
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
                player.setVolume(targetVol, targetVol)
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        fallback?.volume = targetVol
                    }
                    fallback?.play()
                    ringtone = fallback
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to play the fallback ringtone", e)
                }
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
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
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
                setBypassDnd(true)
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
            NotificationRateLimiter.markPostedFromMainThread()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to enter the foreground", e)
            stopSelf()
        }
    }

    private fun shutdown() {
        stopLoops()
        cancelTimerAlarm()
        stopRingAudio()
        timerSlotAssignments.clear()
        try {
            BaseMultiTimerSlotService.stopAllSlots(this)
        } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        lastDisplayedSecond.clear()
        lastPausedPostedTimerIds.clear()
        stopSelf()
    }

    private fun buildNotification(preferences: SettingsDataStore.NowBarPreferences): Notification {
        val state = TimerStopwatchManager.timer.value
        val endTs = if (state.running) {
            if (state.endTimestamp > 0L) {
                state.endTimestamp
            } else if (state.endRealtime > 0L) {
                System.currentTimeMillis() + (state.endRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            } else 0L
        } else 0L
        return buildTimerNotification(
            timerId = state.timerId,
            label = state.label,
            secondsLeft = state.secondsLeft,
            totalSeconds = state.totalSeconds,
            running = state.running,
            preferences = preferences,
            isPromotedNowBar = true,
            endTimestamp = endTs
        )
    }

    private fun buildTimerNotification(
        timerId: Long,
        label: String,
        secondsLeft: Int,
        totalSeconds: Int,
        running: Boolean,
        preferences: SettingsDataStore.NowBarPreferences,
        isPromotedNowBar: Boolean = false,
        endTimestamp: Long = 0L
    ): Notification {
        val accent = preferences.accentArgb

        val timeText = NowBarFormatting.timerTime(secondsLeft)
        val durationText = NowBarFormatting.durationShort(totalSeconds * 1000L, this)
        val subtitle = if (label.isNotBlank()) {
            if (running) {
                "$label • $durationText / ${NowBarFormatting.estimatedEndTime(secondsLeft * 1000L, preferences.is24Hour, preferences.removeLeadingZero, this)}"
            } else {
                "$label • $durationText"
            }
        } else {
            NowBarFormatting.timerSubtitle(
                totalSeconds = totalSeconds,
                secondsLeft = secondsLeft,
                running = running,
                is24Hour = preferences.is24Hour,
                removeLeadingZero = preferences.removeLeadingZero,
                context = this
            )
        }

        val badgeIcon = nowBar.largeIcon(NowBarNotification.Kind.TIMER, accent, preferences.isDark)
        val contentTitle = if (label.isNotBlank()) label else getString(R.string.tab_timer)

        val leftRequestCode = if (timerId != 0L) (30000 + timerId * 2).toInt() else NowBarNotification.LEFT_TIMER_REQUEST
        val rightRequestCode = if (timerId != 0L) (30000 + timerId * 2 + 1).toInt() else NowBarNotification.RIGHT_TIMER_REQUEST

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_nowbar)
            .setContentTitle(contentTitle)
            .setContentText(timeText)
            .setLargeIcon(badgeIcon)
            .setContentIntent(nowBar.openAppPendingIntent(NowBarNotification.INITIAL_TAB_TIMER))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(accent)
            .setColorized(true)

        if (running && secondsLeft > 0) {
            val targetWhen = if (endTimestamp > 0L) endTimestamp else (System.currentTimeMillis() + secondsLeft * 1000L)
            builder.setWhen(targetWhen)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
            builder.setShowWhen(true)
        } else {
            builder.setShowWhen(false)
            builder.setUsesChronometer(false)
        }
            .addAction(
                buildNotificationAction(
                    this,
                    R.drawable.ic_action_close,
                    getString(R.string.action_cancel),
                    nowBar.actionPendingIntent(
                        if (timerId != 0L) ACTION_CANCEL_MULTI else ACTION_CANCEL,
                        NowBarNotification.TARGET_TIMER,
                        leftRequestCode,
                        Bundle().apply { putLong(EXTRA_TIMER_ID, timerId) }
                    )
                )
            )
            .addAction(
                buildNotificationAction(
                    this,
                    if (running) R.drawable.ic_action_pause else R.drawable.ic_action_play,
                    if (running) getString(R.string.action_pause) else getString(R.string.action_resume),
                    nowBar.actionPendingIntent(
                        if (timerId != 0L) {
                            if (running) ACTION_PAUSE_MULTI else ACTION_START_MULTI
                        } else {
                            if (running) ACTION_PAUSE else ACTION_RESUME
                        },
                        NowBarNotification.TARGET_TIMER,
                        rightRequestCode,
                        Bundle().apply {
                            putLong(EXTRA_TIMER_ID, timerId)
                            putString(EXTRA_LABEL, label)
                            putInt(EXTRA_SECONDS, secondsLeft)
                            putInt(EXTRA_TOTAL_SECONDS, totalSeconds)
                        }
                    )
                )
            )

        if (isPromotedNowBar) {
            builder.addExtras(buildNowBarExtras(timeText, subtitle, accent, badgeIcon))
        }
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

    private fun scheduleTimerAlarm(endRealtime: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pendingIntent = PendingIntent.getService(
            this,
            TIMER_ALARM_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endRealtime,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                endRealtime,
                pendingIntent
            )
        }
    }

    private fun cancelTimerAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimerService::class.java).apply {
            action = ACTION_TIMER_EXPIRED
        }
        val pendingIntent = PendingIntent.getService(
            this,
            TIMER_ALARM_REQUEST,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        private const val TAG = "TimerService"

        const val NOTIFICATION_ID = 42001
        const val CHANNEL_ID = "lucid_timer_ongoing"
        const val RING_CHANNEL_ID = "lucid_timer_ringing"

        const val ACTION_START = "com.lucid.alarm.timer.START"
        const val ACTION_START_MULTI = "com.lucid.alarm.timer.START_MULTI"
        const val ACTION_FOCUS_TIMER = "com.lucid.alarm.timer.FOCUS_TIMER"
        const val ACTION_PAUSE = "com.lucid.alarm.timer.PAUSE"
        const val ACTION_PAUSE_MULTI = "com.lucid.alarm.timer.PAUSE_MULTI"
        const val ACTION_RESUME = "com.lucid.alarm.timer.RESUME"
        const val ACTION_ADJUST = "com.lucid.alarm.timer.ADJUST"
        const val ACTION_CANCEL = "com.lucid.alarm.timer.CANCEL"
        const val ACTION_CANCEL_MULTI = "com.lucid.alarm.timer.CANCEL_MULTI"
        const val ACTION_SYNC_MULTI = "com.lucid.alarm.timer.SYNC_MULTI"
        const val ACTION_DISMISS_TIMER = "com.lucid.alarm.timer.DISMISS"
        const val ACTION_ADD_ONE_MINUTE = "com.lucid.alarm.timer.ADD_ONE_MINUTE"
        const val ACTION_TIMER_EXPIRED = "com.lucid.alarm.timer.EXPIRED"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_TOTAL_SECONDS = "extra_total_seconds"
        const val EXTRA_DELTA_SECONDS = "extra_delta_seconds"
        const val EXTRA_TIMER_ID = "extra_timer_id"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_REMAINING_MILLIS = "extra_remaining_millis"
        const val EXTRA_END_REALTIME = "extra_end_realtime"
        const val EXTRA_END_TIMESTAMP = "extra_end_timestamp"

        const val NOTIFICATION_BASE_MULTI_ID = 20000

        private const val RING_DISMISS_REQUEST = 9201
        private const val RING_ADD_REQUEST = 9202
        private const val RING_DELETE_REQUEST = 9203
        private const val TIMER_ALARM_REQUEST = 9204

        fun start(context: Context, seconds: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, seconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startMultiTimer(context: Context, timerId: Long, label: String, secondsLeft: Int, totalSeconds: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START_MULTI
                putExtra(EXTRA_TIMER_ID, timerId)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_SECONDS, secondsLeft)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun focusTimer(
            context: Context,
            timerId: Long,
            label: String,
            secondsLeft: Int,
            totalSeconds: Int,
            remainingMillis: Long = 0L,
            endRealtime: Long = 0L,
            endTimestamp: Long = 0L
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_FOCUS_TIMER
                putExtra(EXTRA_TIMER_ID, timerId)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_SECONDS, secondsLeft)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_REMAINING_MILLIS, remainingMillis)
                putExtra(EXTRA_END_REALTIME, endRealtime)
                putExtra(EXTRA_END_TIMESTAMP, endTimestamp)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_PAUSE }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseMultiTimer(context: Context, timerId: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_PAUSE_MULTI
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_RESUME }
            ContextCompat.startForegroundService(context, intent)
        }

        fun adjust(context: Context, deltaSeconds: Int) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_ADJUST
                putExtra(EXTRA_DELTA_SECONDS, deltaSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_CANCEL }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopMultiTimer(context: Context, timerId: Long) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_CANCEL_MULTI
                putExtra(EXTRA_TIMER_ID, timerId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun syncActiveTimers(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = ACTION_SYNC_MULTI }
            ContextCompat.startForegroundService(context, intent)
        }

        fun dismiss(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_DISMISS_TIMER
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun addOneMinute(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_ADD_ONE_MINUTE
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
