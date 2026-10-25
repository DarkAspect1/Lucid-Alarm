package com.lucid.alarm.repository

import com.lucid.alarm.data.TrashedZone
import com.lucid.alarm.data.ZoneDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorldClockRepository(private val zoneDao: ZoneDao) {

    val trashedZones: Flow<List<TrashedZone>> = zoneDao.getTrashedZones()

    /** Moves [zoneIds] to the world-clock recycle bin (de-duplicated, keeps newest timestamp). */
    suspend fun trashZones(zoneIds: List<String>) = withContext(Dispatchers.IO) {
        if (zoneIds.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        zoneDao.insertAll(zoneIds.distinct().map { TrashedZone(zoneId = it, deletedAt = now) })
    }

    suspend fun restoreZones(zoneIds: List<String>) = withContext(Dispatchers.IO) {
        if (zoneIds.isNotEmpty()) zoneDao.deleteByIds(zoneIds)
    }

    suspend fun deletePermanently(zoneIds: List<String>) = withContext(Dispatchers.IO) {
        if (zoneIds.isNotEmpty()) zoneDao.deleteByIds(zoneIds)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        zoneDao.emptyTrash()
    }

    /** Purges entries older than the retention window; returns the zone ids removed. */
    suspend fun purgeExpired(): List<String> = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - TrashedZone.RECYCLE_BIN_RETENTION_MILLIS
        val expired = zoneDao.getExpiredTrashedZones(threshold)
        if (expired.isNotEmpty()) {
            val ids = expired.map { it.zoneId }
            zoneDao.deleteByIds(ids)
            ids
        } else {
            emptyList()
        }
    }
}
