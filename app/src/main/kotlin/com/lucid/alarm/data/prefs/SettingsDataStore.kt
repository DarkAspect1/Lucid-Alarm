package com.lucid.alarm.data.prefs

import android.content.Context
import android.media.RingtoneManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lucid_clock_settings"
)

enum class VolumePolicy(val label: String) {
    UNIFIED("Unified Volume"),
    PER_ALARM("Per-Alarm Volume")
}

/** Whole-app light/dark appearance mode. Persisted under [KEY_THEME_MODE]. */
enum class ThemeMode(val label: String) {
    SYSTEM("Follow System"),
    LIGHT("Light"),
    DARK("Dark")
}

/**
 * Whole-app accent (primary) Color. Deliberately independent from [RingingBackgroundStyle].
 *
 * [argb] is a packed `0xAARRGGBB` value; UI layers convert it to a Compose `Color` so this data
 * layer stays free of UI dependencies. Persisted under [KEY_ACCENT_COLOR].
 */
enum class AccentColor(val label: String, val argb: Long) {
    DEFAULT("Purple", 0xFF5E5CE6L),
    SUNSET("Sunset Ochre", 0xFFC98A4B),
    PINK("Rose Pink", 0xFFD95B6E),
    TURQUOISE("Turquoise", 0xFF56B9A8),
    CYAN("Cyan Sky", 0xFF68B8D7),
    MINT("Fresh Mint", 0xFF52B76D),
    PURPLE("Electric Purple", 0xFF9C67BF),
    BLUE("Cobalt Blue", 0xFF3B82F6),
    AMBER("Amber Gold", 0xFFD9B478L),
    CORAL("Coral Peach", 0xFFFB7185),
    EMERALD("Emerald", 0xFF10B981),
    LAVENDER("Lavender Mist", 0xFFA78BFA),
    CRIMSON("Crimson Red", 0xFFEF4444),
    SILVER("Titanium Silver", 0xFF94A3B8),
    CHAMPAGNE("Champagne Gold", 0xFFFFD166),
    RADIANT_ROSE("Radiant Rose", 0xFFF43F5E),
    ELECTRIC_VIOLET("Electric Violet", 0xFF8B5CF6),
    AURORA_TEAL("Aurora Teal", 0xFF14B8A6),
    SAPPHIRE("Sapphire", 0xFF2563EB),
    NEON_LIME("Neon Lime", 0xFF84CC16),
    HOT_MAGENTA("Hot Magenta", 0xFFEC4899),
    /**
     * User-picked accent. [argb] is only a safe fallback; the real Color lives under
     * [KEY_ACCENT_CUSTOM_COLOR] and is read via [accentCustomColorFlow].
     */
    CUSTOM("Custom", 0xFF5E5CE6L)
}

enum class TemperatureUnit(val label: String, val symbol: String) {
    CELSIUS("Celsius", "°C"),
    FAHRENHEIT("Fahrenheit", "°F")
}

/**
 * Rich whole-app background palette. Persisted under [KEY_BACKGROUND_PALETTE].
 *
 * [AURORA] additionally renders a vertical indigoâ†’cyan gradient at the theme root; every other
 * entry paints its solid [argb] Color.
 */
enum class BackgroundPalette(
    val label: String,
    val argb: Long,
    val isGradient: Boolean = false
) {
    CHARCOAL("Charcoal Gray", 0xFF161618),
    ONYX("Onyx", 0xFF000000),
    MIDNIGHT("Midnight Navy", 0xFF0B0F19),
    SLATE("Deep Slate", 0xFF1E293B),
    FOREST("Pine Forest", 0xFF062817),
    PLUM("Dark Plum", 0xFF1E0E24),
    BURGUNDY("Wine Burgundy", 0xFF240E13),
    TEAL("Deep Oceanic Teal", 0xFF082026),
    OCEAN("Abyss Blue", 0xFF091A2B),
    INDIGO("Night Indigo", 0xFF13112E),
    ESPRESSO("Roasted Espresso", 0xFF1A120E),
    GRAPHITE("Zinc Graphite", 0xFF18181B),
    MOSS("Dark Moss", 0xFF141E15),
    FROST("Nordic Frost", 0xFF0F1923),
    VOLCANIC("Volcanic Ash", 0xFF1F1A1C),
    OBSIDIAN("Obsidian", 0xFF0A0A0C),
    ECLIPSE("Eclipse", 0xFF121019),
    DEEP_PINE("Deep Pine", 0xFF0A1F1A),
    TWILIGHT("Twilight", 0xFF1A1030),
    ASH("Ash Grey", 0xFF2A2A2E),
    ALABASTER("Alabaster", 0xFFF5F5F5),
    PORCELAIN("Porcelain", 0xFFEFF3F6),
    SANDSTONE("Sandstone", 0xFFF3EBDD),
    SAGE("Sage", 0xFFE6F0E6),
    BLUSH("Blush", 0xFFF7ECEC),
    AURORA("Aurora Gradient", 0xFF0B1E3A, isGradient = true),
    /**
     * User-picked background. [argb] is only a safe fallback; the real Color lives under
     * [KEY_BACKGROUND_CUSTOM_COLOR] and is read via [backgroundCustomColorFlow].
     */
    CUSTOM("Custom Color", 0xFF161618)
}

/**
 * Whole-app background rendering style. Persisted separately for Light and Dark mode (see
 * [KEY_DARK_APP_BACKGROUND_STYLE] / [KEY_LIGHT_APP_BACKGROUND_STYLE]) so switching theme mode
 * restores each mode's exact background â€” style, gradient colours AND interactive colours.
 *
 * [PALETTE] keeps the classic behaviour: a flat [BackgroundPalette] colour (or gradient). The
 * remaining styles render the interactive/living backgrounds shared with the alarm customization
 * screen (see [com.lucid.alarm.ui.components.InteractiveBackgrounds]).
 */
enum class AppBackgroundStyle(val label: String) {
    PALETTE("Solid / Palette"),
    STATIC_GRADIENT("Static Gradient"),
    INTERACTIVE_GRADIENT("Interactive Mesh Gradient"),
    STARFIELD("Starfield Particle Drift"),
    AURORA("Interactive Aurora Borealis"),
    FIREFLIES("Interactive Fireflies / Bokeh")
}

/**
 * App-wide font family for general UI (lists, tabs, menus). Deliberately independent from
 * [RingingClockFont], which only affects the ringing-screen clock.
 *
 * Curated built-in options map to system font families that reliably render, plus an option to
 * pick a `.ttf` / `.otf` file at runtime (see [CUSTOM_FILE] + [com.lucid.alarm.ui.FontManager]).
 */
enum class AppFontStyle(val label: String) {
    SYSTEM_DEFAULT("System Modern"),
    SERIF("Editorial Serif"),
    CURSIVE("Minimalist Cursive"),
    CUSTOM_FILE("Custom File (.ttf/.otf/.apk)")
}

enum class TimeFormat(val label: String) {
    TWELVE_HOUR("12-hour"),
    TWENTY_FOUR_HOUR("24-hour");

    val is24Hour: Boolean get() = this == TWENTY_FOUR_HOUR
}

/** Direction of a two-Color clock / date text gradient. */
enum class GradientDirection(val label: String) {
    VERTICAL("Vertical"),
    HORIZONTAL("Horizontal"),
    DIAGONAL("Diagonal"),
    RADIAL("Radial")
}

/**
 * Ringing-screen background. Deliberately independent from [AccentColor]/[BackgroundPalette] so that changing the
 * app theme never resets the alarm screen appearance.
 */
enum class RingingBackgroundStyle(val label: String) {
    DARK_MIDNIGHT("Dark Midnight Gradient"),
    AMOLED_BLACK("Pure AMOLED Black"),
    WARM_SUNRISE("Warm Sunrise"),
    DYNAMIC_FLUID("Dynamic Fluid Gradient"),
    STARFIELD("Starfield Particle Drift"),
    INTERACTIVE_GRADIENT("Interactive Mesh Gradient"),
    AURORA("Interactive Aurora Borealis"),
    FIREFLIES("Interactive Fireflies"),
    SOLID_COLOR("Solid Color"),
    CUSTOM_IMAGE("Custom Image Wallpaper"),
    VIDEO("Custom Video Wallpaper")
}

enum class RingingClockStyle(val label: String) {
    HORIZONTAL_DIGITAL("Horizontal"),
    VERTICAL_DIGITAL("Vertical (Stacked)"),
    BOLD_HORIZONTAL("Bold Horizontal"),
    BOLD_VERTICAL("Bold Vertical"),
    COMPACT_ANALOG("Compact Analog"),
    NUMBERED_ANALOG("Numbered Analog");

    /** True for the two stacked (hour over minute, no colon) digital faces. */
    val isVertical: Boolean
        get() = this == VERTICAL_DIGITAL || this == BOLD_VERTICAL

    /** True for the bold-weight digital faces. */
    val isBold: Boolean
        get() = this == BOLD_HORIZONTAL || this == BOLD_VERTICAL

    /** True for the two analog faces. */
    val isAnalog: Boolean
        get() = this == COMPACT_ANALOG || this == NUMBERED_ANALOG

    /** True for any digital (non-analog) face. */
    val isDigital: Boolean
        get() = !isAnalog
}

/**
 * Alarm ringing clock font. Persisted under its own key ([KEY_ALARM_CLOCK_FONT_STYLE]) so that
 * changing the general app font never affects the ringing clock and vice-versa.
 *
 * Curated built-in options map to system font families that reliably render, plus an option to
 * pick a `.ttf` / `.otf` file at runtime (see [CUSTOM_FILE] + [com.lucid.alarm.ui.FontManager]).
 */
enum class RingingClockFont(val label: String) {
    SYSTEM_DEFAULT("System Modern"),
    SERIF("Editorial Serif"),
    CURSIVE("Minimalist Cursive"),
    CUSTOM_FILE("Custom File (.ttf/.otf/.apk)")
}

/** How the main alarm list is ordered. Persisted under [KEY_ALARM_SORT_MODE]. */
enum class AlarmSortMode(val label: String) {
    TIME("Alarm Time Order"),
    DAY("Alarm Day Order"),
    ENABLED_FIRST("Turned On Only"),
    DISABLED_FIRST("Turned Off Only"),
    CUSTOM("Custom Order")
}

