package com.lucid.alarm.ui

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import com.lucid.alarm.util.findActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Timer
import com.lucid.alarm.ui.theme.OneUiAppBackground
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lucid.alarm.R
import com.lucid.alarm.data.prefs.AppBackgroundStyle
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.service.NowBarNotification
import com.lucid.alarm.service.TimerStopwatchManager
import com.lucid.alarm.service.TimerService
import com.lucid.alarm.ui.screens.AlarmCustomizationScreen
import com.lucid.alarm.ui.screens.AppThemeScreen
import com.lucid.alarm.ui.screens.MultiTimerScreen
import com.lucid.alarm.ui.screens.RecycleBinScreen
import com.lucid.alarm.ui.screens.SettingsScreen
import com.lucid.alarm.ui.screens.StopwatchScreen
import com.lucid.alarm.ui.screens.TimerRecycleBinScreen
import com.lucid.alarm.ui.screens.TimerScreen
import com.lucid.alarm.ui.screens.WorldClockRecycleBinScreen
import com.lucid.alarm.ui.screens.WorldClockScreen
import com.lucid.alarm.ui.theme.BoxColors
import com.lucid.alarm.ui.theme.LocalBoxColors
import com.lucid.alarm.ui.viewmodel.AlarmViewModel
import com.lucid.alarm.ui.viewmodel.TimerViewModel
import com.lucid.alarm.ui.viewmodel.WorldClockViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ClockTab(
    val titleRes: Int,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    ALARMS(R.string.tab_alarms, Icons.Rounded.Alarm, Icons.Rounded.Alarm),
    WORLD_CLOCK(R.string.tab_world_clock, Icons.Rounded.Public, Icons.Rounded.Public),
    STOPWATCH(R.string.tab_stopwatch, Icons.Rounded.Timer, Icons.Rounded.Timer),
    TIMER(R.string.tab_timer, Icons.Rounded.HourglassEmpty, Icons.Rounded.HourglassEmpty)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockAppScaffold(
    alarmViewModel: AlarmViewModel,
    worldClockViewModel: WorldClockViewModel,
    settingsDataStore: SettingsDataStore,
    timerViewModel: TimerViewModel = viewModel(),
    navRequest: Pair<String, Long>? = null,
    onNavRequestHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showRecycleBin by rememberSaveable { mutableStateOf(false) }
    var showWorldClockRecycleBin by rememberSaveable { mutableStateOf(false) }
    var showTimerRecycleBin by rememberSaveable { mutableStateOf(false) }
    var showMultiTimer by rememberSaveable { mutableStateOf(false) }
    var settingsSubScreen by rememberSaveable { mutableStateOf<SettingsSubScreen>(SettingsSubScreen.MAIN) }

    var alarmsSelectionActive by remember { mutableStateOf(false) }

    var worldClockSelectionActive by remember { mutableStateOf(false) }
    var timerSelectionActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    val removeLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(initial = true)
    val navBarColor by settingsDataStore.navBarColorFlow.collectAsState(initial = 0L)
    val settingsCardColor by settingsDataStore.settingsCardColorFlow.collectAsState(initial = 0L)
    val alarmCardColor by settingsDataStore.alarmCardColorFlow.collectAsState(initial = 0L)
    val worldClockCardColor by settingsDataStore.worldClockCardColorFlow.collectAsState(initial = 0L)
    val appBackgroundStyle by settingsDataStore.appBackgroundStyleFlow.collectAsState(
        initial = AppBackgroundStyle.PALETTE
    )
    val backgroundGradientColors by settingsDataStore.backgroundGradientColorsFlow.collectAsState(
        initial = emptyList()
    )
    val backgroundInteractiveColors by
        settingsDataStore.backgroundInteractiveColorsFlow.collectAsState(initial = emptyList())

    val pagerState = rememberPagerState(pageCount = { ClockTab.entries.size })
    val currentTab = ClockTab.entries[pagerState.currentPage]

    LaunchedEffect(navRequest) {
        val request = navRequest ?: return@LaunchedEffect
        val target = when (request.first) {
            NowBarNotification.INITIAL_TAB_TIMER -> ClockTab.TIMER
            NowBarNotification.INITIAL_TAB_STOPWATCH -> ClockTab.STOPWATCH
            else -> null
        }
        target ?: return@LaunchedEffect
        showSettings = false
        showRecycleBin = false
        showWorldClockRecycleBin = false
        showTimerRecycleBin = false
        showMultiTimer = false
        settingsSubScreen = SettingsSubScreen.MAIN
        pagerState.animateScrollToPage(target.ordinal)
        onNavRequestHandled()
    }

    val timerState by TimerStopwatchManager.timer.collectAsState()
    val stopwatchState by TimerStopwatchManager.stopwatch.collectAsState()

    var alarmsCanScroll by remember { mutableStateOf(false) }
    var worldClockCanScroll by remember { mutableStateOf(false) }
    var alarmsScrolling by remember { mutableStateOf(false) }
    var worldClockScrolling by remember { mutableStateOf(false) }

    val currentTabCanScroll = when (currentTab) {
        ClockTab.ALARMS -> alarmsCanScroll
        ClockTab.WORLD_CLOCK -> worldClockCanScroll
        ClockTab.TIMER -> false
        ClockTab.STOPWATCH -> false
    }
    val isCurrentTabScrolling = when (currentTab) {
        ClockTab.ALARMS -> alarmsScrolling
        ClockTab.WORLD_CLOCK -> worldClockScrolling
        ClockTab.TIMER -> false
        ClockTab.STOPWATCH -> false
    }

    val barsVisibleState = remember { mutableStateOf(true) }
    val scrollAccumulator = remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val hideThreshold = remember(density) { with(density) { 48.dp.toPx() } }
    var reShowJob by remember { mutableStateOf<Job?>(null) }

    val barsConnection = remember(currentTabCanScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!currentTabCanScroll) {
                    barsVisibleState.value = true
                    return Offset.Zero
                }
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    val next = (scrollAccumulator.floatValue + delta)
                        .coerceIn(-hideThreshold, hideThreshold)
                    scrollAccumulator.floatValue = next
                    if (next <= -hideThreshold) {
                        barsVisibleState.value = false
                    } else if (next >= hideThreshold) {
                        barsVisibleState.value = true
                    }
                }

                reShowJob?.cancel()
                reShowJob = scope.launch {
                    delay(400L)
                    barsVisibleState.value = true
                    scrollAccumulator.floatValue = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                reShowJob?.cancel()
                barsVisibleState.value = true
                scrollAccumulator.floatValue = 0f
                return Velocity.Zero
            }
        }
    }

    val resolvedBarsVisible = if (currentTabCanScroll) barsVisibleState.value else true

    LaunchedEffect(isCurrentTabScrolling, currentTabCanScroll) {
        if (!isCurrentTabScrolling || !currentTabCanScroll) {
            reShowJob?.cancel()
            barsVisibleState.value = true
            scrollAccumulator.floatValue = 0f
        }
    }

    LaunchedEffect(currentTab) {
        reShowJob?.cancel()
        barsVisibleState.value = true
        scrollAccumulator.floatValue = 0f
    }

    var lastBackPressMillis by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        when {
            showSettings && settingsSubScreen != SettingsSubScreen.MAIN -> {
                settingsSubScreen = SettingsSubScreen.MAIN
            }
            showSettings -> {
                showSettings = false
            }
            showTimerRecycleBin -> showTimerRecycleBin = false
            showWorldClockRecycleBin -> showWorldClockRecycleBin = false
            showRecycleBin -> showRecycleBin = false
            showMultiTimer -> showMultiTimer = false
            pagerState.currentPage != 0 -> {
                scope.launch { pagerState.animateScrollToPage(0) }
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressMillis <= 2000L) {
                    context.findActivity()?.finish()
                } else {
                    lastBackPressMillis = now
                    Toast.makeText(context, context.getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val showingTabs = !showSettings && !showRecycleBin && !showWorldClockRecycleBin && !showTimerRecycleBin && !showMultiTimer

    val boxColors = remember(settingsCardColor, alarmCardColor, worldClockCardColor) {
        BoxColors(
            settingsCard = settingsCardColor.takeIf { it != 0L }?.let { Color(it) },
            alarmCard = alarmCardColor.takeIf { it != 0L }?.let { Color(it) },
            worldClockCard = worldClockCardColor.takeIf { it != 0L }?.let { Color(it) }
        )
    }

    CompositionLocalProvider(
        LocalBoxColors provides boxColors
    ) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
        ) {
            val isDark = MaterialTheme.colorScheme.background.luminance() <= 0.5f
            val accent = MaterialTheme.colorScheme.primary
            val isSwitchingTabs = pagerState.isScrollInProgress || abs(pagerState.currentPageOffsetFraction) > 0.01f
            OneUiAppBackground(dark = isDark, accentColor = accent, modifier = Modifier.fillMaxSize())
            when {
                showTimerRecycleBin -> TimerRecycleBinScreen(
                    viewModel = timerViewModel,
                    onBack = { showTimerRecycleBin = false }
                )
                showWorldClockRecycleBin -> WorldClockRecycleBinScreen(
                    viewModel = worldClockViewModel,
                    is24Hour = timeFormat.is24Hour,
                    onBack = { showWorldClockRecycleBin = false }
                )
                showRecycleBin -> RecycleBinScreen(
                    viewModel = alarmViewModel,
                    is24Hour = timeFormat.is24Hour,
                    removeLeadingZero = removeLeadingZero,
                    onBack = { showRecycleBin = false }
                )
                showSettings -> AnimatedContent(
                    targetState = settingsSubScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(150))
                    },
                    label = "SettingsNavigation"
                ) { subScreen ->
                    when (subScreen) {
                        SettingsSubScreen.MAIN -> SettingsScreen(
                            settingsDataStore = settingsDataStore,
                            onNavigateBack = { showSettings = false },
                            onNavigateToAppearance = { settingsSubScreen = SettingsSubScreen.APPEARANCE },
                            onNavigateToCustomization = { settingsSubScreen = SettingsSubScreen.CUSTOMIZATION },
                            onNavigateToRecycleBin = { settingsSubScreen = SettingsSubScreen.RECYCLE_BIN },
                            onAlarmSettingsChanged = { alarmViewModel.rescheduleAllAlarms() }
                        )
                        SettingsSubScreen.APPEARANCE -> AppThemeScreen(
                            settingsDataStore = settingsDataStore,
                            onNavigateBack = { settingsSubScreen = SettingsSubScreen.MAIN }
                        )
                        SettingsSubScreen.CUSTOMIZATION -> AlarmCustomizationScreen(
                            settingsDataStore = settingsDataStore,
                            onDone = { settingsSubScreen = SettingsSubScreen.MAIN }
                        )
                        SettingsSubScreen.RECYCLE_BIN -> RecycleBinScreen(
                            viewModel = alarmViewModel,
                            is24Hour = timeFormat.is24Hour,
                            removeLeadingZero = removeLeadingZero,
                            onBack = { settingsSubScreen = SettingsSubScreen.MAIN }
                        )
                    }
                }
                showMultiTimer -> MultiTimerScreen(
                    timerViewModel = timerViewModel,
                    removeLeadingZero = removeLeadingZero,
                    onBack = { showMultiTimer = false },
                    onOpenSettings = {
                        showMultiTimer = false
                        showSettings = true
                        settingsSubScreen = SettingsSubScreen.MAIN
                    },
                    onOpenRecycleBin = {
                        showMultiTimer = false
                        showTimerRecycleBin = true
                    },
                    onOpenTimer = { timer ->
                        showMultiTimer = false
                        val now = System.currentTimeMillis()
                        val rem = if (timer.remainingMillis > 0L) timer.remainingMillis else timer.durationMillis
                        val sec = ((rem + 999L) / 1000L).coerceIn(0L, timer.durationMillis / 1000L).toInt()
                        val tot = (timer.durationMillis / 1000L).toInt()
                        if (!timer.isRunning) {
                            timerViewModel.startTimerCard(timer)
                        } else {
                            val liveRem = (timer.endTimestamp - now).coerceIn(0L, timer.durationMillis)
                            val liveSec = ((liveRem + 999L) / 1000L).coerceIn(0L, timer.durationMillis / 1000L).toInt()
                            val endRt = SystemClock.elapsedRealtime() + liveRem
                            TimerStopwatchManager.setTimer(
                                running = true,
                                totalSeconds = tot,
                                secondsLeft = liveSec,
                                endRealtime = endRt,
                                timerId = timer.id,
                                label = timer.label
                            )
                            TimerService.focusTimer(context, timer.id, timer.label, liveSec, tot, liveRem, endRt)
                        }
                    }
                )
                else -> HorizontalPager(
                    state = pagerState,

                    beyondViewportPageCount = 1,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(barsConnection)
                ) { page ->
                    when (ClockTab.entries[page]) {
                        ClockTab.ALARMS -> MainAlarmsScreen(
                            viewModel = alarmViewModel,
                            settingsDataStore = settingsDataStore,
                            barsVisible = resolvedBarsVisible,
                            onSelectionModeChange = { alarmsSelectionActive = it },
                            onOpenSettings = { showSettings = true },
                            onOpenRecycleBin = { showRecycleBin = true },
                            onCanScrollChange = { alarmsCanScroll = it },
                            onScrollInProgressChange = { alarmsScrolling = it },
                            dismissTransientUi = navRequest?.second,
                            fabInteractive = true
                        )
                        ClockTab.TIMER -> TimerScreen(
                            settingsDataStore = settingsDataStore,
                            barsVisible = resolvedBarsVisible,
                            onOpenSettings = { showSettings = true },
                            onOpenRecycleBin = { showTimerRecycleBin = true },
                            onOpenMultiTimer = { showMultiTimer = true },
                            timerViewModel = timerViewModel,
                            timerState = timerState
                        )
                        ClockTab.STOPWATCH -> StopwatchScreen(
                            barsVisible = true,
                            onOpenSettings = { showSettings = true },
                            stopwatchState = stopwatchState
                        )
                        ClockTab.WORLD_CLOCK -> WorldClockScreen(
                            worldClockViewModel = worldClockViewModel,
                            settingsDataStore = settingsDataStore,
                            barsVisible = resolvedBarsVisible,
                            onSelectionModeChange = { worldClockSelectionActive = it },
                            onOpenSettings = { showSettings = true },
                            onOpenRecycleBin = { showWorldClockRecycleBin = true },
                            onCanScrollChange = { worldClockCanScroll = it },
                            onScrollInProgressChange = { worldClockScrolling = it },
                            fabInteractive = true
                        )
                    }
                }
            }

            if (showingTabs) {

                val selectionHidesBar =
                    (currentTab == ClockTab.ALARMS && alarmsSelectionActive) ||
                        (currentTab == ClockTab.WORLD_CLOCK && worldClockSelectionActive) ||
                        (currentTab == ClockTab.TIMER && timerSelectionActive)
                AnimatedVisibility(
                    visible = resolvedBarsVisible && !selectionHidesBar,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(180)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(2f)
                ) {
                    FloatingPillBottomBar(
                        pagerState = pagerState,
                        interactive = true,
                        onTabSelected = { tab ->
                            scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                        },
                        containerColor = if (navBarColor != 0L) Color(navBarColor) else null
                    )
                }
            }

            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val statusBarBgColor = MaterialTheme.colorScheme.background
            val hideStatusBarShadow = showSettings && settingsSubScreen == SettingsSubScreen.CUSTOMIZATION
            if (!hideStatusBarShadow) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarTop + 32.dp)
                        .align(Alignment.TopCenter)
                        .zIndex(4f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    statusBarBgColor.copy(alpha = 0.88f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
    }
}

