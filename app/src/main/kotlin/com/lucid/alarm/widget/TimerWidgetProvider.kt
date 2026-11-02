package com.lucid.alarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.lucid.alarm.MainActivity
import com.lucid.alarm.R
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.service.TimerService
import com.lucid.alarm.service.TimerSessionState
import com.lucid.alarm.service.TimerStopwatchManager
import java.util.Locale

class TimerWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TIMER_TOGGLE -> {
                val timer = TimerStopwatchManager.timer.value
                when {
                    timer.running -> TimerService.pause(context)
                    timer.secondsLeft > 0 -> TimerService.resume(context)
                    else -> TimerService.start(context, 300)
                }
                WidgetUpdater.updateTimerWidgets(context)
                return
            }
            ACTION_TIMER_RESET -> {
                TimerService.cancel(context)
                WidgetUpdater.updateTimerWidgets(context)
                return
            }
            ACTION_TIMER_START_PRESET -> {
                val seconds = intent.getIntExtra(EXTRA_PRESET_SECONDS, 300)
                val timer = TimerStopwatchManager.timer.value
                if (timer.hasSession) {
                    TimerService.adjust(context, seconds)
                } else {
                    TimerService.start(context, seconds)
                }
                WidgetUpdater.updateTimerWidgets(context)
                return
            }
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                WidgetThemeHelper.invalidateCache()
                WidgetUpdater.updateTimerWidgets(context)
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
        val timerState = TimerStopwatchManager.timer.value
        val isDark = WidgetThemeHelper.isDarkTheme(context)
        val accentColor = WidgetThemeHelper.resolveAccentColor(context)

        for (appWidgetId in appWidgetIds) {
            val viewsMap = mapOf(
                SizeF(80f, 35f) to buildViews(context, WidgetSizeBucket.COMPACT_PILL, timerState, isDark, accentColor),
                SizeF(170f, 35f) to buildViews(context, WidgetSizeBucket.WIDE_BAR, timerState, isDark, accentColor),
                SizeF(80f, 95f) to buildViews(context, WidgetSizeBucket.SQUARE_TILE, timerState, isDark, accentColor),
                SizeF(175f, 95f) to buildViews(context, WidgetSizeBucket.RECTANGLE_3X2, timerState, isDark, accentColor),
                SizeF(246f, 95f) to buildViews(context, WidgetSizeBucket.EXPANDED_CARD, timerState, isDark, accentColor)
            )

            val remoteViews = RemoteViews(viewsMap)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }

    private fun buildViews(
        context: Context,
        bucket: WidgetSizeBucket,
        timer: TimerSessionState,
        isDark: Boolean,
        accentColor: Int
    ): RemoteViews {
        val layoutRes = when (bucket) {
            WidgetSizeBucket.COMPACT_PILL -> R.layout.widget_timer_2x1
            WidgetSizeBucket.WIDE_BAR -> R.layout.widget_timer_3x1
            WidgetSizeBucket.SQUARE_TILE -> R.layout.widget_timer_2x2
            WidgetSizeBucket.RECTANGLE_3X2 -> R.layout.widget_timer_3x2
            WidgetSizeBucket.EXPANDED_CARD -> R.layout.widget_timer_4x2
        }

        val localizedContext = WidgetThemeHelper.getLocalizedContext(context)
        val isRtl = WidgetThemeHelper.isRtl(context)
        val layoutDirection = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(R.id.widget_root, "setBackgroundResource", WidgetThemeHelper.backgroundRes(isDark))
        views.setInt(R.id.widget_root, "setLayoutDirection", layoutDirection)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NowBarNotification.EXTRA_INITIAL_TAB, NowBarNotification.INITIAL_TAB_TIMER)
        }
        for (code in STALE_REQUEST_CODES) {
            PendingIntent.getActivity(context, code, openAppIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
            PendingIntent.getBroadcast(context, code, Intent(context, TimerWidgetProvider::class.java), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.cancel()
        }

        val openAppPi = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_TIMER,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppPi)

        val primaryColor = WidgetThemeHelper.primaryTextColor(isDark)
        val secondaryColor = WidgetThemeHelper.secondaryTextColor(isDark)
        val mutedColor = WidgetThemeHelper.mutedTextColor(isDark)
        val pillBg = WidgetThemeHelper.pillBgColor(isDark)
        val accentContent = WidgetThemeHelper.accentContentColor(accentColor)

        val progress = if (timer.totalSeconds > 0) {
            timer.secondsLeft.toFloat() / timer.totalSeconds.toFloat()
        } else {
            0f
        }

        val ringSizePx = when (bucket) {
            WidgetSizeBucket.COMPACT_PILL -> 110
            WidgetSizeBucket.WIDE_BAR -> 120
            WidgetSizeBucket.SQUARE_TILE -> 132
            WidgetSizeBucket.RECTANGLE_3X2 -> 136
            WidgetSizeBucket.EXPANDED_CARD -> 144
        }

        val ringBm = WidgetThemeHelper.renderTimerRingBitmap(
            progress = progress,
            isPaused = !timer.running && timer.secondsLeft > 0,
            isDark = isDark,
            accentColor = accentColor,
            sizePx = ringSizePx
        )
        views.setImageViewBitmap(R.id.timer_ring_image, ringBm)

        if (timer.running && timer.endRealtime > SystemClock.elapsedRealtime()) {
            views.setViewVisibility(R.id.timer_chronometer, View.VISIBLE)
            views.setViewVisibility(R.id.timer_text, View.GONE)
            views.setChronometer(R.id.timer_chronometer, timer.endRealtime, null, true)
            views.setTextColor(R.id.timer_chronometer, primaryColor)
        } else {
            views.setViewVisibility(R.id.timer_chronometer, View.GONE)
            views.setViewVisibility(R.id.timer_text, View.VISIBLE)
            val staticText = if (timer.secondsLeft > 0) formatSeconds(timer.secondsLeft) else "00:00"
            views.setTextViewText(R.id.timer_text, staticText)
            views.setTextColor(R.id.timer_text, primaryColor)
        }

        val labelText = when {
            timer.label.isNotBlank() -> timer.label
            timer.running -> localizedContext.getString(R.string.widget_timer_title)
            timer.secondsLeft > 0 -> localizedContext.getString(R.string.status_paused)
            else -> localizedContext.getString(R.string.widget_timer_title)
        }
        views.setTextViewText(R.id.timer_label, labelText)
        views.setTextColor(R.id.timer_label, secondaryColor)

        if (bucket == WidgetSizeBucket.SQUARE_TILE || bucket == WidgetSizeBucket.RECTANGLE_3X2 || bucket == WidgetSizeBucket.EXPANDED_CARD) {
            val headerIconBm = WidgetThemeHelper.renderTintedIconBitmap(context, R.drawable.ic_hourglass, accentColor, 14)
            views.setImageViewBitmap(R.id.timer_header_icon, headerIconBm)
            views.setTextViewText(R.id.timer_header, localizedContext.getString(R.string.widget_timer_title))
            views.setTextColor(R.id.timer_header, mutedColor)
        }

        val actionBtnWidth = when (bucket) {
            WidgetSizeBucket.EXPANDED_CARD -> 46
            WidgetSizeBucket.RECTANGLE_3X2 -> 40
            else -> 36
        }

        val resetBm = WidgetThemeHelper.renderPillWithIconBitmap(
            context = context,
            iconRes = R.drawable.ic_widget_reset,
            bgColor = pillBg,
            iconColor = primaryColor,
            widthDp = actionBtnWidth,
            heightDp = 28
        )
        views.setImageViewBitmap(R.id.timer_reset_btn, resetBm)

        val resetIntent = Intent(context, TimerWidgetProvider::class.java).apply {
            action = ACTION_TIMER_RESET
        }
        val resetPi = PendingIntent.getBroadcast(
            context,
            REQUEST_RESET,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.timer_reset_btn, resetPi)

        val playPauseIcon = if (timer.running) R.drawable.ic_action_pause else R.drawable.ic_action_play
        val playPauseBm = WidgetThemeHelper.renderPillWithIconBitmap(
            context = context,
            iconRes = playPauseIcon,
            bgColor = accentColor,
            iconColor = accentContent,
            widthDp = actionBtnWidth,
            heightDp = 28
        )
        views.setImageViewBitmap(R.id.timer_play_pause_btn, playPauseBm)

        val toggleIntent = Intent(context, TimerWidgetProvider::class.java).apply {
            action = ACTION_TIMER_TOGGLE
        }
        val togglePi = PendingIntent.getBroadcast(
            context,
            REQUEST_TOGGLE,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.timer_play_pause_btn, togglePi)

        val p1 = String.format(Locale.US, localizedContext.getString(R.string.widget_timer_preset_min), 1)
        val p5 = String.format(Locale.US, localizedContext.getString(R.string.widget_timer_preset_min), 5)
        val p10 = String.format(Locale.US, localizedContext.getString(R.string.widget_timer_preset_min), 10)
        val p15 = String.format(Locale.US, localizedContext.getString(R.string.widget_timer_preset_min), 15)
        val p30 = String.format(Locale.US, localizedContext.getString(R.string.widget_timer_preset_min), 30)

        if (bucket == WidgetSizeBucket.SQUARE_TILE) {
            bindPreset(context, views, R.id.timer_preset_1, p1, 60, pillBg, primaryColor, 34, 24)
            bindPreset(context, views, R.id.timer_preset_5, p5, 300, pillBg, primaryColor, 34, 24)
            bindPreset(context, views, R.id.timer_preset_10, p10, 600, pillBg, primaryColor, 38, 24)
        } else if (bucket == WidgetSizeBucket.RECTANGLE_3X2) {
            bindPreset(context, views, R.id.timer_preset_1, p1, 60, pillBg, primaryColor, 31, 24)
            bindPreset(context, views, R.id.timer_preset_5, p5, 300, pillBg, primaryColor, 31, 24)
            bindPreset(context, views, R.id.timer_preset_10, p10, 600, pillBg, primaryColor, 35, 24)
            bindPreset(context, views, R.id.timer_preset_15, p15, 900, pillBg, primaryColor, 35, 24)
        } else if (bucket == WidgetSizeBucket.EXPANDED_CARD) {
            bindPreset(context, views, R.id.timer_preset_1, p1, 60, pillBg, primaryColor, 34, 24)
            bindPreset(context, views, R.id.timer_preset_5, p5, 300, pillBg, primaryColor, 34, 24)
            bindPreset(context, views, R.id.timer_preset_10, p10, 600, pillBg, primaryColor, 38, 24)
            bindPreset(context, views, R.id.timer_preset_15, p15, 900, pillBg, primaryColor, 38, 24)
            bindPreset(context, views, R.id.timer_preset_30, p30, 1800, pillBg, primaryColor, 40, 24)
        }

        return views
    }

    private fun bindPreset(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        label: String,
        seconds: Int,
        pillBg: Int,
        textColor: Int,
        widthDp: Int,
        heightDp: Int
    ) {
        val bm = WidgetThemeHelper.renderPillWithTextBitmap(
            context = context,
            text = label,
            bgColor = pillBg,
            textColor = textColor,
            widthDp = widthDp,
            heightDp = heightDp,
            textSizeSp = 11f
        )
        views.setImageViewBitmap(viewId, bm)

        val presetIntent = Intent(context, TimerWidgetProvider::class.java).apply {
            action = ACTION_TIMER_START_PRESET
            putExtra(EXTRA_PRESET_SECONDS, seconds)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_PRESET_BASE + seconds,
            presetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pi)
    }

    private fun formatSeconds(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    companion object {
        const val ACTION_TIMER_TOGGLE = "com.lucid.alarm.widget.TIMER_TOGGLE"
        const val ACTION_TIMER_RESET = "com.lucid.alarm.widget.TIMER_RESET"
        const val ACTION_TIMER_START_PRESET = "com.lucid.alarm.widget.TIMER_START_PRESET"
        const val EXTRA_PRESET_SECONDS = "extra_preset_seconds"

        private const val REQUEST_OPEN_TIMER = 32001
        private const val REQUEST_TOGGLE = 32002
        private const val REQUEST_RESET = 32003
        private const val REQUEST_PRESET_BASE = 32100
        private val STALE_REQUEST_CODES = intArrayOf(10301, 10302, 10303, 10310)
    }
}
