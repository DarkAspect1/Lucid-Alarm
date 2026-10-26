package com.lucid.alarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms WHERE deletedAt = 0 ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE deletedAt = 0 AND (:enabled IS NULL OR isEnabled = :enabled) ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun getLiveAlarmsByEnabled(enabled: Boolean?): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND deletedAt = 0")
    fun getActiveAlarmsSync(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getAlarmById(id: Int): AlarmEntity?

    @Query("SELECT DISTINCT groupName FROM alarms WHERE deletedAt = 0 AND groupName != '' ORDER BY groupName ASC")
    fun getDistinctGroups(): Flow<List<String>>

    @Query("UPDATE alarms SET groupName = :newName WHERE deletedAt = 0 AND groupName = :oldName")
    fun renameGroup(oldName: String, newName: String)

    @Query("SELECT id FROM alarms WHERE deletedAt = 0 AND groupName = :groupName")
    fun idsInGroup(groupName: String): List<Int>

    @Query(
        "UPDATE alarms SET deletedAt = :timestamp, trashedGroup = :groupName " +
            "WHERE deletedAt = 0 AND groupName = :groupName"
    )
    fun trashGroup(groupName: String, timestamp: Long)

    @Query(
        "UPDATE alarms SET deletedAt = :timestamp, trashedGroup = groupName " +
            "WHERE deletedAt = 0 AND groupName IN (:groupNames)"
    )
    fun trashGroupsTagged(groupNames: List<String>, timestamp: Long)

    @Query("SELECT id FROM alarms WHERE deletedAt = 0 AND groupName IN (:groupNames)")
    fun idsInGroups(groupNames: List<String>): List<Int>

    @Query(
        "UPDATE alarms SET deletedAt = :timestamp, trashedGroup = '' " +
            "WHERE deletedAt = 0 AND id IN (:ids) AND groupName NOT IN (:excludedGroups)"
    )
    fun trashIndividualExceptGroups(ids: List<Int>, excludedGroups: List<String>, timestamp: Long)

    @Query("SELECT * FROM alarms WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun getTrashedAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE deletedAt > 0 AND deletedAt < :threshold")
    fun getExpiredTrashedAlarms(threshold: Long): List<AlarmEntity>

    @Query("UPDATE alarms SET deletedAt = :timestamp, trashedGroup = '' WHERE id IN (:ids)")
    fun moveToTrash(ids: List<Int>, timestamp: Long)

    @Query("UPDATE alarms SET deletedAt = 0, trashedGroup = '' WHERE id IN (:ids)")
    fun restoreFromTrash(ids: List<Int>)

    @Query(
        "UPDATE alarms SET deletedAt = 0, trashedGroup = '' " +
            "WHERE deletedAt > 0 AND trashedGroup = :groupName"
    )
    fun restoreTrashedGroup(groupName: String)

    @Query("SELECT id FROM alarms WHERE deletedAt > 0 AND trashedGroup = :groupName")
    fun trashedGroupIds(groupName: String): List<Int>

    @Query("DELETE FROM alarms WHERE deletedAt > 0 AND trashedGroup = :groupName")
    fun deleteTrashedGroupPermanently(groupName: String)

    @Query("DELETE FROM alarms WHERE deletedAt > 0")
    fun emptyTrash()

    @Query("UPDATE alarms SET groupName = :groupName WHERE id IN (:ids)")
    fun assignGroup(ids: List<Int>, groupName: String)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id IN (:ids)")
    fun setEnabled(ids: List<Int>, enabled: Boolean)

    @Query("UPDATE alarms SET sortOrder = :order WHERE id = :id")
    fun updateSortOrder(id: Int, order: Int)

    @Query("SELECT MAX(sortOrder) FROM alarms WHERE deletedAt = 0")
    fun maxSortOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(alarm: AlarmEntity): Long

    @Update
    fun update(alarm: AlarmEntity)

    @Delete
    fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM alarms ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun getAllAlarmsForBackup(): List<AlarmEntity>

    @Query("DELETE FROM alarms")
    fun deleteAllAlarms()
}
