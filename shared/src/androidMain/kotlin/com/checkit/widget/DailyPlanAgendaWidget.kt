package com.checkit.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.unit.ColorProvider
import com.checkit.MainActivity
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemSource
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.NoteItem
import com.checkit.domain.TaskItem
import com.checkit.domain.TaskStatus
import com.checkit.domain.usecase.ObserveDailyPlansUseCase
import com.checkit.domain.usecase.ObserveTaskBoardUseCase
import com.checkit.shared.R
import com.checkit.ui.myday.MyDayTaskViewProjection
import com.checkit.ui.myday.PlannedTaskProjection
import com.checkit.ui.myday.dailyItemColor
import com.checkit.ui.myday.toTaskViewProjection
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.glance.color.ColorProvider as DayNightColorProvider

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
            val allDayItems = remember(projection) {
                projection.toWidgetItems(timed = false)
            }
            val timedItems = remember(projection) {
                projection.toWidgetItems(timed = true)
            }

            // Find the index of the first item that starts AFTER now
            val nextTimedItemIndex = remember(timedItems, nowMinutes) {
                timedItems.indexOfFirst { (it.startTimeMinutes ?: -1) > nowMinutes }
            }

            val hasAllDay = allDayItems.isNotEmpty()

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
                                            allDayItems.forEachIndexed { index, item ->
                                                GlanceAgendaCard(item = item, allDay = true)
                                                if (index < allDayItems.lastIndex) {
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
                                    GlanceAgendaCard(item = item, allDay = false)
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
        val markerColor = if (isHighlighted) themeColors.error else DayNightColorProvider(day = Color.Black.copy(alpha = 0.2f), night = Color.White.copy(alpha = 0.2f))
        val lineColor = DayNightColorProvider(day = Color.Black.copy(alpha = 0.15f), night = Color.White.copy(alpha = 0.15f))
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
    private fun GlanceAgendaCard(item: GlanceAgendaItem, allDay: Boolean) {
        GlanceTypeCard(
            title = item.title,
            supportingText = if (allDay) null else item.timeLabel,
            baseColor = item.color,
            allDay = allDay,
            icon = {
                when (item) {
                    is GlanceAgendaItem.Task -> TaskIcon(completed = item.completed, tintColor = item.color)
                    is GlanceAgendaItem.Note -> NoteIcon()
                    is GlanceAgendaItem.DailyPlan -> DailyPlanIcon()
                }
            }
        )
    }

    @Composable
    private fun GlanceTypeCard(
        title: String,
        supportingText: String?,
        baseColor: Color,
        allDay: Boolean,
        icon: @Composable () -> Unit
    ) {
        val cardHeight = if (allDay) 32.dp else 48.dp
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(cardHeight)
                .cornerRadius(8.dp)
                .background(baseColor.alphaProvider(DefaultCardBackgroundAlpha))
        ) {
            Spacer(
                modifier = GlanceModifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(baseColor.provider())
            )
            Spacer(GlanceModifier.width(8.dp))
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
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
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskIcon(completed: Boolean, tintColor: Color) {
        Image(
            provider = ImageProvider( if (completed) R.drawable.check_box_24px else R.drawable.check_box_outline_blank_24px),
            contentDescription = "check icon",
            modifier = GlanceModifier.size(24.dp),
//            colorFilter = ColorFilter.tint(
//                ColorProvider(tintColor)
//            )
        )
    }

    @Composable
    private fun NoteIcon() {
        Image(
            provider = ImageProvider(R.drawable.notes_24px),
            contentDescription = "note icon",
            modifier = GlanceModifier.size(22.dp),
        )
    }

    @Composable
    private fun DailyPlanIcon() {
        Image(
            provider = ImageProvider(R.drawable.event_available_24px),
            contentDescription = "daily plan icon",
            modifier = GlanceModifier.size(22.dp),
        )
    }
}

private sealed class GlanceAgendaItem {
    abstract val startTimeMinutes: Int?
    abstract val endTimeMinutes: Int?
    abstract val sortOrder: Int
    abstract val title: String
    abstract val color: Color
    abstract val completed: Boolean

    val timeLabel: String?
        get() = startTimeMinutes?.let { start ->
            endTimeMinutes?.let { end -> "${start.toClockLabel()} - ${end.toClockLabel()}" } ?: start.toClockLabel()
        }

    data class Task(
        val projection: PlannedTaskProjection
    ) : GlanceAgendaItem() {
        private val task: TaskItem = projection.task
        private val item: DailyPlanItem = projection.dailyPlanItem
        override val startTimeMinutes: Int? = item.startTimeMinutes
        override val endTimeMinutes: Int? = item.endTimeMinutes
        override val sortOrder: Int = item.sortOrder
        override val title: String = task.name.ifBlank { "Untitled task" }
        override val color: Color = dailyItemColor(task = task, list = task.list)
        override val completed: Boolean = item.status == DailyPlanItemStatus.Done
    }

    data class Note(
        val note: NoteItem
    ) : GlanceAgendaItem() {
        override val startTimeMinutes: Int? = note.startTimeMinutes
        override val endTimeMinutes: Int? = note.startTimeMinutes?.let { it + DefaultNoteDurationMinutes }
        override val sortOrder: Int = note.sortOrder
        override val title: String = note.title.ifBlank { note.content.ifBlank { "Empty note" } }
        override val color: Color = note.cardColor()
        override val completed: Boolean = note.status == TaskStatus.Completed
    }

    data class DailyPlan(
        val item: DailyPlanItem
    ) : GlanceAgendaItem() {
        override val startTimeMinutes: Int? = item.startTimeMinutes
        override val endTimeMinutes: Int? = item.endTimeMinutes
        override val sortOrder: Int = item.sortOrder
        override val title: String = item.widgetTitle()
        override val color: Color = dailyItemColor(task = null, list = null)
        override val completed: Boolean = item.status == DailyPlanItemStatus.Done
    }
}

private fun MyDayTaskViewProjection.toWidgetItems(timed: Boolean): List<GlanceAgendaItem> {
    val items = plannedTasks.map { GlanceAgendaItem.Task(it) } +
        notes.map { GlanceAgendaItem.Note(it) } +
        checkIns.map { GlanceAgendaItem.DailyPlan(it) }
    return items
        .asSequence()
        .filter { (it.startTimeMinutes != null) == timed }
        .sortedWith(compareBy<GlanceAgendaItem> { it.startTimeMinutes ?: -1 }.thenBy { it.sortOrder })
        .toList()
}

private fun DailyPlanItem.widgetTitle(): String =
    when (source) {
        DailyPlanItemSource.CheckInNote -> note.orEmpty().ifBlank { "Empty note" }
        DailyPlanItemSource.CheckInManualDone -> titleSnapshot.ifBlank { "Done item" }
        DailyPlanItemSource.ExistingTask -> titleSnapshot.ifBlank { "Untitled task" }
    }

private fun Color.provider(): ColorProvider = DayNightColorProvider(day = this, night = this)

private fun Color.alphaProvider(alpha: Float): ColorProvider =
    copy(alpha = alpha).provider()

private const val DefaultNoteDurationMinutes = 30
private const val DefaultCardBackgroundAlpha = 0.12f
