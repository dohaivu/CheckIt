package com.checkit.ui.nested

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.checkit.domain.NestedBorderShape
import com.checkit.domain.NestedBorderStyle
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Card outline for a nested row. Card shapes are authored against the row
 * bounds so they can be used for both the background fill and the border.
 *
 * All shapes are cheap to build; the cloud/ticket paths only run on layout
 * (size changes), never per frame.
 */
class NestedCardShape(
    private val kind: NestedBorderShape,
    private val cornerRadius: Dp = 8.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Generic(Path())

        val radius = min(with(density) { cornerRadius.toPx() }, min(w, h) / 2f)
        return when (kind) {
            NestedBorderShape.Rectangle -> Outline.Rectangle(Rect(0f, 0f, w, h))
            NestedBorderShape.Rounded,
            NestedBorderShape.Pill,
            NestedBorderShape.Leaf -> Outline.Rounded(roundedCardRect(kind, w, h, radius))

            NestedBorderShape.Cloud,
            NestedBorderShape.Ticket,
            NestedBorderShape.CutCorner,
            NestedBorderShape.Hexagon,
            NestedBorderShape.Ribbon -> Outline.Generic(
                customPath(kind, w, h, radius, density)
            )
        }
    }
}

/**
 * Corner geometry for the non-path shapes, split out so it can be verified
 * without an Android canvas. All four radii must be set: [RoundRect]'s
 * multi-corner constructor defaults the ones you leave out to zero.
 */
internal fun roundedCardRect(
    kind: NestedBorderShape,
    width: Float,
    height: Float,
    radius: Float
): RoundRect {
    val rect = Rect(0f, 0f, width, height)
    val bloom = min(width, height) / 2f
    return when (kind) {
        NestedBorderShape.Pill -> RoundRect(rect = rect, cornerRadius = CornerRadius(bloom))
        // Two opposite corners bloom into a semicircle, the other two stay
        // softly rounded: reads as an organic "leaf" without clipping text.
        NestedBorderShape.Leaf -> RoundRect(
            rect = rect,
            topLeft = CornerRadius(bloom),
            topRight = CornerRadius(radius),
            bottomRight = CornerRadius(bloom),
            bottomLeft = CornerRadius(radius)
        )

        else -> RoundRect(rect = rect, cornerRadius = CornerRadius(radius))
    }
}

private fun customPath(
    kind: NestedBorderShape,
    w: Float,
    h: Float,
    radius: Float,
    density: Density
): Path = when (kind) {
    NestedBorderShape.Cloud -> cloudPath(w, h, density)
    NestedBorderShape.Ticket -> ticketPath(w, h, radius, density)
    NestedBorderShape.CutCorner -> cutCornerPath(w, h)
    NestedBorderShape.Hexagon -> hexagonPath(w, h)
    NestedBorderShape.Ribbon -> ribbonPath(w, h, radius, density)
    else -> Path()
}

/** Rounded rectangle with a v-notch cut inward on its right edge. */
private fun ribbonPath(w: Float, h: Float, radius: Float, density: Density): Path {
    val r = min(radius, min(w, h) / 2f)
    val notch = min(with(density) { 10.dp.toPx() }, w * 0.12f).coerceAtLeast(4f)
    val cy = h / 2f
    return Path().apply {
        moveTo(r, 0f)
        lineTo(w, 0f)
        lineTo(w - notch, cy)
        lineTo(w, h)
        lineTo(r, h)
        quadraticTo(0f, h, 0f, h - r)
        lineTo(0f, r)
        quadraticTo(0f, 0f, r, 0f)
        close()
    }
}

/** Rounded rectangle with semicircular notches on both short sides. */
private fun ticketPath(w: Float, h: Float, radius: Float, density: Density): Path {
    val r = min(radius, min(w, h) * 0.32f)
    val notch = min(with(density) { 9.dp.toPx() }, h * 0.18f)
    val cy = h / 2f
    val c = notch * 0.66f
    val path = Path()
    path.moveTo(r, 0f)
    path.lineTo(w - r, 0f)
    path.quadraticTo(w, 0f, w, r)
    path.lineTo(w, cy - notch)
    path.cubicTo(w - c, cy - c, w - c, cy + c, w, cy + notch)
    path.lineTo(w, h - r)
    path.quadraticTo(w, h, w - r, h)
    path.lineTo(r, h)
    path.quadraticTo(0f, h, 0f, h - r)
    path.lineTo(0f, cy + notch)
    path.cubicTo(c, cy + c, c, cy - c, 0f, cy - notch)
    path.lineTo(0f, r)
    path.quadraticTo(0f, 0f, r, 0f)
    path.close()
    return path
}

/** Rectangle with the top-right corner sliced off. */
private fun cutCornerPath(w: Float, h: Float): Path {
    val cut = min(h * 0.36f, w * 0.10f).coerceAtLeast(2f)
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(w - cut, 0f)
        lineTo(w, cut)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
}

/** Flat top/bottom with angled left and right ends. */
private fun hexagonPath(w: Float, h: Float): Path {
    val slant = min(h * 0.45f, w * 0.09f).coerceAtLeast(2f)
    return Path().apply {
        moveTo(0f, h / 2f)
        lineTo(slant, 0f)
        lineTo(w - slant, 0f)
        lineTo(w, h / 2f)
        lineTo(w - slant, h)
        lineTo(slant, h)
        close()
    }
}

