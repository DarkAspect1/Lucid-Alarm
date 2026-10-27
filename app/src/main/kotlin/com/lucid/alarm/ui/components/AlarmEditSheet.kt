package com.lucid.alarm.ui.components

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.prefs.AlarmVibrationPreset
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.ui.components.SelectionChipShape
import com.lucid.alarm.ui.components.selectedChipColors
import java.util.Calendar
import kotlin.math.roundToInt

private data class RepeatDay(val calendarConstant: Int, val letter: String)

private val REPEAT_DAYS = listOf(
    RepeatDay(Calendar.SUNDAY, "S"),
    RepeatDay(Calendar.MONDAY, "M"),
    RepeatDay(Calendar.TUESDAY, "T"),
    RepeatDay(Calendar.WEDNESDAY, "W"),
    RepeatDay(Calendar.THURSDAY, "T"),
    RepeatDay(Calendar.FRIDAY, "F"),
    RepeatDay(Calendar.SATURDAY, "S")
)

private val SNOOZE_INTERVAL_OPTIONS = listOf(0, 5, 10, 15, 20, 30)

private val SNOOZE_LIMIT_OPTIONS = listOf(
    -1 to "Continuous",
    3 to "3 times",
    5 to "5 times"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditSheet(
    initialAlarm: AlarmEntity?,
    availableGroups: List<String>,
    showVolumeSlider: Boolean,
    is24Hour: Boolean,
    settingsDataStore: SettingsDataStore,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: 8) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "") }
    var groupName by remember { mutableStateOf(initialAlarm?.groupName ?: AlarmEntity.DEFAULT_GROUP) }
    var selectedDays by remember { mutableStateOf(initialAlarm?.daysOfWeek ?: emptySet()) }
    var soundUri by remember { mutableStateOf(initialAlarm?.soundUri ?: "") }
    var soundTitle by remember { mutableStateOf(initialAlarm?.soundTitle ?: "Default Ringtone") }
    val defaultSoundTitle by settingsDataStore.defaultAlarmRingtoneTitleFlow.collectAsState(
        initial = "System default"
    )
    val displayedSoundTitle = when {
        soundUri.isNotBlank() -> soundTitle
        defaultSoundTitle == "System default" -> "Default Ringtone"
        else -> "Default \u00B7 $defaultSoundTitle"
    }
    var vibrationEnabled by remember { mutableStateOf(initialAlarm?.vibrationEnabled ?: true) }
    var vibrationPattern by remember { mutableStateOf(initialAlarm?.vibrationPattern ?: "HEARTBEAT") }
    var volumeLevel by remember { mutableFloatStateOf(initialAlarm?.volumeLevel ?: 0.85f) }

    var snoozeEnabled by remember { mutableStateOf(initialAlarm?.snoozeEnabled ?: true) }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeMinutes ?: 0) }
    var snoozeLimit by remember { mutableIntStateOf(initialAlarm?.snoozeLimit ?: -1) }

    var areYouAwakeEnabled by remember { mutableStateOf(initialAlarm?.areYouAwakeEnabled ?: false) }
    var areYouAwakeMinutes by remember { mutableIntStateOf(initialAlarm?.areYouAwakeMinutes ?: 0) }
    var upcomingNotificationEnabled by remember {
        mutableStateOf(initialAlarm?.upcomingNotificationEnabled ?: false)
    }
    var upcomingNotificationLeadMinutes by remember {
        mutableIntStateOf(initialAlarm?.upcomingNotificationLeadMinutes ?: 0)
    }
    val globalAreYouAwakeEnabled by settingsDataStore.awakeCheckEnabledFlow.collectAsState(
        initial = false
    )
    val globalUpcomingEnabled by settingsDataStore.upcomingAlarmEnabledFlow.collectAsState(
        initial = false
    )
    var hasCustomAreYouAwake by remember {
        mutableStateOf(initialAlarm?.hasCustomAreYouAwake ?: false)
    }
    var hasCustomUpcoming by remember {
        mutableStateOf(initialAlarm?.hasCustomUpcomingNotification ?: false)
    }
    val effectiveAreYouAwake =
        if (hasCustomAreYouAwake) areYouAwakeEnabled else globalAreYouAwakeEnabled
    val effectiveUpcoming =
        if (hasCustomUpcoming) upcomingNotificationEnabled else globalUpcomingEnabled
    var advancedNotificationsExpanded by remember { mutableStateOf(false) }

    var appearanceEnabled by remember { mutableStateOf(initialAlarm?.customAppearanceEnabled ?: false) }
    var appearanceUri by remember { mutableStateOf(initialAlarm?.customBackgroundUri ?: "") }
    var appearanceBackgroundStyle by remember { mutableStateOf(initialAlarm?.customBackgroundStyle ?: "") }
    var appearanceVideoUri by remember { mutableStateOf(initialAlarm?.customVideoUri ?: "") }
    var appearanceSolidColor by remember { mutableLongStateOf(initialAlarm?.customBackgroundSolidColor ?: 0L) }
    var appearanceColor by remember { mutableLongStateOf(initialAlarm?.customClockColor ?: 0L) }
    var appearanceOffsetX by remember { mutableFloatStateOf(initialAlarm?.customClockOffsetX ?: 0f) }
    var appearanceOffsetY by remember { mutableFloatStateOf(initialAlarm?.customClockOffsetY ?: 0f) }
    var appearanceBoxW by remember { mutableFloatStateOf(initialAlarm?.customBoxWidth ?: 0f) }
    var appearanceBoxH by remember { mutableFloatStateOf(initialAlarm?.customBoxHeight ?: 0f) }
    var appearanceClockBoxEnabled by remember {
        mutableStateOf(initialAlarm?.customClockBoxEnabled ?: false)
    }
    var appearanceClockBoxColor by remember {
        mutableLongStateOf(initialAlarm?.customClockBoxColor ?: 0L)
    }
    var appearanceClockBoxOpacity by remember {
        mutableFloatStateOf(initialAlarm?.customClockBoxOpacity ?: 0.35f)
    }
    var appearanceClockStyle by remember { mutableStateOf(initialAlarm?.customClockStyle ?: "") }
    var appearanceClockFont by remember { mutableStateOf(initialAlarm?.customClockFont ?: "") }
    var appearanceClockFontPath by remember { mutableStateOf(initialAlarm?.customClockFontPath ?: "") }
    var appearanceDateFont by remember { mutableStateOf(initialAlarm?.customDateFont ?: "") }
    var appearanceDateFontPath by remember { mutableStateOf(initialAlarm?.customDateFontPath ?: "") }
    var appearanceDateVisible by remember { mutableStateOf(initialAlarm?.customDateVisible ?: true) }
    var appearanceDateOffsetX by remember { mutableFloatStateOf(initialAlarm?.customDateOffsetX ?: 0f) }
    var appearanceDateOffsetY by remember { mutableFloatStateOf(initialAlarm?.customDateOffsetY ?: 0f) }
    var appearanceDateScale by remember { mutableFloatStateOf(initialAlarm?.customDateScale ?: 1f) }
    var appearanceRemoveLeadingZero by remember {
        mutableStateOf(initialAlarm?.customRemoveLeadingZero ?: false)
    }
    var appearanceBlur by remember { mutableFloatStateOf(initialAlarm?.customBlur ?: -1f) }
    var appearanceDim by remember { mutableFloatStateOf(initialAlarm?.customDim ?: -1f) }
    var appearanceClockColorEnd by remember {
        mutableLongStateOf(initialAlarm?.customClockColorEnd ?: 0L)
    }
    var appearanceClockGradient by remember {
        mutableStateOf(initialAlarm?.customClockGradient ?: false)
    }
    var appearanceClockGradientDir by remember {
        mutableStateOf(initialAlarm?.customClockGradientDir ?: "")
    }
    var appearanceDateColor by remember {
        mutableLongStateOf(initialAlarm?.customDateColor ?: 0L)
    }
    var appearanceDateColorEnd by remember {
        mutableLongStateOf(initialAlarm?.customDateColorEnd ?: 0L)
    }
    var appearanceDateGradient by remember {
        mutableStateOf(initialAlarm?.customDateGradient ?: false)
    }
    var appearanceDateGradientDir by remember {
        mutableStateOf(initialAlarm?.customDateGradientDir ?: "")
    }
    var appearanceShowAmPm by remember { mutableStateOf(initialAlarm?.customShowAmPm) }
    var appearanceAmPmFont by remember { mutableStateOf(initialAlarm?.customAmPmFont ?: "") }
    var appearanceAmPmFontPath by remember {
        mutableStateOf(initialAlarm?.customAmPmFontPath ?: "")
    }
    var appearanceAmPmColor by remember { mutableLongStateOf(initialAlarm?.customAmPmColor ?: 0L) }
    var appearanceLabelVisible by remember { mutableStateOf(initialAlarm?.customAlarmLabelVisible) }
    var appearanceLabelFont by remember {
        mutableStateOf(initialAlarm?.customAlarmLabelFont ?: "")
    }
    var appearanceLabelFontPath by remember {
        mutableStateOf(initialAlarm?.customAlarmLabelFontPath ?: "")
    }
    var appearanceLabelColor by remember {
        mutableLongStateOf(initialAlarm?.customAlarmLabelColor ?: 0L)
    }
    var appearanceSnoozeFont by remember { mutableStateOf(initialAlarm?.customSnoozeFont ?: "") }
    var appearanceSnoozeFontPath by remember {
        mutableStateOf(initialAlarm?.customSnoozeFontPath ?: "")
    }
    var appearanceSnoozeContainerColor by remember {
        mutableLongStateOf(initialAlarm?.customSnoozeContainerColor ?: 0L)
    }
    var appearanceSnoozeTextColor by remember {
        mutableLongStateOf(initialAlarm?.customSnoozeTextColor ?: 0L)
    }
    var appearanceDismissFont by remember { mutableStateOf(initialAlarm?.customDismissFont ?: "") }
    var appearanceDismissFontPath by remember {
        mutableStateOf(initialAlarm?.customDismissFontPath ?: "")
    }
    var appearanceDismissContainerColor by remember {
        mutableLongStateOf(initialAlarm?.customDismissContainerColor ?: 0L)
    }
    var appearanceDismissTextColor by remember {
        mutableLongStateOf(initialAlarm?.customDismissTextColor ?: 0L)
    }

    var groups by remember { mutableStateOf(availableGroups) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var showAppearanceEditor by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.let { data ->
                @Suppress("DEPRECATION")
                data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                soundUri = uri.toString()
                soundTitle = RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                    ?: "Custom Ringtone"
            } else {
                soundUri = ""
                soundTitle = "Default Ringtone"
            }
        }
    }

    val sheetScrollGuard = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return if (available.y < 0f) Offset(0f, available.y) else Offset.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val sheetBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val sheetContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = sheetBg,
        contentColor = sheetContentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (initialAlarm == null) "New Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                VerticalWheelTimePicker(
                    initialHour = hour,
                    initialMinute = minute,
                    is24Hour = is24Hour,
                    onTimeChanged = { h, m ->
                        hour = h
                        minute = m
                    }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(sheetScrollGuard)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = sheetContentColor,
                        unfocusedTextColor = sheetContentColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (isLight) Color(0xFF757575) else Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Repeat", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
                        REPEAT_DAYS.forEach { day ->
                            val selected = selectedDays.contains(day.calendarConstant)
                            val dayBg = when {
                                selected -> MaterialTheme.colorScheme.primary
                                isLight -> Color(0x1F000000)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .background(dayBg)
                                    .clickable {
                                        selectedDays = if (selected) {
                                            selectedDays - day.calendarConstant
                                        } else {
                                            selectedDays + day.calendarConstant
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.letter,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Group",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        FilledIconButton(onClick = {
                            newGroupName = ""
                            showNewGroupDialog = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "New group")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (listOf(AlarmEntity.DEFAULT_GROUP) + groups.filter { it != AlarmEntity.DEFAULT_GROUP })
                            .distinct()
                            .forEach { group ->
                                 FilterChip(
                                     selected = groupName == group,
                                     onClick = { groupName = group },
                                     shape = SelectionChipShape,
                                     colors = selectedChipColors(),
                                     label = { Text(group) }
                                 )
                            }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            displayedSoundTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            if (soundUri.isNotBlank()) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                            }
                        }
                        ringtoneLauncher.launch(intent)
                    }) {
                        Text("Pick")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibrate", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                }
                if (vibrationEnabled) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AlarmVibrationPreset.entries.forEach { preset ->
                             FilterChip(
                                 selected = vibrationPattern == preset.name,
                                 onClick = { vibrationPattern = preset.name },
                                 shape = SelectionChipShape,
                                 colors = selectedChipColors(),
                                 label = { Text(preset.label) }
                             )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snooze", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = snoozeEnabled, onCheckedChange = { snoozeEnabled = it })
                }
                if (snoozeEnabled) {
                    Text(
                        text = "Interval",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SNOOZE_INTERVAL_OPTIONS.forEach { minutes ->
                             FilterChip(
                                 selected = snoozeMinutes == minutes,
                                 onClick = { snoozeMinutes = minutes },
                                 shape = SelectionChipShape,
                                 colors = selectedChipColors(),
                                 label = {
                                     Text(if (minutes == 0) "Default" else "${minutes}m")
                                 }
                             )
                        }
                    }
                    Text(
                        text = "Repeat limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SNOOZE_LIMIT_OPTIONS.forEach { (limit, text) ->
                             FilterChip(
                                 selected = snoozeLimit == limit,
                                 onClick = { snoozeLimit = limit },
                                 shape = SelectionChipShape,
                                 colors = selectedChipColors(),
                                 label = { Text(text) }
                             )
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { advancedNotificationsExpanded = !advancedNotificationsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alert Options", style = MaterialTheme.typography.bodyLarge)
                        Icon(
                            imageVector = if (advancedNotificationsExpanded) {
                                Icons.Outlined.ExpandLess
                            } else {
                                Icons.Outlined.ExpandMore
                            },
                            contentDescription = if (advancedNotificationsExpanded) {
                                "Collapse"
                            } else {
                                "Expand"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (advancedNotificationsExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Are you awake?", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (hasCustomAreYouAwake) {
                                        "Prompts you after the alarm rings"
                                    } else {
                                        "Following global setting (${if (globalAreYouAwakeEnabled) "on" else "off"})"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = effectiveAreYouAwake,
                                onCheckedChange = {
                                    hasCustomAreYouAwake = true
                                    areYouAwakeEnabled = it
                                }
                            )
                        }
                        if (effectiveAreYouAwake) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Check delay",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (areYouAwakeMinutes == 0) {
                                        "Use global"
                                    } else {
                                        "$areYouAwakeMinutes min"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = areYouAwakeMinutes.toFloat(),
                                onValueChange = {
                                    areYouAwakeMinutes = it.roundToInt()
                                    hasCustomAreYouAwake = true
                                    areYouAwakeEnabled = true
                                },
                                valueRange = 0f..15f,
                                steps = 14,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Upcoming alert", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = if (hasCustomUpcoming) {
                                        "Notifies you before this alarm rings"
                                    } else {
                                        "Following global setting (${if (globalUpcomingEnabled) "on" else "off"})"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = effectiveUpcoming,
                                onCheckedChange = {
                                    hasCustomUpcoming = true
                                    upcomingNotificationEnabled = it
                                }
                            )
                        }
                        if (effectiveUpcoming) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lead time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (upcomingNotificationLeadMinutes == 0) {
                                        "Use global"
                                    } else {
                                        "$upcomingNotificationLeadMinutes min before"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = upcomingNotificationLeadMinutes.toFloat(),
                                onValueChange = {
                                    upcomingNotificationLeadMinutes = it.roundToInt()
                                    hasCustomUpcoming = true
                                    upcomingNotificationEnabled = true
                                },
                                valueRange = 0f..60f,
                                steps = 59,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAppearanceEditor = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alarm Appearance", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (appearanceEnabled) {
                                    "Custom wallpaper & clock"
                                } else {
                                    "Uses the global ringing look"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showAppearanceEditor = true }) {
                            Text(if (appearanceEnabled) "Edit" else "Customize")
                        }
                    }
                    if (appearanceEnabled) {
                        TextButton(
                            onClick = {
                                appearanceEnabled = false
                                appearanceUri = ""
                                appearanceBackgroundStyle = ""
                                appearanceVideoUri = ""
                                appearanceSolidColor = 0L
                                appearanceColor = 0L
                                appearanceClockStyle = ""
                                appearanceClockFont = ""
                                appearanceClockFontPath = ""
                                appearanceDateFont = ""
                                appearanceDateFontPath = ""
                                appearanceDateVisible = true
                                appearanceDateOffsetX = 0f
                                appearanceDateOffsetY = 0f
                                appearanceDateScale = 1f
                                appearanceRemoveLeadingZero = false
                                appearanceBlur = -1f
                                appearanceDim = -1f
                                appearanceClockColorEnd = 0L
                                appearanceClockGradient = false
                                appearanceClockGradientDir = ""
                                appearanceDateColor = 0L
                                appearanceDateColorEnd = 0L
                                appearanceDateGradient = false
                                appearanceDateGradientDir = ""
                                appearanceShowAmPm = null
                                appearanceAmPmFont = ""
                                appearanceAmPmFontPath = ""
                                appearanceAmPmColor = 0L
                                appearanceLabelVisible = null
                                appearanceLabelFont = ""
                                appearanceLabelFontPath = ""
                                appearanceLabelColor = 0L
                                appearanceSnoozeFont = ""
                                appearanceSnoozeFontPath = ""
                                appearanceSnoozeContainerColor = 0L
                                appearanceSnoozeTextColor = 0L
                                appearanceDismissFont = ""
                                appearanceDismissFontPath = ""
                                appearanceDismissContainerColor = 0L
                                appearanceDismissTextColor = 0L
                                appearanceOffsetX = 0f
                                appearanceOffsetY = 0f
                                appearanceBoxW = 0f
                                appearanceBoxH = 0f
                                appearanceClockBoxEnabled = false
                                appearanceClockBoxColor = 0L
                                appearanceClockBoxOpacity = 0.35f
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Reset to global appearance")
                        }
                    }
                }

                if (showVolumeSlider) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Alarm Volume", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${(volumeLevel * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = volumeLevel,
                            onValueChange = { volumeLevel = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            val focusManager = LocalFocusManager.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val repeatDays = selectedDays.sorted().joinToString(",")
                            val resolvedGroup = groupName.ifBlank { AlarmEntity.DEFAULT_GROUP }
                            val resolvedAppearance =
                                appearanceEnabled || appearanceUri.isNotBlank() ||
                                    appearanceColor != 0L || appearanceShowAmPm != null ||
                                    appearanceClockBoxEnabled ||
                                    appearanceAmPmColor != 0L || appearanceLabelVisible != null ||
                                    appearanceLabelColor != 0L || appearanceSnoozeContainerColor != 0L ||
                                    appearanceSnoozeTextColor != 0L ||
                                    appearanceDismissContainerColor != 0L ||
                                    appearanceDismissTextColor != 0L
                            val base = initialAlarm
                            val newAlarm = AlarmEntity(
                                id = base?.id ?: 0,
                                hour = hour,
                                minute = minute,
                                label = label,
                                isEnabled = true,
                                repeatDays = repeatDays,
                                groupName = resolvedGroup,
                                soundUri = soundUri,
                                soundTitle = soundTitle,
                                vibrationPattern = vibrationPattern,
                                vibrationEnabled = vibrationEnabled,
                                snoozeEnabled = snoozeEnabled,
                                snoozeMinutes = snoozeMinutes,
                                snoozeLimit = snoozeLimit,
                                areYouAwakeEnabled = if (hasCustomAreYouAwake || areYouAwakeMinutes > 0) (if (hasCustomAreYouAwake) areYouAwakeEnabled else effectiveAreYouAwake) else effectiveAreYouAwake,
                                areYouAwakeMinutes = if (effectiveAreYouAwake) areYouAwakeMinutes else 0,
                                upcomingNotificationEnabled = if (hasCustomUpcoming || upcomingNotificationLeadMinutes > 0) (if (hasCustomUpcoming) upcomingNotificationEnabled else effectiveUpcoming) else effectiveUpcoming,
                                upcomingNotificationLeadMinutes =
                                    if (effectiveUpcoming) upcomingNotificationLeadMinutes else 0,
                                hasCustomAreYouAwake = hasCustomAreYouAwake || areYouAwakeMinutes > 0,
                                hasCustomUpcomingNotification = hasCustomUpcoming || upcomingNotificationLeadMinutes > 0,
                                customAppearanceEnabled = resolvedAppearance,
                                customBackgroundUri = if (resolvedAppearance) appearanceUri else "",
                                customBackgroundStyle = if (resolvedAppearance) appearanceBackgroundStyle else "",
                                customVideoUri = if (resolvedAppearance) appearanceVideoUri else "",
                                customBackgroundSolidColor = if (resolvedAppearance) appearanceSolidColor else 0L,
                                customClockColor = if (resolvedAppearance) appearanceColor else 0L,
                                customClockOffsetX = if (resolvedAppearance) appearanceOffsetX else 0f,
                                customClockOffsetY = if (resolvedAppearance) appearanceOffsetY else 0f,
                                customBoxWidth = if (resolvedAppearance) appearanceBoxW else 0f,
                                customBoxHeight = if (resolvedAppearance) appearanceBoxH else 0f,
                                customClockBoxEnabled =
                                    if (resolvedAppearance) appearanceClockBoxEnabled else false,
                                customClockBoxColor =
                                    if (resolvedAppearance) appearanceClockBoxColor else 0L,
                                customClockBoxOpacity =
                                    if (resolvedAppearance) appearanceClockBoxOpacity else 0.35f,
                                customClockStyle = if (resolvedAppearance) appearanceClockStyle else "",
                                customClockFont = if (resolvedAppearance) appearanceClockFont else "",
                                customClockFontPath = if (resolvedAppearance) appearanceClockFontPath else "",
                                customDateFont = if (resolvedAppearance) appearanceDateFont else "",
                                customDateFontPath = if (resolvedAppearance) appearanceDateFontPath else "",
                                customDateVisible = appearanceDateVisible,
                                customDateOffsetX = if (resolvedAppearance) appearanceDateOffsetX else 0f,
                                customDateOffsetY = if (resolvedAppearance) appearanceDateOffsetY else 0f,
                                customDateScale = if (resolvedAppearance) appearanceDateScale else 1f,
                                customRemoveLeadingZero = appearanceRemoveLeadingZero,
                                customBlur = if (resolvedAppearance) appearanceBlur else -1f,
                                customDim = if (resolvedAppearance) appearanceDim else -1f,
                                customClockColorEnd = if (resolvedAppearance) appearanceClockColorEnd else 0L,
                                customClockGradient = if (resolvedAppearance) appearanceClockGradient else false,
                                customClockGradientDir = if (resolvedAppearance) appearanceClockGradientDir else "",
                                customDateColor = if (resolvedAppearance) appearanceDateColor else 0L,
                                customDateColorEnd = if (resolvedAppearance) appearanceDateColorEnd else 0L,
                                customDateGradient = if (resolvedAppearance) appearanceDateGradient else false,
                                customDateGradientDir = if (resolvedAppearance) appearanceDateGradientDir else "",
                                customShowAmPm = if (resolvedAppearance) appearanceShowAmPm else null,
                                customAmPmFont = if (resolvedAppearance) appearanceAmPmFont else "",
                                customAmPmFontPath = if (resolvedAppearance) appearanceAmPmFontPath else "",
                                customAmPmColor = if (resolvedAppearance) appearanceAmPmColor else 0L,
                                customAlarmLabelVisible = if (resolvedAppearance) appearanceLabelVisible else null,
                                customAlarmLabelFont = if (resolvedAppearance) appearanceLabelFont else "",
                                customAlarmLabelFontPath = if (resolvedAppearance) appearanceLabelFontPath else "",
                                customAlarmLabelColor = if (resolvedAppearance) appearanceLabelColor else 0L,
                                customSnoozeFont = if (resolvedAppearance) appearanceSnoozeFont else "",
                                customSnoozeFontPath = if (resolvedAppearance) appearanceSnoozeFontPath else "",
                                customSnoozeContainerColor = if (resolvedAppearance) appearanceSnoozeContainerColor else 0L,
                                customSnoozeTextColor = if (resolvedAppearance) appearanceSnoozeTextColor else 0L,
                                customDismissFont = if (resolvedAppearance) appearanceDismissFont else "",
                                customDismissFontPath = if (resolvedAppearance) appearanceDismissFontPath else "",
                                customDismissContainerColor = if (resolvedAppearance) appearanceDismissContainerColor else 0L,
                                customDismissTextColor = if (resolvedAppearance) appearanceDismissTextColor else 0L,
                                createdAt = base?.createdAt ?: System.currentTimeMillis(),
                                volumeLevel = volumeLevel,
                                sortOrder = base?.sortOrder ?: 0,
                                deletedAt = base?.deletedAt ?: 0L
                            )
                            onSave(newAlarm)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            containerColor = sheetBg,
            titleContentColor = sheetContentColor,
            textContentColor = sheetContentColor,
            title = { Text("New group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = sheetContentColor,
                        unfocusedTextColor = sheetContentColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (isLight) Color(0xFF757575) else Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newGroupName.trim()
                    if (name.isNotEmpty()) {
                        groups = (groups + name).distinct()
                        groupName = name
                    }
                    showNewGroupDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppearanceEditor) {
        AlarmAppearanceEditor(
            initial = AlarmAppearance(
                backgroundUri = appearanceUri,
                backgroundStyle = appearanceBackgroundStyle,
                videoUri = appearanceVideoUri,
                backgroundSolidColor = appearanceSolidColor,
                clockColor = appearanceColor,
                offsetX = appearanceOffsetX,
                offsetY = appearanceOffsetY,
                boxWidth = appearanceBoxW,
                boxHeight = appearanceBoxH,
                clockBoxEnabled = appearanceClockBoxEnabled,
                clockBoxColor = appearanceClockBoxColor,
                clockBoxOpacity = appearanceClockBoxOpacity,
                clockStyle = appearanceClockStyle,
                clockFont = appearanceClockFont,
                clockFontPath = appearanceClockFontPath,
                dateFont = appearanceDateFont,
                dateFontPath = appearanceDateFontPath,
                dateVisible = appearanceDateVisible,
                dateOffsetX = appearanceDateOffsetX,
                dateOffsetY = appearanceDateOffsetY,
                dateScale = appearanceDateScale,
                removeLeadingZero = appearanceRemoveLeadingZero,
                blur = appearanceBlur,
                dim = appearanceDim,
                clockColorEnd = appearanceClockColorEnd,
                clockGradient = appearanceClockGradient,
                clockGradientDir = appearanceClockGradientDir,
                dateColor = appearanceDateColor,
                dateColorEnd = appearanceDateColorEnd,
                dateGradient = appearanceDateGradient,
                dateGradientDir = appearanceDateGradientDir,
                showAmPm = appearanceShowAmPm,
                amPmFont = appearanceAmPmFont,
                amPmFontPath = appearanceAmPmFontPath,
                amPmColor = appearanceAmPmColor,
                alarmLabelVisible = appearanceLabelVisible,
                alarmLabelFont = appearanceLabelFont,
                alarmLabelFontPath = appearanceLabelFontPath,
                alarmLabelColor = appearanceLabelColor,
                snoozeFont = appearanceSnoozeFont,
                snoozeFontPath = appearanceSnoozeFontPath,
                snoozeContainerColor = appearanceSnoozeContainerColor,
                snoozeTextColor = appearanceSnoozeTextColor,
                dismissFont = appearanceDismissFont,
                dismissFontPath = appearanceDismissFontPath,
                dismissContainerColor = appearanceDismissContainerColor,
                dismissTextColor = appearanceDismissTextColor
            ),
            settingsDataStore = settingsDataStore,
            fontBaseSuffix = (initialAlarm?.id ?: 0).toString(),
            usesGlobalLook = !appearanceEnabled,
            onDismiss = { showAppearanceEditor = false },
            onSave = { result ->
                appearanceUri = result.backgroundUri
                appearanceBackgroundStyle = result.backgroundStyle
                appearanceVideoUri = result.videoUri
                appearanceSolidColor = result.backgroundSolidColor
                appearanceColor = result.clockColor
                appearanceOffsetX = result.offsetX
                appearanceOffsetY = result.offsetY
                appearanceBoxW = result.boxWidth
                appearanceBoxH = result.boxHeight
                appearanceClockBoxEnabled = result.clockBoxEnabled
                appearanceClockBoxColor = result.clockBoxColor
                appearanceClockBoxOpacity = result.clockBoxOpacity
                appearanceClockStyle = result.clockStyle
                appearanceClockFont = result.clockFont
                appearanceClockFontPath = result.clockFontPath
                appearanceDateFont = result.dateFont
                appearanceDateFontPath = result.dateFontPath
                appearanceDateVisible = result.dateVisible
                appearanceDateOffsetX = result.dateOffsetX
                appearanceDateOffsetY = result.dateOffsetY
                appearanceDateScale = result.dateScale
                appearanceRemoveLeadingZero = result.removeLeadingZero
                appearanceBlur = result.blur
                appearanceDim = result.dim
                appearanceClockColorEnd = result.clockColorEnd
                appearanceClockGradient = result.clockGradient
                appearanceClockGradientDir = result.clockGradientDir
                appearanceDateColor = result.dateColor
                appearanceDateColorEnd = result.dateColorEnd
                appearanceDateGradient = result.dateGradient
                appearanceDateGradientDir = result.dateGradientDir
                appearanceShowAmPm = result.showAmPm
                appearanceAmPmFont = result.amPmFont
                appearanceAmPmFontPath = result.amPmFontPath
                appearanceAmPmColor = result.amPmColor
                appearanceLabelVisible = result.alarmLabelVisible
                appearanceLabelFont = result.alarmLabelFont
                appearanceLabelFontPath = result.alarmLabelFontPath
                appearanceLabelColor = result.alarmLabelColor
                appearanceSnoozeFont = result.snoozeFont
                appearanceSnoozeFontPath = result.snoozeFontPath
                appearanceSnoozeContainerColor = result.snoozeContainerColor
                appearanceSnoozeTextColor = result.snoozeTextColor
                appearanceDismissFont = result.dismissFont
                appearanceDismissFontPath = result.dismissFontPath
                appearanceDismissContainerColor = result.dismissContainerColor
                appearanceDismissTextColor = result.dismissTextColor
                appearanceEnabled = true
                showAppearanceEditor = false
            }
        )
    }
}
