package com.example.expensetracker.ui

import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.testutil.TestData.expense
import com.example.expensetracker.testutil.keepCollecting
import com.example.expensetracker.ui.analytics.AnalyticsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class AnalyticsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    @Test
    fun `rows are sorted by amount and limited to the current month`() = runTest {
        val repo = InMemoryExpenseRepository()
        val march = TimeRange.currentMonth(TestData.clock)
        repo.addAll(
            listOf(
                expense(amountMinor = 1_000, category = Category.FOOD, timestamp = march.startInclusive),
                expense(amountMinor = 9_000, category = Category.HOUSING, timestamp = march.startInclusive),
                expense(amountMinor = 5_000, category = Category.HEALTH, timestamp = march.startInclusive),
                expense(amountMinor = 99_999, category = Category.FOOD, timestamp = march.startInclusive - 1),
            ),
        )
        val viewModel = AnalyticsViewModel(repo, TestData.clock)
        keepCollecting(viewModel.state)

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(
            listOf(Category.HOUSING, Category.HEALTH, Category.FOOD),
            viewModel.state.value.rows.map { it.category },
        )
    }
}
