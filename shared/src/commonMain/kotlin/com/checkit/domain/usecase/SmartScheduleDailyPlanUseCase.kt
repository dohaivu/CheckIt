package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DefaultTaskDurationMinutes
import com.checkit.domain.MinimumPlanDurationMinutes
import com.checkit.domain.MyDayMinutesPerDay
import com.checkit.domain.hasEndTime
import com.checkit.domain.nextAvailableTimeRange
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

const val SmartScheduleHistoryDays = 3

data class SmartScheduleResult(
    val scheduledCount: Int,
    val candidateCount: Int
)

/** A past completed daily plan item that carried a start time. */
data class SmartTimeSample(
    val startMinutes: Int,
    val endMinutes: Int?
)

/**
 * Schedules every non-completed, tagged My Day item by repeating the most
 * frequent (start, end) times recorded for each item's first tag across the
 * most recent days (up to [SmartScheduleHistoryDays]) on which that tag was
 * completed with a time. Each resolved range is fed through
 * [nextAvailableTimeRange] so real slots never overlap.
 */
class SmartScheduleDailyPlanUseCase(
    private val repository: CheckItRepository,
    private val todayDate: () -> LocalDate = { today() },
    private val nowMinutes: () -> Int = { currentMyDayTimeMinutes() }
) {
    suspend operator fun invoke(): Result<SmartScheduleResult> = runCatching {
        val plans = repository.observeDailyPlans().first()
        val date = todayDate()
        val todayItems = plans.firstOrNull { it.date == date }?.items.orEmpty()

        val candidates = todayItems.filter {
            it.status != DailyPlanItemStatus.Done && it.tags.isNotEmpty()
        }
        if (candidates.isEmpty()) {
            return@runCatching SmartScheduleResult(scheduledCount = 0, candidateCount = 0)
        }

        val history = buildTagHistory(plans, date)
        val now = nowMinutes()
        val targetIds = candidates.mapTo(HashSet()) { it.id }
        val occupied = todayItems.filterNot { it.id in targetIds }.toMutableList()

        var scheduledCount = 0
        for (item in candidates) {
            val samples = history[item.tags.first().id].orEmpty()
            val (preferredStart, durationMinutes) = if (samples.isEmpty()) {
                now to DefaultTaskDurationMinutes
            } else {
                bestSmartScheduleRange(samples, now) ?: (now to DefaultTaskDurationMinutes)
            }
            val (start, end) = nextAvailableTimeRange(preferredStart, durationMinutes, occupied)
            if (start == null || end == null) continue
            repository.updateDailyPlanItemTime(
                itemId = item.id,
                startTimeMinutes = start,
                endTimeMinutes = if (item.source.hasEndTime()) end else null
            )
            occupied += item.copy(startTimeMinutes = start, endTimeMinutes = end)
            scheduledCount += 1
        }
        SmartScheduleResult(scheduledCount = scheduledCount, candidateCount = candidates.size)
    }

    /**
     * Single pass over the newest-first [plans] building, per tag, the done
     * time samples from at most [SmartScheduleHistoryDays] distinct past days.
     */
    private fun buildTagHistory(
        plans: List<DailyPlan>,
        today: LocalDate
    ): Map<Long, List<SmartTimeSample>> {
        val history = HashMap<Long, MutableList<SmartTimeSample>>()
        val daysSeen = HashMap<Long, Int>()
        for (plan in plans) {
            if (plan.date >= today) continue
            val doneWithTime = plan.items.filter {
                it.status == DailyPlanItemStatus.Done && it.startTimeMinutes != null
            }
            if (doneWithTime.isEmpty()) continue
            val tagsOnDay = HashSet<Long>()
            doneWithTime.forEach { item -> item.tags.forEach { tagsOnDay.add(it.id) } }
            for (tagId in tagsOnDay) {
                val seen = daysSeen[tagId] ?: 0
                if (seen >= SmartScheduleHistoryDays) continue
                val samples = history.getOrPut(tagId) { ArrayList() }
                doneWithTime.forEach { item ->
                    if (item.tags.any { it.id == tagId }) {
                        val start = item.startTimeMinutes ?: return@forEach
                        samples.add(SmartTimeSample(start, item.endTimeMinutes))
                    }
                }
                daysSeen[tagId] = seen + 1
            }
        }
        return history
    }
}

/**
 * Most frequent start time and most frequent end time across [samples], as a
 * (preferred start, duration) pair. The start is clamped to never be before
 * [nowMinutes]. Returns null when there are no samples.
 */
fun bestSmartScheduleRange(
    samples: List<SmartTimeSample>,
    nowMinutes: Int
): Pair<Int, Int>? {
    if (samples.isEmpty()) return null
    val bestStart = modeOf(samples.map { it.startMinutes }) ?: return null
    val bestEnd = modeOf(samples.map { it.endMinutes ?: (it.startMinutes + DefaultTaskDurationMinutes) })
        ?: (bestStart + DefaultTaskDurationMinutes)
    val duration = (bestEnd - bestStart).coerceIn(MinimumPlanDurationMinutes, MyDayMinutesPerDay)
    return maxOf(bestStart, nowMinutes) to duration
}

private fun modeOf(values: List<Int>): Int? {
    if (values.isEmpty()) return null
    val counts = HashMap<Int, Int>()
    values.forEach { counts[it] = (counts[it] ?: 0) + 1 }
    var best = values.first()
    var bestCount = counts.getValue(best)
    for ((value, count) in counts) {
        if (count > bestCount || (count == bestCount && value < best)) {
            best = value
            bestCount = count
        }
    }
    return best
}
