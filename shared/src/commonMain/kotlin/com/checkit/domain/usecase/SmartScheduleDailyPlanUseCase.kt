package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.DailyPlanItemTimeUpdate
import com.checkit.domain.DailyPlan
import com.checkit.domain.DailyPlanItem
import com.checkit.domain.DailyPlanItemStatus
import com.checkit.domain.DefaultTaskDurationMinutes
import com.checkit.domain.MinimumPlanDurationMinutes
import com.checkit.domain.MyDayMinutesPerDay
import com.checkit.domain.hasEndTime
import com.checkit.domain.occupiedTimeRange
import com.checkit.ui.currentMyDayTimeMinutes
import com.checkit.ui.today
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.math.abs

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
 * a conflict-aware planned set of slots so real slots never overlap.
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
        val fixedItems = todayItems.filterNot { it.id in targetIds }
        val requests = candidates.map { item ->
            val samples = history[item.tags.first().id].orEmpty()
            val (preferredStart, durationMinutes) = if (samples.isEmpty()) {
                now to DefaultTaskDurationMinutes
            } else {
                bestSmartScheduleRange(samples, now) ?: (now to DefaultTaskDurationMinutes)
            }
            SmartScheduleRequest(item, preferredStart, durationMinutes)
        }
        val assignments = planRequests(requests, fixedItems, now)

        // Plan everything before persisting anything. This prevents a failed write from
        // changing the slots used by the rest of the planning pass.
        val updates = assignments.values
            .sortedBy { it.item.sortOrder }
            .map { assignment ->
                DailyPlanItemTimeUpdate(
                    itemId = assignment.item.id,
                    startTimeMinutes = assignment.start,
                    endTimeMinutes = if (assignment.item.source.hasEndTime()) assignment.end else null
                )
            }
        repository.updateDailyPlanItemTimes(updates)
        SmartScheduleResult(scheduledCount = assignments.size, candidateCount = candidates.size)
    }

    /**
     * Assigns the most constrained request first. A request's regret is the cost difference
     * between its best and second-best currently available slots. This protects requests that
     * have a strong or narrow preference from being displaced by a more flexible request.
     */
    private fun planRequests(
        requests: List<SmartScheduleRequest>,
        fixedItems: List<DailyPlanItem>,
        earliestStart: Int
    ): Map<Long, SmartScheduleAssignment> {
        val occupied = fixedItems.toMutableList()
        val remaining = requests.toMutableList()
        val assignments = LinkedHashMap<Long, SmartScheduleAssignment>()

        while (remaining.isNotEmpty()) {
            val optionsByRequest = remaining.associateWith { request ->
                availableOptions(request, occupied, earliestStart)
            }
            val request = remaining.maxWithOrNull(
                compareBy<SmartScheduleRequest> {
                    val options = optionsByRequest.getValue(it)
                    if (options.size >= 2) options[1].score - options[0].score else Int.MAX_VALUE
                }.thenBy { it.durationMinutes }
                    .thenBy { -it.item.sortOrder }
            ) ?: break
            val best = optionsByRequest.getValue(request).firstOrNull()
            remaining.remove(request)
            if (best == null) continue

            val assignment = SmartScheduleAssignment(request.item, best.start, best.end)
            assignments[request.item.id] = assignment
            occupied += request.item.copy(startTimeMinutes = best.start, endTimeMinutes = best.end)
        }
        return assignments
    }

    private fun availableOptions(
        request: SmartScheduleRequest,
        occupied: List<DailyPlanItem>,
        earliestStart: Int
    ): List<SmartScheduleOption> {
        val duration = request.durationMinutes.coerceIn(MinimumPlanDurationMinutes, MyDayMinutesPerDay)
        val lastStart = MyDayMinutesPerDay - duration
        return (earliestStart.coerceIn(0, lastStart)..lastStart)
            .filter { start ->
                occupied.none { item ->
                    val range = item.occupiedTimeRange() ?: return@none false
                    start < range.second && start + duration > range.first
                }
            }
            .map { start -> SmartScheduleOption(start, start + duration, abs(start - request.preferredStart)) }
            // Preserve the existing UX of searching forward from the preferred time first.
            // The raw distance score is still used for regret, so a narrow preference can
            // win against a flexible one even when both have the same best slot.
            .sortedWith(
                compareBy<SmartScheduleOption> { if (it.start < request.preferredStart) 1 else 0 }
                    .thenBy { it.score }
                    .thenBy { it.start }
            )
    }

    private data class SmartScheduleRequest(
        val item: DailyPlanItem,
        val preferredStart: Int,
        val durationMinutes: Int
    )

    private data class SmartScheduleOption(
        val start: Int,
        val end: Int,
        val score: Int
    )

    private data class SmartScheduleAssignment(
        val item: DailyPlanItem,
        val start: Int,
        val end: Int
    )

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
 * Most frequent historical (start, end) range across [samples]. The start is
 * clamped to never be before [nowMinutes]. Returns null when there are no samples.
 */
fun bestSmartScheduleRange(
    samples: List<SmartTimeSample>,
    nowMinutes: Int
): Pair<Int, Int>? {
    if (samples.isEmpty()) return null
    val bestRange = modeOf(
        samples.map { sample ->
            sample.startMinutes to (sample.endMinutes ?: (sample.startMinutes + DefaultTaskDurationMinutes))
        },
        tieBreaker = { left, right ->
            left.first < right.first || (left.first == right.first && left.second < right.second)
        }
    ) ?: return null
    val bestStart = bestRange.first
    val bestEnd = bestRange.second
    val duration = (bestEnd - bestStart).coerceIn(MinimumPlanDurationMinutes, MyDayMinutesPerDay)
    return maxOf(bestStart, nowMinutes) to duration
}

private fun <T> modeOf(values: List<T>, tieBreaker: (T, T) -> Boolean = { _, _ -> false }): T? {
    if (values.isEmpty()) return null
    val counts = HashMap<T, Int>()
    values.forEach { counts[it] = (counts[it] ?: 0) + 1 }
    var best = values.first()
    var bestCount = counts.getValue(best)
    for ((value, count) in counts) {
        if (count > bestCount || (count == bestCount && tieBreaker(value, best))) {
            best = value
            bestCount = count
        }
    }
    return best
}
