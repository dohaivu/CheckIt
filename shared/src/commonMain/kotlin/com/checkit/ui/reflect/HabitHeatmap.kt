package com.checkit.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val HeatmapCellSpacing = 6.dp
private val HeatmapCellCornerRadius = 6.dp

private const val DefaultHeatmapMonthCount = 1
private const val HeatmapMaxMinutes = 8 * 60
private const val HeatmapMinAlpha = 0.22f
private const val HeatmapMaxAlpha = 1f

data class HeatmapMonth(
    val monthStart: LocalDate,
    val weeks: List<List<LocalDate?>>
)

@Composable
internal fun EmptyHabitsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = "No habit check-ins yet. Complete a habit on My Day to start your heatmap.",
            modifier = Modifier.padding(22.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun HabitHeatmapSection(
    checkins: List<HabitCheckin>,
    modifier: Modifier = Modifier,
    monthCount: Int = DefaultHeatmapMonthCount
) {
    if (checkins.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Habits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = consistencySubtitle(today(), monthCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            checkins.forEachIndexed { index, checkin ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
                HabitHeatmapCard(checkin = checkin, monthCount = monthCount)
            }
        }
    }
}

@Composable
private fun consistencySubtitle(today: LocalDate, monthCount: Int): String =
    if (monthCount <= 1) {
        "Your consistency in ${today.localizedMonthTitle()}."
    } else {
        "Your consistency over the last $monthCount months."
    }

@Composable
private fun HabitHeatmapCard(
    checkin: HabitCheckin,
    modifier: Modifier = Modifier,
    monthCount: Int = DefaultHeatmapMonthCount
) {
    val today = today()
    val months = remember(checkin.doneMinutesByDate, today, monthCount) { buildHeatmapMonths(today, monthCount) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = checkin.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            HabitStreakBadge(streak = checkin.streak)
            Text(
                text = "${checkin.totalDone} days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            months.forEach { month ->
                HeatmapMonthColumn(
                    month = month,
                    minutesByDate = checkin.doneMinutesByDate,
                    today = today,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeatmapMonthColumn(
    month: HeatmapMonth,
    minutesByDate: Map<LocalDate, Int>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = month.monthStart.localizedMonthTitle(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Column(verticalArrangement = Arrangement.spacedBy(HeatmapCellSpacing)) {
            month.weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(HeatmapCellSpacing)
                ) {
                    week.forEach { date ->
                        HeatmapCell(
                            date = date,
                            minutesByDate = minutesByDate,
                            today = today,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    date: LocalDate?,
    minutesByDate: Map<LocalDate, Int>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val color = when {
        date == null -> Color.Transparent
        date in minutesByDate -> minutesIntensityColor(minutesByDate.getValue(date))
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val isToday = date == today
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(HeatmapCellCornerRadius))
            .background(color)
            .then(
                if (isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(HeatmapCellCornerRadius))
                } else {
                    Modifier
                }
            )
    )
}

private fun minutesIntensityColor(minutes: Int): Color {
    val fraction = (minutes.toFloat() / HeatmapMaxMinutes).coerceIn(0f, 1f)
    val alpha = HeatmapMinAlpha + (HeatmapMaxAlpha - HeatmapMinAlpha) * fraction
    return HabitHeatmapDone.copy(alpha = alpha)
}

@Composable
private fun HabitStreakBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val color = if (streak > 0) HabitHeatmapStreak else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Text(
        text = "$streak day streak",
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HabitHeatmapStreak.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

internal fun buildHeatmapMonths(
    today: LocalDate,
    monthCount: Int = DefaultHeatmapMonthCount
): List<HeatmapMonth> {
    val currentMonthStart = LocalDate(today.year, today.month, 1)
    val firstMonthStart = currentMonthStart.minus(monthCount.coerceAtLeast(1) - 1, DateTimeUnit.MONTH)
    return (0 until monthCount.coerceAtLeast(1)).map { offset ->
        val monthStart = firstMonthStart.plus(offset, DateTimeUnit.MONTH)
        val monthEnd = monthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        val firstWeekStart = monthStart.minus(monthStart.dayOfWeek.ordinal, DateTimeUnit.DAY)
        val weeks = buildList {
            var weekStart = firstWeekStart
            while (weekStart <= monthEnd) {
                add((0 until 7).map { row ->
                    val date = weekStart.plus(row, DateTimeUnit.DAY)
                    if (date >= monthStart && date <= monthEnd) date else null
                })
                weekStart = weekStart.plus(7, DateTimeUnit.DAY)
            }
        }
        HeatmapMonth(monthStart = monthStart, weeks = weeks)
    }
}

private val HabitHeatmapDone = Color(0xFF2EC995)
private val HabitHeatmapStreak = Color(0xFF0E9F73)
