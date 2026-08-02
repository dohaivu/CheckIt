package com.checkit.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val HeatmapWeekCount = 12
private val HeatmapCellSize = 11.dp
private val HeatmapCellSpacing = 3.dp

@Composable
internal fun HabitHeatmapSection(
    checkins: List<HabitCheckin>,
    modifier: Modifier = Modifier
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
                        text = "Your consistency over the last $HeatmapWeekCount weeks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            checkins.forEachIndexed { index, checkin ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
                HabitHeatmapCard(checkin = checkin)
            }
        }
    }
}

@Composable
private fun HabitHeatmapCard(
    checkin: HabitCheckin,
    modifier: Modifier = Modifier
) {
    val today = today()
    val columns = remember(checkin.doneDates, today) { buildHeatmapColumns(today) }
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
            horizontalArrangement = Arrangement.spacedBy(HeatmapCellSpacing)
        ) {
            columns.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(HeatmapCellSpacing)) {
                    week.forEach { date ->
                        HeatmapCell(
                            date = date,
                            doneDates = checkin.doneDates,
                            today = today
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
    doneDates: Set<LocalDate>,
    today: LocalDate
) {
    val color = when {
        date == null -> Color.Transparent
        date in doneDates -> HabitHeatmapDone
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val isToday = date == today
    Box(
        modifier = Modifier
            .size(HeatmapCellSize)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .then(
                if (isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                } else {
                    Modifier
                }
            )
    )
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

internal fun buildHeatmapColumns(
    today: LocalDate,
    weekCount: Int = HeatmapWeekCount
): List<List<LocalDate?>> {
    val monday = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
    val firstWeek = monday.minus((weekCount - 1) * 7, DateTimeUnit.DAY)
    return (0 until weekCount).map { col ->
        (0 until 7).map { row ->
            val date = firstWeek.plus(col * 7 + row, DateTimeUnit.DAY)
            if (date <= today) date else null
        }
    }
}

private val HabitHeatmapDone = Color(0xFF2EC995)
private val HabitHeatmapStreak = Color(0xFF0E9F73)
