package com.lucid.alarm.service

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Snapshot of the timer session. The service owns the countdown; the UI only mirrors this.
 *
 * @param running true while the countdown is ticking.
 * @param totalSeconds the full length of the session (never changes unless the user extends it).
 * @param secondsLeft the remaining seconds, refreshed by the service every second.
 * @param endRealtime [android.os.SystemClock.elapsedRealtime] at which the timer hits zero.
 * @param finished true once the countdown reached zero (so the UI can ring exactly once).
 * @param ringing true while the expired timer is still playing its alert and waiting to be
 * dismissed. The service keeps the foreground notification alive in this state.
 */
data class TimerSessionState(
    val running: Boolean = false,
    val totalSeconds: Int = 0,
    val secondsLeft: Int = 0,
    val endRealtime: Long = 0L,
    val finished: Boolean = false,
    val ringing: Boolean = false
) {
    /** True while there is something to show in the Now Bar. */
    val hasSession: Boolean get() = running || secondsLeft > 0 || ringing
}

/**
 * Snapshot of the stopwatch session.
 *
 * @param running true while the stopwatch is counting up.
 * @param startRealtime base clock of the running stopwatch, 0 while paused.
 * @param elapsedMillis elapsed millis captured when the stopwatch was paused.
 * @param laps cumulative elapsed millis of every recorded lap, newest first.
 */
data class StopwatchSessionState(
    val running: Boolean = false,
    val startRealtime: Long = 0L,
    val elapsedMillis: Long = 0L,
    val laps: List<Long> = emptyList()
) {
    /** True while there is something to show in the Now Bar. */
    val hasSession: Boolean get() = running || elapsedMillis > 0L || startRealtime != 0L

    /** Live elapsed millis, derived from the base clock while running. */
    val liveMillis: Long
        get() = if (running && startRealtime != 0L) {
            (SystemClock.elapsedRealtime() - startRealtime).coerceAtLeast(0L)
        } else {
            elapsedMillis
        }

    /** Split time of the most recent lap (0 when no lap was recorded yet). */
    val latestLapSplit: Long
        get() = when {
            laps.isEmpty() -> 0L
            laps.size == 1 -> laps[0]
            else -> (laps[0] - laps[1]).coerceAtLeast(0L)
        }
}

/**
 * Single source of truth for the Timer and the Stopwatch.
 *
 * The foreground services write here; the in-app Now Bar, the Timer screen and the Stopwatch
 * screen read from here, so the notification and the in-app card can never disagree.
 */
object TimerStopwatchManager {

    private val _timer = MutableStateFlow(TimerSessionState())
    val timer: StateFlow<TimerSessionState> = _timer.asStateFlow()

    private val _stopwatch = MutableStateFlow(StopwatchSessionState())
    val stopwatch: StateFlow<StopwatchSessionState> = _stopwatch.asStateFlow()

    /** Replaces the whole timer session. */
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

    /** Switches the finished session into the ringing state (the service owns the alert). */
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

    /** Updates only the remaining seconds (used by the countdown ticker). */
    fun updateTimerSecondsLeft(secondsLeft: Int) {
        if (_timer.value.secondsLeft == secondsLeft) return
        _timer.value = _timer.value.copy(secondsLeft = secondsLeft)
    }

    /** Replaces the whole stopwatch session. */
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

    /** Records a lap (cumulative elapsed millis). */
    fun addStopwatchLap(lap: Long) {
        val current = _stopwatch.value
        _stopwatch.value = current.copy(laps = listOf(lap) + current.laps)
    }

    /** Clears both sessions. */
    fun resetAll() {
        _timer.value = TimerSessionState()
        _stopwatch.value = StopwatchSessionState()
    }
}
