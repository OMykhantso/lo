package com.example.expensetracker.ui

import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.MoneyAmount
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.testutil.TestData.expense
import com.example.expensetracker.testutil.keepCollecting
import com.example.expensetracker.ui.overview.OverviewViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OverviewViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repo = InMemoryExpenseRepository()
    private val now = TestData.NOW.toEpochMilli()
    private val march = TimeRange.currentMonth(TestData.clock)

    @Test
    fun `starts in loading state then shows empty month`() = runTest {
        val viewModel = OverviewViewModel(repo, TestData.clock)
        assertTrue(viewModel.state.value.isLoading)

        keepCollecting(viewModel.state)

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.monthSpent.isEmpty())
        assertTrue(viewModel.state.value.recent.isEmpty())
    }

    @Test
    fun `sums only the current month, per currency`() = runTest {
        repo.addAll(
            listOf(
                expense(amountMinor = 10_000, timestamp = now),
                expense(amountMinor = 5_050, timestamp = march.startInclusive),
                expense(amountMinor = 999, currency = Currency.USD, timestamp = now),
                expense(amountMinor = 77_777, timestamp = march.startInclusive - 1), // лютий
            ),
        )
        val viewModel = OverviewViewModel(repo, TestData.clock)
        keepCollecting(viewModel.state)

        assertEquals(
            listOf(MoneyAmount(15_050, Currency.UAH), MoneyAmount(999, Currency.USD)),
            viewModel.state.value.monthSpent,
        )
    }

    @Test
    fun `recent list is limited and newest first, including older months`() = runTest {
        repeat(15) { repo.add(expense(timestamp = now - it * 1_000L, note = "n$it")) }
        val viewModel = OverviewViewModel(repo, TestData.clock)
        keepCollecting(viewModel.state)

        val recent = viewModel.state.value.recent
        assertEquals(OverviewViewModel.RECENT_LIMIT, recent.size)
        assertEquals("n0", recent.first().note)
    }

    @Test
    fun `reacts to a newly added expense`() = runTest {
        val viewModel = OverviewViewModel(repo, TestData.clock)
        keepCollecting(viewModel.state)
        assertTrue(viewModel.state.value.recent.isEmpty())

        repo.add(expense(amountMinor = 4_200, timestamp = now))

        assertEquals(1, viewModel.state.value.recent.size)
        assertEquals(listOf(MoneyAmount(4_200, Currency.UAH)), viewModel.state.value.monthSpent)
    }
}
