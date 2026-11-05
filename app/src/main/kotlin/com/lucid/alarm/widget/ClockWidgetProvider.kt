package com.lucid.alarm.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ClockWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ClockWidgetUpdater.ACTION_CLOCK_TICK,
            Intent.ACTION_CONFIGURATION_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                ClockWidgetUpdater.updateAllClockWidgets(context)
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
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            ClockWidgetConfig.delete(context, appWidgetId)
        }
    }

    companion object {
        private const val REQUEST_OPEN_CLOCK_SETTINGS = 35000

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            optionsOverride: Bundle? = null
        ) {
            runCatching {
                val config = ClockWidgetConfig.load(context, appWidgetId)
                val settings = SettingsDataStore(context.applicationContext)
                val is24Hour = runCatching {
                    runBlocking { settings.timeFormatFlow.first().is24Hour }
                }.getOrDefault(false)
                val removeLeadingZero = runCatching {
                    runBlocking { settings.removeLeadingZeroFlow.first() }
                }.getOrDefault(true)

                val layoutRes = when (config.style) {
                    ClockStyle.STYLE_1 -> R.layout.widget_clock_style1
                    ClockStyle.STYLE_2 -> R.layout.widget_clock_style2
                    ClockStyle.STYLE_3 -> R.layout.widget_clock_style3
                    ClockStyle.STYLE_4 -> R.layout.widget_clock_style4
                    ClockStyle.STYLE_5 -> R.layout.widget_clock_style5
                    ClockStyle.STYLE_6 -> R.layout.widget_clock_style6
                }

                val views = RemoteViews(context.packageName, layoutRes)

                val settingsIntent = Intent(context, ClockWidgetSettingsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val settingsPi = PendingIntent.getActivity(
                    context,
                    REQUEST_OPEN_CLOCK_SETTINGS + appWidgetId,
                    settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.clock_widget_root, settingsPi)

                val clockTypeface = resolveClockTypeface(context, config)
                val appTypeface = resolveAppTypeface(context, settings)
                val dateTypeface = resolveDateTypeface(context, config, settings)

                val options = optionsOverride ?: runCatching { appWidgetManager.getAppWidgetOptions(appWidgetId) }.getOrNull()
                val (widthDp, heightDp) = getWidgetSizeDp(context, options)

                val dm = context.resources.displayMetrics
                val density = dm.density
                val rawW = (widthDp * density).toInt().coerceAtLeast(160)
                val rawH = (heightDp * density).toInt().coerceAtLeast(60)
                val maxDim = 500f
                val scale = minOf(1f, maxDim / maxOf(rawW, rawH))
                val widthPx = (rawW * scale).toInt().coerceIn(160, 500)
                val heightPx = (rawH * scale).toInt().coerceIn(60, 500)
                val renderDensity = density * scale

                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                val cal = Calendar.getInstance().apply { time = Date() }
                val targetLocale = WidgetThemeHelper.getTargetLocale(context)
                val dateStr = if (config.showDate) {
                    if (config.style == ClockStyle.STYLE_6) {
                        SimpleDateFormat("EEEE, d MMMM", targetLocale).format(cal.time)
                    } else {
                        SimpleDateFormat("EEE, d MMM", targetLocale).format(cal.time)
                    }
                } else ""

                val amPmStr = if (!is24Hour && config.showAmPm) {
                    val isAm = cal.get(Calendar.AM_PM) == Calendar.AM
                    context.getString(if (isAm) R.string.time_am else R.string.time_pm)
                } else ""

                ClockWidgetRenderer.render(
                    canvas = canvas,
                    widthPx = widthPx.toFloat(),
                    heightPx = heightPx.toFloat(),
                    config = config,
                    cal = cal,
                    is24Hour = is24Hour,
                    removeLeadingZero = removeLeadingZero,
                    clockTypeface = clockTypeface,
                    appTypeface = appTypeface,
                    dateTypeface = dateTypeface,
                    density = renderDensity,
                    amPmStr = amPmStr,
                    dateStr = dateStr
                )

                if (config.style.isAnalog) {
                    views.setImageViewBitmap(R.id.clock_analog_image, bitmap)
                } else {
                    views.setImageViewBitmap(R.id.clock_custom_font_image, bitmap)
                    views.setViewVisibility(R.id.clock_custom_font_image, View.VISIBLE)
                    views.setViewVisibility(R.id.clock_digital_container, View.GONE)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun getWidgetSizeDp(context: Context, options: Bundle?): Pair<Int, Int> {
            if (options == null) return Pair(320, 160)
            val isPortrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
                if (!sizes.isNullOrEmpty()) {
                    val size = if (isPortrait) {
                        sizes.minByOrNull { it.width } ?: sizes.first()
                    } else {
                        sizes.maxByOrNull { it.width } ?: sizes.first()
                    }
                    if (size.width > 0f && size.height > 0f) {
                        return Pair(size.width.toInt().coerceAtLeast(100), size.height.toInt().coerceAtLeast(40))
                    }
                }
            }

            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            val w = if (isPortrait) (if (minW > 0) minW else maxW) else (if (maxW > 0) maxW else minW)
            val h = if (isPortrait) (if (maxH > 0) maxH else minH) else (if (minH > 0) minH else maxH)

            val finalW = if (w > 0) w else 320
            val finalH = if (h > 0) h else 160
            return Pair(finalW, finalH)
        }

        fun resolveClockTypeface(context: Context, config: ClockWidgetConfig): Typeface {
            if (config.fontPreset == ClockFontPreset.CUSTOM_FILE && config.customFontPath.isNotBlank()) {
                val file = File(config.customFontPath)
                if (file.exists()) {
                    val tf = runCatching { Typeface.createFromFile(file) }.getOrNull()
                    if (tf != null) return tf
                }
            }
            return when (config.fontPreset) {
                ClockFontPreset.SYSTEM_DEFAULT -> {
                    if (config.style == ClockStyle.STYLE_5 || config.style == ClockStyle.STYLE_6) {
                        Typeface.create("sans-serif-black", Typeface.BOLD)
                    } else {
                        Typeface.DEFAULT
                    }
                }
                ClockFontPreset.SANS_SERIF -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                ClockFontPreset.SERIF -> Typeface.create("serif", Typeface.NORMAL)
                ClockFontPreset.HEAVY_BOLD -> Typeface.create("sans-serif-black", Typeface.BOLD)
                ClockFontPreset.MONOSPACE -> Typeface.create("monospace", Typeface.NORMAL)
                ClockFontPreset.CUSTOM_FILE -> Typeface.DEFAULT
            }
        }

        fun resolveDateTypeface(context: Context, config: ClockWidgetConfig, settings: SettingsDataStore): Typeface {
            if (config.dateFontPreset == ClockFontPreset.CUSTOM_FILE && config.dateCustomFontPath.isNotBlank()) {
                val file = File(config.dateCustomFontPath)
                if (file.exists()) {
                    val tf = runCatching { Typeface.createFromFile(file) }.getOrNull()
                    if (tf != null) return tf
                }
            }
            return when (config.dateFontPreset) {
                ClockFontPreset.SYSTEM_DEFAULT -> resolveAppTypeface(context, settings)
                ClockFontPreset.SANS_SERIF -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                ClockFontPreset.SERIF -> Typeface.create("serif", Typeface.NORMAL)
                ClockFontPreset.HEAVY_BOLD -> Typeface.create("sans-serif-black", Typeface.BOLD)
                ClockFontPreset.MONOSPACE -> Typeface.create("monospace", Typeface.NORMAL)
                ClockFontPreset.CUSTOM_FILE -> resolveAppTypeface(context, settings)
            }
        }

        fun resolveAppTypeface(context: Context, settings: SettingsDataStore): Typeface {
            val style = runCatching { runBlocking { settings.appFontStyleFlow.first() } }.getOrDefault(AppFontStyle.SYSTEM_DEFAULT)
            val customPath = runCatching { runBlocking { settings.appCustomFontPathFlow.first() } }.getOrDefault("")
            return when (style) {
                AppFontStyle.SYSTEM_DEFAULT -> Typeface.DEFAULT
                AppFontStyle.SERIF -> Typeface.create("serif", Typeface.NORMAL)
                AppFontStyle.CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
                AppFontStyle.CUSTOM_FILE -> {
                    if (customPath.isNotBlank() && File(customPath).exists()) {
                        runCatching { Typeface.createFromFile(customPath) }.getOrNull() ?: Typeface.DEFAULT
                    } else {
                        Typeface.DEFAULT
                    }
                }
            }
        }
    }
}
