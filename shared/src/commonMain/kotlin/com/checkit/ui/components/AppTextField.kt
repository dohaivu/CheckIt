package com.checkit.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.clear_text
import com.checkit.ui.tasks.ContentContainerAlpha

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle = TextStyle.Default,
    placeholder: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    clearEnabled: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = MaterialTheme.colorScheme.surfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        textStyle = textStyle,
        maxLines = maxLines,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                placeholder = if (placeholder != null) {
                        {
                            Text(
                                text = placeholder,
                                fontStyle = textStyle.fontStyle,
                                fontWeight = textStyle.fontWeight,
                                fontSize = textStyle.fontSize,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentContainerAlpha),
                            )
                        }
                    } else null,
                trailingIcon = if (clearEnabled) {
                    {
                        if (value.isNotEmpty()) {
                            IconButton(
                                onClick = { onValueChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(Res.string.clear_text),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else null,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                colors = colors,
            )
        }
    )
}