/** How the saved world-clock cities are ordered. Persisted under [KEY_WORLD_CLOCK_SORT]. */
enum class WorldClockSortMode(val label: String) {
    CUSTOM("Custom Order"),
    CITY_AZ("City (Aâ€“Z)"),
    CITY_ZA("City (Zâ€“A)")
}

/**
 * Font for the independent date component on the ringing clock. [MATCH_CLOCK] follows
 * [RingingClockFont]; the rest mirror [AppFontStyle]'s curated built-ins, with [CUSTOM_FILE]
 * loading an independently imported `.ttf` / `.otf` / `.apk` font for the date only.
 */
enum class ClockDateFont(val label: String) {
    MATCH_CLOCK("Match Clock Font"),
    SYSTEM_DEFAULT("System Modern"),
    SERIF("Editorial Serif"),
    CURSIVE("Minimalist Cursive"),
    CUSTOM_FILE("Custom File (.ttf/.otf/.apk)")
}

enum class RingingClockPosition(val label: String) {
    TOP_ALIGNED("Top Aligned"),
    DEAD_CENTER("Dead Center"),
    BOTTOM_THIRD("Bottom Third"),
    FREE_DRAG("Free Drag")
}

enum class UpcomingAlarmInterval(val minutes: Int, val label: String) {
    DISABLED(0, "Disabled"),
    MINUTES_5(5, "5 Minutes Before"),
    MINUTES_10(10, "10 Minutes Before"),
    MINUTES_15(15, "15 Minutes Before"),
    MINUTES_30(30, "30 Minutes Before"),
    HOUR_1(60, "1 Hour Before");

    companion object {
        fun fromMinutes(minutes: Int): UpcomingAlarmInterval {
            return entries.firstOrNull { it.minutes == minutes } ?: DISABLED
        }
    }
}

enum class TimerVibrationPattern(val label: String, val waveform: LongArray) {
    TICK_TICK("Tick-Tick", longArrayOf(0L, 120L, 120L, 120L, 600L)),
    CONTINUOUS_PULSE("Continuous Pulse", longArrayOf(0L, 500L, 300L, 500L, 300L)),
    URGENT_BUZZ("Urgent Buzz", longArrayOf(0L, 250L, 100L, 250L, 100L, 600L, 200L))
}

/**
 * Per-alarm vibration presets played under `VibrationAttributes.USAGE_ALARM` while an alarm rings.
 * Persisted on [com.lucid.alarm.data.AlarmEntity.vibrationPattern] by enum [name].
 */
enum class AlarmVibrationPreset(val label: String, val waveform: LongArray, val repeat: Boolean = false) {
    HEARTBEAT("Heartbeat", longArrayOf(0L, 120L, 100L, 120L, 600L)),
    TICK_TICK("Tick-Tick", longArrayOf(0L, 80L, 80L, 80L)),
    CONTINUOUS("Continuous Buzz", longArrayOf(0L, 400L), repeat = true),
    URGENT_STACCATO("Urgent Staccato", longArrayOf(0L, 100L, 60L, 100L, 60L, 300L, 60L, 300L));

    companion object {
        fun fromName(name: String?): AlarmVibrationPreset =
            entries.firstOrNull { it.name == name } ?: HEARTBEAT
    }
}

class SettingsDataStore(private val context: Context) {

    /**
     * Reads which stored custom-colour profile is active for [prefs].
     *
     * [ThemeMode.LIGHT] uses the light profile, [ThemeMode.DARK] the dark profile, and
     * [ThemeMode.SYSTEM] follows the device's current night mode so "Follow System" still restores
     * the correct per-mode palette. When a mode profile has never been saved, the legacy shared value
     * (if any) is used so existing installs keep their colours.
     */
    private fun activeProfileIsDark(prefs: Preferences): Boolean = when (
        runCatching { ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    ) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
            uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * Resolves a per-mode custom colour: prefers the active mode's key, then the legacy shared key,
     * then [default]. Returns the packed ARGB (Long).
     */
    private fun resolveModeColor(
        prefs: Preferences,
        darkKey: Preferences.Key<Long>,
        lightKey: Preferences.Key<Long>,
        legacyKey: Preferences.Key<Long>?,
        default: Long
    ): Long {
        val primary = if (activeProfileIsDark(prefs)) darkKey else lightKey
        return prefs[primary]
            ?: legacyKey?.let { prefs[it] }
            ?: default
    }

    /** Encodes a list of packed ARGB colours into a compact comma-separated persisted string. */
    private fun encodeColorList(colors: List<Long>): String =
        colors.joinToString(",") { it.toString() }

    /**
     * Decodes a persisted colour list. Falls back to [default] when the stored value is missing or
     * empty, and coerces the result to between 2 and 4 colours so the gradient renderers always have
     * a usable, sane palette.
     */
    private fun decodeColorList(raw: String?, default: List<Long>): List<Long> {
        if (raw.isNullOrBlank()) return default
        val parsed = raw.split(',').mapNotNull { it.trim().toLongOrNull() }
        return when {
            parsed.size < 2 -> default
            parsed.size > 4 -> parsed.take(4)
            else -> parsed
        }
    }

    /** Sensible per-mode default stops for the static gradient background. */
    private fun defaultBackgroundGradientColors(dark: Boolean): List<Long> =
        if (dark) {
            listOf(0xFF0B1E3A, 0xFF0E2C44, 0xFF07242B)
        } else {
            listOf(0xFFFFFFFF, 0xFFDCE7F7, 0xFFC9E3E6)
        }

    /** Sensible per-mode default mesh colours for the interactive gradient background. */
    private fun defaultInteractiveColors(dark: Boolean): List<Long> =
        if (dark) {
            listOf(0xFF3B82F6, 0xFF8B5CF6, 0xFF14B8A6)
        } else {
            listOf(0xFF93C5FD, 0xFFC4B5FD, 0xFF99F6E4)
        }

    /**
     * Selected whole-app accent (primary) Color for the currently-active Light/Dark profile. Each
     * mode keeps its own selection; installs that never picked a per-mode accent fall back to the
     * legacy shared key so the existing choice survives.
     */
    val accentColorFlow: Flow<AccentColor> = context.settingsDataStore.data.map { prefs ->
        val raw = resolveModeAccentName(prefs)
        runCatching { AccentColor.valueOf(raw) }.getOrDefault(AccentColor.DEFAULT)
    }

    /** Prefers the active mode's accent name, then the legacy shared key, then the default. */
    private fun resolveModeAccentName(prefs: Preferences): String {
        val primary = if (activeProfileIsDark(prefs)) {
            KEY_DARK_ACCENT_COLOR
        } else {
            KEY_LIGHT_ACCENT_COLOR
        }
        return prefs[primary] ?: prefs[KEY_ACCENT_COLOR] ?: AccentColor.DEFAULT.name
    }

    /**
     * Packed ARGB used when [AccentColor.CUSTOM] is selected. Defaults to the stock gold so the
     * accent is always a readable Color even before the user has ever picked one. Dark and Light
     * keep independent custom accents.
     */
    val accentCustomColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        resolveModeColor(
            prefs,
            KEY_DARK_ACCENT_CUSTOM_COLOR,
            KEY_LIGHT_ACCENT_CUSTOM_COLOR,
            KEY_ACCENT_CUSTOM_COLOR,
            AccentColor.DEFAULT.argb
        )
    }

    /**
     * Snapshot of the settings the ongoing Timer/Stopwatch notifications need.
     *
     * @param accentArgb effective accent color as a packed ARGB int.
     */
    data class NowBarPreferences(
        val accentArgb: Int,
        val is24Hour: Boolean,
        val removeLeadingZero: Boolean,
        val isDark: Boolean = true
    )

    /**
     * Reads the persisted appearance preferences on demand, in a single DataStore read.
     *
     * The Timer and Stopwatch foreground services each call this on every tick instead of sharing a
     * cached/static accent value, so running both at once can never make one service overwrite the
     * other's accent color. `@VisibleForTesting`-style snapshot, no UI dependencies.
     */
    suspend fun currentNowBarPreferences(): NowBarPreferences {
        val prefs = context.settingsDataStore.data.first()
        val accent = runCatching {
            AccentColor.valueOf(resolveModeAccentName(prefs))
        }.getOrDefault(AccentColor.DEFAULT)
        // Use the custom accent of the currently-active Light/Dark profile so the Now Bar chip
        // matches the equipped theme, exactly like the Appearance screen.
        val customArgb = resolveModeColor(
            prefs,
            KEY_DARK_ACCENT_CUSTOM_COLOR,
            KEY_LIGHT_ACCENT_CUSTOM_COLOR,
            KEY_ACCENT_CUSTOM_COLOR,
            AccentColor.DEFAULT.argb
        )
        val accentArgb = if (accent == AccentColor.CUSTOM) customArgb.toInt() else accent.argb.toInt()
        val timeFormat = runCatching {
            TimeFormat.valueOf(prefs[KEY_TIME_FORMAT] ?: TimeFormat.TWELVE_HOUR.name)
        }.getOrDefault(TimeFormat.TWELVE_HOUR)
        return NowBarPreferences(
            accentArgb = accentArgb,
            is24Hour = timeFormat.is24Hour,
            removeLeadingZero = prefs[KEY_REMOVE_LEADING_ZERO] ?: true,
            isDark = activeProfileIsDark(prefs)
        )
    }

