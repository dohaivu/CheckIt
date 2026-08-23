package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.domain.DailyReflectStat
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Rebuilds the precomputed Reflect rollup tables (daily stats, tag rollups,
 * habit check-ins) from source data. Reflect is a read-only view of finished
 * work, so rebuilding once a day is sufficient; callers are responsible for the
 * once-per-day gating (see CheckItApp.runAutoTodayTasks).
 */
class RebuildReflectStatsUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke() {
        repository.rebuildReflectStats()
    }
}

/** Observes precomputed daily aggregates for an inclusive date window. */
class ObserveDailyReflectStatsUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<DailyReflectStat>> =
        repository.observeDailyReflectStats(startDate, endDateInclusive)
}
