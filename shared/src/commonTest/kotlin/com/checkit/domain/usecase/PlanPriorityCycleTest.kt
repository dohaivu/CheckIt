package com.checkit.domain.usecase

import com.checkit.domain.PeriodPlan
import com.checkit.domain.PlanPeriod
import com.checkit.domain.PlanPriority
import com.checkit.domain.wouldCreateCycle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanPriorityCycleTest {
    private val testPlan = PeriodPlan(
        id = 1L,
        period = PlanPeriod.Year,
        startEpochDays = 0,
        endEpochDays = 0
    )

    private fun priority(id: Long, parentId: Long? = null) = PlanPriority(
        id = id,
        periodPlan = testPlan,
        parentId = parentId,
        title = "P$id",
        sortOrder = id.toInt(),
        createdAtMillis = 0L,
        updatedAtMillis = 0L
    )

    @Test
    fun movingAUnderItselfIsRejected() {
        val priorities = listOf(priority(1), priority(2, parentId = 1))
        assertTrue(wouldCreateCycle(priorities, id = 1, newParentId = 1))
    }

    @Test
    fun movingAUnderItsOwnDescendantIsRejected() {
        val priorities = listOf(priority(1), priority(2, parentId = 1), priority(3, parentId = 2))
        assertTrue(wouldCreateCycle(priorities, id = 1, newParentId = 3))
        assertTrue(wouldCreateCycle(priorities, id = 1, newParentId = 2))
    }

    @Test
    fun movingToRootOrSiblingIsAllowed() {
        val priorities = listOf(priority(1), priority(2, parentId = 1), priority(3))
        assertFalse(wouldCreateCycle(priorities, id = 2, newParentId = null))
        assertFalse(wouldCreateCycle(priorities, id = 2, newParentId = 3))
    }

    @Test
    fun unrelatedMoveDoesNotCreateCycle() {
        val priorities = listOf(priority(1), priority(2), priority(3))
        assertFalse(wouldCreateCycle(priorities, id = 2, newParentId = 3))
        assertFalse(wouldCreateCycle(priorities, id = 1, newParentId = null))
    }
}
