package com.lucid.alarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.util.Locale
import kotlin.math.abs

/**
 * Vertical wheel time picker with a strict 1:1 mapping between the centered drum item and the
 * value that is emitted.
 *
 * Why this has no off-by-one error:
 *  - [LazyColumn] uses a vertical `contentPadding` of exactly `itemHeight * (visibleCount / 2)`.
 *  - Therefore, when the fling settles, the item whose center aligns with the viewport center is
 *    always the item at `firstVisibleItemIndex` with a zero pixel offset.
 *  - The selected index is detected by choosing the visible item whose center is physically
 *    closest to the viewport center, then normalising with a non-negative modulo into `0 until size`.
 *  - The emitted hour/minute is the item value itself; nothing is ever decremented.
 */
@Composable
fun VerticalWheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean = true,
    onTimeChanged: (hour24: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeInitialHour = initialHour.coerceIn(0, 23)
    val safeInitialMinute = initialMinute.coerceIn(0, 59)

    var selectedHour24 by remember { mutableIntStateOf(safeInitialHour) }
    var selectedMinute by remember { mutableIntStateOf(safeInitialMinute) }
    var isPm by remember { mutableIntStateOf(if (safeInitialHour >= 12) 1 else 0) }

    val hourItems = remember(is24Hour) {
        if (is24Hour) (0..23).toList() else (1..12).toList()
    }
    val minuteItems = remember { (0..59).toList() }

    val initialDisplayHour = remember(safeInitialHour, is24Hour) {
        if (is24Hour) {
            safeInitialHour
        } else {
            when {
                safeInitialHour == 0 -> 12
                safeInitialHour > 12 -> safeInitialHour - 12
                else -> safeInitialHour
            }
        }
    }

    val initialHourIndex = remember(hourItems, initialDisplayHour) {
        hourItems.indexOf(initialDisplayHour).coerceAtLeast(0)
    }
    val initialMinuteIndex = remember(minuteItems, safeInitialMinute) {
        minuteItems.indexOf(safeInitialMinute).coerceAtLeast(0)
    }

    fun emitUpdatedTime(displayHour: Int, minute: Int, pmFlag: Int) {
        val resolvedHour24 = if (is24Hour) {
            displayHour.coerceIn(0, 23)
        } else {
            val h12 = displayHour.coerceIn(1, 12)
            if (pmFlag == 1) {
                if (h12 == 12) 12 else h12 + 12
            } else {
                if (h12 == 12) 0 else h12
            }
        }
        selectedHour24 = resolvedHour24
        selectedMinute = minute.coerceIn(0, 59)
        onTimeChanged(selectedHour24, selectedMinute)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(
                    items = hourItems,
                    initialIndex = initialHourIndex,
                    itemFormatter = { value -> String.format(Locale.getDefault(), "%02d", value) },
                    onItemSelected = { index ->
                        emitUpdatedTime(hourItems[index], selectedMinute, isPm)
                    },
                    modifier = Modifier.width(88.dp)
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,

                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                WheelColumn(
                    items = minuteItems,
                    initialIndex = initialMinuteIndex,
                    itemFormatter = { value -> String.format(Locale.getDefault(), "%02d", value) },
                    onItemSelected = { index ->
                        val currentDisplayHour = if (is24Hour) {
                            selectedHour24
                        } else {
                            when {
                                selectedHour24 == 0 -> 12
                                selectedHour24 > 12 -> selectedHour24 - 12
                                else -> selectedHour24
                            }
                        }
                        emitUpdatedTime(currentDisplayHour, minuteItems[index], isPm)
                    },
                    modifier = Modifier.width(88.dp)
                )

                if (!is24Hour) {
                    val amPmItems = remember { listOf("AM", "PM") }
                    WheelColumn(
                        items = amPmItems,
                        initialIndex = isPm,
                        infiniteLoop = false,
                        itemFormatter = { it },
                        onItemSelected = { index ->
                            isPm = index.coerceIn(0, 1)
                            val currentDisplayHour = when {
                                selectedHour24 == 0 -> 12
                                selectedHour24 > 12 -> selectedHour24 - 12
                                else -> selectedHour24
                            }
                            emitUpdatedTime(currentDisplayHour, selectedMinute, isPm)
                        },
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .width(72.dp)
                    )
                }
            }
        }
    }
}

