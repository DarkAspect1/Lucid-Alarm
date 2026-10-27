package com.lucid.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.AppDatabase
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.AppFontStyle
import com.lucid.alarm.data.prefs.BackgroundPalette
import com.lucid.alarm.data.prefs.ClockDateFont
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.data.prefs.RingingBackgroundStyle
import com.lucid.alarm.data.prefs.RingingClockFont
import com.lucid.alarm.data.prefs.RingingClockStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.ThemeMode
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.receiver.AlarmReceiver
import com.lucid.alarm.service.AlarmAudioService
import com.lucid.alarm.ui.LocalAppFont
import com.lucid.alarm.ui.components.AlarmClockBox
import com.lucid.alarm.ui.components.AlarmLockBackground
import com.lucid.alarm.ui.components.AlarmLockPalette
import com.lucid.alarm.ui.components.ClockDateText
import com.lucid.alarm.ui.components.RingingActionPill
import com.lucid.alarm.ui.components.SnoozePill
import com.lucid.alarm.ui.components.clockTextBrush
import com.lucid.alarm.ui.components.resolveDateFontFamily
import com.lucid.alarm.ui.components.resolveRingingFontFamily
import com.lucid.alarm.ui.theme.LucidAlarmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.roundToInt

class AlarmRingingActivity : ComponentActivity() {

    private var alarmId by mutableIntStateOf(-1)
    private var alarmLabel by mutableStateOf("")
    private var snoozeCount by mutableIntStateOf(0)
    private var snoozeLimit by mutableIntStateOf(-1)
    private var isAwakeCheckActive by mutableStateOf(false)

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmAudioService.ACTION_RINGING_DISMISSED) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockScreenWakeup()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
        }

        val filter = IntentFilter(AlarmAudioService.ACTION_RINGING_DISMISSED)
        ContextCompat.registerReceiver(
            this,
            dismissReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        applyIntent(intent)
        val settingsDataStore = SettingsDataStore(applicationContext)

        setContent {
            LaunchedEffect(Unit) {
                AlarmAudioService.ringingState.collect { state ->
                    if (state != null) {
                        alarmId = state.alarmId
                        alarmLabel = state.label
                        snoozeCount = state.snoozeCount
                        snoozeLimit = state.snoozeLimit
                        isAwakeCheckActive = state.isAwakeCheckActive
                    }
                }
            }

            val appFontStyle by settingsDataStore.appFontStyleFlow.collectAsState(
                initial = AppFontStyle.SYSTEM_DEFAULT
            )
            val accentColor by settingsDataStore.accentColorFlow.collectAsState(
                initial = AccentColor.DEFAULT
            )
            val backgroundPalette by settingsDataStore.backgroundPaletteFlow.collectAsState(
                initial = BackgroundPalette.OBSIDIAN
            )
            val themeMode by settingsDataStore.themeModeFlow.collectAsState(
                initial = ThemeMode.SYSTEM
            )
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val ringingBackground by settingsDataStore.ringingBackgroundFlow.collectAsState(
                initial = RingingBackgroundStyle.STARFIELD
            )
            val ringingSolidColor by settingsDataStore.ringingSolidColorFlow.collectAsState(
                initial = 0L
            )

            val ringingTopIsLight = when (ringingBackground) {
                RingingBackgroundStyle.SOLID_COLOR ->
                    androidx.compose.ui.graphics.Color(ringingSolidColor).luminance() > 0.5f
                RingingBackgroundStyle.WARM_SUNRISE -> true
                else -> false
            }
            LaunchedEffect(ringingTopIsLight) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = ringingTopIsLight
                    isAppearanceLightNavigationBars = ringingTopIsLight
                }
            }

            LucidAlarmTheme(
                accentColor = accentColor,
                backgroundPalette = backgroundPalette,
                appFontStyle = appFontStyle,
                dark = darkTheme,
                settingsDataStore = settingsDataStore
            ) {
                AlarmRingingScreen(
                    alarmId = alarmId,
                    alarmLabel = alarmLabel,
                    snoozeCount = snoozeCount,
                    snoozeLimit = snoozeLimit,
                    settingsDataStore = settingsDataStore,
                    onSnooze = { snoozeMinutes ->
                        val snoozeIntent = Intent(this, AlarmAudioService::class.java).apply {
                            action = AlarmAudioService.ACTION_SNOOZE_ALARM
                            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                        }
                        startService(snoozeIntent)
                        finishAndRemoveTask()
                    },
                    onDismiss = {
                        val stopIntent = Intent(this, AlarmAudioService::class.java).apply {
                            action = AlarmAudioService.ACTION_STOP_ALARM
                            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                            putExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, isAwakeCheckActive)
                        }
                        startService(stopIntent)
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        alarmId = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1
        alarmLabel = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL).orEmpty()
        snoozeCount = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, 0) ?: 0
        snoozeLimit = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, -1) ?: -1
        isAwakeCheckActive = intent?.getBooleanExtra(AlarmReceiver.EXTRA_IS_AWAKE_CHECK_ACTIVE, false) ?: false
        ensureRingingIfRequested(intent)
    }

    private fun ensureRingingIfRequested(intent: Intent?) {
        if (intent == null) return
        if (!intent.getBooleanExtra(AlarmReceiver.EXTRA_ENSURE_RINGING, false)) return
        val serviceIntent = Intent(this, AlarmAudioService::class.java).apply {
            action = AlarmAudioService.ACTION_START_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarmLabel)
            putExtra(
                AlarmReceiver.EXTRA_ALARM_HOUR,
                intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_HOUR, -1)
            )
            putExtra(
                AlarmReceiver.EXTRA_ALARM_MINUTE,
                intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_MINUTE, -1)
            )
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, snoozeCount)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_LIMIT, snoozeLimit)
        }
        runCatching { ContextCompat.startForegroundService(this, serviceIntent) }
            .onSuccess { intent.removeExtra(AlarmReceiver.EXTRA_ENSURE_RINGING) }
            .onFailure {
                Log.e(TAG, "Unable to start ringing service from fallback", it)
            }
    }

    private fun configureLockScreenWakeup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(dismissReceiver) }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmRingingActivity"
    }
}