    /** Whole-app theme mode: Follow System (default), Dark, or Light. */
    val themeModeFlow: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    /** Selected whole-app background palette. */
    val backgroundPaletteFlow: Flow<BackgroundPalette> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_BACKGROUND_PALETTE] ?: BackgroundPalette.OBSIDIAN.name
        runCatching { BackgroundPalette.valueOf(raw) }.getOrDefault(BackgroundPalette.OBSIDIAN)
    }

    /**
     * Packed ARGB used when [BackgroundPalette.CUSTOM] is selected. Defaults to obsidian so the
     * background stays a readable Color even before the user has ever picked one. Dark and Light
     * keep independent custom backgrounds.
     */
    val backgroundCustomColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        resolveModeColor(
            prefs,
            KEY_DARK_BACKGROUND_CUSTOM_COLOR,
            KEY_LIGHT_BACKGROUND_CUSTOM_COLOR,
            KEY_BACKGROUND_CUSTOM_COLOR,
            if (activeProfileIsDark(prefs)) BackgroundPalette.OBSIDIAN.argb else 0xFFFFFFFFL
        )
    }

    /**
     * Custom floating navigation-pill Color (0 = theme default). Per Light/Dark mode. Light mode
     * defaults to 0 so the scaffold draws its crisp white pill with a hairline border; dark mode
     * keeps the charcoal default.
     */
    val navBarColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        resolveModeColor(
            prefs,
            KEY_DARK_NAV_BAR_COLOR,
            KEY_LIGHT_NAV_BAR_COLOR,
            KEY_NAV_BAR_COLOR,
            if (activeProfileIsDark(prefs)) 0xFF2D2D2EL else 0L
        )
    }

    /**
     * Custom container Color for settings section cards (0 = default Material surface). Light mode
     * defaults to 0 so cards fall back to pure white instead of a leftover dark fill.
     */
    val settingsCardColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        resolveModeColor(
            prefs,
            KEY_DARK_SETTINGS_CARD_COLOR,
            KEY_LIGHT_SETTINGS_CARD_COLOR,
            KEY_SETTINGS_CARD_COLOR,
            if (activeProfileIsDark(prefs)) 0xFF252527L else 0L
        )
    }

    /** Custom container Color for alarm list cards (0 = default Material surface). */
    val alarmCardColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        resolveModeColor(
            prefs,
            KEY_DARK_ALARM_CARD_COLOR,
            KEY_LIGHT_ALARM_CARD_COLOR,
            KEY_ALARM_CARD_COLOR,
            if (activeProfileIsDark(prefs)) 0xFF252527L else 0L
        )
    }

    /** Custom container Color for world-clock cards (0 = default Material surface). */
    val worldClockCardColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        resolveModeColor(
            prefs,
            KEY_DARK_WORLD_CLOCK_CARD_COLOR,
            KEY_LIGHT_WORLD_CLOCK_CARD_COLOR,
            KEY_WORLD_CLOCK_CARD_COLOR,
            if (activeProfileIsDark(prefs)) 0xFF252527L else 0L
        )
    }

    /** Whole-app background style for the currently-active Light/Dark profile. */
    val appBackgroundStyleFlow: Flow<AppBackgroundStyle> = context.settingsDataStore.data.map { prefs ->
        val dark = activeProfileIsDark(prefs)
        val raw = prefs[if (dark) KEY_DARK_APP_BACKGROUND_STYLE else KEY_LIGHT_APP_BACKGROUND_STYLE]
        runCatching { AppBackgroundStyle.valueOf(raw ?: AppBackgroundStyle.PALETTE.name) }
            .getOrDefault(AppBackgroundStyle.PALETTE)
    }

    val alarmGroupOrderFlow: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_ALARM_GROUP_ORDER]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    /** Gradient stop colours (packed ARGB) for [AppBackgroundStyle.STATIC_GRADIENT], per mode. */
    val backgroundGradientColorsFlow: Flow<List<Long>> = context.settingsDataStore.data.map { prefs ->
        val dark = activeProfileIsDark(prefs)
        val raw = prefs[
            if (dark) KEY_DARK_BACKGROUND_GRADIENT_COLORS else KEY_LIGHT_BACKGROUND_GRADIENT_COLORS
        ]
        decodeColorList(raw, defaultBackgroundGradientColors(dark))
    }

    /** Up to four mesh colours (packed ARGB) for [AppBackgroundStyle.INTERACTIVE_GRADIENT], per mode. */
    val backgroundInteractiveColorsFlow: Flow<List<Long>> = context.settingsDataStore.data.map { prefs ->
        val dark = activeProfileIsDark(prefs)
        val raw = prefs[
            if (dark) KEY_DARK_BACKGROUND_INTERACTIVE_COLORS
            else KEY_LIGHT_BACKGROUND_INTERACTIVE_COLORS
        ]
        decodeColorList(raw, defaultInteractiveColors(dark))
    }

    /** Whether alarm cards show a time-based sky with a unique procedural landscape. */
    val dynamicDayNightAlarmsLandscapes: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_DAY_NIGHT_ALARMS_LANDSCAPES] ?: false
    }

    /** Whether world-clock cards show a live local sky with a unique procedural landscape. */
    val dynamicDayNightWorldClockLandscapes: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_DAY_NIGHT_WORLD_CLOCK_LANDSCAPES] ?: false
    }

    /** Global UI display scale (0.85x compact â€¦ 1.15x large). Applied to the root density. */
    val uiScaleFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_UI_SCALE] ?: DEFAULT_UI_SCALE).coerceIn(UI_SCALE_MIN, UI_SCALE_MAX)
    }

    val appFontStyleFlow: Flow<AppFontStyle> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_APP_FONT_STYLE] ?: AppFontStyle.SYSTEM_DEFAULT.name
        runCatching { AppFontStyle.valueOf(raw) }.getOrDefault(AppFontStyle.SYSTEM_DEFAULT)
    }

    /**
     * Absolute file path of a custom `.ttf` / `.otf` imported for the whole app UI. Independent of
     * [alarmClockCustomFontPathFlow] (which only drives the ringing clock).
     */
    val appCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_APP_CUSTOM_FONT_PATH].orEmpty()
    }

    val timeFormatFlow: Flow<TimeFormat> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_TIME_FORMAT] ?: TimeFormat.TWELVE_HOUR.name
        runCatching { TimeFormat.valueOf(raw) }.getOrDefault(TimeFormat.TWELVE_HOUR)
    }

    // 1. Volume & Audio Control Strategy
    val volumePolicyFlow: Flow<VolumePolicy> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_VOLUME_POLICY] ?: VolumePolicy.UNIFIED.name
        runCatching { VolumePolicy.valueOf(raw) }.getOrDefault(VolumePolicy.UNIFIED)
    }

    val unifiedVolumeFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_UNIFIED_VOLUME] ?: 0.85f).coerceIn(0f, 1f)
    }

    // 2. Ringing Screen Customization & Appearance
    val ringingBackgroundFlow: Flow<RingingBackgroundStyle> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_RINGING_BACKGROUND] ?: RingingBackgroundStyle.STARFIELD.name
        runCatching { RingingBackgroundStyle.valueOf(raw) }
            .getOrDefault(RingingBackgroundStyle.STARFIELD)
    }

    val ringingBackgroundImageUriFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_RINGING_BACKGROUND_IMAGE_URI].orEmpty()
    }

    /** Persisted video wallpaper URI (persistable read permission taken by the picker). */
    val ringingVideoUriFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_RINGING_VIDEO_URI].orEmpty()
    }

    val ringingSnoozeMinutesFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_RINGING_SNOOZE_MINUTES] ?: DEFAULT_SNOOZE_MINUTES).coerceIn(1, 30)
    }

    val ringingClockStyleFlow: Flow<RingingClockStyle> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_RINGING_CLOCK_STYLE] ?: RingingClockStyle.HORIZONTAL_DIGITAL.name
        runCatching { RingingClockStyle.valueOf(raw) }
            .getOrDefault(RingingClockStyle.HORIZONTAL_DIGITAL)
    }

    /**
     * The alarm-clock font (ringing screen + customization preview). Stored under
     * [KEY_ALARM_CLOCK_FONT_STYLE], fully decoupled from [appFontStyleFlow].
     */
    val alarmClockFontStyleFlow: Flow<RingingClockFont> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_ALARM_CLOCK_FONT_STYLE]
            ?: prefs[KEY_RINGING_CLOCK_FONT]
            ?: RingingClockFont.SYSTEM_DEFAULT.name
        runCatching { RingingClockFont.valueOf(raw) }
            .getOrDefault(RingingClockFont.SYSTEM_DEFAULT)
    }

    /**
     * Absolute file path of a custom `.ttf` / `.otf` imported for the ringing clock only.
     * Independent of [appCustomFontPathFlow].
     */
    val alarmClockCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_ALARM_CLOCK_CUSTOM_FONT_PATH].orEmpty()
    }

    /** Historical alias for [alarmClockFontStyleFlow]. */
    val ringingClockFontFlow: Flow<RingingClockFont> get() = alarmClockFontStyleFlow

    val ringingClockPositionFlow: Flow<RingingClockPosition> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_RINGING_CLOCK_POSITION] ?: RingingClockPosition.DEAD_CENTER.name
        runCatching { RingingClockPosition.valueOf(raw) }
            .getOrDefault(RingingClockPosition.DEAD_CENTER)
    }

    val freeDragOffsetXFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_FREE_DRAG_OFFSET_X] ?: 0f
    }

    val freeDragOffsetYFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_FREE_DRAG_OFFSET_Y] ?: 0f
    }

    /** Clock container scale (pinch / dimension slider). 1f = default size. */
    val clockContainerScaleFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_CLOCK_CONTAINER_SCALE] ?: 1f).coerceIn(CLOCK_SCALE_MIN, CLOCK_SCALE_MAX)
    }

    /** Manual digital-clock text size in `sp` (range 40..120, default 72). */
    val clockFontSizeFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_CLOCK_FONT_SIZE] ?: DEFAULT_CLOCK_FONT_SIZE).coerceIn(
            CLOCK_FONT_MIN,
            CLOCK_FONT_MAX
        )
    }

    // 2b. Modular date component (independent of the clock container).
    val clockDateVisibleFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_DATE_VISIBLE] ?: true
    }

    val clockDateFontFlow: Flow<ClockDateFont> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_CLOCK_DATE_FONT] ?: ClockDateFont.MATCH_CLOCK.name
        runCatching { ClockDateFont.valueOf(raw) }.getOrDefault(ClockDateFont.MATCH_CLOCK)
    }

    /**
     * Absolute file path of a custom font imported specifically for the date component. Used only
     * when [ClockDateFont.CUSTOM_FILE] is selected; fully independent of the clock / app fonts.
     */
    val clockDateCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_DATE_CUSTOM_FONT_PATH].orEmpty()
    }

    val clockDateScaleFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_CLOCK_DATE_SCALE] ?: 1f).coerceIn(DATE_SCALE_MIN, DATE_SCALE_MAX)
    }

    val clockDateOffsetXFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_DATE_OFFSET_X] ?: 0f
    }

    val clockDateOffsetYFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_DATE_OFFSET_Y] ?: 0f
    }

    /** Whether the AM/PM indicator is rendered (12-hour mode only). */
    val showAmPmFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SHOW_AM_PM] ?: true
    }

    /** Whether the decoupled top "Alarm" label is shown on the ringing/customization canvas. */
    val alarmLabelVisibleFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_ALARM_LABEL_VISIBLE] ?: true
    }

    /** Explicit clock bounding box size (px), used by the corner-handle auto-fit engine. */
    val clockBoxWidthFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_CLOCK_BOX_WIDTH] ?: 0f).coerceAtLeast(0f)
    }

    val clockBoxHeightFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_CLOCK_BOX_HEIGHT] ?: 0f).coerceAtLeast(0f)
    }

    val clockBoxEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_BOX_ENABLED] ?: false
    }

    val clockBoxColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_BOX_COLOR] ?: 0L
    }

    val clockBoxOpacityFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_BOX_OPACITY] ?: 0.35f
    }

    // 2d. Clock / date Colors, gradients, blur & opacity, leading-zero
    /** Removes the leading zero from the hour in 12-hour mode (e.g. 8:38 instead of 08:38). */
    val removeLeadingZeroFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_REMOVE_LEADING_ZERO] ?: true
    }

    /** Background blur radius in dp applied to image / video wallpapers (0 = none). */
    val ringingBlurFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_RINGING_BLUR] ?: 0f).coerceIn(BLUR_MIN, BLUR_MAX)
    }

    /** Darkening overlay opacity over the wallpaper (0 = none, 1 = fully black). */
    val ringingDimFlow: Flow<Float> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_RINGING_DIM] ?: DEFAULT_DIM).coerceIn(0f, 1f)
    }

    /** Global clock digit Color (0 = default white). */
    val clockColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_COLOR] ?: 0xFFD9B478L
    }

    val clockGradientFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_GRADIENT] ?: false
    }

    /** Second stop of the clock gradient (0 = default). */
    val clockColorEndFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_CLOCK_COLOR_END] ?: 0L
    }

    val clockGradientDirFlow: Flow<GradientDirection> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_CLOCK_GRADIENT_DIR] ?: GradientDirection.VERTICAL.name
        runCatching { GradientDirection.valueOf(raw) }.getOrDefault(GradientDirection.VERTICAL)
    }

    /** Global date text Color (0 = default white). */
    val dateColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DATE_COLOR] ?: 0xFFD9B478L
    }

    val dateGradientFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DATE_GRADIENT] ?: false
    }

    val dateColorEndFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DATE_COLOR_END] ?: 0L
    }

    val dateGradientDirFlow: Flow<GradientDirection> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_DATE_GRADIENT_DIR] ?: GradientDirection.VERTICAL.name
        runCatching { GradientDirection.valueOf(raw) }.getOrDefault(GradientDirection.VERTICAL)
    }

    /** Solid Color used by [RingingBackgroundStyle.SOLID_COLOR] (0 = default deep charcoal). */
    val ringingSolidColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_RINGING_SOLID_COLOR] ?: 0L
    }

    /**
     * Up to four selectable colours for the [RingingBackgroundStyle.INTERACTIVE_GRADIENT] mesh.
     * Empty means "use the built-in defaults". Encoded as a comma-separated packed-ARGB string.
     */
    val ringingInteractiveColorsFlow: Flow<List<Long>> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_RINGING_INTERACTIVE_COLORS]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            raw.split(',').mapNotNull { it.trim().toLongOrNull() }.take(4)
        }
    }

    // 2e. AM/PM indicator font & Color (12-hour mode only)
    val amPmFontStyleFlow: Flow<RingingClockFont> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_AM_PM_FONT_STYLE] ?: RingingClockFont.SYSTEM_DEFAULT.name
        runCatching { RingingClockFont.valueOf(raw) }.getOrDefault(RingingClockFont.SYSTEM_DEFAULT)
    }

    val amPmCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_AM_PM_CUSTOM_FONT_PATH].orEmpty()
    }

    /** AM/PM text Color (0 = default white @ 60% alpha). */
    val amPmColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_AM_PM_COLOR] ?: 0L
    }

    // 2f. "Alarm" label font & Color
    val alarmLabelFontStyleFlow: Flow<RingingClockFont> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_ALARM_LABEL_FONT_STYLE] ?: RingingClockFont.SYSTEM_DEFAULT.name
        runCatching { RingingClockFont.valueOf(raw) }.getOrDefault(RingingClockFont.SYSTEM_DEFAULT)
    }

    val alarmLabelCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_ALARM_LABEL_CUSTOM_FONT_PATH].orEmpty()
    }

    /** Alarm label text Color (0 = default white @ 75% alpha). */
    val alarmLabelColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_ALARM_LABEL_COLOR] ?: 0L
    }

    // 2g. Snooze button appearance
    val snoozeFontStyleFlow: Flow<RingingClockFont> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_SNOOZE_FONT_STYLE] ?: RingingClockFont.SYSTEM_DEFAULT.name
        runCatching { RingingClockFont.valueOf(raw) }.getOrDefault(RingingClockFont.SYSTEM_DEFAULT)
    }

    val snoozeCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SNOOZE_CUSTOM_FONT_PATH].orEmpty()
    }

    /** Snooze pill container Color (0 = default theme surface variant). */
    val snoozeContainerColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SNOOZE_CONTAINER_COLOR] ?: 0xFF14131AL
    }

    /** Snooze pill label Color (0 = default on-surface). */
    val snoozeTextColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_SNOOZE_TEXT_COLOR] ?: 0xFFD9B478L
    }

    // 2h. Dismiss button appearance
    val dismissFontStyleFlow: Flow<RingingClockFont> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_DISMISS_FONT_STYLE] ?: RingingClockFont.SYSTEM_DEFAULT.name
        runCatching { RingingClockFont.valueOf(raw) }.getOrDefault(RingingClockFont.SYSTEM_DEFAULT)
    }

    val dismissCustomFontPathFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DISMISS_CUSTOM_FONT_PATH].orEmpty()
    }

    /** Dismiss pill container Color (0 = default primary container). */
    val dismissContainerColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DISMISS_CONTAINER_COLOR] ?: 0xFFD9B478L
    }

    /** Dismiss pill label/icon Color (0 = default on-primary-container). */
    val dismissTextColorFlow: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DISMISS_TEXT_COLOR] ?: 0xFF14131AL
    }

    // 2c. Alarm list sorting
    val alarmSortModeFlow: Flow<AlarmSortMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_ALARM_SORT_MODE] ?: AlarmSortMode.TIME.name
        runCatching { AlarmSortMode.valueOf(raw) }.getOrDefault(AlarmSortMode.TIME)
    }

    // 3. Upcoming Alarm Notification
    val upcomingAlarmEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_UPCOMING_ALARM_ENABLED] ?: false
    }

    val upcomingAlarmMinutesFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        val enabled = prefs[KEY_UPCOMING_ALARM_ENABLED] ?: false
        if (!enabled) 0 else (prefs[KEY_UPCOMING_ALARM_MINUTES] ?: 15)
    }

    /** The pre-alarm lead time regardless of the enabled flag (used by the settings slider). */
    val rawUpcomingAlarmMinutesFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_UPCOMING_ALARM_MINUTES] ?: 15).coerceIn(1, 60)
    }

    val upcomingAlarmIntervalFlow: Flow<UpcomingAlarmInterval> = context.settingsDataStore.data.map { prefs ->
        val minutes = prefs[KEY_UPCOMING_ALARM_MINUTES] ?: 0
        UpcomingAlarmInterval.fromMinutes(minutes)
    }

    // 4. Timer Settings
    val timerRingtoneUriFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_TIMER_RINGTONE_URI]
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString().orEmpty()
    }

    val timerRingtoneTitleFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_TIMER_RINGTONE_TITLE] ?: "Default Timer Sound"
    }

    /** Default sound used by alarms whose own sound is unset (blank = system default). */
    val defaultAlarmRingtoneUriFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_ALARM_RINGTONE_URI].orEmpty()
    }

    val defaultAlarmRingtoneTitleFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_ALARM_RINGTONE_TITLE] ?: "System default"
    }

    val timerVibrationEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_TIMER_VIBRATION_ENABLED] ?: true
    }

    val timerVibrationPatternFlow: Flow<TimerVibrationPattern> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_TIMER_VIBRATION_PATTERN] ?: TimerVibrationPattern.TICK_TICK.name
        runCatching { TimerVibrationPattern.valueOf(raw) }
            .getOrDefault(TimerVibrationPattern.TICK_TICK)
    }

    // 5. "Are you awake?" check after dismissing an alarm
    val awakeCheckEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_AWAKE_CHECK_ENABLED] ?: false
    }

    val awakeCheckMinutesFlow: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        (prefs[KEY_AWAKE_CHECK_MINUTES] ?: DEFAULT_AWAKE_CHECK_MINUTES).coerceIn(1, 15)
    }

    val vibrateAlarmsAndTimersFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_VIBRATE_ALARMS_AND_TIMERS] ?: true
    }

    val worldClockShowWeatherFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_WORLD_CLOCK_SHOW_WEATHER] ?: true
    }

    val worldClockTemperatureUnitFlow: Flow<TemperatureUnit> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_WORLD_CLOCK_TEMPERATURE_UNIT] ?: TemperatureUnit.CELSIUS.name
        runCatching { TemperatureUnit.valueOf(raw) }.getOrDefault(TemperatureUnit.CELSIUS)
    }

    val timerSoundEnabledFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_TIMER_SOUND_ENABLED] ?: true
    }

    // 6. World clock saved zones (order preserving, pipe-separated)
    val worldClockZonesFlow: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[KEY_WORLD_CLOCK_ZONES]
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    /** Ordering mode for saved world-clock cities. */
    val worldClockSortFlow: Flow<WorldClockSortMode> = context.settingsDataStore.data.map { prefs ->
        val raw = prefs[KEY_WORLD_CLOCK_SORT] ?: WorldClockSortMode.CUSTOM.name
        runCatching { WorldClockSortMode.valueOf(raw) }.getOrDefault(WorldClockSortMode.CUSTOM)
    }

    // ---- Mutators ----

    /** Writes [value] to the dark or light profile key depending on the currently-active mode. */
    private fun MutablePreferences.editActiveColor(
        darkKey: Preferences.Key<Long>,
        lightKey: Preferences.Key<Long>,
        value: Long
    ) {
        this[if (activeProfileIsDark(this)) darkKey else lightKey] = value
    }

    /** Persists the accent selection for the currently-active Light/Dark profile. */
    suspend fun setAccentColor(color: AccentColor) {
        context.settingsDataStore.edit { prefs ->
            val key = if (activeProfileIsDark(prefs)) {
                KEY_DARK_ACCENT_COLOR
            } else {
                KEY_LIGHT_ACCENT_COLOR
            }
            prefs[key] = color.name
        }
    }

    /**
     * Stores the packed ARGB used by [AccentColor.CUSTOM] (does not select it by itself), writing to
     * the active Light/Dark profile so the other mode's custom accent is preserved.
     */
    suspend fun setAccentCustomColor(argb: Long) {
        context.settingsDataStore.edit {
            it.editActiveColor(
                KEY_DARK_ACCENT_CUSTOM_COLOR,
                KEY_LIGHT_ACCENT_CUSTOM_COLOR,
                argb
            )
        }
    }

    /**
     * Stores the custom background Color and selects [BackgroundPalette.CUSTOM] in one atomic
     * edit, so the theme never lands on CUSTOM while the Color still holds a stale default. Written
     * to the active Light/Dark profile so the other mode's custom background is preserved.
     */
    suspend fun setBackgroundCustomColor(argb: Long) {
        context.settingsDataStore.edit {
            it.editActiveColor(
                KEY_DARK_BACKGROUND_CUSTOM_COLOR,
                KEY_LIGHT_BACKGROUND_CUSTOM_COLOR,
                argb
            )
            it[KEY_BACKGROUND_PALETTE] = BackgroundPalette.CUSTOM.name
        }
    }

    /** Sets the floating navigation-pill Color; pass 0 to restore the default surface. */
    suspend fun setNavBarColor(argb: Long) {
        context.settingsDataStore.edit {
            it.editActiveColor(KEY_DARK_NAV_BAR_COLOR, KEY_LIGHT_NAV_BAR_COLOR, argb)
        }
    }

    /** Sets the settings-card container Color; pass 0 to restore the default surface. */
    suspend fun setSettingsCardColor(argb: Long) {
        context.settingsDataStore.edit {
            it.editActiveColor(
                KEY_DARK_SETTINGS_CARD_COLOR,
                KEY_LIGHT_SETTINGS_CARD_COLOR,
                argb
            )
        }
    }

    /** Sets the alarm-card container Color; pass 0 to restore the default surface. */
    suspend fun setAlarmCardColor(argb: Long) {
        context.settingsDataStore.edit {
            it.editActiveColor(KEY_DARK_ALARM_CARD_COLOR, KEY_LIGHT_ALARM_CARD_COLOR, argb)
        }
    }

    /** Sets the world-clock card container Color; pass 0 to restore the default surface. */
    suspend fun setWorldClockCardColor(argb: Long) {
        context.settingsDataStore.edit {
            it.editActiveColor(
                KEY_DARK_WORLD_CLOCK_CARD_COLOR,
                KEY_LIGHT_WORLD_CLOCK_CARD_COLOR,
                argb
            )
        }
    }

    suspend fun setDynamicDayNightAlarmsLandscapes(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_DYNAMIC_DAY_NIGHT_ALARMS_LANDSCAPES] = enabled }
    }

    suspend fun setDynamicDayNightWorldClockLandscapes(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_DYNAMIC_DAY_NIGHT_WORLD_CLOCK_LANDSCAPES] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setBackgroundPalette(palette: BackgroundPalette) {
        context.settingsDataStore.edit { it[KEY_BACKGROUND_PALETTE] = palette.name }
    }

    /** Persists the whole-app background style for the currently-active Light/Dark profile. */
    suspend fun setAppBackgroundStyle(style: AppBackgroundStyle) {
        context.settingsDataStore.edit { prefs ->
            val key = if (activeProfileIsDark(prefs)) {
                KEY_DARK_APP_BACKGROUND_STYLE
            } else {
                KEY_LIGHT_APP_BACKGROUND_STYLE
            }
            prefs[key] = style.name
        }
    }

    /** Persists the static-gradient stops for the active profile (2..4 packed ARGB colours). */
    suspend fun setBackgroundGradientColors(colors: List<Long>) {
        val clean = colors.filter { it != 0L }.let { if (it.size >= 2) it.take(4) else null }
            ?: return
        context.settingsDataStore.edit { prefs ->
            val key = if (activeProfileIsDark(prefs)) {
                KEY_DARK_BACKGROUND_GRADIENT_COLORS
            } else {
                KEY_LIGHT_BACKGROUND_GRADIENT_COLORS
            }
            prefs[key] = encodeColorList(clean)
        }
    }

    /** Persists the interactive mesh colours for the active profile (2..4 packed ARGB colours). */
    suspend fun setBackgroundInteractiveColors(colors: List<Long>) {
        val clean = colors.filter { it != 0L }.let { if (it.size >= 2) it.take(4) else null }
            ?: return
        context.settingsDataStore.edit { prefs ->
            val key = if (activeProfileIsDark(prefs)) {
                KEY_DARK_BACKGROUND_INTERACTIVE_COLORS
            } else {
                KEY_LIGHT_BACKGROUND_INTERACTIVE_COLORS
            }
            prefs[key] = encodeColorList(clean)
        }
    }

    suspend fun setUiScale(scale: Float) {
        context.settingsDataStore.edit { it[KEY_UI_SCALE] = scale.coerceIn(UI_SCALE_MIN, UI_SCALE_MAX) }
    }

    suspend fun setAppFontStyle(font: AppFontStyle) {
        context.settingsDataStore.edit { it[KEY_APP_FONT_STYLE] = font.name }
    }

    suspend fun setAppCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_APP_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setTimeFormat(format: TimeFormat) {
        context.settingsDataStore.edit { it[KEY_TIME_FORMAT] = format.name }
    }

    suspend fun setVolumePolicy(policy: VolumePolicy) {
        context.settingsDataStore.edit { it[KEY_VOLUME_POLICY] = policy.name }
    }

    suspend fun setUnifiedVolume(volume: Float) {
        context.settingsDataStore.edit { it[KEY_UNIFIED_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setRingingBackground(style: RingingBackgroundStyle) {
        context.settingsDataStore.edit { it[KEY_RINGING_BACKGROUND] = style.name }
    }

    suspend fun setRingingBackgroundImageUri(uri: String) {
        context.settingsDataStore.edit { it[KEY_RINGING_BACKGROUND_IMAGE_URI] = uri }
    }

    suspend fun setRingingVideoUri(uri: String) {
        context.settingsDataStore.edit { it[KEY_RINGING_VIDEO_URI] = uri }
    }

    suspend fun setRingingSnoozeMinutes(minutes: Int) {
        context.settingsDataStore.edit {
            it[KEY_RINGING_SNOOZE_MINUTES] = minutes.coerceIn(1, 30)
        }
    }

    suspend fun setRingingClockStyle(style: RingingClockStyle) {
        context.settingsDataStore.edit { prefs ->
            val previous = prefs[KEY_RINGING_CLOCK_STYLE]
                ?.let { runCatching { RingingClockStyle.valueOf(it) }.getOrNull() }
            prefs[KEY_RINGING_CLOCK_STYLE] = style.name
            // A manual clock-box size (pinch/drag) is fitted to one orientation only: a wide
            // horizontal box would crush a stacked vertical clock (and vice-versa). Clear the
            // stored box so the clock auto-fits the newly selected layout instead of carrying a
            // stale, mismatched scale across the switch.
            if (previous != null && previous != style) {
                prefs[KEY_CLOCK_BOX_WIDTH] = 0f
                prefs[KEY_CLOCK_BOX_HEIGHT] = 0f
            }
        }
    }

    suspend fun setAlarmClockFontStyle(font: RingingClockFont) {
        context.settingsDataStore.edit { it[KEY_ALARM_CLOCK_FONT_STYLE] = font.name }
    }

    suspend fun setAlarmClockCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_ALARM_CLOCK_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setRingingClockFont(font: RingingClockFont) = setAlarmClockFontStyle(font)

    suspend fun setRingingClockPosition(position: RingingClockPosition) {
        context.settingsDataStore.edit { it[KEY_RINGING_CLOCK_POSITION] = position.name }
    }

    suspend fun setFreeDragOffset(offsetX: Float, offsetY: Float) {
        context.settingsDataStore.edit {
            it[KEY_FREE_DRAG_OFFSET_X] = offsetX
            it[KEY_FREE_DRAG_OFFSET_Y] = offsetY
        }
    }

    suspend fun setClockContainerScale(scale: Float) {
        context.settingsDataStore.edit {
            it[KEY_CLOCK_CONTAINER_SCALE] = scale.coerceIn(CLOCK_SCALE_MIN, CLOCK_SCALE_MAX)
        }
    }

    suspend fun setClockFontSize(sizeSp: Float) {
        context.settingsDataStore.edit {
            it[KEY_CLOCK_FONT_SIZE] = sizeSp.coerceIn(CLOCK_FONT_MIN, CLOCK_FONT_MAX)
        }
    }

    suspend fun setClockDateVisible(visible: Boolean) {
        context.settingsDataStore.edit { it[KEY_CLOCK_DATE_VISIBLE] = visible }
    }

    suspend fun setClockDateFont(font: ClockDateFont) {
        context.settingsDataStore.edit { it[KEY_CLOCK_DATE_FONT] = font.name }
    }

    suspend fun setClockDateCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_CLOCK_DATE_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setClockDateScale(scale: Float) {
        context.settingsDataStore.edit {
            it[KEY_CLOCK_DATE_SCALE] = scale.coerceIn(DATE_SCALE_MIN, DATE_SCALE_MAX)
        }
    }

    suspend fun setClockDateOffset(offsetX: Float, offsetY: Float) {
        context.settingsDataStore.edit {
            it[KEY_CLOCK_DATE_OFFSET_X] = offsetX
            it[KEY_CLOCK_DATE_OFFSET_Y] = offsetY
        }
    }

    suspend fun setShowAmPm(show: Boolean) {
        context.settingsDataStore.edit { it[KEY_SHOW_AM_PM] = show }
    }

    suspend fun setAlarmLabelVisible(visible: Boolean) {
        context.settingsDataStore.edit { it[KEY_ALARM_LABEL_VISIBLE] = visible }
    }

    suspend fun setClockBoxSize(widthPx: Float, heightPx: Float) {
        context.settingsDataStore.edit {
            it[KEY_CLOCK_BOX_WIDTH] = widthPx.coerceAtLeast(0f)
            it[KEY_CLOCK_BOX_HEIGHT] = heightPx.coerceAtLeast(0f)
        }
    }

    suspend fun setClockBoxEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_CLOCK_BOX_ENABLED] = enabled }
    }

    suspend fun setClockBoxColor(color: Long) {
        context.settingsDataStore.edit { it[KEY_CLOCK_BOX_COLOR] = color }
    }

    suspend fun setClockBoxOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[KEY_CLOCK_BOX_OPACITY] = opacity.coerceIn(0f, 1f) }
    }

    suspend fun setRemoveLeadingZero(remove: Boolean) {
        context.settingsDataStore.edit { it[KEY_REMOVE_LEADING_ZERO] = remove }
    }

    suspend fun setRingingBlur(radius: Float) {
        context.settingsDataStore.edit { it[KEY_RINGING_BLUR] = radius.coerceIn(BLUR_MIN, BLUR_MAX) }
    }

    suspend fun setRingingDim(dim: Float) {
        context.settingsDataStore.edit { it[KEY_RINGING_DIM] = dim.coerceIn(0f, 1f) }
    }

    suspend fun setClockColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_CLOCK_COLOR] = argb }
    }

    suspend fun setClockGradient(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_CLOCK_GRADIENT] = enabled }
    }

    suspend fun setClockColorEnd(argb: Long) {
        context.settingsDataStore.edit { it[KEY_CLOCK_COLOR_END] = argb }
    }

    suspend fun setClockGradientDir(direction: GradientDirection) {
        context.settingsDataStore.edit { it[KEY_CLOCK_GRADIENT_DIR] = direction.name }
    }

    suspend fun setDateColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_DATE_COLOR] = argb }
    }

    suspend fun setDateGradient(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_DATE_GRADIENT] = enabled }
    }

    suspend fun setDateColorEnd(argb: Long) {
        context.settingsDataStore.edit { it[KEY_DATE_COLOR_END] = argb }
    }

    suspend fun setDateGradientDir(direction: GradientDirection) {
        context.settingsDataStore.edit { it[KEY_DATE_GRADIENT_DIR] = direction.name }
    }

    suspend fun setRingingSolidColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_RINGING_SOLID_COLOR] = argb }
    }

    /** Persists the interactive-gradient colour stops for the ringing/customization screen. */
    suspend fun setRingingInteractiveColors(colors: List<Long>) {
        val clean = colors.filter { it != 0L }.take(4)
        context.settingsDataStore.edit { it[KEY_RINGING_INTERACTIVE_COLORS] = encodeColorList(clean) }
    }

    suspend fun setAmPmFontStyle(font: RingingClockFont) {
        context.settingsDataStore.edit { it[KEY_AM_PM_FONT_STYLE] = font.name }
    }

    suspend fun setAmPmCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_AM_PM_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setAmPmColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_AM_PM_COLOR] = argb }
    }

    suspend fun setAlarmLabelFontStyle(font: RingingClockFont) {
        context.settingsDataStore.edit { it[KEY_ALARM_LABEL_FONT_STYLE] = font.name }
    }

    suspend fun setAlarmLabelCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_ALARM_LABEL_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setAlarmLabelColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_ALARM_LABEL_COLOR] = argb }
    }

    suspend fun setSnoozeFontStyle(font: RingingClockFont) {
        context.settingsDataStore.edit { it[KEY_SNOOZE_FONT_STYLE] = font.name }
    }

    suspend fun setSnoozeCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_SNOOZE_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setSnoozeContainerColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_SNOOZE_CONTAINER_COLOR] = argb }
    }

    suspend fun setSnoozeTextColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_SNOOZE_TEXT_COLOR] = argb }
    }

    suspend fun setDismissFontStyle(font: RingingClockFont) {
        context.settingsDataStore.edit { it[KEY_DISMISS_FONT_STYLE] = font.name }
    }

    suspend fun setDismissCustomFontPath(path: String) {
        context.settingsDataStore.edit { it[KEY_DISMISS_CUSTOM_FONT_PATH] = path }
    }

    suspend fun setDismissContainerColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_DISMISS_CONTAINER_COLOR] = argb }
    }

    suspend fun setDismissTextColor(argb: Long) {
        context.settingsDataStore.edit { it[KEY_DISMISS_TEXT_COLOR] = argb }
    }

    suspend fun saveAlarmCustomization(
        background: RingingBackgroundStyle,
        backgroundImageUri: String,
        backgroundVideoUri: String,
        solidColor: Long,
        interactiveColors: List<Long>,
        blur: Float,
        dim: Float,
        clockStyle: RingingClockStyle,
        clockColor: Long,
        clockGradient: Boolean,
        clockColorEnd: Long,
        clockGradientDir: GradientDirection,
        showAmPm: Boolean,
        amPmFont: RingingClockFont,
        amPmCustomFontPath: String,
        amPmColor: Long,
        alarmLabelVisible: Boolean,
        alarmLabelFont: RingingClockFont,
        alarmLabelCustomFontPath: String,
        alarmLabelColor: Long,
        clockFont: RingingClockFont,
        clockCustomFontPath: String,
        dateVisible: Boolean,
        dateFont: ClockDateFont,
        dateCustomFontPath: String,
        dateColor: Long,
        dateGradient: Boolean,
        dateColorEnd: Long,
        dateGradientDir: GradientDirection,
        dateScale: Float,
        offsetX: Float,
        offsetY: Float,
        dateOffsetX: Float,
        dateOffsetY: Float,
        boxWidth: Float,
        boxHeight: Float,
        clockBoxEnabled: Boolean = false,
        clockBoxColor: Long = 0L,
        clockBoxOpacity: Float = 0.35f,
        snoozeMinutes: Int,
        dismissFont: RingingClockFont,
        dismissCustomFontPath: String,
        dismissContainerColor: Long,
        dismissTextColor: Long,
        snoozeFont: RingingClockFont,
        snoozeCustomFontPath: String,
        snoozeContainerColor: Long,
        snoozeTextColor: Long
    ) {
        val cleanColors = interactiveColors.filter { it != 0L }.take(4)
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_RINGING_BACKGROUND] = background.name
            prefs[KEY_RINGING_BACKGROUND_IMAGE_URI] = backgroundImageUri
            prefs[KEY_RINGING_VIDEO_URI] = backgroundVideoUri
            prefs[KEY_RINGING_SOLID_COLOR] = solidColor
            prefs[KEY_RINGING_INTERACTIVE_COLORS] = encodeColorList(cleanColors)
            prefs[KEY_RINGING_BLUR] = blur.coerceIn(BLUR_MIN, BLUR_MAX)
            prefs[KEY_RINGING_DIM] = dim.coerceIn(0f, 1f)
            prefs[KEY_RINGING_CLOCK_STYLE] = clockStyle.name
            prefs[KEY_CLOCK_COLOR] = clockColor
            prefs[KEY_CLOCK_GRADIENT] = clockGradient
            prefs[KEY_CLOCK_COLOR_END] = clockColorEnd
            prefs[KEY_CLOCK_GRADIENT_DIR] = clockGradientDir.name
            prefs[KEY_SHOW_AM_PM] = showAmPm
            prefs[KEY_AM_PM_FONT_STYLE] = amPmFont.name
            prefs[KEY_AM_PM_CUSTOM_FONT_PATH] = amPmCustomFontPath
            prefs[KEY_AM_PM_COLOR] = amPmColor
            prefs[KEY_ALARM_LABEL_VISIBLE] = alarmLabelVisible
            prefs[KEY_ALARM_LABEL_FONT_STYLE] = alarmLabelFont.name
            prefs[KEY_ALARM_LABEL_CUSTOM_FONT_PATH] = alarmLabelCustomFontPath
            prefs[KEY_ALARM_LABEL_COLOR] = alarmLabelColor
            prefs[KEY_ALARM_CLOCK_FONT_STYLE] = clockFont.name
            prefs[KEY_ALARM_CLOCK_CUSTOM_FONT_PATH] = clockCustomFontPath
            prefs[KEY_CLOCK_DATE_VISIBLE] = dateVisible
            prefs[KEY_CLOCK_DATE_FONT] = dateFont.name
            prefs[KEY_CLOCK_DATE_CUSTOM_FONT_PATH] = dateCustomFontPath
            prefs[KEY_DATE_COLOR] = dateColor
            prefs[KEY_DATE_GRADIENT] = dateGradient
            prefs[KEY_DATE_COLOR_END] = dateColorEnd
            prefs[KEY_DATE_GRADIENT_DIR] = dateGradientDir.name
            prefs[KEY_CLOCK_DATE_SCALE] = dateScale.coerceIn(DATE_SCALE_MIN, DATE_SCALE_MAX)
            if (!offsetX.isNaN()) prefs[KEY_FREE_DRAG_OFFSET_X] = offsetX
            if (!offsetY.isNaN()) prefs[KEY_FREE_DRAG_OFFSET_Y] = offsetY
            if (!dateOffsetX.isNaN()) prefs[KEY_CLOCK_DATE_OFFSET_X] = dateOffsetX
            if (!dateOffsetY.isNaN()) prefs[KEY_CLOCK_DATE_OFFSET_Y] = dateOffsetY
            prefs[KEY_CLOCK_BOX_WIDTH] = boxWidth.coerceAtLeast(0f)
            prefs[KEY_CLOCK_BOX_HEIGHT] = boxHeight.coerceAtLeast(0f)
            prefs[KEY_CLOCK_BOX_ENABLED] = clockBoxEnabled
            prefs[KEY_CLOCK_BOX_COLOR] = clockBoxColor
            prefs[KEY_CLOCK_BOX_OPACITY] = clockBoxOpacity.coerceIn(0f, 1f)
            prefs[KEY_RINGING_SNOOZE_MINUTES] = snoozeMinutes.coerceIn(1, 30)
            prefs[KEY_DISMISS_FONT_STYLE] = dismissFont.name
            prefs[KEY_DISMISS_CUSTOM_FONT_PATH] = dismissCustomFontPath
            prefs[KEY_DISMISS_CONTAINER_COLOR] = dismissContainerColor
            prefs[KEY_DISMISS_TEXT_COLOR] = dismissTextColor
            prefs[KEY_SNOOZE_FONT_STYLE] = snoozeFont.name
            prefs[KEY_SNOOZE_CUSTOM_FONT_PATH] = snoozeCustomFontPath
            prefs[KEY_SNOOZE_CONTAINER_COLOR] = snoozeContainerColor
            prefs[KEY_SNOOZE_TEXT_COLOR] = snoozeTextColor
        }
    }

    suspend fun setAlarmSortMode(mode: AlarmSortMode) {
        context.settingsDataStore.edit { it[KEY_ALARM_SORT_MODE] = mode.name }
    }

    suspend fun setUpcomingAlarmEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_UPCOMING_ALARM_ENABLED] = enabled
            if (enabled && (prefs[KEY_UPCOMING_ALARM_MINUTES] ?: 0) == 0) {
                prefs[KEY_UPCOMING_ALARM_MINUTES] = UpcomingAlarmInterval.MINUTES_15.minutes
            }
        }
    }

    suspend fun setUpcomingAlarmInterval(interval: UpcomingAlarmInterval) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_UPCOMING_ALARM_MINUTES] = interval.minutes
            prefs[KEY_UPCOMING_ALARM_ENABLED] = interval != UpcomingAlarmInterval.DISABLED
        }
    }

    /** Sets the pre-alarm lead time directly (1..60 minutes) and marks the feature enabled. */
    suspend fun setUpcomingAlarmMinutes(minutes: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_UPCOMING_ALARM_MINUTES] = minutes.coerceIn(1, 60)
            prefs[KEY_UPCOMING_ALARM_ENABLED] = true
        }
    }

    suspend fun setTimerRingtone(uri: String, title: String) {
        context.settingsDataStore.edit {
            it[KEY_TIMER_RINGTONE_URI] = uri
            it[KEY_TIMER_RINGTONE_TITLE] = title
        }
    }

    suspend fun setDefaultAlarmRingtone(uri: String, title: String) {
        context.settingsDataStore.edit {
            it[KEY_DEFAULT_ALARM_RINGTONE_URI] = uri
            it[KEY_DEFAULT_ALARM_RINGTONE_TITLE] = title
        }
    }

    suspend fun resetDefaultAlarmRingtone() {
        context.settingsDataStore.edit {
            it[KEY_DEFAULT_ALARM_RINGTONE_URI] = ""
            it[KEY_DEFAULT_ALARM_RINGTONE_TITLE] = "System default"
        }
    }

    suspend fun setTimerVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_TIMER_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setTimerVibrationPattern(pattern: TimerVibrationPattern) {
        context.settingsDataStore.edit { it[KEY_TIMER_VIBRATION_PATTERN] = pattern.name }
    }

    suspend fun setAwakeCheckEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AWAKE_CHECK_ENABLED] = enabled }
    }

    suspend fun setAwakeCheckMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[KEY_AWAKE_CHECK_MINUTES] = minutes.coerceIn(1, 15) }
    }

    suspend fun setVibrateAlarmsAndTimers(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_VIBRATE_ALARMS_AND_TIMERS] = enabled }
    }

    suspend fun setWorldClockShowWeather(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_WORLD_CLOCK_SHOW_WEATHER] = enabled }
    }

    suspend fun setWorldClockTemperatureUnit(unit: TemperatureUnit) {
        context.settingsDataStore.edit { it[KEY_WORLD_CLOCK_TEMPERATURE_UNIT] = unit.name }
    }

    suspend fun setTimerSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_TIMER_SOUND_ENABLED] = enabled }
    }

    suspend fun setAlarmGroupOrder(order: List<String>) {
        context.settingsDataStore.edit { it[KEY_ALARM_GROUP_ORDER] = order.joinToString(",") }
    }

    suspend fun addWorldClockZone(zoneId: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_WORLD_CLOCK_ZONES]
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            if (!current.contains(zoneId)) {
                prefs[KEY_WORLD_CLOCK_ZONES] = (current + zoneId).joinToString("|")
            }
        }
    }

    suspend fun removeWorldClockZone(zoneId: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[KEY_WORLD_CLOCK_ZONES]
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            prefs[KEY_WORLD_CLOCK_ZONES] = current.filterNot { it == zoneId }.joinToString("|")
        }
    }

    suspend fun setWorldClockZones(zoneIds: List<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_WORLD_CLOCK_ZONES] = zoneIds.filter { it.isNotBlank() }.joinToString("|")
        }
    }

    suspend fun setWorldClockSort(mode: WorldClockSortMode) {
        context.settingsDataStore.edit { it[KEY_WORLD_CLOCK_SORT] = mode.name }
    }

    /**
     * Snapshots every persisted preference as a `name -> (typeTag, stringValue)` map so a backup can
     * round-trip the settings without enumerating each key by hand. Type tags: `s` string, `b`
     * boolean, `i` int, `l` long, `f` float, `d` double, `sn` string-set (values joined by \u001F).
     */
    suspend fun dumpPreferences(): List<Triple<String, String, String>> {
        val prefs = context.settingsDataStore.data.first()
        return prefs.asMap().mapNotNull { (key, value) ->
            val (tag, encoded) = when (value) {
                is Boolean -> "b" to value.toString()
                is Int -> "i" to value.toString()
                is Long -> "l" to value.toString()
                is Float -> "f" to value.toString()
                is Double -> "d" to value.toString()
                is String -> "s" to value
                is Set<*> -> "sn" to value.joinToString("\u001F")
                else -> "s" to value.toString()
            }
            Triple(key.name, tag, encoded)
        }
    }

    /** Reads the live world-clock city list independently of the generic preference dump. */
    suspend fun dumpWorldClockZones(): List<String> =
        context.settingsDataStore.data.first()[KEY_WORLD_CLOCK_ZONES]
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    /** Restores the live world-clock city list. */
    suspend fun restoreWorldClockZones(zoneIds: List<String>) = setWorldClockZones(zoneIds)

    /** Name of the world-clock zone preference, so a generic dump can exclude it. */
    val worldClockZonesKeyName: String get() = KEY_WORLD_CLOCK_ZONES.name

    /**
     * Restores a set of dumped preferences. Only the supplied entries are written; [replaceAll] wipes
     * every existing preference first so a backup restores a clean slate for the settings category.
     */
    suspend fun restorePreferences(
        entries: List<Triple<String, String, String>>,
        replaceAll: Boolean
    ) {
        context.settingsDataStore.edit { prefs ->
            if (replaceAll) {
                prefs.asMap().keys.toList().forEach { prefs.remove(it) }
            }
            entries.forEach { (name, tag, encoded) ->
                when (tag) {
                    "b" -> prefs[booleanPreferencesKey(name)] = encoded.toBooleanStrictOrNull() ?: false
                    "i" -> prefs[intPreferencesKey(name)] = encoded.toIntOrNull() ?: 0
                    "l" -> prefs[longPreferencesKey(name)] = encoded.toLongOrNull() ?: 0L
                    "f" -> prefs[floatPreferencesKey(name)] = encoded.toFloatOrNull() ?: 0f
                    "d" -> {
                        val v = encoded.toDoubleOrNull() ?: 0.0
                        prefs[stringPreferencesKey(name)] = v.toString()
                    }
                    "sn" -> prefs[stringPreferencesKey(name)] = encoded
                    else -> prefs[stringPreferencesKey(name)] = encoded
                }
            }
        }
    }

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 5
        const val DEFAULT_AWAKE_CHECK_MINUTES = 5

        const val CLOCK_FONT_MIN = 40f
        const val CLOCK_FONT_MAX = 120f
        const val DEFAULT_CLOCK_FONT_SIZE = 72f

        const val CLOCK_SCALE_MIN = 0.5f
        const val CLOCK_SCALE_MAX = 2.5f

        const val DATE_SCALE_MIN = 0.5f
        const val DATE_SCALE_MAX = 2.5f

        /** Global UI display scale bounds (0.85x compact â€¦ 1.15x large). */
        const val UI_SCALE_MIN = 0.85f
        const val UI_SCALE_MAX = 1.15f
        const val DEFAULT_UI_SCALE = 1f

        /** Bounds for a clock box resized via the corner handles (px). */
        const val CLOCK_BOX_MIN = 180f
        const val CLOCK_BOX_MAX = 2000f

        /** Wallpaper blur radius bounds (dp). */
        const val BLUR_MIN = 0f
        const val BLUR_MAX = 25f

        /** Default darkening overlay opacity over the wallpaper. */
        const val DEFAULT_DIM = 0.35f

        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_ACCENT_CUSTOM_COLOR = longPreferencesKey("accent_custom_color")
        private val KEY_DARK_ACCENT_COLOR = stringPreferencesKey("dark_accent_color")
        private val KEY_LIGHT_ACCENT_COLOR = stringPreferencesKey("light_accent_color")
        private val KEY_BACKGROUND_PALETTE = stringPreferencesKey("background_palette")
        private val KEY_BACKGROUND_CUSTOM_COLOR = longPreferencesKey("background_custom_color")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_UI_SCALE = floatPreferencesKey("ui_scale")

        // Per-mode custom colour profiles. Dark and Light each own an independent palette so
        // editing colours in one mode never overwrites the other mode's saved colours.
        private val KEY_DARK_ACCENT_CUSTOM_COLOR = longPreferencesKey("dark_accent_custom_color")
        private val KEY_LIGHT_ACCENT_CUSTOM_COLOR = longPreferencesKey("light_accent_custom_color")
        private val KEY_DARK_BACKGROUND_CUSTOM_COLOR =
            longPreferencesKey("dark_background_custom_color")
        private val KEY_LIGHT_BACKGROUND_CUSTOM_COLOR =
            longPreferencesKey("light_background_custom_color")
        private val KEY_DARK_NAV_BAR_COLOR = longPreferencesKey("dark_nav_bar_color")
        private val KEY_LIGHT_NAV_BAR_COLOR = longPreferencesKey("light_nav_bar_color")
        private val KEY_DARK_SETTINGS_CARD_COLOR = longPreferencesKey("dark_settings_card_color")
        private val KEY_LIGHT_SETTINGS_CARD_COLOR = longPreferencesKey("light_settings_card_color")
        private val KEY_DARK_ALARM_CARD_COLOR = longPreferencesKey("dark_alarm_card_color")
        private val KEY_LIGHT_ALARM_CARD_COLOR = longPreferencesKey("light_alarm_card_color")
        private val KEY_DARK_WORLD_CLOCK_CARD_COLOR =
            longPreferencesKey("dark_world_clock_card_color")
        private val KEY_LIGHT_WORLD_CLOCK_CARD_COLOR =
            longPreferencesKey("light_world_clock_card_color")

        // Per-mode whole-app background profiles. Each mode stores its own background STYLE plus the
        // colours that style needs (gradient colours for STATIC_GRADIENT, up to 4 interactive mesh
        // colours for INTERACTIVE_GRADIENT), so a Light <-> Dark switch restores the exact
        // background the user configured for that mode.
        private val KEY_DARK_APP_BACKGROUND_STYLE = stringPreferencesKey("dark_app_background_style")
        private val KEY_LIGHT_APP_BACKGROUND_STYLE = stringPreferencesKey("light_app_background_style")
        private val KEY_DARK_BACKGROUND_GRADIENT_COLORS =
            stringPreferencesKey("dark_background_gradient_colors")
        private val KEY_LIGHT_BACKGROUND_GRADIENT_COLORS =
            stringPreferencesKey("light_background_gradient_colors")
        private val KEY_DARK_BACKGROUND_INTERACTIVE_COLORS =
            stringPreferencesKey("dark_background_interactive_colors")
        private val KEY_LIGHT_BACKGROUND_INTERACTIVE_COLORS =
            stringPreferencesKey("light_background_interactive_colors")

        private val KEY_APP_FONT_STYLE = stringPreferencesKey("app_font_style")
        private val KEY_APP_CUSTOM_FONT_PATH = stringPreferencesKey("app_custom_font_path")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")

        private val KEY_VOLUME_POLICY = stringPreferencesKey("volume_policy")
        private val KEY_UNIFIED_VOLUME = floatPreferencesKey("unified_volume")

        private val KEY_RINGING_BACKGROUND = stringPreferencesKey("ringing_background")
        private val KEY_RINGING_BACKGROUND_IMAGE_URI =
            stringPreferencesKey("ringing_background_image_uri")
        private val KEY_RINGING_VIDEO_URI = stringPreferencesKey("ringing_video_uri")
        private val KEY_RINGING_SNOOZE_MINUTES = intPreferencesKey("ringing_snooze_minutes")
        private val KEY_RINGING_CLOCK_STYLE = stringPreferencesKey("ringing_clock_style")
        private val KEY_RINGING_CLOCK_FONT = stringPreferencesKey("ringing_clock_font")

        /** Independent alarm-clock font key (see [RingingClockFont]). */
        private val KEY_ALARM_CLOCK_FONT_STYLE = stringPreferencesKey("alarm_clock_font_style")
        private val KEY_ALARM_CLOCK_CUSTOM_FONT_PATH =
            stringPreferencesKey("alarm_clock_custom_font_path")
        private val KEY_RINGING_CLOCK_POSITION = stringPreferencesKey("ringing_clock_position")
        private val KEY_FREE_DRAG_OFFSET_X = floatPreferencesKey("free_drag_offset_x")
        private val KEY_FREE_DRAG_OFFSET_Y = floatPreferencesKey("free_drag_offset_y")
        private val KEY_CLOCK_CONTAINER_SCALE = floatPreferencesKey("clock_container_scale")
        private val KEY_CLOCK_FONT_SIZE = floatPreferencesKey("clock_font_size")

        private val KEY_CLOCK_DATE_VISIBLE = booleanPreferencesKey("clock_date_visible")
        private val KEY_CLOCK_DATE_FONT = stringPreferencesKey("clock_date_font")
        private val KEY_CLOCK_DATE_CUSTOM_FONT_PATH =
            stringPreferencesKey("clock_date_custom_font_path")
        private val KEY_CLOCK_DATE_SCALE = floatPreferencesKey("clock_date_scale")
        private val KEY_CLOCK_DATE_OFFSET_X = floatPreferencesKey("clock_date_offset_x")
        private val KEY_CLOCK_DATE_OFFSET_Y = floatPreferencesKey("clock_date_offset_y")
        private val KEY_SHOW_AM_PM = booleanPreferencesKey("show_am_pm")
        private val KEY_ALARM_LABEL_VISIBLE = booleanPreferencesKey("alarm_label_visible")
        private val KEY_CLOCK_BOX_WIDTH = floatPreferencesKey("clock_box_width")
        private val KEY_CLOCK_BOX_HEIGHT = floatPreferencesKey("clock_box_height")
        private val KEY_CLOCK_BOX_ENABLED = booleanPreferencesKey("clock_box_enabled")
        private val KEY_CLOCK_BOX_COLOR = longPreferencesKey("clock_box_color")
        private val KEY_CLOCK_BOX_OPACITY = floatPreferencesKey("clock_box_opacity")
        private val KEY_REMOVE_LEADING_ZERO = booleanPreferencesKey("remove_leading_zero")
        private val KEY_RINGING_BLUR = floatPreferencesKey("ringing_blur")
        private val KEY_RINGING_DIM = floatPreferencesKey("ringing_dim")
        private val KEY_CLOCK_COLOR = longPreferencesKey("clock_color")
        private val KEY_CLOCK_GRADIENT = booleanPreferencesKey("clock_gradient")
        private val KEY_CLOCK_COLOR_END = longPreferencesKey("clock_color_end")
        private val KEY_CLOCK_GRADIENT_DIR = stringPreferencesKey("clock_gradient_dir")
        private val KEY_DATE_COLOR = longPreferencesKey("date_color")
        private val KEY_DATE_GRADIENT = booleanPreferencesKey("date_gradient")
        private val KEY_DATE_COLOR_END = longPreferencesKey("date_color_end")
        private val KEY_DATE_GRADIENT_DIR = stringPreferencesKey("date_gradient_dir")
        private val KEY_RINGING_SOLID_COLOR = longPreferencesKey("ringing_solid_color")
        private val KEY_RINGING_INTERACTIVE_COLORS =
            stringPreferencesKey("ringing_interactive_colors")
        private val KEY_AM_PM_FONT_STYLE = stringPreferencesKey("am_pm_font_style")
        private val KEY_AM_PM_CUSTOM_FONT_PATH = stringPreferencesKey("am_pm_custom_font_path")
        private val KEY_AM_PM_COLOR = longPreferencesKey("am_pm_color")
        private val KEY_ALARM_LABEL_FONT_STYLE = stringPreferencesKey("alarm_label_font_style")
        private val KEY_ALARM_LABEL_CUSTOM_FONT_PATH =
            stringPreferencesKey("alarm_label_custom_font_path")
        private val KEY_ALARM_LABEL_COLOR = longPreferencesKey("alarm_label_color")
        private val KEY_SNOOZE_FONT_STYLE = stringPreferencesKey("snooze_font_style")
        private val KEY_SNOOZE_CUSTOM_FONT_PATH = stringPreferencesKey("snooze_custom_font_path")
        private val KEY_SNOOZE_CONTAINER_COLOR = longPreferencesKey("snooze_container_color")
        private val KEY_SNOOZE_TEXT_COLOR = longPreferencesKey("snooze_text_color")
        private val KEY_DISMISS_FONT_STYLE = stringPreferencesKey("dismiss_font_style")
        private val KEY_DISMISS_CUSTOM_FONT_PATH = stringPreferencesKey("dismiss_custom_font_path")
        private val KEY_DISMISS_CONTAINER_COLOR = longPreferencesKey("dismiss_container_color")
        private val KEY_DISMISS_TEXT_COLOR = longPreferencesKey("dismiss_text_color")
        private val KEY_ALARM_SORT_MODE = stringPreferencesKey("alarm_sort_mode")
        private val KEY_NAV_BAR_COLOR = longPreferencesKey("nav_bar_color")
        private val KEY_SETTINGS_CARD_COLOR = longPreferencesKey("settings_card_color")
