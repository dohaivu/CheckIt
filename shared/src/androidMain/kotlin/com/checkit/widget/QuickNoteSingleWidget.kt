package com.checkit.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.checkit.MainActivity
import com.checkit.domain.usecase.ObserveQuickNotesForWidgetUseCase
import com.checkit.shared.R
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.color.ColorProvider as DayNightColorProvider

class QuickNoteSingleWidget : GlanceAppWidget(), KoinComponent {

    private val observeQuickNotesForWidget: ObserveQuickNotesForWidgetUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dateFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val currentFormattedDate = dateFormatter.format(Date())

        val translucentBackground = DayNightColorProvider(
            day = Color(0x4DFFFFFF), 
            night = Color(0x3D1C1B1F)
        )

        val subtleIconTint = DayNightColorProvider(
            day = Color(0x661C1B1F), 
            night = Color(0x66E6E1E5)
        )

        provideContent {
            val notes by observeQuickNotesForWidget(limit = 1).collectAsState(initial = emptyList())
            val topNote = remember(notes) { notes.firstOrNull() }

            GlanceTheme {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .cornerRadius(80.dp)
                        .background(translucentBackground)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.mipmap.ic_launcher),
                        contentDescription = "App icon",
                        modifier = GlanceModifier
                            .size(56.dp)
                            .cornerRadius(56.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(12.dp))

                    if (topNote != null) {
                        Text(
                            text = topNote.content.ifBlank { "Empty note" },
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 3
                        )
                    } else {
                        Text(
                            text = currentFormattedDate,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .cornerRadius(16.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable(actionRunCallback<RefreshQuickNoteWidgetAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.refresh_24px),
                            contentDescription = "Refresh",
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = ColorFilter.tint(subtleIconTint)
                        )
                    }
                }

            }
        }
    }
}

class RefreshQuickNoteWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        QuickNoteSingleWidget().update(context, glanceId)
    }
}
