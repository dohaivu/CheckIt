package com.checkit.ui.myday

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.checkit.ui.components.AiGlowSpectrum
import com.checkit.ui.components.rotatingAiGlow

/** Gap between the field surface and the rotating ring. */
private val QuickAddHaloPadding = 10.dp

private val GeminiBlue = Color(0xFF4285F4)

/**
 * Slim floating bottom bar for capturing a plan item right now.
 * Shown only when nothing on today's plan sits within ±30 minutes of the current time.
 */
@Composable
internal fun FloatingQuickAddBar(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val orbit = rememberInfiniteTransition(label = "aiOrbit")
    val rotationState = orbit.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "aiRotation"
    )
    
    val pulseState = orbit.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiPulse"
    )

    val ringAlphaState = animateFloatAsState(
        targetValue = if (isFocused) 0.3f else 1f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "aiAlpha"
    )

    val surfaceShape = RoundedCornerShape(24.dp)
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(QuickAddHaloPadding)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotatingAiGlow(
                    rotationFraction = { rotationState.value },
                    alpha = { ringAlphaState.value },
                    pulse = { pulseState.value },
                    spectrum = AiGlowSpectrum.Gemini,
                    cornerRadius = 24.dp + QuickAddHaloPadding
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .shadow(elevation = 3.dp, shape = surfaceShape)
                .background(color = surfaceColor, shape = surfaceShape)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GeminiBlue.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.4f),
                            radius = size.width * 0.7f
                        )
                    )
                }
                .border(width = 1.dp, color = outlineColor, shape = surfaceShape)
                .onFocusChanged { isFocused = it.isFocused }
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(GeminiBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val title = text.trim()
                        if (title.isNotEmpty()) onSubmit(title)
                        text = ""
                        focusManager.clearFocus()
                    }
                ),
                decorationBox = { innerField ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        if (text.isEmpty()) {
                            Text(
                                text = "What's on your mind?",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
