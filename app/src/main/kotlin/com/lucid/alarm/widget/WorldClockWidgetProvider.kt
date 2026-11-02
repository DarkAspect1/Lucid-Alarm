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
import com.lucid.alarm.MainActivity
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.ui.WorldClockLocalization
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WorldClockWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED ||
            intent.action == Intent.ACTION_LOCALE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            WidgetThemeHelper.invalidateCache()
            WidgetUpdater.updateWorldClockWidgets(context.applicationContext)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateInternal(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateInternal(context, appWidgetManager, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val settings = SettingsDataStore(context.applicationContext)
        val savedZones = runCatching { settings.worldClockZonesFlow.first() }.getOrDefault(emptyList())
        val is24Hour = runCatching { settings.timeFormatFlow.first().is24Hour }.getOrDefault(false)
        val removeLeadingZero = runCatching { settings.removeLeadingZeroFlow.first() }.getOrDefault(true)
        val isDark = WidgetThemeHelper.isDarkTheme(context)
        val accentColor = WidgetThemeHelper.resolveAccentColor(context)

        val localZone = ZoneId.systemDefault()
        val zones = if (savedZones.isNotEmpty()) {
            savedZones
        } else {
            listOf(localZone.id)
        }

        for (appWidgetId in appWidgetIds) {
            val viewsMap = mapOf(
                SizeF(80f, 35f) to buildViews(context, appWidgetId, WidgetSizeBucket.COMPACT_PILL, zones, savedZones.isNotEmpty(), is24Hour, removeLeadingZero, isDark, accentColor),
                SizeF(170f, 35f) to buildViews(context, appWidgetId, WidgetSizeBucket.WIDE_BAR, zones, savedZones.isNotEmpty(), is24Hour, removeLeadingZero, isDark, accentColor),
                SizeF(80f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.SQUARE_TILE, zones, savedZones.isNotEmpty(), is24Hour, removeLeadingZero, isDark, accentColor),
                SizeF(175f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.RECTANGLE_3X2, zones, savedZones.isNotEmpty(), is24Hour, removeLeadingZero, isDark, accentColor),
                SizeF(246f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.EXPANDED_CARD, zones, savedZones.isNotEmpty(), is24Hour, removeLeadingZero, isDark, accentColor)
            )

            val remoteViews = RemoteViews(viewsMap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.world_clock_widget_list)
    }

    private fun buildViews(
        context: Context,
        appWidgetId: Int,
        bucket: WidgetSizeBucket,
        zones: List<String>,
        hasSavedZones: Boolean,
        is24Hour: Boolean,
        removeLeadingZero: Boolean,
        isDark: Boolean,
        accentColor: Int
    ): RemoteViews {
        val layoutRes = when (bucket) {
            WidgetSizeBucket.COMPACT_PILL -> R.layout.widget_world_clock_2x1
            WidgetSizeBucket.WIDE_BAR -> R.layout.widget_world_clock_3x1
            WidgetSizeBucket.SQUARE_TILE -> R.layout.widget_world_clock_2x2
            WidgetSizeBucket.RECTANGLE_3X2 -> R.layout.widget_world_clock_3x2
            WidgetSizeBucket.EXPANDED_CARD -> R.layout.widget_world_clock_4x2
        }

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(R.id.widget_root, "setBackgroundResource", WidgetThemeHelper.backgroundRes(isDark))
        views.setInt(R.id.widget_root, "setLayoutDirection", layoutDirection)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NowBarNotification.EXTRA_INITIAL_TAB, NowBarNotification.INITIAL_TAB_WORLD_CLOCK)
        }
        for (code in STALE_REQUEST_CODES) {
            PendingIntent.getActivity(context, code, openAppIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
            PendingIntent.getBroadcast(context, code, Intent(context, WorldClockWidgetProvider::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
        }

        val openAppPi = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_WORLD_CLOCK,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPi)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val secondaryColor = WidgetThemeHelper.secondaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)

        val timeFormatPattern = when {
            is24Hour && removeLeadingZero -> "H:mm"
            is24Hour && !removeLeadingZero -> "HH:mm"
            !is24Hour && removeLeadingZero -> "h:mm a"
            else -> "hh:mm a"
        }
        val nowMillis = System.currentTimeMillis()
        val localZone = ZoneId.systemDefault()

        if (bucket == WidgetSizeBucket.COMPACT_PILL) {
            val primaryZoneId = zones.firstOrNull() ?: localZone.id
            val cityName = WorldClockLocalization.getLocalizedCity(primaryZoneId, localizedContext)
            val zone = runCatching { ZoneId.of(primaryZoneId) }.getOrDefault(localZone)
            val subtitle = WorldClockLocalization.formatWorldClockSubtitle(nowMillis, zone, localZone, localizedContext)

            views.setTextViewText(R.id.city_name, cityName)
            views.setTextColor(R.id.city_name, primaryColor)
            views.setTextViewText(R.id.city_offset, subtitle)
            views.setTextColor(R.id.city_offset, secondaryColor)

            views.setString(R.id.city_clock, "setTimeZone", primaryZoneId)
            views.setCharSequence(R.id.city_clock, "setFormat12Hour", timeFormatPattern)
            views.setCharSequence(R.id.city_clock, "setFormat24Hour", timeFormatPattern)
            views.setTextColor(R.id.city_clock, primaryColor)
        }

        if (bucket == WidgetSizeBucket.WIDE_BAR) {
            val zone1 = zones.getOrNull(0) ?: localZone.id
            val city1 = WorldClockLocalization.getLocalizedCity(zone1, localizedContext)
            val z1 = runCatching { ZoneId.of(zone1) }.getOrDefault(localZone)
            val sub1 = WorldClockLocalization.formatWorldClockSubtitle(nowMillis, z1, localZone, localizedContext)

            views.setTextViewText(R.id.city_name_1, city1)
            views.setTextColor(R.id.city_name_1, primaryColor)
            views.setTextViewText(R.id.city_offset_1, sub1)
            views.setTextColor(R.id.city_offset_1, secondaryColor)
            views.setString(R.id.city_clock_1, "setTimeZone", zone1)
            views.setCharSequence(R.id.city_clock_1, "setFormat12Hour", timeFormatPattern)
            views.setCharSequence(R.id.city_clock_1, "setFormat24Hour", timeFormatPattern)
            views.setTextColor(R.id.city_clock_1, primaryColor)

            val zone2 = zones.getOrNull(1)
            if (zone2 != null) {
                val city2 = WorldClockLocalization.getLocalizedCity(zone2, localizedContext)
                val z2 = runCatching { ZoneId.of(zone2) }.getOrDefault(localZone)
                val sub2 = WorldClockLocalization.formatWorldClockSubtitle(nowMillis, z2, localZone, localizedContext)

                views.setViewVisibility(R.id.city_col_2, View.VISIBLE)
                views.setViewVisibility(R.id.city_divider, View.VISIBLE)
                views.setInt(R.id.city_divider, "setBackgroundResource", WidgetThemeHelper.dividerRes(isDark))

                views.setTextViewText(R.id.city_name_2, city2)
                views.setTextColor(R.id.city_name_2, primaryColor)
                views.setTextViewText(R.id.city_offset_2, sub2)
                views.setTextColor(R.id.city_offset_2, secondaryColor)
                views.setString(R.id.city_clock_2, "setTimeZone", zone2)
                views.setCharSequence(R.id.city_clock_2, "setFormat12Hour", timeFormatPattern)
                views.setCharSequence(R.id.city_clock_2, "setFormat24Hour", timeFormatPattern)
                views.setTextColor(R.id.city_clock_2, primaryColor)
            } else {
                views.setViewVisibility(R.id.city_col_2, View.GONE)
                views.setViewVisibility(R.id.city_divider, View.GONE)
            }
        }

        if (bucket == WidgetSizeBucket.SQUARE_TILE || bucket == WidgetSizeBucket.RECTANGLE_3X2 || bucket == WidgetSizeBucket.EXPANDED_CARD) {
            val headerIconBm = WidgetThemeHelper.renderTintedIconBitmap(context, R.drawable.ic_world_clock, accentColor, 14)
            views.setImageViewBitmap(R.id.world_clock_header_icon, headerIconBm)
            views.setTextViewText(R.id.world_clock_header, localizedContext.getString(R.string.tab_world_clock))
            views.setTextColor(R.id.world_clock_header, mutedColor)

            views.setInt(R.id.world_clock_divider, "setBackgroundResource", WidgetThemeHelper.dividerRes(isDark))

            val serviceIntent = Intent(context, WorldClockRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.world_clock_widget_list, serviceIntent)

            val templatePi = PendingIntent.getActivity(
                context,
                REQUEST_TEMPLATE_WORLD_CLOCK,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.world_clock_widget_list, templatePi)

            if (!hasSavedZones) {
                views.setViewVisibility(R.id.world_clock_widget_list, View.GONE)
                views.setViewVisibility(R.id.world_clock_no_cities, View.VISIBLE)
                views.setTextViewText(R.id.world_clock_no_cities, localizedContext.getString(R.string.no_cities_added))
                views.setTextColor(R.id.world_clock_no_cities, mutedColor)
            } else {
                views.setViewVisibility(R.id.world_clock_widget_list, View.VISIBLE)
                views.setViewVisibility(R.id.world_clock_no_cities, View.GONE)
            }
        }

        return views
    }

    companion object {
        private const val REQUEST_OPEN_WORLD_CLOCK = 34001
        private const val REQUEST_TEMPLATE_WORLD_CLOCK = 34002
        private val STALE_REQUEST_CODES = intArrayOf(10501)
    }
}