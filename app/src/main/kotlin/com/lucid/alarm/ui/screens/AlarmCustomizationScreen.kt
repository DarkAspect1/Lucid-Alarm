package com.lucid.alarm.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import com.lucid.alarm.data.prefs.ClockDateFont
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.data.prefs.RingingBackgroundStyle
import com.lucid.alarm.data.prefs.RingingClockFont
import com.lucid.alarm.data.prefs.RingingClockStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.LocalAppFont
import com.lucid.alarm.ui.components.AlarmClockBox
import com.lucid.alarm.ui.components.AlarmLockBackground
import com.lucid.alarm.ui.components.AlarmLockPalette
import com.lucid.alarm.ui.components.AppearanceColorEditor
import com.lucid.alarm.ui.components.ClockDateText
import com.lucid.alarm.ui.components.ColorSwatchRow
import com.lucid.alarm.ui.components.InteractiveColorPickerRow
import com.lucid.alarm.ui.components.RingingActionPill
import com.lucid.alarm.ui.components.clockTextBrush
import com.lucid.alarm.ui.components.holdToRepeat
import com.lucid.alarm.ui.components.resolveDateFontFamily
import com.lucid.alarm.ui.components.resolveRingingFontFamily
import com.lucid.alarm.ui.components.SelectionChipShape
import com.lucid.alarm.ui.components.selectedChipColors
import com.lucid.alarm.ui.rememberFontPicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmCustomizationScreen(
    settingsDataStore: SettingsDataStore,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BackHandler(enabled = true) {
        onDone()
    }

    var isInitialized by remember { mutableStateOf(false) }

    var background by remember { mutableStateOf(RingingBackgroundStyle.STARFIELD) }
    var backgroundImageUri by remember { mutableStateOf("") }
    var backgroundVideoUri by remember { mutableStateOf("") }
    var clockStyle by remember { mutableStateOf(RingingClockStyle.HORIZONTAL_DIGITAL) }
    var clockFont by remember { mutableStateOf(RingingClockFont.SYSTEM_DEFAULT) }
    var clockCustomFontPath by remember { mutableStateOf("") }
    var clockFontSize by remember { mutableFloatStateOf(SettingsDataStore.DEFAULT_CLOCK_FONT_SIZE) }
    var removeLeadingZero by remember { mutableStateOf(true) }
    var ringingBlur by remember { mutableFloatStateOf(0f) }
    var ringingDim by remember { mutableFloatStateOf(SettingsDataStore.DEFAULT_DIM) }
    var clockColor by remember { mutableLongStateOf(0L) }
    var clockGradient by remember { mutableStateOf(false) }
    var clockColorEnd by remember { mutableLongStateOf(0L) }
    var clockGradientDir by remember { mutableStateOf(GradientDirection.VERTICAL) }
    var dateColor by remember { mutableLongStateOf(0L) }
    var dateGradient by remember { mutableStateOf(false) }
    var dateColorEnd by remember { mutableLongStateOf(0L) }
    var dateGradientDir by remember { mutableStateOf(GradientDirection.VERTICAL) }
    var showAmPm by remember { mutableStateOf(true) }
    var alarmLabelVisible by remember { mutableStateOf(true) }
    var solidColorRaw by remember { mutableLongStateOf(0L) }
    var interactiveColorsState by remember { mutableStateOf<List<Color>>(emptyList()) }
    var amPmFont by remember { mutableStateOf(RingingClockFont.SYSTEM_DEFAULT) }
    var amPmCustomFontPath by remember { mutableStateOf("") }
    var amPmColor by remember { mutableLongStateOf(0L) }
    var alarmLabelFont by remember { mutableStateOf(RingingClockFont.SYSTEM_DEFAULT) }
    var alarmLabelCustomFontPath by remember { mutableStateOf("") }
    var alarmLabelColor by remember { mutableLongStateOf(0L) }
    var snoozeFont by remember { mutableStateOf(RingingClockFont.SYSTEM_DEFAULT) }
    var snoozeCustomFontPath by remember { mutableStateOf("") }
    var snoozeContainerColor by remember { mutableLongStateOf(0L) }
    var snoozeTextColor by remember { mutableLongStateOf(0L) }
    var dismissFont by remember { mutableStateOf(RingingClockFont.SYSTEM_DEFAULT) }
    var dismissCustomFontPath by remember { mutableStateOf("") }
    var dismissContainerColor by remember { mutableLongStateOf(0L) }
    var dismissTextColor by remember { mutableLongStateOf(0L) }
    var dateVisible by remember { mutableStateOf(true) }
    var dateFont by remember { mutableStateOf(ClockDateFont.MATCH_CLOCK) }
    var dateCustomFontPath by remember { mutableStateOf("") }
    var dateScaleLocal by remember { mutableFloatStateOf(1f) }
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    var snoozeMinutes by remember { mutableIntStateOf(SettingsDataStore.DEFAULT_SNOOZE_MINUTES) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf(false) }

    var dateOffsetX by remember { mutableFloatStateOf(0f) }
    var dateOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingDate by remember { mutableStateOf(false) }
    var dateSnapX by remember { mutableStateOf(false) }
    var dateSnapY by remember { mutableStateOf(false) }

    var clockScale by remember { mutableFloatStateOf(1f) }
    var clockResizeBase by remember { mutableFloatStateOf(1f) }
    var clockResizeAccum by remember { mutableFloatStateOf(0f) }
    var boxWidth by remember { mutableFloatStateOf(0f) }
    var boxHeight by remember { mutableFloatStateOf(0f) }
    var clockBoxEnabled by remember { mutableStateOf(false) }
    var clockBoxColor by remember { mutableLongStateOf(0L) }
    var clockBoxOpacity by remember { mutableFloatStateOf(0.35f) }
    var isResizing by remember { mutableStateOf(false) }

    var clockW by remember { mutableFloatStateOf(0f) }
    var clockH by remember { mutableFloatStateOf(0f) }
    var dateW by remember { mutableFloatStateOf(0f) }
    var dateH by remember { mutableFloatStateOf(0f) }
    var rootH by remember { mutableIntStateOf(0) }
    var rootW by remember { mutableIntStateOf(0) }

    var isResizingDate by remember { mutableStateOf(false) }
    var dateResizeBase by remember { mutableFloatStateOf(1f) }
    var dateResizeAccum by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (!isInitialized) {
            background = settingsDataStore.ringingBackgroundFlow.first()
            backgroundImageUri = settingsDataStore.ringingBackgroundImageUriFlow.first()
            backgroundVideoUri = settingsDataStore.ringingVideoUriFlow.first()
            clockStyle = settingsDataStore.ringingClockStyleFlow.first()
            clockFont = settingsDataStore.alarmClockFontStyleFlow.first()
            clockCustomFontPath = settingsDataStore.alarmClockCustomFontPathFlow.first()
            clockFontSize = settingsDataStore.clockFontSizeFlow.first()
            removeLeadingZero = settingsDataStore.removeLeadingZeroFlow.first()
            ringingBlur = settingsDataStore.ringingBlurFlow.first()
            ringingDim = settingsDataStore.ringingDimFlow.first()
            clockColor = settingsDataStore.clockColorFlow.first()
            clockGradient = settingsDataStore.clockGradientFlow.first()
            clockColorEnd = settingsDataStore.clockColorEndFlow.first()
            clockGradientDir = settingsDataStore.clockGradientDirFlow.first()
            dateColor = settingsDataStore.dateColorFlow.first()
            dateGradient = settingsDataStore.dateGradientFlow.first()
            dateColorEnd = settingsDataStore.dateColorEndFlow.first()
            dateGradientDir = settingsDataStore.dateGradientDirFlow.first()
            showAmPm = settingsDataStore.showAmPmFlow.first()
            alarmLabelVisible = settingsDataStore.alarmLabelVisibleFlow.first()
            solidColorRaw = settingsDataStore.ringingSolidColorFlow.first()
            interactiveColorsState = settingsDataStore.ringingInteractiveColorsFlow.first().map { Color(it.toInt()) }
            amPmFont = settingsDataStore.amPmFontStyleFlow.first()
            amPmCustomFontPath = settingsDataStore.amPmCustomFontPathFlow.first()
            amPmColor = settingsDataStore.amPmColorFlow.first()
            alarmLabelFont = settingsDataStore.alarmLabelFontStyleFlow.first()
            alarmLabelCustomFontPath = settingsDataStore.alarmLabelCustomFontPathFlow.first()
            alarmLabelColor = settingsDataStore.alarmLabelColorFlow.first()
            snoozeFont = settingsDataStore.snoozeFontStyleFlow.first()
            snoozeCustomFontPath = settingsDataStore.snoozeCustomFontPathFlow.first()
            snoozeContainerColor = settingsDataStore.snoozeContainerColorFlow.first()
            snoozeTextColor = settingsDataStore.snoozeTextColorFlow.first()
            dismissFont = settingsDataStore.dismissFontStyleFlow.first()
            dismissCustomFontPath = settingsDataStore.dismissCustomFontPathFlow.first()
            dismissContainerColor = settingsDataStore.dismissContainerColorFlow.first()
            dismissTextColor = settingsDataStore.dismissTextColorFlow.first()
            dateVisible = settingsDataStore.clockDateVisibleFlow.first()
            dateFont = settingsDataStore.clockDateFontFlow.first()
            dateCustomFontPath = settingsDataStore.clockDateCustomFontPathFlow.first()
            dateScaleLocal = settingsDataStore.clockDateScaleFlow.first()
            snoozeMinutes = settingsDataStore.ringingSnoozeMinutesFlow.first()

            val savedOx = settingsDataStore.freeDragOffsetXFlow.first()
            val savedOy = settingsDataStore.freeDragOffsetYFlow.first()
            if (!savedOx.isNaN()) offsetX = savedOx
            if (!savedOy.isNaN()) offsetY = savedOy

            val savedDox = settingsDataStore.clockDateOffsetXFlow.first()
            val savedDoy = settingsDataStore.clockDateOffsetYFlow.first()
            if (!savedDox.isNaN()) dateOffsetX = savedDox
            if (!savedDoy.isNaN()) dateOffsetY = savedDoy

            boxWidth = settingsDataStore.clockBoxWidthFlow.first()
            boxHeight = settingsDataStore.clockBoxHeightFlow.first()
            clockScale = settingsDataStore.clockContainerScaleFlow.first()
            clockBoxEnabled = settingsDataStore.clockBoxEnabledFlow.first()
            clockBoxColor = settingsDataStore.clockBoxColorFlow.first()
            clockBoxOpacity = settingsDataStore.clockBoxOpacityFlow.first()

            isInitialized = true
        }
    }

    LaunchedEffect(rootW, rootH, dateW) {
        if (!isDraggingDate && rootW > 0 && dateW > 0 &&
            (abs(dateOffsetX) > rootW / 4f || dateOffsetY > rootH / 3f)
        ) {
            dateOffsetX = 0f
            dateOffsetY = 0f
        }
    }

    var showBackgroundSheet by remember { mutableStateOf(false) }
    var showClockSheet by remember { mutableStateOf(false) }
    var showDismissSheet by remember { mutableStateOf(false) }
    var showSnoozeSheet by remember { mutableStateOf(false) }

    var nowCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            if (!isDragging && !isDraggingDate) nowCalendar = Calendar.getInstance()
            delay(1_000L)
        }
    }

    val fontFamily = remember(clockFont, clockCustomFontPath) {
        resolveRingingFontFamily(clockFont, clockCustomFontPath)
    }
    val dateFontFamily = remember(dateFont, clockFont, clockCustomFontPath, dateCustomFontPath) {
        resolveDateFontFamily(dateFont, clockFont, clockCustomFontPath, dateCustomFontPath)
    }
    val is24Hour = timeFormat.is24Hour
    val hasBox = boxWidth > 0f && boxHeight > 0f

    val pickClockFont = rememberFontPicker(
        baseName = FontManager.CLOCK_FONT_BASE
    ) { path ->
        clockCustomFontPath = path
        clockFont = RingingClockFont.CUSTOM_FILE
    }

    val pickDateFont = rememberFontPicker(
        baseName = FontManager.DATE_FONT_BASE
    ) { path ->
        dateCustomFontPath = path
        dateFont = ClockDateFont.CUSTOM_FILE
    }

    val pickAmPmFont = rememberFontPicker(
        baseName = FontManager.AMPM_FONT_BASE
    ) { path ->
        amPmCustomFontPath = path
        amPmFont = RingingClockFont.CUSTOM_FILE
    }

    val pickLabelFont = rememberFontPicker(
        baseName = FontManager.LABEL_FONT_BASE
    ) { path ->
        alarmLabelCustomFontPath = path
        alarmLabelFont = RingingClockFont.CUSTOM_FILE
    }

    val pickSnoozeFont = rememberFontPicker(
        baseName = FontManager.SNOOZE_FONT_BASE
    ) { path ->
        snoozeCustomFontPath = path
        snoozeFont = RingingClockFont.CUSTOM_FILE
    }

    val pickDismissFont = rememberFontPicker(
        baseName = FontManager.DISMISS_FONT_BASE
    ) { path ->
        dismissCustomFontPath = path
        dismissFont = RingingClockFont.CUSTOM_FILE
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            backgroundImageUri = uri.toString()
            background = RingingBackgroundStyle.CUSTOM_IMAGE
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            backgroundVideoUri = uri.toString()
            background = RingingBackgroundStyle.VIDEO
        }
    }

    val snapThresholdPx = with(density) { 16.dp.toPx() }
    val dateGapPx = with(density) { 14.dp.toPx() }

    var resizeBaseW by remember { mutableFloatStateOf(0f) }
    var resizeBaseH by remember { mutableFloatStateOf(0f) }
    var resizeAccum by remember { mutableFloatStateOf(0f) }
    val resizeStart: () -> Unit = {
        isResizing = true
        clockResizeBase = clockScale
        clockResizeAccum = 0f
    }
    val resizeDelta: (Float, Float) -> Unit = { dx, dy ->
        clockResizeAccum += (dx + dy) / 2f
        val ref = with(density) { 180.dp.toPx() }
        clockScale = (clockResizeBase * (1f + clockResizeAccum / ref))
            .coerceAtLeast(SettingsDataStore.CLOCK_SCALE_MIN)
    }
    val resizeEnd: () -> Unit = {
        isResizing = false
    }

    val dateResizeStart: () -> Unit = {
        isResizingDate = true
        dateResizeBase = dateScaleLocal
        dateResizeAccum = 0f
    }
    val dateResizeDelta: (Float, Float) -> Unit = { dx, dy ->
        dateResizeAccum += (dx + dy) / 2f
        val ref = with(density) { 160.dp.toPx() }
        dateScaleLocal = (dateResizeBase * (1f + dateResizeAccum / ref))
            .coerceAtLeast(SettingsDataStore.DATE_SCALE_MIN)
    }
    val dateResizeEnd: () -> Unit = {
        isResizingDate = false
    }

    val clockBaseColor = if (clockColor != 0L) Color(clockColor) else Color.White
    val clockEndColor = if (clockColorEnd != 0L) Color(clockColorEnd) else clockBaseColor
    val clockBrush = if (clockGradient) {
        clockTextBrush(clockBaseColor, true, clockEndColor, clockGradientDir)
    } else {
        null
    }
    val dateBaseColor = if (dateColor != 0L) Color(dateColor) else Color.White
    val dateEndColor = if (dateColorEnd != 0L) Color(dateColorEnd) else dateBaseColor
    val dateBrush = if (dateGradient) {
        clockTextBrush(dateBaseColor, true, dateEndColor, dateGradientDir)
    } else {
        null
    }

    val amPmFontFamily = resolveRingingFontFamily(amPmFont, amPmCustomFontPath)
    val amPmTextColor = if (amPmColor != 0L) Color(amPmColor) else null
    val amPmBrush = amPmTextColor?.let { androidx.compose.ui.graphics.SolidColor(it) }
    val labelFontFamily = resolveRingingFontFamily(alarmLabelFont, alarmLabelCustomFontPath)
    val labelColor = if (alarmLabelColor != 0L) Color(alarmLabelColor) else Color.White.copy(alpha = 0.75f)

    val snoozeTypeface = resolveRingingFontFamily(snoozeFont, snoozeCustomFontPath)
    val dismissTypeface = resolveRingingFontFamily(dismissFont, dismissCustomFontPath)
    val solidColor = if (solidColorRaw != 0L) Color(solidColorRaw) else Color(0xFF101014)
    val dateTopMarginPx = with(density) { 64.dp.toPx() }

    val isDarkBackground = when (background) {
        RingingBackgroundStyle.SOLID_COLOR -> solidColor.luminance() <= 0.5f
        else -> true
    }
    val view = LocalView.current
    DisposableEffect(view, isDarkBackground) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, window.decorView) }
        val prevLightStatus = insetsController?.isAppearanceLightStatusBars
        if (insetsController != null) {
            insetsController.isAppearanceLightStatusBars = !isDarkBackground
        }
        onDispose {
            if (insetsController != null && prevLightStatus != null) {
                insetsController.isAppearanceLightStatusBars = prevLightStatus
            }
        }
    }

    AlarmLockBackground(
        style = background,
        imageUri = backgroundImageUri,
        videoUri = backgroundVideoUri,
        blur = ringingBlur.dp,
        dim = ringingDim,
        solidColor = solidColor,
        interactiveColors = interactiveColorsState,
        dark = isSystemInDarkTheme(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { rootW = it.width; rootH = it.height },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .wrapContentSize(Alignment.Center, unbounded = true)
                        .onSizeChanged {
                            clockW = it.width.toFloat()
                            clockH = it.height.toFloat()
                            boxWidth = clockW
                            boxHeight = clockH
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val handleRadius = (22f * clockScale).dp
                    AlarmClockBox(
                        calendar = nowCalendar,
                        style = clockStyle,
                        fontFamily = fontFamily,
                        is24Hour = is24Hour,
                        showAmPm = showAmPm,
                        contentColor = clockBaseColor,
                        textBrush = clockBrush,
                        amPmFontFamily = LocalAppFont.current,
                        amPmBrush = null,
                        removeLeadingZero = removeLeadingZero,
                        fontSize = (clockFontSize * clockScale).sp,
                        autoFit = false,
                        showContainerBorder = false,
                        clockScale = clockScale,
                        clockFont = clockFont,
                        customFontPath = clockCustomFontPath,
                        modifier = Modifier
                            .wrapContentSize(Alignment.Center, unbounded = true)
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

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(handleRadius * 2f)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { resizeStart() },
                                    onDragEnd = { resizeEnd() },
                                    onDragCancel = { resizeEnd() }
                                ) { change, dragAmount ->
                                    change.consume()
                                    resizeDelta(dragAmount.x, dragAmount.y)
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

                if (dateVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center, unbounded = true)
                    ) {
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
                            .wrapContentSize(Alignment.Center, unbounded = true)
                            .onSizeChanged {
                                dateW = it.width.toFloat()
                                dateH = it.height.toFloat()
                            }
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
                                            dateOffsetY = -(clockH / 2f + dateGapPx + dateH / 2f)
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
                            fontSize = (18f * dateScaleLocal).sp,
                            contentColor = dateBaseColor,
                            textBrush = dateBrush,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { dateResizeStart() },
                                        onDragEnd = { dateResizeEnd() },
                                        onDragCancel = { dateResizeEnd() }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dateResizeDelta(dragAmount.x, dragAmount.y)
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
            }

            if ((isDragging && (snapX || snapY)) || (isDraggingDate && (dateSnapX || dateSnapY))) {
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

            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val statusShadowColor = when (background) {
                RingingBackgroundStyle.SOLID_COLOR -> solidColor
                else -> Color.Black
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarTop + 32.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(4f)
                    .pointerInput(Unit) {}
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                statusShadowColor.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(5f)
                    .statusBarsPadding()
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
                        scope.launch {
                            settingsDataStore.saveAlarmCustomization(
                                background = background,
                                backgroundImageUri = backgroundImageUri,
                                backgroundVideoUri = backgroundVideoUri,
                                solidColor = solidColorRaw,
                                interactiveColors = interactiveColorsState.map { it.toArgb().toLong() },
                                blur = ringingBlur,
                                dim = ringingDim,
                                clockStyle = clockStyle,
                                clockColor = clockColor,
                                clockGradient = clockGradient,
                                clockColorEnd = clockColorEnd,
                                clockGradientDir = clockGradientDir,
                                showAmPm = showAmPm,
                                amPmFont = amPmFont,
                                amPmCustomFontPath = amPmCustomFontPath,
                                amPmColor = amPmColor,
                                alarmLabelVisible = alarmLabelVisible,
                                alarmLabelFont = alarmLabelFont,
                                alarmLabelCustomFontPath = alarmLabelCustomFontPath,
                                alarmLabelColor = alarmLabelColor,
                                clockFont = clockFont,
                                clockCustomFontPath = clockCustomFontPath,
                                dateVisible = dateVisible,
                                dateFont = dateFont,
                                dateCustomFontPath = dateCustomFontPath,
                                dateColor = dateColor,
                                dateGradient = dateGradient,
                                dateColorEnd = dateColorEnd,
                                dateGradientDir = dateGradientDir,
                                dateScale = dateScaleLocal,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                dateOffsetX = dateOffsetX,
                                dateOffsetY = dateOffsetY,
                                boxWidth = boxWidth,
                                boxHeight = boxHeight,
                                clockScale = clockScale,
                                clockBoxEnabled = clockBoxEnabled,
                                clockBoxColor = clockBoxColor,
                                clockBoxOpacity = clockBoxOpacity,
                                snoozeMinutes = snoozeMinutes,
                                dismissFont = dismissFont,
                                dismissCustomFontPath = dismissCustomFontPath,
                                dismissContainerColor = dismissContainerColor,
                                dismissTextColor = dismissTextColor,
                                snoozeFont = snoozeFont,
                                snoozeCustomFontPath = snoozeCustomFontPath,
                                snoozeContainerColor = snoozeContainerColor,
                                snoozeTextColor = snoozeTextColor
                            )
                            onDone()
                        }
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

            if (alarmLabelVisible) {
                Text(
                    text = "Alarm",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LocalAppFont.current,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 4.sp
                    ),
                    color = clockBaseColor,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RingingActionPill(
                    text = "Dismiss",
                    onClick = { showDismissSheet = true },
                    containerColor = if (dismissContainerColor != 0L) {
                        Color(dismissContainerColor)
                    } else {
                        AlarmLockPalette.DismissButton
                    },
                    contentColor = if (dismissTextColor != 0L) {
                        Color(dismissTextColor)
                    } else {
                        Color.White
                    },
                    fontFamily = dismissTypeface
                )
                Spacer(modifier = Modifier.height(16.dp))
                CustomizationSnoozePill(
                    snoozeMinutes = snoozeMinutes,
                    onSnooze = { showSnoozeSheet = true },
                    onMinus = {
                        if (snoozeMinutes > 1) snoozeMinutes--
                    },
                    onPlus = {
                        if (snoozeMinutes < 30) snoozeMinutes++
                    },
                    containerColor = if (snoozeContainerColor != 0L) {
                        Color(snoozeContainerColor)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (snoozeTextColor != 0L) {
                        Color(snoozeTextColor)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontFamily = snoozeTypeface
                )
            }
        }
    }

    if (showBackgroundSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBackgroundSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {

            val consumeVerticalFlingConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset = Offset(0f, available.y)

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity = Velocity(0f, available.y)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(consumeVerticalFlingConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    Text("Background Style", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RingingBackgroundStyle.entries.forEach { style ->
                            FilterChip(
                                selected = background == style,
                                colors = selectedChipColors(),
                                shape = SelectionChipShape,
                                onClick = {
                                    background = style
                                    if (style != RingingBackgroundStyle.CUSTOM_IMAGE &&
                                        style != RingingBackgroundStyle.VIDEO &&
                                        style != RingingBackgroundStyle.SOLID_COLOR
                                    ) {
                                        showBackgroundSheet = false
                                    }
                                },
                                label = { Text(style.label) }
                            )
                        }
                    }
                    if (background == RingingBackgroundStyle.SOLID_COLOR) {
                        Text(
                            text = "Solid Color",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ColorSwatchRow(
                            selected = solidColorRaw,
                            onSelect = { solidColorRaw = it }
                        )
                    }
                    if (background == RingingBackgroundStyle.INTERACTIVE_GRADIENT ||
                        background == RingingBackgroundStyle.AURORA ||
                        background == RingingBackgroundStyle.FIREFLIES
                    ) {
                        Text(
                            text = "Interactive Colors",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        InteractiveColorPickerRow(
                            colors = interactiveColorsState,
                            onColorsChange = { updated ->
                                interactiveColorsState = updated
                            }
                        )
                    }
                    Text("Background Blur & Opacity", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Blur: ${"%.1f".format(ringingBlur)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = ringingBlur,
                        onValueChange = { ringingBlur = it },
                        valueRange = SettingsDataStore.BLUR_MIN..SettingsDataStore.BLUR_MAX
                    )
                    Text(
                        text = "Dim: ${(ringingDim * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = ringingDim,
                        onValueChange = { ringingDim = it },
                        valueRange = 0f..1f
                    )
                    if (background == RingingBackgroundStyle.CUSTOM_IMAGE) {
                        Button(
                            onClick = { imagePicker.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose image\u2026")
                        }
                    }
                    if (background == RingingBackgroundStyle.VIDEO) {
                        Text(
                            text = "Looping, muted video wallpaper (MP4/MKV).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { videoPicker.launch(arrayOf("video/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose video\u2026")
                        }
                        if (backgroundVideoUri.isNotBlank()) {
                            Text(
                                text = "Video selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                Text("Alarm Clock", style = MaterialTheme.typography.titleLarge)

                Text("Orientation & Style", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RingingClockStyle.entries.forEach { style ->
                        FilterChip(
                            selected = clockStyle == style,
                            colors = selectedChipColors(),
                            shape = SelectionChipShape,
                            onClick = {
                                if (clockStyle != style) {
                                    boxWidth = 0f
                                    boxHeight = 0f
                                    clockScale = 1f
                                }
                                clockStyle = style
                            },
                            label = { Text(style.label) }
                        )
                    }
                }

                Text("Clock Font", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Applies only to the ringing clock digits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RingingClockFont.entries.forEach { font ->
                        FilterChip(
                            selected = clockFont == font,
                            colors = selectedChipColors(),
                            shape = SelectionChipShape,
                            onClick = { clockFont = font },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (clockFont == RingingClockFont.CUSTOM_FILE) {
                    OutlinedButton(
                        onClick = { pickClockFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".ttf / .otf font file\u2026")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(clockCustomFontPath)) {
                            "Custom font loaded"
                        } else {
                            "No custom font selected yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AppearanceColorEditor(
                    title = "Clock Color",
                    color = clockColor,
                    gradient = clockGradient,
                    colorEnd = clockColorEnd,
                    direction = clockGradientDir,
                    onColor = { clockColor = it },
                    onGradient = { clockGradient = it },
                    onColorEnd = { clockColorEnd = it },
                    onDirection = { clockGradientDir = it }
                )

                Text("Clock Size", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Drag the bottom-right arc handle on the preview to resize. The clock " +
                        "and digits scale uniformly (aspect-locked), and the font auto-fits the " +
                        "box, so no size slider is needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

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
                    Switch(
                        checked = showAmPm,
                        onCheckedChange = { showAmPm = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show \"Alarm\" label",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = alarmLabelVisible,
                        onCheckedChange = { alarmLabelVisible = it }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

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
                    Switch(
                        checked = dateVisible,
                        onCheckedChange = { dateVisible = it }
                    )
                }
                if (dateVisible) {
                    Text(
                        text = "Date font",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockDateFont.entries.forEach { font ->
                            FilterChip(
                                selected = dateFont == font,
                                colors = selectedChipColors(),
                                shape = SelectionChipShape,
                                onClick = { dateFont = font },
                                label = { Text(font.label) }
                            )
                        }
                    }
                    if (dateFont == ClockDateFont.CUSTOM_FILE) {
                        OutlinedButton(
                            onClick = { pickDateFont() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Date font file (.ttf / .otf / .apk)\u2026")
                        }
                        Text(
                            text = if (FontManager.hasCustomFont(dateCustomFontPath)) {
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
                        direction = dateGradientDir,
                        onColor = { dateColor = it },
                        onGradient = { dateGradient = it },
                        onColorEnd = { dateColorEnd = it },
                        onDirection = { dateGradientDir = it }
                    )
                    Text(
                        text = "Drag the corner handle on the date to grow/shrink it uniformly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        boxWidth = 0f
                        boxHeight = 0f
                        clockScale = 1f
                        offsetX = 0f
                        offsetY = 0f
                        dateScaleLocal = 1f
                        dateOffsetX = 0f
                        dateOffsetY = 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset date/clock")
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
                    RingingClockFont.entries.forEach { font ->
                        FilterChip(
                            selected = dismissFont == font,
                            colors = selectedChipColors(),
                            shape = SelectionChipShape,
                            onClick = { dismissFont = font },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (dismissFont == RingingClockFont.CUSTOM_FILE) {
                    OutlinedButton(
                        onClick = { pickDismissFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".ttf / .otf font file\u2026")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(dismissCustomFontPath)) {
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
                    selected = dismissContainerColor,
                    onSelect = { dismissContainerColor = it }
                )
                Text("Text Color", style = MaterialTheme.typography.titleMedium)
                ColorSwatchRow(
                    selected = dismissTextColor,
                    onSelect = { dismissTextColor = it }
                )
                Button(onClick = { showDismissSheet = false }, modifier = Modifier.fillMaxWidth()) {
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
                    RingingClockFont.entries.forEach { font ->
                        FilterChip(
                            selected = snoozeFont == font,
                            colors = selectedChipColors(),
                            shape = SelectionChipShape,
                            onClick = { snoozeFont = font },
                            label = { Text(font.label) }
                        )
                    }
                }
                if (snoozeFont == RingingClockFont.CUSTOM_FILE) {
                    OutlinedButton(
                        onClick = { pickSnoozeFont() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(".ttf / .otf font file\u2026")
                    }
                    Text(
                        text = if (FontManager.hasCustomFont(snoozeCustomFontPath)) {
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
                    selected = snoozeContainerColor,
                    onSelect = { snoozeContainerColor = it }
                )
                Text("Text Color", style = MaterialTheme.typography.titleMedium)
                ColorSwatchRow(
                    selected = snoozeTextColor,
                    onSelect = { snoozeTextColor = it }
                )
                Button(onClick = { showSnoozeSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CustomizationSnoozePill(
    snoozeMinutes: Int,
    onSnooze: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    fontFamily: FontFamily? = null,
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
        SnoozeStepper(
            icon = Icons.Filled.Remove,
            contentDescription = "Decrease snooze",
            contentColor = contentColor,
            onStep = onMinus
        )
        Text(
            text = "Snooze $snoozeMinutes mins",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            ),
            color = contentColor,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSnooze)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
        SnoozeStepper(
            icon = Icons.Filled.Add,
            contentDescription = "Increase snooze",
            contentColor = contentColor,
            onStep = onPlus
        )
    }
}

@Composable
private fun SnoozeStepper(
    icon: ImageVector,
    contentDescription: String,
    contentColor: Color,
    onStep: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(contentColor.copy(alpha = 0.12f))
            .holdToRepeat(onStep = onStep),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