enum class SettingsSubScreen {
    MAIN,
    APPEARANCE,
    CUSTOMIZATION,
    RECYCLE_BIN
}

private val TabBarHeight = 56.dp

private val NavBarPillWidth = 248.dp

@Composable
fun FloatingPillBottomBar(
    pagerState: PagerState,
    onTabSelected: (ClockTab) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    interactive: Boolean = true
) {
    val tabs = ClockTab.entries
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val resolvedContainer = if (containerColor != null && containerColor != Color(0xFF181920) && containerColor != Color(0xFF2D2D2E)) {
        containerColor
    } else if (isLight) {
        Color(0xFFFFFFFF)
    } else {
        Color(0xFF2C2D35)
    }
    val indicatorColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val activeIconColor = MaterialTheme.colorScheme.onPrimary
    val inactiveIconColor = if (isLight) Color(0xFF1E1E1E).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.65f)

    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var lastTooltipIndex by remember { mutableIntStateOf(0) }
    var tooltipTitle by remember { mutableStateOf("") }

    LaunchedEffect(tooltipIndex) {
        if (tooltipIndex != null) {
            delay(2000L)
            tooltipIndex = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {

        AnimatedVisibility(
            visible = tooltipIndex != null,
            enter = fadeIn(animationSpec = tween(120)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(10f)
                .offset(
                    x = NavBarPillWidth * (lastTooltipIndex + 0.5f) / tabs.size - NavBarPillWidth / 2,
                    y = (-72).dp
                )
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = tooltipTitle,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .width(NavBarPillWidth)
                .height(TabBarHeight),
            shape = RoundedCornerShape(50),
            color = resolvedContainer,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val innerCellWidth = maxWidth / tabs.size
                val innerCellPx = with(LocalDensity.current) { innerCellWidth.toPx() }
                val indicatorPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction

                Box(
                    modifier = Modifier
                        .offset(x = innerCellWidth * indicatorPosition)
                        .width(innerCellWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(indicatorColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(innerCellPx) {
                            detectHorizontalDragGestures { change, _ ->
                                val page = (change.position.x / innerCellPx)
                                    .roundToInt()
                                    .coerceIn(0, tabs.size - 1)
                                if (pagerState.currentPage != page) {
                                    scope.launch { pagerState.scrollToPage(page) }
                                }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val active = abs(indicatorPosition - index) < 0.5f
                        val contentColor = if (active) activeIconColor else inactiveIconColor
                        val iconVector = if (active) tab.activeIcon else tab.inactiveIcon

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .pointerInput(tab) {
                                    detectTapGestures(
                                        onTap = { onTabSelected(tab) },
                                        onLongPress = {
                                            tooltipIndex = index
                                            lastTooltipIndex = index
                                            tooltipTitle = context.getString(tab.titleRes)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = stringResource(tab.titleRes),
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
