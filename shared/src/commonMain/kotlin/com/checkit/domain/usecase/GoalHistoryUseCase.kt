package com.checkit.domain.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.checkit.data.CheckItRepository
import com.checkit.domain.Period
import com.checkit.domain.PeriodGoalHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * Unlimited newest-first history of persisted period goals of one [Period]
 * before [beforeEpochDays] (exclusive, epoch days — the current focus start).
 * Each item carries tracked minutes aggregated from the daily rollups, so the
 * history sheet needs no extra queries. Rows are shown with any data (no
 * blank filtering).
 */
class ObserveGoalHistoryUseCase(
    private val repository: CheckItRepository
) {
    operator fun invoke(
        period: Period,
        beforeEpochDays: Int,
        pageSize: Int = DefaultPageSize
    ): Flow<PagingData<PeriodGoalHistoryItem>> =
        Pager(
            config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
            pagingSourceFactory = { repository.pagingGoalHistory(period, beforeEpochDays) }
        ).flow

    companion object {
        const val DefaultPageSize: Int = 20
    }
}
