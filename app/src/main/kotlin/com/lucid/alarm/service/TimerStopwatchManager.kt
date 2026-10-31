package com.lucid.alarm.service

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimerSessionState(
    val running: Boolean = false,
    val totalSeconds: Int = 0,
    val secondsLeft: Int = 0,
    val endRealtime: Long = 0L,
    val finished: Boolean = false,
    val ringing: Boolean = false,
    val timerId: Long = 0L,
    val label: String = "",
    val endTimestamp: Long = 0L
) {

    val hasSession: Boolean get() = running || secondsLeft > 0 || ringing
}

data class ActiveTimerLiveState(
    val id: Long,
    val label: String,
    val durationMillis: Long,
    val remainingMillis: Long,
    val endTimestamp: Long,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val isRinging: Boolean
)

data class StopwatchSessionState(
    val running: Boolean = false,
    val startRealtime: Long = 0L,
    val elapsedMillis: Long = 0L,
    val laps: List<Long> = emptyList()
) {

    val hasSession: Boolean get() = running || elapsedMillis > 0L || startRealtime != 0L

    val liveMillis: Long
        get() = if (running && startRealtime != 0L) {
            (SystemClock.elapsedRealtime() - startRealtime).coerceAtLeast(0L)
        } else {
            elapsedMillis
        }

    val latestLapSplit: Long
        get() = when {
            laps.isEmpty() -> 0L
            laps.size == 1 -> laps[0]
            else -> (laps[0] - laps[1]).coerceAtLeast(0L)
        }
}

object TimerStopwatchManager {

    private val _timer = MutableStateFlow(TimerSessionState())
    val timer: StateFlow<TimerSessionState> = _timer.asStateFlow()

    private val _stopwatch = MutableStateFlow(StopwatchSessionState())
    val stopwatch: StateFlow<StopwatchSessionState> = _stopwatch.asStateFlow()

    private val _ringingTimerIds = MutableStateFlow<Set<Long>>(emptySet())
    val ringingTimerIds: StateFlow<Set<Long>> = _ringingTimerIds.asStateFlow()

    private val _activeTimersLive = MutableStateFlow<Map<Long, ActiveTimerLiveState>>(emptyMap())
    val activeTimersLive: StateFlow<Map<Long, ActiveTimerLiveState>> = _activeTimersLive.asStateFlow()

    fun markTimerRinging(timerId: Long) {
        _ringingTimerIds.update { it + timerId }
        val current = _timer.value
        if (current.timerId == timerId || current.timerId == 0L) {
            _timer.value = current.copy(
                running = false,
                secondsLeft = 0,
                endRealtime = 0L,
                finished = true,
                ringing = true,
                timerId = timerId
            )
        }
    }

    fun clearTimerRinging(timerId: Long) {
        _ringingTimerIds.update { it - timerId }
        if (_timer.value.timerId == timerId) {
            _timer.value = _timer.value.copy(ringing = false, finished = false)
        }
    }

    fun clearAllRinging() {
        _ringingTimerIds.value = emptySet()
        if (_timer.value.ringing) {
            _timer.value = _timer.value.copy(ringing = false, finished = false)
        }
    }

    fun updateActiveTimersLive(map: Map<Long, ActiveTimerLiveState>) {
        _activeTimersLive.value = map
    }

    fun setTimer(
        running: Boolean,
        totalSeconds: Int,
        secondsLeft: Int,
        endRealtime: Long,
        finished: Boolean = false,
        ringing: Boolean = false,
        timerId: Long = 0L,
        label: String = "",
        endTimestamp: Long = 0L
    ) {
        _timer.value = TimerSessionState(
            running = running,
            totalSeconds = totalSeconds,
            secondsLeft = secondsLeft,
            endRealtime = endRealtime,
            finished = finished,
            ringing = ringing,
            timerId = timerId,
            label = label,
            endTimestamp = endTimestamp
        )
    }

    fun markTimerRinging() {
        val current = _timer.value
        _timer.value = current.copy(
            running = false,
            secondsLeft = 0,
            endRealtime = 0L,
            finished = true,
            ringing = true,
            endTimestamp = 0L
        )
    }

    fun updateTimerSecondsLeft(secondsLeft: Int) {
        if (_timer.value.secondsLeft == secondsLeft) return
        _timer.value = _timer.value.copy(secondsLeft = secondsLeft)
    }

    fun setStopwatch(
        running: Boolean,
        startRealtime: Long,
        elapsedMillis: Long,
        laps: List<Long>
    ) {
        _stopwatch.value = StopwatchSessionState(
            running = running,
            startRealtime = startRealtime,
            elapsedMillis = elapsedMillis,
            laps = laps
        )
    }

    fun addStopwatchLap(lap: Long) {
        val current = _stopwatch.value
        _stopwatch.value = current.copy(laps = listOf(lap) + current.laps)
    }

    fun resetAll() {
        _timer.value = TimerSessionState()
        _stopwatch.value = StopwatchSessionState()
        _ringingTimerIds.value = emptySet()
        _activeTimersLive.value = emptyMap()
    }
}
