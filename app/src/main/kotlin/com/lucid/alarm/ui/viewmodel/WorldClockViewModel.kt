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

    fun trashZones(zoneIds: List<String>) {
        viewModelScope.launch {
            if (zoneIds.isEmpty()) return@launch
            settingsDataStore.setWorldClockZones(
                settingsDataStore.worldClockZonesFlow.first().filterNot { it in zoneIds }
            )
            repository.trashZones(zoneIds)
        }
    }

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
