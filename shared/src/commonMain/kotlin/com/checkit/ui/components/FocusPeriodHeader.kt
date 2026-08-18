package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.plan_next_period
import checkit.shared.generated.resources.plan_period_day
import checkit.shared.generated.resources.plan_period_month
import checkit.shared.generated.resources.plan_period_quarter
import checkit.shared.generated.resources.plan_period_week
import checkit.shared.generated.resources.plan_period_year
import checkit.shared.generated.resources.plan_previous_period
import com.checkit.domain.PlanFocus
import com.checkit.domain.PlanPeriod
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedShortMonthName
import com.checkit.ui.localizedShortName
import com.checkit.ui.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.toLocalIsoWeekDate
import org.jetbrains.compose.resources.stringResource

@Composable
fun FocusPeriodHeader(
    focus: PlanFocus?,
    onFocusSelected: (PlanFocus) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FocusPeriodSwitcher(
            selectedPeriod = focus?.period,
            onPeriodSelected = { period ->
                val anchor = if (focus?.contains(today()) == true) today() else focus?.anchorDate ?: today()
                onFocusSelected(PlanFocus(period, anchor))
            }
        )
        if (focus != null) {
            FocusPeriodNavHeader(
                focus = focus,
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod,
                onCurrentPeriod = onCurrentPeriod
            )
            FocusPeriodBreadcrumbRow(focus = focus, onZoomOutTo = onFocusSelected)
        }
    }
}

@Composable
internal fun FocusPeriodSwitcher(
    selectedPeriod: PlanPeriod?,
    onPeriodSelected: (PlanPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlanPeriod.entries.forEach { period ->
            val selected = selectedPeriod == period
            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .border(1.dp, tint, CircleShape)
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.shortLabel(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FocusPeriodNavHeader(
    focus: PlanFocus,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrentPeriod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(Res.string.plan_previous_period))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onCurrentPeriod),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = focus.title(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(Res.string.plan_next_period))
            }
        }
    }
}

@Composable
private fun FocusPeriodBreadcrumbRow(
    focus: PlanFocus,
    onZoomOutTo: (PlanFocus) -> Unit
) {
    val crumbs = buildList {
        PlanPeriod.entries.forEach { period ->
            if (period.ordinal <= focus.period.ordinal) {
                val crumbFocus = PlanFocus(period, focus.anchorDate)
                add(FocusPeriodCrumb(period, crumbFocus, crumbFocus.crumbLabel()))
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        crumbs.forEachIndexed { index, crumb ->
            if (index > 0) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (crumb.period == focus.period) {
                Text(
                    text = crumb.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = crumb.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onZoomOutTo(crumb.focus) }
                )
            }
        }
    }
}

private data class FocusPeriodCrumb(
    val period: PlanPeriod,
    val focus: PlanFocus,
    val label: String
)

@Composable
private fun PlanPeriod.shortLabel(): String = when (this) {
    PlanPeriod.Year -> stringResource(Res.string.plan_period_year)
    PlanPeriod.Quarter -> stringResource(Res.string.plan_period_quarter)
    PlanPeriod.Month -> stringResource(Res.string.plan_period_month)
    PlanPeriod.Week -> stringResource(Res.string.plan_period_week)
    PlanPeriod.Day -> stringResource(Res.string.plan_period_day)
}

@Composable
internal fun PlanFocus.title(): String = when (period) {
    PlanPeriod.Year -> "${start.year}"
    PlanPeriod.Quarter -> {
        val quarter = ((start.monthNumber - 1) / 3) + 1
        "Q$quarter ${start.year}"
    }
    PlanPeriod.Month -> start.localizedMonthTitle()
    PlanPeriod.Week -> weekRangeTitle(start, endInclusive)
    PlanPeriod.Day -> start.localizedCompactDateWithDayName()
}

/** Compact per-level label for the breadcrumb; drops year/month info already shown by ancestor crumbs. */
@Composable
internal fun PlanFocus.crumbLabel(): String = when (period) {
    PlanPeriod.Year -> "${start.year}"
    PlanPeriod.Quarter -> {
        val quarter = ((start.month.number - 1) / 3) + 1
        "Q$quarter"
    }
    PlanPeriod.Month -> start.month.localizedName()
    PlanPeriod.Week -> {
        val week = start.toLocalIsoWeekDate().isoWeekNumber
        "W$week ${start.day} - ${endInclusive.day}"
    }
    PlanPeriod.Day -> "${start.day} ${start.dayOfWeek.localizedShortName()}"
}

@Composable
private fun weekRangeTitle(start: LocalDate, end: LocalDate): String {
    val startLabel = "${start.localizedShortMonthName()} ${start.day}"
    val endLabel = "${end.localizedShortMonthName()} ${end.day}"
    return if (start.year == end.year) {
        "$startLabel - $endLabel, ${end.year}"
    } else {
        "$startLabel, ${start.year} - $endLabel, ${end.year}"
    }
}
