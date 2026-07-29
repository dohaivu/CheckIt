package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.checkit.ui.theme.isLight
import com.checkit.ui.theme.materialIcon
import com.checkit.ui.theme.toColor
import com.checkit.ui.theme.toHex
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
internal fun ColorPicker(
    colors: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    val isCustomColor = remember(selected, colors) {
        selected.isNotEmpty() && colors.none { it.equals(selected, ignoreCase = true) }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { hex ->
            ColorSwatch(
                color = hex.toColor(),
                isSelected = hex.equals(selected, ignoreCase = true),
                onClick = { onSelect(hex) }
            )
        }

        CustomColorSwatch(
            selectedColor = if (isCustomColor) selected.toColor() else null,
            isSelected = isCustomColor,
            onClick = { showCustomPicker = true }
        )
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = if (isCustomColor) selected.toColor() else Color.White,
            onDismiss = { showCustomPicker = false },
            onColorSelected = { color ->
                onSelect(color.toHex())
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun CustomColorSwatch(
    selectedColor: Color?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val rainbowBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red
            )
        )
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (selectedColor != null) {
                    Modifier.background(selectedColor)
                } else {
                    Modifier.background(rainbowBrush)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (selectedColor?.isLight() == true) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else if (selectedColor == null) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Custom color",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val controller = rememberColorPickerController()
    var lastSelectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    controller = controller,
                    initialColor = initialColor,
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            lastSelectedColor = envelope.color
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    controller = controller
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(lastSelectedColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun IconPicker(
    icons: List<String>,
    selected: String,
    tint: Color,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        icons.forEach { iconName ->
            val isSelected = iconName == selected
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        } else {
                            Color.Unspecified
                        }
                    )
                    .clickable { onSelect(iconName) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = materialIcon(iconName),
                    contentDescription = iconName,
                    tint = tint
                )
            }
        }
    }
}
