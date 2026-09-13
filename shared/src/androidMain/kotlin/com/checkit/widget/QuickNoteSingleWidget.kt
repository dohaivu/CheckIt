package com.checkit.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.checkit.MainActivity
import com.checkit.domain.QuickNote
import com.checkit.domain.usecase.ObserveQuickNextUseCase
import com.checkit.shared.R
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuickNoteSingleWidget : GlanceAppWidget(), KoinComponent {

    private val observeQuickNext: ObserveQuickNextUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val topNote = observeQuickNext().first()
            .sortedWith(compareByDescending<QuickNote> { it.priority }.thenBy { it.sortOrder })
            .firstOrNull()

        provideContent {
            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.quick_note_widget_background))
                        .cornerRadius(32.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = GlanceModifier.size(48.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = topNote?.content?.ifBlank { "Empty note" } ?: "No quick notes yet",
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = if (topNote == null) {
                                GlanceTheme.colors.onSurfaceVariant
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
