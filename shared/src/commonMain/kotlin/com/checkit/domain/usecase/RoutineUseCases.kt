package com.checkit.domain.usecase

import com.checkit.data.RoutineRepository
import com.checkit.data.RoutineTodayStore
import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.RoutineTodayState
import com.checkit.domain.resolveRoutineTodayChecks
import com.checkit.domain.routinePercent
import com.checkit.notifications.RoutineReminderScheduler
import com.checkit.notifications.ScheduledRoutineReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ObserveRoutinesUseCase(
    private val repository: RoutineRepository
) {
    operator fun invoke(): Flow<List<Routine>> = repository.observeRoutines()
}

class ObserveRoutineTodayUseCase(
    private val todayStore: RoutineTodayStore
) {
    operator fun invoke(): Flow<RoutineTodayState> = todayStore.observe()
}

class ObserveRoutineLogsUseCase(
    private val repository: RoutineRepository
) {
    operator fun invoke(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<RoutineLog>> =
        repository.observeLogs(
            startEpochDays = startDate.toEpochDays().toInt(),
            endEpochDays = endDateInclusive.toEpochDays().toInt()
        )
}

class SaveRoutineUseCase(
    private val repository: RoutineRepository,
    private val reminderScheduler: RoutineReminderScheduler
) {
    suspend operator fun invoke(
        id: String?,
        title: String,
        reminderMinutes: Int?,
        steps: List<RoutineStepTemplate>
    ): String {
        val routineId = repository.saveRoutine(id, title, reminderMinutes, steps)
        val trimmedTitle = title.trim()
        if (reminderMinutes != null) {
            reminderScheduler.scheduleRoutineReminder(
                ScheduledRoutineReminder(
                    routineId = routineId,
                    title = trimmedTitle,
                    reminderMinutes = reminderMinutes,
                    stepCount = steps.count { it.title.isNotBlank() }
                )
            )
        } else if (id != null) {
            reminderScheduler.cancelRoutineReminder(routineId)
        }
        return routineId
    }
}

class DeleteRoutineUseCase(
    private val repository: RoutineRepository,
    private val todayStore: RoutineTodayStore,
    private val reminderScheduler: RoutineReminderScheduler
) {
    suspend operator fun invoke(id: String) {
        repository.deleteRoutine(id)
        reminderScheduler.cancelRoutineReminder(id)
        val current = todayStore.observe().first()
        if (id in current.checks && current.epochDay != null) {
            todayStore.save(current.epochDay, current.checks - id)
        }
    }
}

/**
 * Toggles one step for today. Handles day rollover (stale checks are
 * dropped), persists the transient checks, and seals today's percent-only
 * [RoutineLog] so the heatmap survives the DataStore reset.
 */
class ToggleRoutineStepUseCase(
    private val repository: RoutineRepository,
    private val todayStore: RoutineTodayStore
) {
    suspend operator fun invoke(routineId: String, stepId: String) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val todayEpochDay = today.toEpochDays().toInt()
        val routine = repository.observeRoutines().first().firstOrNull { it.id == routineId }
            ?: return
        val current = todayStore.observe().first()
        val base = resolveRoutineTodayChecks(current.epochDay, current.checks, todayEpochDay)
        val checked = base[routineId].orEmpty()
        val next = if (stepId in checked) checked - stepId else checked + stepId
        todayStore.save(todayEpochDay, base + (routineId to next))
        val validStepIds = routine.steps.map { it.id }.toSet()
        repository.upsertLog(
            RoutineLog(
                routineId = routineId,
                dateEpochDays = todayEpochDay,
                percent = routinePercent(routine.steps.size, next, validStepIds)
            )
        )
    }
}
