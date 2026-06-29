package com.checkit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.checkit.ui.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

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

    companion object {
        val Presets = listOf(Q1, Q2, Q3, Q4, H1, H2, Year)

        fun fromRange(start: LocalDate?, end: LocalDate?): Period {
            if (start == null || end == null) return Custom
            return Presets.firstOrNull { 
                val range = it.getRange(start)
                range.first == start && range.second == end
            } ?: Custom
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Timeframe")
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
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun TimeframePill(
    startDate: LocalDate?,
    endDate: LocalDate?,
    modifier: Modifier = Modifier
) {
    if (startDate == null && endDate == null) return

    val period = remember(startDate, endDate) { Period.fromRange(startDate, endDate) }
    val label = if (period is Period.Custom) {
        val startStr = startDate?.let { "${it.month.name.take(3)} ${it.day}" } ?: "..."
        val endStr = endDate?.let { "${it.month.name.take(3)} ${it.day}" } ?: "..."
        "$startStr - $endStr"
    } else {
        period.label
    }

    DetailChip(
        icon = Icons.Default.DateRange,
        label = label,
        modifier = modifier
    )
}
