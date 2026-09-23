package com.checkit.ui.myday

import com.checkit.data.RoutineRepository
import com.checkit.data.RoutineTodayStore
import com.checkit.domain.Routine
import com.checkit.domain.RoutineLog
import com.checkit.domain.RoutineStepTemplate
import com.checkit.domain.RoutineTodayState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeRoutineRepository(initial: List<Routine> = emptyList()) : RoutineRepository {
    val routines = initial.toMutableList()
    val logs = mutableListOf<RoutineLog>()
    private val routinesFlow = MutableStateFlow(initial)

    override fun observeRoutines(): Flow<List<Routine>> = routinesFlow.asStateFlow()

    override fun observeLogs(startEpochDays: Int, endEpochDays: Int): Flow<List<RoutineLog>> =
        MutableStateFlow(logs.filter { it.dateEpochDays in startEpochDays..endEpochDays })

    override suspend fun saveRoutine(
        id: String?,
        title: String,
        reminderMinutes: Int?,
        steps: List<RoutineStepTemplate>
    ): String {
        val routineId = id ?: "generated"
        routines.removeAll { it.id == routineId }
        routines.add(Routine(id = routineId, title = title, reminderMinutes = reminderMinutes, steps = steps))
        routinesFlow.value = routines.toList()
        return routineId
    }

    override suspend fun deleteRoutine(id: String) {
        routines.removeAll { it.id == id }
        routinesFlow.value = routines.toList()
    }

    override suspend fun upsertLog(log: RoutineLog) {
        logs.removeAll { it.routineId == log.routineId && it.dateEpochDays == log.dateEpochDays }
        logs.add(log)
    }
}

internal class FakeRoutineTodayStore(
    initial: RoutineTodayState = RoutineTodayState(null)
) : RoutineTodayStore {
    private val flow = MutableStateFlow(initial)

    override fun observe(): Flow<RoutineTodayState> = flow.asStateFlow()

    override suspend fun save(epochDay: Int, checks: Map<String, Set<String>>) {
        flow.value = RoutineTodayState(epochDay, checks)
    }
}
