package com.lucid.alarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.Dialog
import com.lucid.alarm.ui.components.FrostedBlurBox
import com.lucid.alarm.ui.components.GroupCapsulePill
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.data.prefs.AlarmSortMode
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.data.prefs.VolumePolicy
import com.lucid.alarm.ui.components.AlarmCard
import com.lucid.alarm.ui.components.AlarmEditSheet
import com.lucid.alarm.ui.components.AppDropdownMenu
import com.lucid.alarm.ui.components.AppMenuItem
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.components.SelectionChipShape
import com.lucid.alarm.ui.components.selectedChipColors
import com.lucid.alarm.ui.theme.onColorFor
import com.lucid.alarm.ui.viewmodel.AlarmViewModel
import com.lucid.alarm.util.TimeLeftFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DayFilter(val calendarConstant: Int, val label: String)

private enum class AlarmFilterStatus(val label: String) {
    ALL("All"),
    ENABLED("Turned On"),
    DISABLED("Turned Off")
}

private val DAY_FILTERS = listOf(
    DayFilter(Calendar.SUNDAY, "Sun"),
    DayFilter(Calendar.MONDAY, "Mon"),
    DayFilter(Calendar.TUESDAY, "Tue"),
    DayFilter(Calendar.WEDNESDAY, "Wed"),
    DayFilter(Calendar.THURSDAY, "Thu"),
    DayFilter(Calendar.FRIDAY, "Fri"),
    DayFilter(Calendar.SATURDAY, "Sat")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAlarmsScreen(
    viewModel: AlarmViewModel,
    settingsDataStore: SettingsDataStore,
    barsVisible: Boolean,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    dismissTransientUi: Long? = null,
    onCanScrollChange: (Boolean) -> Unit = {},
    onScrollInProgressChange: (Boolean) -> Unit = {},
    fabInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    val recentlyDisabledRepeating by viewModel.recentlyDisabledRepeating.collectAsState()
    val volumePolicy by settingsDataStore.volumePolicyFlow.collectAsState(
        initial = VolumePolicy.UNIFIED
    )
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    val removeLeadingZero by settingsDataStore.removeLeadingZeroFlow.collectAsState(initial = true)
    val sortMode by settingsDataStore.alarmSortModeFlow.collectAsState(initial = AlarmSortMode.TIME)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var selectionActive by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedGroupNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupEditTarget by remember { mutableStateOf<String?>(null) }
    var groupRenameTarget by remember { mutableStateOf<String?>(null) }
    var alarmRenameTarget by remember { mutableStateOf<AlarmEntity?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showDayFilterDialog by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf(AlarmFilterStatus.ALL) }

    val savedGroupOrder by settingsDataStore.alarmGroupOrderFlow.collectAsState(initial = emptyList())

    val groups = remember(alarms, savedGroupOrder) {
        val allPresent = alarms.map { it.groupName }.filter { it.isNotBlank() }.distinct()
        val allGroups = (listOf(AlarmEntity.DEFAULT_GROUP) + allPresent).distinct()
        if (savedGroupOrder.isEmpty()) {
            val custom = allGroups.filter { it != AlarmEntity.DEFAULT_GROUP }.sorted()
            listOf(AlarmEntity.DEFAULT_GROUP) + custom
        } else {
            val ordered = savedGroupOrder.filter { it in allGroups }
            val remaining = allGroups.filter { it !in ordered }
            ordered + remaining
        }
    }
    val displayGroups = remember(groups) { mutableStateListOf<String>().apply { addAll(groups) } }
    LaunchedEffect(groups) {
        if (displayGroups.toList() != groups) {
            displayGroups.clear()
            displayGroups.addAll(groups)
        }
    }
    val hasCustomGroups = groups.size > 1

    val groupCounts by remember(alarms) {
        derivedStateOf {
            val totalEnabled = alarms.count { it.isEnabled }
            val map = HashMap<String?, Pair<Int, Int>>()
            map[null] = totalEnabled to (alarms.size - totalEnabled)
            alarms.groupBy { it.groupName }.forEach { (group, groupAlarms) ->
                val enabled = groupAlarms.count { it.isEnabled }
                map[group] = enabled to (groupAlarms.size - enabled)
            }
            map
        }
    }

    val filteredAlarms = remember(alarms, selectedGroup, filterStatus, selectedDay) {
        alarms.filter { alarm ->
            val groupMatches = selectedGroup == null || alarm.groupName == selectedGroup
            val dayMatches = selectedDay == null || alarm.daysOfWeek.contains(selectedDay)
            val enabledMatches = when (filterStatus) {
                AlarmFilterStatus.ALL -> true
                AlarmFilterStatus.ENABLED -> alarm.isEnabled
                AlarmFilterStatus.DISABLED -> !alarm.isEnabled
            }
            groupMatches && dayMatches && enabledMatches
        }
    }

    val sortedAlarms = remember(filteredAlarms, sortMode) {
        when (sortMode) {
            AlarmSortMode.TIME -> filteredAlarms.sortedWith(
                compareBy({ it.hour }, { it.minute })
            )
            AlarmSortMode.DAY -> filteredAlarms.sortedWith(
                compareBy({ it.daysOfWeek.minOrNull() ?: 8 }, { it.hour }, { it.minute })
            )
            AlarmSortMode.ENABLED_FIRST -> filteredAlarms.sortedWith(
                compareByDescending<AlarmEntity> { it.isEnabled }
                    .thenBy { it.hour }.thenBy { it.minute }
            )
            AlarmSortMode.DISABLED_FIRST -> filteredAlarms.sortedWith(
                compareBy<AlarmEntity> { it.isEnabled }
                    .thenBy { it.hour }.thenBy { it.minute }
            )
            AlarmSortMode.CUSTOM -> filteredAlarms.sortedBy { it.sortOrder }
        }
    }

    val selectionMode = selectionActive

    LaunchedEffect(selectionMode) { onSelectionModeChange(selectionMode) }

    LaunchedEffect(dismissTransientUi) {
        if (dismissTransientUi == null) return@LaunchedEffect
        showEditSheet = false
        editingAlarm = null
        showGroupDialog = false
        groupEditTarget = null
        showSortDialog = false
        showOverflowMenu = false
        showFilterDialog = false
        showDayFilterDialog = false
        selectionActive = false
        selectedIds = emptySet()
        selectedGroupNames = emptySet()
    }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val cornerButtonsAlpha by animateFloatAsState(
        targetValue = if (barsVisible) 1f else 0f,
        label = "cornerButtonsAlpha"
    )

    val displayAlarms = remember { mutableStateListOf<AlarmEntity>() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    var dragMoved by remember { mutableStateOf(false) }
    var dragSettleJob by remember { mutableStateOf<Job?>(null) }

    var pendingOrderIds by remember { mutableStateOf<List<Int>?>(null) }

    LaunchedEffect(pendingOrderIds) {
        val pending = pendingOrderIds ?: return@LaunchedEffect
        delay(3_000L)
        if (pendingOrderIds == pending) pendingOrderIds = null
    }

    LaunchedEffect(sortedAlarms, sortMode) {
        val pending = pendingOrderIds
        if (pending != null) {
            if (sortedAlarms.map { it.id } == pending) {
                pendingOrderIds = null
            } else {
                return@LaunchedEffect
            }
        }
        if (draggingIndex == -1) {
            displayAlarms.clear()
            displayAlarms.addAll(sortedAlarms)
        }
    }

    val headerCount = if (hasCustomGroups) 2 else 1
    val headerCountState = rememberUpdatedState(headerCount)
    val renderList = displayAlarms

    val canScroll by remember {
        derivedStateOf {
            if (renderList.isEmpty()) {
                false
            } else {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val visibleItems = layoutInfo.visibleItemsInfo
                listState.canScrollForward || listState.canScrollBackward || (visibleItems.size < totalItems && totalItems > 0)
            }
        }
    }

    LaunchedEffect(canScroll) {
        onCanScrollChange(canScroll)
    }

    LaunchedEffect(listState.isScrollInProgress) {
        onScrollInProgressChange(listState.isScrollInProgress)
    }

    val headerCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12 }
    }
    var isScrollingUp by remember { mutableStateOf(false) }
    var prevIndex by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    var prevOffset by remember { mutableIntStateOf(listState.firstVisibleItemScrollOffset) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val curIndex = listState.firstVisibleItemIndex
        val curOffset = listState.firstVisibleItemScrollOffset
        if (curIndex < prevIndex || (curIndex == prevIndex && curOffset < prevOffset)) {
            isScrollingUp = true
        } else if (curIndex > prevIndex || (curIndex == prevIndex && curOffset > prevOffset)) {
            isScrollingUp = false
        }
        prevIndex = curIndex
        prevOffset = curOffset
    }

    val showScrollToTop by remember {
        derivedStateOf {
            val pastThreshold = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 220
            pastThreshold && (!listState.isScrollInProgress || isScrollingUp)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (alarms.isEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().edgeFadeMask(),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                item(key = "main_header") {
                    Box(modifier = Modifier.zIndex(5f).padding(top = statusBarTop)) {
                        MainHeader()
                    }
                }
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No alarms scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().edgeFadeMask(),
                contentPadding = PaddingValues(bottom = 112.dp)
            ) {
                item(key = "hero") {
                    Box(modifier = Modifier.zIndex(5f)) {
                        NextAlarmHeader(
                            alarms = alarms,
                            is24Hour = timeFormat.is24Hour,
                            removeLeadingZero = removeLeadingZero,
                            modifier = Modifier.padding(top = statusBarTop)
                        )
                    }
                }
                if (hasCustomGroups) {
                    item(key = "groupFilter") {
                        GroupFilterRow(
                            groups = displayGroups,
                            selectedGroup = selectedGroup,
                            selectionMode = selectionMode,
                            selectedGroupNames = selectedGroupNames,
                            counts = { group -> groupCounts[group] ?: (0 to 0) },
                            onGroupSelected = { selectedGroup = it },
                            onToggleSelection = { group ->
                                selectedGroupNames = if (selectedGroupNames.contains(group)) {
                                    selectedGroupNames - group
                                } else {
                                    selectedGroupNames + group
                                }
                            },
                            onEnterSelection = { group ->
                                selectionActive = true
                                selectedGroupNames = setOf(group)
                                selectedIds = emptySet()
                            },
                            onReorderGroups = { newOrder ->
                                scope.launch { settingsDataStore.setAlarmGroupOrder(newOrder) }
                            }
                        )
                    }
                }

                if (renderList.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No alarms scheduled",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(renderList, key = { _, alarm -> alarm.id }) { index, alarm ->
                        val isDragging = draggingIndex == index

                        val dragModifier = if (selectionMode) {
                            Modifier
                                .zIndex(if (isDragging) 5f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) draggedDistance else 0f
                                    if (isDragging) {
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                }
                                .pointerInput(alarm.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragSettleJob?.cancel()
                                            draggingIndex = displayAlarms
                                                .indexOfFirst { it.id == alarm.id }
                                            draggedDistance = 0f
                                            dragMoved = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedDistance += dragAmount.y
                                            val from = displayAlarms.indexOfFirst { it.id == alarm.id }
                                            if (from == -1) return@detectDragGesturesAfterLongPress

                                            val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == alarm.id }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val draggedTop = draggedInfo.offset + draggedDistance
                                            val draggedBottom = draggedTop + draggedInfo.size
                                            val draggedCenterY = (draggedTop + draggedBottom) / 2f

                                            val scrollMargin = 100f
                                            val vpStart = listState.layoutInfo.viewportStartOffset.toFloat()
                                            val vpEnd = listState.layoutInfo.viewportEndOffset.toFloat()
                                            if (draggedTop < vpStart + scrollMargin) {
                                                scope.launch { listState.scrollBy(-18f) }
                                            } else if (draggedBottom > vpEnd - scrollMargin) {
                                                scope.launch { listState.scrollBy(18f) }
                                            }

                                            if (dragAmount.y > 0f || draggedDistance > 0f) {
                                                if (from + 1 < displayAlarms.size) {
                                                    val nextAlarm = displayAlarms[from + 1]
                                                    val nextInfo = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == nextAlarm.id }
                                                    if (nextInfo != null && draggedCenterY > (nextInfo.offset + nextInfo.size * 0.5f)) {
                                                        val moved = displayAlarms.removeAt(from)
                                                        displayAlarms.add(from + 1, moved)
                                                        draggingIndex = from + 1
                                                        draggedDistance = draggedInfo.offset + draggedDistance - nextInfo.offset
                                                        dragMoved = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            } else if (dragAmount.y < 0f || draggedDistance < 0f) {
                                                if (from - 1 >= 0) {
                                                    val prevAlarm = displayAlarms[from - 1]
                                                    val prevInfo = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == prevAlarm.id }
                                                    if (prevInfo != null && draggedCenterY < (prevInfo.offset + prevInfo.size * 0.5f)) {
                                                        val moved = displayAlarms.removeAt(from)
                                                        displayAlarms.add(from - 1, moved)
                                                        draggingIndex = from - 1
                                                        draggedDistance = draggedInfo.offset + draggedDistance - prevInfo.offset
                                                        dragMoved = true
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggingIndex != -1 && dragMoved) {

                                                pendingOrderIds = displayAlarms.map { it.id }
                                                viewModel.setSortMode(AlarmSortMode.CUSTOM)
                                                viewModel.reorderAlarms(displayAlarms.map { it.id })
                                            }
                                            dragMoved = false
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
                                            dragMoved = false
                                        }
                                    )
                                }
                        } else {
                            Modifier
                        }

                        val totalCount = renderList.size
                        val isSelected = selectedIds.contains(alarm.id)
                        val cardShape = when {
                            totalCount == 1 -> RoundedCornerShape(28.dp)
                            index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                            index == totalCount - 1 -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                        val cardPadding = when {
                            totalCount == 1 -> PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            index == 0 -> PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 0.dp)
                            index == totalCount - 1 -> PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 6.dp)
                            else -> PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        }
                        val showBottomDivider = index < totalCount - 1

                        AlarmCard(
                            alarm = alarm,
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            is24Hour = timeFormat.is24Hour,
                            removeLeadingZero = removeLeadingZero,
                            onToggle = { enabled ->
                                if (selectionMode) {
                                    selectedIds = selectedIds.toggle(alarm.id)
                                } else {
                                    viewModel.toggleAlarm(alarm, enabled)
                                }
                            },
                            onClick = {
                                if (selectionMode) {
                                    selectedIds = selectedIds.toggle(alarm.id)
                                } else {
                                    editingAlarm = alarm
                                    showEditSheet = true
                                }
                            },
                            onLongClick = {

                                if (!selectionMode) {
                                    selectionActive = true
                                    selectedGroupNames = emptySet()
                                    selectedIds = selectedIds + alarm.id
                                }
                            },
                            showReenablePill = !selectionMode &&
                                alarm.id in recentlyDisabledRepeating &&
                                !alarm.isEnabled &&
                                alarm.daysOfWeek.isNotEmpty(),
                            onReenable = { viewModel.toggleAlarm(alarm, true) },
                            cardShape = cardShape,
                            cardPadding = cardPadding,
                            showBottomDivider = showBottomDivider,
                            connectedIndex = index,
                            totalConnected = totalCount,
                            modifier = if (isDragging) dragModifier else dragModifier.animateItem()
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
                .padding(horizontal = 6.dp, vertical = 20.dp)
        ) {
            val groupAlarmIds = alarms
                .filter { it.groupName in selectedGroupNames }
                .map { it.id }
            val groupsOnly = selectedGroupNames.isNotEmpty() && selectedIds.isEmpty()
            val mixedSelection = selectedGroupNames.isNotEmpty() && selectedIds.isNotEmpty()
            val allSelectedIds = (selectedIds + groupAlarmIds).toList()
            val singleGroup = if (selectedGroupNames.size == 1 && selectedIds.isEmpty()) selectedGroupNames.first() else null
            val singleAlarm = if (selectedIds.size == 1 && selectedGroupNames.isEmpty()) alarms.firstOrNull { it.id == selectedIds.first() } else null
            SelectionActionBar(
                selectedCount = selectedIds.size + selectedGroupNames.size,
                allSelected = filteredAlarms.isNotEmpty() &&
                    selectedIds.containsAll(filteredAlarms.map { it.id }),
                groupsOnly = groupsOnly,
                mixedSelection = mixedSelection,
                onRename = if (singleGroup != null) {
                    if (singleGroup != AlarmEntity.DEFAULT_GROUP) {
                        { groupRenameTarget = singleGroup }
                    } else null
                } else if (singleAlarm != null) {
                    { alarmRenameTarget = singleAlarm }
                } else null,
                onClose = {
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                },
                onSelectAllToggle = {
                    selectedIds = if (selectedIds.containsAll(filteredAlarms.map { it.id })) {
                        emptySet()
                    } else {
                        filteredAlarms.map { it.id }.toSet()
                    }
                },
                onMoveToGroup = { showGroupDialog = true },
                onCopy = {
                    viewModel.copyAlarms(selectedIds.toList())
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                },
                onToggle = {
                    val ids = selectedIds.toList()
                    val anyEnabled = alarms.filter { it.id in selectedIds }.any { it.isEnabled }
                    viewModel.setEnabledFor(ids, !anyEnabled)
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                },
                onTurnOn = {
                    viewModel.setEnabledFor(allSelectedIds, true)
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                },
                onTurnOff = {
                    viewModel.setEnabledFor(allSelectedIds, false)
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                },
                onUngroup = {
                    viewModel.assignGroup(groupAlarmIds, AlarmEntity.DEFAULT_GROUP)
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                },
                onDelete = {
                    val activeGroup = selectedGroup
                    val autoTrashGroup = if (activeGroup != null && activeGroup != AlarmEntity.DEFAULT_GROUP && filteredAlarms.isNotEmpty() && selectedIds.containsAll(filteredAlarms.map { it.id })) {
                        setOf(activeGroup)
                    } else {
                        emptySet()
                    }
                    val groupsToTrash = selectedGroupNames + autoTrashGroup
                    viewModel.moveSelectionToTrash(groupsToTrash, selectedIds)
                    selectionActive = false
                    selectedIds = emptySet()
                    selectedGroupNames = emptySet()
                }
            )
        }

        AnimatedVisibility(
            visible = !selectionMode && barsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 96.dp)
                .zIndex(4f)
        ) {
            val accentColor = MaterialTheme.colorScheme.primary
            SmallFloatingActionButton(
                onClick = {
                    editingAlarm = null
                    showEditSheet = true
                },
                containerColor = accentColor,
                contentColor = onColorFor(accentColor),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add alarm",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop && !selectionMode && barsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
                .zIndex(5f)
        ) {
            Surface(
                onClick = {
                    scope.launch {
                        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) return@launch
                        listState.animateScrollToItem(0)
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
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Back to top",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !selectionMode && barsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 96.dp)
                .zIndex(4f)
        ) {
            Box {
                SmallFloatingActionButton(
                    onClick = {
                        showOverflowMenu = true
                    },
                    containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF2C2D35),
                    contentColor = if (isLight) Color(0xFF1E1E1E) else Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(22.dp)
                    )
                }
                AppDropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    items = listOf(
                        AppMenuItem("Select", Icons.Outlined.Checklist) { selectionActive = true },
                        AppMenuItem("Sort", Icons.AutoMirrored.Outlined.Sort) { showSortDialog = true },
                        AppMenuItem("Filter", Icons.Outlined.Tune) { showFilterDialog = true },
                        AppMenuItem("Filter by Days", Icons.Outlined.DateRange) {
                            showDayFilterDialog = true
                        },
                        AppMenuItem("Recycle Bin", Icons.Outlined.DeleteOutline) { onOpenRecycleBin() },
                        AppMenuItem("Settings", Icons.Outlined.Settings) { onOpenSettings() }
                    )
                )
            }
        }
    }

    if (showEditSheet) {
        AlarmEditSheet(
            initialAlarm = editingAlarm,
            availableGroups = groups,
            showVolumeSlider = volumePolicy == VolumePolicy.PER_ALARM,
            is24Hour = timeFormat.is24Hour,
            settingsDataStore = settingsDataStore,
            onDismiss = { showEditSheet = false },
            onSave = { updatedAlarm ->
                if (editingAlarm == null) {
                    viewModel.addAlarm(updatedAlarm)
                } else {
                    viewModel.updateAlarm(updatedAlarm)
                }
                showEditSheet = false
            }
        )
    }

    if (showGroupDialog) {
        GroupAssignmentDialog(
            groups = groups,
            onDismiss = { showGroupDialog = false },
            onConfirm = { group ->
                viewModel.assignGroup(selectedIds.toList(), group)
                selectionActive = false
                selectedIds = emptySet()
                showGroupDialog = false
            }
        )
    }

    groupEditTarget?.let { target ->
        GroupEditDialog(
            group = target,
            hasMultipleGroups = hasCustomGroups,
            onDismiss = { groupEditTarget = null },
            onRename = { newName ->
                viewModel.renameGroup(target, newName)
                selectedGroup = newName.trim().ifBlank { target }
                groupEditTarget = null
                selectionActive = false
                selectedIds = emptySet()
                selectedGroupNames = emptySet()
            },
            onDelete = {
                viewModel.deleteGroup(target)
                if (selectedGroup == target) selectedGroup = null
                groupEditTarget = null
                selectionActive = false
                selectedIds = emptySet()
                selectedGroupNames = emptySet()
            }
        )
    }

    groupRenameTarget?.let { group ->
        CompactRenameDialog(
            title = "Rename group",
            initialValue = group,
            label = "Group name",
            onDismiss = { groupRenameTarget = null },
            onConfirm = { newName ->
                viewModel.renameGroup(group, newName)
                groupRenameTarget = null
                selectionActive = false
                selectedIds = emptySet()
                selectedGroupNames = emptySet()
            }
        )
    }

    alarmRenameTarget?.let { target ->
        CompactRenameDialog(
            title = "Rename alarm",
            initialValue = target.label,
            label = "Alarm label",
            onDismiss = { alarmRenameTarget = null },
            onConfirm = { newLabel ->
                viewModel.updateAlarm(target.copy(label = newLabel))
                alarmRenameTarget = null
                selectionActive = false
                selectedIds = emptySet()
                selectedGroupNames = emptySet()
            }
        )
    }

    if (showSortDialog) {
        SortModeDialog(
            current = sortMode,
            onDismiss = { showSortDialog = false },
            onSelect = { mode ->
                viewModel.setSortMode(mode)
                showSortDialog = false
            }
        )
    }

    if (showFilterDialog) {
        var draftStatus by remember { mutableStateOf(filterStatus) }
        val dialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
        val dialogContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            containerColor = dialogBg,
            titleContentColor = dialogContentColor,
            textContentColor = dialogContentColor,
            title = { Text("Filter alarms") },
            text = {
                Column {
                    AlarmFilterStatus.entries.forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = draftStatus == status,
                                onClick = { draftStatus = status }
                            )
                            Text(
                                text = status.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    filterStatus = draftStatus
                    showFilterDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        draftStatus = AlarmFilterStatus.ALL
                    }) { Text("Reset") }
                    TextButton(onClick = { showFilterDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (showDayFilterDialog) {
        DayFilterDialog(
            selectedDay = selectedDay,
            onSelect = { selectedDay = it },
            onDismiss = { showDayFilterDialog = false }
        )
    }
}

@Composable
private fun MainHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .zIndex(5f)
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Alarms",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

    }
}

@Composable
private fun SortModeDialog(
    current: AlarmSortMode,
    onDismiss: () -> Unit,
    onSelect: (AlarmSortMode) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val dialogContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        title = { Text("Sort alarms by") },
        text = {
            Column {
                AlarmSortMode.entries.filter { it == AlarmSortMode.TIME || it == AlarmSortMode.DAY || it == AlarmSortMode.CUSTOM }.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = { onSelect(mode) }
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun NextAlarmHeader(
    alarms: List<AlarmEntity>,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false,
    modifier: Modifier = Modifier
) {
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowTick = System.currentTimeMillis()
            delay(30_000L)
        }
    }

    val now = remember(nowTick) { Calendar.getInstance() }
    val upcoming = alarms
        .filter { it.isEnabled }
        .minByOrNull { nextTriggerMillis(it, now) }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val titleColor = if (isLight) Color(0xFF0B0C10) else Color.White
    val subtitleColor = titleColor.copy(alpha = 0.78f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 88.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (upcoming == null) {
            Text(
                text = "All alarms are off",
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No upcoming alarms",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = subtitleColor,
                maxLines = 1
            )
        } else {
            val triggerMillis = nextTriggerMillis(upcoming, now)
            Text(
                text = formatCountdownLines(triggerMillis - nowTick),
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = nextAlarmSubtitle(upcoming, triggerMillis, is24Hour, removeLeadingZero),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun nextTriggerMillis(alarm: AlarmEntity, now: Calendar): Long {
    val target = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
        set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    val days = alarm.daysOfWeek
    if (days.isNotEmpty()) {
        var guard = 0
        while (guard < 8 && !days.contains(target.get(Calendar.DAY_OF_WEEK))) {
            target.add(Calendar.DAY_OF_YEAR, 1)
            guard++
        }
    }
    return target.timeInMillis
}

private fun formatCountdownLines(diffMillis: Long): String {
    val human = TimeLeftFormatter.humanized(diffMillis)
    if (human == "less than a minute") return "Alarm in\nless than a minute"
    val tokens = human
        .replace(" and ", " ")
        .replace(",", " ")
        .split(" ")
        .filter { it.isNotBlank() }
    if (tokens.size <= 2) return "Alarm in $human"
    return "Alarm in ${tokens[0]} ${tokens[1]}\n" + tokens.drop(2).joinToString(" ")
}

private fun nextAlarmSubtitle(
    alarm: AlarmEntity,
    triggerMillis: Long,
    is24Hour: Boolean,
    removeLeadingZero: Boolean = false
): String {
    val trigger = Calendar.getInstance().apply { timeInMillis = triggerMillis }
    val date = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(trigger.time)
    val time = formatHourMinute(alarm.hour, alarm.minute, is24Hour, removeLeadingZero) +
        if (is24Hour) {
            ""
        } else {
            " " + amPmSuffix(alarm.hour, is24Hour).lowercase(Locale.getDefault())
        }
    return "$date, $time"
}

private fun Set<Int>.toggle(id: Int): Set<Int> =
    if (contains(id)) this - id else this + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupFilterRow(
    groups: MutableList<String>,
    selectedGroup: String?,
    selectionMode: Boolean,
    selectedGroupNames: Set<String>,
    counts: (String?) -> Pair<Int, Int>,
    onGroupSelected: (String?) -> Unit,
    onToggleSelection: (String) -> Unit,
    onEnterSelection: (String) -> Unit,
    onReorderGroups: (List<String>) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val itemBounds = remember { mutableMapOf<String, Rect>() }
    var draggingGroup by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val chips: List<String?> = listOf<String?>(null) + groups
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chips.chunked(2).forEach { rowChips ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowChips.forEach { group ->
                    val groupSelected = if (selectionMode) {
                        group != null && selectedGroupNames.contains(group)
                    } else {
                        selectedGroup == group
                    }
                    val isDraggingThis = draggingGroup == group

                    val dragModifier = if (selectionMode && group != null) {
                        Modifier
                            .zIndex(if (isDraggingThis) 10f else 1f)
                            .graphicsLayer {
                                if (isDraggingThis) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                }
                            }
                            .onGloballyPositioned { coords ->
                                itemBounds[group] = coords.boundsInParent()
                            }
                            .pointerInput(group) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingGroup = group
                                        dragOffset = Offset.Zero
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount
                                        val currentBounds = itemBounds[group] ?: return@detectDragGesturesAfterLongPress
                                        val draggedCenter = currentBounds.center + dragOffset
                                        val targetGroup = groups.minByOrNull { other ->
                                            if (other == group) Float.MAX_VALUE
                                            else {
                                                val b = itemBounds[other] ?: return@minByOrNull Float.MAX_VALUE
                                                (b.center - draggedCenter).getDistance()
                                            }
                                        }
                                        if (targetGroup != null) {
                                            val otherBounds = itemBounds[targetGroup]
                                            if (otherBounds != null && (otherBounds.center - draggedCenter).getDistance() < 90f) {
                                                val fromIndex = groups.indexOf(group)
                                                val toIndex = groups.indexOf(targetGroup)
                                                if (fromIndex != -1 && toIndex != -1) {
                                                    val moved = groups.removeAt(fromIndex)
                                                    groups.add(toIndex, moved)
                                                    dragOffset -= (otherBounds.center - currentBounds.center)
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggingGroup != null) {
                                            onReorderGroups(groups.toList())
                                        }
                                        draggingGroup = null
                                        dragOffset = Offset.Zero
                                    },
                                    onDragCancel = {
                                        draggingGroup = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                    } else {
                        Modifier.onGloballyPositioned { coords ->
                            if (group != null) itemBounds[group] = coords.boundsInParent()
                        }
                    }

                    GroupCapsulePill(
                        label = group ?: "All",
                        selected = groupSelected,
                        counts = counts(group),
                        onClick = {
                            if (selectionMode) {
                                if (group != null) onToggleSelection(group)
                            } else {
                                onGroupSelected(group)
                            }
                        },
                        onLongClick = if (group != null) {
                            {
                                if (selectionMode) {
                                    onToggleSelection(group)
                                } else {
                                    onEnterSelection(group)
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .weight(1f)
                            .then(dragModifier)
                    )
                }
                if (rowChips.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayFilterDialog(
    selectedDay: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val dialogContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        title = { Text("Filter by days") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UniformChip(
                    label = "All",
                    selected = selectedDay == null,
                    onClick = { onSelect(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                )
                DAY_FILTERS.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { day ->
                            UniformChip(
                                label = day.label,
                                selected = selectedDay == day.calendarConstant,
                                onClick = {
                                    onSelect(
                                        if (selectedDay == day.calendarConstant) {
                                            null
                                        } else {
                                            day.calendarConstant
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        dismissButton = {
            if (selectedDay != null) {
                TextButton(onClick = { onSelect(null) }) { Text("Clear Filter") }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UniformChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    counts: Pair<Int, Int>? = null
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val chipBg = when {
        selected -> MaterialTheme.colorScheme.primary
        isLight -> Color(0x1F000000)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(chipBg)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(
                    start = if (counts == null) 4.dp else 10.dp,
                    end = if (counts == null) 4.dp else 10.dp
                ),
            horizontalArrangement = if (counts == null) {
                Arrangement.Center
            } else {
                Arrangement.Start
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelColor
            )
        if (counts != null) {
            Spacer(modifier = Modifier.width(6.dp))
            CountChip(
                icon = Icons.Outlined.NotificationsActive,
                count = counts.first,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            CountChip(
                icon = Icons.Outlined.NotificationsOff,
                count = counts.second,
                tint = labelColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CountChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun GroupEditDialog(
    group: String,
    hasMultipleGroups: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(group) { mutableStateOf(group) }
    val isDefault = group == AlarmEntity.DEFAULT_GROUP
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val dialogContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = dialogBg,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        title = { Text(if (isDefault) "General alarms" else "Edit group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isDefault) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Group name") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = dialogContentColor,
                            unfocusedTextColor = dialogContentColor,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = if (isLight) Color(0xFF757575) else Color(0xFFAAAAAA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "This group contains your general alarms.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isDefault || hasMultipleGroups) {
                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDefault) "Delete general alarms" else "Delete group")
                    }
                }
            }
        },
        confirmButton = {
            if (!isDefault) {
                TextButton(
                    onClick = { onRename(name) },
                    enabled = name.isNotBlank() && name != group
                ) {
                    Text("Rename")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CompactRenameDialog(
    title: String,
    initialValue: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val contentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
    var textValue by remember(initialValue) { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        FrostedBlurBox(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(label) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (isLight) Color(0xFF757575) else Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(textValue.trim()) },
                        enabled = textValue.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    allSelected: Boolean,
    groupsOnly: Boolean,
    mixedSelection: Boolean,
    onClose: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onMoveToGroup: () -> Unit,
    onCopy: () -> Unit,
    onToggle: () -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onUngroup: () -> Unit,
    onDelete: () -> Unit,
    onRename: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val lightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f

        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(onClick = onClose) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            border = null,
            color = if (lightMode) {
                Color(0xFFFFFFFF)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(999.dp),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                when {
                    groupsOnly -> {
                        SelectionAction(Icons.Outlined.NotificationsActive, "Turn On", onTurnOn)
                        SelectionAction(Icons.Outlined.NotificationsOff, "Turn Off", onTurnOff)
                        if (selectedCount == 1 && onRename != null) {
                            SelectionAction(Icons.Outlined.Edit, "Rename", onRename)
                        }
                        SelectionAction(Icons.Outlined.FolderOff, "Ungroup", onUngroup)
                        SelectionAction(Icons.Outlined.DeleteOutline, "Delete", onDelete)
                    }

                    mixedSelection -> {
                        SelectionAction(Icons.Outlined.NotificationsActive, "Turn On", onTurnOn)
                        SelectionAction(Icons.Outlined.NotificationsOff, "Turn Off", onTurnOff)
                        SelectionAction(Icons.Outlined.DeleteOutline, "Delete", onDelete)
                    }

                    else -> {
                        SelectionAction(
                            Icons.Outlined.SelectAll,
                            if (allSelected) "Deselect" else "Select",
                            onSelectAllToggle
                        )
                        if (selectedCount == 1 && onRename != null) {
                            SelectionAction(Icons.Outlined.Edit, "Rename", onRename)
                        }
                        SelectionAction(Icons.Outlined.CreateNewFolder, "Group", onMoveToGroup)
                        SelectionAction(Icons.Outlined.ContentCopy, "Copy", onCopy)
                        SelectionAction(Icons.Outlined.PowerSettingsNew, "Toggle", onToggle)
                        SelectionAction(Icons.Outlined.DeleteOutline, "Delete", onDelete)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GroupAssignmentDialog(
    groups: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var groupName by remember { mutableStateOf(groups.firstOrNull() ?: AlarmEntity.DEFAULT_GROUP) }
    var newGroup by remember { mutableStateOf("") }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val dialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val dialogContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        title = { Text("Move / Add to group") },
        text = {
            Column {
                OutlinedTextField(
                    value = newGroup,
                    onValueChange = { newGroup = it },
                    label = { Text("New group name") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dialogContentColor,
                        unfocusedTextColor = dialogContentColor,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (isLight) Color(0xFF757575) else Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = newGroup.isBlank() && groupName == group,
                            onClick = {
                                newGroup = ""
                                groupName = group
                            },
                            shape = SelectionChipShape,
                            colors = selectedChipColors(),
                            label = { Text(group) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(newGroup.ifBlank { groupName }.ifBlank { AlarmEntity.DEFAULT_GROUP })
            }) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
