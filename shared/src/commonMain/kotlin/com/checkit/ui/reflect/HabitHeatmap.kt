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
import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.routineIntensityByDate
import com.checkit.domain.routineStreakDates
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

/** Generic heatmap row: intensity 0..1 per date, shared by habits and routines. */
data class HeatmapSeries(
    val key: String,
    val title: String,
    val intensityByDate: Map<LocalDate, Float>,
    val streak: Int,
    val totalDone: Int
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
    val series = remember(checkins) {
        checkins.map { checkin ->
            HeatmapSeries(
                key = checkin.habitKey,
                title = checkin.title,
                intensityByDate = checkin.doneMinutesByDate.mapValues { (_, minutes) ->
                    (minutes.toFloat() / HeatmapMaxMinutes).coerceIn(0f, 1f)
                },
                streak = checkin.streak,
                totalDone = checkin.totalDone
            )
        }
    }
    HeatmapSeriesSection(
        headerTitle = "Habits",
        subtitle = consistencySubtitle(today(), monthCount),
        series = series,
        baseColor = HabitHeatmapDone,
        modifier = modifier,
        monthCount = monthCount
    )
}

@Composable
internal fun RoutineHeatmapSection(
    series: List<HeatmapSeries>,
    modifier: Modifier = Modifier,
    monthCount: Int = DefaultHeatmapMonthCount
) {
    if (series.isEmpty()) return
    HeatmapSeriesSection(
        headerTitle = "Routines",
        subtitle = "Days with \u226580% count toward streaks.",
        series = series,
        baseColor = RoutineHeatmapDone,
        modifier = modifier,
        monthCount = monthCount
    )
}

@Composable
private fun HeatmapSeriesSection(
    headerTitle: String,
    subtitle: String,
    series: List<HeatmapSeries>,
    baseColor: Color,
    modifier: Modifier = Modifier,
    monthCount: Int = DefaultHeatmapMonthCount
) {
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
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            series.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
                HeatmapSeriesCard(series = row, baseColor = baseColor, monthCount = monthCount)
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
    val intensityByDate = remember(checkin.doneMinutesByDate) {
        checkin.doneMinutesByDate.mapValues { (_, minutes) ->
            (minutes.toFloat() / HeatmapMaxMinutes).coerceIn(0f, 1f)
        }
    }
    HeatmapSeriesCard(
        series = HeatmapSeries(
            key = checkin.habitKey,
            title = checkin.title,
            intensityByDate = intensityByDate,
            streak = checkin.streak,
            totalDone = checkin.totalDone
        ),
        baseColor = HabitHeatmapDone,
        months = months,
        modifier = modifier
    )
}

@Composable
private fun HeatmapSeriesCard(
    series: HeatmapSeries,
    baseColor: Color,
    modifier: Modifier = Modifier,
    monthCount: Int = DefaultHeatmapMonthCount
) {
    val today = today()
    val months = remember(series.intensityByDate, today, monthCount) { buildHeatmapMonths(today, monthCount) }
    HeatmapSeriesCard(series = series, baseColor = baseColor, months = months, modifier = modifier)
}

@Composable
private fun HeatmapSeriesCard(
    series: HeatmapSeries,
    baseColor: Color,
    months: List<HeatmapMonth>,
    modifier: Modifier = Modifier
) {
    val today = today()
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
                text = series.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            HabitStreakBadge(streak = series.streak)
            Text(
                text = "${series.totalDone} days",
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
                    intensityByDate = series.intensityByDate,
                    baseColor = baseColor,
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
    intensityByDate: Map<LocalDate, Float>,
    baseColor: Color,
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
                            intensityByDate = intensityByDate,
                            baseColor = baseColor,
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
    intensityByDate: Map<LocalDate, Float>,
    baseColor: Color,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val color = when {
        date == null -> Color.Transparent
        date in intensityByDate -> fractionIntensityColor(intensityByDate.getValue(date), baseColor)
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

private fun fractionIntensityColor(fraction: Float, baseColor: Color): Color {
    val clamped = fraction.coerceIn(0f, 1f)
    val alpha = HeatmapMinAlpha + (HeatmapMaxAlpha - HeatmapMinAlpha) * clamped
    return baseColor.copy(alpha = alpha)
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
private val RoutineHeatmapDone = Color(0xFF8B5CF6)

/** Builds one heatmap row per routine from percent-only logs. */
internal fun buildRoutineSeries(
    routines: List<Routine>,
    logs: List<RoutineLog>,
    today: LocalDate
): List<HeatmapSeries> {
    val logsByRoutine = logs.groupBy { it.routineId }
    return routines.map { routine ->
        val rows = logsByRoutine[routine.id].orEmpty()
        val doneDates = routineStreakDates(rows)
        HeatmapSeries(
            key = routine.id,
            title = routine.title.ifBlank { "Routine" },
            intensityByDate = routineIntensityByDate(rows),
            streak = calculateStreak(doneDates, today),
            totalDone = doneDates.size
        )
    }.sortedWith(compareByDescending<HeatmapSeries> { it.streak }.thenBy { it.title.lowercase() })
}
