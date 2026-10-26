package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SelectAll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.ui.components.AlarmCard
import com.lucid.alarm.ui.components.GroupCapsulePill
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch

private sealed interface TrashedItem {
    val key: String
    val deletedAt: Long
    val alarmIds: List<Int>

    data class Group(
        val groupName: String,
        val alarms: List<AlarmEntity>,
        override val deletedAt: Long
    ) : TrashedItem {
        override val key: String get() = "group_$groupName"
        override val alarmIds: List<Int> get() = alarms.map { it.id }
        val enabledCount: Int get() = alarms.count { it.isEnabled }
        val disabledCount: Int get() = alarms.size - enabledCount
    }

    data class StandaloneAlarm(
        val alarm: AlarmEntity
    ) : TrashedItem {
        override val key: String get() = "alarm_${alarm.id}"
        override val deletedAt: Long get() = alarm.deletedAt
        override val alarmIds: List<Int> get() = listOf(alarm.id)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecycleBinScreen(
    viewModel: AlarmViewModel,
    is24Hour: Boolean = true,
    removeLeadingZero: Boolean = false,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val trashed by viewModel.trashedAlarms.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val trashedItems = remember(trashed) {
        val groups = trashed.filter { it.trashedGroup.isNotBlank() }
            .groupBy { it.trashedGroup }
            .map { (groupName, groupAlarms) ->
                TrashedItem.Group(
                    groupName = groupName,
                    alarms = groupAlarms,
                    deletedAt = groupAlarms.maxOfOrNull { it.deletedAt } ?: 0L
                )
            }
        val standalone = trashed.filter { it.trashedGroup.isBlank() }
            .map { TrashedItem.StandaloneAlarm(it) }

        (groups + standalone).sortedByDescending { it.deletedAt }
    }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val selectedItemsCount = trashedItems.count { item ->
        when (item) {
            is TrashedItem.Group -> item.alarms.isNotEmpty() && item.alarms.all { selectedIds.contains(it.id) }
            is TrashedItem.StandaloneAlarm -> selectedIds.contains(item.alarm.id)
        }
    }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val showFloatingBack by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
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
                    onClick = onBack,
                    enabled = showFloatingBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isLight) Color(0xCCFFFFFF) else Color(0xCC282932),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().edgeFadeMask(),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            item(key = "recycle_bin_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                        .padding(top = statusBarTop)
                ) {
                    RecycleBinHeader(
                        onBack = onBack,
                        itemCount = trashedItems.size,
                        selectionCount = if (selectionMode) selectedItemsCount else null
                    )
                }
            }

