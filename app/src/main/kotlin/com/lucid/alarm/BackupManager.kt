package com.lucid.alarm

import android.content.Context
import android.net.Uri
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.AppDatabase
import com.lucid.alarm.data.TrashedZone
import com.lucid.alarm.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Granular backup / restore using Android's Storage Access Framework.
 *
 * A backup is a single JSON document. Each [BackupCategory] maps to one top-level key so the user
 * can choose exactly which slices to write on export and which to apply on import. Settings are
 * stored as typed `[name, typeTag, value]` triples (see [SettingsDataStore.dumpPreferences]) so the
 * preferences round-trip without this class needing to know every key.
 */
object BackupManager {

    private const val FORMAT_VERSION = 1

    /** Which direction the backup dialog is operating in. */
    enum class BackupMode { EXPORT, IMPORT }

    enum class BackupCategory(val key: String, val label: String) {
        SETTINGS("settings", "App settings & appearance"),
        ALARMS("alarms", "Alarms"),
        WORLD_CLOCK("worldClock", "World-clock cities"),
        RECYCLE_BIN("recycleBin", "Recycle bin")
    }

    /** What the UI asked for: which categories to read/write. */
    data class Selection(
        val settings: Boolean = true,
        val alarms: Boolean = true,
        val worldClock: Boolean = true,
        val recycleBin: Boolean = true
    )

    data class ImportResult(
        val applied: List<String>,
        val error: String? = null
    ) {
        val success: Boolean get() = error == null
    }

    // ---- Export -------------------------------------------------------------------------------

