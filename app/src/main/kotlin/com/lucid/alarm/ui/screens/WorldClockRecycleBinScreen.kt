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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import com.lucid.alarm.ui.components.SelectionBarContainer
import com.lucid.alarm.ui.components.SelectionIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lucid.alarm.R
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
import androidx.compose.ui.platform.LocalContext
import com.lucid.alarm.ui.amPmSuffix
import com.lucid.alarm.ui.WorldClockLocalization
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
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
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
                            text = stringResource(R.string.recycle_bin_empty),
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
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.action_back_to_top),
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
            title = { Text(stringResource(R.string.dialog_delete_all_cities)) },
            text = {
                Text(stringResource(R.string.dialog_delete_all_cities_msg, trashed.size))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showDeleteAllDialog = false
                }) {
                    Text(stringResource(R.string.action_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.action_cancel)) }
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
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.recycle_bin),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = if (selectionCount != null) {
                    stringResource(R.string.count_selected, selectionCount)
                } else {
                    stringResource(R.string.count_items, itemCount)
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
    val context = LocalContext.current
    val zone = remember(zoneId) { runCatching { ZoneId.of(zoneId) }.getOrNull() }
    val now = remember(zone, nowMillis) {
        zone?.let { Instant.ofEpochMilli(nowMillis).atZone(it) }
    }
    val timeText = if (now != null) formatHourMinute(now.hour, now.minute, is24Hour) else "--:--"

    val cardShape = RoundedCornerShape(28.dp)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    val localZone = remember { ZoneId.systemDefault() }
    val cityName = remember(zoneId, context) {
        WorldClockLocalization.getLocalizedCity(zoneId, context)
    }
    val subtitleText = remember(now, nowMillis, context) {
        if (now == null || zone == null) ""
        else WorldClockLocalization.formatWorldClockSubtitle(nowMillis, zone, localZone, context)
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
                    text = cityName,
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
                            text = amPmSuffix(now.hour, is24Hour, context),
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
    WorldClockLocalization.rawCityName(zoneId)

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
    SelectionBarContainer(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Surface(
            border = null,
            color = if (lightMode) {
                Color(0xFFFFFFFF)
            } else {
                Color(0xFF2C2D35)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (selectionMode) {
                    SelectionIconButton(
                        icon = Icons.Rounded.SelectAll,
                        label = if (allSelected) stringResource(R.string.action_deselect) else stringResource(R.string.action_select_all),
                        enabled = hasEntries,
                        onClick = onSelectAllToggle
                    )
                    SelectionIconButton(
                        icon = Icons.Rounded.RestoreFromTrash,
                        label = stringResource(R.string.action_restore),
                        enabled = selectedCount > 0,
                        onClick = onRestore
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
                } else {
                    SelectionIconButton(
                        icon = Icons.Rounded.SelectAll,
                        label = stringResource(R.string.action_select),
                        enabled = hasEntries,
                        onClick = onSelect
                    )
                    SelectionIconButton(
                        icon = Icons.Rounded.RestoreFromTrash,
                        label = stringResource(R.string.action_restore_all),
                        enabled = hasEntries,
                        onClick = onRestore
                    )
                    SelectionIconButton(
                        icon = Icons.Rounded.Delete,
                        label = stringResource(R.string.action_delete_all),
                        enabled = hasEntries,
                        onClick = onDelete
                    )
                }
            }
        }
    }
}
