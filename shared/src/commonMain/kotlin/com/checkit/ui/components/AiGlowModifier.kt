package com.checkit.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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
            0.080f to Color(0x334285F4),
            0.180f to Color(0xFF4285F4),
            0.300f to Color(0xFF9B72CB),
            0.420f to Color(0xFFD96570),
            0.540f to Color(0xFFF4AF5F),
            0.640f to Color(0x33F4AF5F),
            0.720f to Color.Transparent,
            1.000f to Color.Transparent
        ))
    }
}

/**
 * A simpler, single-color breathing glow for status indicators.
 * Optimized: early exits when invisible and avoids redundant conversions.
 */
fun Modifier.statusBreathingGlow(
    color: Color,
    pulseFraction: () -> Float,
    cornerRadius: Dp,
    baseAlpha: Float = 0.6f
): Modifier = drawBehind {
    val pulse = pulseFraction()
    if (pulse <= 0.001f || baseAlpha <= 0.001f) return@drawBehind

    val cornerRadiusPx = CornerRadius(cornerRadius.toPx())

    // Multi-layered glow that "breathes" in intensity and thickness
    // Layer 1: Outer faint aura (most affected by pulse)
    drawRoundRect(
        color = color,
        cornerRadius = cornerRadiusPx,
        style = Stroke(width = 12.dp.toPx() * pulse, cap = StrokeCap.Round),
        alpha = baseAlpha * 0.2f * pulse
    )
    // Layer 2: Mid-range glow
    drawRoundRect(
        color = color,
        cornerRadius = cornerRadiusPx,
        style = Stroke(width = 7.dp.toPx() * pulse, cap = StrokeCap.Round),
        alpha = baseAlpha * 0.4f * pulse
    )
    // Layer 3: Inner core (now also follows pulse to allow "disappearing")
    drawRoundRect(
        color = color,
        cornerRadius = cornerRadiusPx,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        alpha = baseAlpha * 0.9f * pulse.coerceIn(0f, 1f)
    )
}

private val DefaultGlowLayerWidths = floatArrayOf(16f, 10f, 5f, 2f)
private val DefaultGlowLayerAlphas = floatArrayOf(0.10f, 0.25f, 0.55f, 1.0f)

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
    glowLayers: List<Pair<Dp, Float>>? = null
): Modifier = drawBehind {
    val baseAlpha = alpha()
    if (baseAlpha <= 0.001f) return@drawBehind

    val rawRot = rotationFraction() % 1f
    val rot = if (rawRot < 0f) rawRot + 1f else rawRot
    val pulseFactor = pulse()
    val cornerRadiusPx = CornerRadius(cornerRadius.toPx())

    val stopsArray = ringColorStopsArray(rot, spectrum.stops)
    val brush = Brush.sweepGradient(
        colorStops = stopsArray,
        center = Offset(size.width * 0.5f, size.height * 0.5f)
    )

    if (glowLayers == null) {
        for (i in DefaultGlowLayerWidths.indices) {
            val widthDp = DefaultGlowLayerWidths[i]
            val layerAlpha = DefaultGlowLayerAlphas[i]
            val effectiveAlpha = baseAlpha * layerAlpha * (if (widthDp > 5f) pulseFactor else 1f)
            if (effectiveAlpha > 0.001f) {
                drawRoundRect(
                    brush = brush,
                    cornerRadius = cornerRadiusPx,
                    style = Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round),
                    alpha = effectiveAlpha
                )
            }
        }
    } else {
        for (i in glowLayers.indices) {
            val (width, layerAlpha) = glowLayers[i]
            val effectiveAlpha = baseAlpha * layerAlpha * (if (width > 5.dp) pulseFactor else 1f)
            if (effectiveAlpha > 0.001f) {
                drawRoundRect(
                    brush = brush,
                    cornerRadius = cornerRadiusPx,
                    style = Stroke(width = width.toPx(), cap = StrokeCap.Round),
                    alpha = effectiveAlpha
                )
            }
        }
    }
}

/**
 * Builds the rotated color stops array without sorting.
 * Because the original stops are sorted in [0, 1], rotating by [rotation] simply splits
 * the interior stops into wrapped elements (shifted into [0, rot)) and non-wrapped elements
 * (shifted into [rot, 1)), both of which are strictly sorted.
 */
internal fun ringColorStopsArray(
    rotation: Float,
    stops: List<Pair<Float, Color>>
): Array<Pair<Float, Color>> {
    if (stops.isEmpty()) return emptyArray()
    val n = stops.size
    val rawRot = rotation % 1f
    val rot = if (rawRot < 0f) rawRot + 1f else rawRot

    val rawSeam = 1f - rot
    val seamFraction = if (rawSeam < 0f) rawSeam + 1f else if (rawSeam >= 1f) rawSeam - 1f else rawSeam
    val seamColor = interpolateRingColor(seamFraction, stops)

    val result = ArrayList<Pair<Float, Color>>(n + 2)
    result.add(0f to seamColor)

    // Find the split point among interior stops where pos + rot >= 1f
    var splitIndex = n
    for (i in 0 until n) {
        val pos = stops[i].first
        if (pos > 0.0001f && pos < 0.9999f && pos + rot >= 1f) {
            splitIndex = i
            break
        }
    }

    // 1. Wrapped interior stops (pos + rot >= 1.0 -> new pos in (0, rot))
    for (i in splitIndex until n) {
        val pos = stops[i].first
        if (pos > 0.0001f && pos < 0.9999f) {
            val wrappedPos = pos + rot - 1f
            if (wrappedPos > 0.0001f && wrappedPos < 0.9999f) {
                result.add(wrappedPos to stops[i].second)
            }
        }
    }

    // 2. Non-wrapped interior stops (pos + rot < 1.0 -> new pos in [rot, 1.0))
    for (i in 0 until splitIndex) {
        val pos = stops[i].first
        if (pos > 0.0001f && pos < 0.9999f) {
            val nonWrappedPos = pos + rot
            if (nonWrappedPos > 0.0001f && nonWrappedPos < 0.9999f) {
                result.add(nonWrappedPos to stops[i].second)
            }
        }
    }

    result.add(1f to seamColor)
    return result.toTypedArray()
}

/**
 * Linearly interpolates the color at [fraction] along the circular stop spectrum.
 */
internal fun interpolateRingColor(fraction: Float, stops: List<Pair<Float, Color>>): Color {
    if (stops.isEmpty()) return Color.Transparent
    if (fraction <= stops.first().first) return stops.first().second
    if (fraction >= stops.last().first) return stops.last().second

    for (i in 0 until stops.size - 1) {
        val s1 = stops[i]
        val s2 = stops[i + 1]
        if (fraction in s1.first..s2.first) {
            val range = s2.first - s1.first
            if (range <= 0.00001f) return s1.second
            val t = (fraction - s1.first) / range
            return lerp(s1.second, s2.second, t)
        }
    }
    return Color.Transparent
}
