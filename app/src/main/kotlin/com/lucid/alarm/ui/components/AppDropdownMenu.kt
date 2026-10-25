package com.lucid.alarm.ui.components

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A single entry in an [AppDropdownMenu]. */
data class AppMenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

/**
 * Compact One UI styled frosted translucent popup menu with window behind-blur.
 */
@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    if (!expanded) return

    val menuShape = RoundedCornerShape(20.dp)
    val itemShape = RoundedCornerShape(12.dp)
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val textColor = if (isLight) Color(0xFF111318) else Color.White
    val pressedOverlay = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .widthIn(min = 155.dp, max = 195.dp),
        offset = offset,
        shape = menuShape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = null
    ) {
        // Window behind-blur on Android 12+ (API 31+) with blurBehindRadius = 60
        val view = LocalView.current
        SideEffect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var current: ViewParent? = view.parent
                while (current != null) {
                    if (current is View) {
                        val lp = current.layoutParams
                        if (lp is WindowManager.LayoutParams) {
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                            lp.blurBehindRadius = 60
                            val wm = current.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                            wm?.updateViewLayout(current, lp)
                            break
                        }
                    }
                    current = current.parent
                }
            }
        }

        FrostedBlurBox(
            modifier = Modifier.widthIn(min = 155.dp, max = 195.dp),
            shape = menuShape
        ) {
            Column(
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items.forEach { item ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clip(itemShape)
                            .background(if (isPressed) pressedOverlay else Color.Transparent)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onDismissRequest()
                                item.onClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
