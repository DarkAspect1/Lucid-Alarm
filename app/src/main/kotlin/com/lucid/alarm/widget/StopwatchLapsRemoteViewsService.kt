package com.lucid.alarm.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.lucid.alarm.R
import com.lucid.alarm.service.TimerStopwatchManager
import java.util.Locale

class StopwatchLapsRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StopwatchLapsRemoteViewsFactory(applicationContext)
    }
}

class StopwatchLapsRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var laps: List<Long> = emptyList()
    private var isDark: Boolean = false

    override fun onCreate() {
        fetchData()
    }

    override fun onDataSetChanged() {
        fetchData()
    }

    private fun fetchData() {
        val sw = TimerStopwatchManager.stopwatch.value
        laps = sw.laps
        isDark = WidgetThemeHelper.isDarkTheme(context)
    }

    override fun onDestroy() {
        laps = emptyList()
    }

    override fun getCount(): Int = laps.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= laps.size) return null

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, R.layout.widget_stopwatch_lap_row)
        views.setInt(R.id.lap_row_root, "setLayoutDirection", layoutDirection)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val secondaryColor = WidgetThemeHelper.secondaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)

        val lapNumber = laps.size - position
        val totalTime = laps[position]
        val splitTime = if (position + 1 < laps.size) {
            (totalTime - laps[position + 1]).coerceAtLeast(0L)
        } else {
            totalTime
        }

        views.setTextViewText(R.id.lap_num, localizedContext.getString(R.string.widget_lap_prefix, lapNumber))
        views.setTextColor(R.id.lap_num, secondaryColor)

        views.setTextViewText(R.id.lap_split, "+${formatStopwatchMillis(splitTime)}")
        views.setTextColor(R.id.lap_split, mutedColor)

        views.setTextViewText(R.id.lap_total, formatStopwatchMillis(totalTime))
        views.setTextColor(R.id.lap_total, primaryColor)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.lap_row_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    private fun formatStopwatchMillis(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val minutes = (safe / 60_000L) % 60L
        val seconds = (safe / 1_000L) % 60L
        val centis = (safe % 1_000L) / 10L
        val hours = safe / 3_600_000L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
        }
    }
}