            if (trashedItems.isEmpty()) {
                item(key = "recycleBinEmpty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Recycle bin is empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val totalCount = trashedItems.size
                itemsIndexed(trashedItems, key = { _, item -> item.key }) { index, item ->
                    val isSelected = when (item) {
                        is TrashedItem.Group -> item.alarms.isNotEmpty() && item.alarms.all { selectedIds.contains(it.id) }
                        is TrashedItem.StandaloneAlarm -> selectedIds.contains(item.alarm.id)
                    }
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

                    when (item) {
                        is TrashedItem.Group -> {
                            GroupCapsulePill(
                                label = item.groupName,
                                selected = isSelected,
                                counts = item.enabledCount to item.disabledCount,
                                onClick = {
                                    val groupIds = item.alarms.map { it.id }.toSet()
                                    if (selectionMode) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - groupIds
                                        } else {
                                            selectedIds + groupIds
                                        }
                                    } else {
                                        selectionMode = true
                                        selectedIds = groupIds
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedIds = item.alarms.map { it.id }.toSet()
                                    }
                                },
                                cardShape = cardShape,
                                selectionMode = selectionMode,
                                showBottomDivider = showBottomDivider,
                                connectedIndex = index,
                                totalConnected = totalCount,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(cardPadding)
                            )
                        }
                        is TrashedItem.StandaloneAlarm -> {
                            AlarmCard(
                                alarm = item.alarm,
                                selectionMode = selectionMode,
                                isSelected = isSelected,
                                is24Hour = is24Hour,
                                removeLeadingZero = removeLeadingZero,
                                onToggle = {
                                    selectedIds = if (selectedIds.contains(item.alarm.id)) {
                                        selectedIds - item.alarm.id
                                    } else {
                                        selectedIds + item.alarm.id
                                    }
                                },
                                onClick = {
                                    if (selectionMode) {
                                        selectedIds = if (selectedIds.contains(item.alarm.id)) {
                                            selectedIds - item.alarm.id
                                        } else {
                                            selectedIds + item.alarm.id
                                        }
                                    } else {
                                        selectionMode = true
                                        selectedIds = setOf(item.alarm.id)
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        selectionMode = true
                                        selectedIds = setOf(item.alarm.id)
                                    }
                                },
                                cardShape = cardShape,
                                cardPadding = cardPadding,
                                showBottomDivider = showBottomDivider,
                                connectedIndex = index,
                                totalConnected = totalCount,
                                showSwitch = false
                            )
                        }
                    }
                }
            }
        }

        RecycleBinBottomBar(
            selectionMode = selectionMode,
            selectedCount = selectedItemsCount,
            allSelected = trashedItems.isNotEmpty() && selectedItemsCount == trashedItems.size,
            hasEntries = trashedItems.isNotEmpty(),
            onSelect = { selectionMode = true },
            onCancel = { exitSelection() },
            onSelectAllToggle = {
                val allIds = trashed.map { it.id }.toSet()
                if (selectedIds.containsAll(allIds)) {
                    selectedIds = emptySet()
                } else {
                    selectedIds = allIds
                }
            },
            onRestore = {
                if (selectionMode) {
                    viewModel.restoreFromTrash(selectedIds.toList())
                    exitSelection()
                } else {
                    viewModel.restoreFromTrash(trashed.map { it.id })
                }
            },
            onDelete = {
                if (selectionMode) {
                    viewModel.deletePermanently(selectedIds.toList())
                    exitSelection()
                } else {
                    showDeleteAllDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        )

        AnimatedVisibility(
            visible = showScrollToTop && !selectionMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
                .zIndex(5f)
        ) {
            Surface(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
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

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Empty recycle bin?") },
                text = { Text("Permanently delete all ${trashedItems.size} trashed item${if (trashedItems.size == 1) "" else "s"}? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.emptyRecycleBin()
                        exitSelection()
                        showDeleteAllDialog = false
                    }) {
                        Text("Delete all")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun RecycleBinHeader(
    onBack: () -> Unit,
    itemCount: Int,
    selectionCount: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Recycle Bin",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = if (selectionCount != null) {
                    "$selectionCount selected"
                } else {
                    "$itemCount item" + if (itemCount == 1) "" else "s"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecycleBinBottomBar(
    selectionMode: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    hasEntries: Boolean,
    onSelect: () -> Unit,
    onCancel: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Surface(
        border = null,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
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
            if (selectionMode) {
                RecycleBinBarAction(
                    icon = Icons.Outlined.SelectAll,
                    label = if (allSelected) "Deselect" else "Select all",
                    enabled = hasEntries,
                    onClick = onSelectAllToggle
                )
                RecycleBinBarAction(
                    icon = Icons.Filled.RestoreFromTrash,
                    label = "Restore",
                    enabled = selectedCount > 0,
                    onClick = onRestore
                )
                RecycleBinBarAction(
                    icon = Icons.Outlined.DeleteOutline,
                    label = "Delete",
                    enabled = selectedCount > 0,
                    onClick = onDelete
                )
                RecycleBinBarAction(
                    icon = Icons.Outlined.Close,
                    label = "Cancel",
                    enabled = true,
                    onClick = onCancel
                )
            } else {
                RecycleBinBarAction(
                    icon = Icons.Outlined.SelectAll,
                    label = "Select",
                    enabled = hasEntries,
                    onClick = onSelect
                )
                RecycleBinBarAction(
                    icon = Icons.Filled.RestoreFromTrash,
                    label = "Restore all",
                    enabled = hasEntries,
                    onClick = onRestore
                )
                RecycleBinBarAction(
                    icon = Icons.Outlined.DeleteOutline,
                    label = "Delete all",
                    enabled = hasEntries,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun RecycleBinBarAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.35f)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
