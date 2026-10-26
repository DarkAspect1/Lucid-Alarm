package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.data.prefs.SettingsDataStore
import com.lucid.alarm.data.prefs.TimeFormat
import com.lucid.alarm.data.prefs.WorldClockSortMode
import com.lucid.alarm.ui.components.AppDropdownMenu
import com.lucid.alarm.ui.components.AppMenuItem
import com.lucid.alarm.ui.components.FrostedBlurBox
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.components.SelectionCheckbox
import com.lucid.alarm.ui.viewmodel.WorldClockViewModel
import com.lucid.alarm.ui.amPmSuffix
import com.lucid.alarm.ui.formatHourMinute
import com.lucid.alarm.ui.theme.onColorFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

data class WorldCity(
    val zoneId: String,
    val city: String,
    val region: String
)

private fun buildWorldCities(): List<WorldCity> =
    ZoneId.getAvailableZoneIds()
        .filter { it.contains("/") }
        .map { zoneId ->
            WorldCity(
                zoneId = zoneId,
                city = zoneCityName(zoneId),
                region = zoneId.substringBefore("/").replace("_", " ")
            )
        }
        .distinctBy { it.zoneId }
        .sortedBy { it.city.lowercase(Locale.getDefault()) }

private fun zoneCityName(zoneId: String): String =
    zoneId.substringAfter("/").substringAfterLast("/").replace("_", " ")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorldClockScreen(
    worldClockViewModel: WorldClockViewModel,
    settingsDataStore: SettingsDataStore,
    barsVisible: Boolean = true,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenRecycleBin: () -> Unit = {},
    onCanScrollChange: (Boolean) -> Unit = {},
    onScrollInProgressChange: (Boolean) -> Unit = {},
    fabInteractive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val savedZones by settingsDataStore.worldClockZonesFlow.collectAsState(initial = emptyList())
    val sortMode by settingsDataStore.worldClockSortFlow.collectAsState(
        initial = WorldClockSortMode.CUSTOM
    )
    val timeFormat by settingsDataStore.timeFormatFlow.collectAsState(
        initial = TimeFormat.TWELVE_HOUR
    )
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            val current = System.currentTimeMillis()
            value = current
            delay(1000L - current % 1000L)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var selectionActive by remember { mutableStateOf(false) }
    var selectedZones by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(selectionActive) { onSelectionModeChange(selectionActive) }

    val validZones = remember(savedZones) { savedZones.filter { it.contains("/") } }
    val displayZones = remember(savedZones, sortMode) {
        when (sortMode) {
            WorldClockSortMode.CITY_AZ -> savedZones
                .filter { it.contains("/") }
                .sortedBy { zoneCityName(it).lowercase(Locale.getDefault()) }
            WorldClockSortMode.CITY_ZA -> savedZones
                .filter { it.contains("/") }
                .sortedByDescending { zoneCityName(it).lowercase(Locale.getDefault()) }
            WorldClockSortMode.CUSTOM -> savedZones.filter { it.contains("/") }
        }
    }

    val isCustomMode = sortMode == WorldClockSortMode.CUSTOM
    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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

    val listZones = remember { mutableStateListOf<String>() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    var dragSettleJob by remember { mutableStateOf<Job?>(null) }

    var customPending by remember { mutableStateOf(false) }

    var pendingOrderIds by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(sortMode) {
        if (sortMode == WorldClockSortMode.CUSTOM) customPending = false
    }

    LaunchedEffect(pendingOrderIds) {
        val pending = pendingOrderIds ?: return@LaunchedEffect
        delay(3_000L)
        if (pendingOrderIds == pending) pendingOrderIds = null
    }

    LaunchedEffect(savedZones, sortMode) {
        val pending = pendingOrderIds
        if (pending != null) {
            if (displayZones == pending) {
                pendingOrderIds = null
            } else {
                return@LaunchedEffect
            }
        }
        if (draggingIndex == -1) {
            listZones.clear()
            listZones.addAll(displayZones)
        }
    }

    val effectiveCustom = isCustomMode || customPending
    val renderZones = if (effectiveCustom) listZones else displayZones

    val canScroll by remember {
        derivedStateOf {
            if (renderZones.isEmpty()) {
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

    val localZone = remember { ZoneId.systemDefault() }
    val localZonedDateTime = remember(nowMillis) { Instant.ofEpochMilli(nowMillis).atZone(localZone) }
    val heroTimeText = remember(localZonedDateTime, timeFormat) {
        if (timeFormat.is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", localZonedDateTime.hour, localZonedDateTime.minute, localZonedDateTime.second)
        } else {
            val h = if (localZonedDateTime.hour % 12 == 0) 12 else localZonedDateTime.hour % 12
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, localZonedDateTime.minute, localZonedDateTime.second)
        }
    }
    val heroAmPm = remember(localZonedDateTime, timeFormat) {
        if (timeFormat.is24Hour) "" else if (localZonedDateTime.hour < 12) "am" else "pm"
    }
    val heroTzName = remember(localZone) {
        localZone.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().edgeFadeMask(),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item(key = "world_clock_hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                        .padding(top = statusBarTop + 88.dp, bottom = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = heroTimeText,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (heroAmPm.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = heroAmPm,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = heroTzName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (renderZones.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No cities added",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add time zones to see them here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(renderZones, key = { _, zoneId -> zoneId }) { index, zoneId ->
                        val isDragging = effectiveCustom && draggingIndex == index

                        val dragModifier = if (selectionActive) {
                            Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) draggedDistance else 0f
                                }
                                .pointerInput(zoneId) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragSettleJob?.cancel()

                                            if (sortMode != WorldClockSortMode.CUSTOM) {
                                                customPending = true
                                                listZones.clear()
                                                listZones.addAll(displayZones)
                                                scope.launch {
                                                    settingsDataStore.setWorldClockSort(
                                                        WorldClockSortMode.CUSTOM
                                                    )
                                                }
                                            }
                                            draggingIndex = listZones.indexOfFirst { it == zoneId }
                                            draggedDistance = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedDistance += dragAmount.y
                                            val from = listZones.indexOfFirst { it == zoneId }
                                            if (from == -1) return@detectDragGesturesAfterLongPress

                                            val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == zoneId }
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
                                                if (from + 1 < listZones.size) {
                                                    val nextZone = listZones[from + 1]
                                                    val nextInfo = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == nextZone }
                                                    if (nextInfo != null && draggedCenterY > (nextInfo.offset + nextInfo.size * 0.5f)) {
                                                        val moved = listZones.removeAt(from)
                                                        listZones.add(from + 1, moved)
                                                        draggingIndex = from + 1
                                                        draggedDistance = draggedInfo.offset + draggedDistance - nextInfo.offset
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            } else if (dragAmount.y < 0f || draggedDistance < 0f) {
                                                if (from - 1 >= 0) {
                                                    val prevZone = listZones[from - 1]
                                                    val prevInfo = listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull { it.key == prevZone }
                                                    if (prevInfo != null && draggedCenterY < (prevInfo.offset + prevInfo.size * 0.5f)) {
                                                        val moved = listZones.removeAt(from)
                                                        listZones.add(from - 1, moved)
                                                        draggingIndex = from - 1
                                                        draggedDistance = draggedInfo.offset + draggedDistance - prevInfo.offset
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggingIndex != -1) {

                                                pendingOrderIds = listZones.toList()
                                                scope.launch {
                                                    settingsDataStore.setWorldClockZones(
                                                        listZones.toList()
                                                    )
                                                }
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
                                        }
                                    )
                                }
                        } else {
                            Modifier
                        }

                        WorldClockCard(
                            zoneId = zoneId,
                            nowMillis = nowMillis,
                            is24Hour = timeFormat.is24Hour,
                            selectionMode = selectionActive,
                            isSelected = selectedZones.contains(zoneId),
                            onToggleSelect = {
                                selectedZones = selectedZones.toggle(zoneId)
                            },
                            onLongClick = {

                                if (!selectionActive) {
                                    selectionActive = true
                                    selectedZones = selectedZones + zoneId
                                }
                            },
                            onRemove = {
                                worldClockViewModel.trashZones(listOf(zoneId))
                            },
                            modifier = if (isDragging) dragModifier else dragModifier.animateItem()
                        )
                    }
                }
        }

        AnimatedVisibility(
            visible = selectionActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            WorldClockSelectionBar(
                selectedCount = selectedZones.size,
                allSelected = renderZones.isNotEmpty() &&
                    selectedZones.containsAll(renderZones),
                onClose = {
                    selectionActive = false
                    selectedZones = emptySet()
                },
                onSelectAllToggle = {
                    selectedZones = if (selectedZones.containsAll(renderZones)) {
                        emptySet()
                    } else {
                        renderZones.toSet()
                    }
                },
                onRemove = {
                    val ids = selectedZones.toList()
                    worldClockViewModel.trashZones(ids)
                    selectionActive = false
                    selectedZones = emptySet()
                }
            )
        }

        AnimatedVisibility(
            visible = !selectionActive && barsVisible,
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
                    if (!fabInteractive) return@SmallFloatingActionButton
                    showAddDialog = true
                },
                containerColor = accentColor,
                contentColor = onColorFor(accentColor),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add city",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop && !selectionActive && barsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
                .zIndex(5f)
        ) {
            val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
            Surface(
                onClick = {
                    scope.launch {
                        if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) return@launch
                        if (listState.firstVisibleItemIndex > 6) {
                            listState.scrollToItem(6)
                        }
                        listState.animateScrollToItem(0, 0)
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
            visible = !selectionActive && barsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 96.dp)
                .zIndex(4f)
        ) {
            val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
            Box {
                SmallFloatingActionButton(
                    onClick = {
                        if (!fabInteractive) return@SmallFloatingActionButton
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
                        AppMenuItem("Recycle Bin", Icons.Outlined.DeleteOutline) { onOpenRecycleBin() },
                        AppMenuItem("Settings", Icons.Outlined.Settings) { onOpenSettings() }
                    )
                )
            }
        }
    }

    if (showAddDialog) {
        AddWorldClockDialog(
            is24Hour = timeFormat.is24Hour,
            nowMillis = nowMillis,
            onDismiss = { showAddDialog = false },
            onAdd = { zoneId ->
                scope.launch {
                    settingsDataStore.addWorldClockZone(zoneId)
                    if (sortMode != WorldClockSortMode.CUSTOM) {
                        settingsDataStore.setWorldClockSort(WorldClockSortMode.CUSTOM)
                    }
                }
                showAddDialog = false
            }
        )
    }

    if (showSortDialog) {
        WorldClockSortDialog(
            current = sortMode,
            onDismiss = { showSortDialog = false },
            onSelect = { mode ->
                scope.launch { settingsDataStore.setWorldClockSort(mode) }
                showSortDialog = false
            }
        )
    }
}

