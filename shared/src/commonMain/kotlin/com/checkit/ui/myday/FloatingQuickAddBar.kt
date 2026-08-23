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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Gap between the field surface and the rotating ring. */
private val QuickAddHaloPadding = 10.dp

// Gemini / Google AI colors
private val GeminiBlue = Color(0xFF4285F4)
private val GeminiPurple = Color(0xFF9B72CB)
private val GeminiRed = Color(0xFFD96570)
private val GeminiOrange = Color(0xFFF4AF5F)

/**
 * A fluid, colorful AI-style gradient spectrum.
 */
private val RingStops = listOf(
    0.000f to Color.Transparent,
    0.080f to GeminiBlue.copy(alpha = 0.2f),
    0.180f to GeminiBlue,
    0.300f to GeminiPurple,
    0.420f to GeminiRed,
    0.540f to GeminiOrange,
    0.640f to GeminiOrange.copy(alpha = 0.2f),
    0.720f to Color.Transparent,
    1.000f to Color.Transparent
)

/**
 * Linearly interpolates a color from the [RingStops] at a given [fraction].
 */
private fun interpolateRingColor(fraction: Float): Color {
    val stops = RingStops
    if (fraction <= stops.first().first) return stops.first().second
    if (fraction >= stops.last().first) return stops.last().second

    for (i in 0 until stops.size - 1) {
        val s1 = stops[i]
        val s2 = stops[i + 1]
        if (fraction >= s1.first && fraction <= s2.first) {
            val t = (fraction - s1.first) / (s2.first - s1.first)
            return lerpColor(s1.second, s2.second, t)
        }
    }
    return Color.Transparent
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    return Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction
    )
}

/**
 * Shifts the base stops by [rotation]. Optimized to avoid jumps at the 0/1 seam.
 */
private fun ringColorStops(rotation: Float): List<Pair<Float, Color>> {
    val stops = RingStops
    val result = ArrayList<Pair<Float, Color>>(stops.size + 4)

    // Calculate the exact color at the 0/1 seam (3 o'clock position)
    // The seam at 0.0 in the rotated gradient corresponds to (1 - rotation) in the original.
    val seamFraction = (1f - rotation).let { if (it < 0) it + 1 else it }
    val seamColor = interpolateRingColor(seamFraction)
    
    result.add(0f to seamColor)

    // Map each original stop to its new position
    for (stop in stops) {
        var newPos = stop.first + rotation
        if (newPos > 1f) newPos -= 1f
        result.add(newPos to stop.second)
    }

    // Sorting is necessary but we've minimized the list size
    result.sortBy { it.first }
    
    // Close the seam at the end
    result.add(1f to seamColor)

    return result
}

/**
 * Draws a pill-shaped multi-layer stroke whose colors follow a rotating conic
 * gradient. The outer strokes act as a multi-layered glow "AI aura".
 */
private fun Modifier.rotatingRing(
    rotationFraction: () -> Float,
    alpha: () -> Float,
    pulse: () -> Float,
    cornerRadius: Dp
): Modifier = drawBehind {
    val rot = rotationFraction()
    val baseAlpha = alpha()
    val pulseFactor = pulse()
    
    // Primary Gemini gradient
    val brush = Brush.sweepGradient(
        colorStops = ringColorStops(rot).toTypedArray(),
        center = Offset(size.width / 2f, size.height / 2f)
    )
    
    // Multi-layered glow for that "AI aura" look
    val glowLayers = listOf(
        16.dp to 0.10f * pulseFactor,
        10.dp to 0.25f,
        5.dp to 0.55f,
        2.dp to 1.0f
    )

    for ((width, layerAlpha) in glowLayers) {
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = width.toPx(), cap = StrokeCap.Round),
            alpha = baseAlpha * layerAlpha
        )
    }
}

/**
 * Slim floating bottom bar for capturing a plan item right now.
 * Shown only when nothing on today's plan sits within ±30 minutes of the current time.
 *
 * Styled after the "Google AI input" gradient: a conic gradient ring (blue, red,
 * yellow, green) orbits the field while unfocused, backed by a blurred twin for
 * glow. On focus the orbit calms down to a faint steady shimmer.
 */
@Composable
internal fun FloatingQuickAddBar(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // AI Orbiting Animation
    val orbit = rememberInfiniteTransition(label = "aiOrbit")
    val rotationState = orbit.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "aiRotation"
    )
    
    // Subtle breathing pulse for the glow
    val pulseState = orbit.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiPulse"
    )

    // Fade the ring slightly when focused to not distract from typing.
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
        // AI Ring Animation - Performance optimized: reads .value inside lambda (Draw phase)
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotatingRing(
                    rotationFraction = { rotationState.value },
                    alpha = { ringAlphaState.value },
                    pulse = { pulseState.value },
                    cornerRadius = 24.dp + QuickAddHaloPadding
                )
        )

        // Main Input Field Container (Replaces Surface for better control and performance)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .shadow(elevation = 3.dp, shape = surfaceShape)
                .background(color = surfaceColor, shape = surfaceShape)
                .drawBehind {
                    // Subtle AI background shimmer depth
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
