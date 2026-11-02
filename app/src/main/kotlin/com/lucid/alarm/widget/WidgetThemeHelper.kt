package com.lucid.alarm.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.ThemeMode
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class WidgetSizeBucket {
    COMPACT_PILL,
    WIDE_BAR,
    SQUARE_TILE,
    RECTANGLE_3X2,
    EXPANDED_CARD
}

object WidgetThemeHelper {

    @Volatile
    private var cachedThemeMode: ThemeMode? = null
    @Volatile
    private var cacheTimestamp: Long = 0L

    @Volatile
    private var cachedAccentColor: Int? = null
    @Volatile
    private var accentCacheTimestamp: Long = 0L

    @Volatile
    private var cachedLanguage: String? = null
    @Volatile
    private var langCacheTimestamp: Long = 0L

    fun resolveSizeBucket(widthDp: Int, heightDp: Int): WidgetSizeBucket {
        return when {
            heightDp < 95 -> if (widthDp < 170) WidgetSizeBucket.COMPACT_PILL else WidgetSizeBucket.WIDE_BAR
            widthDp < 175 -> WidgetSizeBucket.SQUARE_TILE
            widthDp in 175..245 -> WidgetSizeBucket.RECTANGLE_3X2
            else -> WidgetSizeBucket.EXPANDED_CARD
        }
    }

