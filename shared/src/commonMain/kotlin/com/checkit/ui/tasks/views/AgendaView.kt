package com.checkit.ui.tasks.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.checkit.ui.shortName
import com.checkit.ui.shortMonthName
import com.checkit.ui.tasks.TimelineItem
import com.checkit.ui.tasks.toClockLabel
import com.checkit.ui.today
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Composable
internal fun AgendaView(
    items: List<TimelineItem>,
    onItemClick: (TimelineItem) -> Unit,
    dayLimit: Int? = null,
    focusedDate: LocalDate = today(),
    modifier: Modifier = Modifier,
    itemContent: @Composable (TimelineItem) -> Unit
) {
    val today = today()
    val itemsByDate = remember(items) { items.groupedByAgendaDate() }
    val boundedDayCount = dayLimit?.coerceAtLeast(1)

    val visibleDates = remember(itemsByDate, today, focusedDate, boundedDayCount) {
        if (boundedDayCount != null) {
            val initialIndex = 0
            List(boundedDayCount) { index ->
                focusedDate.plus(index - initialIndex, DateTimeUnit.DAY)
            }
        } else {
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            (itemsByDate.keys + today + tomorrow).distinct().sorted()
        }
    }

    val initialIndex = remember(visibleDates, focusedDate) {
        val exactIndex = visibleDates.indexOf(focusedDate)
        if (exactIndex != -1) {
            exactIndex
        } else {
            val index = visibleDates.indexOfFirst { it > focusedDate }
            if (index != -1) index else (visibleDates.size - 1).coerceAtLeast(0)
        }
    }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val nowMinutes = now.hour * 60 + now.minute

    LaunchedEffect(focusedDate, initialIndex) {
        if (initialIndex in visibleDates.indices) {
            state.scrollToItem(initialIndex)
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                count = visibleDates.size,
                key = { index -> "agenda-day-${visibleDates[index]}" }
            ) { index ->
                val date = visibleDates[index]
                AgendaDaySection(
                    date = date,
                    today = today,
                    items = itemsByDate[date] ?: AgendaDayItems.Empty,
                    nowMinutes = nowMinutes,
                    showHeader = boundedDayCount != 1,
                    onItemClick = onItemClick,
                    itemContent = itemContent
                )
            }
        }

        if (boundedDayCount != 1) {
            val todayIndex = remember(visibleDates, today) {
                visibleDates.indexOf(today).coerceAtLeast(0)
            }
            FilledTonalButton(
                onClick = { scope.launch { state.animateScrollToItem(todayIndex) } },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Today")
            }
        }
    }
}

@Composable
private fun AgendaDaySection(
    date: LocalDate,
    today: LocalDate,
    items: AgendaDayItems,
    nowMinutes: Int,
    showHeader: Boolean,
    onItemClick: (TimelineItem) -> Unit,
    itemContent: @Composable (TimelineItem) -> Unit
) {
    val nextTimedItemIndex = remember(date, today, items.timedItems, nowMinutes) {
        if (date == today) {
            items.timedItems.indexOfFirst { (it.startTimeMinutes ?: 0) > nowMinutes }
        } else {
            -1
        }
    }

    Column {
        if (showHeader) {
            AgendaDayHeader(date = date, today = today)
            Spacer(Modifier.height(8.dp))
        }
        if (items.isEmpty) {
            AgendaEmptyDay()
        } else {
            if (items.allDayItems.isNotEmpty()) {
                AgendaAllDayRow(
                    items = items.allDayItems,
                    showBottomLine = items.timedItems.isNotEmpty(),
                    onItemClick = onItemClick,
                    itemContent = itemContent
                )
            }
            items.timedItems.forEachIndexed { index, item ->
                AgendaTimedRow(
                    item = item,
                    showTopLine = index > 0 || items.allDayItems.isNotEmpty(),
                    showBottomLine = index < items.timedItems.lastIndex,
                    isHighlighted = index == nextTimedItemIndex,
                    onItemClick = onItemClick,
                    itemContent = itemContent
                )
            }
        }
    }
}

