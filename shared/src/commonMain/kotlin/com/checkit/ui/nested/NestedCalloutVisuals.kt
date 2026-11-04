package com.checkit.ui.nested

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Dangerous
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.checkit.domain.NestedCalloutLayout
import com.checkit.domain.NestedCalloutSpec
import com.checkit.domain.NestedCalloutStyle
import com.checkit.domain.NestedCalloutTitlePlacement

/**
 * Maps the shared [com.checkit.domain.NestedCalloutSpec.iconKey] to a Compose
 * vector. Both the formatting picker and the row header use the same lookup so
 * a kind cannot drift between the menu and the canvas.
 */
internal fun nestedCalloutIcon(iconKey: String): ImageVector = when (iconKey) {
    "info" -> Icons.Outlined.Info
    "lightbulb" -> Icons.Outlined.Lightbulb
    "sparkles" -> Icons.Outlined.AutoAwesome
    "important" -> Icons.Outlined.PriorityHigh
    "warning" -> Icons.Outlined.Warning
    "caution" -> Icons.Outlined.Dangerous
    "question" -> Icons.Outlined.HelpOutline
    "quote" -> Icons.Outlined.FormatQuote
    else -> Icons.Outlined.Info
}

/** Icon for a callout; [NestedCalloutStyle.None] falls back to the picker glyph. */
internal fun nestedCalloutIcon(style: NestedCalloutStyle): ImageVector =
    style.spec()?.let { nestedCalloutIcon(it.iconKey) } ?: Icons.Outlined.Campaign

/**
 * Row title treatment. Panel uses an uppercase eyebrow, Tab a filled label
 * chip, and Rail an inline accent label next to the icon. The accent color is
 * shared with the frame so all three read as one callout.
 */
@Composable
internal fun NestedCalloutHeader(
    spec: NestedCalloutSpec,
    title: String,
    accent: Color,
    focusAlpha: Float
) {
    val icon = nestedCalloutIcon(spec.iconKey)
    when (spec.titlePlacement) {
        NestedCalloutTitlePlacement.Eyebrow -> Row(
            modifier = Modifier.alpha(focusAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        NestedCalloutTitlePlacement.TopTab -> Row(
            modifier = Modifier
                .alpha(focusAlpha)
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }

        NestedCalloutTitlePlacement.Inline -> Row(
            modifier = Modifier.alpha(focusAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Vertical gap between the callout header and the row text. */
internal fun NestedCalloutLayout.headerGapDp(): Int = when (this) {
    NestedCalloutLayout.Tab -> 4
    else -> 3
}
