package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Rebuilds the precomputed Reflect rollup tables (daily stats, tag rollups,
 * habit check-ins) from source data. Reflect is a read-only view of finished
 * work, so rebuilding once per day is sufficient; an in-memory guard keeps it
 * to at most one rebuild per process per day.
 */
class RebuildReflectStatsUseCase(
    private val repository: CheckItRepository
) {
    private val mutex = Mutex()
    private var lastRebuiltEpochDay: Int? = null

    suspend operator fun invoke() {
        val todayEpochDay = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays().toInt()
        mutex.withLock {
            if (lastRebuiltEpochDay == todayEpochDay) return@withLock
            runCatching { repository.rebuildReflectStats() }
                .onSuccess { lastRebuiltEpochDay = todayEpochDay }
        }
    }
}
