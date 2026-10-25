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
import com.lucid.alarm.receiver.AwakeCheckReceiver
import com.lucid.alarm.ui.theme.toColor
import com.lucid.alarm.service.AlarmAudioService
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.ui.ClockAppScaffold
import com.lucid.alarm.ui.theme.LucidAlarmTheme
import com.lucid.alarm.ui.viewmodel.AlarmViewModel
import com.lucid.alarm.ui.viewmodel.WorldClockViewModel

class MainActivity : ComponentActivity() {

    /**
     * Tab the ongoing Now Bar notification asked us to open, paired with a timestamp (null = none).
     *
     * The timestamp makes every tap a unique event: tapping "STOPWATCH" while the Stopwatch tab is
     * already the current page must still close Settings / the recycle bin and navigate back, which
     * a plain `State<String>` would swallow because its value would not change.
     *
     * Set both when the activity is created from the notification intent and when it is already
     * running and receives the intent through [onNewIntent].
     */
    private val navRequest = mutableStateOf<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readRequestedTab(intent)

        // Register notification channels before scheduling/posting anything.
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
        // Absolute edge-to-edge: let content draw under the system bars and keep both bars fully
        // transparent so no solid Color block sits behind the status-bar icons.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // enableEdgeToEdge() leaves nav-bar contrast enforcement on, which makes the system
            // paint its own translucent black block over the transparent navigation-bar area —
            // a black frame that pops in/out behind the bottom of the app. Opt out so the app
            // background stays uniform edge-to-edge.
            window.isNavigationBarContrastEnforced = false
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
                // POST_NOTIFICATIONS (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // SCHEDULE_EXACT_ALARM (Android 12+)
                if (!scheduler.canScheduleExactAlarms()) {
                    runCatching {
                        startActivity(scheduler.requestExactAlarmPermissionIntent())
                    }
                }

                // USE_FULL_SCREEN_INTENT is user-revocable on Android 14+. Without it, alarms
                // only show a heads-up notification instead of the lock-screen ring screen.
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

            // Resolve the visible top-of-screen background so the status/nav-bar icons always
            // contrast with whatever is actually drawn behind them (dark bg -> light icons and
            // vice-versa), regardless of the chosen theme mode.
            val topBackground = remember(
                backgroundPalette,
                backgroundCustomColor,
                darkTheme
            ) {
                val isCustom = backgroundPalette ==
                    com.lucid.alarm.data.prefs.BackgroundPalette.CUSTOM &&
                    backgroundCustomColor != 0L
                when {
                    // Light mode renders pure white unless a custom light background is set, in
                    // which case the exact raw Color is used (never washed toward white).
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

    /**
     * Opens the Timer or Stopwatch tab when the activity was launched by the Now Bar body tap.
     *
     * Every request is stamped with the current time so a repeated tap is still a new event. The
     * extra is consumed immediately, so a configuration change does not navigate away from the tab
     * the user has moved to afterwards.
     */
    private fun readRequestedTab(intent: Intent?) {
        if (intent == null) return
        val tab = intent.getStringExtra(NowBarNotification.EXTRA_INITIAL_TAB) ?: return
        navRequest.value = tab to System.currentTimeMillis()
        intent.removeExtra(NowBarNotification.EXTRA_INITIAL_TAB)
    }

    private fun registerNotificationChannels() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val firingAlarmChannel = NotificationChannel(
            AlarmAudioService.ALARM_CHANNEL_ID,
            "Firing Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority alarm notifications and lock screen wakeups"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            setSound(null, null)
        }

        val upcomingAlarmChannel = NotificationChannel(
            AlarmReceiver.UPCOMING_CHANNEL_ID,
            "Upcoming Alarms",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Heads-up notification before scheduled alarms fire"
            setShowBadge(false)
        }

        val awakeCheckChannel = NotificationChannel(
            AwakeCheckReceiver.CHANNEL_ID,
            "Awake Check",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Post-dismiss check confirming you are awake"
            setBypassDnd(true)
        }

        notificationManager.createNotificationChannels(
            listOf(firingAlarmChannel, upcomingAlarmChannel, awakeCheckChannel)
        )
    }
}
