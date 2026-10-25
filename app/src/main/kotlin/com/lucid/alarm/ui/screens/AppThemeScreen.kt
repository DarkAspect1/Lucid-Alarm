package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.BackgroundPalette
import com.lucid.alarm.data.prefs.AppBackgroundStyle
import com.lucid.alarm.ui.components.InteractiveColorPickerRow
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.ThemeMode
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.components.CustomColorPickerDialog
import com.lucid.alarm.ui.components.selectedChipColors
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.rememberFontPicker
import com.lucid.alarm.ui.theme.backgroundBrushFor
import com.lucid.alarm.ui.theme.onColorFor
import com.lucid.alarm.ui.theme.toColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppThemeScreen(
    settingsDataStore: SettingsDataStore,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val themeMode by settingsDataStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val accentColor by settingsDataStore.accentColorFlow.collectAsState(initial = AccentColor.DEFAULT)
    val accentCustomArgb by settingsDataStore.accentCustomColorFlow.collectAsState(
        initial = AccentColor.DEFAULT.argb
    )
    val backgroundPalette by settingsDataStore.backgroundPaletteFlow.collectAsState(
        initial = BackgroundPalette.OBSIDIAN
    )
    val backgroundCustomArgb by settingsDataStore.backgroundCustomColorFlow.collectAsState(
        initial = BackgroundPalette.OBSIDIAN.argb
    )
    val navBarColor by settingsDataStore.navBarColorFlow.collectAsState(initial = 0L)
    val settingsCardColor by settingsDataStore.settingsCardColorFlow.collectAsState(initial = 0L)
    val alarmCardColor by settingsDataStore.alarmCardColorFlow.collectAsState(initial = 0L)
    val worldClockCardColor by settingsDataStore.worldClockCardColorFlow.collectAsState(initial = 0L)
    val appBackgroundStyle by settingsDataStore.appBackgroundStyleFlow.collectAsState(
        initial = AppBackgroundStyle.PALETTE
    )
    val backgroundGradientColors by settingsDataStore.backgroundGradientColorsFlow.collectAsState(
        initial = emptyList()
    )
    val backgroundInteractiveColors by settingsDataStore.backgroundInteractiveColorsFlow.collectAsState(
        initial = emptyList()
    )
    val dynamicDayNightAlarms by settingsDataStore.dynamicDayNightAlarmsLandscapes.collectAsState(
        initial = true
    )
    val dynamicDayNightWorldClock by settingsDataStore.dynamicDayNightWorldClockLandscapes.collectAsState(
        initial = false
    )
    val appFontStyle by settingsDataStore.appFontStyleFlow.collectAsState(initial = AppFontStyle.SYSTEM_DEFAULT)
    val appCustomFontPath by settingsDataStore.appCustomFontPathFlow.collectAsState(initial = "")
    val uiScale by settingsDataStore.uiScaleFlow.collectAsState(initial = SettingsDataStore.DEFAULT_UI_SCALE)

    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val pickAppFont = rememberFontPicker(
        baseName = FontManager.APP_FONT_BASE
    ) { path ->
        scope.launch { settingsDataStore.setAppCustomFontPath(path) }
    }

    var showAccentPicker by remember { mutableStateOf(false) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showNavBarPicker by remember { mutableStateOf(false) }
    var showSettingsCardPicker by remember { mutableStateOf(false) }
    var showAlarmCardPicker by remember { mutableStateOf(false) }
    var showWorldClockCardPicker by remember { mutableStateOf(false) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val listState = rememberLazyListState()

    val showFloatingBack by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 48
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Floating Back Button: Appears only when scrolled and header disappears
        AnimatedVisibility(
            visible = showFloatingBack,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .padding(top = statusBarTop + 4.dp, start = 12.dp)
                .zIndex(10f)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isLight) Color(0xCCFFFFFF) else Color(0xCC282932),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().edgeFadeMask(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "appearance_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                        .padding(top = statusBarTop + 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                SettingsSectionCard(
                    title = "Theme Mode",
                    icon = Icons.Outlined.DarkMode
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { scope.launch { settingsDataStore.setThemeMode(mode) } },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = mode.label,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Display Size",
                icon = Icons.Outlined.FormatSize
            ) {
                var tempUiScale by remember { mutableFloatStateOf(uiScale) }
                LaunchedEffect(uiScale) { tempUiScale = uiScale }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Display scale",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(tempUiScale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = tempUiScale,
                    onValueChange = { tempUiScale = it },
                    onValueChangeFinished = {
                        scope.launch { settingsDataStore.setUiScale(tempUiScale) }
                    },
                    valueRange = SettingsDataStore.UI_SCALE_MIN..SettingsDataStore.UI_SCALE_MAX,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Scales the whole app uniformly. Larger text and spacing make alarms easier to read.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsSectionCard(
                title = "Colors",
                icon = Icons.Outlined.Palette
            ) {
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccentColor.entries.forEach { accent ->
                        val selected = accentColor == accent
                        val swatchColor =
                            if (accent == AccentColor.CUSTOM) Color(accentCustomArgb)
                            else accent.toColor()
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                                .clickable {
                                    if (accent == AccentColor.CUSTOM) {
                                        showAccentPicker = true
                                    } else {
                                        scope.launch {
                                            settingsDataStore.setAccentColor(accent)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = accent.label,
                                    tint = onColorFor(swatchColor)
                                )
                            } else if (accent == AccentColor.CUSTOM) {
                                Text(
                                    text = "+",
                                    color = onColorFor(swatchColor),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Background Style",
                icon = Icons.Outlined.Palette
            ) {
                Text(
                    text = "Living, touch-reactive backgrounds that span the whole app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppBackgroundStyle.entries.forEach { style ->
                        FilterChip(
                            selected = appBackgroundStyle == style,
                            onClick = {
                                scope.launch { settingsDataStore.setAppBackgroundStyle(style) }
                            },
                            label = { Text(style.label) }
                        )
                    }
                }
                if (appBackgroundStyle == AppBackgroundStyle.STATIC_GRADIENT) {
                    Text(
                        text = "Gradient Colors",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    InteractiveColorPickerRow(
                        colors = backgroundGradientColors.map { Color(it.toInt()) },
                        onColorsChange = { updated ->
                            scope.launch {
                                settingsDataStore.setBackgroundGradientColors(
                                    updated.map { it.toArgb().toLong() }
                                )
                            }
                        }
                    )
                }
                if (appBackgroundStyle == AppBackgroundStyle.INTERACTIVE_GRADIENT ||
                    appBackgroundStyle == AppBackgroundStyle.AURORA ||
                    appBackgroundStyle == AppBackgroundStyle.FIREFLIES
                ) {
                    Text(
                        text = "Background Colors",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    InteractiveColorPickerRow(
                        colors = backgroundInteractiveColors.map { Color(it.toInt()) },
                        onColorsChange = { updated ->
                            scope.launch {
                                settingsDataStore.setBackgroundInteractiveColors(
                                    updated.map { it.toArgb().toLong() }
                                )
                            }
                        }
                    )
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Background",
                icon = Icons.Outlined.Palette
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BackgroundPalette.entries.forEach { palette ->
                        val selected = backgroundPalette == palette
                        val swatchBrush = backgroundBrushFor(
                            palette = palette,
                            dark = dark,
                            customArgb = backgroundCustomArgb
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(swatchBrush)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (palette == BackgroundPalette.CUSTOM) {
                                        showBackgroundPicker = true
                                    } else {
                                        scope.launch {
                                            settingsDataStore.setBackgroundPalette(palette)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = palette.label,
                                    tint = if (palette == BackgroundPalette.CUSTOM) {
                                        onColorFor(Color(backgroundCustomArgb))
                                    } else if (dark) {
                                        Color.White
                                    } else {
                                        Color(0xFF14171A)
                                    }
                                )
                            } else if (palette == BackgroundPalette.CUSTOM) {
                                Text(
                                    text = "+",
                                    color = onColorFor(Color(backgroundCustomArgb)),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "Navigation Bar",
                icon = Icons.Outlined.Palette
            ) {
                Text(
                    text = "Color of the floating tab bar at the bottom",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                ColorSwatchGrid(
                    selectedArgb = navBarColor,
                    defaultColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onSelectPreset = { argb ->
                        scope.launch { settingsDataStore.setNavBarColor(argb) }
                    },
                    onChooseCustom = { showNavBarPicker = true }
                )
            }
        }

        item {
            SettingsSectionCard(
                title = "Card Colors",
                icon = Icons.Outlined.Palette
            ) {
                Text(
                    text = "Container Colors for the cards used across the app. Text and icons " +
                        "automatically switch between black and white for contrast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Settings panels",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Section cards on Settings and Appearance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                ColorSwatchGrid(
                    selectedArgb = settingsCardColor,
                    defaultColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    onSelectPreset = { argb ->
                        scope.launch { settingsDataStore.setSettingsCardColor(argb) }
                    },
                    onChooseCustom = { showSettingsCardPicker = true }
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Alarm cards",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cards in the alarm list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                ColorSwatchGrid(
                    selectedArgb = alarmCardColor,
                    defaultColor = MaterialTheme.colorScheme.surfaceVariant,
                    onSelectPreset = { argb ->
                        scope.launch { settingsDataStore.setAlarmCardColor(argb) }
                    },
                    onChooseCustom = { showAlarmCardPicker = true }
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "World clock cards",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Cards in the world clock list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                ColorSwatchGrid(
                    selectedArgb = worldClockCardColor,
                    defaultColor = MaterialTheme.colorScheme.surfaceVariant,
                    onSelectPreset = { argb ->
                        scope.launch { settingsDataStore.setWorldClockCardColor(argb) }
                    },
                    onChooseCustom = { showWorldClockCardPicker = true }
                )
            }
        }

        item {
            SettingsSectionCard(
                title = "Dynamic Day-Night Landscapes",
                icon = Icons.Outlined.DarkMode
            ) {
                Text(
                    text = "Give alarm and world-clock cards a live sky with a unique procedural " +
                        "landscape. Turn each list on or off independently; custom card Colors " +
                        "return whenever a list is off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Dynamic Day-Night Alarm Landscapes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Shows a time-based sky & procedural landscape according to " +
                                "each alarm's set time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dynamicDayNightAlarms,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsDataStore.setDynamicDayNightAlarmsLandscapes(enabled)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Dynamic Day-Night World Clock Landscapes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Shows a live local sky & procedural landscape for each city.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = dynamicDayNightWorldClock,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsDataStore.setDynamicDayNightWorldClockLandscapes(enabled)
                            }
                        }
                    )
                }
            }
        }

        item {
            SettingsSectionCard(
                title = "App Font",
                icon = Icons.Outlined.TextFields
            ) {
                Text(
                    text = "Separate from the alarm clock font",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFontStyle.entries.forEach { style ->
                        val selected = appFontStyle == style
                        FilterChip(
                            selected = selected,
                            onClick = {
                                scope.launch { settingsDataStore.setAppFontStyle(style) }
                            },
                    label = { Text(style.label) },
                    colors = selectedChipColors()
                )
                    }
                }
                if (appFontStyle == AppFontStyle.CUSTOM_FILE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { pickAppFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose custom font")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (appCustomFontPath.isBlank()) {
                            "No custom font selected yet"
                        } else {
                            "Font: ${appCustomFontPath.substringAfterLast('/')}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

    if (showAccentPicker) {
        CustomColorPickerDialog(
            initialColor =
                if (accentColor == AccentColor.CUSTOM) accentCustomArgb else accentColor.argb,
            onDismiss = { showAccentPicker = false },
            onConfirm = { argb ->
                showAccentPicker = false
                scope.launch {
                    // Store the Color first, then select CUSTOM, so the theme never lands on
                    // AccentColor.CUSTOM while the stored value is still the fallback.
                    settingsDataStore.setAccentCustomColor(argb)
                    settingsDataStore.setAccentColor(AccentColor.CUSTOM)
                }
            }
        )
    }

    if (showBackgroundPicker) {
        CustomColorPickerDialog(
            initialColor =
                if (backgroundPalette == BackgroundPalette.CUSTOM) {
                    backgroundCustomArgb
                } else {
                    backgroundPalette.argb
                },
            onDismiss = { showBackgroundPicker = false },
            onConfirm = { argb ->
                showBackgroundPicker = false
                // Atomic: stores the Color and selects BackgroundPalette.CUSTOM together.
                scope.launch { settingsDataStore.setBackgroundCustomColor(argb) }
            }
        )
    }

    if (showNavBarPicker) {
        CustomColorPickerDialog(
            initialColor = if (navBarColor != 0L) {
                navBarColor
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.toArgb().toLong() and 0xFFFFFFFFL
            },
            onDismiss = { showNavBarPicker = false },
            onConfirm = { argb ->
                showNavBarPicker = false
                scope.launch { settingsDataStore.setNavBarColor(argb) }
            }
        )
    }

    if (showSettingsCardPicker) {
        CustomColorPickerDialog(
            initialColor = if (settingsCardColor != 0L) {
                settingsCardColor
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow.toArgb().toLong() and 0xFFFFFFFFL
            },
            onDismiss = { showSettingsCardPicker = false },
            onConfirm = { argb ->
                showSettingsCardPicker = false
                scope.launch { settingsDataStore.setSettingsCardColor(argb) }
            }
        )
    }

    if (showAlarmCardPicker) {
        CustomColorPickerDialog(
            initialColor = if (alarmCardColor != 0L) {
                alarmCardColor
            } else {
                MaterialTheme.colorScheme.surfaceVariant.toArgb().toLong() and 0xFFFFFFFFL
            },
            onDismiss = { showAlarmCardPicker = false },
            onConfirm = { argb ->
                showAlarmCardPicker = false
                scope.launch { settingsDataStore.setAlarmCardColor(argb) }
            }
        )
    }

    if (showWorldClockCardPicker) {
        CustomColorPickerDialog(
            initialColor = if (worldClockCardColor != 0L) {
                worldClockCardColor
            } else {
                MaterialTheme.colorScheme.surfaceVariant.toArgb().toLong() and 0xFFFFFFFFL
            },
            onDismiss = { showWorldClockCardPicker = false },
            onConfirm = { argb ->
                showWorldClockCardPicker = false
                scope.launch { settingsDataStore.setWorldClockCardColor(argb) }
            }
        )
    }
}

@Composable
private fun ColorSwatchGrid(
    selectedArgb: Long,
    defaultColor: Color,
    onSelectPreset: (Long) -> Unit,
    onChooseCustom: () -> Unit
) {
    val isCustom = selectedArgb != 0L
    val selectedPreset = if (isCustom) {
        AccentColor.entries.firstOrNull {
            it != AccentColor.CUSTOM && it.argb == selectedArgb
        }
    } else {
        null
    }
    val customSelected = isCustom && selectedPreset == null
    val customSwatchColor = if (customSelected) {
        Color(selectedArgb)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PaletteSwatch(
            color = defaultColor,
            selected = !isCustom,
            contentDescription = "Default"
        ) {
            onSelectPreset(0L)
        }
        AccentColor.entries.forEach { accent ->
            if (accent == AccentColor.CUSTOM) return@forEach
            PaletteSwatch(
                color = accent.toColor(),
                selected = selectedPreset == accent,
                contentDescription = accent.label
            ) {
                onSelectPreset(accent.argb)
            }
        }
        PaletteSwatch(
            color = customSwatchColor,
            selected = customSelected,
            contentDescription = "Custom Color",
            showPlus = !customSelected,
            onClick = onChooseCustom
        )
    }
}

@Composable
private fun PaletteSwatch(
    color: Color,
    selected: Boolean,
    contentDescription: String,
    showPlus: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = contentDescription,
                tint = onColorFor(color)
            )
        } else if (showPlus) {
            Text(
                text = "+",
                color = onColorFor(color),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
internal fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

