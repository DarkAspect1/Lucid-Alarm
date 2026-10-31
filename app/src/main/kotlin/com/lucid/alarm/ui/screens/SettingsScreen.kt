package com.lucid.alarm.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.util.Locale
import com.lucid.alarm.BackupManager
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.AlarmVibrationPolicy
import com.lucid.alarm.data.prefs.AlarmVibrationPreset
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.ThemeMode
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.data.prefs.TimerVibrationPolicy
import com.lucid.alarm.data.prefs.TimerVolumePolicy
import com.lucid.alarm.data.prefs.VolumePolicy
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.localizedLabel
import com.lucid.alarm.ui.localizedName
import com.lucid.alarm.ui.components.CustomColorPickerDialog
import com.lucid.alarm.ui.components.FrostedBlurBox
import com.lucid.alarm.ui.theme.toColor
import com.lucid.alarm.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
private fun ThemeMode.localizedLabel(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun AppFontStyle.localizedLabel(): String = when (this) {
    AppFontStyle.SYSTEM_DEFAULT -> stringResource(R.string.font_system_default)
    AppFontStyle.SERIF -> stringResource(R.string.font_editorial_serif)
    AppFontStyle.CURSIVE -> stringResource(R.string.font_minimalist_cursive)
    AppFontStyle.CUSTOM_FILE -> stringResource(R.string.font_custom_file)
}

@Composable
private fun VolumePolicy.localizedLabel(): String = when (this) {
    VolumePolicy.UNIFIED -> stringResource(R.string.volume_policy_unified)
    VolumePolicy.PER_ALARM -> stringResource(R.string.volume_policy_per_alarm)
}

@Composable
private fun TimerVolumePolicy.localizedLabel(): String = when (this) {
    TimerVolumePolicy.UNIFIED -> stringResource(R.string.volume_policy_unified)
    TimerVolumePolicy.PER_TIMER -> stringResource(R.string.volume_policy_per_timer)
}

@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    onNavigateBack: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToCustomization: () -> Unit = {},
    onNavigateToRecycleBin: () -> Unit = {},
    onAlarmSettingsChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val screenBg = if (isLight) Color(0xFFF2F2F7) else Color(0xFF000000)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val cardContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
    val cardBorder = if (isLight) Color(0xFFE5E7EB) else Color.Transparent
    val accentColor = MaterialTheme.colorScheme.primary

    val themeMode by settingsDataStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(initial = TimeFormat.TWELVE_HOUR)
    val removeLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(initial = true)
    val currentAccent by settingsDataStore.accentColorFlow.collectAsState(initial = AccentColor.DEFAULT)
    val accentCustomArgb by settingsDataStore.accentCustomColorFlow.collectAsState(
        initial = AccentColor.DEFAULT.argb
    )
    val appFontStyle by settingsDataStore.appFontStyleFlow.collectAsState(initial = AppFontStyle.SYSTEM_DEFAULT)

    val volumePolicy by settingsDataStore.volumePolicyFlow.collectAsState(initial = VolumePolicy.UNIFIED)
    val unifiedVolume by settingsDataStore.unifiedVolumeFlow.collectAsState(initial = 0.85f)
    val alarmVibrationPolicy by settingsDataStore.alarmVibrationPolicyFlow.collectAsState(initial = AlarmVibrationPolicy.UNIFIED)
    val vibrateAlarmsAndTimers by settingsDataStore.vibrateAlarmsAndTimersFlow.collectAsState(initial = true)
    val alarmVibrationPreset by settingsDataStore.alarmVibrationPresetFlow.collectAsState(initial = AlarmVibrationPreset.HEARTBEAT)
    val timerVolumePolicy by settingsDataStore.timerVolumePolicyFlow.collectAsState(initial = TimerVolumePolicy.UNIFIED)
    val timerUnifiedVolume by settingsDataStore.timerUnifiedVolumeFlow.collectAsState(initial = 0.85f)
    val timerVibrationPolicy by settingsDataStore.timerVibrationPolicyFlow.collectAsState(initial = TimerVibrationPolicy.UNIFIED)
    val defaultAlarmRingtoneUri by settingsDataStore.defaultAlarmRingtoneUriFlow.collectAsState(initial = "")
    val defaultAlarmRingtoneTitle by settingsDataStore.defaultAlarmRingtoneTitleFlow.collectAsState(
        initial = "System default"
    )

    val ringingSnoozeMinutes by settingsDataStore.ringingSnoozeMinutesFlow.collectAsState(initial = 5)
    val upcomingEnabled by settingsDataStore.upcomingAlarmEnabledFlow.collectAsState(initial = false)
    val upcomingMinutesRaw by settingsDataStore.rawUpcomingAlarmMinutesFlow.collectAsState(initial = 15)
    val awakeCheckEnabled by settingsDataStore.awakeCheckEnabledFlow.collectAsState(initial = false)
    val awakeCheckMinutes by settingsDataStore.awakeCheckMinutesFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_AWAKE_CHECK_MINUTES
    )

    val timerSoundEnabled by settingsDataStore.timerSoundEnabledFlow.collectAsState(initial = true)
    val timerRingtoneTitle by settingsDataStore.timerRingtoneTitleFlow.collectAsState(
        initial = "Default Timer Sound"
    )
    val timerRingtoneUri by settingsDataStore.timerRingtoneUriFlow.collectAsState(initial = "")
    val timerVibrationEnabled by settingsDataStore.timerVibrationEnabledFlow.collectAsState(initial = true)
    val timerVibrationPattern by settingsDataStore.timerVibrationPatternFlow.collectAsState(
        initial = TimerVibrationPattern.TICK_TICK
    )

    val appLanguage by settingsDataStore.appLanguageFlow.collectAsState(initial = "")
    var showAccentMenu by remember { mutableStateOf(false) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var showAppFontMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val systemDefaultLabel = stringResource(R.string.language_system_default)
    val languageOptions = listOf(
        "" to systemDefaultLabel,
        "en" to "English",
        "ar" to "العربية",
        "tr" to "Türkçe",
        "it" to "Italiano",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch"
    )
    val currentLanguageLabel = when (appLanguage) {
        "en" -> "English"
        "ar" -> "العربية"
        "tr" -> "Türkçe"
        "it" -> "Italiano"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        else -> systemDefaultLabel
    }

    val appFontFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val importedPath = FontManager.importFontFromUri(context, uri, FontManager.APP_FONT_BASE)
                if (importedPath.isNotBlank()) {
                    settingsDataStore.setAppCustomFontPath(importedPath)
                    settingsDataStore.setAppFontStyle(AppFontStyle.CUSTOM_FILE)
                    Toast.makeText(context, context.getString(R.string.font_applied_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.font_load_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val timerRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, pickedUri)
                val title = ringtone?.getTitle(context) ?: "Custom Timer Sound"
                scope.launch {
                    settingsDataStore.setTimerRingtone(pickedUri.toString(), title)
                }
            }
        }
    }

    val alarmRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                val ringtone = RingtoneManager.getRingtone(context, pickedUri)
                val title = ringtone?.getTitle(context) ?: "Custom Alarm Sound"
                scope.launch {
                    settingsDataStore.setDefaultAlarmRingtone(pickedUri.toString(), title)
                }
            } else {
                scope.launch {
                    settingsDataStore.resetDefaultAlarmRingtone()
                }
            }
        }
    }

    var showBackupMenu by remember { mutableStateOf(false) }
    var backupMode by remember { mutableStateOf(BackupManager.BackupMode.EXPORT) }
    var backupSettings by remember { mutableStateOf(true) }
    var backupAlarms by remember { mutableStateOf(true) }
    var backupWorldClock by remember { mutableStateOf(true) }
    var backupRecycleBin by remember { mutableStateOf(true) }

    fun currentSelection() = BackupManager.Selection(
        settings = backupSettings,
        alarms = backupAlarms,
        worldClock = backupWorldClock,
        recycleBin = backupRecycleBin
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = BackupManager.export(context, uri, currentSelection())
                val message = if (result.success) {
                    context.getString(R.string.toast_backup_saved, result.applied.joinToString(", "))
                } else {
                    context.getString(R.string.toast_backup_failed, result.error ?: "")
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = BackupManager.import(context, uri, currentSelection())
                val message = if (result.success) {
                    context.getString(R.string.toast_restore_saved, result.applied.joinToString(", "))
                } else {
                    context.getString(R.string.toast_restore_failed, result.error ?: "")
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cardShape = RoundedCornerShape(26.dp)
    val listState = rememberLazyListState()

    var isScrollingUp by remember { mutableStateOf(false) }
    var prevIndex by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    var prevOffset by remember { mutableIntStateOf(listState.firstVisibleItemScrollOffset) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val curIndex = listState.firstVisibleItemIndex
        val curOffset = listState.firstVisibleItemScrollOffset
        if (curIndex < prevIndex || (curIndex == prevIndex && curOffset < prevOffset)) {
            isScrollingUp = true
        } else if (curIndex > prevIndex || (curIndex == prevIndex && curOffset > prevOffset)) {
            isScrollingUp = false
        }
        prevIndex = curIndex
        prevOffset = curOffset
    }

    val showScrollToTop by remember {
        derivedStateOf {
            val pastThreshold = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 220
            pastThreshold && (!listState.isScrollInProgress || isScrollingUp)
        }
    }

    val itemHeights = remember { mutableMapOf<Int, Int>() }
    val spacingPx = with(LocalDensity.current) { 16.dp.roundToPx() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                for (item in visibleItems) {
                    itemHeights[item.index] = item.size
                }
            }
    }

    val showFloatingBack by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }
    val floatingBackAlpha by animateFloatAsState(
        targetValue = if (showFloatingBack) 1f else 0f,
        animationSpec = tween(180),
        label = "floatingBackAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {

        Box(
            modifier = Modifier
                .padding(top = statusBarTop + 4.dp, start = 12.dp)
                .zIndex(10f)
                .graphicsLayer {
                    alpha = floatingBackAlpha
                }
        ) {
            if (floatingBackAlpha > 0.01f) {
                IconButton(
                    onClick = onNavigateBack,
                    enabled = showFloatingBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isLight) Color(0xCCFFFFFF) else Color(0xCC282932),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.clock_settings),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item(key = "section_general") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
                    contentColor = cardContentColor,
                    border = null,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_general_appearance),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.theme_mode),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = themeMode.localizedLabel(),
                                    fontSize = 13.sp,
                                    color = accentColor,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val isSelected = themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setThemeMode(mode) }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.localizedLabel(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.setting_24_hour_format),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (timeFormat.is24Hour) stringResource(R.string.desc_24_hour_on) else stringResource(R.string.desc_24_hour_off),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = timeFormat.is24Hour,
                                onCheckedChange = { is24 ->
                                    scope.launch {
                                        settingsDataStore.setTimeFormat(
                                            if (is24) TimeFormat.TWENTY_FOUR_HOUR else TimeFormat.TWELVE_HOUR
                                        )
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.setting_remove_leading_zero),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.desc_remove_leading_zero),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = removeLeadingZero,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setRemoveLeadingZero(checked) }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAccentMenu = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.accent_color),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentAccent == AccentColor.CUSTOM) stringResource(R.string.sort_custom) else currentAccent.label,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }

                            FrostedDropdownMenu(
                                expanded = showAccentMenu,
                                onDismissRequest = { showAccentMenu = false },
                                modifier = Modifier.widthIn(min = 320.dp, max = 350.dp)
                            ) {
                                val presets = AccentColor.entries.filter { it != AccentColor.CUSTOM }
                                presets.chunked(7).forEach { row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        row.forEach { accent ->
                                            val selected = currentAccent == accent
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(accent.toColor())
                                                    .clickable {
                                                        scope.launch { settingsDataStore.setAccentColor(accent) }
                                                        showAccentMenu = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )

                                val isCustomSelected = currentAccent == AccentColor.CUSTOM
                                val customSource = remember { MutableInteractionSource() }
                                val isCustomPressed by customSource.collectIsPressedAsState()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isCustomPressed) {
                                                if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)
                                            } else Color.Transparent
                                        )
                                        .clickable(
                                            interactionSource = customSource,
                                            indication = null
                                        ) {
                                            showAccentMenu = false
                                            showCustomPicker = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.sweepGradient(
                                                        listOf(
                                                            Color.Red, Color.Yellow, Color.Green,
                                                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCustomSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.custom_color),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isLight) Color(0xFF111318) else Color.White
                                            )
                                            Text(
                                                text = stringResource(R.string.desc_custom_color),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAppFontMenu = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.app_font),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = appFontStyle.localizedLabel(),
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FrostedDropdownMenu(
                                expanded = showAppFontMenu,
                                onDismissRequest = { showAppFontMenu = false },
                                modifier = Modifier.widthIn(min = 290.dp, max = 330.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AppFontStyle.entries.forEach { style ->
                                        val isSelected = appFontStyle == style
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isPressed by interactionSource.collectIsPressedAsState()
                                        val itemBg = when {
                                            isSelected -> accentColor.copy(alpha = 0.14f)
                                            isPressed -> if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(itemBg)
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    showAppFontMenu = false
                                                    if (style == AppFontStyle.CUSTOM_FILE) {
                                                        appFontFilePickerLauncher.launch(FontManager.FONT_MIME_TYPES)
                                                    } else {
                                                        scope.launch { settingsDataStore.setAppFontStyle(style) }
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = style.localizedLabel(),
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) accentColor else (if (isLight) Color(0xFF111318) else Color.White)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onNavigateToCustomization)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.setting_alarm_appearance),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.desc_alarm_appearance),
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showLanguageMenu = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_app_language),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentLanguageLabel,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FrostedDropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false },
                                modifier = Modifier.widthIn(min = 260.dp, max = 300.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    languageOptions.forEach { (code, label) ->
                                        val isSelected = appLanguage == code
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val isPressed by interactionSource.collectIsPressedAsState()
                                        val itemBg = when {
                                            isSelected -> accentColor.copy(alpha = 0.14f)
                                            isPressed -> if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(itemBg)
                                                .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null
                                                ) {
                                                    showLanguageMenu = false
                                                    scope.launch {
                                                        settingsDataStore.setAppLanguage(code)
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? android.app.LocaleManager
                                                            if (code.isBlank()) {
                                                                localeManager?.applicationLocales = android.os.LocaleList.getEmptyLocaleList()
                                                            } else {
                                                                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(code)
                                                            }
                                                        }
                                                        delay(50)
                                                        context.findActivity()?.recreate()
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) accentColor else (if (isLight) Color(0xFF111318) else Color.White)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "section_audio") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
                    contentColor = cardContentColor,
                    border = null,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_audio_volume),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.setting_alarm_vibration_policy),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = alarmVibrationPolicy.localizedLabel(),
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isLight) Color(0xFFF3F4F6) else Color(0xFF282830))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AlarmVibrationPolicy.entries.forEach { policy ->
                                    val isSelected = alarmVibrationPolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else Color.Transparent)
                                            .clickable {
                                                scope.launch { settingsDataStore.setAlarmVibrationPolicy(policy) }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = policy.localizedLabel(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (alarmVibrationPolicy == AlarmVibrationPolicy.UNIFIED) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.setting_vibrate_alerts),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.desc_vibrate_alerts),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = vibrateAlarmsAndTimers,
                                    onCheckedChange = { checked ->
                                        scope.launch { settingsDataStore.setVibrateAlarmsAndTimers(checked) }
                                    }
                                )
                            }

                            if (vibrateAlarmsAndTimers) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.vibration_pattern),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = alarmVibrationPreset.localizedName(),
                                            fontSize = 13.sp,
                                            color = accentColor,
                                            maxLines = 1
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AlarmVibrationPreset.entries.forEach { preset ->
                                            val isSelected = alarmVibrationPreset == preset
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                                    .clickable {
                                                        scope.launch { settingsDataStore.setAlarmVibrationPreset(preset) }
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = preset.localizedName(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.desc_vibration_per_alarm),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.setting_volume_policy),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = volumePolicy.localizedLabel(),
                                    fontSize = 13.sp,
                                    color = accentColor,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VolumePolicy.entries.forEach { policy ->
                                    val isSelected = volumePolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setVolumePolicy(policy) }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = policy.localizedLabel(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (volumePolicy == VolumePolicy.UNIFIED) {
                            var tempVolume by remember { mutableFloatStateOf(unifiedVolume) }
                            LaunchedEffect(unifiedVolume) { tempVolume = unifiedVolume }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.setting_unified_volume),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%d", (tempVolume * 100).roundToInt())}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Slider(
                                value = tempVolume,
                                onValueChange = { tempVolume = it },
                                onValueChangeFinished = {
                                    scope.launch { settingsDataStore.setUnifiedVolume(tempVolume) }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                        if (defaultAlarmRingtoneUri.isNotBlank()) {
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                                Uri.parse(defaultAlarmRingtoneUri)
                                            )
                                        }
                                    }
                                    alarmRingtonePickerLauncher.launch(intent)
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.default_alarm_sound),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = defaultAlarmRingtoneTitle,
                                    fontSize = 13.sp,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "section_alarm_behavior") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
                    contentColor = cardContentColor,
                    border = null,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_alarm_behavior),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.setting_snooze_duration),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.snooze_duration_mins, ringingSnoozeMinutes),
                                    fontSize = 13.sp,
                                    color = accentColor,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5, 10, 15, 20).forEach { min ->
                                    val isSelected = ringingSnoozeMinutes == min
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setRingingSnoozeMinutes(min) }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.min_short_x, min),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.upcoming_alarm_notification),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.desc_upcoming_notification),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = upcomingEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        settingsDataStore.setUpcomingAlarmEnabled(checked)
                                        onAlarmSettingsChanged()
                                    }
                                }
                            )
                        }
                        if (upcomingEnabled) {
                            var tempUpcomingMinutes by remember { mutableIntStateOf(upcomingMinutesRaw) }
                            LaunchedEffect(upcomingMinutesRaw) { tempUpcomingMinutes = upcomingMinutesRaw }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.lead_time),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.lead_time_min_before, tempUpcomingMinutes),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Slider(
                                value = tempUpcomingMinutes.toFloat(),
                                onValueChange = { tempUpcomingMinutes = it.roundToInt() },
                                onValueChangeFinished = {
                                    scope.launch {
                                        settingsDataStore.setUpcomingAlarmMinutes(tempUpcomingMinutes)
                                        onAlarmSettingsChanged()
                                    }
                                },
                                valueRange = 1f..60f,
                                steps = 58,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.awake_check_title),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.desc_awake_check),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = awakeCheckEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        settingsDataStore.setAwakeCheckEnabled(checked)
                                        onAlarmSettingsChanged()
                                    }
                                }
                            )
                        }
                        if (awakeCheckEnabled) {
                            var tempAwakeMinutes by remember { mutableIntStateOf(awakeCheckMinutes) }
                            LaunchedEffect(awakeCheckMinutes) { tempAwakeMinutes = awakeCheckMinutes }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.awake_check_delay),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.awake_check_delay_min, tempAwakeMinutes),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Slider(
                                value = tempAwakeMinutes.toFloat(),
                                onValueChange = { tempAwakeMinutes = it.roundToInt() },
                                onValueChangeFinished = {
                                    scope.launch { settingsDataStore.setAwakeCheckMinutes(tempAwakeMinutes) }
                                },
                                valueRange = 1f..15f,
                                steps = 13,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item(key = "section_timer") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
                    contentColor = cardContentColor,
                    border = null,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_timer_stopwatch),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                        if (timerRingtoneUri.isNotBlank()) {
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                                Uri.parse(timerRingtoneUri)
                                            )
                                        }
                                    }
                                    timerRingtonePickerLauncher.launch(intent)
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.timer_sound),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = timerRingtoneTitle,
                                    fontSize = 13.sp,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = timerSoundEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setTimerSoundEnabled(checked) }
                                }
                            )
                        }

                        if (timerSoundEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.setting_timer_volume_policy),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = timerVolumePolicy.localizedLabel(),
                                        fontSize = 13.sp,
                                        color = accentColor,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TimerVolumePolicy.entries.forEach { policy ->
                                        val isSelected = timerVolumePolicy == policy
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(50))
                                                .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                                .clickable {
                                                    scope.launch { settingsDataStore.setTimerVolumePolicy(policy) }
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = policy.localizedLabel(),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            if (timerVolumePolicy == TimerVolumePolicy.UNIFIED) {
                                var tempTimerVolume by remember { mutableFloatStateOf(timerUnifiedVolume) }
                                LaunchedEffect(timerUnifiedVolume) { tempTimerVolume = timerUnifiedVolume }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.setting_timer_unified_volume),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%d", (tempTimerVolume * 100).roundToInt())}%",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                Slider(
                                    value = tempTimerVolume,
                                    onValueChange = { tempTimerVolume = it },
                                    onValueChangeFinished = {
                                        scope.launch { settingsDataStore.setTimerUnifiedVolume(tempTimerVolume) }
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.setting_timer_vibration_policy),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = timerVibrationPolicy.localizedLabel(),
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isLight) Color(0xFFF3F4F6) else Color(0xFF282830))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TimerVibrationPolicy.entries.forEach { policy ->
                                    val isSelected = timerVibrationPolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else Color.Transparent)
                                            .clickable {
                                                scope.launch { settingsDataStore.setTimerVibrationPolicy(policy) }
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = policy.localizedLabel(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (timerVibrationPolicy == TimerVibrationPolicy.UNIFIED) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.timer_vibration),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.desc_timer_vibration),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = timerVibrationEnabled,
                                    onCheckedChange = { checked ->
                                        scope.launch { settingsDataStore.setTimerVibrationEnabled(checked) }
                                    }
                                )
                            }

                            if (timerVibrationEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.vibration_pattern),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = timerVibrationPattern.localizedName(),
                                            fontSize = 13.sp,
                                            color = accentColor,
                                            maxLines = 1
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TimerVibrationPattern.entries.forEach { pattern ->
                                            val isSelected = timerVibrationPattern == pattern
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                                    .clickable {
                                                        scope.launch { settingsDataStore.setTimerVibrationPattern(pattern) }
                                                    }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pattern.localizedName(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.desc_vibration_per_timer),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "section_data") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
                    contentColor = cardContentColor,
                    border = null,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_data_storage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showBackupMenu = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.setting_backup_restore),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.desc_backup_restore),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FrostedDropdownMenu(
                                expanded = showBackupMenu,
                                onDismissRequest = { showBackupMenu = false },
                                modifier = Modifier.widthIn(min = 310.dp, max = 345.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val exportSource = remember { MutableInteractionSource() }
                                    val isExportPressed by exportSource.collectIsPressedAsState()
                                    val hasSelection = backupSettings || backupAlarms || backupWorldClock || backupRecycleBin

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isExportPressed && hasSelection) {
                                                    if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)
                                                } else Color.Transparent
                                            )
                                            .clickable(
                                                enabled = hasSelection,
                                                interactionSource = exportSource,
                                                indication = null
                                            ) {
                                                showBackupMenu = false
                                                backupMode = BackupManager.BackupMode.EXPORT
                                                exportLauncher.launch("lucid-clock-backup.json")
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FileUpload,
                                            contentDescription = null,
                                            tint = if (hasSelection) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.btn_export_backup),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (hasSelection) (if (isLight) Color(0xFF111318) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }

                                    val importSource = remember { MutableInteractionSource() }
                                    val isImportPressed by importSource.collectIsPressedAsState()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isImportPressed && hasSelection) {
                                                    if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.1f)
                                                } else Color.Transparent
                                            )
                                            .clickable(
                                                enabled = hasSelection,
                                                interactionSource = importSource,
                                                indication = null
                                            ) {
                                                showBackupMenu = false
                                                backupMode = BackupManager.BackupMode.IMPORT
                                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FileDownload,
                                            contentDescription = null,
                                            tint = if (hasSelection) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.btn_import_backup),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (hasSelection) (if (isLight) Color(0xFF111318) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                    )

                                    Text(
                                        text = stringResource(R.string.desc_include_backup),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CompactBackupPill(
                                            label = stringResource(R.string.chip_settings),
                                            checked = backupSettings,
                                            onToggle = { backupSettings = !backupSettings },
                                            accentColor = accentColor,
                                            isLight = isLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactBackupPill(
                                            label = stringResource(R.string.tab_alarms),
                                            checked = backupAlarms,
                                            onToggle = { backupAlarms = !backupAlarms },
                                            accentColor = accentColor,
                                            isLight = isLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CompactBackupPill(
                                            label = stringResource(R.string.tab_world_clock),
                                            checked = backupWorldClock,
                                            onToggle = { backupWorldClock = !backupWorldClock },
                                            accentColor = accentColor,
                                            isLight = isLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactBackupPill(
                                            label = stringResource(R.string.recycle_bin),
                                            checked = backupRecycleBin,
                                            onToggle = { backupRecycleBin = !backupRecycleBin },
                                            accentColor = accentColor,
                                            isLight = isLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .zIndex(5f)
        ) {
            Surface(
                onClick = {
                    scope.launch {
                        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) return@launch
                        val totalDistance = run {
                            var dist = listState.firstVisibleItemScrollOffset.toFloat()
                            val visible = listState.layoutInfo.visibleItemsInfo
                            val avgHeight = if (visible.isNotEmpty()) visible.map { it.size }.average().toFloat() else 800f
                            dist + listState.firstVisibleItemIndex * avgHeight + 1000f
                        }
                        var prev = 0f
                        listState.scroll {
                            AnimationState(0f).animateTo(
                                targetValue = totalDistance,
                                sequentialAnimation = false,
                                animationSpec = tween(
                                    durationMillis = 350,
                                    easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                                )
                            ) {
                                val delta = value - prev
                                prev = value
                                scrollBy(-delta)
                                if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                    cancelAnimation()
                                }
                            }
                        }
                        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
                            listState.scrollToItem(0, 0)
                        }
                    }
                },
                shape = CircleShape,
                color = if (isLight) Color(0xFFFFFFFF) else Color(0xFF2C2D35),
                contentColor = if (isLight) Color(0xFF1E1E1E) else Color.White,
                shadowElevation = 4.dp,
                border = null,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.action_back_to_top),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = if (currentAccent == AccentColor.CUSTOM) accentCustomArgb else accentColor.toArgb().toLong(),
            onDismiss = { showCustomPicker = false },
            onConfirm = { argb ->
                scope.launch {
                    settingsDataStore.setAccentCustomColor(argb)
                    settingsDataStore.setAccentColor(AccentColor.CUSTOM)
                }
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun FrostedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return

    val menuShape = RoundedCornerShape(20.dp)
    val layoutDir = LocalLayoutDirection.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = menuShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = null
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
            val view = LocalView.current
            SideEffect {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var current: ViewParent? = view.parent
                    while (current != null) {
                        if (current is View) {
                            val lp = current.layoutParams
                            if (lp is WindowManager.LayoutParams) {
                                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                                lp.blurBehindRadius = 60
                                val wm = current.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                                wm?.updateViewLayout(current, lp)
                                break
                            }
                        }
                        current = current.parent
                    }
                }
            }

            FrostedBlurBox(
                modifier = Modifier.fillMaxWidth(),
                shape = menuShape
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CompactBackupPill(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    val pillBg = if (checked) {
        accentColor.copy(alpha = 0.16f)
    } else {
        if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.06f)
    }
    val contentColor = if (checked) {
        accentColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(pillBg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
