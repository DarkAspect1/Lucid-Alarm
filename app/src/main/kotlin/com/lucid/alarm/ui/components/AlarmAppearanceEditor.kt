package com.lucid.alarm.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lucid.alarm.data.prefs.ClockDateFont
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.data.prefs.RingingBackgroundStyle
import com.lucid.alarm.data.prefs.RingingClockFont
import com.lucid.alarm.ui.LocalAppFont
import com.lucid.alarm.data.prefs.RingingClockStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.rememberFontPicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/** Immutable snapshot of every per-alarm appearance override ("" and negative sentinels = inherit). */
data class AlarmAppearance(
    val backgroundUri: String = "",
    val backgroundStyle: String = "",
    val videoUri: String = "",
    val backgroundSolidColor: Long = 0L,
    val clockColor: Long = 0L,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val boxWidth: Float = 0f,
    val boxHeight: Float = 0f,
    val clockBoxEnabled: Boolean = false,
    val clockBoxColor: Long = 0L,
    val clockBoxOpacity: Float = 0.35f,
    val clockStyle: String = "",
    val clockFont: String = "",
    val clockFontPath: String = "",
    val dateFont: String = "",
    val dateFontPath: String = "",
    val dateVisible: Boolean = true,
    val dateOffsetX: Float = 0f,
    val dateOffsetY: Float = 0f,
    val dateScale: Float = 1f,
    val removeLeadingZero: Boolean = false,
    val blur: Float = -1f,
    val dim: Float = -1f,
    val clockColorEnd: Long = 0L,
    val clockGradient: Boolean = false,
    val clockGradientDir: String = "",
    val dateColor: Long = 0L,
    val dateColorEnd: Long = 0L,
    val dateGradient: Boolean = false,
    val dateGradientDir: String = "",
    val showAmPm: Boolean? = null,
    val amPmFont: String = "",
    val amPmFontPath: String = "",
    val amPmColor: Long = 0L,
    val alarmLabelVisible: Boolean? = null,
    val alarmLabelFont: String = "",
    val alarmLabelFontPath: String = "",
    val alarmLabelColor: Long = 0L,
    val snoozeFont: String = "",
    val snoozeFontPath: String = "",
    val snoozeContainerColor: Long = 0L,
    val snoozeTextColor: Long = 0L,
    val dismissFont: String = "",
    val dismissFontPath: String = "",
    val dismissContainerColor: Long = 0L,
    val dismissTextColor: Long = 0L
)

