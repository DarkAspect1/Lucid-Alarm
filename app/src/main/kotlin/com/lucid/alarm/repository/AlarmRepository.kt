package com.lucid.alarm.repository

import com.lucid.alarm.data.AlarmDao
import com.lucid.alarm.data.AlarmEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    val trashedAlarms: Flow<List<AlarmEntity>> = alarmDao.getTrashedAlarms()

    fun filterLiveAlarms(enabled: Boolean?, days: Set<Int>): Flow<List<AlarmEntity>> =
        alarmDao.getLiveAlarmsByEnabled(enabled).map { list ->
            if (days.isEmpty()) list
            else list.filter { alarm -> alarm.daysOfWeek.any { it in days } }
        }

    suspend fun insert(alarm: AlarmEntity): Long = withContext(Dispatchers.IO) {
        alarmDao.insert(alarm)
    }

    suspend fun update(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        alarmDao.update(alarm)
    }

    suspend fun delete(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        alarmDao.delete(alarm)
    }

    suspend fun getActiveAlarmsSync(): List<AlarmEntity> = withContext(Dispatchers.IO) {
        alarmDao.getActiveAlarmsSync()
    }

    suspend fun getAlarmById(id: Int): AlarmEntity? = withContext(Dispatchers.IO) {
        alarmDao.getAlarmById(id)
    }

    suspend fun moveToTrash(ids: List<Int>) = withContext(Dispatchers.IO) {
        alarmDao.moveToTrash(ids, System.currentTimeMillis())
    }

    suspend fun renameGroup(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        alarmDao.renameGroup(oldName, newName)
    }

    suspend fun trashGroup(groupName: String): List<Int> = withContext(Dispatchers.IO) {
        val ids = alarmDao.idsInGroup(groupName)
        if (ids.isNotEmpty()) {
            alarmDao.trashGroup(groupName, System.currentTimeMillis())
        }
        ids
    }

    suspend fun trashSelection(
        groupNames: Set<String>,
        ids: List<Int>
    ): List<Int> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val groups = groupNames.toList()
        val groupIds = if (groups.isEmpty()) emptyList() else alarmDao.idsInGroups(groups)
        if (groups.isNotEmpty()) {
            alarmDao.trashGroupsTagged(groups, timestamp)
        }
        val standaloneIds = ids.filterNot { it in groupIds }
        if (standaloneIds.isNotEmpty()) {
            alarmDao.trashIndividualExceptGroups(standaloneIds, groups, timestamp)
        }
        groupIds + standaloneIds
    }

    suspend fun restoreFromTrash(ids: List<Int>) = withContext(Dispatchers.IO) {
        alarmDao.restoreFromTrash(ids)
    }

    suspend fun restoreTrashedGroup(groupName: String): List<Int> = withContext(Dispatchers.IO) {
        val ids = alarmDao.trashedGroupIds(groupName)
        if (ids.isNotEmpty()) {
            alarmDao.restoreTrashedGroup(groupName)
        }
        ids
    }

    suspend fun deleteTrashedGroupPermanently(groupName: String) = withContext(Dispatchers.IO) {
        alarmDao.deleteTrashedGroupPermanently(groupName)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        alarmDao.emptyTrash()
    }

    suspend fun deletePermanently(ids: List<Int>) = withContext(Dispatchers.IO) {
        alarmDao.deleteByIds(ids)
    }

    suspend fun assignGroup(ids: List<Int>, groupName: String) = withContext(Dispatchers.IO) {
        alarmDao.assignGroup(ids, groupName)
    }

    suspend fun setEnabled(ids: List<Int>, enabled: Boolean) = withContext(Dispatchers.IO) {
        alarmDao.setEnabled(ids, enabled)
    }

    suspend fun maxSortOrder(): Int = withContext(Dispatchers.IO) {
        alarmDao.maxSortOrder() ?: 0
    }

    suspend fun updateSortOrder(id: Int, order: Int) = withContext(Dispatchers.IO) {
        alarmDao.updateSortOrder(id, order)
    }

    suspend fun persistOrder(orderedIds: List<Int>) = withContext(Dispatchers.IO) {
        orderedIds.forEachIndexed { index, id -> alarmDao.updateSortOrder(id, index) }
    }

    suspend fun reorderAlarms(orderedIds: List<Int>) = persistOrder(orderedIds)

    suspend fun purgeExpiredTrash(): List<Int> = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - AlarmEntity.RECYCLE_BIN_RETENTION_MILLIS
        val expired = alarmDao.getExpiredTrashedAlarms(threshold)
        if (expired.isNotEmpty()) {
            val ids = expired.map { it.id }
            alarmDao.deleteByIds(ids)
            ids
        } else {
            emptyList()
        }
    }
}
