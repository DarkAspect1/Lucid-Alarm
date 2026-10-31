package com.lucid.alarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.data.TimerEntity
import com.lucid.alarm.service.TimerService
import com.lucid.alarm.service.TimerStopwatchManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AlarmApplication).timerRepository

    val activeTimers: StateFlow<List<TimerEntity>> = repository.activeTimers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val deletedTimers: StateFlow<List<TimerEntity>> = repository.deletedTimers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.purgeExpiredTrash()
        }
    }

    fun addPreset(
        label: String,
        durationMillis: Long,
        volume: Float = 1.0f,
        vibrate: Boolean = true,
        vibrationPattern: String = "TICK_TICK"
    ) {
        viewModelScope.launch {
            repository.addTimer(label, durationMillis, volume, vibrate, vibrationPattern)
        }
    }

    fun startTimerCard(timer: TimerEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rem = if (timer.remainingMillis > 0L) timer.remainingMillis else timer.durationMillis
            val remSeconds = ((rem + 999L) / 1000L).coerceAtLeast(0L)
            val alignedMillis = remSeconds * 1000L
            repository.updateTimerState(
                id = timer.id,
                remainingMillis = alignedMillis,
                endTimestamp = now + alignedMillis,
                isRunning = true,
                isPaused = false
            )
            TimerService.startMultiTimer(
                getApplication(),
                timer.id,
                timer.label,
                remSeconds.toInt(),
                (timer.durationMillis / 1000L).toInt()
            )
        }
    }

    fun pauseTimerCard(timer: TimerEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rem = (timer.endTimestamp - now).coerceIn(0L, timer.durationMillis)
            val remSeconds = ((rem + 999L) / 1000L).coerceAtLeast(0L)
            val alignedMillis = remSeconds * 1000L
            repository.updateTimerState(
                id = timer.id,
                remainingMillis = alignedMillis,
                endTimestamp = 0L,
                isRunning = false,
                isPaused = true
            )
            TimerService.pauseMultiTimer(getApplication(), timer.id)
            if (TimerStopwatchManager.timer.value.timerId == timer.id) {
                TimerStopwatchManager.setTimer(
                    running = false,
                    totalSeconds = (timer.durationMillis / 1000L).toInt(),
                    secondsLeft = (alignedMillis / 1000L).toInt(),
                    endRealtime = 0L,
                    timerId = timer.id,
                    label = timer.label
                )
            }
        }
    }

    fun stopTimerCard(timer: TimerEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val rem = if (timer.isRunning) {
                (timer.endTimestamp - now).coerceIn(0L, timer.durationMillis)
            } else {
                timer.remainingMillis.coerceIn(0L, timer.durationMillis)
            }
            val remSeconds = ((rem + 999L) / 1000L).coerceAtLeast(0L)
            val alignedMillis = remSeconds * 1000L
            repository.updateTimerState(
                id = timer.id,
                remainingMillis = alignedMillis,
                endTimestamp = 0L,
                isRunning = false,
                isPaused = true
            )
            TimerService.pauseMultiTimer(getApplication(), timer.id)
            if (TimerStopwatchManager.timer.value.timerId == timer.id) {
                TimerStopwatchManager.setTimer(
                    running = false,
                    totalSeconds = (timer.durationMillis / 1000L).toInt(),
                    secondsLeft = (alignedMillis / 1000L).toInt(),
                    endRealtime = 0L,
                    timerId = timer.id,
                    label = timer.label
                )
            }
        }
    }

    fun resetTimerCard(timer: TimerEntity) {
        viewModelScope.launch {
            repository.updateTimerState(
                id = timer.id,
                remainingMillis = timer.durationMillis,
                endTimestamp = 0L,
                isRunning = false,
                isPaused = false
            )
            TimerService.stopMultiTimer(getApplication(), timer.id)
            if (TimerStopwatchManager.timer.value.timerId == timer.id) {
                TimerStopwatchManager.setTimer(
                    running = false,
                    totalSeconds = (timer.durationMillis / 1000L).toInt(),
                    secondsLeft = (timer.durationMillis / 1000L).toInt(),
                    endRealtime = 0L,
                    timerId = timer.id,
                    label = timer.label
                )
            }
        }
    }

    fun startTimers(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = activeTimers.value.filter { it.id in ids }
            current.forEach { timer ->
                val rem = if (timer.remainingMillis > 0L) timer.remainingMillis else timer.durationMillis
                val remSeconds = ((rem + 999L) / 1000L).coerceAtLeast(0L)
                val alignedMillis = remSeconds * 1000L
                repository.updateTimerState(
                    id = timer.id,
                    remainingMillis = alignedMillis,
                    endTimestamp = now + alignedMillis,
                    isRunning = true,
                    isPaused = false
                )
            }
            TimerService.syncActiveTimers(getApplication())
        }
    }

    fun pauseTimers(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = activeTimers.value.filter { it.id in ids && it.isRunning }
            current.forEach { timer ->
                val rem = (timer.endTimestamp - now).coerceIn(0L, timer.durationMillis)
                val remSeconds = ((rem + 999L) / 1000L).coerceAtLeast(0L)
                val alignedMillis = remSeconds * 1000L
                repository.updateTimerState(
                    id = timer.id,
                    remainingMillis = alignedMillis,
                    endTimestamp = 0L,
                    isRunning = false,
                    isPaused = true
                )
            }
            TimerService.syncActiveTimers(getApplication())
        }
    }

    fun reorderTimers(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateTimerOrder(ids)
        }
    }

    fun resetTimers(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val current = activeTimers.value.filter { it.id in ids }
            current.forEach { timer ->
                repository.updateTimerState(
                    id = timer.id,
                    remainingMillis = timer.durationMillis,
                    endTimestamp = 0L,
                    isRunning = false,
                    isPaused = false
                )
            }
            TimerService.syncActiveTimers(getApplication())
        }
    }

    fun softDelete(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.softDelete(ids)
            TimerService.syncActiveTimers(getApplication())
        }
    }

    fun restore(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restore(ids)
        }
    }

    fun deletePermanently(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deletePermanently(ids)
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
        }
    }
}