    fun isDarkTheme(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val mode = if (cachedThemeMode != null && now - cacheTimestamp < 3000L) {
            cachedThemeMode!!
        } else {
            val fetched = runCatching {
                runBlocking { SettingsDataStore(context.applicationContext).themeModeFlow.first() }
            }.getOrDefault(ThemeMode.SYSTEM)
            cachedThemeMode = fetched
            cacheTimestamp = now
            fetched
        }

        return when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun resolveAccentColor(context: Context): Int {
        val now = System.currentTimeMillis()
        if (cachedAccentColor != null && now - accentCacheTimestamp < 3000L) {
            return cachedAccentColor!!
        }
        val resolved = runCatching {
            runBlocking {
                val settings = SettingsDataStore(context.applicationContext)
                settings.currentNowBarPreferences().accentArgb
            }
        }.getOrDefault(Color.parseColor("#5E5CE6"))
        cachedAccentColor = resolved
        accentCacheTimestamp = now
        return resolved
    }

    fun accentContentColor(accentColor: Int): Int {
        val r = Color.red(accentColor)
        val g = Color.green(accentColor)
        val b = Color.blue(accentColor)
        val lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return if (lum > 0.65) Color.parseColor("#0F172A") else Color.WHITE
    }

    fun getAppLanguage(context: Context): String {
        val now = System.currentTimeMillis()
        return if (cachedLanguage != null && now - langCacheTimestamp < 3000L) {
            cachedLanguage!!
        } else {
            val fetched = runCatching {
                runBlocking { SettingsDataStore(context.applicationContext).appLanguageFlow.first() }
            }.getOrDefault("")
            cachedLanguage = fetched
            langCacheTimestamp = now
            fetched
        }
    }

    fun isRtl(context: Context): Boolean {
        val lang = getAppLanguage(context)
        return if (lang.isNotBlank()) {
            lang == "ar"
        } else {
            val config = context.resources.configuration
            config.layoutDirection == View.LAYOUT_DIRECTION_RTL
        }
    }

    fun getTargetLocale(context: Context): Locale {
        val lang = getAppLanguage(context)
        return if (lang.isNotBlank()) {
            Locale.forLanguageTag(lang)
        } else {
            Locale.getDefault()
        }
    }

    fun getLocalizedContext(context: Context): Context {
        val targetLocale = getTargetLocale(context)
        val config = Configuration(context.resources.configuration).apply {
            setLocale(targetLocale)
            setLayoutDirection(targetLocale)
        }
        return context.createConfigurationContext(config)
    }

    fun invalidateCache() {
        cachedThemeMode = null
        cacheTimestamp = 0L
        cachedAccentColor = null
        accentCacheTimestamp = 0L
        cachedLanguage = null
        langCacheTimestamp = 0L
    }

    fun backgroundRes(isDark: Boolean): Int {
        return if (isDark) R.drawable.bg_widget_frosted_dark else R.drawable.bg_widget_frosted_light
    }

    fun pillBgRes(isDark: Boolean): Int {
        return if (isDark) R.drawable.bg_widget_pill_dark else R.drawable.bg_widget_pill_light
    }

    fun accentPillBgRes(isDark: Boolean): Int {
        return if (isDark) R.drawable.bg_widget_pill_accent_dark else R.drawable.bg_widget_pill_accent_light
    }

    fun dividerRes(isDark: Boolean): Int {
        return if (isDark) R.drawable.bg_widget_divider_dark else R.drawable.bg_widget_divider_light
    }

    fun primaryTextColor(isDark: Boolean): Int {
        return if (isDark) Color.parseColor("#FFFFFFFF") else Color.parseColor("#FF0F172A")
    }

    fun secondaryTextColor(isDark: Boolean): Int {
        return if (isDark) Color.parseColor("#FFE2E8F0") else Color.parseColor("#FF334155")
    }

    fun mutedTextColor(isDark: Boolean): Int {
        return if (isDark) Color.parseColor("#FF94A3B8") else Color.parseColor("#FF64748B")
    }

    fun pillBgColor(isDark: Boolean): Int {
        return if (isDark) Color.parseColor("#FF353642") else Color.parseColor("#FFEEF1F7")
    }

    fun dividerColor(isDark: Boolean): Int {
        return if (isDark) Color.parseColor("#FF353642") else Color.parseColor("#FFE8EBF2")
    }

    fun timerTrackColor(isDark: Boolean): Int {
        return if (isDark) Color.parseColor("#FF353642") else Color.parseColor("#FFEEF1F7")
    }

    fun renderTimerRingBitmap(
        progress: Float,
        isPaused: Boolean,
        isDark: Boolean,
        accentColor: Int,
        sizePx: Int
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(32)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = safeSize * 0.11f
        val radius = (safeSize - strokeWidth) / 2f
        val cx = safeSize / 2f
        val cy = safeSize / 2f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = timerTrackColor(isDark)
        }
        canvas.drawCircle(cx, cy, radius, trackPaint)

        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress > 0f) {
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                color = accentColor
            }
            val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(oval, -90f, 360f * safeProgress, false, arcPaint)
        }

        val glyphColor = accentColor
        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = glyphColor
        }

        if (isPaused || safeProgress <= 0f) {
            val playPath = Path()
            val triRadius = safeSize * 0.18f
            val left = cx - triRadius * 0.7f
            val right = cx + triRadius
            val top = cy - triRadius * 0.85f
            val bottom = cy + triRadius * 0.85f
            playPath.moveTo(left, top)
            playPath.lineTo(right, cy)
            playPath.lineTo(left, bottom)
            playPath.close()
            canvas.drawPath(playPath, glyphPaint)
        } else {
            val barW = safeSize * 0.08f
            val barH = safeSize * 0.30f
            val gap = safeSize * 0.08f
            val top = cy - barH / 2f
            val r = barW / 2f

            val left1 = cx - gap / 2f - barW
            canvas.drawRoundRect(RectF(left1, top, left1 + barW, top + barH), r, r, glyphPaint)

            val left2 = cx + gap / 2f
            canvas.drawRoundRect(RectF(left2, top, left2 + barW, top + barH), r, r, glyphPaint)
        }

        return bitmap
    }

    fun renderPillWithIconBitmap(
        context: Context,
        iconRes: Int,
        bgColor: Int,
        iconColor: Int,
        widthDp: Int,
        heightDp: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).toInt().coerceAtLeast(24)
        val h = (heightDp * density).toInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val radius = h / 2f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = bgColor
        }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, bgPaint)

        val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate()
        if (drawable != null) {
            DrawableCompat.setTint(drawable, iconColor)
            val iconSize = (h * 0.52f).toInt().coerceAtLeast(12)
            val left = (w - iconSize) / 2
            val top = (h - iconSize) / 2
            drawable.setBounds(left, top, left + iconSize, top + iconSize)
            drawable.draw(canvas)
        }

        return bitmap
    }

    fun renderPillWithTextBitmap(
        context: Context,
        text: String,
        bgColor: Int,
        textColor: Int,
        widthDp: Int,
        heightDp: Int,
        textSizeSp: Float = 12f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).toInt().coerceAtLeast(24)
        val h = (heightDp * density).toInt().coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val radius = h / 2f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = bgColor
        }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = textSizeSp * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val y = (h / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, w / 2f, y, textPaint)

        return bitmap
    }

    fun renderTintedIconBitmap(
        context: Context,
        iconRes: Int,
        tintColor: Int,
        sizeDp: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(16)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate()
        if (drawable != null) {
            DrawableCompat.setTint(drawable, tintColor)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        }
        return bitmap
    }
}
