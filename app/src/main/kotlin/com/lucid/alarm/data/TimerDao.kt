package com.lucid.alarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers WHERE isDeleted = 0 ORDER BY orderIndex ASC, id ASC")
    fun getActiveTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE id = :id")
    fun getTimerById(id: Long): TimerEntity?

    @Query("SELECT COUNT(*) FROM timers WHERE isDeleted = 0")
    fun getActiveCount(): Int

    @Query("SELECT COUNT(*) FROM timers")
    fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(timer: TimerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(timers: List<TimerEntity>)

    @Update
    fun update(timer: TimerEntity)

    @Update
    fun updateAll(timers: List<TimerEntity>)

    @Query("UPDATE timers SET isDeleted = 1, deletedAt = :deletedAt, isRunning = 0, isPaused = 0, endTimestamp = 0 WHERE id IN (:ids)")
    fun softDelete(ids: List<Long>, deletedAt: Long)

    @Query("UPDATE timers SET isDeleted = 0, deletedAt = NULL WHERE id IN (:ids)")
    fun restore(ids: List<Long>)

    @Query("DELETE FROM timers WHERE id IN (:ids)")
    fun deletePermanently(ids: List<Long>)

    @Query("DELETE FROM timers WHERE isDeleted = 1")
    fun emptyRecycleBin()

    @Query("DELETE FROM timers WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :threshold")
    fun purgeExpired(threshold: Long)

    @Query("SELECT * FROM timers WHERE isDeleted = 0 AND (isRunning = 1 OR isPaused = 1)")
    fun getRunningOrPausedTimers(): List<TimerEntity>

    @Query("UPDATE timers SET remainingMillis = :remainingMillis, endTimestamp = :endTimestamp, isRunning = :isRunning, isPaused = :isPaused WHERE id = :id")
    fun updateTimerState(id: Long, remainingMillis: Long, endTimestamp: Long, isRunning: Boolean, isPaused: Boolean)

    @Query("UPDATE timers SET orderIndex = :orderIndex WHERE id = :id")
    fun updateOrderIndex(id: Long, orderIndex: Int)
}
