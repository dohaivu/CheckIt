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
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import com.checkit.domain.usecase.ObserveNotesForDateUseCase
import com.checkit.shared.R
import com.checkit.ui.myday.DayViewProjection
import com.checkit.ui.myday.doneWorkMinutes
import com.checkit.ui.myday.toDayViewProjection
import com.checkit.ui.tasks.cardColor
import com.checkit.ui.tasks.isOverdue
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.tasks.toDurationLabel
import com.checkit.ui.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.glance.color.ColorProvider as DayNightColorProvider

val today = today()
class DailyPlanAgendaWidget : GlanceAppWidget(), KoinComponent {

    private val observeNotesForDate: ObserveNotesForDateUseCase by inject()
    private val observeDailyPlans: ObserveDailyPlansUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = today()
        val notes = observeNotesForDate(today).first()
        val dailyPlans = observeDailyPlans(startDate = today, endDate = today).first()
        val todayPlan = dailyPlans.find { it.date == today }
        val items = todayPlan?.items ?: emptyList()

        // Get current time for highlighting
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val nowMinutes = now.hour * 60 + now.minute

        provideContent {
            val projection = remember(items, notes) { items.toDayViewProjection(notes, emptyList()) }
            val allDayItems = remember(projection) {
                projection.toWidgetItems(timed = false)
            }
            val timedItems = remember(projection) {
                projection.toWidgetItems(timed = true)
            }

            val totalCount = remember(allDayItems, timedItems) { allDayItems.size + timedItems.size }
            val doneCount = remember(allDayItems, timedItems) {
                allDayItems.count { it.completed } + timedItems.count { it.completed }
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
                            .fillMaxWidth(),
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
                        Row(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(actionStartActivity<MainActivity>()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Day",
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = GlanceTheme.colors.onSurface
                                )
                            )
                            if (totalCount > 0) {
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                val doneMinutes = todayPlan.doneWorkMinutes()
                                val countLabel = if (doneMinutes > 0) {
                                    "$doneCount/$totalCount (${doneMinutes.toDurationLabel(compact = true)})"
                                } else {
                                    "$doneCount/$totalCount"
                                }
                                Text(
                                    text = countLabel,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .cornerRadius(16.dp)
                                .clickable(openNewJournalEntryAction()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.notes_24px),
                                contentDescription = "Add journal entry",
                                modifier = GlanceModifier.size(22.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .cornerRadius(16.dp)
                                .clickable(openQuickSprintAction()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.bolt_24px),
                                contentDescription = "Open quick sprint",
                                modifier = GlanceModifier.size(24.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .cornerRadius(16.dp)
                                .clickable(openSuggestionsAction()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.lightbulb_24px),
                                contentDescription = "Open suggestions",
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .size(32.dp)
                                .cornerRadius(16.dp)
                                .clickable(actionRunCallback<RefreshAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.refresh_24px),
                                contentDescription = "Refresh data",
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                            )
                        }
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

    private fun openSuggestionsAction(): Action = actionStartActivity<MainActivity>(
        parameters = actionParametersOf(OpenMyDaySuggestionsParameterKey to true)
    )

    private fun openQuickSprintAction(): Action = actionStartActivity<MainActivity>(
        parameters = actionParametersOf(OpenQuickSprintParameterKey to true)
    )

    private fun openNewJournalEntryAction(): Action = actionStartActivity<MainActivity>(
        parameters = actionParametersOf(OpenNewJournalEntryParameterKey to true)
    )

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
                modifier = GlanceModifier.width(48.dp).height(32.dp),
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
            Spacer(modifier = GlanceModifier.width(8.dp))

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
            Spacer(modifier = GlanceModifier.width(8.dp))

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
            label = item.label,
            supportingText = if (allDay) null else item.timeLabel,
            baseColor = item.color,
            allDay = allDay,
            overdue = item.overdue,
            clickAction = item.clickAction(),
            icon = {
                when (item) {
                    is GlanceAgendaItem.Note -> NoteIcon(item.color)
                    is GlanceAgendaItem.DailyPlan -> DailyPlanIcon(source = item.item.source, completed = item.completed, item.color)
                }
            }
        )
    }

    @Composable
    private fun GlanceTypeCard(
        title: String,
        label: String?,
        supportingText: String?,
        baseColor: Color,
        allDay: Boolean,
        overdue: Boolean,
        clickAction: Action,
        icon: @Composable () -> Unit
    ) {
        val cardHeight = if (allDay) 32.dp else 48.dp
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(cardHeight)
                .cornerRadius(8.dp)
                .background(baseColor.alphaProvider(DefaultCardBackgroundAlpha))
                .clickable(clickAction)
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
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            modifier = GlanceModifier
                                .defaultWeight(),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GlanceTheme.colors.onSurface
                            ),
                            maxLines = 1
                        )
                        if (!label.isNullOrEmpty()) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = GlanceTheme.colors.primary
                                )
                            )
                        }
                    }
                    if (supportingText != null) {
                        Text(
                            text = supportingText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = if (overdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun NoteIcon(tintColor: Color) {
        Image(
            provider = ImageProvider(R.drawable.notes_24px),
            contentDescription = "note icon",
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(
                ColorProvider(tintColor)
            )
        )
    }

    @Composable
    private fun DailyPlanIcon(source: DailyPlanItemSource, completed: Boolean, tintColor: Color) {
        Image(
            provider = ImageProvider(
                resId = if (source == DailyPlanItemSource.MyDayNote) R.drawable.event_note_24px
                        else if (source == DailyPlanItemSource.MyDayReminder) R.drawable.schedule_24px
                        else if (completed) R.drawable.check_box_24px
                        else R.drawable.check_box_outline_blank_24px
            ),
            contentDescription = "daily plan icon",
            modifier = GlanceModifier.size(22.dp),
            colorFilter = ColorFilter.tint(
                ColorProvider(tintColor)
            )
        )
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        DailyPlanAgendaWidget().update(context, glanceId)
    }
}

private sealed class GlanceAgendaItem {
    abstract val startTimeMinutes: Int?
    abstract val endTimeMinutes: Int?
    abstract val sortOrder: Int
    abstract val title: String
    abstract val label: String?
    abstract val color: Color
    abstract val completed: Boolean
    abstract val overdue: Boolean
    abstract val dailyPlanItemId: Long?
    abstract val taskId: Long?
    abstract val noteId: Long?

    val timeLabel: String?
        get() = startTimeMinutes?.let { start ->
            endTimeMinutes?.let { end -> "${start.toClockLabel()} - ${end.toClockLabel()}" } ?: start.toClockLabel()
        }

    fun clickAction(): Action {
        val currentTaskId = taskId
        val currentNoteId = noteId
        val currentDailyPlanItemId = dailyPlanItemId
        return when {
            currentTaskId != null && currentDailyPlanItemId != null -> actionStartActivity<MainActivity>(
                parameters = actionParametersOf(
                    TaskIdParameterKey to currentTaskId,
                    DailyPlanItemIdParameterKey to currentDailyPlanItemId
                )
            )
            currentTaskId != null -> actionStartActivity<MainActivity>(
                parameters = actionParametersOf(TaskIdParameterKey to currentTaskId)
            )
            currentNoteId != null -> actionStartActivity<MainActivity>(
                parameters = actionParametersOf(NoteIdParameterKey to currentNoteId)
            )
            currentDailyPlanItemId != null -> actionStartActivity<MainActivity>(
                parameters = actionParametersOf(DailyPlanItemIdParameterKey to currentDailyPlanItemId)
            )
            else -> actionStartActivity<MainActivity>()
        }
    }

    data class Note(
        val note: NoteItem
    ) : GlanceAgendaItem() {
        override val startTimeMinutes: Int? = note.startTimeMinutes
        override val endTimeMinutes: Int? = note.startTimeMinutes?.let { it + DefaultNoteDurationMinutes }
        override val sortOrder: Int = note.sortOrder
        override val title: String = note.title.ifBlank { note.content.ifBlank { "Empty note" } }
        override val label: String? = note.label
        override val color: Color = note.cardColor()
        override val completed: Boolean = note.status == TaskStatus.Completed
        override val overdue: Boolean = false
        override val dailyPlanItemId: Long? = null
        override val taskId: Long? = null
        override val noteId: Long = note.id
    }

    data class DailyPlan(
        val item: DailyPlanItem
    ) : GlanceAgendaItem() {
        override val startTimeMinutes: Int? = item.startTimeMinutes
        override val endTimeMinutes: Int? = item.endTimeMinutes
        override val sortOrder: Int = item.sortOrder
        override val title: String = item.widgetTitle()
        override val label: String? = item.label
        override val color: Color = item.cardColor()
        override val completed: Boolean = item.status == DailyPlanItemStatus.Done
        override val overdue: Boolean = item.isOverdue(today)
        override val dailyPlanItemId: Long = item.id
        override val taskId: Long? = item.taskId
        override val noteId: Long? = null
    }
}

private fun DayViewProjection.toWidgetItems(timed: Boolean): List<GlanceAgendaItem> {
    val widgetItems = items.map { GlanceAgendaItem.DailyPlan(it) } + notes.map { GlanceAgendaItem.Note(it) }
    
    return widgetItems
        .asSequence()
        .filter { (it.startTimeMinutes != null) == timed }
        .sortedWith(compareBy<GlanceAgendaItem> { it.startTimeMinutes ?: -1 }.thenBy { it.sortOrder })
        .toList()
}

private fun DailyPlanItem.widgetTitle(): String =
    when (source) {
        DailyPlanItemSource.MyDayNote -> checkInNoteTitle()
        DailyPlanItemSource.MyDayReminder -> reminderTitle()
        DailyPlanItemSource.MyDayTask -> title.ifBlank { "Done item" }
        DailyPlanItemSource.ExistingTask -> title.ifBlank { "Untitled task" }
    }

private fun DailyPlanItem.checkInNoteTitle(): String =
    title
        .ifBlank { note.orEmpty() }
        .ifBlank { "Empty note" }

private fun DailyPlanItem.reminderTitle(): String =
    title
        .ifBlank { note.orEmpty() }
        .ifBlank { "Reminder" }

private fun Color.provider(): ColorProvider = DayNightColorProvider(day = this, night = this)

private fun Color.alphaProvider(alpha: Float): ColorProvider =
    copy(alpha = alpha).provider()

private const val DefaultNoteDurationMinutes = 30
private const val DefaultCardBackgroundAlpha = 0.12f
