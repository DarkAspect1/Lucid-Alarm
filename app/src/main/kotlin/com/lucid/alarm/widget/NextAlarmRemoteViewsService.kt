package com.lucid.alarm.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.R
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.receiver.AlarmScheduler
import com.lucid.alarm.util.TimeLeftFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NextAlarmRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NextAlarmRemoteViewsFactory(applicationContext)
    }
}

class NextAlarmRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var otherAlarms: List<AlarmEntity> = emptyList()
    private var is24Hour: Boolean = false
    private var removeLeadingZero: Boolean = true
    private var isDark: Boolean = false

    override fun onCreate() {
        fetchData()
    }

    override fun onDataSetChanged() {
        fetchData()
    }

    private fun fetchData() {
        val app = context.applicationContext as? AlarmApplication ?: return
        val alarmDao = app.database.alarmDao()
        val settings = SettingsDataStore(context.applicationContext)

        val allAlarms: List<AlarmEntity> = runCatching {
            runBlocking { alarmDao.getAllAlarms().first() }
        }.getOrDefault(emptyList())

        is24Hour = runCatching {
            runBlocking { settings.timeFormatFlow.first().is24Hour }
        }.getOrDefault(false)

        removeLeadingZero = runCatching {
            runBlocking { settings.removeLeadingZeroFlow.first() }
        }.getOrDefault(true)

        isDark = WidgetThemeHelper.isDarkTheme(context)

        val now = System.currentTimeMillis()
        val sortedAlarms = allAlarms
            .filter { it.isEnabled }
            .map { alarm ->
                val nextTrigger = AlarmScheduler.calculateTriggerTimeMillisStatic(
                    hour24 = alarm.hour,
                    minute = alarm.minute,
                    daysOfWeek = alarm.daysOfWeek,
                    weekInterval = alarm.weekInterval,
                    anchorWeekStartEpochDay = alarm.anchorWeekStartEpochDay,
                    afterMillis = now
                )
                alarm to nextTrigger
            }
            .sortedBy { it.second }
            .map { it.first }
            .ifEmpty { allAlarms }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cycleIndex = prefs.getInt(KEY_CYCLE_INDEX, 0)
        val totalAlarms = sortedAlarms.size
        val selectedIndex = if (totalAlarms > 0) Math.floorMod(cycleIndex, totalAlarms) else 0

        otherAlarms = sortedAlarms.filterIndexed { index, _ -> index != selectedIndex }
    }

    override fun onDestroy() {
        otherAlarms = emptyList()
    }

    override fun getCount(): Int = otherAlarms.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= otherAlarms.size) return null

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, R.layout.item_widget_alarm_row)
        views.setInt(R.id.alarm_row_root, "setLayoutDirection", layoutDirection)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val secondaryColor = WidgetThemeHelper.secondaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)

        val alarm = otherAlarms[position]
        val timeFormatted = formatTime(localizedContext, alarm.hour, alarm.minute, is24Hour, removeLeadingZero)
        views.setTextViewText(R.id.alarm_row_time, timeFormatted)
        views.setTextColor(R.id.alarm_row_time, if (alarm.isEnabled) primaryColor else mutedColor)

        val now = System.currentTimeMillis()
        val triggerMillis = if (alarm.isEnabled) {
            AlarmScheduler.calculateTriggerTimeMillisStatic(
                hour24 = alarm.hour,
                minute = alarm.minute,
                daysOfWeek = alarm.daysOfWeek,
                weekInterval = alarm.weekInterval,
                anchorWeekStartEpochDay = alarm.anchorWeekStartEpochDay,
                afterMillis = now
            )
        } else {
            0L
        }

        val countdownText = if (alarm.isEnabled && triggerMillis > now) {
            val diff = triggerMillis - now
            localizedContext.getString(R.string.widget_rings_in, TimeLeftFormatter.humanized(localizedContext, diff))
        } else {
            localizedContext.getString(R.string.setting_off)
        }
        views.setTextViewText(R.id.alarm_row_countdown, countdownText)
        views.setTextColor(R.id.alarm_row_countdown, if (alarm.isEnabled) secondaryColor else mutedColor)

        val daysText = formatDays(localizedContext, alarm.daysOfWeek, isRtl)
        val labelPrefix = if (alarm.label.isNotBlank()) "${alarm.label} • " else ""
        val subText = if (daysText.isNotBlank()) "$labelPrefix$daysText" else alarm.label
        views.setTextViewText(R.id.alarm_row_sub, subText)
        views.setTextColor(R.id.alarm_row_sub, mutedColor)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.alarm_row_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    private fun formatTime(
        context: Context,
        hour: Int,
        minute: Int,
        is24Hour: Boolean,
        removeLeadingZero: Boolean
    ): String {
        return if (is24Hour) {
            if (removeLeadingZero) {
                String.format(Locale.US, "%d:%02d", hour, minute)
            } else {
                String.format(Locale.US, "%02d:%02d", hour, minute)
            }
        } else {
            val h = if (hour % 12 == 0) 12 else hour % 12
            val hStr = if (removeLeadingZero) h.toString() else String.format(Locale.US, "%02d", h)
            val isAm = hour < 12
            val amPm = context.getString(if (isAm) R.string.time_am else R.string.time_pm)
            String.format(Locale.US, "%s:%02d %s", hStr, minute, amPm)
        }
    }

    private fun formatDays(context: Context, days: Set<Int>, isRtl: Boolean): String {
        val weekdays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        val weekends = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val targetLocale = WidgetThemeHelper.getTargetLocale(context)
        return when {
            days.isEmpty() -> context.getString(R.string.repeat_once)
            days.size == 7 -> context.getString(R.string.repeat_every_day)
            days == weekdays -> context.getString(R.string.repeat_weekdays)
            days == weekends -> context.getString(R.string.repeat_weekends)
            else -> days.sorted().joinToString(", ") { dayOfWeek ->
                val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek) }
                SimpleDateFormat("EEE", targetLocale).format(cal.time)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "lucid_alarm_widget_prefs"
        private const val KEY_CYCLE_INDEX = "cycle_index"
    }
}