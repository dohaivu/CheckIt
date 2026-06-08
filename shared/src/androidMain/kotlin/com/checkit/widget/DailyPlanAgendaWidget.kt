package com.checkit.widget

import android.content.Context
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.checkit.MainActivity
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskBoard
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.shared.R
import com.checkit.ui.myday.dailyItemColor
import com.checkit.ui.myday.toTaskViewProjection
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyPlanAgendaWidget : GlanceAppWidget(), KoinComponent {

    private val observeTaskBoard: ObserveTaskBoardUseCase by inject()
    private val observeDailyPlans: ObserveDailyPlansUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val board = observeTaskBoard().first()
        val dailyPlans = observeDailyPlans().first()
        val today = today()
        val todayPlan = dailyPlans.find { it.date == today }
        val items = todayPlan?.items ?: emptyList()

        // Get current time for highlighting
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val nowMinutes = now.hour * 60 + now.minute

        provideContent {
            val projection = remember(items, board) { items.toTaskViewProjection(board, today) }
            val allDayTasks = remember(projection) { projection.tasks.filter { it.startTimeMinutes == null } }
            val allDayNotes = remember(projection) { projection.notes.filter { it.startTimeMinutes == null } }
            val timedTasks = remember(projection) { projection.tasks.filter { it.startTimeMinutes != null } }
            val timedNotes = remember(projection) { projection.notes.filter { it.startTimeMinutes != null } }

            val timedItems = remember(timedTasks, timedNotes) {
                (timedTasks.map { GlanceAgendaItem.Task(it) } + timedNotes.map { GlanceAgendaItem.Note(it) })
                    .sortedBy { it.startTimeMinutes }
            }

            // Find the index of the first item that starts AFTER now
            val nextTimedItemIndex = remember(timedItems, nowMinutes) {
                timedItems.indexOfFirst { (it.startTimeMinutes ?: -1) > nowMinutes }
            }

            val hasAllDay = allDayTasks.isNotEmpty() || allDayNotes.isNotEmpty()

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity<MainActivity>()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = GlanceModifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "My Day",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = GlanceTheme.colors.onSurface
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(12.dp))

                    if (!hasAllDay && timedItems.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nothing planned for today",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            if (hasAllDay) {
                                item {
                                    val hasTimed = timedItems.isNotEmpty()
                                    GlanceAgendaAxisRow(
                                        label = "All Day",
                                        isFirst = true,
                                        isLast = !hasTimed,
                                        isHighlighted = false
                                    ) {
                                        Column {
                                            allDayTasks.forEachIndexed { index, task ->
                                                GlanceTaskCard(task = task, board = board)
                                                if (index < allDayTasks.lastIndex || allDayNotes.isNotEmpty()) {
                                                    Spacer(GlanceModifier.height(6.dp))
                                                }
                                            }
                                            allDayNotes.forEachIndexed { index, note ->
                                                GlanceNoteCard(note = note, board = board)
                                                if (index < allDayNotes.lastIndex) {
                                                    Spacer(GlanceModifier.height(6.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            itemsIndexed(timedItems) { index, item ->
                                val label = item.startTimeMinutes?.toClockLabel() ?: ""
                                GlanceAgendaAxisRow(
                                    label = label,
                                    isFirst = index == 0 && !hasAllDay,
                                    isLast = index == timedItems.lastIndex,
                                    isHighlighted = index == nextTimedItemIndex
                                ) {
                                    when (item) {
                                        is GlanceAgendaItem.Task -> GlanceTaskCard(task = item.task, board = board)
                                        is GlanceAgendaItem.Note -> GlanceNoteCard(note = item.note, board = board)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GlanceAgendaAxisRow(
        label: String,
        isFirst: Boolean = false,
        isLast: Boolean = false,
        isHighlighted: Boolean = false,
        content: @Composable () -> Unit
    ) {
        val themeColors = GlanceTheme.colors
        val accentColor = if (isHighlighted) themeColors.error else themeColors.onSurfaceVariant
        val markerColor = if (isHighlighted) themeColors.error else ColorProvider(day = Color.Black.copy(alpha = 0.2f), night = Color.White.copy(alpha = 0.2f))
        val lineColor = ColorProvider(day = Color.Black.copy(alpha = 0.15f), night = Color.White.copy(alpha = 0.15f))
        val labelWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
        ) {
            // Label
            Box(
                modifier = GlanceModifier.width(44.dp).height(32.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = labelWeight
                    )
                )
            }
            Spacer(modifier = GlanceModifier.width(10.dp))

            // Marker & Vertical Line
            Box(
                modifier = GlanceModifier.width(12.dp).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Connecting Line Container
                Column(
                    modifier = GlanceModifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Line above dot
                    if (!isFirst) {
                        Spacer(
                            modifier = GlanceModifier
                                .width(1.5.dp)
                                .height(16.dp)
                                .background(lineColor)
                        )
                    } else {
                        Spacer(modifier = GlanceModifier.height(16.dp))
                    }
                    
                    // Line below dot
                    if (!isLast) {
                        Spacer(
                            modifier = GlanceModifier
                                .width(1.5.dp)
                                .defaultWeight()
                                .background(lineColor)
                        )
                    }
                }

                // Marker Dot Container (overlay)
                Box(
                    modifier = GlanceModifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(12.dp)
                            .background(markerColor)
                            .cornerRadius(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(6.dp)
                                .background(GlanceTheme.colors.widgetBackground)
                                .cornerRadius(3.dp)
                        ) {}
                    }
                }
            }
            Spacer(modifier = GlanceModifier.width(10.dp))

            // Content
            Box(modifier = GlanceModifier.defaultWeight().padding(bottom = 8.dp)) {
                content()
            }
        }
    }

    @Composable
    private fun GlanceTaskCard(task: TaskItem, board: TaskBoard) {
        val list = board.lists.find { it.id == task.listId }
        val baseColor = dailyItemColor(task, list)
        
        GlanceItemCard(
            title = task.name.ifBlank { "Untitled task" },
            timeLabel = if (task.startTimeMinutes != null) task.timeRangeLabel() else null,
            baseColor = baseColor,
            isCompleted = task.status == TaskStatus.Completed
        )
    }

    @Composable
    private fun GlanceNoteCard(note: NoteItem, board: TaskBoard) {
        val list = board.lists.find { it.id == note.listId }
        val baseColor = dailyItemColor(null, list)

        GlanceItemCard(
            title = note.content.ifBlank { "Empty note" },
            timeLabel = note.startTimeMinutes?.toClockLabel(),
            baseColor = baseColor,
            isCompleted = note.status == TaskStatus.Completed
        )
    }

    @Composable
    private fun GlanceItemCard(
        title: String,
        timeLabel: String?,
        baseColor: Color,
        isCompleted: Boolean
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(day = baseColor.copy(alpha = 0.12f), night = baseColor.copy(alpha = 0.12f)))
                .padding(end = 8.dp)
                .cornerRadius(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator bar
            Spacer(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(ColorProvider(day = baseColor, night = baseColor))
                    .cornerRadius(8.dp)
            )
            Spacer(GlanceModifier.width(8.dp))
            
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
            if (isCompleted) {
                // Completed indicator (simple colored dot)
                Box(
                    modifier = GlanceModifier
                        .size(10.dp)
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(5.dp)
                ) {}
            }
        }
    }

    private sealed class GlanceAgendaItem {
        data class Task(val task: TaskItem) : GlanceAgendaItem()
        data class Note(val note: NoteItem) : GlanceAgendaItem()

        val startTimeMinutes: Int? get() = when (this) {
            is Task -> task.startTimeMinutes
            is Note -> note.startTimeMinutes
        }
    }
}

private fun TaskItem.timeRangeLabel(): String {
    val start = startTimeMinutes?.toClockLabel() ?: return ""
    val end = endTimeMinutes?.toClockLabel() ?: return start
    return "$start - $end"
}
