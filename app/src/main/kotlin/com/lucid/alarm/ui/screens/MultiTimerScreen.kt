package com.lucid.alarm.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lucid.alarm.data.prefs.TimerVibrationPattern
import com.lucid.alarm.data.prefs.TimerVibrationPolicy
import com.lucid.alarm.ui.localizedName
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.R
import com.lucid.alarm.data.TimerEntity
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimerVolumePolicy
import com.lucid.alarm.ui.components.AppDropdownMenu
import com.lucid.alarm.ui.components.AppMenuItem
import com.lucid.alarm.ui.components.FrostedBlurBox
import com.lucid.alarm.ui.components.SelectionBarContainer
import com.lucid.alarm.ui.components.SelectionCheckbox
import com.lucid.alarm.ui.components.SelectionIconButton
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.theme.onColorFor
import com.lucid.alarm.ui.viewmodel.TimerViewModel
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MultiTimerScreen(
    timerViewModel: TimerViewModel,
    onBack: () -> Unit,
    onOpenTimer: (TimerEntity) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenRecycleBin: () -> Unit = {},
    removeLeadingZero: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val accent = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val settingsDataStore = remember { SettingsDataStore(context) }
    val timerVolumePolicy by settingsDataStore.timerVolumePolicyFlow.collectAsState(initial = TimerVolumePolicy.UNIFIED)
    val timerVibrationPolicy by settingsDataStore.timerVibrationPolicyFlow.collectAsState(initial = TimerVibrationPolicy.UNIFIED)

    val activeTimers by timerViewModel.activeTimers.collectAsState()

    val listTimers = remember { mutableStateListOf<TimerEntity>() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    var dragSettleJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(activeTimers) {
        if (draggingIndex == -1) {
            listTimers.clear()
            listTimers.addAll(activeTimers)
        }
    }

    var selectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    var liveTicker by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(100)
            liveTicker++
        }
    }

    BackHandler(enabled = true) {
        if (selectionMode) {
            selectionMode = false
            selectedIds.clear()
        } else {
            onBack()
        }
    }

    val cardListState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf {
            cardListState.firstVisibleItemIndex > 0 || cardListState.firstVisibleItemScrollOffset > 300
        }
    }
    val showFloatingBack by remember {
        derivedStateOf {
            cardListState.firstVisibleItemIndex > 0 || cardListState.firstVisibleItemScrollOffset > 140
        }
    }
    val floatingBackAlpha by animateFloatAsState(
        targetValue = if (showFloatingBack) 1f else 0f,
        animationSpec = tween(180),
        label = "floatingBackAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
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
                    onClick = {
                        if (selectionMode) {
                            selectionMode = false
                            selectedIds.clear()
                        } else {
                            onBack()
                        }
                    },
                    enabled = showFloatingBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isLight) Color(0xCCFFFFFF) else Color(0xCC282932),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = textColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = cardListState,
            modifier = Modifier
                .fillMaxSize()
                .edgeFadeMask(),
            contentPadding = PaddingValues(top = statusBarTop + 4.dp, bottom = 160.dp)
        ) {
            item(key = "multi_timer_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (selectionMode) {
                                selectionMode = false
                                selectedIds.clear()
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = stringResource(R.string.multi_timer),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            itemsIndexed(listTimers, key = { _, timer -> timer.id }) { index, timer ->
                val isSelected = selectedIds.contains(timer.id)
                val rem = if (timer.isRunning) {
                    val now = System.currentTimeMillis()
                    (timer.endTimestamp - now).coerceIn(0L, timer.durationMillis)
                } else {
                    timer.remainingMillis.coerceIn(0L, timer.durationMillis)
                }

                val isDragging = draggingIndex == index
                val dragModifier = if (selectionMode) {
                    Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) draggedDistance else 0f
                        }
                        .pointerInput(timer.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragSettleJob?.cancel()
                                    draggingIndex = listTimers.indexOfFirst { it.id == timer.id }
                                    draggedDistance = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggedDistance += dragAmount.y
                                    val from = listTimers.indexOfFirst { it.id == timer.id }
                                    if (from == -1) return@detectDragGesturesAfterLongPress

                                    val draggedInfo = cardListState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == timer.id }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val draggedTop = draggedInfo.offset + draggedDistance
                                    val draggedBottom = draggedTop + draggedInfo.size
                                    val draggedCenterY = (draggedTop + draggedBottom) / 2f

                                    val scrollMargin = 100f
                                    val vpStart = cardListState.layoutInfo.viewportStartOffset.toFloat()
                                    val vpEnd = cardListState.layoutInfo.viewportEndOffset.toFloat()
                                    if (draggedTop < vpStart + scrollMargin) {
                                        scope.launch { cardListState.scrollBy(-18f) }
                                    } else if (draggedBottom > vpEnd - scrollMargin) {
                                        scope.launch { cardListState.scrollBy(18f) }
                                    }

                                    if (dragAmount.y > 0f || draggedDistance > 0f) {
                                        if (from + 1 < listTimers.size) {
                                            val nextTimer = listTimers[from + 1]
                                            val nextInfo = cardListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == nextTimer.id }
                                            if (nextInfo != null && draggedCenterY > (nextInfo.offset + nextInfo.size * 0.5f)) {
                                                val moved = listTimers.removeAt(from)
                                                listTimers.add(from + 1, moved)
                                                draggingIndex = from + 1
                                                draggedDistance = draggedInfo.offset + draggedDistance - nextInfo.offset
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    } else if (dragAmount.y < 0f || draggedDistance < 0f) {
                                        if (from - 1 >= 0) {
                                            val prevTimer = listTimers[from - 1]
                                            val prevInfo = cardListState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == prevTimer.id }
                                            if (prevInfo != null && draggedCenterY < (prevInfo.offset + prevInfo.size * 0.5f)) {
                                                val moved = listTimers.removeAt(from)
                                                listTimers.add(from - 1, moved)
                                                draggingIndex = from - 1
                                                draggedDistance = draggedInfo.offset + draggedDistance - prevInfo.offset
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (draggingIndex != -1) {
                                        timerViewModel.reorderTimers(listTimers.map { it.id })
                                    }
                                    dragSettleJob?.cancel()
                                    dragSettleJob = scope.launch {
                                        animate(
                                            initialValue = draggedDistance,
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) { value, _ ->
                                            draggedDistance = value
                                        }
                                        draggingIndex = -1
                                    }
                                },
                                onDragCancel = {
                                    dragSettleJob?.cancel()
                                    draggingIndex = -1
                                    draggedDistance = 0f
                                    listTimers.clear()
                                    listTimers.addAll(activeTimers)
                                }
                            )
                        }
                } else {
                    Modifier
                }

                TimerCardItem(
                    timer = timer,
                    remainingMillis = rem,
                    removeLeadingZero = removeLeadingZero,
                    selectionMode = selectionMode,
                    isSelected = isSelected,
                    accent = accent,
                    onToggleSelect = {
                        if (selectedIds.contains(timer.id)) {
                            selectedIds.remove(timer.id)
                        } else {
                            selectedIds.add(timer.id)
                        }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectionMode = true
                            selectedIds.clear()
                            selectedIds.add(timer.id)
                        }
                    },
                    onClick = {
                        onOpenTimer(timer)
                    },
                    onStartToggle = {
                        if (timer.isRunning) {
                            timerViewModel.pauseTimerCard(timer)
                        } else {
                            timerViewModel.startTimerCard(timer)
                        }
                    },
                    onReset = {
                        timerViewModel.resetTimerCard(timer)
                    },
                    modifier = if (isDragging) dragModifier else dragModifier.animateItem()
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop && !selectionMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .zIndex(5f)
        ) {
            Surface(
                onClick = {
                    scope.launch {
                        if (cardListState.firstVisibleItemIndex == 0 && cardListState.firstVisibleItemScrollOffset == 0) return@launch
                        cardListState.animateScrollToItem(0)
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
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.action_back_to_top),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (selectionMode) {
            MultiTimerSelectionBottomBar(
                selectedCount = selectedIds.size,
                allSelected = activeTimers.isNotEmpty() && selectedIds.size == activeTimers.size,
                hasEntries = activeTimers.isNotEmpty(),
                onSelectAllToggle = {
                    if (selectedIds.size == activeTimers.size) {
                        selectedIds.clear()
                    } else {
                        selectedIds.clear()
                        selectedIds.addAll(activeTimers.map { it.id })
                    }
                },
                onStart = {
                    timerViewModel.startTimers(selectedIds.toList())
                },
                onStop = {
                    timerViewModel.pauseTimers(selectedIds.toList())
                },
                onReset = {
                    timerViewModel.resetTimers(selectedIds.toList())
                },
                onDelete = {
                    timerViewModel.softDelete(selectedIds.toList())
                    selectionMode = false
                    selectedIds.clear()
                },
                onCancel = {
                    selectionMode = false
                    selectedIds.clear()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(6f)
            )
        } else {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 24.dp, bottom = 32.dp)
                    .zIndex(4f)
            ) {
                SmallFloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = accent,
                    contentColor = onColorFor(accent),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_timer),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, bottom = 32.dp)
                    .zIndex(4f)
            ) {
                Box {
                    SmallFloatingActionButton(
                        onClick = { showOverflowMenu = true },
                        containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF2C2D35),
                        contentColor = if (isLight) Color(0xFF1E1E1E) else Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    AppDropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        items = listOf(
                            AppMenuItem(
                                stringResource(R.string.action_select),
                                Icons.Rounded.Checklist
                            ) {
                                selectionMode = true
                            },
                            AppMenuItem(
                                stringResource(R.string.recycle_bin),
                                Icons.Rounded.DeleteOutline
                            ) {
                                onOpenRecycleBin()
                            },
                            AppMenuItem(
                                stringResource(R.string.clock_settings),
                                Icons.Rounded.Settings
                            ) {
                                onOpenSettings()
                            }
                        )
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTimerBottomSheet(
            isPerTimerVolume = timerVolumePolicy == TimerVolumePolicy.PER_TIMER,
            isPerTimerVibration = timerVibrationPolicy == TimerVibrationPolicy.PER_TIMER,
            onDismiss = { showAddSheet = false },
            onSave = { label, durationMillis, volume, vibrate, pattern ->
                timerViewModel.addPreset(label, durationMillis, volume, vibrate, pattern)
                Toast.makeText(
                    context,
                    context.getString(R.string.timer_saved_preset),
                    Toast.LENGTH_SHORT
                ).show()
                showAddSheet = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerCardItem(
    timer: TimerEntity,
    remainingMillis: Long,
    removeLeadingZero: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    accent: Color,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onStartToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(28.dp)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val labelText = if (timer.label.isNotBlank()) {
        timer.label
    } else {
        stringResource(R.string.tab_timer)
    }

    var liveRemaining by remember(timer.id, timer.isRunning, timer.endTimestamp, timer.remainingMillis) {
        val initialRem = if (timer.isRunning) {
            (timer.endTimestamp - System.currentTimeMillis()).coerceIn(0L, timer.durationMillis)
        } else {
            timer.remainingMillis.coerceIn(0L, timer.durationMillis)
        }
        mutableLongStateOf(initialRem)
    }

    LaunchedEffect(timer.id, timer.isRunning, timer.endTimestamp, timer.remainingMillis) {
        if (timer.isRunning) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val rem = (timer.endTimestamp - now).coerceIn(0L, timer.durationMillis)
                liveRemaining = rem
                if (rem <= 0L) break
                delay(100L)
            }
        } else {
            liveRemaining = timer.remainingMillis.coerceIn(0L, timer.durationMillis)
        }
    }

    val currentRemaining = if (timer.isRunning) liveRemaining else timer.remainingMillis
    val timeText = if (timer.isRunning || timer.isPaused || (timer.remainingMillis in 1 until timer.durationMillis)) {
        formatCardTime(currentRemaining, timer.durationMillis, removeLeadingZero)
    } else {
        formatCardTime(timer.durationMillis, timer.durationMillis, removeLeadingZero)
    }

    val buttonBg = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
    } else if (isLight) {
        Color.Black.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    FrostedBlurBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelect() else onClick()
                },
                onLongClick = onLongClick
            ),
        shape = cardShape,
        tintColor = if (isSelected) MaterialTheme.colorScheme.primary else null,
        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = labelText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SelectionCheckbox(
                        checked = isSelected,
                        onToggle = onToggleSelect
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = textColor
                )

                if (!selectionMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(buttonBg)
                                .clickable(onClick = onReset),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.action_reset),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(buttonBg)
                                .clickable(onClick = onStartToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            val progress = if (timer.durationMillis > 0L) {
                                (currentRemaining.toFloat() / timer.durationMillis.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            val trackColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                            } else if (isLight) {
                                Color.Black.copy(alpha = 0.12f)
                            } else {
                                Color(0xFF2C2D35)
                            }
                            val progressColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else accent

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidthPx = 4.dp.toPx()
                                val diameter = size.minDimension - strokeWidthPx
                                val topLeftOffset = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)
                                val arcSize = Size(diameter, diameter)

                                drawArc(
                                    color = trackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeftOffset,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx)
                                )

                                if (progress > 0.001f) {
                                    drawArc(
                                        color = progressColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress,
                                        useCenter = false,
                                        topLeft = topLeftOffset,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (timer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (timer.isRunning) stringResource(R.string.action_pause) else stringResource(R.string.action_start),
                                tint = progressColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimerBottomSheet(
    isPerTimerVolume: Boolean = false,
    isPerTimerVibration: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, Long, Float, Boolean, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    var timerVolume by remember { mutableFloatStateOf(1.0f) }
    var timerVibrate by remember { mutableStateOf(true) }
    var timerVibrationPattern by remember { mutableStateOf(TimerVibrationPattern.TICK_TICK) }

    val focusManager = LocalFocusManager.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val sheetBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val sheetContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
    val accent = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val wheelUnselected = if (isLight) Color(0xFF9CA3AF) else mutedColor

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
                .padding(horizontal = 20.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_timer),
                style = MaterialTheme.typography.headlineSmall,
                color = sheetContentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            TimerWheelPicker(
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                onHours = { hours = it },
                onMinutes = { minutes = it },
                onSeconds = { seconds = it },
                textColor = sheetContentColor,
                mutedColor = mutedColor,
                unselectedColor = wheelUnselected,
                accent = accent,
                isLight = isLight,
                itemHeight = 56.dp,
                height = 168.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                listOf(
                    "00:10:00" to 600,
                    "00:15:00" to 900,
                    "00:30:00" to 1800
                ).forEach { (presetLabel, total) ->
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isLight) Color(0xFFF3F4F6) else accent.copy(alpha = 0.16f)
                            )
                            .clickable {
                                hours = total / 3600
                                minutes = (total % 3600) / 60
                                seconds = total % 60
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(presetLabel, color = sheetContentColor, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.timer_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = if (isLight) Color(0x33000000) else Color(0x33FFFFFF),
                    focusedLabelColor = accent,
                    unfocusedLabelColor = mutedColor,
                    focusedTextColor = sheetContentColor,
                    unfocusedTextColor = sheetContentColor
                )
            )

            if (isPerTimerVolume) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.timer_volume),
                        fontSize = 14.sp,
                        color = sheetContentColor
                    )
                    Text(
                        text = "${String.format(Locale.US, "%d", (timerVolume * 100).roundToInt())}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Slider(
                    value = timerVolume,
                    onValueChange = { timerVolume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isPerTimerVibration) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.timer_vibration),
                        fontSize = 14.sp,
                        color = sheetContentColor
                    )
                    Switch(
                        checked = timerVibrate,
                        onCheckedChange = { timerVibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = accent
                        )
                    )
                }

                if (timerVibrate) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                                    .background(if (isSelected) accent else if (isLight) Color(0xFFE5E7EB) else Color(0xFF282830))
                                    .clickable { timerVibrationPattern = pattern }
                                    .padding(vertical = 8.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pattern.localizedName(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        val totalMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L
                        if (totalMillis > 0L) {
                            onSave(label.trim(), totalMillis, timerVolume, timerVibrate, timerVibrationPattern.name)
                        }
                    },
                    enabled = (hours > 0 || minutes > 0 || seconds > 0),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
fun MultiTimerSelectionBottomBar(
    selectedCount: Int,
    allSelected: Boolean,
    hasEntries: Boolean,
    onSelectAllToggle: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    SelectionBarContainer(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Surface(
            border = null,
            color = if (lightMode) Color(0xFFFFFFFF) else Color(0xFF2C2D35),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            ) {
                SelectionIconButton(
                    icon = Icons.Rounded.SelectAll,
                    label = if (allSelected) stringResource(R.string.action_deselect) else stringResource(R.string.action_select_all),
                    enabled = hasEntries,
                    onClick = onSelectAllToggle
                )
                SelectionIconButton(
                    icon = Icons.Rounded.PlayArrow,
                    label = stringResource(R.string.action_start),
                    enabled = selectedCount > 0,
                    iconSize = 26.dp,
                    onClick = onStart
                )
                SelectionIconButton(
                    icon = Icons.Rounded.Stop,
                    label = stringResource(R.string.action_stop),
                    enabled = selectedCount > 0,
                    iconSize = 24.dp,
                    onClick = onStop
                )
                SelectionIconButton(
                    icon = Icons.Rounded.Refresh,
                    label = stringResource(R.string.action_reset),
                    enabled = selectedCount > 0,
                    onClick = onReset
                )
                SelectionIconButton(
                    icon = Icons.Rounded.Delete,
                    label = stringResource(R.string.action_delete),
                    enabled = selectedCount > 0,
                    onClick = onDelete
                )
                SelectionIconButton(
                    icon = Icons.Rounded.Close,
                    label = stringResource(R.string.action_cancel),
                    enabled = true,
                    onClick = onCancel
                )
            }
        }
    }
}

private fun formatCardTime(
    millis: Long,
    maxMillis: Long = Long.MAX_VALUE,
    removeLeadingZero: Boolean = false
): String {
    val maxSec = maxMillis / 1000L
    val totalSec = ((millis + 999L) / 1000L).coerceIn(0L, maxSec)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        if (removeLeadingZero) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        }
    } else {
        if (removeLeadingZero) {
            if (m > 0) {
                String.format(Locale.US, "%d:%02d", m, s)
            } else {
                String.format(Locale.US, "%d", s)
            }
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }
}