/**
 * A scalloped "cloud" card. The body is inset by [depth] and every edge
 * bulges back out to the row bounds with a smooth cubic curve. Bump counts
 * adapt to the edge length so wide rows do not turn into a fine comb.
 */
private fun cloudPath(w: Float, h: Float, density: Density): Path {
    val depth = min(with(density) { 11.dp.toPx() }, h * 0.20f).coerceAtLeast(1.5f)
    val left = depth
    val top = depth
    val right = w - depth
    val bottom = h - depth
    // Only slivers with no room for a body fall back to a plain card. The
    // scaled threshold that used to live here changed between the fill rect
    // and the half-stroke-inset border rect, so a Cloud fill could pair with
    // a non-cloud stroke on the same row.
    if (right - left <= 1f || bottom - top <= 1f) {
        return Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(min(w, h) * 0.2f))) }
    }

    val corner = min(with(density) { 14.dp.toPx() }, min(right - left, bottom - top) * 0.28f)
    val horizontal = right - left - 2f * corner
    val vertical = bottom - top - 2f * corner
    val horizontalBumps = (horizontal / (depth * 3f)).roundToInt().coerceIn(2, 14)
    val verticalBumps = (vertical / (depth * 3f)).roundToInt().coerceIn(1, 5)
    val bumpW = horizontal / horizontalBumps
    val bumpH = vertical / verticalBumps
    val bulge = depth / 3f

    val path = Path()
    path.moveTo(left + corner, top)

    // Top: swipe outward between the rounded corners.
    var x = left + corner
    repeat(horizontalBumps) {
        path.cubicTo(
            x + bumpW * 0.25f, top - bulge,
            x + bumpW * 0.75f, top - bulge,
            x + bumpW, top
        )
        x += bumpW
    }
    path.quadraticTo(right, top, right, top + corner)

    // Right side.
    var y = top + corner
    repeat(verticalBumps) {
        path.cubicTo(
            right + bulge, y + bumpH * 0.25f,
            right + bulge, y + bumpH * 0.75f,
            right, y + bumpH
        )
        y += bumpH
    }
    path.quadraticTo(right, bottom, right - corner, bottom)

    // Bottom, right to left.
    x = right - corner
    repeat(horizontalBumps) {
        path.cubicTo(
            x - bumpW * 0.25f, bottom + bulge,
            x - bumpW * 0.75f, bottom + bulge,
            x - bumpW, bottom
        )
        x -= bumpW
    }
    path.quadraticTo(left, bottom, left, bottom - corner)

    // Left side, bottom to top.
    y = bottom - corner
    repeat(verticalBumps) {
        path.cubicTo(
            left - bulge, y - bumpH * 0.25f,
            left - bulge, y - bumpH * 0.75f,
            left, y - bumpH
        )
        y -= bumpH
    }
    path.quadraticTo(left, top, left + corner, top)
    path.close()
    return path
}

/**
 * Solid/dashed/dotted stroke; null means "no border". Dash lengths are in dp,
 * matching the macOS `StrokeStyle` metrics so both platforms draw the same
 * pattern at any density.
 */
fun NestedBorderStyle.toStroke(widthPx: Float, density: Density): Stroke? = when (this) {
    NestedBorderStyle.None -> null
    NestedBorderStyle.Solid -> Stroke(width = widthPx)
    NestedBorderStyle.Dashed -> Stroke(
        width = widthPx,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(
                with(density) { 4.5.dp.toPx() },
                with(density) { 3.dp.toPx() }
            ),
            0f
        ),
        cap = StrokeCap.Round
    )

    NestedBorderStyle.Dotted -> Stroke(
        width = widthPx,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(
                with(density) { 0.5.dp.toPx() },
                with(density) { 2.8.dp.toPx() }
            ),
            0f
        ),
        cap = StrokeCap.Round
    )
}

/**
 * Draws [stroke] inside the card shape. The draw is inset by half the stroke
 * width so a centered [Stroke] lands fully inside the row instead of bleeding
 * into the neighbouring row.
 */
fun Modifier.nestedCardBorder(
    shape: Shape,
    stroke: Stroke?,
    color: Color
): Modifier {
    if (stroke == null) return this
    return drawBehind {
        val insetPx = stroke.width / 2f
        inset(insetPx) {
            drawOutline(
                outline = shape.createOutline(size, layoutDirection, this),
                color = color,
                style = stroke
            )
        }
    }
}

/** Extra inside padding so content clears notches and scallops. */
fun NestedBorderShape.horizontalContentPadding(): Dp = when (this) {
    NestedBorderShape.Cloud,
    NestedBorderShape.Ticket,
    NestedBorderShape.Ribbon -> 8.dp

    NestedBorderShape.Hexagon -> 6.dp
    NestedBorderShape.CutCorner,
    NestedBorderShape.Leaf -> 3.dp

    else -> 0.dp
}

fun NestedBorderShape.verticalContentPadding(): Dp = when (this) {
    NestedBorderShape.Cloud -> 5.dp
    NestedBorderShape.Ticket -> 2.dp
    else -> 0.dp
}
