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
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.service.StopwatchService
import com.lucid.alarm.service.StopwatchSessionState
import com.lucid.alarm.service.TimerStopwatchManager
import java.util.Locale

class StopwatchWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STOPWATCH_TOGGLE -> {
                val sw = TimerStopwatchManager.stopwatch.value
                when {
                    sw.running -> StopwatchService.stop(context)
                    sw.elapsedMillis > 0L -> StopwatchService.resume(context)
                    else -> StopwatchService.start(context)
                }
                WidgetUpdater.updateStopwatchWidgets(context)
                return
            }
            ACTION_STOPWATCH_LAP -> {
                val sw = TimerStopwatchManager.stopwatch.value
                if (sw.running) {
                    StopwatchService.lap(context)
                    WidgetUpdater.updateStopwatchWidgets(context)
                }
                return
            }
            ACTION_STOPWATCH_RESET -> {
                StopwatchService.cancel(context)
                WidgetUpdater.updateStopwatchWidgets(context)
                return
            }
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                WidgetThemeHelper.invalidateCache()
                WidgetUpdater.updateStopwatchWidgets(context)
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
        val stopwatchState = TimerStopwatchManager.stopwatch.value
        val isDark = WidgetThemeHelper.isDarkTheme(context)
        val accentColor = WidgetThemeHelper.resolveAccentColor(context)

        for (appWidgetId in appWidgetIds) {
            val viewsMap = mapOf(
                SizeF(80f, 35f) to buildViews(context, appWidgetId, WidgetSizeBucket.COMPACT_PILL, stopwatchState, isDark, accentColor),
                SizeF(170f, 35f) to buildViews(context, appWidgetId, WidgetSizeBucket.WIDE_BAR, stopwatchState, isDark, accentColor),
                SizeF(80f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.SQUARE_TILE, stopwatchState, isDark, accentColor),
                SizeF(175f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.RECTANGLE_3X2, stopwatchState, isDark, accentColor),
                SizeF(246f, 95f) to buildViews(context, appWidgetId, WidgetSizeBucket.EXPANDED_CARD, stopwatchState, isDark, accentColor)
            )

            val remoteViews = RemoteViews(viewsMap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.stopwatch_widget_laps_list)
    }

    private fun buildViews(
        context: Context,
        appWidgetId: Int,
        bucket: WidgetSizeBucket,
        sw: StopwatchSessionState,
        isDark: Boolean,
        accentColor: Int
    ): RemoteViews {
        val layoutRes = when (bucket) {
            WidgetSizeBucket.COMPACT_PILL -> R.layout.widget_stopwatch_2x1
            WidgetSizeBucket.WIDE_BAR -> R.layout.widget_stopwatch_3x1
            WidgetSizeBucket.SQUARE_TILE -> R.layout.widget_stopwatch_2x2
            WidgetSizeBucket.RECTANGLE_3X2 -> R.layout.widget_stopwatch_3x2
            WidgetSizeBucket.EXPANDED_CARD -> R.layout.widget_stopwatch_4x2
        }

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(R.id.widget_root, "setBackgroundResource", WidgetThemeHelper.backgroundRes(isDark))
        views.setInt(R.id.widget_root, "setLayoutDirection", layoutDirection)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NowBarNotification.EXTRA_INITIAL_TAB, NowBarNotification.INITIAL_TAB_STOPWATCH)
        }
        for (code in STALE_REQUEST_CODES) {
            PendingIntent.getActivity(context, code, openAppIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
            PendingIntent.getBroadcast(context, code, Intent(context, StopwatchWidgetProvider::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
        }

        val openAppPi = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_STOPWATCH,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPi)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val secondaryColor = WidgetThemeHelper.secondaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)
        val pillBg = WidgetThemeHelper.pillBgColor(isDark)
        val accentContent = WidgetThemeHelper.accentContentColor(accentColor)

        val toggleIntent = Intent(context, StopwatchWidgetProvider::class.java).apply {
            action = ACTION_STOPWATCH_TOGGLE
        }
        val togglePi = PendingIntent.getBroadcast(
            context,
            REQUEST_TOGGLE_STOPWATCH,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = Intent(context, StopwatchWidgetProvider::class.java).apply {
            action = ACTION_STOPWATCH_RESET
        }
        val resetPi = PendingIntent.getBroadcast(
            context,
            REQUEST_RESET_STOPWATCH,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lapIntent = Intent(context, StopwatchWidgetProvider::class.java).apply {
            action = ACTION_STOPWATCH_LAP
        }
        val lapPi = PendingIntent.getBroadcast(
            context,
            REQUEST_LAP_STOPWATCH,
            lapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (sw.running && sw.startRealtime > 0L) {
            views.setViewVisibility(R.id.stopwatch_chronometer, View.VISIBLE)
            views.setViewVisibility(R.id.stopwatch_text, View.GONE)
            views.setChronometer(R.id.stopwatch_chronometer, sw.startRealtime, null, true)
            views.setChronometerCountDown(R.id.stopwatch_chronometer, false)
            views.setTextColor(R.id.stopwatch_chronometer, primaryColor)
        } else {
            views.setViewVisibility(R.id.stopwatch_chronometer, View.GONE)
            views.setViewVisibility(R.id.stopwatch_text, View.VISIBLE)
            views.setTextViewText(R.id.stopwatch_text, formatStoppedStopwatchTime(sw.elapsedMillis))
            views.setTextColor(R.id.stopwatch_text, primaryColor)
        }

        val isRunning = sw.running
        val hasElapsed = sw.elapsedMillis > 0L
        val toggleIcon = if (isRunning) R.drawable.ic_action_pause else R.drawable.ic_action_play

        val (btnWidthDp, btnHeightDp) = when (bucket) {
            WidgetSizeBucket.SQUARE_TILE -> 44 to 30
            else -> 32 to 32
        }

        val toggleBm = WidgetThemeHelper.renderPillWithIconBitmap(
            context = context,
            iconRes = toggleIcon,
            bgColor = accentColor,
            iconColor = accentContent,
            widthDp = btnWidthDp,
            heightDp = btnHeightDp
        )
        views.setImageViewBitmap(R.id.stopwatch_toggle_btn, toggleBm)
        views.setOnClickPendingIntent(R.id.stopwatch_toggle_btn, togglePi)

        if (isRunning) {
            views.setViewVisibility(R.id.stopwatch_secondary_btn, View.VISIBLE)
            val secBm = WidgetThemeHelper.renderPillWithIconBitmap(
                context = context,
                iconRes = R.drawable.ic_action_lap,
                bgColor = pillBg,
                iconColor = primaryColor,
                widthDp = btnWidthDp,
                heightDp = btnHeightDp
            )
            views.setImageViewBitmap(R.id.stopwatch_secondary_btn, secBm)
            views.setOnClickPendingIntent(R.id.stopwatch_secondary_btn, lapPi)
        } else if (hasElapsed) {
            views.setViewVisibility(R.id.stopwatch_secondary_btn, View.VISIBLE)
            val secBm = WidgetThemeHelper.renderPillWithIconBitmap(
                context = context,
                iconRes = R.drawable.ic_widget_reset,
                bgColor = pillBg,
                iconColor = primaryColor,
                widthDp = btnWidthDp,
                heightDp = btnHeightDp
            )
            views.setImageViewBitmap(R.id.stopwatch_secondary_btn, secBm)
            views.setOnClickPendingIntent(R.id.stopwatch_secondary_btn, resetPi)
        } else {
            views.setViewVisibility(R.id.stopwatch_secondary_btn, View.GONE)
        }

        if (bucket == WidgetSizeBucket.WIDE_BAR || bucket == WidgetSizeBucket.SQUARE_TILE || bucket == WidgetSizeBucket.RECTANGLE_3X2 || bucket == WidgetSizeBucket.EXPANDED_CARD) {
            val headerIconBm = WidgetThemeHelper.renderTintedIconBitmap(context, R.drawable.ic_stopwatch, accentColor, 14)
            views.setImageViewBitmap(R.id.stopwatch_header_icon, headerIconBm)
            views.setTextViewText(R.id.stopwatch_header, localizedContext.getString(R.string.widget_stopwatch_title))
            views.setTextColor(R.id.stopwatch_header, mutedColor)
        }

        if (bucket == WidgetSizeBucket.WIDE_BAR) {
            val lapSub = if (sw.laps.isNotEmpty()) {
                val latestLapNum = sw.laps.size
                val latestLapSplit = sw.latestLapSplit
                "${localizedContext.getString(R.string.widget_lap_prefix, latestLapNum)} • ${formatStopwatchMillis(latestLapSplit)}"
            } else {
                ""
            }
            views.setTextViewText(R.id.stopwatch_lap_sub, lapSub)
            views.setTextColor(R.id.stopwatch_lap_sub, secondaryColor)
        }

        if (bucket == WidgetSizeBucket.EXPANDED_CARD || bucket == WidgetSizeBucket.RECTANGLE_3X2 || bucket == WidgetSizeBucket.SQUARE_TILE) {
            if (bucket == WidgetSizeBucket.EXPANDED_CARD || bucket == WidgetSizeBucket.RECTANGLE_3X2) {
                views.setInt(R.id.stopwatch_divider, "setBackgroundResource", WidgetThemeHelper.dividerRes(isDark))
            }

            val serviceIntent = Intent(context, StopwatchLapsRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.stopwatch_widget_laps_list, serviceIntent)

            val templatePi = PendingIntent.getActivity(
                context,
                REQUEST_TEMPLATE_STOPWATCH,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.stopwatch_widget_laps_list, templatePi)

            if (sw.laps.isEmpty()) {
                views.setViewVisibility(R.id.stopwatch_widget_laps_list, View.GONE)
                if (bucket == WidgetSizeBucket.EXPANDED_CARD || bucket == WidgetSizeBucket.RECTANGLE_3X2) {
                    views.setViewVisibility(R.id.stopwatch_no_laps, View.VISIBLE)
                    views.setTextViewText(R.id.stopwatch_no_laps, localizedContext.getString(R.string.widget_no_laps))
                    views.setTextColor(R.id.stopwatch_no_laps, mutedColor)
                } else {
                    views.setViewVisibility(R.id.stopwatch_lap_sub, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.stopwatch_widget_laps_list, View.VISIBLE)
                if (bucket == WidgetSizeBucket.EXPANDED_CARD || bucket == WidgetSizeBucket.RECTANGLE_3X2) {
                    views.setViewVisibility(R.id.stopwatch_no_laps, View.GONE)
                } else {
                    views.setViewVisibility(R.id.stopwatch_lap_sub, View.GONE)
                }
            }
        }

        return views
    }

    private fun formatStoppedStopwatchTime(elapsedMillis: Long): String {
        val safe = elapsedMillis.coerceAtLeast(0L)
        val hours = safe / 3_600_000L
        val minutes = (safe / 60_000L) % 60L
        val seconds = (safe / 1_000L) % 60L
        val centis = (safe % 1_000L) / 10L
        return when {
            hours > 0L -> String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, centis)
            minutes > 0L -> String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
            else -> String.format(Locale.US, "%02d.%02d", seconds, centis)
        }
    }

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

    companion object {
        const val ACTION_STOPWATCH_TOGGLE = "com.lucid.alarm.widget.STOPWATCH_TOGGLE"
        const val ACTION_STOPWATCH_LAP = "com.lucid.alarm.widget.STOPWATCH_LAP"
        const val ACTION_STOPWATCH_RESET = "com.lucid.alarm.widget.STOPWATCH_RESET"

        private const val REQUEST_OPEN_STOPWATCH = 33001
        private const val REQUEST_TEMPLATE_STOPWATCH = 33002
        private const val REQUEST_TOGGLE_STOPWATCH = 33003
        private const val REQUEST_RESET_STOPWATCH = 33004
        private const val REQUEST_LAP_STOPWATCH = 33005
        private val STALE_REQUEST_CODES = intArrayOf(10301, 10302, 10303, 10304, 10401, 10402, 10403, 10404)
    }
}
