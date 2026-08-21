package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.checkit.ui.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalIsoWeekDate

sealed class Period(val label: String) {
    abstract fun getRange(reference: LocalDate = today()): Pair<LocalDate, LocalDate>

    data object Q1 : Period("Q1") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.JANUARY, 1) to LocalDate(reference.year, Month.MARCH, 31)
    }
    data object Q2 : Period("Q2") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.APRIL, 1) to LocalDate(reference.year, Month.JUNE, 30)
    }
    data object Q3 : Period("Q3") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.JULY, 1) to LocalDate(reference.year, Month.SEPTEMBER, 30)
    }
    data object Q4 : Period("Q4") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.OCTOBER, 1) to LocalDate(reference.year, Month.DECEMBER, 31)
    }
    data object H1 : Period("H1") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.JANUARY, 1) to LocalDate(reference.year, Month.JUNE, 30)
    }
    data object H2 : Period("H2") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.JULY, 1) to LocalDate(reference.year, Month.DECEMBER, 31)
    }
    data object Year : Period("Year") {
        override fun getRange(reference: LocalDate) = 
            LocalDate(reference.year, Month.JANUARY, 1) to LocalDate(reference.year, Month.DECEMBER, 31)
    }
    data object Custom : Period("Custom") {
        override fun getRange(reference: LocalDate) = reference to reference
    }

    data class MonthPeriod(val month: Month) : Period(
        when (month) {
            Month.JANUARY -> "Jan"
            Month.FEBRUARY -> "Feb"
            Month.MARCH -> "Mar"
            Month.APRIL -> "Apr"
            Month.MAY -> "May"
            Month.JUNE -> "Jun"
            Month.JULY -> "Jul"
            Month.AUGUST -> "Aug"
            Month.SEPTEMBER -> "Sep"
            Month.OCTOBER -> "Oct"
            Month.NOVEMBER -> "Nov"
            Month.DECEMBER -> "Dec"
        }
    ) {
        override fun getRange(reference: LocalDate): Pair<LocalDate, LocalDate> {
            val start = LocalDate(reference.year, month, 1)
            val lastDay = when (month) {
                Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY, Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
                Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
                Month.FEBRUARY -> if (isLeapYear(reference.year)) 29 else 28
            }
            return start to LocalDate(reference.year, month, lastDay)
        }
    }

    data class WeekPeriod(val start: LocalDate, val end: LocalDate, val weekNumber: Int) : Period("W$weekNumber") {
        override fun getRange(reference: LocalDate) = start to end
    }

    companion object {
        val Months = Month.entries.map { MonthPeriod(it) }
        val Presets = listOf(Q1, Q2, Q3, Q4, H1, H2, Year)

        fun getWeeksForMonth(month: Month, year: Int): List<WeekPeriod> {
            val firstDay = LocalDate(year, month, 1)
            val nextMonth = if (month == Month.DECEMBER) LocalDate(year + 1, Month.JANUARY, 1) 
                           else LocalDate(year, Month.entries[month.ordinal + 1], 1)
            val lastDay = nextMonth.minus(1, DateTimeUnit.DAY)
            
            var current = firstDay
            while (current.dayOfWeek != DayOfWeek.MONDAY) {
                current = current.minus(1, DateTimeUnit.DAY)
            }
            
            val weeks = mutableListOf<WeekPeriod>()
            while (current <= lastDay) {
                val weekStart = current
                val weekEnd = current.plus(6, DateTimeUnit.DAY)
                weeks.add(WeekPeriod(weekStart, weekEnd, weekStart.toLocalIsoWeekDate().isoWeekNumber))
                current = current.plus(1, DateTimeUnit.WEEK)
            }
            return weeks
        }

        fun fromRange(start: LocalDate?, end: LocalDate?): Period {
            if (start == null || end == null) return Custom
            val found = (Presets + Months).firstOrNull { 
                val range = it.getRange(start)
                range.first == start && range.second == end
            }
            if (found != null) return found

            if (start.dayOfWeek == DayOfWeek.MONDAY && start.plus(6, DateTimeUnit.DAY) == end) {
                return WeekPeriod(start, end, start.toLocalIsoWeekDate().isoWeekNumber)
            }

            return Custom
        }
    }
}

@Composable
internal fun PeriodPicker(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPeriod = remember(startDate, endDate) { Period.fromRange(startDate, endDate) }

    val selectedMonthForWeeks = remember(currentPeriod) {
        when (currentPeriod) {
            is Period.MonthPeriod -> currentPeriod
            is Period.WeekPeriod -> Period.Months.firstOrNull { it.month == currentPeriod.start.month }
            else -> null
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(Period.Presets) { period ->
                PeriodChip(
                    label = period.label,
                    isSelected = currentPeriod == period,
                    onClick = {
                        val range = period.getRange()
                        onRangeChange(range.first, range.second)
                    }
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(Period.Months) { period ->
                PeriodChip(
                    label = period.label,
                    isSelected = currentPeriod == period || selectedMonthForWeeks == period,
                    onClick = {
                        val range = period.getRange()
                        onRangeChange(range.first, range.second)
                    }
                )
            }
        }

        if (selectedMonthForWeeks != null) {
            val weeks = remember(selectedMonthForWeeks) {
                Period.getWeeksForMonth(selectedMonthForWeeks.month, (startDate ?: today()).year)
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(weeks) { week ->
                    PeriodChip(
                        label = week.label,
                        isSelected = currentPeriod == week,
                        onClick = {
                            val range = week.getRange()
                            onRangeChange(range.first, range.second)
                        }
                    )
                }
            }
        }
        
        DateRangePicker(
            startDate = startDate,
            endDate = endDate,
            onRangeChange = onRangeChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) colorScheme.primaryContainer else colorScheme.surface.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun DateRangePill(
    startDate: LocalDate?,
    endDate: LocalDate?,
    modifier: Modifier = Modifier
) {
    if (startDate == null && endDate == null) return

    val period = remember(startDate, endDate) { Period.fromRange(startDate, endDate) }
    val label = if (period is Period.Custom) {
        when {
            startDate != null && endDate != null && startDate == endDate -> "${startDate.month.name.take(3)} ${startDate.day}"
            startDate != null && endDate != null -> "${startDate.month.name.take(3)} ${startDate.day} - ${endDate.month.name.take(3)} ${endDate.day}"
            startDate != null -> "${startDate.month.name.take(3)} ${startDate.day}"
            else -> ""
        }
    } else {
        period.label.uppercase()
    }

    CompactDetailChip(
        icon = Icons.Default.DateRange,
        label = label,
        modifier = modifier
    )
}

private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
