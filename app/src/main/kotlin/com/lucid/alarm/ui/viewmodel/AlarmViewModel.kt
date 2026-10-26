package com.lucid.alarm.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.receiver.AlarmScheduler
import com.lucid.alarm.util.TimeLeftFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = (application as AlarmApplication).repository
    private val settingsDataStore = SettingsDataStore(appContext)
    private val scheduler = AlarmScheduler(appContext)

    val alarms: StateFlow<List<AlarmEntity>> = repository.allAlarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val trashedAlarms: StateFlow<List<AlarmEntity>> = repository.trashedAlarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _recentlyDisabledRepeating = MutableStateFlow<Set<Int>>(emptySet())
    val recentlyDisabledRepeating: StateFlow<Set<Int>> = _recentlyDisabledRepeating.asStateFlow()

    private fun forgetReenable(ids: Collection<Int>) {
        if (ids.isEmpty()) return
        _recentlyDisabledRepeating.update { it - ids.toSet() }
    }

    init {
        viewModelScope.launch { purgeExpiredTrash() }
    }

    private data class UpcomingConfig(val enabled: Boolean, val minutes: Int)

    private suspend fun upcomingConfig(): UpcomingConfig {
        val enabled = settingsDataStore.upcomingAlarmEnabledFlow.first()
        val minutes = settingsDataStore.rawUpcomingAlarmMinutesFlow.first().coerceIn(0, 60)
        return UpcomingConfig(enabled, minutes)
    }

    private fun effectiveLeadMinutes(alarm: AlarmEntity, config: UpcomingConfig): Int {
        if (!alarm.effectiveUpcomingNotification(config.enabled)) return 0
        return if (alarm.upcomingNotificationLeadMinutes > 0) {
            alarm.upcomingNotificationLeadMinutes
        } else {
            config.minutes
        }
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            val config = upcomingConfig()
            repository.allAlarms.first().forEach { alarm ->
                if (alarm.isEnabled && !alarm.isTrashed) {
                    scheduler.scheduleAlarm(alarm, effectiveLeadMinutes(alarm, config))
                }
            }
        }
    }

    fun addAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val nextOrder = repository.maxSortOrder() + 1
            val id = repository.insert(alarm.copy(sortOrder = nextOrder)).toInt()
            val insertedAlarm = repository.getAlarmById(id)
            if (insertedAlarm != null && insertedAlarm.isEnabled) {
                scheduler.scheduleAlarm(
                    insertedAlarm,
                    effectiveLeadMinutes(insertedAlarm, upcomingConfig())
                )
                showToast(relativeTimeText(insertedAlarm))
            }
        }
    }

    fun updateAlarm(alarm: AlarmEntity) {
        forgetReenable(listOf(alarm.id))
        viewModelScope.launch {
            repository.update(alarm)
            if (alarm.isEnabled) {
                scheduler.scheduleAlarm(alarm, effectiveLeadMinutes(alarm, upcomingConfig()))
                showToast(relativeTimeText(alarm))
            } else {
                scheduler.cancelAlarm(alarm.id)
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        if (!isEnabled && alarm.daysOfWeek.isNotEmpty()) {
            _recentlyDisabledRepeating.update { it + alarm.id }
        } else if (isEnabled) {
            forgetReenable(listOf(alarm.id))
        }
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            repository.update(updatedAlarm)
            if (isEnabled) {
                scheduler.scheduleAlarm(
                    updatedAlarm,
                    effectiveLeadMinutes(updatedAlarm, upcomingConfig())
                )
                showToast(relativeTimeText(updatedAlarm))
            } else {
                scheduler.cancelAlarm(updatedAlarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) = moveToTrash(listOf(alarm.id))

    fun moveToTrash(ids: List<Int>) {
        if (ids.isEmpty()) return
        forgetReenable(ids)
        viewModelScope.launch {
            ids.forEach { scheduler.cancelAlarm(it) }
            repository.moveToTrash(ids)
            showToast("Moved ${ids.size} alarm(s) to recycle bin")
        }
    }

    fun moveSelectionToTrash(groupNames: Set<String>, alarmIds: Set<Int>) {

        val deletableGroups = groupNames.filterNot { it == AlarmEntity.DEFAULT_GROUP }.toSet()
        if (deletableGroups.isEmpty() && alarmIds.isEmpty()) return
        viewModelScope.launch {
            val affected = repository.trashSelection(deletableGroups, alarmIds.toList())
            affected.forEach { scheduler.cancelAlarm(it) }
            val total = affected.size
            showToast("Moved $total alarm(s) to recycle bin")
        }
    }

    fun restoreFromTrash(ids: List<Int>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.restoreFromTrash(ids)
            val config = upcomingConfig()
            ids.forEach { id ->
                repository.getAlarmById(id)?.let { alarm ->
                    if (alarm.isEnabled) {
                        scheduler.scheduleAlarm(alarm, effectiveLeadMinutes(alarm, config))
                    }
                }
            }
            showToast("Restored ${ids.size} alarm(s)")
        }
    }

    fun deletePermanently(ids: List<Int>) {
        if (ids.isEmpty()) return
        forgetReenable(ids)
        viewModelScope.launch {
            repository.deletePermanently(ids)
            showToast("Deleted ${ids.size} alarm(s)")
        }
    }

    fun restoreGroupFromTrash(groupName: String) {
        if (groupName.isEmpty()) return
        viewModelScope.launch {
            val ids = repository.restoreTrashedGroup(groupName)
            val config = upcomingConfig()
            ids.forEach { id ->
                repository.getAlarmById(id)?.let { alarm ->
                    if (alarm.isEnabled) {
                        scheduler.scheduleAlarm(alarm, effectiveLeadMinutes(alarm, config))
                    }
                }
            }
            showToast("Restored group \"$groupName\"")
        }
    }

    fun deleteGroupPermanently(groupName: String) {
        if (groupName.isEmpty()) return
        viewModelScope.launch {
            val ids = trashedAlarms.value
                .filter { it.trashedGroup == groupName }
                .map { it.id }
            ids.forEach { scheduler.cancelAlarm(it) }
            repository.deleteTrashedGroupPermanently(groupName)
            showToast("Deleted group \"$groupName\"")
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val ids = trashedAlarms.value.map { it.id }
            ids.forEach { scheduler.cancelAlarm(it) }
            repository.emptyTrash()
            showToast("Recycle bin emptied")
        }
    }

    fun assignGroup(ids: List<Int>, groupName: String) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.assignGroup(ids, groupName)
            showToast("Moved ${ids.size} alarm(s) to \"$groupName\"")
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        val target = newName.trim()
        if (target.isEmpty() || target == oldName) return
        viewModelScope.launch {
            repository.renameGroup(oldName, target)
            showToast("Renamed group to \"$target\"")
        }
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch {
            val ids = repository.trashGroup(groupName)
            ids.forEach { scheduler.cancelAlarm(it) }
            showToast("Moved ${ids.size} alarm(s) to recycle bin")
        }
    }

    fun setEnabledFor(ids: List<Int>, enabled: Boolean) {
        if (ids.isEmpty()) return
        forgetReenable(ids)
        viewModelScope.launch {
            repository.setEnabled(ids, enabled)
            val config = upcomingConfig()
            ids.forEach { id ->
                val alarm = repository.getAlarmById(id) ?: return@forEach
                if (enabled) {
                    scheduler.scheduleAlarm(alarm, effectiveLeadMinutes(alarm, config))
                } else {
                    scheduler.cancelAlarm(id)
                }
            }
        }
    }

    fun copyAlarms(ids: List<Int>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val config = upcomingConfig()
            var nextOrder = repository.maxSortOrder() + 1
            ids.forEach { id ->
                val source = repository.getAlarmById(id) ?: return@forEach
                val copy = source.copy(
                    id = 0,
                    label = if (source.label.isBlank()) source.label else "${source.label} (copy)",
                    createdAt = System.currentTimeMillis(),
                    sortOrder = nextOrder++
                )
                val newId = repository.insert(copy).toInt()
                repository.getAlarmById(newId)?.let { inserted ->
                    if (inserted.isEnabled) {
                        scheduler.scheduleAlarm(inserted, effectiveLeadMinutes(inserted, config))
                    }
                }
            }
            showToast("Copied ${ids.size} alarm(s)")
        }
    }

    fun reorderAlarms(orderedIds: List<Int>) {
        if (orderedIds.isEmpty()) return
        viewModelScope.launch { repository.persistOrder(orderedIds) }
    }

    fun setSortMode(mode: com.lucid.alarm.data.prefs.AlarmSortMode) {
        viewModelScope.launch { settingsDataStore.setAlarmSortMode(mode) }
    }

    suspend fun purgeExpiredTrash() {
        val expiredIds = repository.purgeExpiredTrash()
        expiredIds.forEach { scheduler.cancelAlarm(it) }
    }

    private fun relativeTimeText(alarm: AlarmEntity): String {
        val triggerAt = scheduler.calculateTriggerTimeMillis(
            hour24 = alarm.hour,
            minute = alarm.minute,
            daysOfWeek = alarm.daysOfWeek
        )
        val diff = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        return if (TimeLeftFormatter.remainingMinutes(diff) <= 0L) {
            "Alarm set for less than a minute from now."
        } else {
            "Alarm set for ${TimeLeftFormatter.humanized(diff)} from now."
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