private data class AgendaDayItems(
    val allDayItems: List<TimelineItem>,
    val timedItems: List<TimelineItem>
) {
    val isEmpty: Boolean get() = allDayItems.isEmpty() && timedItems.isEmpty()

    companion object {
        val Empty = AgendaDayItems(emptyList(), emptyList())
    }
}

private data class MutableAgendaDayItems(
    val allDayItems: MutableList<TimelineItem> = mutableListOf(),
    val timedItems: MutableList<TimelineItem> = mutableListOf()
)

private fun List<TimelineItem>.groupedByAgendaDate(): Map<LocalDate, AgendaDayItems> {
    val buckets = mutableMapOf<LocalDate, MutableAgendaDayItems>()
    forEach { item ->
        val date = item.date ?: return@forEach
        val dayItems = buckets.getOrPut(date) { MutableAgendaDayItems() }
        if (item.startTimeMinutes == null) {
            dayItems.allDayItems += item
        } else {
            dayItems.timedItems += item
        }
    }
    return buckets.mapValues { (_, dayItems) ->
        AgendaDayItems(
            allDayItems = dayItems.allDayItems.sortedBy { it.sortOrder },
            timedItems = dayItems.timedItems.sortedWith(AgendaTimedItemComparator)
        )
    }
}

private val AgendaTimedItemComparator: Comparator<TimelineItem> =
    compareBy<TimelineItem> { it.startTimeMinutes }.thenBy { it.sortOrder }

@Composable
private fun AgendaDayHeader(date: LocalDate, today: LocalDate) {
    val isToday = date == today
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isToday) "Today, ${date.agendaDateLabel()}" else date.agendaDateLabel(),
            modifier = if (isToday) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            } else {
                Modifier
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun AgendaEmptyDay() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AgendaAxisLabel(text = "", isHighlighted = false)
        Spacer(Modifier.width(14.dp))
        Text(
            text = "No items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AgendaAllDayRow(
    items: List<TimelineItem>,
    showBottomLine: Boolean,
    onItemClick: (TimelineItem) -> Unit,
    itemContent: @Composable (TimelineItem) -> Unit
) {
    AgendaAxisRow(
        label = "All Day",
        showTopLine = false,
        showBottomLine = showBottomLine,
        isHighlighted = false
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Box(Modifier.clickable { onItemClick(item) }) {
                    itemContent(item)
                }
            }
        }
    }
}

@Composable
private fun AgendaTimedRow(
    item: TimelineItem,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isHighlighted: Boolean,
    onItemClick: (TimelineItem) -> Unit,
    itemContent: @Composable (TimelineItem) -> Unit
) {
    AgendaAxisRow(
        label = item.startTimeMinutes?.toClockLabel() ?: "",
        showTopLine = showTopLine,
        showBottomLine = showBottomLine,
        isHighlighted = isHighlighted
    ) {
        Box(Modifier.clickable { onItemClick(item) }) {
            itemContent(item)
        }
    }
}

@Composable
private fun AgendaAxisRow(
    label: String,
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isHighlighted: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        AgendaAxisLabel(text = label, isHighlighted)
        AgendaAxisMarker(showTopLine = showTopLine, showBottomLine = showBottomLine, isHighlighted = isHighlighted)
        Box(
            Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AgendaAxisLabel(text: String, isHighlighted: Boolean) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(34.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            style = if (isHighlighted) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun AgendaAxisMarker(
    showTopLine: Boolean,
    showBottomLine: Boolean,
    isHighlighted: Boolean,
) {
    Box(
        modifier = Modifier
            .width(14.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (showTopLine) {
            Box(
                modifier = Modifier
                    .padding(top = 0.dp)
                    .width(1.dp)
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        )
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
        if (showBottomLine) {
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

private fun LocalDate.agendaDateLabel(): String =
    "${dayOfWeek.shortName()}, ${shortMonthName()} $day"