/**
 * Per-alarm appearance editor: a full-screen, immersive copy of
 * [com.lucid.alarm.ui.screens.AlarmCustomizationScreen] that persists its overrides onto the
 * alarm's own entity columns (background style/image/video, clock face/font/Color/placement/size
 * and the decoupled date's font/placement/scale). Blank fields inherit the global ringing setting
 * so the ringing screen renders exactly what this editor previews.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmAppearanceEditor(
    initial: AlarmAppearance,
    settingsDataStore: SettingsDataStore,
    fontBaseSuffix: String,
    onDismiss: () -> Unit,
    onSave: (AlarmAppearance) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // ---- Global (inherited) settings used for preview + fallback ----
    val globalBackground by settingsDataStore.ringingBackgroundFlow.collectAsState(
        initial = RingingBackgroundStyle.STARFIELD
    )
    val globalBackgroundImageUri by settingsDataStore.ringingBackgroundImageUriFlow.collectAsState(
        initial = ""
    )
    val globalVideoUri by settingsDataStore.ringingVideoUriFlow.collectAsState(initial = "")
    val globalSolidColor by settingsDataStore.ringingSolidColorFlow.collectAsState(initial = 0L)
    val globalClockFont by settingsDataStore.alarmClockFontStyleFlow.collectAsState(
        initial = RingingClockFont.SYSTEM_DEFAULT
    )
    val globalClockFontPath by settingsDataStore.alarmClockCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val globalDateFont by settingsDataStore.clockDateFontFlow.collectAsState(
        initial = ClockDateFont.MATCH_CLOCK
    )
    val globalDateFontPath by settingsDataStore.clockDateCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val globalClockStyle by settingsDataStore.ringingClockStyleFlow.collectAsState(
        initial = RingingClockStyle.HORIZONTAL_DIGITAL
    )
    val globalRemoveLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(
        initial = true
    )
    val globalBlur by settingsDataStore.ringingBlurFlow.collectAsState(initial = 0f)
    val globalDim by settingsDataStore.ringingDimFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_DIM
    )
    val globalClockColor by settingsDataStore.clockColorFlow.collectAsState(initial = 0L)
    val globalClockGradient by settingsDataStore.clockGradientFlow.collectAsState(initial = false)
    val globalClockColorEnd by settingsDataStore.clockColorEndFlow.collectAsState(initial = 0L)
    val globalClockGradientDir by settingsDataStore.clockGradientDirFlow.collectAsState(
        initial = GradientDirection.VERTICAL
    )
    val globalDateColor by settingsDataStore.dateColorFlow.collectAsState(initial = 0L)
    val globalDateGradient by settingsDataStore.dateGradientFlow.collectAsState(initial = false)
    val globalDateColorEnd by settingsDataStore.dateColorEndFlow.collectAsState(initial = 0L)
    val globalDateGradientDir by settingsDataStore.dateGradientDirFlow.collectAsState(
        initial = GradientDirection.VERTICAL
    )
    val clockFontSize by settingsDataStore.clockFontSizeFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_CLOCK_FONT_SIZE
    )
    val showAmPm by settingsDataStore.showAmPmFlow.collectAsState(initial = true)
    val alarmLabelVisible by settingsDataStore.alarmLabelVisibleFlow.collectAsState(initial = true)
    val amPmFont by settingsDataStore.amPmFontStyleFlow.collectAsState(
        initial = RingingClockFont.SYSTEM_DEFAULT
    )
    val amPmCustomFontPath by settingsDataStore.amPmCustomFontPathFlow.collectAsState(initial = "")
    val amPmColor by settingsDataStore.amPmColorFlow.collectAsState(initial = 0L)
    val alarmLabelFont by settingsDataStore.alarmLabelFontStyleFlow.collectAsState(
        initial = RingingClockFont.SYSTEM_DEFAULT
    )
    val alarmLabelCustomFontPath by settingsDataStore.alarmLabelCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val alarmLabelColor by settingsDataStore.alarmLabelColorFlow.collectAsState(initial = 0L)
    val snoozeFont by settingsDataStore.snoozeFontStyleFlow.collectAsState(
        initial = RingingClockFont.SYSTEM_DEFAULT
    )
    val snoozeCustomFontPath by settingsDataStore.snoozeCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val snoozeContainerColor by settingsDataStore.snoozeContainerColorFlow.collectAsState(
        initial = 0L
    )
    val snoozeTextColor by settingsDataStore.snoozeTextColorFlow.collectAsState(initial = 0L)
    val dismissFont by settingsDataStore.dismissFontStyleFlow.collectAsState(
        initial = RingingClockFont.SYSTEM_DEFAULT
    )
    val dismissCustomFontPath by settingsDataStore.dismissCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val dismissContainerColor by settingsDataStore.dismissContainerColorFlow.collectAsState(
        initial = 0L
    )
    val dismissTextColor by settingsDataStore.dismissTextColorFlow.collectAsState(initial = 0L)
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    val snoozeMinutes by settingsDataStore.ringingSnoozeMinutesFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_SNOOZE_MINUTES
    )
    val is24Hour = timeFormat.is24Hour

    // ---- Local per-alarm override state ----
    var backgroundUri by remember { mutableStateOf(initial.backgroundUri) }
    var backgroundStyle by remember { mutableStateOf(initial.backgroundStyle) }
    var videoUri by remember { mutableStateOf(initial.videoUri) }
    var backgroundSolidColor by remember { mutableLongStateOf(initial.backgroundSolidColor) }
    var clockColor by remember { mutableLongStateOf(initial.clockColor) }
    var offsetX by remember { mutableFloatStateOf(initial.offsetX) }
    var offsetY by remember { mutableFloatStateOf(initial.offsetY) }
    var boxWidth by remember { mutableFloatStateOf(initial.boxWidth) }
    var boxHeight by remember { mutableFloatStateOf(initial.boxHeight) }
    var clockBoxEnabled by remember { mutableStateOf(initial.clockBoxEnabled) }
    var clockBoxColor by remember { mutableLongStateOf(initial.clockBoxColor) }
    var clockBoxOpacity by remember { mutableFloatStateOf(initial.clockBoxOpacity) }
    var clockStyle by remember {
        mutableStateOf(
            initial.clockStyle
                .takeIf { it.isNotBlank() }
                ?.let { raw -> runCatching { RingingClockStyle.valueOf(raw) }.getOrNull() }
                ?: globalClockStyle
        )
    }
    var clockFont by remember { mutableStateOf(initial.clockFont) }
    var clockFontPath by remember { mutableStateOf(initial.clockFontPath) }
    var dateFont by remember { mutableStateOf(initial.dateFont) }
    var dateFontPath by remember { mutableStateOf(initial.dateFontPath) }
    var dateVisible by remember { mutableStateOf(initial.dateVisible) }
    var dateOffsetX by remember { mutableFloatStateOf(initial.dateOffsetX) }
    var dateOffsetY by remember { mutableFloatStateOf(initial.dateOffsetY) }
    var dateScale by remember { mutableFloatStateOf(initial.dateScale) }
    var blur by remember { mutableFloatStateOf(initial.blur) }
    var dim by remember { mutableFloatStateOf(initial.dim) }
    var clockColorEnd by remember { mutableLongStateOf(initial.clockColorEnd) }
    var clockGradient by remember { mutableStateOf(initial.clockGradient) }
    var clockGradientDir by remember { mutableStateOf(initial.clockGradientDir) }
    var dateColor by remember { mutableLongStateOf(initial.dateColor) }
    var dateColorEnd by remember { mutableLongStateOf(initial.dateColorEnd) }
    var dateGradient by remember { mutableStateOf(initial.dateGradient) }
    var dateGradientDir by remember { mutableStateOf(initial.dateGradientDir) }

    // Per-alarm overrides for AM/PM, label and the snooze/dismiss buttons (null/""/0 = inherit).
    var ovShowAmPm by remember { mutableStateOf(initial.showAmPm) }
    var ovAmPmFont by remember { mutableStateOf(initial.amPmFont) }
    var ovAmPmFontPath by remember { mutableStateOf(initial.amPmFontPath) }
    var ovAmPmColor by remember { mutableLongStateOf(initial.amPmColor) }
    var ovLabelVisible by remember { mutableStateOf(initial.alarmLabelVisible) }
    var ovLabelFont by remember { mutableStateOf(initial.alarmLabelFont) }
    var ovLabelFontPath by remember { mutableStateOf(initial.alarmLabelFontPath) }
    var ovLabelColor by remember { mutableLongStateOf(initial.alarmLabelColor) }
    var ovSnoozeFont by remember { mutableStateOf(initial.snoozeFont) }
    var ovSnoozeFontPath by remember { mutableStateOf(initial.snoozeFontPath) }
    var ovSnoozeContainerColor by remember { mutableLongStateOf(initial.snoozeContainerColor) }
    var ovSnoozeTextColor by remember { mutableLongStateOf(initial.snoozeTextColor) }
    var ovDismissFont by remember { mutableStateOf(initial.dismissFont) }
    var ovDismissFontPath by remember { mutableStateOf(initial.dismissFontPath) }
    var ovDismissContainerColor by remember { mutableLongStateOf(initial.dismissContainerColor) }
    var ovDismissTextColor by remember { mutableLongStateOf(initial.dismissTextColor) }

    // ---- Measured sizes / drag state ----
    var isDragging by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf(false) }
    var isDraggingDate by remember { mutableStateOf(false) }
    var dateSnapX by remember { mutableStateOf(false) }
    var dateSnapY by remember { mutableStateOf(false) }
    var clockW by remember { mutableFloatStateOf(0f) }
    var clockH by remember { mutableFloatStateOf(0f) }
    var dateH by remember { mutableFloatStateOf(0f) }
    var rootH by remember { mutableIntStateOf(0) }
    var resizeBaseW by remember { mutableFloatStateOf(0f) }
    var resizeBaseH by remember { mutableFloatStateOf(0f) }
    var resizeAccum by remember { mutableFloatStateOf(0f) }
    var dateResizeBase by remember { mutableFloatStateOf(1f) }
    var dateResizeAccum by remember { mutableFloatStateOf(0f) }

    var showBackgroundSheet by remember { mutableStateOf(false) }
    var showClockSheet by remember { mutableStateOf(false) }
    var showSnoozeSheet by remember { mutableStateOf(false) }
    var showDismissSheet by remember { mutableStateOf(false) }

    var nowCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!isDragging && !isDraggingDate) nowCalendar = Calendar.getInstance()
            delay(1_000L)
        }
    }

    // ---- Resolve effective (per-alarm â†’ global) appearance for the preview ----
    val effectiveBackgroundStyle = backgroundStyle
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingBackgroundStyle.valueOf(raw) }.getOrNull() }
        ?: if (backgroundUri.isNotBlank()) RingingBackgroundStyle.CUSTOM_IMAGE else globalBackground
    val effectiveImageUri = backgroundUri.takeIf { it.isNotBlank() } ?: globalBackgroundImageUri
    val effectiveVideoUri = videoUri.takeIf { it.isNotBlank() } ?: globalVideoUri
    val effSolidColorLong = backgroundSolidColor.takeIf { it != 0L } ?: globalSolidColor

    val effectiveClockFontEnum = clockFont
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: globalClockFont
    val effectiveClockFontPath = clockFontPath.takeIf { it.isNotBlank() } ?: globalClockFontPath
    val effectiveDateFontEnum = dateFont
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { ClockDateFont.valueOf(raw) }.getOrNull() }
        ?: globalDateFont
    val effectiveDateFontPath = dateFontPath.takeIf { it.isNotBlank() } ?: globalDateFontPath

    val fontFamily = remember(effectiveClockFontEnum, effectiveClockFontPath) {
        resolveRingingFontFamily(effectiveClockFontEnum, effectiveClockFontPath)
    }
    val dateFontFamily = remember(
        effectiveDateFontEnum,
        effectiveClockFontEnum,
        effectiveClockFontPath,
        effectiveDateFontPath
    ) {
        resolveDateFontFamily(
            effectiveDateFontEnum,
            effectiveClockFontEnum,
            effectiveClockFontPath,
            effectiveDateFontPath
        )
    }

    val hasBox = boxWidth > 0f && boxHeight > 0f
    val snapThresholdPx = with(density) { 16.dp.toPx() }
    val dateGapPx = with(density) { 14.dp.toPx() }
    val dateTopMarginPx = with(density) { 64.dp.toPx() }

    // ---- Effective clock / date appearance (per-alarm override â†’ global) ----
    val effClockColorLong = clockColor.takeIf { it != 0L } ?: globalClockColor
    val effClockBase = effClockColorLong.takeIf { it != 0L }?.let { Color(it) } ?: Color.White
    val effClockGradient = if (clockColor != 0L) clockGradient else globalClockGradient
    val effClockGradientDir = clockGradientDir
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { GradientDirection.valueOf(raw) }.getOrNull() }
        ?: globalClockGradientDir
    val effClockEndLong = clockColorEnd.takeIf { it != 0L } ?: globalClockColorEnd
    val effClockEnd = effClockEndLong.takeIf { it != 0L }?.let { Color(it) } ?: effClockBase
    val clockBrush = if (effClockGradient) {
        clockTextBrush(effClockBase, true, effClockEnd, effClockGradientDir)
    } else {
        null
    }

    val effDateColorLong = dateColor.takeIf { it != 0L } ?: globalDateColor
    val effDateBase = effDateColorLong.takeIf { it != 0L }?.let { Color(it) } ?: Color.White
    val effDateGradient = if (dateColor != 0L) dateGradient else globalDateGradient
    val effDateGradientDir = dateGradientDir
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { GradientDirection.valueOf(raw) }.getOrNull() }
        ?: globalDateGradientDir
    val effDateEndLong = dateColorEnd.takeIf { it != 0L } ?: globalDateColorEnd
    val effDateEnd = effDateEndLong.takeIf { it != 0L }?.let { Color(it) } ?: effDateBase
    val dateBrush = if (effDateGradient) {
        clockTextBrush(effDateBase, true, effDateEnd, effDateGradientDir)
    } else {
        null
    }

    val effBlur = if (blur >= 0f) blur else globalBlur
    val effDim = if (dim >= 0f) dim else globalDim

    val effShowAmPm = ovShowAmPm ?: showAmPm
    val effAmPmFontEnum = ovAmPmFont
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: amPmFont
    val effAmPmFontPath = ovAmPmFontPath.takeIf { it.isNotBlank() } ?: amPmCustomFontPath
    val effAmPmColorLong = ovAmPmColor.takeIf { it != 0L } ?: amPmColor
    val amPmFontFamily = resolveRingingFontFamily(effAmPmFontEnum, effAmPmFontPath)
    val amPmBrush = if (effAmPmColorLong != 0L) SolidColor(Color(effAmPmColorLong)) else null

    val effLabelVisible = ovLabelVisible ?: alarmLabelVisible
    val effLabelFontEnum = ovLabelFont
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: alarmLabelFont
    val effLabelFontPath = ovLabelFontPath.takeIf { it.isNotBlank() } ?: alarmLabelCustomFontPath
    val effLabelColorLong = ovLabelColor.takeIf { it != 0L } ?: alarmLabelColor
    val labelFontFamily = resolveRingingFontFamily(effLabelFontEnum, effLabelFontPath)
    val labelColor = if (effLabelColorLong != 0L) {
        Color(effLabelColorLong)
    } else {
        Color.White.copy(alpha = 0.75f)
    }

    val effSnoozeFontEnum = ovSnoozeFont
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: snoozeFont
    val effSnoozeFontPath = ovSnoozeFontPath.takeIf { it.isNotBlank() } ?: snoozeCustomFontPath
    val effSnoozeContainerColorLong =
        ovSnoozeContainerColor.takeIf { it != 0L } ?: snoozeContainerColor
    val effSnoozeTextColorLong = ovSnoozeTextColor.takeIf { it != 0L } ?: snoozeTextColor
    val snoozeTypeface = resolveRingingFontFamily(effSnoozeFontEnum, effSnoozeFontPath)

    val effDismissFontEnum = ovDismissFont
        .takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: dismissFont
    val effDismissFontPath = ovDismissFontPath.takeIf { it.isNotBlank() } ?: dismissCustomFontPath
    val effDismissContainerColorLong =
        ovDismissContainerColor.takeIf { it != 0L } ?: dismissContainerColor
    val effDismissTextColorLong = ovDismissTextColor.takeIf { it != 0L } ?: dismissTextColor
    val dismissTypeface = resolveRingingFontFamily(effDismissFontEnum, effDismissFontPath)

    val pickClockFont = rememberFontPicker(baseName = "pa_clock_$fontBaseSuffix") { path ->
        clockFontPath = path
        clockFont = RingingClockFont.CUSTOM_FILE.name
    }
    val pickDateFont = rememberFontPicker(baseName = "pa_date_$fontBaseSuffix") { path ->
        dateFontPath = path
        dateFont = ClockDateFont.CUSTOM_FILE.name
    }
    val pickAmPmFont = rememberFontPicker(baseName = "pa_am_pm_$fontBaseSuffix") { path ->
        ovAmPmFontPath = path
        ovAmPmFont = RingingClockFont.CUSTOM_FILE.name
    }
    val pickLabelFont = rememberFontPicker(baseName = "pa_label_$fontBaseSuffix") { path ->
        ovLabelFontPath = path
        ovLabelFont = RingingClockFont.CUSTOM_FILE.name
    }
    val pickSnoozeFont = rememberFontPicker(baseName = "pa_snooze_$fontBaseSuffix") { path ->
        ovSnoozeFontPath = path
        ovSnoozeFont = RingingClockFont.CUSTOM_FILE.name
    }
    val pickDismissFont = rememberFontPicker(baseName = "pa_dismiss_$fontBaseSuffix") { path ->
        ovDismissFontPath = path
        ovDismissFont = RingingClockFont.CUSTOM_FILE.name
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            backgroundUri = picked.toString()
            backgroundStyle = RingingBackgroundStyle.CUSTOM_IMAGE.name
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { picked ->
        if (picked != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            videoUri = picked.toString()
            backgroundStyle = RingingBackgroundStyle.VIDEO.name
        }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlarmLockBackground(
                style = effectiveBackgroundStyle,
                imageUri = effectiveImageUri,
                videoUri = effectiveVideoUri,
                blur = effBlur.dp,
                dim = effDim,
                solidColor = effSolidColorLong.takeIf { it != 0L }?.let { Color(it) },
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Draggable / resizable clock + freely draggable decoupled date.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { rootH = it.height },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (hasBox) {
                                        Modifier
                                            .requiredWidth(with(density) { boxWidth.toDp() })
                                            .requiredHeight(with(density) { boxHeight.toDp() })
                                    } else {
                                        Modifier
                                    }
                                )
                                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                .onSizeChanged {
                                    clockW = it.width.toFloat()
                                    clockH = it.height.toFloat()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val clockScale = 1f
                            val handleRadius = (22f * clockScale).dp
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (hasBox) Modifier.fillMaxSize()
                                        else Modifier.wrapContentSize(Alignment.Center)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AlarmClockBox(
                                    calendar = nowCalendar,
                                    style = clockStyle,
                                    fontFamily = fontFamily,
                                    is24Hour = is24Hour,
                                    showAmPm = effShowAmPm,
                                    amPmFontFamily = LocalAppFont.current,
                                    amPmBrush = null,
                                    contentColor = effClockBase,
                                    textBrush = clockBrush,
                                    removeLeadingZero = globalRemoveLeadingZero,
                                    fontSize = clockFontSize.sp,
                                    autoFit = hasBox,
                                    showContainerBorder = false,
                                    clockFont = effectiveClockFontEnum,
                                    customFontPath = effectiveClockFontPath,
                                    modifier = Modifier
                                        .then(
                                            if (hasBox) Modifier.fillMaxSize() else Modifier
                                        )
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = { showClockSheet = true })
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    isDragging = true
                                                    snapX = false
                                                    snapY = false
                                                },
                                                onDragEnd = {
                                                    if (snapX) offsetX = 0f
                                                    if (snapY) offsetY = 0f
                                                    isDragging = false
                                                    snapX = false
                                                    snapY = false
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    snapX = false
                                                    snapY = false
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                offsetX += dragAmount.x
                                                offsetY += dragAmount.y
                                                snapX = abs(offsetX) < snapThresholdPx
                                                snapY = abs(offsetY) < snapThresholdPx
                                            }
                                        }
                                )

                                // White arc grip sitting on the same bordered box's bottom-right
                                // corner so it reads as part of the clock outline.
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(handleRadius * 2f)
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    resizeBaseW = if (boxWidth > 0f) boxWidth else clockW
                                                    resizeBaseH = if (boxHeight > 0f) boxHeight else clockH
                                                    resizeAccum = 0f
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                resizeAccum += (dragAmount.x + dragAmount.y) / 2f
                                                val ref = with(density) { 160.dp.toPx() }
                                                val factor =
                                                    (1f + resizeAccum / ref).coerceAtLeast(0.05f)
                                                boxWidth = (resizeBaseW * factor).coerceAtLeast(1f)
                                                boxHeight = (resizeBaseH * factor).coerceAtLeast(1f)
                                            }
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = 4.dp.toPx()
                                        val radius = handleRadius.toPx()
                                        drawArc(
                                            color = Color.White,
                                            startAngle = 0f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            topLeft = Offset.Zero,
                                            size = Size(radius * 2f, radius * 2f),
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }

                        if (dateVisible) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset {
                                        IntOffset(
                                            dateOffsetX.roundToInt(),
                                            (clockH / 2f + dateGapPx + dateH / 2f + dateOffsetY)
                                                .roundToInt()
                                        )
                                    }
                                    .onSizeChanged { dateH = it.height.toFloat() }
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { showClockSheet = true })
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDraggingDate = true
                                                dateSnapX = false
                                                dateSnapY = false
                                            },
                                            onDragEnd = {
                                                if (dateSnapX) dateOffsetX = 0f
                                                if (dateSnapY) {
                                                    dateOffsetY =
                                                        -(clockH / 2f + dateGapPx + dateH / 2f)
                                                }
                                                isDraggingDate = false
                                                dateSnapX = false
                                                dateSnapY = false
                                            },
                                            onDragCancel = {
                                                isDraggingDate = false
                                                dateSnapX = false
                                                dateSnapY = false
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            dateOffsetX += dragAmount.x
                                            dateOffsetY += dragAmount.y
                                            dateSnapX = abs(dateOffsetX) < snapThresholdPx
                                            dateSnapY = abs(
                                                clockH / 2f + dateGapPx + dateH / 2f + dateOffsetY
                                            ) < snapThresholdPx
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                ClockDateText(
                                    calendar = nowCalendar,
                                    fontFamily = dateFontFamily,
                                    fontSize = (18f * dateScale).sp,
                                    contentColor = effDateBase,
                                    textBrush = dateBrush,
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 6.dp
                                    )
                                )
                                // Corner handle: uniform date scale (grow/shrink only).
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    dateResizeBase = dateScale
                                                    dateResizeAccum = 0f
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                dateResizeAccum +=
                                                    (dragAmount.x + dragAmount.y) / 2f
                                                val ref = with(density) { 160.dp.toPx() }
                                                dateScale = (dateResizeBase * (1f + dateResizeAccum / ref))
                                                    .coerceIn(
                                                        SettingsDataStore.DATE_SCALE_MIN,
                                                        SettingsDataStore.DATE_SCALE_MAX
                                                    )
                                            }
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = 3.dp.toPx()
                                        val radius = 12.dp.toPx()
                                        drawArc(
                                            color = Color.White,
                                            startAngle = 0f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            topLeft = Offset.Zero,
                                            size = Size(radius * 2f, radius * 2f),
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Axis guide lines while snapping.
                    if ((isDragging && (snapX || snapY)) ||
                        (isDraggingDate && (dateSnapX || dateSnapY))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val guideColor = Color.White.copy(alpha = 0.6f)
                            val stroke = 1.dp.toPx()
                            if (snapY || dateSnapY) {
                                drawLine(
                                    color = guideColor,
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = stroke
                                )
                            }
                            if (snapX || dateSnapX) {
                                drawLine(
                                    color = guideColor,
                                    start = Offset(size.width / 2f, 0f),
                                    end = Offset(size.width / 2f, size.height),
                                    strokeWidth = stroke
                                )
                            }
                        }
                    }

                    // Top action header: background chooser + Done.
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showBackgroundSheet = true },
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text("Background", color = Color.White)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                onSave(
                                    AlarmAppearance(
                                        backgroundUri = backgroundUri,
                                        backgroundStyle = backgroundStyle,
                                        videoUri = videoUri,
                                        backgroundSolidColor = backgroundSolidColor,
                                        clockColor = clockColor,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        boxWidth = boxWidth,
                                        boxHeight = boxHeight,
                                        clockBoxEnabled = clockBoxEnabled,
                                        clockBoxColor = clockBoxColor,
                                        clockBoxOpacity = clockBoxOpacity,
                                        clockStyle = clockStyle.name,
                                        clockFont = clockFont,
                                        clockFontPath = clockFontPath,
                                        dateFont = dateFont,
                                        dateFontPath = dateFontPath,
                                        dateVisible = dateVisible,
                                        dateOffsetX = dateOffsetX,
                                        dateOffsetY = dateOffsetY,
                                        dateScale = dateScale,
                                        removeLeadingZero = initial.removeLeadingZero,
                                        blur = blur,
                                        dim = dim,
                                        clockColorEnd = clockColorEnd,
                                        clockGradient = clockGradient,
                                        clockGradientDir = clockGradientDir,
                                        dateColor = dateColor,
                                        dateColorEnd = dateColorEnd,
                                        dateGradient = dateGradient,
                                        dateGradientDir = dateGradientDir,
                                        showAmPm = ovShowAmPm,
                                        amPmFont = ovAmPmFont,
                                        amPmFontPath = ovAmPmFontPath,
                                        amPmColor = ovAmPmColor,
                                        alarmLabelVisible = ovLabelVisible,
                                        alarmLabelFont = ovLabelFont,
                                        alarmLabelFontPath = ovLabelFontPath,
                                        alarmLabelColor = ovLabelColor,
                                        snoozeFont = ovSnoozeFont,
                                        snoozeFontPath = ovSnoozeFontPath,
                                        snoozeContainerColor = ovSnoozeContainerColor,
                                        snoozeTextColor = ovSnoozeTextColor,
                                        dismissFont = ovDismissFont,
                                        dismissFontPath = ovDismissFontPath,
                                        dismissContainerColor = ovDismissContainerColor,
                                        dismissTextColor = ovDismissTextColor
                                    )
                                )
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Done")
                        }
                    }

                    if (effLabelVisible) {
                        Text(
                            text = "Alarm",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LocalAppFont.current,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 4.sp
                            ),
                            color = effClockBase,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 64.dp)
                        )
                    }

                    // Bottom preview pills (non-destructive).
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PreviewDismissPill(
                            containerColor = if (effDismissContainerColorLong != 0L) {
                                Color(effDismissContainerColorLong)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            contentColor = if (effDismissTextColorLong != 0L) {
                                Color(effDismissTextColorLong)
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            },
                            fontFamily = dismissTypeface,
                            onClick = { showDismissSheet = true }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PreviewSnoozePill(
                            snoozeMinutes = snoozeMinutes,
                            containerColor = if (effSnoozeContainerColorLong != 0L) {
                                Color(effSnoozeContainerColorLong)
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (effSnoozeTextColorLong != 0L) {
                                Color(effSnoozeTextColorLong)
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            fontFamily = snoozeTypeface,
                            onSnooze = { showSnoozeSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showBackgroundSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBackgroundSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Background Style", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = backgroundStyle.isBlank(),
                        colors = selectedChipColors(),
                        onClick = {
                            backgroundStyle = ""
                            backgroundUri = ""
                            videoUri = ""
                        },
                        label = { Text("Inherit (global)") }
                    )
                    RingingBackgroundStyle.entries.forEach { option ->
                        FilterChip(
                            selected = backgroundStyle == option.name,
                            colors = selectedChipColors(),
                            onClick = {
                                backgroundStyle = option.name
                                if (option != RingingBackgroundStyle.CUSTOM_IMAGE) backgroundUri = ""
                                if (option != RingingBackgroundStyle.VIDEO) videoUri = ""
                                if (option != RingingBackgroundStyle.CUSTOM_IMAGE &&
                                    option != RingingBackgroundStyle.VIDEO
                                ) {
                                    showBackgroundSheet = false
                                }
                            },
                            label = { Text(option.label) }
                        )
                    }
                }
                if (backgroundStyle == RingingBackgroundStyle.CUSTOM_IMAGE.name ||
                    (backgroundStyle.isBlank() &&
                        globalBackground == RingingBackgroundStyle.CUSTOM_IMAGE)
                ) {
                    Button(
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose imageâ€¦")
                    }
                }
                if (backgroundStyle == RingingBackgroundStyle.VIDEO.name ||
                    (backgroundStyle.isBlank() && globalBackground == RingingBackgroundStyle.VIDEO)
                ) {
                    Text(
                        text = "Looping, muted video wallpaper (MP4/MKV).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { videoPicker.launch(arrayOf("video/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Choose videoâ€¦")
                    }
                    if (effectiveVideoUri.isNotBlank()) {
                        Text(
                            text = "Video selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (effectiveBackgroundStyle == RingingBackgroundStyle.SOLID_COLOR) {
                    Text(
                        text = "Solid Color",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ColorSwatchRow(
                        selected = backgroundSolidColor,
                        onSelect = { backgroundSolidColor = it },
                        inheritEnabled = true,
                        inherited = backgroundSolidColor == 0L,
                        onInherit = { backgroundSolidColor = 0L }
                    )
                }
                Text("Background Blur & Opacity", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Drag to set a per-alarm blur and dim; leave untouched to inherit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Blur: ${"%.1f".format(if (blur >= 0f) blur else globalBlur)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = if (blur >= 0f) blur else globalBlur,
                    onValueChange = { blur = it },
                    valueRange = SettingsDataStore.BLUR_MIN..SettingsDataStore.BLUR_MAX
                )
                Text(
                    text = "Dim: ${((if (dim >= 0f) dim else globalDim) * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = if (dim >= 0f) dim else globalDim,
                    onValueChange = { dim = it },
                    valueRange = 0f..1f
                )
                OutlinedButton(
                    onClick = { blur = -1f; dim = -1f },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Inherit blur & dim")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showClockSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClockSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Clock Style", style = MaterialTheme.typography.titleLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RingingClockStyle.entries.forEach { option ->
                        FilterChip(
                            selected = clockStyle == option,
                            colors = selectedChipColors(),
                            onClick = { clockStyle = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Text("Alarm Clock Font", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Applies only to this alarm's ringing clock. \"Inherit\" uses the global clock font.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = clockFont.isBlank(),
                        colors = selectedChipColors(),
                        onClick = { clockFont = ""; clockFontPath = "" },
                        label = { Text("Inherit") }
                    )
                    RingingClockFont.entries.forEach { font ->
                        FilterChip(
                            selected = clockFont == font.name,
                            colors = selectedChipColors(),
                            onClick = {
                                clockFont = font.name
                                if (font != RingingClockFont.CUSTOM_FILE) clockFontPath = ""
                            },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (clockFont == RingingClockFont.CUSTOM_FILE.name) {
                    OutlinedButton(
                        onClick = { pickClockFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".ttf / .otf font fileâ€¦")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(clockFontPath)) {
                            "Custom font loaded"
                        } else {
                            "No custom font selected yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text("Date", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show date",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = dateVisible, onCheckedChange = { dateVisible = it })
                }
                Text("Date Font", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = dateFont.isBlank(),
                        colors = selectedChipColors(),
                        onClick = { dateFont = ""; dateFontPath = "" },
                        label = { Text("Inherit") }
                    )
                    ClockDateFont.entries.forEach { font ->
                        FilterChip(
                            selected = dateFont == font.name,
                            colors = selectedChipColors(),
                            onClick = {
                                dateFont = font.name
                                if (font != ClockDateFont.CUSTOM_FILE) dateFontPath = ""
                            },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (dateFont == ClockDateFont.CUSTOM_FILE.name) {
                    OutlinedButton(
                        onClick = { pickDateFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Date font file (.ttf / .otf / .apk)â€¦")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(dateFontPath)) {
                            "Custom date font loaded"
                        } else {
                            "No custom date font selected yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AppearanceColorEditor(
                    title = "Date Color",
                    color = dateColor,
                    gradient = dateGradient,
                    colorEnd = dateColorEnd,
                    direction = effDateGradientDir,
                    onColor = { dateColor = it },
                    onGradient = { dateGradient = it },
                    onColorEnd = { dateColorEnd = it },
                    onDirection = { dateGradientDir = it.name },
                    inheritEnabled = true,
                    inherited = dateColor == 0L,
                    onInherit = { dateColor = 0L }
                )

                AppearanceColorEditor(
                    title = "Clock Color",
                    color = clockColor,
                    gradient = clockGradient,
                    colorEnd = clockColorEnd,
                    direction = effClockGradientDir,
                    onColor = { clockColor = it },
                    onGradient = { clockGradient = it },
                    onColorEnd = { clockColorEnd = it },
                    onDirection = { clockGradientDir = it.name },
                    inheritEnabled = true,
                    inherited = clockColor == 0L,
                    onInherit = { clockColor = 0L }
                )

                Text("AM/PM", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show AM/PM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = effShowAmPm, onCheckedChange = { ovShowAmPm = it })
                }
                if (ovShowAmPm != null) {
                    OutlinedButton(
                        onClick = { ovShowAmPm = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Inherit (global)")
                    }
                }
                Text("Clock Size", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Drag the bottom-right arc handle on the preview to resize. The clock " +
                        "and digits scale uniformly (aspect-locked) and the font auto-fits the box.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        boxWidth = 0f
                        boxHeight = 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset size")
                }

                Button(
                    onClick = { showClockSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showSnoozeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnoozeSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Snooze Button", style = MaterialTheme.typography.titleLarge)
                Text("Font", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ovSnoozeFont.isBlank(),
                        colors = selectedChipColors(),
                        onClick = {
                            ovSnoozeFont = ""
                            ovSnoozeFontPath = ""
                        },
                        label = { Text("Inherit") }
                    )
                    RingingClockFont.entries.forEach { font ->
                        FilterChip(
                            selected = ovSnoozeFont == font.name,
                            colors = selectedChipColors(),
                            onClick = {
                                ovSnoozeFont = font.name
                                if (font != RingingClockFont.CUSTOM_FILE) ovSnoozeFontPath = ""
                            },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (ovSnoozeFont == RingingClockFont.CUSTOM_FILE.name) {
                    OutlinedButton(
                        onClick = { pickSnoozeFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".ttf / .otf font fileâ€¦")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(ovSnoozeFontPath)) {
                            "Custom snooze font loaded"
                        } else {
                            "No custom font selected yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Container Color", style = MaterialTheme.typography.titleMedium)
                ColorSwatchRow(
                    selected = ovSnoozeContainerColor,
                    inheritEnabled = true,
                    inherited = ovSnoozeContainerColor == 0L,
                    onInherit = { ovSnoozeContainerColor = 0L },
                    onSelect = { ovSnoozeContainerColor = it }
                )
                Text("Text Color", style = MaterialTheme.typography.titleMedium)
                ColorSwatchRow(
                    selected = ovSnoozeTextColor,
                    inheritEnabled = true,
                    inherited = ovSnoozeTextColor == 0L,
                    onInherit = { ovSnoozeTextColor = 0L },
                    onSelect = { ovSnoozeTextColor = it }
                )
                Button(
                    onClick = { showSnoozeSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showDismissSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDismissSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Dismiss Button", style = MaterialTheme.typography.titleLarge)
                Text("Font", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = ovDismissFont.isBlank(),
                        colors = selectedChipColors(),
                        onClick = {
                            ovDismissFont = ""
                            ovDismissFontPath = ""
                        },
                        label = { Text("Inherit") }
                    )
                    RingingClockFont.entries.forEach { font ->
                        FilterChip(
                            selected = ovDismissFont == font.name,
                            colors = selectedChipColors(),
                            onClick = {
                                ovDismissFont = font.name
                                if (font != RingingClockFont.CUSTOM_FILE) ovDismissFontPath = ""
                            },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (ovDismissFont == RingingClockFont.CUSTOM_FILE.name) {
                    OutlinedButton(
                        onClick = { pickDismissFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".ttf / .otf font fileâ€¦")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(ovDismissFontPath)) {
                            "Custom dismiss font loaded"
                        } else {
                            "No custom font selected yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Container Color", style = MaterialTheme.typography.titleMedium)
                ColorSwatchRow(
                    selected = ovDismissContainerColor,
                    inheritEnabled = true,
                    inherited = ovDismissContainerColor == 0L,
                    onInherit = { ovDismissContainerColor = 0L },
                    onSelect = { ovDismissContainerColor = it }
                )
                Text("Text Color", style = MaterialTheme.typography.titleMedium)
                ColorSwatchRow(
                    selected = ovDismissTextColor,
                    inheritEnabled = true,
                    inherited = ovDismissTextColor == 0L,
                    onInherit = { ovDismissTextColor = 0L },
                    onSelect = { ovDismissTextColor = it }
                )
                Button(onClick = { showDismissSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/** Preview-only dismiss pill (identical chrome to the customization screen). */
@Composable
private fun PreviewDismissPill(
    containerColor: Color = MaterialTheme.colorScheme.errorContainer,
    contentColor: Color = MaterialTheme.colorScheme.onErrorContainer,
    fontFamily: FontFamily? = null,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Dismiss",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/** Preview-only snooze pill (`[-] Snooze N mins [+]`). */
@Composable
private fun PreviewSnoozePill(
    snoozeMinutes: Int,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    fontFamily: FontFamily? = null,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        PreviewSnoozeStepper(
            icon = Icons.Filled.Remove,
            contentDescription = "Decrease snooze",
            tint = contentColor
        )
        Text(
            text = "Snooze $snoozeMinutes mins",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = contentColor,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSnooze)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
        PreviewSnoozeStepper(
            icon = Icons.Filled.Add,
            contentDescription = "Increase snooze",
            tint = contentColor
        )
    }
}

@Composable
private fun PreviewSnoozeStepper(
    icon: ImageVector,
    contentDescription: String,
    tint: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
