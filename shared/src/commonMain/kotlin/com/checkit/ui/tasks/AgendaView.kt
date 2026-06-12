package com.checkit.ui.tasks

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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
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
import com.checkit.ui.today
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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
    val itemsByDate = remember(items) { items.groupBy { it.date } }
    val boundedDayCount = dayLimit?.coerceAtLeast(1)
    val initialIndex = if (boundedDayCount == null) TodayIndex else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(focusedDate, initialIndex) {
        state.scrollToItem(initialIndex)
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(
                count = boundedDayCount ?: AgendaDayCount,
                key = { index -> "agenda-day-${focusedDate.plus(index - initialIndex, DateTimeUnit.DAY)}" }
            ) { index ->
                val date = focusedDate.plus(index - initialIndex, DateTimeUnit.DAY)
                AgendaDaySection(
                    date = date,
                    today = today,
                    items = itemsByDate[date].orEmpty(),
                    showHeader = boundedDayCount != 1,
                    onItemClick = onItemClick,
                    itemContent = itemContent
                )
            }
        }

        if (boundedDayCount != 1) {
            FilledTonalButton(
                onClick = { scope.launch { state.animateScrollToItem(initialIndex) } },
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
    items: List<TimelineItem>,
    showHeader: Boolean,
    onItemClick: (TimelineItem) -> Unit,
    itemContent: @Composable (TimelineItem) -> Unit
) {
    val allDayItems = items
        .filter { it.startTimeMinutes == null }
        .sortedBy { it.sortOrder }
    val timedItems = items
        .filter { it.startTimeMinutes != null }
        .sortedWith(compareBy<TimelineItem> { it.startTimeMinutes }.thenBy { it.sortOrder })

    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val nowMinutes = now.hour * 60 + now.minute
    val nextTimedItemIndex = remember(date, today, timedItems, nowMinutes) {
        if (date == today) {
            timedItems.indexOfFirst { (it.startTimeMinutes ?: 0) > nowMinutes }
        } else {
            -1
        }
    }

    Column {
        if (showHeader) {
            AgendaDayHeader(date = date, today = today)
            Spacer(Modifier.height(8.dp))
        }
        if (allDayItems.isEmpty() && timedItems.isEmpty()) {
            AgendaEmptyDay()
        } else {
            if (allDayItems.isNotEmpty()) {
                AgendaAllDayRow(
                    items = allDayItems,
                    showBottomLine = timedItems.isNotEmpty(),
                    onItemClick = onItemClick,
                    itemContent = itemContent
                )
            }
            timedItems.forEachIndexed { index, item ->
                AgendaTimedRow(
                    item = item,
                    showTopLine = index > 0 || allDayItems.isNotEmpty(),
                    showBottomLine = index < timedItems.lastIndex,
                    isHighlighted = index == nextTimedItemIndex,
                    onItemClick = onItemClick,
                    itemContent = itemContent
                )
            }
        }
    }
}

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

private const val AgendaDayCount = 20_001
private const val TodayIndex = AgendaDayCount / 2