    suspend fun export(
        context: Context,
        uri: Uri,
        selection: Selection
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("formatVersion", FORMAT_VERSION)
            root.put("app", "Lucid Clock")
            root.put("exportedAt", System.currentTimeMillis())

            val db = AppDatabase.getDatabase(context)
            val store = SettingsDataStore(context)
            val applied = mutableListOf<String>()

            if (selection.settings) {
                root.put(BackupCategory.SETTINGS.key, dumpSettings(store))
                applied += BackupCategory.SETTINGS.label
            }
            if (selection.alarms || selection.recycleBin) {
                var alarms = db.alarmDao().getAllAlarmsForBackup()
                alarms = if (selection.alarms && !selection.recycleBin) {
                    alarms.filter { !it.isTrashed }
                } else if (selection.recycleBin && !selection.alarms) {
                    alarms.filter { it.isTrashed }
                } else {
                    alarms
                }
                val payload = JSONArray()
                alarms.forEach { payload.put(encodeAlarm(it)) }
                root.put(BackupCategory.ALARMS.key, payload)
                applied += BackupCategory.ALARMS.label
            }
            if (selection.worldClock) {
                root.put(BackupCategory.WORLD_CLOCK.key, dumpWorldClock(store))
                applied += BackupCategory.WORLD_CLOCK.label
            }
            if (selection.recycleBin) {
                root.put(BackupCategory.RECYCLE_BIN.key, dumpTrashedZones(db))
                applied += BackupCategory.RECYCLE_BIN.label
            }

            val writer = context.contentResolver.openOutputStream(uri)
                ?: return@withContext ImportResult(emptyList(), "Unable to open the destination file")
            writer.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ImportResult(applied)
        } catch (e: Exception) {
            ImportResult(emptyList(), e.message ?: "Export failed")
        }
    }

    // ---- Import -------------------------------------------------------------------------------

    suspend fun import(
        context: Context,
        uri: Uri,
        selection: Selection
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: return@withContext ImportResult(emptyList(), "Unable to read the selected file")

            val root = JSONObject(text)
            val db = AppDatabase.getDatabase(context)
            val store = SettingsDataStore(context)
            val applied = mutableListOf<String>()

            if (selection.settings && root.has(BackupCategory.SETTINGS.key)) {
                val entries = parseSettings(root.getJSONArray(BackupCategory.SETTINGS.key))
                store.restorePreferences(entries, replaceAll = true)
                applied += BackupCategory.SETTINGS.label
            }

            val restoreAlarms =
                (selection.alarms || selection.recycleBin) && root.has(BackupCategory.ALARMS.key)
            if (restoreAlarms) {
                var alarms = parseAlarms(root.getJSONArray(BackupCategory.ALARMS.key))
                if (selection.alarms && !selection.recycleBin) {
                    alarms = alarms.filter { !it.isTrashed }
                } else if (selection.recycleBin && !selection.alarms) {
                    alarms = alarms.filter { it.isTrashed }
                }
                // Replace the whole table so ids stay stable and the recycle bin is consistent.
                db.alarmDao().deleteAllAlarms()
                alarms.forEach { db.alarmDao().insert(it.copy(id = 0)) }
                rescheduleAlarms(context, db.alarmDao().getAllAlarmsForBackup())
                applied += BackupCategory.ALARMS.label
            }

            if (selection.worldClock && root.has(BackupCategory.WORLD_CLOCK.key)) {
                val zoneIds = parseWorldClock(root.getJSONArray(BackupCategory.WORLD_CLOCK.key))
                store.restoreWorldClockZones(zoneIds)
                applied += BackupCategory.WORLD_CLOCK.label
            }

            if (selection.recycleBin && root.has(BackupCategory.RECYCLE_BIN.key)) {
                val zones = parseZones(root.getJSONArray(BackupCategory.RECYCLE_BIN.key))
                db.zoneDao().deleteAllTrashedZones()
                if (zones.isNotEmpty()) db.zoneDao().insertAll(zones)
                applied += BackupCategory.RECYCLE_BIN.label
            }

            if (applied.isEmpty()) {
                ImportResult(emptyList(), "Nothing to import from this file")
            } else {
                ImportResult(applied)
            }
        } catch (e: Exception) {
            ImportResult(emptyList(), e.message ?: "Import failed")
        }
    }

    private suspend fun rescheduleAlarms(context: Context, alarms: List<AlarmEntity>) {
        val store = SettingsDataStore(context)
        val globalEnabled = store.upcomingAlarmEnabledFlow.first()
        val lead = store.rawUpcomingAlarmMinutesFlow.first().coerceIn(0, 60)
        val scheduler = com.lucid.alarm.receiver.AlarmScheduler(context)
        alarms.forEach { alarm ->
            if (alarm.isEnabled && !alarm.isTrashed) {
                val effectiveLead = when {
                    !alarm.effectiveUpcomingNotification(globalEnabled) -> 0
                    alarm.upcomingNotificationLeadMinutes > 0 -> alarm.upcomingNotificationLeadMinutes
                    else -> lead
                }
                scheduler.scheduleAlarm(alarm, effectiveLead)
            }
        }
    }

    // ---- Settings (de)serialisation -----------------------------------------------------------

    private fun dumpSettings(store: SettingsDataStore): JSONArray = runCatching {
        kotlinx.coroutines.runBlocking { store.dumpPreferences() }
    }.getOrDefault(emptyList())
        .filterNot { it.first == store.worldClockZonesKeyName }
        .let { entries ->
            val arr = JSONArray()
            entries.forEach { (name, tag, value) ->
                arr.put(JSONArray().apply {
                    put(name)
                    put(tag)
                    put(value)
                })
            }
            arr
        }

    private fun dumpWorldClock(store: SettingsDataStore): JSONArray = runCatching {
        kotlinx.coroutines.runBlocking { store.dumpWorldClockZones() }
    }.getOrDefault(emptyList()).let { zones ->
        val arr = JSONArray()
        zones.forEach { arr.put(it) }
        arr
    }

    private fun dumpTrashedZones(db: AppDatabase): JSONArray = runCatching {
        db.zoneDao().getAllTrashedZonesForBackup()
    }.getOrDefault(emptyList()).let { zones ->
        val arr = JSONArray()
        zones.forEach { arr.put(encodeZone(it)) }
        arr
    }

    private fun parseSettings(arr: JSONArray): List<Triple<String, String, String>> {
        val out = mutableListOf<Triple<String, String, String>>()
        for (i in 0 until arr.length()) {
            val row = arr.getJSONArray(i)
            out += Triple(row.getString(0), row.getString(1), row.optString(2))
        }
        return out
    }

    private fun parseWorldClock(arr: JSONArray): List<String> {
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val id = arr.optString(i)
            if (id.isNotBlank()) out += id
        }
        return out
    }

    // ---- Alarm (de)serialisation --------------------------------------------------------------

    private fun encodeAlarm(a: AlarmEntity): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("hour", a.hour)
        put("minute", a.minute)
        put("label", a.label)
        put("isEnabled", a.isEnabled)
        put("repeatDays", a.repeatDays)
        put("groupName", a.groupName)
        put("soundUri", a.soundUri)
        put("soundTitle", a.soundTitle)
        put("vibrationPattern", a.vibrationPattern)
        put("vibrationEnabled", a.vibrationEnabled)
        put("snoozeEnabled", a.snoozeEnabled)
        put("snoozeMinutes", a.snoozeMinutes)
        put("snoozeLimit", a.snoozeLimit)
        put("areYouAwakeEnabled", a.areYouAwakeEnabled)
        put("areYouAwakeMinutes", a.areYouAwakeMinutes)
        put("upcomingNotificationEnabled", a.upcomingNotificationEnabled)
        put("upcomingNotificationLeadMinutes", a.upcomingNotificationLeadMinutes)
        put("hasCustomAreYouAwake", a.hasCustomAreYouAwake)
        put("hasCustomUpcomingNotification", a.hasCustomUpcomingNotification)
        put("customAppearanceEnabled", a.customAppearanceEnabled)
        put("customBackgroundUri", a.customBackgroundUri)
        put("customBackgroundStyle", a.customBackgroundStyle)
        put("customBackgroundSolidColor", a.customBackgroundSolidColor)
        put("customVideoUri", a.customVideoUri)
        put("customClockColor", a.customClockColor)
        put("customClockOffsetX", a.customClockOffsetX.toDouble())
        put("customClockOffsetY", a.customClockOffsetY.toDouble())
        put("customBoxWidth", a.customBoxWidth.toDouble())
        put("customBoxHeight", a.customBoxHeight.toDouble())
        put("customClockBoxEnabled", a.customClockBoxEnabled)
        put("customClockBoxColor", a.customClockBoxColor)
        put("customClockBoxOpacity", a.customClockBoxOpacity.toDouble())
        put("customClockStyle", a.customClockStyle)
        put("customClockFont", a.customClockFont)
        put("customClockFontPath", a.customClockFontPath)
        put("customDateFont", a.customDateFont)
        put("customDateFontPath", a.customDateFontPath)
        put("customDateVisible", a.customDateVisible)
        put("customDateOffsetX", a.customDateOffsetX.toDouble())
        put("customDateOffsetY", a.customDateOffsetY.toDouble())
        put("customDateScale", a.customDateScale.toDouble())
        put("customRemoveLeadingZero", a.customRemoveLeadingZero)
        put("customBlur", a.customBlur.toDouble())
        put("customDim", a.customDim.toDouble())
        put("customClockColorEnd", a.customClockColorEnd)
        put("customClockGradient", a.customClockGradient)
        put("customClockGradientDir", a.customClockGradientDir)
        put("customDateColor", a.customDateColor)
        put("customDateColorEnd", a.customDateColorEnd)
        put("customDateGradient", a.customDateGradient)
        put("customDateGradientDir", a.customDateGradientDir)
        put("customShowAmPm", a.customShowAmPm ?: JSONObject.NULL)
        put("customAmPmFont", a.customAmPmFont)
        put("customAmPmFontPath", a.customAmPmFontPath)
        put("customAmPmColor", a.customAmPmColor)
        put("customAlarmLabelVisible", a.customAlarmLabelVisible ?: JSONObject.NULL)
        put("customAlarmLabelFont", a.customAlarmLabelFont)
        put("customAlarmLabelFontPath", a.customAlarmLabelFontPath)
        put("customAlarmLabelColor", a.customAlarmLabelColor)
        put("customSnoozeFont", a.customSnoozeFont)
        put("customSnoozeFontPath", a.customSnoozeFontPath)
        put("customSnoozeContainerColor", a.customSnoozeContainerColor)
        put("customSnoozeTextColor", a.customSnoozeTextColor)
        put("customDismissFont", a.customDismissFont)
        put("customDismissFontPath", a.customDismissFontPath)
        put("customDismissContainerColor", a.customDismissContainerColor)
        put("customDismissTextColor", a.customDismissTextColor)
        put("createdAt", a.createdAt)
        put("volumeLevel", a.volumeLevel.toDouble())
        put("sortOrder", a.sortOrder)
        put("deletedAt", a.deletedAt)
        put("trashedGroup", a.trashedGroup)
    }

    private fun parseAlarms(arr: JSONArray): List<AlarmEntity> {
        val out = mutableListOf<AlarmEntity>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += AlarmEntity(
                id = o.optInt("id", 0),
                hour = o.optInt("hour", 0),
                minute = o.optInt("minute", 0),
                label = o.optString("label", ""),
                isEnabled = o.optBoolean("isEnabled", true),
                repeatDays = o.optString("repeatDays", ""),
                groupName = o.optString("groupName", AlarmEntity.DEFAULT_GROUP),
                soundUri = o.optString("soundUri", ""),
                soundTitle = o.optString("soundTitle", "Default"),
                vibrationPattern = o.optString("vibrationPattern", "HEARTBEAT"),
                vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                snoozeEnabled = o.optBoolean("snoozeEnabled", true),
                snoozeMinutes = o.optInt("snoozeMinutes", 0),
                snoozeLimit = o.optInt("snoozeLimit", -1),
                areYouAwakeEnabled = o.optBoolean("areYouAwakeEnabled", false),
                areYouAwakeMinutes = o.optInt("areYouAwakeMinutes", 0),
                upcomingNotificationEnabled = o.optBoolean("upcomingNotificationEnabled", false),
                upcomingNotificationLeadMinutes = o.optInt("upcomingNotificationLeadMinutes", 0),
                hasCustomAreYouAwake = o.optBoolean("hasCustomAreYouAwake", false),
                hasCustomUpcomingNotification = o.optBoolean(
                    "hasCustomUpcomingNotification",
                    false
                ),
                customAppearanceEnabled = o.optBoolean("customAppearanceEnabled", false),
                customBackgroundUri = o.optString("customBackgroundUri", ""),
                customBackgroundStyle = o.optString("customBackgroundStyle", ""),
                customBackgroundSolidColor = o.optLong("customBackgroundSolidColor", 0L),
                customVideoUri = o.optString("customVideoUri", ""),
                customClockColor = o.optLong("customClockColor", 0L),
                customClockOffsetX = o.optDouble("customClockOffsetX", 0.0).toFloat(),
                customClockOffsetY = o.optDouble("customClockOffsetY", 0.0).toFloat(),
                customBoxWidth = o.optDouble("customBoxWidth", 0.0).toFloat(),
                customBoxHeight = o.optDouble("customBoxHeight", 0.0).toFloat(),
                customClockBoxEnabled = o.optBoolean("customClockBoxEnabled", false),
                customClockBoxColor = o.optLong("customClockBoxColor", 0L),
                customClockBoxOpacity = o.optDouble("customClockBoxOpacity", 0.35).toFloat(),
                customClockStyle = o.optString("customClockStyle", ""),
                customClockFont = o.optString("customClockFont", ""),
                customClockFontPath = o.optString("customClockFontPath", ""),
                customDateFont = o.optString("customDateFont", ""),
                customDateFontPath = o.optString("customDateFontPath", ""),
                customDateVisible = o.optBoolean("customDateVisible", true),
                customDateOffsetX = o.optDouble("customDateOffsetX", 0.0).toFloat(),
                customDateOffsetY = o.optDouble("customDateOffsetY", 0.0).toFloat(),
                customDateScale = o.optDouble("customDateScale", 1.0).toFloat(),
                customRemoveLeadingZero = o.optBoolean("customRemoveLeadingZero", false),
                customBlur = o.optDouble("customBlur", -1.0).toFloat(),
                customDim = o.optDouble("customDim", -1.0).toFloat(),
                customClockColorEnd = o.optLong("customClockColorEnd", 0L),
                customClockGradient = o.optBoolean("customClockGradient", false),
                customClockGradientDir = o.optString("customClockGradientDir", ""),
                customDateColor = o.optLong("customDateColor", 0L),
                customDateColorEnd = o.optLong("customDateColorEnd", 0L),
                customDateGradient = o.optBoolean("customDateGradient", false),
                customDateGradientDir = o.optString("customDateGradientDir", ""),
                customShowAmPm = if (o.isNull("customShowAmPm")) null else o.optBoolean("customShowAmPm"),
                customAmPmFont = o.optString("customAmPmFont", ""),
                customAmPmFontPath = o.optString("customAmPmFontPath", ""),
                customAmPmColor = o.optLong("customAmPmColor", 0L),
                customAlarmLabelVisible = if (o.isNull("customAlarmLabelVisible")) {
                    null
                } else {
                    o.optBoolean("customAlarmLabelVisible")
                },
                customAlarmLabelFont = o.optString("customAlarmLabelFont", ""),
                customAlarmLabelFontPath = o.optString("customAlarmLabelFontPath", ""),
                customAlarmLabelColor = o.optLong("customAlarmLabelColor", 0L),
                customSnoozeFont = o.optString("customSnoozeFont", ""),
                customSnoozeFontPath = o.optString("customSnoozeFontPath", ""),
                customSnoozeContainerColor = o.optLong("customSnoozeContainerColor", 0L),
                customSnoozeTextColor = o.optLong("customSnoozeTextColor", 0L),
                customDismissFont = o.optString("customDismissFont", ""),
                customDismissFontPath = o.optString("customDismissFontPath", ""),
                customDismissContainerColor = o.optLong("customDismissContainerColor", 0L),
                customDismissTextColor = o.optLong("customDismissTextColor", 0L),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                volumeLevel = o.optDouble("volumeLevel", 0.85).toFloat(),
                sortOrder = o.optInt("sortOrder", 0),
                deletedAt = o.optLong("deletedAt", 0L),
                trashedGroup = o.optString("trashedGroup", "")
            )
        }
        return out
    }

    // ---- Zone (de)serialisation ---------------------------------------------------------------

    private fun encodeZone(z: TrashedZone): JSONObject = JSONObject().apply {
        put("zoneId", z.zoneId)
        put("deletedAt", z.deletedAt)
    }

    private fun parseZones(arr: JSONArray): List<TrashedZone> {
        val out = mutableListOf<TrashedZone>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += TrashedZone(
                zoneId = o.optString("zoneId"),
                deletedAt = o.optLong("deletedAt", 0L)
            )
        }
        return out
    }
}
