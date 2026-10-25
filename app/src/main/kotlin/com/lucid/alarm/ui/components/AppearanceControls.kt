package com.lucid.alarm.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.ui.theme.onColorFor
import com.lucid.alarm.ui.theme.toColor

/**
 * High-contrast Colors for a selectable [FilterChip]: a solid accent fill with matching foreground,
 * so a selected chip stands out clearly against any background palette.
 */
@Composable
fun selectedChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
)

/**
 * Color + optional two-stop gradient editor shared by the global customization screen and the
 * per-alarm appearance editor.
 *
 * @param inheritEnabled when true an explicit "Inherit" chip is offered (per-alarm overrides), and
 * [inherited] marks whether the value currently falls back to the global setting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceColorEditor(
    title: String,
    color: Long,
    gradient: Boolean,
    colorEnd: Long,
    direction: GradientDirection,
    onColor: (Long) -> Unit,
    onGradient: (Boolean) -> Unit,
    onColorEnd: (Long) -> Unit,
    onDirection: (GradientDirection) -> Unit,
    inheritEnabled: Boolean = false,
    inherited: Boolean = false,
    onInherit: () -> Unit = {}
) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(
        text = "Color",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ColorSwatchRow(
        selected = color,
        inheritEnabled = inheritEnabled,
        inherited = inherited,
        onInherit = onInherit,
        onSelect = onColor
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Gradient",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = gradient, onCheckedChange = onGradient)
    }
    if (gradient) {
        Text(
            text = "Gradient end",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ColorSwatchRow(selected = colorEnd, onSelect = onColorEnd)
        Text(
            text = "Direction",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradientDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = direction == dir,
                            onClick = { onDirection(dir) },
                            label = { Text(dir.label) },
                            colors = selectedChipColors()
                        )
            }
        }
    }
    if (inheritEnabled && inherited) {
        Text(
            text = "Inherit uses the global value.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Row of tappable Color swatches: optional inherit chip, a default white chip and every accent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchRow(
    selected: Long,
    onSelect: (Long) -> Unit,
    inheritEnabled: Boolean = false,
    inherited: Boolean = false,
    onInherit: () -> Unit = {}
) {
    // Preset swatches only: AccentColor.CUSTOM carries a placeholder argb and renders as the
    // dedicated rainbow picker chip below, so it must not appear as a duplicate preset.
    val presets = remember { AccentColor.entries.filter { it != AccentColor.CUSTOM }.map { it.argb } }
    val options = remember(presets) { listOf(0L) + presets }
    val isCustom = !inherited && selected != 0L && selected !in presets
    var showPicker by remember { mutableStateOf(false) }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (inheritEnabled) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(
                        width = if (inherited) 3.dp else 1.dp,
                        color = if (inherited) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
                    .clickable { onInherit() },
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        options.forEach { argb ->
            val swatch = if (argb == 0L) Color.White else Color(argb)
            val isSelected = !inherited && selected == argb
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = onColorFor(swatch)
                    )
                }
            }
        }
        // Custom Color chip — opens a hue / saturation / brightness + hex picker.
        val rainbow = remember {
            Brush.sweepGradient(
                listOf(
                    Color(0xFFEF4444),
                    Color(0xFFD9B478),
                    Color(0xFF84CC16),
                    Color(0xFF14B8A6),
                    Color(0xFF2563EB),
                    Color(0xFF8B5CF6),
                    Color(0xFFEC4899),
                    Color(0xFFEF4444)
                )
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isCustom) Color(selected) else Color.Unspecified)
                .then(if (isCustom) Modifier else Modifier.background(rainbow))
                .border(
                    width = if (isCustom) 3.dp else 1.dp,
                    color = if (isCustom) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape
                )
                .clickable { showPicker = true },
            contentAlignment = Alignment.Center
        ) {
            if (isCustom) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = onColorFor(Color(selected))
                )
            } else {
                Text("+", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (showPicker) {
        CustomColorPickerDialog(
            initialColor = if (selected != 0L) selected else 0xFFFFFFFF,
            onDismiss = { showPicker = false },
            onConfirm = {
                onSelect(it)
                showPicker = false
            }
        )
    }
}

/**
 * Compact HSV + hex Color picker. Emits a packed ARGB [Long] (fully opaque) via [onConfirm].
 */
@Composable
fun CustomColorPickerDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initial = remember(initialColor) {
        val argb = if (initialColor == 0L) 0xFFFFFFFF else initialColor
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(argb.toInt(), hsv)
        hsv
    }
    var hue by remember { mutableStateOf(initial[0]) }
    var saturation by remember { mutableStateOf(initial[1]) }
    var brightness by remember { mutableStateOf(initial[2]) }
    var hex by remember {
        mutableStateOf(String.format("#%06X", (0xFFFFFF and (initialColor.toInt().takeIf { initialColor != 0L } ?: 0xFFFFFF))))
    }

    val current = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))

    fun syncHex() {
        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
        hex = String.format("#%06X", 0xFFFFFF and argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(current)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                )
                Spacer(Modifier.height(16.dp))
                Text("Hue", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = hue,
                    onValueChange = { hue = it; syncHex() },
                    valueRange = 0f..360f
                )
                Text("Saturation", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it; syncHex() },
                    valueRange = 0f..1f
                )
                Text("Brightness", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it; syncHex() },
                    valueRange = 0f..1f
                )
                OutlinedTextField(
                    value = hex,
                    onValueChange = { input ->
                        hex = input
                        val cleaned = input.trim().removePrefix("#")
                        if (cleaned.length == 6 || cleaned.length == 8) {
                            cleaned.toLongOrNull(16)?.let { parsed ->
                                val argb = if (cleaned.length == 6) {
                                    (0xFF000000L or parsed).toInt()
                                } else {
                                    parsed.toInt()
                                }
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(argb, hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                            }
                        }
                    },
                    label = { Text("Hex") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
                onConfirm(0xFF000000L or (argb.toLong() and 0xFFFFFFL))
            }) { Text("Use Color") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
