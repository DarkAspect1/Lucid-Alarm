package com.lucid.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lucid.alarm.R
import com.lucid.alarm.ui.components.FrostedCardSurface
import com.lucid.alarm.ui.theme.onColorFor
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.receiver.AlarmReceiver
import com.lucid.alarm.receiver.AlarmScheduler
import com.lucid.alarm.ui.theme.toColor
import com.lucid.alarm.service.AlarmAudioService
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.ui.ClockAppScaffold
import com.lucid.alarm.ui.theme.LucidAlarmTheme
import com.lucid.alarm.ui.viewmodel.AlarmViewModel
import com.lucid.alarm.ui.viewmodel.WorldClockViewModel

class MainActivity : ComponentActivity() {

    private val navRequest = mutableStateOf<Pair<String, Long>?>(null)

    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.content.res.Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale
        }
    }

    private fun resolveTargetLocale(langCode: String): Locale {
        if (langCode.isBlank() || langCode.equals("system", ignoreCase = true)) {
            return getSystemLocale()
        }
        return Locale.forLanguageTag(langCode)
    }

    override fun attachBaseContext(newBase: Context) {
        val settings = SettingsDataStore(newBase)
        val lang = runBlocking { settings.appLanguageFlow.first() }
        val targetLocale = resolveTargetLocale(lang)
        Locale.setDefault(targetLocale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        val contextToAttach = newBase.createConfigurationContext(config)
        super.attachBaseContext(contextToAttach)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readRequestedTab(intent)

        registerNotificationChannels()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

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

        val settingsDataStore = SettingsDataStore(applicationContext)
        val lang = runBlocking { settingsDataStore.appLanguageFlow.first() }
        val targetLocale = resolveTargetLocale(lang)
        Locale.setDefault(targetLocale)
        val config = Configuration(resources.configuration)
        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        val isRtl = android.text.TextUtils.getLayoutDirectionFromLocale(targetLocale) == android.view.View.LAYOUT_DIRECTION_RTL
        window.decorView.layoutDirection = if (isRtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR

        val scheduler = AlarmScheduler(applicationContext)

        setContent {
            val alarmViewModel: AlarmViewModel = viewModel()
            val worldClockViewModel: WorldClockViewModel = viewModel()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { }
            )

            LaunchedEffect(Unit) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (!scheduler.canScheduleExactAlarms()) {
                    runCatching {
                        startActivity(scheduler.requestExactAlarmPermissionIntent())
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    !scheduler.canUseFullScreenIntent()
                ) {
                    runCatching {
                        scheduler.requestFullScreenIntentPermissionIntent()
                            ?.let { startActivity(it) }
                    }
                }
            }

            val appFontStyle by settingsDataStore.appFontStyleFlow.collectAsState(
                initial = com.lucid.alarm.data.prefs.AppFontStyle.SYSTEM_DEFAULT
            )
            val accentColor by settingsDataStore.accentColorFlow.collectAsState(
                initial = com.lucid.alarm.data.prefs.AccentColor.DEFAULT
            )
            val backgroundPalette by settingsDataStore.backgroundPaletteFlow.collectAsState(
                initial = com.lucid.alarm.data.prefs.BackgroundPalette.OBSIDIAN
            )
            val backgroundCustomColor by settingsDataStore.backgroundCustomColorFlow.collectAsState(
                initial = 0L
            )
            val uiScale by settingsDataStore.uiScaleFlow.collectAsState(
                initial = com.lucid.alarm.data.prefs.SettingsDataStore.DEFAULT_UI_SCALE
            )
            val themeMode by settingsDataStore.themeModeFlow.collectAsState(
                initial = com.lucid.alarm.data.prefs.ThemeMode.SYSTEM
            )
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                com.lucid.alarm.data.prefs.ThemeMode.DARK -> true
                com.lucid.alarm.data.prefs.ThemeMode.LIGHT -> false
                com.lucid.alarm.data.prefs.ThemeMode.SYSTEM -> systemDark
            }

            val topBackground = remember(
                backgroundPalette,
                backgroundCustomColor,
                darkTheme
            ) {
                val isCustom = backgroundPalette ==
                    com.lucid.alarm.data.prefs.BackgroundPalette.CUSTOM &&
                    backgroundCustomColor != 0L
                when {

                    !darkTheme && !isCustom -> androidx.compose.ui.graphics.Color(0xFFFFFFFF)
                    !darkTheme -> androidx.compose.ui.graphics.Color(backgroundCustomColor)
                    isCustom -> androidx.compose.ui.graphics.Color(backgroundCustomColor)
                    else -> backgroundPalette.toColor()
                }
            }
            val topIsLight = topBackground.luminance() > 0.5f

            LaunchedEffect(topIsLight) {
                val window = this@MainActivity.window
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = topIsLight
                    isAppearanceLightNavigationBars = topIsLight
                }
            }

            val appLanguage by settingsDataStore.appLanguageFlow.collectAsState(initial = "")
            val activeLocale = remember(appLanguage) { resolveTargetLocale(appLanguage) }
            val isRtl = remember(activeLocale) {
                android.text.TextUtils.getLayoutDirectionFromLocale(activeLocale) == android.view.View.LAYOUT_DIRECTION_RTL
            }
            val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

            LaunchedEffect(isRtl) {
                window.decorView.layoutDirection = if (isRtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR
            }

            val packageInfo = remember {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, 0)
                    }
                }.getOrNull()
            }
            val currentVersionCode = remember(packageInfo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: 5L
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toLong() ?: 5L
                }
            }

            val lastPromptedVersionCode by settingsDataStore.lastBatteryPromptedVersionCodeFlow.collectAsState(initial = -1L)
            val powerManager = remember { getSystemService(Context.POWER_SERVICE) as? PowerManager }
            val isBatteryExempted = powerManager?.isIgnoringBatteryOptimizations(packageName) == true
            var showBatteryDialog by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(lastPromptedVersionCode, isBatteryExempted, currentVersionCode) {
                if (lastPromptedVersionCode != -1L && !isBatteryExempted) {
                    if (lastPromptedVersionCode < currentVersionCode) {
                        showBatteryDialog = true
                    }
                }
            }

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalDensity provides Density(
                    density = baseDensity.density * uiScale,
                    fontScale = baseDensity.fontScale * uiScale
                )
            ) {
                LucidAlarmTheme(
                    accentColor = accentColor,
                    backgroundPalette = backgroundPalette,
                    appFontStyle = appFontStyle,
                    dark = darkTheme,
                    settingsDataStore = settingsDataStore
                ) {
                    ClockAppScaffold(
                        alarmViewModel = alarmViewModel,
                        worldClockViewModel = worldClockViewModel,
                        settingsDataStore = settingsDataStore,
                        navRequest = navRequest.value,
                        onNavRequestHandled = { navRequest.value = null }
                    )

                    if (showBatteryDialog) {
                        Dialog(onDismissRequest = {
                            showBatteryDialog = false
                            scope.launch { settingsDataStore.setLastBatteryPromptedVersionCode(currentVersionCode) }
                        }) {
                            FrostedCardSurface(
                                modifier = Modifier.widthIn(min = 280.dp, max = 340.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.dialog_battery_optimization_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = stringResource(R.string.badge_recommended),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = onColorFor(MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.dialog_battery_optimization_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = {
                                            showBatteryDialog = false
                                            scope.launch { settingsDataStore.setLastBatteryPromptedVersionCode(currentVersionCode) }
                                        }) {
                                            Text(stringResource(R.string.action_cancel))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                showBatteryDialog = false
                                                scope.launch { settingsDataStore.setLastBatteryPromptedVersionCode(currentVersionCode) }
                                                runCatching {
                                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                        data = Uri.parse("package:$packageName")
                                                    }
                                                    startActivity(intent)
                                                }.onFailure {
                                                    runCatching {
                                                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Text(stringResource(R.string.action_allow))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readRequestedTab(intent)
    }

    private fun readRequestedTab(intent: Intent?) {
        if (intent == null) return
        val tab = intent.getStringExtra(NowBarNotification.EXTRA_INITIAL_TAB) ?: return
        navRequest.value = tab to System.currentTimeMillis()
        intent.removeExtra(NowBarNotification.EXTRA_INITIAL_TAB)
    }

    private fun registerNotificationChannels() {
        AlarmAudioService.initSilentAlarmChannels(this)
    }
}
