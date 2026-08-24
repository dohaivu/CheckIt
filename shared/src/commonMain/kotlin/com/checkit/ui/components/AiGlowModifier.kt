package com.checkit.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Definition of a colorful spectrum for the AI glow.
 */
data class AiGlowSpectrum(
    val stops: List<Pair<Float, Color>>
) {
    companion object {
        val Gemini = AiGlowSpectrum(listOf(
            0.000f to Color.Transparent,
            0.080f to Color(0xFF4285F4).copy(alpha = 0.2f),
            0.180f to Color(0xFF4285F4),
            0.300f to Color(0xFF9B72CB),
            0.420f to Color(0xFFD96570),
            0.540f to Color(0xFFF4AF5F),
            0.640f to Color(0xFFF4AF5F).copy(alpha = 0.2f),
            0.720f to Color.Transparent,
            1.000f to Color.Transparent
        ))

        val Critical = AiGlowSpectrum(listOf(
            0.000f to Color.Transparent,
            0.080f to Color(0xFFFF5252).copy(alpha = 0.2f),
            0.180f to Color(0xFFFF5252),
            0.300f to Color(0xFFD32F2F),
            0.420f to Color(0xFFFF1744),
            0.540f to Color(0xFFFF8A80),
            0.640f to Color(0xFFFF8A80).copy(alpha = 0.2f),
            0.720f to Color.Transparent,
            1.000f to Color.Transparent
        ))

        val Warning = AiGlowSpectrum(listOf(
            0.000f to Color.Transparent,
            0.080f to Color(0xFFFFD740).copy(alpha = 0.2f),
            0.180f to Color(0xFFFFD740),
            0.300f to Color(0xFFFFC107),
            0.420f to Color(0xFFFFE57F),
            0.540f to Color(0xFFFFAB00),
            0.640f to Color(0xFFFFAB00).copy(alpha = 0.2f),
            0.720f to Color.Transparent,
            1.000f to Color.Transparent
        ))

        val Healthy = AiGlowSpectrum(listOf(
            0.000f to Color.Transparent,
            0.080f to Color(0xFF69F0AE).copy(alpha = 0.2f),
            0.180f to Color(0xFF69F0AE),
            0.300f to Color(0xFF00E676),
            0.420f to Color(0xFFB9F6CA),
            0.540f to Color(0xFF00C853),
            0.640f to Color(0xFF00C853).copy(alpha = 0.2f),
            0.720f to Color.Transparent,
            1.000f to Color.Transparent
        ))
    }
}

/**
 * A simpler, single-color breathing glow for status indicators.
 * Performance optimized: zero allocations in the draw loop.
 */
fun Modifier.statusBreathingGlow(
    color: Color,
    pulseFraction: () -> Float,
    cornerRadius: Dp,
    baseAlpha: Float = 0.6f
): Modifier = drawBehind {
    val pulse = pulseFraction()
    
    // Multi-layered glow that "breathes" in intensity and thickness
    // Layer 1: Outer faint aura (most affected by pulse)
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = 12.dp.toPx() * pulse, cap = StrokeCap.Round),
        alpha = baseAlpha * 0.2f * pulse
    )
    // Layer 2: Mid-range glow
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = 7.dp.toPx() * pulse, cap = StrokeCap.Round),
        alpha = baseAlpha * 0.4f * pulse
    )
    // Layer 3: Inner core (now also follows pulse to allow "disappearing")
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        alpha = baseAlpha * 0.9f * pulse.coerceIn(0f, 1f)
    )
}

/**
 * Draws a pill-shaped multi-layer stroke whose colors follow a rotating conic
 * gradient. The outer strokes act as a multi-layered glow "AI aura".
 */
fun Modifier.rotatingAiGlow(
    rotationFraction: () -> Float,
    alpha: () -> Float,
    pulse: () -> Float,
    spectrum: AiGlowSpectrum,
    cornerRadius: Dp,
    glowLayers: List<Pair<Dp, Float>> = listOf(
        16.dp to 0.10f,
        10.dp to 0.25f,
        5.dp to 0.55f,
        2.dp to 1.0f
    )
): Modifier = drawBehind {
    val rot = rotationFraction()
    val baseAlpha = alpha()
    val pulseFactor = pulse()
    
    val brush = Brush.sweepGradient(
        colorStops = ringColorStops(rot, spectrum.stops).toTypedArray(),
        center = Offset(size.width / 2f, size.height / 2f)
    )

    for ((width, layerAlpha) in glowLayers) {
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = width.toPx(), cap = StrokeCap.Round),
            alpha = baseAlpha * layerAlpha * (if (width > 5.dp) pulseFactor else 1f)
        )
    }
}

private fun ringColorStops(rotation: Float, stops: List<Pair<Float, Color>>): List<Pair<Float, Color>> {
    val result = ArrayList<Pair<Float, Color>>(stops.size + 4)
    val seamFraction = (1f - rotation).let { if (it < 0) it + 1 else it }
    val seamColor = interpolateRingColor(seamFraction, stops)
    
    result.add(0f to seamColor)
    for (stop in stops) {
        var newPos = stop.first + rotation
        if (newPos > 1f) newPos -= 1f
        result.add(newPos to stop.second)
    }
    result.sortBy { it.first }
    result.add(1f to seamColor)
    return result
}

private fun interpolateRingColor(fraction: Float, stops: List<Pair<Float, Color>>): Color {
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
