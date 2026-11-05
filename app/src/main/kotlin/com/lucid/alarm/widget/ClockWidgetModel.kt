package com.lucid.alarm.widget

import android.content.Context
import android.graphics.Color
import com.lucid.alarm.R

enum class ClockStyle(val titleRes: Int) {
    STYLE_1(R.string.clock_widget_style_1),
    STYLE_2(R.string.clock_widget_style_2),
    STYLE_3(R.string.clock_widget_style_3),
    STYLE_4(R.string.clock_widget_style_4),
    STYLE_5(R.string.clock_widget_style_5),
    STYLE_6(R.string.clock_widget_style_6);

    val isAnalog: Boolean
        get() = this == STYLE_3 || this == STYLE_4
}

enum class ClockFontPreset(val titleRes: Int) {
    SYSTEM_DEFAULT(R.string.clock_widget_font_default),
    SANS_SERIF(R.string.clock_widget_font_compact),
    SERIF(R.string.clock_widget_font_serif),
    HEAVY_BOLD(R.string.clock_widget_font_bold),
    MONOSPACE(R.string.clock_widget_font_mono),
    CUSTOM_FILE(R.string.clock_widget_font_custom);

    val systemFontFamilyName: String
        get() = when (this) {
            SYSTEM_DEFAULT -> "sans-serif"
            SANS_SERIF -> "sans-serif-condensed"
            SERIF -> "serif"
            HEAVY_BOLD -> "sans-serif-black"
            MONOSPACE -> "monospace"
            CUSTOM_FILE -> "sans-serif"
        }
}