private fun Set<String>.toggle(zoneId: String): Set<String> =
    if (contains(zoneId)) this - zoneId else this + zoneId

@Composable
private fun WorldClockSortDialog(
    current: WorldClockSortMode,
    onDismiss: () -> Unit,
    onSelect: (WorldClockSortMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort cities by") },
        text = {
            Column {
                WorldClockSortMode.entries.forEach { mode ->
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
private fun WorldClockSelectionBar(
    selectedCount: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onRemove: () -> Unit
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
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                WorldClockSelectionAction(
                    icon = Icons.Outlined.SelectAll,
                    label = if (allSelected) "Deselect" else "Select",
                    onClick = onSelectAllToggle
                )
                WorldClockSelectionAction(
                    icon = Icons.Outlined.DeleteOutline,
                    label = "Remove",
                    onClick = onRemove
                )
            }
        }
    }
}

@Composable
private fun WorldClockSelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorldClockCard(
    zoneId: String,
    nowMillis: Long,
    is24Hour: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember(zoneId) { ZoneId.of(zoneId) }
    val now = remember(zone, nowMillis) {
        Instant.ofEpochMilli(nowMillis).atZone(zone)
    }
    val timeText = formatHourMinute(now.hour, now.minute, is24Hour)
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val cardShape = RoundedCornerShape(28.dp)
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isLight -> Color(0xF0FFFFFF)
        else -> Color(0xCC1E1F27)
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isLight -> Color(0x1F000000)
        else -> Color(0x1AFFFFFF)
    }
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val localZone = remember { ZoneId.systemDefault() }
    val targetOffsetSec = remember(now) { now.offset.totalSeconds }
    val localOffsetSec = remember(nowMillis) { localZone.rules.getOffset(Instant.ofEpochMilli(nowMillis)).totalSeconds }
    val diffSec = targetOffsetSec - localOffsetSec
    val diffHours = diffSec / 3600
    val diffMinutes = (kotlin.math.abs(diffSec) % 3600) / 60
    val offsetSign = if (diffSec >= 0) "+" else "-"
    val diffText = if (diffMinutes > 0) {
        "$offsetSign${kotlin.math.abs(diffHours)}h ${diffMinutes}m"
    } else if (diffHours == 0) {
        "0 hrs"
    } else {
        "$offsetSign${kotlin.math.abs(diffHours)} hrs"
    }
    val localDate = remember(nowMillis) { Instant.ofEpochMilli(nowMillis).atZone(localZone).toLocalDate() }
    val targetDate = now.toLocalDate()
    val dayText = when {
        targetDate.isEqual(localDate) -> "Today"
        targetDate.isAfter(localDate) -> "Tomorrow"
        else -> "Yesterday"
    }
    val subtitleText = "$dayText, $diffText"

    FrostedBlurBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onLongClick
            ),
        shape = cardShape,
        tintColor = if (isSelected) MaterialTheme.colorScheme.primary else null,
        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zoneCityName(zoneId),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitleText,
                    fontSize = 14.sp,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    if (!is24Hour) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = amPmSuffix(now.hour, is24Hour),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                if (selectionMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectionCheckbox(
                        checked = isSelected,
                        onToggle = onToggleSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun AddWorldClockDialog(
    is24Hour: Boolean,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allCities = remember { buildWorldCities() }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val filtered = remember(query, allCities) {
        if (query.isBlank()) {
            allCities
        } else {
            allCities.filter {
                it.city.contains(query, ignoreCase = true) ||
                    it.region.contains(query, ignoreCase = true) ||
                    it.zoneId.contains(query, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        if (dialogWindow != null) {
            DisposableEffect(isLight) {
                val insetsController = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
                insetsController.isAppearanceLightStatusBars = isLight
                onDispose {}
            }
        }

        FrostedBlurBox(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .heightIn(min = 380.dp, max = 580.dp),
            shape = RoundedCornerShape(28.dp),
            tintColor = if (isLight) Color(0xF5FFFFFF) else Color(0xE61E1F27),
            borderColor = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add City",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isLight) Color(0x12000000) else Color(0x20FFFFFF),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    color = if (isLight) Color(0x0C000000) else Color(0x18FFFFFF),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search city, region or timezone",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No cities found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (query.isBlank()) {
                            val grouped = filtered.groupBy { it.city.first().uppercaseChar() }
                            grouped.keys.sorted().forEach { letter ->
                                item(key = "header_$letter") {
                                    Text(
                                        text = letter.toString(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                items(grouped[letter].orEmpty(), key = { it.zoneId }) { city ->
                                    CityListItem(
                                        city = city,
                                        is24Hour = is24Hour,
                                        nowMillis = nowMillis,
                                        isLight = isLight,
                                        onSelect = { onAdd(city.zoneId) }
                                    )
                                }
                            }
                        } else {
                            items(filtered, key = { it.zoneId }) { city ->
                                CityListItem(
                                    city = city,
                                    is24Hour = is24Hour,
                                    nowMillis = nowMillis,
                                    isLight = isLight,
                                    onSelect = { onAdd(city.zoneId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityListItem(
    city: WorldCity,
    is24Hour: Boolean,
    nowMillis: Long,
    isLight: Boolean,
    onSelect: () -> Unit
) {
    val cityTime = remember(city.zoneId, nowMillis, is24Hour) {
        runCatching {
            val zone = ZoneId.of(city.zoneId)
            val zdt = Instant.ofEpochMilli(nowMillis).atZone(zone)
            formatHourMinute(zdt.hour, zdt.minute, is24Hour) + if (!is24Hour) " " + amPmSuffix(zdt.hour, is24Hour) else ""
        }.getOrDefault("")
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.city,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${city.region} \u00B7 ${city.zoneId}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cityTime.isNotBlank()) {
                    Text(
                        text = cityTime,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
