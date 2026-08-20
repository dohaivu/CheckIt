package com.checkit.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.checkit.domain.FocusPeriod
import com.checkit.domain.Period
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
    focus: FocusPeriod?,
    onFocusSelected: (FocusPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCurrentPeriod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        FocusPeriodSwitcher(
            selectedPeriod = focus?.period,
            onPeriodSelected = { period ->
                val anchor = if (focus?.contains(today()) == true) today() else focus?.anchorDate ?: today()
                onFocusSelected(FocusPeriod(period, anchor))
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
    selectedPeriod: Period?,
    onPeriodSelected: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Period.entries.forEach { period ->
                val selected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            }
                        )
                        .clickable { onPeriodSelected(period) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.shortLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusPeriodNavHeader(
    focus: FocusPeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrentPeriod: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = stringResource(Res.string.plan_previous_period),
                modifier = Modifier.size(20.dp)
            )
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
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = focus.title(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(Res.string.plan_next_period),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FocusPeriodBreadcrumbRow(
    focus: FocusPeriod,
    onZoomOutTo: (FocusPeriod) -> Unit
) {
    val crumbs = buildList {
        Period.entries.forEach { period ->
            if (period.ordinal <= focus.period.ordinal) {
                val crumbFocus = FocusPeriod(period, focus.anchorDate)
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
    val period: Period,
    val focus: FocusPeriod,
    val label: String
)

@Composable
private fun Period.shortLabel(): String = when (this) {
    Period.Year -> stringResource(Res.string.plan_period_year)
    Period.Quarter -> stringResource(Res.string.plan_period_quarter)
    Period.Month -> stringResource(Res.string.plan_period_month)
    Period.Week -> stringResource(Res.string.plan_period_week)
    Period.Day -> stringResource(Res.string.plan_period_day)
}

@Composable
internal fun FocusPeriod.title(): String = when (period) {
    Period.Year -> "${start.year}"
    Period.Quarter -> {
        val quarter = ((start.month.number - 1) / 3) + 1
        "Q$quarter ${start.year}"
    }
    Period.Month -> start.localizedMonthTitle()
    Period.Week -> weekRangeTitle(start, endInclusive)
    Period.Day -> start.localizedCompactDateWithDayName()
}

/** Compact per-level label for the breadcrumb; drops year/month info already shown by ancestor crumbs. */
@Composable
internal fun FocusPeriod.crumbLabel(): String = when (period) {
    Period.Year -> "${start.year}"
    Period.Quarter -> {
        val quarter = ((start.month.number - 1) / 3) + 1
        "Q$quarter"
    }
    Period.Month -> start.month.localizedName()
    Period.Week -> {
        val week = start.toLocalIsoWeekDate().isoWeekNumber
        "W$week ${start.day} - ${endInclusive.day}"
    }
    Period.Day -> "${start.day} ${start.dayOfWeek.localizedShortName()}"
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