/**
 * Simplified numeric wheel for use outside the time picker (e.g. the Timer screen).
 */
@Composable
fun NumberWheel(
    values: List<Int>,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedFontSize: TextUnit = 28.sp,
    unselectedFontSize: TextUnit = 20.sp,
    unselectedAlpha: Float = 0.45f,
    itemHeight: Dp = 56.dp,
    itemFormat: (Int) -> String = { String.format(Locale.getDefault(), "%02d", it) },
    listState: LazyListState? = null
) {
    val initialIndex = remember(values, initialValue) {
        values.indexOf(initialValue).coerceAtLeast(0)
    }
    WheelColumn(
        items = values,
        initialIndex = initialIndex,
        itemFormatter = itemFormat,
        onItemSelected = { index -> onValueChange(values[index]) },
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        selectedFontSize = selectedFontSize,
        unselectedFontSize = unselectedFontSize,
        unselectedAlpha = unselectedAlpha,
        itemHeight = itemHeight,
        modifier = modifier,
        externalListState = listState
    )
}

fun wheelScrollIndex(itemCount: Int, initialIndex: Int): Int {
    if (itemCount <= 0) return 0
    val midPoint = Int.MAX_VALUE / 2
    val alignedBase = midPoint - (midPoint % itemCount)
    return alignedBase + initialIndex.coerceIn(0, itemCount - 1)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelColumn(
    items: List<T>,
    initialIndex: Int,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 56.dp,
    visibleCount: Int = 3,
    infiniteLoop: Boolean = true,
    selectedColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedFontSize: TextUnit = 28.sp,
    unselectedFontSize: TextUnit = 20.sp,
    unselectedAlpha: Float = 0.45f,
    itemFormatter: (T) -> String,
    onItemSelected: (index: Int) -> Unit,
    externalListState: LazyListState? = null
) {
    val itemCount = items.size
    val totalCount = if (infiniteLoop) Int.MAX_VALUE else itemCount

    // Start near the middle of Int.MAX_VALUE, aligned to an exact multiple of itemCount plus
    // the initial index, so that (listIndex % itemCount) == initialIndex with zero pixel offset.
    val startScrollIndex = remember(itemCount, initialIndex, infiniteLoop) {
        if (infiniteLoop) {
            wheelScrollIndex(itemCount, initialIndex)
        } else {
            initialIndex.coerceIn(0, itemCount - 1)
        }
    }

    val ownedListState = rememberLazyListState(initialFirstVisibleItemIndex = startScrollIndex)
    val listState = externalListState ?: ownedListState
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centeredListIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                startScrollIndex
            } else {
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                    val itemCenter = itemInfo.offset + (itemInfo.size / 2)
                    abs(itemCenter - viewportCenter)
                }?.index ?: listState.firstVisibleItemIndex
            }
        }
    }

    LaunchedEffect(externalListState, initialIndex, itemCount) {
        if (externalListState == null || itemCount == 0) return@LaunchedEffect
        val currentActual = ((centeredListIndex % itemCount) + itemCount) % itemCount
        if (currentActual != initialIndex.coerceIn(0, itemCount - 1)) {
            val forward = ((initialIndex - currentActual) % itemCount + itemCount) % itemCount
            externalListState.animateScrollToItem(centeredListIndex + forward)
        }
    }

    LaunchedEffect(listState, itemCount) {
        snapshotFlow { listState.isScrollInProgress to centeredListIndex }
            .filter { (inProgress, _) -> !inProgress }
            .distinctUntilChanged()
            .collect { (_, snappedListIndex) ->
                val actualIndex = ((snappedListIndex % itemCount) + itemCount) % itemCount
                onItemSelected(actualIndex)
            }
    }

    val verticalPadding = itemHeight * (visibleCount / 2)

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(itemHeight * visibleCount)
    ) {
        items(
            count = totalCount,
            key = { index -> index }
        ) { listIndex ->
            val actualIndex = ((listIndex % itemCount) + itemCount) % itemCount
            val isSelected = listIndex == centeredListIndex

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = itemFormatter(items[actualIndex]),
                    style = if (isSelected) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = selectedFontSize
                        )
                    } else {
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = unselectedFontSize
                        )
                    },
                    color = if (isSelected) {
                        selectedColor
                    } else {
                        unselectedColor
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(if (isSelected) 1f else unselectedAlpha)
                )
            }
        }
    }
}
