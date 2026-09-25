package com.checkit.data

import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.decodeActiveWeekdays
import com.checkit.domain.decodeRoutineSteps
import com.checkit.domain.encodeActiveWeekdays
import com.checkit.domain.encodeRoutineSteps
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.Uuid

interface RoutineRepository {
    fun observeRoutines(): Flow<List<Routine>>
    fun observeLogs(startEpochDays: Int, endEpochDays: Int): Flow<List<RoutineLog>>
    suspend fun saveRoutine(
        id: String?,
        title: String,
        description: String,
        reminderMinutes: Int?,
        activeWeekdays: Set<DayOfWeek>,
        steps: List<RoutineStepTemplate>
    ): String
    suspend fun deleteRoutine(id: String)
    suspend fun upsertLog(log: RoutineLog)
}

class RoomRoutineRepository(
    private val dao: CheckItDao
) : RoutineRepository {

    override fun observeRoutines(): Flow<List<Routine>> =
        dao.observeRoutines().map { entities -> entities.map { it.toDomain() } }

    override fun observeLogs(startEpochDays: Int, endEpochDays: Int): Flow<List<RoutineLog>> =
        dao.observeRoutineLogs(startEpochDays, endEpochDays).map { entities ->
            entities.map { RoutineLog(it.routineId, it.dateEpochDays, it.percent) }
        }

    override suspend fun saveRoutine(
        id: String?,
        title: String,
        description: String,
        reminderMinutes: Int?,
        activeWeekdays: Set<DayOfWeek>,
        steps: List<RoutineStepTemplate>
    ): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "Routine title must not be blank" }
        val normalizedSteps = steps
            .filter { it.title.isNotBlank() }
            .mapIndexed { index, step ->
                step.copy(
                    title = step.title.trim(),
                    description = step.description.trim(),
                    sortOrder = index
                )
            }
        val existing = id?.let { dao.routineById(it) }
        val routineId = existing?.id ?: Uuid.random().toString()
        dao.upsertRoutine(
            RoutineEntity(
                id = routineId,
                title = trimmed,
                description = description.trim(),
                reminderMinutes = reminderMinutes,
                activeWeekdaysJson = encodeActiveWeekdays(activeWeekdays),
                sortOrder = existing?.sortOrder ?: dao.nextRoutineSortOrder(),
                stepsJson = encodeRoutineSteps(normalizedSteps),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
        )
        return routineId
    }

    override suspend fun deleteRoutine(id: String) {
        dao.deleteRoutine(id)
    }

    override suspend fun upsertLog(log: RoutineLog) {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.upsertRoutineLog(
            RoutineLogEntity(
                routineId = log.routineId,
                dateEpochDays = log.dateEpochDays,
                percent = log.percent.coerceIn(0, 100),
                updatedAtMillis = now
            )
        )
    }
}

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    title = title,
    description = description,
    reminderMinutes = reminderMinutes,
    activeWeekdays = decodeActiveWeekdays(activeWeekdaysJson),
    sortOrder = sortOrder,
    steps = decodeRoutineSteps(stepsJson).sortedBy { it.sortOrder },
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)
