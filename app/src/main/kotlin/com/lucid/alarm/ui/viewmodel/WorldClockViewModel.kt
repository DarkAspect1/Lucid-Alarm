package com.lucid.alarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.data.TrashedZone
import com.lucid.alarm.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the world-clock recycle bin. Removing a city moves it here instead of deleting it
 * outright; entries older than [TrashedZone.RECYCLE_BIN_RETENTION_MILLIS] are purged on startup.
 */
class WorldClockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AlarmApplication).worldClockRepository
    private val settingsDataStore = SettingsDataStore(application.applicationContext)

    val trashedZones: StateFlow<List<TrashedZone>> = repository.trashedZones.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch { repository.purgeExpired() }
    }

    /** Moves [zoneIds] to the world-clock recycle bin and removes them from the saved list. */
    fun trashZones(zoneIds: List<String>) {
        viewModelScope.launch {
            if (zoneIds.isEmpty()) return@launch
            settingsDataStore.setWorldClockZones(
                settingsDataStore.worldClockZonesFlow.first().filterNot { it in zoneIds }
            )
            repository.trashZones(zoneIds)
        }
    }

    /** Restores [zoneIds] to the saved list (appended, de-duplicated) and clears them from trash. */
    fun restoreZones(zoneIds: List<String>) {
        viewModelScope.launch {
            if (zoneIds.isEmpty()) return@launch
            val current = settingsDataStore.worldClockZonesFlow.first()
            val restored = zoneIds.filterNot { it in current }
            settingsDataStore.setWorldClockZones(current + restored)
            repository.restoreZones(zoneIds)
        }
    }

    fun deletePermanently(zoneIds: List<String>) {
        viewModelScope.launch { repository.deletePermanently(zoneIds) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }
}
