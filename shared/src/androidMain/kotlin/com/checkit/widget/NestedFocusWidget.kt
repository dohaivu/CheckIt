package com.checkit.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.checkit.MainActivity
import com.checkit.domain.NestedDocumentTree
import com.checkit.domain.flattenFocusRows
import com.checkit.domain.focusSubtrees
import com.checkit.domain.parseRichText
import com.checkit.domain.usecase.ObserveNestedDocumentTreeUseCase
import com.checkit.shared.R
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal val NestedFocusDocumentKey = stringPreferencesKey("nested_focus_document_id")
internal val NestedFocusDepthKey = intPreferencesKey("nested_focus_max_depth")

/** Default levels of descendants below each promoted high-priority item. */
internal const val NESTED_FOCUS_DEFAULT_DEPTH = 2

/** High-contrast per-depth hues for rows with children, tuned per mode. */
private val NestedFocusDepthColors = listOf(
    Color(0xFFC62828) to Color(0xFFEF5350), // 0 red
    Color(0xFF1565C0) to Color(0xFF64B5F6), // 1 blue
    Color(0xFF2E7D32) to Color(0xFF81C784), // 2 green
    Color(0xFFE65100) to Color(0xFFFFB74D), // 3 orange
).map { (day, night) -> DayNightColorProvider(day = day, night = night) }

class NestedFocusWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    private val observeTree: ObserveNestedDocumentTreeUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val maxDepth = currentState(NestedFocusDepthKey) ?: NESTED_FOCUS_DEFAULT_DEPTH
            val documentId = currentState(NestedFocusDocumentKey)
            // One-shot read: manual refresh only, no live collection.
            val tree by produceState<NestedDocumentTree?>(initialValue = null, documentId) {
                value = documentId?.let { observeTree(it).first() }
            }

            val subtleIconTint = DayNightColorProvider(
                day = Color(0x661C1B1F),
                night = Color(0x66E6E1E5)
            )

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(24.dp)
                        .background(GlanceTheme.colors.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = GlanceModifier
                                .size(20.dp)
                                .clickable(actionStartActivity<MainActivity>()),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = tree?.document?.title?.ifBlank { "High priority" } ?: "High priority",
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .size(28.dp)
                                .cornerRadius(14.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable(actionRunCallback<RefreshNestedFocusWidgetAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.refresh_24px),
                                contentDescription = "Refresh",
                                modifier = GlanceModifier.size(18.dp),
                                colorFilter = ColorFilter.tint(subtleIconTint)
                            )
                        }
                    }

                    val rows = remember(tree, maxDepth) {
                        val roots = tree?.rootNodes.orEmpty()
                        flattenFocusRows(focusSubtrees(roots, maxDepth))
                    }
                    if (rows.isEmpty()) {
                        Text(
                            text = if (documentId == null) "Choose a document" else "No high-priority items",
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 2
                        )
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                            items(rows) { row ->
                                val (item, depth, hasChildren) = row
                                Text(
                                    // Glance Text is String-only: strip markers, no spans.
                                    text = parseRichText(item.text).text.ifBlank { "Untitled item" },
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(start = (depth * 12).dp)
                                        .padding(vertical = 3.dp)
                                        .clickable(actionStartActivity<MainActivity>()),
                                    style = TextStyle(
                                        fontWeight = if (depth == 0) FontWeight.Bold else if (hasChildren) FontWeight.Medium else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (hasChildren) {
                                            NestedFocusDepthColors[depth % NestedFocusDepthColors.size]
                                        } else {
                                            GlanceTheme.colors.onSurface
                                        }
                                    ),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class RefreshNestedFocusWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NestedFocusWidget().update(context, glanceId)
    }
}
