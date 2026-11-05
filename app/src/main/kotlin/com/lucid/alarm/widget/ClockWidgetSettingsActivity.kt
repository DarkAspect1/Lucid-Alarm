package com.lucid.alarm.widget

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.ui.FontManager
import com.lucid.alarm.ui.components.CustomColorPickerDialog
import com.lucid.alarm.ui.theme.LucidAlarmTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class WidgetElementFocus {
    NONE, CLOCK, DATE
}

class ClockWidgetSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        setContent {
            LucidAlarmTheme {
                ClockWidgetSettingsScreen(
                    appWidgetId = appWidgetId,
                    onDismiss = { finish() },
                    onApplied = {
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ClockWidgetSettingsScreen(
    appWidgetId: Int,
    onDismiss: () -> Unit,
    onApplied: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember(context) { SettingsDataStore(context.applicationContext) }

    val initialConfig = remember(appWidgetId) {
        ClockWidgetConfig.load(context, appWidgetId)
    }

    var selectedStyle by remember { mutableStateOf(initialConfig.style) }
    var selectedColor by remember { mutableStateOf(Color(initialConfig.colorHex)) }
    var clockGradientEnabled by remember { mutableStateOf(initialConfig.clockGradientEnabled) }
    var clockGradientEndColor by remember { mutableStateOf(Color(initialConfig.clockGradientEndColorHex)) }

    var selectedFontPreset by remember { mutableStateOf(initialConfig.fontPreset) }
    var customFontPath by remember { mutableStateOf(initialConfig.customFontPath) }
    var showDate by remember { mutableStateOf(initialConfig.showDate) }
    var showAmPm by remember { mutableStateOf(initialConfig.showAmPm) }

    var dateColor by remember { mutableStateOf(Color(initialConfig.dateColorHex)) }
    var dateGradientEnabled by remember { mutableStateOf(initialConfig.dateGradientEnabled) }
    var dateGradientEndColor by remember { mutableStateOf(Color(initialConfig.dateGradientEndColorHex)) }
    var dateFontPreset by remember { mutableStateOf(initialConfig.dateFontPreset) }
    var dateCustomFontPath by remember { mutableStateOf(initialConfig.dateCustomFontPath) }

    var clockOffsetX by remember { mutableFloatStateOf(initialConfig.clockOffsetX) }
    var clockOffsetY by remember { mutableFloatStateOf(initialConfig.clockOffsetY) }
    var clockScale by remember { mutableFloatStateOf(initialConfig.clockScale) }

    var dateOffsetX by remember { mutableFloatStateOf(initialConfig.dateOffsetX) }
    var dateOffsetY by remember { mutableFloatStateOf(initialConfig.dateOffsetY) }
    var dateScale by remember { mutableFloatStateOf(initialConfig.dateScale) }

    val widgetSize = remember(appWidgetId) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            ClockWidgetProvider.getWidgetSizeDp(context, options)
        } else {
            Pair(320, 160)
        }
    }

    val widgetAspectRatio = remember(widgetSize) {
        val w = widgetSize.first.toFloat().coerceAtLeast(100f)
        val h = widgetSize.second.toFloat().coerceAtLeast(40f)
        (w / h).coerceIn(0.5f, 4.0f)
    }

    var is24Hour by remember { mutableStateOf(false) }
    var removeLeadingZero by remember { mutableStateOf(true) }
    var customColorTarget by remember { mutableStateOf<String?>(null) }

    val appFontStyle by settings.appFontStyleFlow.collectAsState(initial = AppFontStyle.SYSTEM_DEFAULT)
    val appCustomFontPath by settings.appCustomFontPathFlow.collectAsState(initial = "")

    LaunchedEffect(Unit) {
        is24Hour = runCatching { settings.timeFormatFlow.first().is24Hour }.getOrDefault(false)
        removeLeadingZero = runCatching { settings.removeLeadingZeroFlow.first() }.getOrDefault(true)
    }

    val wallpaperBitmap = remember(context) {
        runCatching {
            val wm = WallpaperManager.getInstance(context)
            val drawable = wm.drawable ?: wm.fastDrawable
            drawable?.let { drawableToBitmap(it) }
        }.getOrNull()
    }

    val fontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = FontManager.importFontFromUri(context, uri, "clock_widget_font")
                if (path.isNotEmpty()) {
                    customFontPath = path
                    selectedFontPreset = ClockFontPreset.CUSTOM_FILE
                }
            }
        }
    }

    val dateFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = FontManager.importFontFromUri(context, uri, "clock_widget_date_font")
                if (path.isNotEmpty()) {
                    dateCustomFontPath = path
                    dateFontPreset = ClockFontPreset.CUSTOM_FILE
                }
            }
        }
    }

    val presetColors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFF212121),
        Color(0xFF5C6BC0),
        Color(0xFFFFA726),
        Color(0xFF81C784)
    )

    val pillShape = RoundedCornerShape(percent = 50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1016))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.25f)
                .statusBarsPadding()
        ) {
            if (wallpaperBitmap != null) {
                Image(
                    bitmap = wallpaperBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1E202C),
                                    Color(0xFF12131D),
                                    Color(0xFF0A0B10)
                                )
                            )
                        )
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableW = maxWidth
                val availableH = maxHeight
                val outerPadding = 10.dp
                val (widgetW, widgetH) = remember(availableW, availableH, widgetAspectRatio) {
                    val maxW = (availableW.value - outerPadding.value * 2f).coerceAtLeast(80f)
                    val maxH = (availableH.value - outerPadding.value * 2f).coerceAtLeast(80f)
                    val containerRatio = maxW / maxH
                    if (containerRatio > widgetAspectRatio) {
                        val w = maxH * widgetAspectRatio
                        Pair(w.dp, maxH.dp)
                    } else {
                        val h = maxW / widgetAspectRatio
                        Pair(maxW.dp, h.dp)
                    }
                }

                LiveClockPreview(
                    widgetWidth = widgetW,
                    widgetHeight = widgetH,
                    style = selectedStyle,
                    color = selectedColor,
                    clockGradientEnabled = clockGradientEnabled,
                    clockGradientEndColor = clockGradientEndColor,
                    clockFontPreset = selectedFontPreset,
                    clockCustomFontPath = customFontPath,
                    appFontStyle = appFontStyle,
                    appCustomFontPath = appCustomFontPath,
                    dateColor = dateColor,
                    dateGradientEnabled = dateGradientEnabled,
                    dateGradientEndColor = dateGradientEndColor,
                    dateFontPreset = dateFontPreset,
                    dateCustomFontPath = dateCustomFontPath,
                    is24Hour = is24Hour,
                    removeLeadingZero = removeLeadingZero,
                    showDate = showDate,
                    showAmPm = showAmPm,
                    clockOffsetX = clockOffsetX,
                    clockOffsetY = clockOffsetY,
                    clockScale = clockScale,
                    dateOffsetX = dateOffsetX,
                    dateOffsetY = dateOffsetY,
                    dateScale = dateScale,
                    onClockOffsetChange = { x, y ->
                        clockOffsetX = x
                        clockOffsetY = y
                    },
                    onClockScaleChange = { clockScale = it },
                    onDateOffsetChange = { x, y ->
                        dateOffsetX = x
                        dateOffsetY = y
                    },
                    onDateScaleChange = { dateScale = it }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.05f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clockOffsetX = 0f
                            clockOffsetY = 0f
                            clockScale = 1.0f
                        },
                        shape = pillShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clock_widget_reset_clock),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            dateOffsetX = 0f
                            dateOffsetY = ClockWidgetConfig.DEFAULT_DATE_OFFSET_Y
                            dateScale = 1.0f
                        },
                        shape = pillShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clock_widget_reset_date),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.clock_widget_style),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClockStyle.entries.forEach { style ->
                        val isSelected = style == selectedStyle
                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            label = "styleBg"
                        )

                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(cardBg)
                                .clickable {
                                    if (selectedStyle != style) {
                                        selectedStyle = style
                                        clockScale = 1.0f
                                        clockOffsetX = 0f
                                        clockOffsetY = 0f
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(style.titleRes),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.clock_widget_show_date),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = showDate,
                        onCheckedChange = { showDate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (!is24Hour) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(pillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.clock_widget_show_ampm),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = showAmPm,
                            onCheckedChange = { showAmPm = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.clock_widget_color),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presetColors.forEach { color ->
                        val isSelected = selectedColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (color == Color.White) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(pillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { customColorTarget = "clock_primary" }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ColorLens,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.clock_widget_custom_color),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.clock_widget_clock_gradient),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = clockGradientEnabled,
                        onCheckedChange = { clockGradientEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (clockGradientEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.clock_widget_gradient_end_color),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetColors.forEach { color ->
                            val isSelected = clockGradientEndColor.toArgb() == color.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { clockGradientEndColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.White) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(pillShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { customColorTarget = "clock_gradient_end" }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ColorLens,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.clock_widget_custom_color),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.clock_widget_font),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClockFontPreset.entries.filter { it != ClockFontPreset.CUSTOM_FILE }.forEach { preset ->
                        val isSelected = selectedFontPreset == preset
                        val bg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            label = "fontBg"
                        )

                        Box(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(bg)
                                .clickable { selectedFontPreset = preset }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(preset.titleRes),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val hasCustom = selectedFontPreset == ClockFontPreset.CUSTOM_FILE && customFontPath.isNotBlank()
                Button(
                    onClick = { fontLauncher.launch(FontManager.FONT_MIME_TYPES) },
                    shape = pillShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (hasCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FontDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasCustom) {
                            stringResource(R.string.clock_widget_custom_font_selected, File(customFontPath).name)
                        } else {
                            stringResource(R.string.clock_widget_font_custom)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (showDate) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.clock_widget_date_color),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetColors.forEach { color ->
                            val isSelected = dateColor.toArgb() == color.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { dateColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.White) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(pillShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { customColorTarget = "date_primary" }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ColorLens,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.clock_widget_custom_color),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(pillShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.clock_widget_date_gradient),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = dateGradientEnabled,
                            onCheckedChange = { dateGradientEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (dateGradientEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.clock_widget_gradient_end_color),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetColors.forEach { color ->
                                val isSelected = dateGradientEndColor.toArgb() == color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { dateGradientEndColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = if (color == Color.White) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(pillShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { customColorTarget = "date_gradient_end" }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.ColorLens,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.clock_widget_custom_color),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.clock_widget_date_font),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ClockFontPreset.entries.filter { it != ClockFontPreset.CUSTOM_FILE }.forEach { preset ->
                            val isSelected = dateFontPreset == preset
                            val bg by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                label = "dateFontBg"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(pillShape)
                                    .background(bg)
                                    .clickable { dateFontPreset = preset }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(preset.titleRes),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val hasCustomDateFont = dateFontPreset == ClockFontPreset.CUSTOM_FILE && dateCustomFontPath.isNotBlank()
                    Button(
                        onClick = { dateFontLauncher.launch(FontManager.FONT_MIME_TYPES) },
                        shape = pillShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (hasCustomDateFont) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FontDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasCustomDateFont) {
                                stringResource(R.string.clock_widget_custom_font_selected, File(dateCustomFontPath).name)
                            } else {
                                stringResource(R.string.clock_widget_custom_date_font)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = pillShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = null,
                        border = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clock_widget_cancel),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val newConfig = ClockWidgetConfig(
                                style = selectedStyle,
                                colorHex = selectedColor.toArgb(),
                                clockGradientEnabled = clockGradientEnabled,
                                clockGradientEndColorHex = clockGradientEndColor.toArgb(),
                                fontPreset = selectedFontPreset,
                                customFontPath = customFontPath,
                                scale = clockScale,
                                showDate = showDate,
                                showAmPm = showAmPm,
                                dateFontPreset = dateFontPreset,
                                dateCustomFontPath = dateCustomFontPath,
                                dateColorHex = dateColor.toArgb(),
                                dateGradientEnabled = dateGradientEnabled,
                                dateGradientEndColorHex = dateGradientEndColor.toArgb(),
                                clockOffsetX = clockOffsetX,
                                clockOffsetY = clockOffsetY,
                                clockScale = clockScale,
                                dateOffsetX = dateOffsetX,
                                dateOffsetY = dateOffsetY,
                                dateScale = dateScale
                            )
                            ClockWidgetConfig.save(context, appWidgetId, newConfig)
                            ClockWidgetUpdater.updateClockWidget(context, appWidgetId)
                            onApplied()
                        },
                        shape = pillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clock_widget_apply),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (customColorTarget != null) {
        val initialColorForTarget = when (customColorTarget) {
            "clock_primary" -> selectedColor
            "clock_gradient_end" -> clockGradientEndColor
            "date_primary" -> dateColor
            "date_gradient_end" -> dateGradientEndColor
            else -> selectedColor
        }
        CustomColorPickerDialog(
            initialColor = initialColorForTarget.toArgb().toLong(),
            onDismiss = { customColorTarget = null },
            onConfirm = { argb ->
                val color = Color(argb.toInt())
                when (customColorTarget) {
                    "clock_primary" -> selectedColor = color
                    "clock_gradient_end" -> clockGradientEndColor = color
                    "date_primary" -> dateColor = color
                    "date_gradient_end" -> dateGradientEndColor = color
                }
                customColorTarget = null
            }
        )
    }
}

@Composable
fun LiveClockPreview(
    widgetWidth: Dp,
    widgetHeight: Dp,
    style: ClockStyle,
    color: Color,
    clockGradientEnabled: Boolean,
    clockGradientEndColor: Color,
    clockFontPreset: ClockFontPreset,
    clockCustomFontPath: String,
    appFontStyle: AppFontStyle,
    appCustomFontPath: String,
    dateColor: Color,
    dateGradientEnabled: Boolean,
    dateGradientEndColor: Color,
    dateFontPreset: ClockFontPreset,
    dateCustomFontPath: String,
    is24Hour: Boolean,
    removeLeadingZero: Boolean,
    showDate: Boolean,
    showAmPm: Boolean,
    clockOffsetX: Float,
    clockOffsetY: Float,
    clockScale: Float,
    dateOffsetX: Float,
    dateOffsetY: Float,
    dateScale: Float,
    onClockOffsetChange: (Float, Float) -> Unit,
    onClockScaleChange: (Float) -> Unit,
    onDateOffsetChange: (Float, Float) -> Unit,
    onDateScaleChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val liveTime by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000L)
        }
    }

    val cal = remember(liveTime) {
        Calendar.getInstance().apply { time = liveTime }
    }

    val targetLocale = WidgetThemeHelper.getTargetLocale(context)
    val dateStr = if (style == ClockStyle.STYLE_6) {
        SimpleDateFormat("EEEE, d MMMM", targetLocale).format(liveTime)
    } else {
        SimpleDateFormat("EEE, d MMM", targetLocale).format(liveTime)
    }

    val isAm = cal.get(Calendar.AM_PM) == Calendar.AM
    val amPmStr = if (!is24Hour && showAmPm) {
        context.getString(if (isAm) R.string.time_am else R.string.time_pm)
    } else {
        ""
    }

    val clockTypeface: Typeface = remember(clockFontPreset, clockCustomFontPath, style) {
        val tf = if (clockFontPreset == ClockFontPreset.CUSTOM_FILE && clockCustomFontPath.isNotBlank()) {
            val file = File(clockCustomFontPath)
            if (file.exists()) {
                runCatching { Typeface.createFromFile(file) }.getOrNull()
            } else null
        } else {
            when (clockFontPreset) {
                ClockFontPreset.SYSTEM_DEFAULT -> {
                    if (style == ClockStyle.STYLE_5 || style == ClockStyle.STYLE_6) {
                        Typeface.create("sans-serif-black", Typeface.BOLD)
                    } else {
                        Typeface.DEFAULT
                    }
                }
                ClockFontPreset.SANS_SERIF -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                ClockFontPreset.SERIF -> Typeface.create("serif", Typeface.NORMAL)
                ClockFontPreset.HEAVY_BOLD -> Typeface.create("sans-serif-black", Typeface.BOLD)
                ClockFontPreset.MONOSPACE -> Typeface.create("monospace", Typeface.NORMAL)
                ClockFontPreset.CUSTOM_FILE -> Typeface.DEFAULT
            }
        }
        tf ?: Typeface.DEFAULT
    }

    val appTypeface: Typeface = remember(appFontStyle, appCustomFontPath) {
        val tf = when (appFontStyle) {
            AppFontStyle.SYSTEM_DEFAULT -> Typeface.DEFAULT
            AppFontStyle.SERIF -> Typeface.create("serif", Typeface.NORMAL)
            AppFontStyle.CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
            AppFontStyle.CUSTOM_FILE -> {
                if (appCustomFontPath.isNotBlank() && File(appCustomFontPath).exists()) {
                    runCatching { Typeface.createFromFile(appCustomFontPath) }.getOrNull()
                } else null
            }
        }
        tf ?: Typeface.DEFAULT
    }

    val dateTypeface: Typeface = remember(dateFontPreset, dateCustomFontPath, appTypeface) {
        val tf = if (dateFontPreset == ClockFontPreset.CUSTOM_FILE && dateCustomFontPath.isNotBlank()) {
            val file = File(dateCustomFontPath)
            if (file.exists()) {
                runCatching { Typeface.createFromFile(file) }.getOrNull()
            } else null
        } else {
            when (dateFontPreset) {
                ClockFontPreset.SYSTEM_DEFAULT -> appTypeface
                ClockFontPreset.SANS_SERIF -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                ClockFontPreset.SERIF -> Typeface.create("serif", Typeface.NORMAL)
                ClockFontPreset.HEAVY_BOLD -> Typeface.create("sans-serif-black", Typeface.BOLD)
                ClockFontPreset.MONOSPACE -> Typeface.create("monospace", Typeface.NORMAL)
                ClockFontPreset.CUSTOM_FILE -> appTypeface
            }
        }
        tf ?: appTypeface
    }

    var selectedElement by remember { mutableStateOf(WidgetElementFocus.NONE) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var clockBounds by remember { mutableStateOf(RectF()) }
    var dateBounds by remember { mutableStateOf(RectF()) }
    var previousStyle by remember { mutableStateOf<ClockStyle?>(null) }
    var pendingStyleFit by remember { mutableStateOf(false) }

    var isDraggingClock by remember { mutableStateOf(false) }
    var isDraggingDate by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var snapY by remember { mutableStateOf(false) }
    var dateSnapX by remember { mutableStateOf(false) }
    var dateSnapY by remember { mutableStateOf(false) }

    val currentClockOffsetX by rememberUpdatedState(clockOffsetX)
    val currentClockOffsetY by rememberUpdatedState(clockOffsetY)
    val currentDateOffsetX by rememberUpdatedState(dateOffsetX)
    val currentDateOffsetY by rememberUpdatedState(dateOffsetY)

    var rawClockOffsetX by remember { mutableFloatStateOf(0f) }
    var rawClockOffsetY by remember { mutableFloatStateOf(0f) }
    var rawDateOffsetX by remember { mutableFloatStateOf(0f) }
    var rawDateOffsetY by remember { mutableFloatStateOf(0f) }

    val currentClockScale by rememberUpdatedState(clockScale)
    val currentDateScale by rememberUpdatedState(dateScale)
    var dragStartClockScale by remember { mutableFloatStateOf(1f) }
    var dragStartDateScale by remember { mutableFloatStateOf(1f) }

    val currentConfig = remember(
        style, color, clockGradientEnabled, clockGradientEndColor,
        clockFontPreset, clockCustomFontPath,
        dateColor, dateGradientEnabled, dateGradientEndColor,
        dateFontPreset, dateCustomFontPath,
        clockOffsetX, clockOffsetY, clockScale,
        dateOffsetX, dateOffsetY, dateScale, showDate, showAmPm
    ) {
        ClockWidgetConfig(
            style = style,
            colorHex = color.toArgb(),
            clockGradientEnabled = clockGradientEnabled,
            clockGradientEndColorHex = clockGradientEndColor.toArgb(),
            fontPreset = clockFontPreset,
            customFontPath = clockCustomFontPath,
            dateFontPreset = dateFontPreset,
            dateCustomFontPath = dateCustomFontPath,
            dateColorHex = dateColor.toArgb(),
            dateGradientEnabled = dateGradientEnabled,
            dateGradientEndColorHex = dateGradientEndColor.toArgb(),
            clockOffsetX = clockOffsetX,
            clockOffsetY = clockOffsetY,
            clockScale = clockScale,
            dateOffsetX = dateOffsetX,
            dateOffsetY = dateOffsetY,
            dateScale = dateScale,
            showDate = showDate,
            showAmPm = showAmPm
        )
    }

    val widgetWPx = with(LocalDensity.current) { widgetWidth.toPx() }
    val widgetHPx = with(LocalDensity.current) { widgetHeight.toPx() }

    LaunchedEffect(style) {
        if (previousStyle != null && previousStyle != style) {
            selectedElement = WidgetElementFocus.CLOCK
            pendingStyleFit = true
        }
        previousStyle = style
    }

    LaunchedEffect(style, clockBounds, pendingStyleFit) {
        if (pendingStyleFit && clockBounds.width() > 0 && clockBounds.height() > 0 && widgetWPx > 0f && widgetHPx > 0f) {
            val maxAllowedW = widgetWPx * 0.94f
            val maxAllowedH = widgetHPx * 0.94f
            if (clockBounds.width() > maxAllowedW || clockBounds.height() > maxAllowedH) {
                val scaleFactor = minOf(maxAllowedW / clockBounds.width(), maxAllowedH / clockBounds.height())
                if (scaleFactor < 1.0f) {
                    onClockScaleChange((clockScale * scaleFactor).coerceAtLeast(0.2f))
                    onClockOffsetChange(0f, 0f)
                }
            }
            pendingStyleFit = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                canvasWidth = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { selectedElement = WidgetElementFocus.NONE })
            }
    ) {
        val widgetBoxLeft = (canvasWidth - widgetWPx) / 2f
        val widgetBoxTop = (canvasHeight - widgetHPx) / 2f
        val widgetBoxRight = widgetBoxLeft + widgetWPx
        val widgetBoxBottom = widgetBoxTop + widgetHPx

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            val cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
            drawRoundRect(
                color = Color.White.copy(alpha = 0.45f),
                topLeft = Offset(widgetBoxLeft + strokeWidth / 2f, widgetBoxTop + strokeWidth / 2f),
                size = Size(widgetWPx - strokeWidth, widgetHPx - strokeWidth),
                cornerRadius = cornerRadius,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(widgetBoxLeft, widgetBoxTop)
                val out = ClockWidgetRenderer.render(
                    canvas = canvas.nativeCanvas,
                    widthPx = widgetWPx,
                    heightPx = widgetHPx,
                    config = currentConfig,
                    cal = cal,
                    is24Hour = is24Hour,
                    removeLeadingZero = removeLeadingZero,
                    clockTypeface = clockTypeface,
                    appTypeface = appTypeface,
                    dateTypeface = dateTypeface,
                    density = density,
                    amPmStr = amPmStr,
                    dateStr = if (showDate) dateStr else ""
                )
                canvas.restore()
                clockBounds = out.clockBounds
                dateBounds = out.dateBounds
            }
        }

        if ((isDraggingClock && (snapX || snapY)) || (isDraggingDate && (dateSnapX || dateSnapY))) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val guideColor = Color.White.copy(alpha = 0.6f)
                val stroke = 1.dp.toPx()
                if (snapY || dateSnapY) {
                    drawLine(
                        color = guideColor,
                        start = Offset(widgetBoxLeft, widgetBoxTop + widgetHPx / 2f),
                        end = Offset(widgetBoxRight, widgetBoxTop + widgetHPx / 2f),
                        strokeWidth = stroke
                    )
                }
                if (snapX || dateSnapX) {
                    drawLine(
                        color = guideColor,
                        start = Offset(widgetBoxLeft + widgetWPx / 2f, widgetBoxTop),
                        end = Offset(widgetBoxLeft + widgetWPx / 2f, widgetBoxBottom),
                        strokeWidth = stroke
                    )
                }
            }
        }

        if (clockBounds.width() > 0 && clockBounds.height() > 0 && widgetWPx > 0f && widgetHPx > 0f) {
            val pad = 4f * density
            val clkW = clockBounds.width() + pad * 2f
            val clkH = clockBounds.height() + pad * 2f
            val boxLeft = widgetBoxLeft + clockBounds.left - pad
            val boxTop = widgetBoxTop + clockBounds.top - pad

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(boxLeft.roundToInt(), boxTop.roundToInt())
                    }
                    .size((clkW / density).dp, (clkH / density).dp)
                    .pointerInput(clkW, clkH) {
                        detectTapGestures(onTap = { selectedElement = WidgetElementFocus.CLOCK })
                    }
                    .pointerInput(widgetWPx, widgetHPx) {
                        detectDragGestures(
                            onDragStart = {
                                selectedElement = WidgetElementFocus.CLOCK
                                isDraggingClock = true
                                snapX = false
                                snapY = false
                                rawClockOffsetX = currentClockOffsetX * widgetWPx
                                rawClockOffsetY = currentClockOffsetY * widgetHPx
                            },
                            onDragEnd = {
                                isDraggingClock = false
                                snapX = false
                                snapY = false
                            },
                            onDragCancel = {
                                isDraggingClock = false
                                snapX = false
                                snapY = false
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val maxRawX = widgetWPx * 0.5f
                            val maxRawY = widgetHPx * 0.5f
                            rawClockOffsetX = (rawClockOffsetX + dragAmount.x).coerceIn(-maxRawX, maxRawX)
                            rawClockOffsetY = (rawClockOffsetY + dragAmount.y).coerceIn(-maxRawY, maxRawY)

                            val snapThresholdPx = 16f * density
                            val isSnappedX = abs(rawClockOffsetX) < snapThresholdPx
                            val isSnappedY = abs(rawClockOffsetY) < snapThresholdPx
                            snapX = isSnappedX
                            snapY = isSnappedY

                            val snappedX = if (isSnappedX) 0f else rawClockOffsetX
                            val snappedY = if (isSnappedY) 0f else rawClockOffsetY

                            onClockOffsetChange(
                                (snappedX / widgetWPx).coerceIn(-0.5f, 0.5f),
                                (snappedY / widgetHPx).coerceIn(-0.5f, 0.5f)
                            )
                        }
                    }
            )

            if (selectedElement == WidgetElementFocus.CLOCK) {
                val handleRadius = (20f * clockScale.coerceIn(0.5f, 3.0f)).dp.coerceIn(16.dp, 40.dp)
                val handleRadiusPx = handleRadius.value * density
                val handleBoxSizePx = handleRadiusPx * 2f
                val handleInset = 4f * density
                val normalRight = boxLeft + clkW
                val normalBottom = boxTop + clkH
                val targetRight = minOf(normalRight, widgetBoxRight - handleInset)
                    .coerceAtLeast(widgetBoxLeft + handleBoxSizePx + handleInset)
                val targetBottom = minOf(normalBottom, widgetBoxBottom - handleInset)
                    .coerceAtLeast(widgetBoxTop + handleBoxSizePx + handleInset)
                val handleLeft = targetRight - handleBoxSizePx
                val handleTop = targetBottom - handleBoxSizePx

                Box(
                    modifier = Modifier
                        .offset { IntOffset(handleLeft.roundToInt(), handleTop.roundToInt()) }
                        .size(handleRadius * 2f)
                        .pointerInput(selectedElement) {
                            detectDragGestures(
                                onDragStart = {
                                    dragStartClockScale = currentClockScale
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = (dragAmount.x + dragAmount.y) / (160f * density)
                                    val updatedScale = (dragStartClockScale + delta).coerceIn(0.2f, 10.0f)
                                    dragStartClockScale = updatedScale
                                    onClockScaleChange(updatedScale)
                                }
                            )
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

        if (showDate && dateBounds.width() > 0 && dateBounds.height() > 0 && widgetWPx > 0f && widgetHPx > 0f) {
            val padX = 14f * density
            val padY = 8f * density
            val dtW = dateBounds.width() + padX * 2f
            val dtH = dateBounds.height() + padY * 2f
            val dtCx = widgetBoxLeft + (widgetWPx / 2f) + (dateOffsetX * widgetWPx)
            val dtCy = widgetBoxTop + (widgetHPx / 2f) + (dateOffsetY * widgetHPx)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((dtCx - dtW / 2f).roundToInt(), (dtCy - dtH / 2f).roundToInt())
                    }
                    .size((dtW / density).dp, (dtH / density).dp)
                    .pointerInput(dtW, dtH) {
                        detectTapGestures(onTap = { selectedElement = WidgetElementFocus.DATE })
                    }
                    .pointerInput(widgetWPx, widgetHPx) {
                        detectDragGestures(
                            onDragStart = {
                                selectedElement = WidgetElementFocus.DATE
                                isDraggingDate = true
                                dateSnapX = false
                                dateSnapY = false
                                rawDateOffsetX = currentDateOffsetX * widgetWPx
                                rawDateOffsetY = currentDateOffsetY * widgetHPx
                            },
                            onDragEnd = {
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
                            val maxRawX = widgetWPx * 0.5f
                            val maxRawY = widgetHPx * 0.5f
                            rawDateOffsetX = (rawDateOffsetX + dragAmount.x).coerceIn(-maxRawX, maxRawX)
                            rawDateOffsetY = (rawDateOffsetY + dragAmount.y).coerceIn(-maxRawY, maxRawY)

                            val snapThresholdPx = 16f * density
                            val isSnappedX = abs(rawDateOffsetX) < snapThresholdPx
                            val isSnappedY = abs(rawDateOffsetY) < snapThresholdPx
                            dateSnapX = isSnappedX
                            dateSnapY = isSnappedY

                            val snappedX = if (isSnappedX) 0f else rawDateOffsetX
                            val snappedY = if (isSnappedY) 0f else rawDateOffsetY

                            onDateOffsetChange(
                                (snappedX / widgetWPx).coerceIn(-0.5f, 0.5f),
                                (snappedY / widgetHPx).coerceIn(-0.5f, 0.5f)
                            )
                        }
                    }
            )

            if (selectedElement == WidgetElementFocus.DATE) {
                val dtHandleRadius = (10f * dateScale.coerceIn(0.6f, 2.5f)).dp.coerceIn(8.dp, 18.dp)
                val dtHandleRadiusPx = dtHandleRadius.value * density
                val dtHandleBoxSizePx = dtHandleRadiusPx * 2f
                val handleInset = 4f * density
                val normalDtRight = dtCx + dtW / 2f
                val normalDtBottom = dtCy + dtH / 2f
                val targetDtRight = minOf(normalDtRight, widgetBoxRight - handleInset)
                    .coerceAtLeast(widgetBoxLeft + dtHandleBoxSizePx + handleInset)
                val targetDtBottom = minOf(normalDtBottom, widgetBoxBottom - handleInset)
                    .coerceAtLeast(widgetBoxTop + dtHandleBoxSizePx + handleInset)
                val dtHandleLeft = targetDtRight - dtHandleBoxSizePx
                val dtHandleTop = targetDtBottom - dtHandleBoxSizePx

                Box(
                    modifier = Modifier
                        .offset { IntOffset(dtHandleLeft.roundToInt(), dtHandleTop.roundToInt()) }
                        .size(dtHandleRadius * 2f)
                        .pointerInput(selectedElement) {
                            detectDragGestures(
                                onDragStart = {
                                    dragStartDateScale = currentDateScale
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val delta = (dragAmount.x + dragAmount.y) / (140f * density)
                                    val updatedScale = (dragStartDateScale + delta).coerceIn(0.2f, 10.0f)
                                    dragStartDateScale = updatedScale
                                    onDateScaleChange(updatedScale)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 2.5f.dp.toPx()
                        val radius = dtHandleRadius.toPx()
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

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
