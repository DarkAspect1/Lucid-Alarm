package com.lucid.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
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

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
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
