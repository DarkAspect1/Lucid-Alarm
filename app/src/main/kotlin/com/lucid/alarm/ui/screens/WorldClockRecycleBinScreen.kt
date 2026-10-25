package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.lucid.alarm.ui.components.SelectionCheckbox
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.formatHourMinute
import com.lucid.alarm.ui.viewmodel.WorldClockViewModel
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale

/**
 * Dedicated recycle bin for the world-clock tab. Cities removed from the World Clock are held here
 * for three days before being purged, and can be restored or deleted permanently.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorldClockRecycleBinScreen(
    viewModel: WorldClockViewModel,
    is24Hour: Boolean = true,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val trashed by viewModel.trashedZones.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    fun toggle(id: String) {
        if (selected.contains(id)) selected.remove(id) else selected.add(id)
    }

    fun exitSelection() {
        selectionMode = false
        selected.clear()
    }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val listState = rememberLazyListState()

    val showFloatingBack by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 48
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

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().edgeFadeMask(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 112.dp
            )
        ) {
            item(key = "world_clock_bin_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                        .padding(top = statusBarTop)
                ) {
                    WorldClockBinHeader(
                        onBack = onBack,
                        itemCount = trashed.size,
                        selectionCount = if (selectionMode) selected.size else null
                    )
                }
            }

            if (trashed.isEmpty()) {
                item(key = "worldClockBinEmpty") {
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

            items(trashed, key = { it.zoneId }) { entry ->
                TrashedZoneCard(
                    zoneId = entry.zoneId,
                    deletedAt = entry.deletedAt,
                    is24Hour = is24Hour,
                    selectionMode = selectionMode,
                    selected = selected.contains(entry.zoneId),
                    onToggleSelect = { toggle(entry.zoneId) },
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selected.add(entry.zoneId)
                        }
                    },
                    onRestore = { viewModel.restoreZones(listOf(entry.zoneId)) },
                    onDelete = { viewModel.deletePermanently(listOf(entry.zoneId)) }
                )
            }
        }


        WorldClockBinBottomBar(
            selectionMode = selectionMode,
            selectedCount = selected.size,
            allSelected = trashed.isNotEmpty() &&
                selected.containsAll(trashed.map { it.zoneId }),
            hasEntries = trashed.isNotEmpty(),
            onSelect = { selectionMode = true },
            onCancel = { exitSelection() },
            onSelectAllToggle = {
                val allIds = trashed.map { it.zoneId }
                if (selected.containsAll(allIds)) {
                    selected.clear()
                } else {
                    selected.clear()
                    selected.addAll(allIds)
                }
            },
            onRestore = {
                if (selectionMode) {
                    viewModel.restoreZones(selected.toList())
                    exitSelection()
                } else {
                    viewModel.restoreZones(trashed.map { it.zoneId })
                }
            },
            onDelete = {
                if (selectionMode) {
                    viewModel.deletePermanently(selected.toList())
                    exitSelection()
                } else {
                    showDeleteAllDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete all cities?") },
            text = {
                Text("Permanently delete all ${trashed.size} trashed cities? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showDeleteAllDialog = false
                }) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WorldClockBinHeader(
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
private fun TrashedZoneCard(
    zoneId: String,
    deletedAt: Long,
    is24Hour: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit = {},
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrNull() }
    val now = remember(zone) { zone?.let { ZonedDateTime.now(it) } }
    val time = now?.let { formatHourMinute(it.hour, it.minute, is24Hour) } ?: "--:--"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() },
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    SelectionCheckbox(checked = selected, onToggle = onToggleSelect)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clockCityName(zoneId),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = zoneId.substringBefore("/").replace("_", " ") + " Â· " + zoneId,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (deletedAt > 0L) {
                        Text(
                            text = "Trashed: " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(Date(deletedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            if (!selectionMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRestore) {
                        Icon(
                            Icons.Filled.RestoreFromTrash,
                            contentDescription = "Restore",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}


@Composable
private fun WorldClockBinBottomBar(
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
                WorldClockBinBarAction(
                    icon = Icons.Outlined.SelectAll,
                    label = if (allSelected) "Deselect" else "Select all",
                    enabled = hasEntries,
                    onClick = onSelectAllToggle
                )
                WorldClockBinBarAction(
                    icon = Icons.Filled.RestoreFromTrash,
                    label = "Restore",
                    enabled = selectedCount > 0,
                    onClick = onRestore
                )
                WorldClockBinBarAction(
                    icon = Icons.Filled.Delete,
                    label = "Delete",
                    enabled = selectedCount > 0,
                    onClick = onDelete
                )
                WorldClockBinBarAction(
                    icon = Icons.Outlined.Close,
                    label = "Cancel",
                    enabled = true,
                    onClick = onCancel
                )
            } else {
                WorldClockBinBarAction(
                    icon = Icons.Outlined.SelectAll,
                    label = "Select",
                    enabled = hasEntries,
                    onClick = onSelect
                )
                WorldClockBinBarAction(
                    icon = Icons.Filled.RestoreFromTrash,
                    label = "Restore all",
                    enabled = hasEntries,
                    onClick = onRestore
                )
                WorldClockBinBarAction(
                    icon = Icons.Filled.Delete,
                    label = "Delete all",
                    enabled = hasEntries,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun WorldClockBinBarAction(
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

private fun clockCityName(zoneId: String): String =
    zoneId.substringAfter("/").substringAfterLast("/").replace("_", " ")