private val KEY_ALARM_CARD_COLOR = longPreferencesKey("alarm_card_color")
private val KEY_WORLD_CLOCK_CARD_COLOR = longPreferencesKey("world_clock_card_color")
private val KEY_DYNAMIC_DAY_NIGHT_ALARMS_LANDSCAPES =
    booleanPreferencesKey("dynamic_day_night_alarms_landscapes")
private val KEY_DYNAMIC_DAY_NIGHT_WORLD_CLOCK_LANDSCAPES =
    booleanPreferencesKey("dynamic_day_night_world_clock_landscapes")


        private val KEY_UPCOMING_ALARM_ENABLED = booleanPreferencesKey("upcoming_alarm_enabled")
        private val KEY_UPCOMING_ALARM_MINUTES = intPreferencesKey("upcoming_alarm_minutes")

        private val KEY_TIMER_RINGTONE_URI = stringPreferencesKey("timer_ringtone_uri")
        private val KEY_TIMER_RINGTONE_TITLE = stringPreferencesKey("timer_ringtone_title")

        private val KEY_DEFAULT_ALARM_RINGTONE_URI = stringPreferencesKey("default_alarm_ringtone_uri")
        private val KEY_DEFAULT_ALARM_RINGTONE_TITLE = stringPreferencesKey("default_alarm_ringtone_title")
        private val KEY_TIMER_VIBRATION_ENABLED = booleanPreferencesKey("timer_vibration_enabled")
        private val KEY_TIMER_VIBRATION_PATTERN = stringPreferencesKey("timer_vibration_pattern")

        private val KEY_AWAKE_CHECK_ENABLED = booleanPreferencesKey("awake_check_enabled")
        private val KEY_AWAKE_CHECK_MINUTES = intPreferencesKey("awake_check_minutes")

        private val KEY_WORLD_CLOCK_ZONES = stringPreferencesKey("world_clock_zones")
        private val KEY_WORLD_CLOCK_SORT = stringPreferencesKey("world_clock_sort")

        private val KEY_VIBRATE_ALARMS_AND_TIMERS = booleanPreferencesKey("vibrate_alarms_and_timers")
        private val KEY_WORLD_CLOCK_SHOW_WEATHER = booleanPreferencesKey("world_clock_show_weather")
        private val KEY_WORLD_CLOCK_TEMPERATURE_UNIT = stringPreferencesKey("world_clock_temperature_unit")
        private val KEY_TIMER_SOUND_ENABLED = booleanPreferencesKey("timer_sound_enabled")
        private val KEY_ALARM_GROUP_ORDER = stringPreferencesKey("alarm_group_order")
    }
}

typealias SettingsStore = SettingsDataStore
