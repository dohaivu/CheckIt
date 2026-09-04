package com.checkit.ui.reflect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.reflect_period_day
import checkit.shared.generated.resources.reflect_period_month
import checkit.shared.generated.resources.reflect_period_week
import checkit.shared.generated.resources.reflect_period_year
import checkit.shared.generated.resources.tab_reflect
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoal
import com.checkit.ui.components.ReportPeriod
import com.checkit.ui.components.ReportPeriodHeader
import com.checkit.ui.components.TinyTopAppBar
import com.checkit.ui.localizedCompactDateWithDayName
import com.checkit.ui.localizedMonthTitle
import com.checkit.ui.localizedShortMonthName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private val ReflectPeriods = listOf(
    ReportPeriod.Daily,
    ReportPeriod.Week,
    ReportPeriod.Month,
    ReportPeriod.Annual,
    ReportPeriod.Habit
)

@Composable
internal fun ReflectScreen(
    state: ReflectUiState,
    viewModel: ReflectViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TinyTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.tab_reflect),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 12.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportPeriodHeader(
                selectedPeriod = state.selectedPeriod,
                selectedDate = state.selectedDate,
                onPeriodSelected = viewModel::selectPeriod,
                onPreviousPeriod = viewModel::previousPeriod,
                onNextPeriod = viewModel::nextPeriod,
                onCurrentPeriod = viewModel::resetToCurrentPeriod,
                onZoomOutTo = viewModel::zoomOutTo,
                periods = ReflectPeriods
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                when (state.selectedPeriod) {
                    ReportPeriod.Habit -> {
                        val checkins = state.habitCheckins
                        if (checkins.isEmpty()) {
                            EmptyHabitsCard()
                        } else {
                            HabitHeatmapSection(checkins = checkins, monthCount = 2)
                        }
                    }

                    ReportPeriod.Daily,
                    ReportPeriod.Week,
                    ReportPeriod.Month,
                    ReportPeriod.Annual -> {
                        val digest = state.digestReport

                        // 1. Goal & Reflection Section
                        GoalReflectionCard(
                            state = state,
                            onOpenEditor = viewModel::openEditor
                        )

                        // 2. Activity & Tag Hours Section
                        ActivityAndTagsSection(
                            totalMinutes = digest.totalMinutes,
                            activityItems = digest.activityItems,
                            topTags = digest.topTags,
                            selectedDate = state.selectedDate,
                            selectedPeriod = state.selectedPeriod,
                            onZoomInTo = viewModel::zoomInTo
                        )

                        // 3. Past Days Chronicle Section
                        PastDaysChronicleSection(
                            items = state.chronicleItems,
                            selectedPeriod = state.selectedPeriod,
                            onItemClick = {
                                coroutineScope.launch {
                                    viewModel.goToGoal(it)
                                    delay(100.milliseconds)
                                    scrollState.animateScrollTo(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun Period.reviewIcon(): ImageVector = when (this) {
    Period.Day -> Icons.Default.Star
    Period.Week -> Icons.Default.DateRange
    Period.Month -> Icons.Default.CalendarMonth
    Period.Year -> Icons.Default.EmojiEvents
    else -> Icons.Default.DateRange
}

@Composable
internal fun PeriodGoal.rangeLabel(): String = when (period) {
    Period.Day -> startDate.localizedCompactDateWithDayName()
    Period.Week -> "${startDate.localizedShortMonthName()} ${startDate.day}"
    Period.Month -> startDate.localizedMonthTitle()
    Period.Year -> startDate.year.toString()
    else -> startDate.toString()
}

@Composable
internal fun Period.label(): String = when (this) {
    Period.Day -> stringResource(Res.string.reflect_period_day)
    Period.Week -> stringResource(Res.string.reflect_period_week)
    Period.Month -> stringResource(Res.string.reflect_period_month)
    Period.Year -> stringResource(Res.string.reflect_period_year)
    else -> name
}