@Composable
fun AlarmRingingScreen(
    alarmId: Int,
    alarmLabel: String,
    snoozeCount: Int,
    snoozeLimit: Int,
    settingsDataStore: SettingsDataStore,
    onSnooze: (snoozeMinutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext

    var alarmEntity by remember { mutableStateOf<AlarmEntity?>(null) }
    LaunchedEffect(alarmId) {
        alarmEntity = null
        if (alarmId == -1) return@LaunchedEffect
        alarmEntity = runCatching {
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(appContext).alarmDao().getAlarmById(alarmId)
            }
        }.getOrNull()
    }

    val backgroundStyle by settingsDataStore.ringingBackgroundFlow.collectAsState(
        initial = RingingBackgroundStyle.STARFIELD
    )
    val backgroundImageUri by settingsDataStore.ringingBackgroundImageUriFlow.collectAsState(
        initial = ""
    )
    val backgroundVideoUri by settingsDataStore.ringingVideoUriFlow.collectAsState(
        initial = ""
    )
    val globalInteractiveColors by settingsDataStore.ringingInteractiveColorsFlow.collectAsState(
        initial = emptyList()
    )
    val themeMode by settingsDataStore.themeModeFlow.collectAsState(
        initial = com.lucid.alarm.data.prefs.ThemeMode.SYSTEM
    )
    val clockStyle by settingsDataStore.ringingClockStyleFlow.collectAsState(
        initial = RingingClockStyle.HORIZONTAL_DIGITAL
    )
    val clockFont by settingsDataStore.alarmClockFontStyleFlow.collectAsState(
        initial = RingingClockFont.SYSTEM_DEFAULT
    )
    val clockCustomFontPath by settingsDataStore.alarmClockCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val clockFontSize by settingsDataStore.clockFontSizeFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_CLOCK_FONT_SIZE
    )
    val clockScale by settingsDataStore.clockContainerScaleFlow.collectAsState(initial = 1f)
    val showAmPm by settingsDataStore.showAmPmFlow.collectAsState(initial = true)
    val dateVisible by settingsDataStore.clockDateVisibleFlow.collectAsState(initial = true)
    val dateFont by settingsDataStore.clockDateFontFlow.collectAsState(
        initial = ClockDateFont.MATCH_CLOCK
    )
    val dateCustomFontPath by settingsDataStore.clockDateCustomFontPathFlow.collectAsState(
        initial = ""
    )
    val dateScale by settingsDataStore.clockDateScaleFlow.collectAsState(initial = 1f)
    val dateOffsetX by settingsDataStore.clockDateOffsetXFlow.collectAsState(initial = 0f)
    val dateOffsetY by settingsDataStore.clockDateOffsetYFlow.collectAsState(initial = 0f)
    val alarmLabelVisible by settingsDataStore.alarmLabelVisibleFlow.collectAsState(initial = true)
    val boxWidth by settingsDataStore.clockBoxWidthFlow.collectAsState(initial = 0f)
    val boxHeight by settingsDataStore.clockBoxHeightFlow.collectAsState(initial = 0f)
    val removeLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(initial = true)
    val ringingBlur by settingsDataStore.ringingBlurFlow.collectAsState(initial = 0f)
    val ringingDim by settingsDataStore.ringingDimFlow.collectAsState(
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
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    val savedSnoozeMinutes by settingsDataStore.ringingSnoozeMinutesFlow.collectAsState(
        initial = SettingsDataStore.DEFAULT_SNOOZE_MINUTES
    )
    val savedOffsetX by settingsDataStore.freeDragOffsetXFlow.collectAsState(initial = 0f)
    val savedOffsetY by settingsDataStore.freeDragOffsetYFlow.collectAsState(initial = 0f)
    val globalClockBoxEnabled by settingsDataStore.clockBoxEnabledFlow.collectAsState(initial = false)
    val globalClockBoxColor by settingsDataStore.clockBoxColorFlow.collectAsState(initial = 0L)
    val globalClockBoxOpacity by settingsDataStore.clockBoxOpacityFlow.collectAsState(initial = 0.35f)
    val globalSolidColor by settingsDataStore.ringingSolidColorFlow.collectAsState(initial = 0L)
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

    var snoozeMinutes by remember(savedSnoozeMinutes) { mutableIntStateOf(savedSnoozeMinutes) }

    val appearance = alarmEntity?.takeIf { it.customAppearanceEnabled }
    val appearanceBackgroundStyle = appearance?.customBackgroundStyle
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingBackgroundStyle.valueOf(raw) }.getOrNull() }
    val effectiveBackgroundStyle = when {
        appearanceBackgroundStyle != null -> appearanceBackgroundStyle
        appearance != null && appearance.customBackgroundUri.isNotBlank() ->
            RingingBackgroundStyle.CUSTOM_IMAGE
        else -> backgroundStyle
    }
    val effectiveImageUri = appearance?.customBackgroundUri?.takeIf { it.isNotBlank() }
        ?: backgroundImageUri
    val effectiveVideoUri = appearance?.customVideoUri?.takeIf { it.isNotBlank() }
        ?: backgroundVideoUri

    val effectiveInteractiveColors = appearance?.customBackgroundInteractiveColors
        ?.takeIf { it.isNotBlank() }
        ?.split(',')
        ?.mapNotNull { it.trim().toLongOrNull() }
        ?.map { Color(it.toInt()) }
        ?: globalInteractiveColors.map { Color(it.toInt()) }
    val isDarkTheme = when (themeMode) {
        com.lucid.alarm.data.prefs.ThemeMode.DARK -> true
        com.lucid.alarm.data.prefs.ThemeMode.LIGHT -> false
        com.lucid.alarm.data.prefs.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val clockColorBaseLong = appearance?.customClockColor?.takeIf { it != 0L } ?: globalClockColor
    val clockColor = clockColorBaseLong.takeIf { it != 0L }?.let { Color(it.toInt()) } ?: Color.White
    val effectiveClockGradient =
        if (appearance != null) appearance.customClockGradient else globalClockGradient
    val clockColorEndLong = appearance?.customClockColorEnd?.takeIf { it != 0L } ?: globalClockColorEnd
    val clockColorEnd = clockColorEndLong.takeIf { it != 0L }?.let { Color(it.toInt()) } ?: clockColor
    val clockGradientDir = appearance?.customClockGradientDir?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { GradientDirection.valueOf(raw) }.getOrNull() }
        ?: globalClockGradientDir
    val effectiveOffsetX = if (appearance != null && (appearance.customClockOffsetX != 0f || appearance.customClockOffsetY != 0f)) appearance.customClockOffsetX else savedOffsetX
    val effectiveOffsetY = if (appearance != null && (appearance.customClockOffsetX != 0f || appearance.customClockOffsetY != 0f)) appearance.customClockOffsetY else savedOffsetY
    val effectiveBoxWidth = appearance?.customBoxWidth?.takeIf { it > 0f } ?: boxWidth
    val effectiveBoxHeight = appearance?.customBoxHeight?.takeIf { it > 0f } ?: boxHeight
    val effectiveClockStyle = appearance?.customClockStyle
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockStyle.valueOf(raw) }.getOrNull() }
        ?: clockStyle
    val effectiveClockFont = appearance?.customClockFont
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: clockFont
    val effectiveClockFontPath = appearance?.customClockFontPath?.takeIf { it.isNotBlank() }
        ?: clockCustomFontPath
    val effectiveDateFont = appearance?.customDateFont
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { ClockDateFont.valueOf(raw) }.getOrNull() }
        ?: dateFont
    val effectiveDateFontPath = appearance?.customDateFontPath?.takeIf { it.isNotBlank() }
        ?: dateCustomFontPath
    val effectiveDateVisible = if (appearance != null) appearance.customDateVisible else dateVisible
    val effectiveDateOffsetX = if (appearance != null && (appearance.customDateOffsetX != 0f || appearance.customDateOffsetY != 0f)) appearance.customDateOffsetX else dateOffsetX
    val effectiveDateOffsetY = if (appearance != null && (appearance.customDateOffsetX != 0f || appearance.customDateOffsetY != 0f)) appearance.customDateOffsetY else dateOffsetY
    val effectiveDateScale = appearance?.customDateScale?.takeIf { it > 0f && it != 1f } ?: dateScale
    val effectiveRemoveLeadingZero = removeLeadingZero
    val effectiveBlur = appearance?.customBlur?.takeIf { it >= 0f } ?: ringingBlur
    val effectiveDim = appearance?.customDim?.takeIf { it >= 0f } ?: ringingDim
    val dateColorBaseLong = appearance?.customDateColor?.takeIf { it != 0L } ?: globalDateColor
    val dateColorBase = dateColorBaseLong.takeIf { it != 0L }?.let { Color(it.toInt()) } ?: Color.White
    val effectiveDateGradient =
        if (appearance != null) appearance.customDateGradient else globalDateGradient
    val dateColorEndLong = appearance?.customDateColorEnd?.takeIf { it != 0L } ?: globalDateColorEnd
    val dateColorEnd = dateColorEndLong.takeIf { it != 0L }?.let { Color(it.toInt()) } ?: dateColorBase
    val dateGradientDir = appearance?.customDateGradientDir?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { GradientDirection.valueOf(raw) }.getOrNull() }
        ?: globalDateGradientDir
    val effectiveClockBrush = if (effectiveClockGradient) {
        clockTextBrush(clockColor, true, clockColorEnd, clockGradientDir)
    } else {
        null
    }
    val effectiveDateBrush = if (effectiveDateGradient) {
        clockTextBrush(dateColorBase, true, dateColorEnd, dateGradientDir)
    } else {
        null
    }

    LaunchedEffect(alarmEntity) {
        val perAlarm = alarmEntity?.snoozeMinutes ?: 0
        if (perAlarm > 0) snoozeMinutes = perAlarm
    }

    val snoozeExhausted = snoozeLimit >= 0 && snoozeCount >= snoozeLimit

    var nowCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowCalendar = Calendar.getInstance()
            delay(1_000L)
        }
    }

    val fontFamily = remember(effectiveClockFont, effectiveClockFontPath) {
        resolveRingingFontFamily(effectiveClockFont, effectiveClockFontPath)
    }
    val dateFontFamily = remember(
        effectiveDateFont,
        effectiveClockFont,
        effectiveClockFontPath,
        effectiveDateFontPath
    ) {
        resolveDateFontFamily(
            effectiveDateFont,
            effectiveClockFont,
            effectiveClockFontPath,
            effectiveDateFontPath
        )
    }
    val is24Hour = timeFormat.is24Hour
    val hasBox = effectiveBoxWidth > 0f && effectiveBoxHeight > 0f

    val effShowAmPm = appearance?.customShowAmPm ?: showAmPm
    val amPmFontEnum = appearance?.customAmPmFont
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: amPmFont
    val amPmFontPath = appearance?.customAmPmFontPath?.takeIf { it.isNotBlank() }
        ?: amPmCustomFontPath
    val amPmColorLong = appearance?.customAmPmColor?.takeIf { it != 0L } ?: amPmColor
    val amPmFontFamily = resolveRingingFontFamily(amPmFontEnum, amPmFontPath)
    val amPmBrush = if (amPmColorLong != 0L) SolidColor(Color(amPmColorLong.toInt())) else null

    val effectiveLabelVisible = appearance?.customAlarmLabelVisible ?: alarmLabelVisible
    val labelFontEnum = appearance?.customAlarmLabelFont
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: alarmLabelFont
    val labelFontPath = appearance?.customAlarmLabelFontPath?.takeIf { it.isNotBlank() }
        ?: alarmLabelCustomFontPath
    val labelColorLong = appearance?.customAlarmLabelColor?.takeIf { it != 0L } ?: alarmLabelColor
    val labelFontFamily = resolveRingingFontFamily(labelFontEnum, labelFontPath)
    val labelColor = if (labelColorLong != 0L) {
        Color(labelColorLong.toInt())
    } else {
        Color.White.copy(alpha = 0.75f)
    }

    val snoozeFontEnum = appearance?.customSnoozeFont
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: snoozeFont
    val snoozeFontPath = appearance?.customSnoozeFontPath?.takeIf { it.isNotBlank() }
        ?: snoozeCustomFontPath
    val snoozeContainerColorLong =
        appearance?.customSnoozeContainerColor?.takeIf { it != 0L } ?: snoozeContainerColor
    val snoozeTextColorLong =
        appearance?.customSnoozeTextColor?.takeIf { it != 0L } ?: snoozeTextColor
    val snoozeTypeface = resolveRingingFontFamily(snoozeFontEnum, snoozeFontPath)

    val dismissFontEnum = appearance?.customDismissFont
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> runCatching { RingingClockFont.valueOf(raw) }.getOrNull() }
        ?: dismissFont
    val dismissFontPath = appearance?.customDismissFontPath?.takeIf { it.isNotBlank() }
        ?: dismissCustomFontPath
    val dismissContainerColorLong =
        appearance?.customDismissContainerColor?.takeIf { it != 0L } ?: dismissContainerColor
    val dismissTextColorLong =
        appearance?.customDismissTextColor?.takeIf { it != 0L } ?: dismissTextColor
    val dismissTypeface = resolveRingingFontFamily(dismissFontEnum, dismissFontPath)
    val solidColorLong = appearance?.customBackgroundSolidColor?.takeIf { it != 0L }
        ?: globalSolidColor
    val solidColor = solidColorLong.takeIf { it != 0L }?.let { Color(it) }

    var clockH by remember { mutableFloatStateOf(0f) }
    var dateH by remember { mutableFloatStateOf(0f) }
    val dateGapPx = with(LocalDensity.current) { 14.dp.toPx() }

    AlarmLockBackground(
        style = effectiveBackgroundStyle,
        imageUri = effectiveImageUri,
        videoUri = effectiveVideoUri,
        blur = effectiveBlur.dp,
        dim = effectiveDim,
        solidColor = solidColor,
        interactiveColors = effectiveInteractiveColors,
        dark = isDarkTheme,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (effectiveLabelVisible) {
                Text(
                    text = "Alarm",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LocalAppFont.current,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 4.sp
                    ),
                    color = clockColor,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(effectiveOffsetX.roundToInt(), effectiveOffsetY.roundToInt())
                        }
                        .wrapContentSize(Alignment.Center, unbounded = true)
                        .onSizeChanged { clockH = it.height.toFloat() },
                    contentAlignment = Alignment.Center
                ) {
                    AlarmClockBox(
                        calendar = nowCalendar,
                        style = effectiveClockStyle,
                        fontFamily = fontFamily,
                        is24Hour = is24Hour,
                        showAmPm = effShowAmPm,
                        amPmFontFamily = LocalAppFont.current,
                        amPmBrush = null,
                        contentColor = clockColor,
                        textBrush = effectiveClockBrush,
                        removeLeadingZero = effectiveRemoveLeadingZero,
                        fontSize = (clockFontSize * clockScale).sp,
                        autoFit = false,
                        showContainerBorder = false,
                        clockFont = effectiveClockFont,
                        customFontPath = effectiveClockFontPath,
                        clockScale = clockScale,
                        modifier = Modifier.wrapContentSize(Alignment.Center, unbounded = true)
                    )
                }

                if (effectiveDateVisible) {
                    ClockDateText(
                        calendar = nowCalendar,
                        fontFamily = dateFontFamily,
                        fontSize = (18f * effectiveDateScale).sp,
                        contentColor = dateColorBase,
                        textBrush = effectiveDateBrush,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset {
                                IntOffset(
                                    effectiveDateOffsetX.roundToInt(),
                                    (clockH / 2f + dateGapPx + dateH / 2f + effectiveDateOffsetY)
                                        .roundToInt()
                                )
                            }
                            .wrapContentSize(Alignment.Center, unbounded = true)
                            .onSizeChanged { dateH = it.height.toFloat() }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                if (alarmLabel.isNotBlank()) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = alarmLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                RingingActionPill(
                    text = "Dismiss",
                    onClick = onDismiss,
                    containerColor = if (dismissContainerColorLong != 0L) {
                        Color(dismissContainerColorLong.toInt())
                    } else {
                        AlarmLockPalette.DismissButton
                    },
                    contentColor = if (dismissTextColorLong != 0L) {
                        Color(dismissTextColorLong.toInt())
                    } else {
                        Color.White
                    },
                    fontFamily = dismissTypeface
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (snoozeExhausted) {
                    Text(
                        text = "Snooze limit reached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    SnoozePill(
                        snoozeMinutes = snoozeMinutes,
                        onSnooze = { onSnooze(snoozeMinutes) },
                        containerColor = if (snoozeContainerColorLong != 0L) {
                            Color(snoozeContainerColorLong.toInt())
                        } else {
                            Color.White.copy(alpha = 0.12f)
                        },
                        contentColor = if (snoozeTextColorLong != 0L) {
                            Color(snoozeTextColorLong.toInt())
                        } else {
                            Color.White
                        },
                        fontFamily = snoozeTypeface,
                        onMinus = {
                            val updated = (snoozeMinutes - 1).coerceAtLeast(1)
                            snoozeMinutes = updated
                            scope.launch { settingsDataStore.setRingingSnoozeMinutes(updated) }
                        },
                        onPlus = {
                            val updated = (snoozeMinutes + 1).coerceAtMost(30)
                            snoozeMinutes = updated
                            scope.launch { settingsDataStore.setRingingSnoozeMinutes(updated) }
                        }
                    )
                }
            }
        }
    }
}
