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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.BackupManager
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.ThemeMode
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.data.prefs.VolumePolicy
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.components.CustomColorPickerDialog
import com.lucid.alarm.ui.components.FrostedBlurBox
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
    val vibrateAlarmsAndTimers by settingsDataStore.vibrateAlarmsAndTimersFlow.collectAsState(initial = true)
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

    var showAccentMenu by remember { mutableStateOf(false) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var showAppFontMenu by remember { mutableStateOf(false) }

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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
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
                            text = "General & Appearance",
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
                                    text = "Theme mode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = themeMode.label,
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
                                            text = mode.label.substringBefore(" "),
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
                                                        imageVector = Icons.Filled.Check,
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
                                                color = if (isLight) Color(0xFF111318) else Color.White
                                            )
                                            Text(
                                                text = "HSV spectrum & Hex",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
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
                                                text = style.label,
                                                fontSize = 15.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) accentColor else (if (isLight) Color(0xFF111318) else Color.White)
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
                            text = "Audio & Volume Policy",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

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

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Volume policy",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = volumePolicy.label,
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
                                            text = if (policy == VolumePolicy.UNIFIED) "Unified" else "Per-alarm",
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
                            text = "Alarm Behavior",
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
                                    text = "Snooze duration",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$ringingSnoozeMinutes minutes",
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
                                            text = "${min}m",
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
                            text = "Timer & Stopwatch",
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
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = pattern.label.substringBefore(" "),
                                                fontSize = 13.sp,
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
                            text = "Data & Storage",
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
                                            imageVector = Icons.Outlined.FileUpload,
                                            contentDescription = null,
                                            tint = if (hasSelection) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Export Backup (.json)",
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
                                            imageVector = Icons.Outlined.FileDownload,
                                            contentDescription = null,
                                            tint = if (hasSelection) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Import Backup (.json)",
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
                                        text = "Include in backup / restore:",
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
                                            label = "Settings",
                                            checked = backupSettings,
                                            onToggle = { backupSettings = !backupSettings },
                                            accentColor = accentColor,
                                            isLight = isLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactBackupPill(
                                            label = "Alarms",
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
                                            label = "World Clock",
                                            checked = backupWorldClock,
                                            onToggle = { backupWorldClock = !backupWorldClock },
                                            accentColor = accentColor,
                                            isLight = isLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                        CompactBackupPill(
                                            label = "Recycle Bin",
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
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Back to top",
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
                imageVector = Icons.Filled.Check,
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
