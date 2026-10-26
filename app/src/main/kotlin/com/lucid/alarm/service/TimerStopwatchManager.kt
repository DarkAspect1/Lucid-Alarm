package com.lucid.alarm.service

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TimerSessionState(
    val running: Boolean = false,
    val totalSeconds: Int = 0,
    val secondsLeft: Int = 0,
    val endRealtime: Long = 0L,
    val finished: Boolean = false,
    val ringing: Boolean = false
) {

    val hasSession: Boolean get() = running || secondsLeft > 0 || ringing
}

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

    fun setTimer(
        running: Boolean,
        totalSeconds: Int,
        secondsLeft: Int,
        endRealtime: Long,
        finished: Boolean = false,
        ringing: Boolean = false
    ) {
        _timer.value = TimerSessionState(
            running = running,
            totalSeconds = totalSeconds,
            secondsLeft = secondsLeft,
            endRealtime = endRealtime,
            finished = finished,
            ringing = ringing
        )
    }

    fun markTimerRinging() {
        val current = _timer.value
        _timer.value = current.copy(
            running = false,
            secondsLeft = 0,
            endRealtime = 0L,
            finished = true,
            ringing = true
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
    }
}
