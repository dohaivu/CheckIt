package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.checkit.ui.tasks.PredefinedLabels

/**
 * A specialized text field for labels that shows suggestions in a [Popup] when focused.
 * Uses a horizontal [LabelSuggestions] layout inside the popup.
 */
@Composable
fun LabelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    recentLabels: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    placeholderStyle: TextStyle? = null,
    enabled: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
    maxWidth: Dp = 120.dp
) {
    var isFocused by remember { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier) {
        AppOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .onSizeChanged { fieldSize = it }
                .onFocusChanged {
                    isFocused = it.isFocused
                }
                .clearFocusOnKeyboardDismiss(),
            textStyle = textStyle,
            maxLines = 1,
            placeholder = placeholder,
            placeholderStyle = placeholderStyle ?: textStyle,
            enabled = enabled,
            contentPadding = contentPadding
        )

        if (isFocused && fieldSize != IntSize.Zero) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, fieldSize.height),
                onDismissRequest = { isFocused = false },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = true,
                )
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    LabelSuggestions(
                        currentLabel = value,
                        recentLabels = recentLabels,
                        onLabelSelect = { label ->
                            onValueChange(label)
                            isFocused = false
                        },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
