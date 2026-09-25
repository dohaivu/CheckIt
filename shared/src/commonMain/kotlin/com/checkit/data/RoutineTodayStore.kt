package com.checkit.data

import com.checkit.domain.RoutineTodayState
import kotlinx.coroutines.flow.Flow

/** Transient today-state for routines; backed by AppDataStore, excluded from backup/sync. */
interface RoutineTodayStore {
    fun observe(): Flow<RoutineTodayState>
    suspend fun save(epochDay: Int, checks: Map<String, Set<String>>)
}

class DataStoreRoutineTodayStore(
    private val appDataStore: AppDataStore
) : RoutineTodayStore {
    override fun observe(): Flow<RoutineTodayState> = appDataStore.routineToday

    override suspend fun save(epochDay: Int, checks: Map<String, Set<String>>) {
        appDataStore.setRoutineToday(epochDay, checks)
    }
}
