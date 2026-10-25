package com.lucid.alarm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A high-contrast, rounded selection indicator used across the alarm list, recycle bin and world
 * clock list.
 *
 * Selected rows in those lists are painted with the solid [MaterialTheme.colorScheme.primary]
 * accent, so the checked box is drawn in the inverse ([onPrimary] fill + [primary] tick) to stay
 * legible on the accent surface. Unchecked boxes are a bold rounded outline.
 */
@Composable
fun SelectionCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)
    val filled = MaterialTheme.colorScheme.onPrimary
    val tick = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(shape)
            .background(if (checked) filled else Color.Transparent)
            .border(
                width = if (checked) 0.dp else 2.dp,
                color = if (checked) filled else outline,
                shape = shape
            )
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = tick,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
