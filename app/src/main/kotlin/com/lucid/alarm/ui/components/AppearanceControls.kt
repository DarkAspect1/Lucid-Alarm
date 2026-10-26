package com.lucid.alarm.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.WindowManager
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lucid.alarm.data.prefs.AccentColor
import com.lucid.alarm.data.prefs.GradientDirection
import com.lucid.alarm.ui.theme.onColorFor

val SelectionChipShape = RoundedCornerShape(50)

@Composable
fun selectedChipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
)

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
                            shape = SelectionChipShape,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatchRow(
    selected: Long,
    onSelect: (Long) -> Unit,
    inheritEnabled: Boolean = false,
    inherited: Boolean = false,
    onInherit: () -> Unit = {}
) {

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
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var brightness by remember { mutableFloatStateOf(initial[2]) }
    var hex by remember {
        mutableStateOf(String.format("#%06X", (0xFFFFFF and (initialColor.toInt().takeIf { initialColor != 0L } ?: 0xFFFFFF))))
    }

    val current = Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))

    fun syncHex() {
        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
        hex = String.format("#%06X", 0xFFFFFF and argb)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var currentParent: ViewParent? = view.parent
                while (currentParent != null) {
                    if (currentParent is View) {
                        val lp = currentParent.layoutParams
                        if (lp is WindowManager.LayoutParams) {
                            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                            lp.blurBehindRadius = 60
                            val wm = currentParent.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                            wm?.updateViewLayout(currentParent, lp)
                            break
                        }
                    }
                    currentParent = currentParent.parent
                }
            }
        }

        val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
        val accentColor = MaterialTheme.colorScheme.primary

        FrostedCardSurface(
            modifier = Modifier
                .widthIn(min = 290.dp, max = 330.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom Color",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(current)
                            .border(
                                width = 1.dp,
                                color = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.07f)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
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
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${hue.toInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = hue,
                        onValueChange = { hue = it; syncHex() },
                        valueRange = 0f..360f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = current,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saturation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(saturation * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it; syncHex() },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = current,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Brightness",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(brightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it; syncHex() },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = current,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accentColor)
                            .clickable {
                                val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
                                onConfirm(0xFF000000L or (argb.toLong() and 0xFFFFFFL))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Apply",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
