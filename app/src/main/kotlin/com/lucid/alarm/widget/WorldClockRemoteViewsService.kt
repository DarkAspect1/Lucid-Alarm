package com.lucid.alarm.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.ui.WorldClockLocalization
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WorldClockRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WorldClockRemoteViewsFactory(applicationContext)
    }
}

class WorldClockRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var savedZones: List<String> = emptyList()
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
        val settings = SettingsDataStore(context.applicationContext)
        savedZones = runCatching {
            runBlocking { settings.worldClockZonesFlow.first() }
        }.getOrDefault(emptyList())
        is24Hour = runCatching {
            runBlocking { settings.timeFormatFlow.first().is24Hour }
        }.getOrDefault(false)
        removeLeadingZero = runCatching {
            runBlocking { settings.removeLeadingZeroFlow.first() }
        }.getOrDefault(true)
        isDark = WidgetThemeHelper.isDarkTheme(context)
    }

    override fun onDestroy() {
        savedZones = emptyList()
    }

    override fun getCount(): Int = savedZones.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= savedZones.size) return null

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, R.layout.item_widget_world_clock_row)
        views.setInt(R.id.world_clock_row_root, "setLayoutDirection", layoutDirection)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)

        val zoneId = savedZones[position]
        val localZone = ZoneId.systemDefault()
        val cityName = WorldClockLocalization.getLocalizedCity(zoneId, localizedContext)
        val z = runCatching { ZoneId.of(zoneId) }.getOrDefault(localZone)
        val nowMillis = System.currentTimeMillis()
        val subtitle = WorldClockLocalization.formatWorldClockSubtitle(nowMillis, z, localZone, localizedContext)
        val timeFormatPattern = when {
            is24Hour && removeLeadingZero -> "H:mm"
            is24Hour && !removeLeadingZero -> "HH:mm"
            !is24Hour && removeLeadingZero -> "h:mm a"
            else -> "hh:mm a"
        }

        views.setTextViewText(R.id.city_name, cityName)
        views.setTextColor(R.id.city_name, primaryColor)

        views.setTextViewText(R.id.city_offset, subtitle)
        views.setTextColor(R.id.city_offset, mutedColor)

        views.setString(R.id.city_clock, "setTimeZone", zoneId)
        views.setCharSequence(R.id.city_clock, "setFormat12Hour", timeFormatPattern)
        views.setCharSequence(R.id.city_clock, "setFormat24Hour", timeFormatPattern)
        views.setTextColor(R.id.city_clock, primaryColor)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.world_clock_row_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}