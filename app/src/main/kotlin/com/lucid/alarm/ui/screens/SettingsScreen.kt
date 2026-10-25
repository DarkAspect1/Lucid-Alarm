package com.lucid.alarm.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.BackupManager
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TemperatureUnit
import com.lucid.alarm.data.prefs.ThemeMode
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.data.prefs.VolumePolicy
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.components.CustomColorPickerDialog
import com.lucid.alarm.ui.theme.toColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    // One UI background colors
    val screenBg = if (isLight) Color(0xFFF2F2F7) else Color(0xFF000000)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val cardBorder = if (isLight) Color(0xFFE5E7EB) else Color.Transparent
    val accentColor = MaterialTheme.colorScheme.primary

    // General & Appearance
    val themeMode by settingsDataStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(initial = TimeFormat.TWELVE_HOUR)
    val removeLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(initial = true)
    val currentAccent by settingsDataStore.accentColorFlow.collectAsState(initial = AccentColor.DEFAULT)
    val accentCustomArgb by settingsDataStore.accentCustomColorFlow.collectAsState(
        initial = AccentColor.DEFAULT.argb
    )
    val appFontStyle by settingsDataStore.appFontStyleFlow.collectAsState(initial = AppFontStyle.SYSTEM_DEFAULT)

    // Volume & Sound
    val volumePolicy by settingsDataStore.volumePolicyFlow.collectAsState(initial = VolumePolicy.UNIFIED)
    val unifiedVolume by settingsDataStore.unifiedVolumeFlow.collectAsState(initial = 0.85f)
    val vibrateAlarmsAndTimers by settingsDataStore.vibrateAlarmsAndTimersFlow.collectAsState(initial = true)
    val defaultAlarmRingtoneUri by settingsDataStore.defaultAlarmRingtoneUriFlow.collectAsState(initial = "")
    val defaultAlarmRingtoneTitle by settingsDataStore.defaultAlarmRingtoneTitleFlow.collectAsState(
        initial = "System default"
    )

    // Alarm Behavior
    val ringingSnoozeMinutes by settingsDataStore.ringingSnoozeMinutesFlow.collectAsState(initial = 5)
    val upcomingEnabled by settingsDataStore.upcomingAlarmEnabledFlow.collectAsState(initial = false)
    val upcomingMinutesRaw by settingsDataStore.rawUpcomingAlarmMinutesFlow.collectAsState(initial = 15)
    val awakeCheckEnabled by settingsDataStore.awakeCheckEnabledFlow.collectAsState(initial = false)
    val awakeCheckMinutes by settingsDataStore.awakeCheckMinutesFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_AWAKE_CHECK_MINUTES
    )

    // World Clock
    val worldClockShowWeather by settingsDataStore.worldClockShowWeatherFlow.collectAsState(initial = true)
    val worldClockTempUnit by settingsDataStore.worldClockTemperatureUnitFlow.collectAsState(
        initial = TemperatureUnit.CELSIUS
    )

    // Timer
    val timerSoundEnabled by settingsDataStore.timerSoundEnabledFlow.collectAsState(initial = true)
    val timerRingtoneTitle by settingsDataStore.timerRingtoneTitleFlow.collectAsState(
        initial = "Default Timer Sound"
    )
    val timerRingtoneUri by settingsDataStore.timerRingtoneUriFlow.collectAsState(initial = "")
    val timerVibrationEnabled by settingsDataStore.timerVibrationEnabledFlow.collectAsState(initial = true)
    val timerVibrationPattern by settingsDataStore.timerVibrationPatternFlow.collectAsState(
        initial = TimerVibrationPattern.TICK_TICK
    )

    var showAccentDialog by remember { mutableStateOf(false) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var showAppFontDialog by remember { mutableStateOf(false) }

    val appFontFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val importedPath = FontManager.importFontFromUri(context, uri, FontManager.APP_FONT_BASE)
                if (importedPath.isNotBlank()) {
                    settingsDataStore.setAppCustomFontPath(importedPath)
                    settingsDataStore.setAppFontStyle(AppFontStyle.CUSTOM_FILE)
                    Toast.makeText(context, "App font applied successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to load font file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Ringtone Pickers
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

    // Granular backup / restore (Storage Access Framework)
    var showBackupDialog by remember { mutableStateOf(false) }
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
                    "Backup saved (${result.applied.joinToString(", ")})"
                } else {
                    "Backup failed: ${result.error}"
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
                    "Restored (${result.applied.joinToString(", ")})"
                } else {
                    "Restore failed: ${result.error}"
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
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 48
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: < Clock settings
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
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clock settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // SECTION 1: General & Appearance
            item(key = "section_general") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
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
                            text = "General & Appearance",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Theme mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Theme mode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = themeMode.label,
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ThemeMode.entries.forEach { mode ->
                                    val isSelected = themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setThemeMode(mode) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label.substringBefore(" "),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 2. 24-hour format
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "24-hour format",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (timeFormat.is24Hour) "Using 24-hour time (13:00)" else "Using 12-hour time (1:00 pm)",
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

                        // 3. Remove leading zero
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Remove leading zero",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Show 9:00 instead of 09:00",
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

                        // 4. Accent color picker
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAccentDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accent color",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentAccent == AccentColor.CUSTOM) "Custom" else currentAccent.label,
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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 5. App font
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAppFontDialog = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App font",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = appFontStyle.label,
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 6. Alarm appearance
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
                                    text = "Alarm appearance",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Customize layout, clock font, size & colors",
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // SECTION 2: Audio & Volume Policy
            item(key = "section_audio") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
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
                            text = "Audio & Volume Policy",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Vibrate for alarms and timers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Vibrate for alarms and timers",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vibrate device while alerts sound",
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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 2. Volume policy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Volume policy",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = volumePolicy.label,
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                VolumePolicy.entries.forEach { policy ->
                                    val isSelected = volumePolicy == policy
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setVolumePolicy(policy) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (policy == VolumePolicy.UNIFIED) "Unified" else "Per-alarm",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Unified volume slider
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
                                    text = "Unified alarm volume",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(tempVolume * 100).roundToInt()}%",
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

                        // 4. Default alarm sound
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
                                    text = "Default alarm sound",
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
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // SECTION 3: Alarm Behavior
            item(key = "section_alarm_behavior") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
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
                            text = "Alarm Behavior",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Snooze duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Snooze duration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$ringingSnoozeMinutes minutes",
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5, 10, 15, 20).forEach { min ->
                                    val isSelected = ringingSnoozeMinutes == min
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setRingingSnoozeMinutes(min) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${min}m",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 2. Upcoming alarm notification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Upcoming alarm notification",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Show pre-alarm heads-up reminder",
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
                                    text = "Lead time",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$tempUpcomingMinutes min before",
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

                        // 3. Are you awake? check
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Are you awake? check",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Confirm you're awake after dismissing",
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
                                    text = "Check delay",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$tempAwakeMinutes min",
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

            // SECTION 4: World Clock
            item(key = "section_world_clock") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
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
                            text = "World Clock",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Show weather
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show weather",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Show temperature on city cards",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = worldClockShowWeather,
                                onCheckedChange = { checked ->
                                    scope.launch { settingsDataStore.setWorldClockShowWeather(checked) }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 2. Temperature unit (°C / °F)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Temperature unit",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = worldClockTempUnit.label,
                                    fontSize = 13.sp,
                                    color = accentColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TemperatureUnit.entries.forEach { unit ->
                                    val isSelected = worldClockTempUnit == unit
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) accentColor else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                            .clickable {
                                                scope.launch { settingsDataStore.setWorldClockTemperatureUnit(unit) }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unit.symbol,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: Timer & Stopwatch
            item(key = "section_timer") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
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
                            text = "Timer & Stopwatch",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Timer sound with picker
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
                                    text = "Timer sound",
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

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // 2. Timer vibration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Timer vibration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vibrate when timer completes",
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
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            // 3. Vibration pattern
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
                                        text = "Vibration pattern",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = timerVibrationPattern.label,
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
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pattern.label.substringBefore(" "),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 6: Data & Storage
            item(key = "section_data") {
                Surface(
                    shape = cardShape,
                    color = cardBg,
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
                            text = "Data & Storage",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Backup & restore
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    backupMode = BackupManager.BackupMode.EXPORT
                                    showBackupDialog = true
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Backup & restore",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Export or restore clock data",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        val curIndex = listState.firstVisibleItemIndex
                        val curOffset = listState.firstVisibleItemScrollOffset
                        var totalDistance = curOffset.toFloat()
                        for (i in 0 until curIndex) {
                            totalDistance += (itemHeights[i] ?: 600) + spacingPx
                        }
                        val targetDistance = totalDistance + 100f
                        listState.scroll(MutatePriority.Default) {
                            var prevValue = 0f
                            val anim = AnimationState(initialValue = 0f)
                            val duration = (totalDistance / 7f).toInt().coerceIn(320, 480)
                            anim.animateTo(
                                targetValue = targetDistance,
                                animationSpec = tween(
                                    durationMillis = duration,
                                    easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
                                )
                            ) {
                                val delta = value - prevValue
                                prevValue = value
                                scrollBy(-delta)
                                if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                                    cancelAnimation()
                                }
                            }
                        }
                        listState.scrollToItem(0, 0)
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
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Back to top",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // App Font Picker Dialog
    if (showAppFontDialog) {
        AlertDialog(
            onDismissRequest = { showAppFontDialog = false },
            title = { Text("Choose App Font") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFontStyle.entries.forEach { style ->
                        val isSelected = appFontStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (style == AppFontStyle.CUSTOM_FILE) {
                                        showAppFontDialog = false
                                        appFontFilePickerLauncher.launch(FontManager.FONT_MIME_TYPES)
                                    } else {
                                        scope.launch { settingsDataStore.setAppFontStyle(style) }
                                        showAppFontDialog = false
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = style.label,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppFontDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Accent Color Picker Dialog (Presets + Custom Color Option)
    if (showAccentDialog) {
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Choose Accent Color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val presets = AccentColor.entries.filter { it != AccentColor.CUSTOM }
                    presets.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { accent ->
                                val selected = currentAccent == accent
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(accent.toColor())
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            scope.launch { settingsDataStore.setAccentColor(accent) }
                                            showAccentDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom color entry with full spectrum gradient
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAccentDialog = false
                                showCustomPicker = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color.Red, Color.Yellow, Color.Green,
                                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                            )
                                        )
                                    )
                                    .border(
                                        width = if (currentAccent == AccentColor.CUSTOM) 3.dp else 1.dp,
                                        color = if (currentAccent == AccentColor.CUSTOM) MaterialTheme.colorScheme.primary else Color.White,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentAccent == AccentColor.CUSTOM) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Custom Color",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Full HSV spectrum & Hex picker",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Custom Color Picker Dialog
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

    // Backup & Restore Dialog
    if (showBackupDialog) {
        val exporting = backupMode == BackupManager.BackupMode.EXPORT
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(if (exporting) "Export backup" else "Import backup") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { backupMode = BackupManager.BackupMode.EXPORT },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Export")
                        }
                        OutlinedButton(
                            onClick = { backupMode = BackupManager.BackupMode.IMPORT },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Import")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (exporting) {
                            "Choose what to include in the backup file."
                        } else {
                            "Choose what to restore from the backup file. Selected categories replace their current data."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BackupOptionRow(
                        label = "App settings & appearance",
                        checked = backupSettings,
                        onCheckedChange = { backupSettings = it }
                    )
                    BackupOptionRow(
                        label = "Alarms",
                        checked = backupAlarms,
                        onCheckedChange = { backupAlarms = it }
                    )
                    BackupOptionRow(
                        label = "World-clock cities",
                        checked = backupWorldClock,
                        onCheckedChange = { backupWorldClock = it }
                    )
                    BackupOptionRow(
                        label = "Recycle bin",
                        checked = backupRecycleBin,
                        onCheckedChange = { backupRecycleBin = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selection = currentSelection()
                        showBackupDialog = false
                        if (exporting) {
                            exportLauncher.launch("lucid-clock-backup.json")
                        } else {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    },
                    enabled = backupSettings || backupAlarms || backupWorldClock || backupRecycleBin
                ) {
                    Text("Choose file…")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BackupOptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
