package com.lucid.alarm.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    @Query("SELECT * FROM trashed_zones ORDER BY deletedAt DESC")
    fun getTrashedZones(): Flow<List<TrashedZone>>

    @Query("SELECT * FROM trashed_zones WHERE deletedAt < :threshold")
    fun getExpiredTrashedZones(threshold: Long): List<TrashedZone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(zones: List<TrashedZone>)

    @Query("DELETE FROM trashed_zones WHERE zoneId IN (:ids)")
    fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM trashed_zones")
    fun emptyTrash()

    @Query("SELECT * FROM trashed_zones ORDER BY deletedAt DESC")
    fun getAllTrashedZonesForBackup(): List<TrashedZone>

    @Query("DELETE FROM trashed_zones")
    fun deleteAllTrashedZones()
}
