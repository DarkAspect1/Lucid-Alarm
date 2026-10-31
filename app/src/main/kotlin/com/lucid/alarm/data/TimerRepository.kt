package com.lucid.alarm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TimerRepository(private val timerDao: TimerDao) {

    val activeTimers: Flow<List<TimerEntity>> = timerDao.getActiveTimers()

    val deletedTimers: Flow<List<TimerEntity>> = timerDao.getDeletedTimers()

    suspend fun getTimerById(id: Long): TimerEntity? = withContext(Dispatchers.IO) {
        timerDao.getTimerById(id)
    }

    suspend fun getRunningOrPausedTimers(): List<TimerEntity> = withContext(Dispatchers.IO) {
        timerDao.getRunningOrPausedTimers()
    }

    suspend fun addTimer(
        label: String,
        durationMillis: Long,
        volume: Float = 1.0f,
        vibrate: Boolean = true,
        vibrationPattern: String = "TICK_TICK"
    ): Long = withContext(Dispatchers.IO) {
        val currentCount = timerDao.getActiveCount()
        val timer = TimerEntity(
            label = label,
            durationMillis = durationMillis,
            remainingMillis = durationMillis,
            orderIndex = currentCount,
            volume = volume,
            vibrate = vibrate,
            vibrationPattern = vibrationPattern
        )
        timerDao.insert(timer)
    }

    suspend fun updateTimerOrder(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEachIndexed { index, id ->
            timerDao.updateOrderIndex(id, index)
        }
    }

    suspend fun updateTimer(timer: TimerEntity) = withContext(Dispatchers.IO) {
        timerDao.update(timer)
    }

    suspend fun updateTimerState(id: Long, remainingMillis: Long, endTimestamp: Long, isRunning: Boolean, isPaused: Boolean) =
        withContext(Dispatchers.IO) {
            timerDao.updateTimerState(id, remainingMillis, endTimestamp, isRunning, isPaused)
        }

    suspend fun softDelete(ids: List<Long>, deletedAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        timerDao.softDelete(ids, deletedAt)
    }

    suspend fun restore(ids: List<Long>) = withContext(Dispatchers.IO) {
        timerDao.restore(ids)
    }

    suspend fun deletePermanently(ids: List<Long>) = withContext(Dispatchers.IO) {
        timerDao.deletePermanently(ids)
    }

    suspend fun emptyRecycleBin() = withContext(Dispatchers.IO) {
        timerDao.emptyRecycleBin()
    }

    suspend fun purgeExpiredTrash() = withContext(Dispatchers.IO) {
        val threeDaysAgo = System.currentTimeMillis() - 3L * 24L * 60L * 60L * 1000L
        timerDao.purgeExpired(threeDaysAgo)
    }
}
