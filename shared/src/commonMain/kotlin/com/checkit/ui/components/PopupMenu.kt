package com.checkit.ui.components
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
fun AppleStylePopup(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    anchor: @Composable () -> Unit,
    popupContent: @Composable () -> Unit
) {
    Box(modifier = modifier.wrapContentSize()) {
        // Render the clickable item that serves as the visual anchor point
        anchor()

        if (isExpanded) {
            Popup(
                popupPositionProvider = remember { CenteredMenuPositionProvider() },
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true)
            ) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessLow
                        ),
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    ),
                    exit = scaleOut(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    )
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.wrapContentSize(),
                        content = popupContent
                    )
                }
            }
        }
    }
}

private class CenteredMenuPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val anchorCenter = anchorBounds.left + (anchorBounds.width / 2)
        val idealX = anchorCenter - (popupContentSize.width / 2)
        val maxAllowedX = windowSize.width - popupContentSize.width

        val finalX = idealX.coerceIn(40, (maxAllowedX-40).coerceAtLeast(40))
        val finalY = anchorBounds.bottom + 8
        return IntOffset(finalX, finalY)
    }
}
