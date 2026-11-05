package com.lucid.alarm.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object ClockWidgetUpdater {
    const val ACTION_CLOCK_TICK = "com.lucid.alarm.widget.CLOCK_TICK"
    private const val REQUEST_CLOCK_TICK = 36000

    fun updateClockWidget(context: Context, appWidgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        ClockWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
        scheduleTickIfNeeded(context)
    }

    fun updateAllClockWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ClockWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isNotEmpty()) {
            for (id in appWidgetIds) {
                ClockWidgetProvider.updateAppWidget(context, appWidgetManager, id)
            }
            scheduleTickIfNeeded(context)
        }
    }

    private fun scheduleTickIfNeeded(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ClockWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        val needsPeriodicTick = appWidgetIds.any { id ->
            val config = ClockWidgetConfig.load(context, id)
            config.style.isAnalog || config.fontPreset != ClockFontPreset.SYSTEM_DEFAULT || config.customFontPath.isNotBlank()
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ClockWidgetProvider::class.java).apply {
            action = ACTION_CLOCK_TICK
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CLOCK_TICK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!needsPeriodicTick) {
            alarmManager.cancel(pi)
            return
        }

        val now = System.currentTimeMillis()
        val nextMinute = ((now / 60000L) + 1L) * 60000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setWindow(AlarmManager.RTC, nextMinute, 5000L, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pi)
        }
    }
}
