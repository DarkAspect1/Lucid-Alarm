package com.lucid.alarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.lucid.alarm.AlarmApplication
import com.lucid.alarm.MainActivity
import com.lucid.alarm.R
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.receiver.AlarmScheduler
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.util.TimeLeftFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NextAlarmWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CYCLE_ALARM -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getInt(KEY_CYCLE_INDEX, 0)
                prefs.edit().putInt(KEY_CYCLE_INDEX, current + 1).apply()
                WidgetUpdater.updateNextAlarmWidgets(context)
                return
            }
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetThemeHelper.invalidateCache()
                WidgetUpdater.updateNextAlarmWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateInternal(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateInternal(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun updateInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as AlarmApplication
        val alarmDao = app.database.alarmDao()
        val settings = SettingsDataStore(context.applicationContext)

        val allAlarms: List<AlarmEntity> = runCatching {
            runBlocking { alarmDao.getAllAlarms().first() }
        }.getOrDefault(emptyList())

        val is24Hour: Boolean = runCatching {
            runBlocking { settings.timeFormatFlow.first().is24Hour }
        }.getOrDefault(false)

        val removeLeadingZero: Boolean = runCatching {
            runBlocking { settings.removeLeadingZeroFlow.first() }
        }.getOrDefault(true)

        val isDark = WidgetThemeHelper.isDarkTheme(context)
        val accentColor = WidgetThemeHelper.resolveAccentColor(context)

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

        val currentAlarm = sortedAlarms.getOrNull(selectedIndex)
        val currentTrigger = if (currentAlarm != null && currentAlarm.isEnabled) {
            AlarmScheduler.calculateTriggerTimeMillisStatic(
                hour24 = currentAlarm.hour,
                minute = currentAlarm.minute,
                daysOfWeek = currentAlarm.daysOfWeek,
                weekInterval = currentAlarm.weekInterval,
                anchorWeekStartEpochDay = currentAlarm.anchorWeekStartEpochDay,
                afterMillis = now
            )
        } else {
            0L
        }
        val otherAlarms = sortedAlarms.filterIndexed { index, _ -> index != selectedIndex }

        for (appWidgetId in appWidgetIds) {
            val viewsMap = mapOf(
                SizeF(80f, 35f) to buildViews(context, appWidgetId, WidgetSizeBucket.COMPACT_PILL, currentAlarm, currentTrigger, otherAlarms, is24Hour, removeLeadingZero, isDark, accentColor, selectedIndex, totalAlarms),
                SizeF(170f, 35f) to buildViews(context, appWidgetId, WidgetSizeBucket.WIDE_BAR, currentAlarm, currentTrigger, otherAlarms, is24Hour, removeLeadingZero, isDark, accentColor, selectedIndex, totalAlarms),
                SizeF(80f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.SQUARE_TILE, currentAlarm, currentTrigger, otherAlarms, is24Hour, removeLeadingZero, isDark, accentColor, selectedIndex, totalAlarms),
                SizeF(175f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.RECTANGLE_3X2, currentAlarm, currentTrigger, otherAlarms, is24Hour, removeLeadingZero, isDark, accentColor, selectedIndex, totalAlarms),
                SizeF(246f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.EXPANDED_CARD, currentAlarm, currentTrigger, otherAlarms, is24Hour, removeLeadingZero, isDark, accentColor, selectedIndex, totalAlarms)
            )

            val remoteViews = RemoteViews(viewsMap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.alarm_widget_list)
    }

    private fun buildViews(
        context: Context,
        appWidgetId: Int,
        bucket: WidgetSizeBucket,
        alarm: AlarmEntity?,
        triggerMillis: Long,
        otherAlarms: List<AlarmEntity>,
        is24Hour: Boolean,
        removeLeadingZero: Boolean,
        isDark: Boolean,
        accentColor: Int,
        selectedIndex: Int,
        totalAlarms: Int
    ): RemoteViews {
        val layoutRes = when (bucket) {
            WidgetSizeBucket.COMPACT_PILL -> R.layout.widget_next_alarm_2x1
            WidgetSizeBucket.WIDE_BAR -> R.layout.widget_next_alarm_3x1
            WidgetSizeBucket.SQUARE_TILE -> R.layout.widget_next_alarm_2x2
            WidgetSizeBucket.RECTANGLE_3X2 -> R.layout.widget_next_alarm_3x2
            WidgetSizeBucket.EXPANDED_CARD -> R.layout.widget_next_alarm_4x2
        }

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(R.id.widget_root, "setBackgroundResource", WidgetThemeHelper.backgroundRes(isDark))
        views.setInt(R.id.widget_root, "setLayoutDirection", layoutDirection)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NowBarNotification.EXTRA_INITIAL_TAB, NowBarNotification.INITIAL_TAB_ALARMS)
        }
        for (code in STALE_REQUEST_CODES) {
            PendingIntent.getActivity(context, code, openAppIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
            PendingIntent.getBroadcast(context, code, Intent(context, NextAlarmWidgetProvider::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
        }

        val openAppPi = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_ALARMS,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPi)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val secondaryColor = WidgetThemeHelper.secondaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)
        val pillBg = WidgetThemeHelper.pillBgColor(isDark)

        if (alarm == null) {
            views.setTextViewText(R.id.alarm_time, "--:--")
            views.setTextViewText(R.id.alarm_sub, localizedContext.getString(R.string.widget_no_alarms))
            views.setTextColor(R.id.alarm_time, mutedColor)
            views.setTextColor(R.id.alarm_sub, mutedColor)
            views.setViewVisibility(R.id.alarm_cycle_btn, View.GONE)
            views.setViewVisibility(R.id.alarm_divider, View.GONE)
            views.setViewVisibility(R.id.alarm_widget_list, View.GONE)
            views.setViewVisibility(R.id.alarm_no_other_alarms, View.GONE)
            return views
        }

        val timeFormatted = formatTime(localizedContext, alarm.hour, alarm.minute, is24Hour, removeLeadingZero)
        views.setTextViewText(R.id.alarm_time, timeFormatted)
        views.setTextColor(R.id.alarm_time, if (alarm.isEnabled) primaryColor else mutedColor)

        val countdownText = if (alarm.isEnabled && triggerMillis > System.currentTimeMillis()) {
            val diff = triggerMillis - System.currentTimeMillis()
            localizedContext.getString(R.string.widget_rings_in, TimeLeftFormatter.humanized(localizedContext, diff))
        } else {
            localizedContext.getString(R.string.setting_off)
        }

        val daysText = formatDays(localizedContext, alarm.daysOfWeek, isRtl)
        val labelPrefix = if (alarm.label.isNotBlank()) "${alarm.label} • " else ""
        val subText = if (daysText.isNotBlank()) "$labelPrefix$daysText • $countdownText" else "$labelPrefix$countdownText"
        views.setTextViewText(R.id.alarm_sub, subText)
        views.setTextColor(R.id.alarm_sub, secondaryColor)

        views.setViewVisibility(R.id.alarm_cycle_btn, View.VISIBLE)
        val cycleBm = WidgetThemeHelper.renderPillWithIconBitmap(
            context = context,
            iconRes = R.drawable.ic_widget_cycle,
            bgColor = pillBg,
            iconColor = accentColor,
            widthDp = 30,
            heightDp = 30
        )
        views.setImageViewBitmap(R.id.alarm_cycle_btn, cycleBm)

        val cycleIntent = Intent(context, NextAlarmWidgetProvider::class.java).apply {
            action = ACTION_CYCLE_ALARM
        }
        val cyclePi = PendingIntent.getBroadcast(
            context,
            REQUEST_CYCLE_ALARM,
            cycleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.alarm_cycle_btn, cyclePi)

        if (bucket == WidgetSizeBucket.WIDE_BAR || bucket == WidgetSizeBucket.SQUARE_TILE || bucket == WidgetSizeBucket.RECTANGLE_3X2 || bucket == WidgetSizeBucket.EXPANDED_CARD) {
            val headerIconBm = WidgetThemeHelper.renderTintedIconBitmap(context, R.drawable.ic_alarm, accentColor, 14)
            views.setImageViewBitmap(R.id.alarm_header_icon, headerIconBm)

            val headerText = if (totalAlarms > 1) {
                "${localizedContext.getString(R.string.widget_next_alarm_title)} ${selectedIndex + 1}/$totalAlarms"
            } else {
                localizedContext.getString(R.string.widget_next_alarm_title)
            }
            views.setTextViewText(R.id.alarm_header, headerText)
            views.setTextColor(R.id.alarm_header, mutedColor)
        }

        if (bucket == WidgetSizeBucket.SQUARE_TILE || bucket == WidgetSizeBucket.RECTANGLE_3X2 || bucket == WidgetSizeBucket.EXPANDED_CARD) {
            views.setTextViewText(R.id.alarm_days, if (alarm.label.isNotBlank()) "${alarm.label} • $daysText" else daysText)
            views.setTextColor(R.id.alarm_days, secondaryColor)
            views.setTextViewText(R.id.alarm_sub, countdownText)
            views.setTextColor(R.id.alarm_sub, mutedColor)

            views.setInt(R.id.alarm_divider, "setBackgroundResource", WidgetThemeHelper.dividerRes(isDark))

            val serviceIntent = Intent(context, NextAlarmRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.alarm_widget_list, serviceIntent)

            val templatePi = PendingIntent.getActivity(
                context,
                REQUEST_TEMPLATE_ALARMS,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.alarm_widget_list, templatePi)

            if (otherAlarms.isEmpty()) {
                views.setViewVisibility(R.id.alarm_widget_list, View.GONE)
                if (bucket == WidgetSizeBucket.SQUARE_TILE) {
                    views.setViewVisibility(R.id.alarm_divider, View.GONE)
                    views.setViewVisibility(R.id.alarm_no_other_alarms, View.GONE)
                } else {
                    views.setViewVisibility(R.id.alarm_divider, View.VISIBLE)
                    views.setViewVisibility(R.id.alarm_no_other_alarms, View.VISIBLE)
                    views.setTextViewText(R.id.alarm_no_other_alarms, localizedContext.getString(R.string.widget_no_other_alarms))
                    views.setTextColor(R.id.alarm_no_other_alarms, mutedColor)
                }
            } else {
                views.setViewVisibility(R.id.alarm_widget_list, View.VISIBLE)
                views.setViewVisibility(R.id.alarm_no_other_alarms, View.GONE)
                views.setViewVisibility(R.id.alarm_divider, View.VISIBLE)
            }
        }

        return views
    }

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
        const val ACTION_CYCLE_ALARM = "com.lucid.alarm.widget.CYCLE_ALARM"
        private const val PREFS_NAME = "lucid_alarm_widget_prefs"
        private const val KEY_CYCLE_INDEX = "cycle_index"
        private const val REQUEST_OPEN_ALARMS = 31001
        private const val REQUEST_TEMPLATE_ALARMS = 31002
        private const val REQUEST_CYCLE_ALARM = 31003
        private val STALE_REQUEST_CODES = intArrayOf(10101, 10102)
    }
}