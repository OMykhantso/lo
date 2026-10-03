package com.example.expensetracker.ui

import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.data.repository.InMemorySettingsRepository
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.repository.RefreshResult
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.domain.usecase.ObserveBalance
import com.example.expensetracker.domain.usecase.RatesSync
import com.example.expensetracker.domain.usecase.SyncState
import com.example.expensetracker.testutil.FakeRatesRepository
import com.example.expensetracker.testutil.FakeRatesRepository.Companion.snapshotOf
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.testutil.TestData.expense
import com.example.expensetracker.testutil.keepCollecting
import com.example.expensetracker.ui.overview.OverviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repo = InMemoryExpenseRepository()
    private val settings = InMemorySettingsRepository(budgetLimitMinor = 100_000)
    private val rates = FakeRatesRepository(snapshotOf(Currency.USD to "40", Currency.EUR to "50"))
    private val now = TestData.NOW.toEpochMilli()
    private val march = TimeRange.currentMonth(TestData.clock)

    private fun kotlinx.coroutines.test.TestScope.viewModel(): OverviewViewModel {
        val sync = RatesSync(rates, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), TestData.clock)
        return OverviewViewModel(repo, settings, sync, ObserveBalance(repo, settings, rates, TestData.clock))
    }

    @Test
    fun `starts loading and then shows the balance`() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.state.value.isLoading)

        keepCollecting(viewModel.state)

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(100_000L, state.balance!!.remainingMinor)
        assertEquals(BudgetStatus.OK, state.balance!!.status)
        assertTrue(state.recent.isEmpty())
    }

    @Test
    fun `balance is reactive - a new expense lowers the remainder immediately`() = runTest {
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        repo.add(expense(amountMinor = 30_000, timestamp = now))
        assertEquals(70_000L, viewModel.state.value.balance!!.remainingMinor)

        repo.add(expense(amountMinor = 55_000, timestamp = now))
        assertEquals(15_000L, viewModel.state.value.balance!!.remainingMinor)
        assertEquals(BudgetStatus.WARNING, viewModel.state.value.balance!!.status)
        assertEquals(2, viewModel.state.value.recent.size)

        repo.add(expense(amountMinor = 20_000, timestamp = now))
        assertEquals(BudgetStatus.EXCEEDED, viewModel.state.value.balance!!.status)
        assertEquals(-5_000L, viewModel.state.value.balance!!.remainingMinor)
    }

    @Test
    fun `only the current month counts, but older expenses still show in the recent list`() = runTest {
        repo.addAll(
            listOf(
                expense(amountMinor = 10_000, timestamp = now),
                expense(amountMinor = 77_777, timestamp = march.startInclusive - 1),
            ),
        )
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        assertEquals(10_000L, viewModel.state.value.balance!!.spentMinor)
        assertEquals(2, viewModel.state.value.recent.size)
    }

    @Test
    fun `recent list is limited to ten items, newest first`() = runTest {
        repeat(15) { repo.add(expense(timestamp = now - it * 1_000L, note = "n$it")) }
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        assertEquals(OverviewViewModel.RECENT_LIMIT, viewModel.state.value.recent.size)
        assertEquals("n0", viewModel.state.value.recent.first().note)
    }

    @Test
    fun `switching the display currency recalculates with cached rates`() = runTest {
        repo.add(expense(amountMinor = 40_000, timestamp = now)) // 400 грн
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        viewModel.onCurrencySelected(Currency.USD)

        val balance = viewModel.state.value.balance!!
        assertEquals(Currency.USD, balance.currency)
        assertEquals(1_000L, balance.spentMinor)       // $10
        assertEquals(1_500L, balance.remainingMinor)   // $25 - $10
        assertEquals(Currency.USD, viewModel.state.value.requestedCurrency)
    }

    @Test
    fun `offline with no cached rates falls back to hryvnia and flags foreign expenses`() = runTest {
        rates.snapshot.value = snapshotOf()
        settings.setDisplayCurrency(Currency.USD)
        repo.addAll(listOf(expense(amountMinor = 10_000, timestamp = now), expense(amountMinor = 500, currency = Currency.EUR, timestamp = now)))
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        val state = viewModel.state.value
        assertEquals(Currency.USD, state.requestedCurrency)
        assertEquals(Currency.UAH, state.balance!!.currency)
        assertEquals(10_000L, state.balance!!.spentMinor)
        assertEquals(1, state.balance!!.unconvertibleCount)
    }

    @Test
    fun `new cached rates are picked up without restarting`() = runTest {
        rates.snapshot.value = snapshotOf()
        repo.add(expense(amountMinor = 100, currency = Currency.USD, timestamp = now))
        val viewModel = viewModel()
        keepCollecting(viewModel.state)
        assertEquals(1, viewModel.state.value.balance!!.unconvertibleCount)

        rates.snapshot.value = snapshotOf(Currency.USD to "40")

        assertEquals(0, viewModel.state.value.balance!!.unconvertibleCount)
        assertEquals(4_000L, viewModel.state.value.balance!!.spentMinor)
    }

    @Test
    fun `without a budget only the spent amount is known`() = runTest {
        settings.setBudgetLimit(null)
        repo.add(expense(amountMinor = 1_000, timestamp = now))
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        val balance = viewModel.state.value.balance!!
        assertEquals(BudgetStatus.NO_BUDGET, balance.status)
        assertNull(balance.remainingMinor)
        assertEquals(1_000L, balance.spentMinor)
    }

    @Test
    fun `sync failure is surfaced and refresh is forced from the button`() = runTest {
        rates.result = RefreshResult.Failure(RefreshFailure.NO_NETWORK)
        val viewModel = viewModel()
        keepCollecting(viewModel.state)
        assertEquals(SyncState.Idle, viewModel.state.value.sync)

        viewModel.onRefreshRates()

        assertEquals(SyncState.Failed(RefreshFailure.NO_NETWORK), viewModel.state.value.sync)
        assertEquals(1, rates.refreshCalls)
        assertNotNull(viewModel.state.value.balance) // офлайн не ламає екран
    }
}
