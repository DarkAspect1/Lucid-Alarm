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

    // ---- Live alarms (recycle bin excluded) ----

    @Query("SELECT * FROM alarms WHERE deletedAt = 0 ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    /** Live alarms optionally restricted to a single enabled/disabled state. */
    @Query("SELECT * FROM alarms WHERE deletedAt = 0 AND (:enabled IS NULL OR isEnabled = :enabled) ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun getLiveAlarmsByEnabled(enabled: Boolean?): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 AND deletedAt = 0")
    fun getActiveAlarmsSync(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getAlarmById(id: Int): AlarmEntity?

    /** Distinct non-empty groups across all live alarms, used by the edit-sheet group picker. */
    @Query("SELECT DISTINCT groupName FROM alarms WHERE deletedAt = 0 AND groupName != '' ORDER BY groupName ASC")
    fun getDistinctGroups(): Flow<List<String>>

    /** Renames every live alarm's group from [oldName] to [newName]. */
    @Query("UPDATE alarms SET groupName = :newName WHERE deletedAt = 0 AND groupName = :oldName")
    fun renameGroup(oldName: String, newName: String)

    /** Live alarm ids belonging to [groupName], so callers can cancel their PendingIntents. */
    @Query("SELECT id FROM alarms WHERE deletedAt = 0 AND groupName = :groupName")
    fun idsInGroup(groupName: String): List<Int>

    /** Moves every live alarm in [groupName] to the recycle bin, tagged as a restorable group. */
    @Query(
        "UPDATE alarms SET deletedAt = :timestamp, trashedGroup = :groupName " +
            "WHERE deletedAt = 0 AND groupName = :groupName"
    )
    fun trashGroup(groupName: String, timestamp: Long)

    /**
     * Moves every live alarm in [groupNames] to the recycle bin while TAGGING each alarm with its
     * own group name in `trashedGroup`. This is what lets a whole group survive a mixed
     * (groups + individual alarms) deletion as a single restorable Group card in the recycle bin,
     * instead of every member alarm being flattened into a standalone entry.
     */
    @Query(
        "UPDATE alarms SET deletedAt = :timestamp, trashedGroup = groupName " +
            "WHERE deletedAt = 0 AND groupName IN (:groupNames)"
    )
    fun trashGroupsTagged(groupNames: List<String>, timestamp: Long)

    /** Live ids for every alarm whose group is in [groupNames] (for cancelling PendingIntents). */
    @Query("SELECT id FROM alarms WHERE deletedAt = 0 AND groupName IN (:groupNames)")
    fun idsInGroups(groupNames: List<String>): List<Int>

    /**
     * Moves the individually-selected alarms in [ids] to the recycle bin as standalone entries,
     * never overwriting a group tag (the caller only passes alarms NOT belonging to a selected
     * group, so [ids] and the group-tagged rows never overlap).
     */
    @Query(
        "UPDATE alarms SET deletedAt = :timestamp, trashedGroup = '' " +
            "WHERE deletedAt = 0 AND id IN (:ids) AND groupName NOT IN (:excludedGroups)"
    )
    fun trashIndividualExceptGroups(ids: List<Int>, excludedGroups: List<String>, timestamp: Long)

    // ---- Recycle bin ----

    @Query("SELECT * FROM alarms WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun getTrashedAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE deletedAt > 0 AND deletedAt < :threshold")
    fun getExpiredTrashedAlarms(threshold: Long): List<AlarmEntity>

    @Query("UPDATE alarms SET deletedAt = :timestamp, trashedGroup = '' WHERE id IN (:ids)")
    fun moveToTrash(ids: List<Int>, timestamp: Long)

    @Query("UPDATE alarms SET deletedAt = 0, trashedGroup = '' WHERE id IN (:ids)")
    fun restoreFromTrash(ids: List<Int>)

    /** Restores every alarm previously trashed as part of [groupName]. */
    @Query(
        "UPDATE alarms SET deletedAt = 0, trashedGroup = '' " +
            "WHERE deletedAt > 0 AND trashedGroup = :groupName"
    )
    fun restoreTrashedGroup(groupName: String)

    /** Live ids previously trashed as part of [groupName] (for rescheduling on restore). */
    @Query("SELECT id FROM alarms WHERE deletedAt > 0 AND trashedGroup = :groupName")
    fun trashedGroupIds(groupName: String): List<Int>

    /** Permanently deletes every alarm previously trashed as part of [groupName]. */
    @Query("DELETE FROM alarms WHERE deletedAt > 0 AND trashedGroup = :groupName")
    fun deleteTrashedGroupPermanently(groupName: String)

    /** Permanently deletes everything currently in the recycle bin. */
    @Query("DELETE FROM alarms WHERE deletedAt > 0")
    fun emptyTrash()

    // ---- Bulk operations ----

    @Query("UPDATE alarms SET groupName = :groupName WHERE id IN (:ids)")
    fun assignGroup(ids: List<Int>, groupName: String)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id IN (:ids)")
    fun setEnabled(ids: List<Int>, enabled: Boolean)

    @Query("UPDATE alarms SET sortOrder = :order WHERE id = :id")
    fun updateSortOrder(id: Int, order: Int)

    @Query("SELECT MAX(sortOrder) FROM alarms WHERE deletedAt = 0")
    fun maxSortOrder(): Int?

    // ---- CRUD ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(alarm: AlarmEntity): Long

    @Update
    fun update(alarm: AlarmEntity)

    @Delete
    fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Int>)

    // ---- Backup / restore ----

    /** Every alarm (live and trashed) for a full backup snapshot. */
    @Query("SELECT * FROM alarms ORDER BY sortOrder ASC, hour ASC, minute ASC")
    fun getAllAlarmsForBackup(): List<AlarmEntity>

    /** Wipes every alarm so a backup restore can start from a clean slate. */
    @Query("DELETE FROM alarms")
    fun deleteAllAlarms()
}
