package com.checkit.domain.usecase

import com.checkit.ui.tasks.FakeCheckItRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedCalloutResetUseCaseTest {

    @Test
    fun resetDelegatesTheItemToTheRepository() = runTest {
        val repository = FakeCheckItRepository()
        val useCase = ResetNestedItemCalloutToDefaultsUseCase(repository)

        useCase("item-1")

        assertEquals(listOf("item-1"), repository.resetCalloutItemIds)
    }
}
