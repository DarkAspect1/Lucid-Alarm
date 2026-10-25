package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import com.lucid.alarm.ui.components.SelectionCheckbox
import com.lucid.alarm.ui.components.edgeFadeMask
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lucid.alarm.data.AlarmEntity
import com.lucid.alarm.ui.amPmSuffix
import com.lucid.alarm.ui.formatHourMinute
import com.lucid.alarm.ui.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface TrashEntry {
    val key: String
    val ids: List<Int>

    data class Group(val name: String, val members: List<AlarmEntity>) : TrashEntry {
        override val key: String = "group:$name"
        override val ids: List<Int> = members.map { it.id }
    }

    data class Single(val alarm: AlarmEntity) : TrashEntry {
        override val key: String = "single:${alarm.id}"
        override val ids: List<Int> = listOf(alarm.id)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val groups = remember(trashed) {
        trashed.filter { it.trashedGroup.isNotBlank() }
            .groupBy { it.trashedGroup }
            .map { (name, members) -> TrashEntry.Group(name, members) }
    }
    val singles = remember(trashed) {
        trashed.filter { it.trashedGroup.isBlank() }.map { TrashEntry.Single(it) }
    }
    val entries: List<TrashEntry> = groups + singles

    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var isScrollingUp by remember { mutableStateOf(false) }
    var prevIndex by remember { mutableIntStateOf(gridState.firstVisibleItemIndex) }
    var prevOffset by remember { mutableIntStateOf(gridState.firstVisibleItemScrollOffset) }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        val curIndex = gridState.firstVisibleItemIndex
        val curOffset = gridState.firstVisibleItemScrollOffset
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
            val pastThreshold = gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 220
            pastThreshold && (!gridState.isScrollInProgress || isScrollingUp)
        }
    }

    fun toggle(key: String) {
        if (selected.contains(key)) selected.remove(key) else selected.add(key)
    }

    fun exitSelection() {
        selectionMode = false
        selected.clear()
    }

    fun restoreEntries(keys: List<String>) {
        keys.forEach { key ->
            when (val entry = entries.firstOrNull { it.key == key }) {
                is TrashEntry.Group -> viewModel.restoreGroupFromTrash(entry.name)
                is TrashEntry.Single -> viewModel.restoreFromTrash(listOf(entry.alarm.id))
                null -> Unit
            }
        }
    }

    fun deleteEntries(keys: List<String>) {
        keys.forEach { key ->
            when (val entry = entries.firstOrNull { it.key == key }) {
                is TrashEntry.Group -> viewModel.deleteGroupPermanently(entry.name)
                is TrashEntry.Single -> viewModel.deletePermanently(listOf(entry.alarm.id))
                null -> Unit
            }
        }
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val showFloatingBack by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 48
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Floating Back Button: Appears only when scrolled and header disappears
        AnimatedVisibility(
            visible = showFloatingBack,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .padding(top = statusBarTop + 4.dp, start = 12.dp)
                .zIndex(10f)
        ) {
            IconButton(
                onClick = onBack,
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

        // Strict two-group grid: group cards always flow exactly two per row; single alarms
        // follow below spanning the full width so the time stays readable.
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().edgeFadeMask(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 112.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "recycle_bin_header", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                        .padding(top = statusBarTop)
                ) {
                    RecycleBinHeader(
                        onBack = onBack,
                        itemCount = trashed.size,
                        selectionCount = if (selectionMode) selected.size else null
                    )
                }
            }

            if (entries.isEmpty()) {
                item(key = "recycleBinEmpty", span = { GridItemSpan(maxLineSpan) }) {
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
            }

            gridItems(groups, key = { it.key }) { entry ->
                TrashGroupCard(
                    name = entry.name,
                    members = entry.members,
                    is24Hour = is24Hour,
                    removeLeadingZero = removeLeadingZero,
                    selectionMode = selectionMode,
                    selected = selected.contains(entry.key),
                    onToggleSelect = { toggle(entry.key) },
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selected.add(entry.key)
                        }
                    },
                    onRestore = { viewModel.restoreGroupFromTrash(entry.name) },
                    onDelete = { viewModel.deleteGroupPermanently(entry.name) }
                )
            }

            gridItems(
                singles,
                key = { it.key },
                span = { GridItemSpan(maxLineSpan) }
            ) { entry ->
                TrashedAlarmCard(
                    alarm = entry.alarm,
                    is24Hour = is24Hour,
                    removeLeadingZero = removeLeadingZero,
                    selectionMode = selectionMode,
                    selected = selected.contains(entry.key),
                    onToggleSelect = { toggle(entry.key) },
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selected.add(entry.key)
                        }
                    },
                    onRestore = { viewModel.restoreFromTrash(listOf(entry.alarm.id)) },
                    onDelete = { viewModel.deletePermanently(listOf(entry.alarm.id)) }
                )
            }
        }

        RecycleBinBottomBar(
            selectionMode = selectionMode,
            selectedCount = selected.size,
            allSelected = entries.isNotEmpty() && selected.containsAll(entries.map { it.key }),
            hasEntries = entries.isNotEmpty(),
            onSelect = { selectionMode = true },
            onCancel = { exitSelection() },
            onSelectAllToggle = {
                val allKeys = entries.map { it.key }
                if (selected.containsAll(allKeys)) {
                    selected.clear()
                } else {
                    selected.clear()
                    selected.addAll(allKeys)
                }
            },
            onRestore = {
                if (selectionMode) {
                    restoreEntries(selected.toList())
                    exitSelection()
                } else {
                    restoreEntries(entries.map { it.key })
                }
            },
            onDelete = {
                if (selectionMode) {
                    deleteEntries(selected.toList())
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
            val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
            Surface(
                onClick = { scope.launch { gridState.animateScrollToItem(0) } },
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
                text = { Text("Permanently delete all ${trashed.size} trashed alarms? This cannot be undone.") },
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
private fun TrashCountChip(
    icon: ImageVector,
    count: Int,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TrashGroupCard(
    name: String,
    members: List<AlarmEntity>,
    is24Hour: Boolean,
    removeLeadingZero: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val active = members.count { it.isEnabled }
    val inactive = members.size - active
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrashCountChip(
                            icon = Icons.Outlined.NotificationsActive,
                            count = active,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                        TrashCountChip(
                            icon = Icons.Outlined.NotificationsOff,
                            count = inactive,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!selectionMode) {
                    IconButton(onClick = onRestore, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.RestoreFromTrash,
                            contentDescription = "Restore group",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                    SelectionCheckbox(checked = selected, onToggle = onToggleSelect)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            members.take(2).forEach { member ->
                Text(
                    text = if (member.label.isNotBlank()) member.label else "Alarm",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (members.size > 2) {
                Text(
                    text = "+${members.size - 2} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!selectionMode) {
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TrashedAlarmCard(
    alarm: AlarmEntity,
    is24Hour: Boolean,
    removeLeadingZero: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val time = formatHourMinute(alarm.hour, alarm.minute, is24Hour, removeLeadingZero)
    val trashedDate = rememberTrashedDate(alarm.deletedAt)
    val selectedNow = selectionMode && selected
    val container = if (selectedNow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selectedNow) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryColor = if (selectedNow) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = container,
            contentColor = contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (alarm.label.isNotBlank()) alarm.label else "Alarm",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (selectedNow) {
                            contentColor
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp
                            ),
                            color = contentColor,
                            modifier = Modifier.alignByBaseline()
                        )
                        if (!is24Hour) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = amPmSuffix(alarm.hour, is24Hour),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = contentColor.copy(alpha = 0.85f),
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                    if (trashedDate.isNotBlank()) {
                        Text(
                            text = "Trashed: $trashedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor
                        )
                    }
                }
                if (!selectionMode) {
                    IconButton(onClick = onRestore, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.RestoreFromTrash,
                            contentDescription = "Restore",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                    SelectionCheckbox(checked = selected, onToggle = onToggleSelect)
                }
            }
            if (!selectionMode) {
                Spacer(modifier = Modifier.height(2.dp))
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun rememberTrashedDate(deletedAt: Long): String {
    if (deletedAt <= 0) return ""
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(deletedAt))
}

/**
 * Persistent action bar shared by the idle and selection states. Keeping one bar mounted means the
 * restore/delete affordances never jump or leave the bottom edge unoccupied, even when the bin is
 * empty (the actions simply dim instead of disappearing).
 */
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