data class ClockWidgetConfig(
    val style: ClockStyle = ClockStyle.STYLE_1,
    val colorHex: Int = 0xFFFFFFFF.toInt(),
    val fontPreset: ClockFontPreset = ClockFontPreset.SYSTEM_DEFAULT,
    val customFontPath: String = "",
    val scale: Float = 1.0f,
    val showDate: Boolean = true,
    val showAmPm: Boolean = true,
    val clockOffsetX: Float = 0f,
    val clockOffsetY: Float = 0f,
    val clockScale: Float = 1.0f,
    val dateOffsetX: Float = 0f,
    val dateOffsetY: Float = DEFAULT_DATE_OFFSET_Y,
    val dateScale: Float = 1.0f,
    val clockGradientEnabled: Boolean = false,
    val clockGradientEndColorHex: Int = 0xFF5C6BC0.toInt(),
    val dateFontPreset: ClockFontPreset = ClockFontPreset.SYSTEM_DEFAULT,
    val dateCustomFontPath: String = "",
    val dateColorHex: Int = 0xFFFFFFFF.toInt(),
    val dateGradientEnabled: Boolean = false,
    val dateGradientEndColorHex: Int = 0xFF5C6BC0.toInt()
) {
    companion object {
        const val DEFAULT_DATE_OFFSET_Y = 0.25f
        const val PREFS_NAME = "lucid_clock_widget_prefs"
        private const val KEY_STYLE = "style_"
        private const val KEY_COLOR = "color_"
        private const val KEY_FONT = "font_"
        private const val KEY_CUSTOM_FONT = "custom_font_"
        private const val KEY_SCALE = "scale_"
        private const val KEY_SHOW_DATE = "show_date_"
        private const val KEY_SHOW_AMPM = "show_ampm_"
        private const val KEY_CLOCK_OFFSET_X = "clock_offset_x_"
        private const val KEY_CLOCK_OFFSET_Y = "clock_offset_y_"
        private const val KEY_CLOCK_SCALE = "clock_scale_"
        private const val KEY_DATE_OFFSET_X = "date_offset_x_"
        private const val KEY_DATE_OFFSET_Y = "date_offset_y_"
        private const val KEY_DATE_SCALE = "date_scale_"
        private const val KEY_CLOCK_GRADIENT = "clock_gradient_"
        private const val KEY_CLOCK_GRADIENT_END = "clock_gradient_end_"
        private const val KEY_DATE_FONT = "date_font_"
        private const val KEY_DATE_CUSTOM_FONT = "date_custom_font_"
        private const val KEY_DATE_COLOR = "date_color_"
        private const val KEY_DATE_GRADIENT = "date_gradient_"
        private const val KEY_DATE_GRADIENT_END = "date_gradient_end_"

        fun load(context: Context, appWidgetId: Int): ClockWidgetConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val styleName = prefs.getString("$KEY_STYLE$appWidgetId", null)
                ?: prefs.getString("default_style", ClockStyle.STYLE_1.name)
            val style = runCatching { ClockStyle.valueOf(styleName!!) }.getOrDefault(ClockStyle.STYLE_1)

            val color = prefs.getInt("$KEY_COLOR$appWidgetId", prefs.getInt("default_color", Color.WHITE))

            val fontName = prefs.getString("$KEY_FONT$appWidgetId", null)
                ?: prefs.getString("default_font", ClockFontPreset.SYSTEM_DEFAULT.name)
            val fontPreset = runCatching { ClockFontPreset.valueOf(fontName!!) }.getOrDefault(ClockFontPreset.SYSTEM_DEFAULT)

            val customFont = prefs.getString("$KEY_CUSTOM_FONT$appWidgetId", null)
                ?: prefs.getString("default_custom_font", "") ?: ""

            val scale = prefs.getFloat("$KEY_SCALE$appWidgetId", prefs.getFloat("default_scale", 1.0f)).coerceIn(0.2f, 10.0f)
            val showDate = prefs.getBoolean("$KEY_SHOW_DATE$appWidgetId", prefs.getBoolean("default_show_date", true))
            val showAmPm = prefs.getBoolean("$KEY_SHOW_AMPM$appWidgetId", prefs.getBoolean("default_show_ampm", true))

            val clockOffsetX = prefs.getFloat("$KEY_CLOCK_OFFSET_X$appWidgetId", prefs.getFloat("default_clock_offset_x", 0f)).coerceIn(-1f, 1f)
            val clockOffsetY = prefs.getFloat("$KEY_CLOCK_OFFSET_Y$appWidgetId", prefs.getFloat("default_clock_offset_y", 0f)).coerceIn(-1f, 1f)
            val clockScale = prefs.getFloat("$KEY_CLOCK_SCALE$appWidgetId", prefs.getFloat("default_clock_scale", scale)).coerceIn(0.2f, 10.0f)

            val dateOffsetX = prefs.getFloat("$KEY_DATE_OFFSET_X$appWidgetId", prefs.getFloat("default_date_offset_x", 0f)).coerceIn(-1f, 1f)
            val dateOffsetY = prefs.getFloat("$KEY_DATE_OFFSET_Y$appWidgetId", prefs.getFloat("default_date_offset_y", DEFAULT_DATE_OFFSET_Y)).coerceIn(-1f, 1f)
            val dateScale = prefs.getFloat("$KEY_DATE_SCALE$appWidgetId", prefs.getFloat("default_date_scale", 1.0f)).coerceIn(0.2f, 10.0f)

            val clockGradientEnabled = prefs.getBoolean("$KEY_CLOCK_GRADIENT$appWidgetId", prefs.getBoolean("default_clock_gradient", false))
            val clockGradientEndColor = prefs.getInt("$KEY_CLOCK_GRADIENT_END$appWidgetId", prefs.getInt("default_clock_gradient_end", 0xFF5C6BC0.toInt()))

            val dateFontName = prefs.getString("$KEY_DATE_FONT$appWidgetId", null)
                ?: prefs.getString("default_date_font", ClockFontPreset.SYSTEM_DEFAULT.name)
            val dateFontPreset = runCatching { ClockFontPreset.valueOf(dateFontName!!) }.getOrDefault(ClockFontPreset.SYSTEM_DEFAULT)

            val dateCustomFont = prefs.getString("$KEY_DATE_CUSTOM_FONT$appWidgetId", null)
                ?: prefs.getString("default_date_custom_font", "") ?: ""

            val dateColor = prefs.getInt("$KEY_DATE_COLOR$appWidgetId", prefs.getInt("default_date_color", Color.WHITE))

            val dateGradientEnabled = prefs.getBoolean("$KEY_DATE_GRADIENT$appWidgetId", prefs.getBoolean("default_date_gradient", false))
            val dateGradientEndColor = prefs.getInt("$KEY_DATE_GRADIENT_END$appWidgetId", prefs.getInt("default_date_gradient_end", 0xFF5C6BC0.toInt()))

            return ClockWidgetConfig(
                style = style,
                colorHex = color,
                fontPreset = fontPreset,
                customFontPath = customFont,
                scale = scale,
                showDate = showDate,
                showAmPm = showAmPm,
                clockOffsetX = clockOffsetX,
                clockOffsetY = clockOffsetY,
                clockScale = clockScale,
                dateOffsetX = dateOffsetX,
                dateOffsetY = dateOffsetY,
                dateScale = dateScale,
                clockGradientEnabled = clockGradientEnabled,
                clockGradientEndColorHex = clockGradientEndColor,
                dateFontPreset = dateFontPreset,
                dateCustomFontPath = dateCustomFont,
                dateColorHex = dateColor,
                dateGradientEnabled = dateGradientEnabled,
                dateGradientEndColorHex = dateGradientEndColor
            )
        }

        fun save(context: Context, appWidgetId: Int, config: ClockWidgetConfig) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("$KEY_STYLE$appWidgetId", config.style.name)
                .putInt("$KEY_COLOR$appWidgetId", config.colorHex)
                .putString("$KEY_FONT$appWidgetId", config.fontPreset.name)
                .putString("$KEY_CUSTOM_FONT$appWidgetId", config.customFontPath)
                .putFloat("$KEY_SCALE$appWidgetId", config.clockScale)
                .putBoolean("$KEY_SHOW_DATE$appWidgetId", config.showDate)
                .putBoolean("$KEY_SHOW_AMPM$appWidgetId", config.showAmPm)
                .putFloat("$KEY_CLOCK_OFFSET_X$appWidgetId", config.clockOffsetX)
                .putFloat("$KEY_CLOCK_OFFSET_Y$appWidgetId", config.clockOffsetY)
                .putFloat("$KEY_CLOCK_SCALE$appWidgetId", config.clockScale)
                .putFloat("$KEY_DATE_OFFSET_X$appWidgetId", config.dateOffsetX)
                .putFloat("$KEY_DATE_OFFSET_Y$appWidgetId", config.dateOffsetY)
                .putFloat("$KEY_DATE_SCALE$appWidgetId", config.dateScale)
                .putBoolean("$KEY_CLOCK_GRADIENT$appWidgetId", config.clockGradientEnabled)
                .putInt("$KEY_CLOCK_GRADIENT_END$appWidgetId", config.clockGradientEndColorHex)
                .putString("$KEY_DATE_FONT$appWidgetId", config.dateFontPreset.name)
                .putString("$KEY_DATE_CUSTOM_FONT$appWidgetId", config.dateCustomFontPath)
                .putInt("$KEY_DATE_COLOR$appWidgetId", config.dateColorHex)
                .putBoolean("$KEY_DATE_GRADIENT$appWidgetId", config.dateGradientEnabled)
                .putInt("$KEY_DATE_GRADIENT_END$appWidgetId", config.dateGradientEndColorHex)
                .apply()
        }

        fun delete(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("$KEY_STYLE$appWidgetId")
                .remove("$KEY_COLOR$appWidgetId")
                .remove("$KEY_FONT$appWidgetId")
                .remove("$KEY_CUSTOM_FONT$appWidgetId")
                .remove("$KEY_SCALE$appWidgetId")
                .remove("$KEY_SHOW_DATE$appWidgetId")
                .remove("$KEY_SHOW_AMPM$appWidgetId")
                .remove("$KEY_CLOCK_OFFSET_X$appWidgetId")
                .remove("$KEY_CLOCK_OFFSET_Y$appWidgetId")
                .remove("$KEY_CLOCK_SCALE$appWidgetId")
                .remove("$KEY_DATE_OFFSET_X$appWidgetId")
                .remove("$KEY_DATE_OFFSET_Y$appWidgetId")
                .remove("$KEY_DATE_SCALE$appWidgetId")
                .remove("$KEY_CLOCK_GRADIENT$appWidgetId")
                .remove("$KEY_CLOCK_GRADIENT_END$appWidgetId")
                .remove("$KEY_DATE_FONT$appWidgetId")
                .remove("$KEY_DATE_CUSTOM_FONT$appWidgetId")
                .remove("$KEY_DATE_COLOR$appWidgetId")
                .remove("$KEY_DATE_GRADIENT$appWidgetId")
                .remove("$KEY_DATE_GRADIENT_END$appWidgetId")
                .apply()
        }
    }
}
