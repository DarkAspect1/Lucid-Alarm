package com.lucid.alarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.produceState
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
import com.lucid.alarm.ui.amPmSuffix
import com.lucid.alarm.ui.components.FrostedBlurBox
import com.lucid.alarm.ui.components.SelectionCheckbox
import com.lucid.alarm.ui.components.edgeFadeMask
import com.lucid.alarm.ui.formatHourMinute
import com.lucid.alarm.ui.viewmodel.WorldClockViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalFoundationApi::class)
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
    val scope = rememberCoroutineScope()

    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            val current = System.currentTimeMillis()
            value = current
            delay(1000L - current % 1000L)
        }
    }

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
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 140
        }
    }
    val floatingBackAlpha by animateFloatAsState(
        targetValue = if (showFloatingBack) 1f else 0f,
        animationSpec = tween(180),
        label = "floatingBackAlpha"
    )

    val showScrollToTop by remember {
        derivedStateOf {
            val pastThreshold = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 220
            pastThreshold && !listState.isScrollInProgress
        }
    }

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
            } else {
                items(trashed, key = { it.zoneId }) { entry ->
                    val isSelected = selected.contains(entry.zoneId)
                    TrashedWorldClockCard(
                        zoneId = entry.zoneId,
                        nowMillis = nowMillis,
                        is24Hour = is24Hour,
                        selectionMode = selectionMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (selectionMode) {
                                toggle(entry.zoneId)
                            } else {
                                selectionMode = true
                                selected.add(entry.zoneId)
                            }
                        },
                        onLongClick = {
                            if (!selectionMode) {
                                selectionMode = true
                                selected.add(entry.zoneId)
                            }
                        }
                    )
                }
            }
        }

        WorldClockBinBottomBar(
            selectionMode = selectionMode,
            selectedCount = selected.size,
            allSelected = trashed.isNotEmpty() && selected.containsAll(trashed.map { it.zoneId }),
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
    }

    if (showDeleteAllDialog) {
        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
        val dialogBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF17171C)
        val dialogContentColor = if (isLight) Color(0xFF111318) else Color(0xFFFFFFFF)
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = dialogBg,
            titleContentColor = dialogContentColor,
            textContentColor = dialogContentColor,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashedWorldClockCard(
    zoneId: String,
    nowMillis: Long,
    is24Hour: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrNull() }
    val now = remember(zone, nowMillis) {
        zone?.let { Instant.ofEpochMilli(nowMillis).atZone(it) }
    }
    val timeText = if (now != null) formatHourMinute(now.hour, now.minute, is24Hour) else "--:--"

    val cardShape = RoundedCornerShape(28.dp)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val localZone = remember { ZoneId.systemDefault() }
    val subtitleText = remember(now, nowMillis) {
        if (now == null) ""
        else {
            val targetOffsetSec = now.offset.totalSeconds
            val localOffsetSec = localZone.rules.getOffset(Instant.ofEpochMilli(nowMillis)).totalSeconds
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
            val localDate = Instant.ofEpochMilli(nowMillis).atZone(localZone).toLocalDate()
            val targetDate = now.toLocalDate()
            val dayText = when {
                targetDate.isEqual(localDate) -> "Today"
                targetDate.isAfter(localDate) -> "Tomorrow"
                else -> "Yesterday"
            }
            "$dayText, $diffText"
        }
    }

    FrostedBlurBox(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onToggleSelect,
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
                    text = clockCityName(zoneId),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitleText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitleText,
                        fontSize = 14.sp,
                        color = mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    if (!is24Hour && now != null) {
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

private fun clockCityName(zoneId: String): String =
    zoneId.substringAfter("/").substringAfterLast("/").replace("_", " ")

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
