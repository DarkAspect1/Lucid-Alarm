package com.lucid.alarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCapsulePill(
    label: String,
    selected: Boolean,
    counts: Pair<Int, Int>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    cardShape: Shape = RoundedCornerShape(28.dp),
    selectionMode: Boolean = false,
    showBottomDivider: Boolean = false,
    connectedIndex: Int = 0,
    totalConnected: Int = 1,
    modifier: Modifier = Modifier
) {
    val enabledCount = counts.first
    val disabledCount = counts.second
    val hasEnabled = enabledCount > 0
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val titleColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isLight -> MaterialTheme.colorScheme.primary
        else -> Color.White
    }
    val countColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    FrostedBlurBox(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = cardShape,
        tintColor = if (selected) MaterialTheme.colorScheme.primary else null,
        borderColor = null,
        connectedIndex = connectedIndex,
        totalConnected = totalConnected
    ) {
        val circleBackground = when {
            selected -> MaterialTheme.colorScheme.onPrimary
            hasEnabled -> MaterialTheme.colorScheme.primary
            else -> Color(0xFF484952)
        }
        val circleIconTint = when {
            selected && hasEnabled -> MaterialTheme.colorScheme.primary
            selected -> Color(0xFF484952)
            else -> Color.White
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = if (selectionMode) 24.dp else 14.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(circleBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasEnabled) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                        contentDescription = null,
                        tint = circleIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = "Active alarms",
                            tint = countColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$enabledCount",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = countColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.AlarmOff,
                            contentDescription = "Inactive alarms",
                            tint = countColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$disabledCount",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = countColor.copy(alpha = 0.7f)
                        )
                    }
                }
                if (selectionMode) {
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionCheckbox(
                        checked = selected,
                        onToggle = { onClick() }
                    )
                }
            }
            if (showBottomDivider && !selected) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 1.dp,
                    color = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
                )
            }
        }
    }
}
