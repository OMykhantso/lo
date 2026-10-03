package com.example.expensetracker.ui

import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.data.repository.InMemorySettingsRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.domain.usecase.ObserveCategoryBreakdown
import com.example.expensetracker.domain.usecase.RatesSync
import com.example.expensetracker.testutil.FakeRatesRepository
import com.example.expensetracker.testutil.FakeRatesRepository.Companion.snapshotOf
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.testutil.TestData.expense
import com.example.expensetracker.testutil.keepCollecting
import com.example.expensetracker.ui.analytics.AnalyticsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repo = InMemoryExpenseRepository()
    private val settings = InMemorySettingsRepository()
    private val rates = FakeRatesRepository(snapshotOf(Currency.USD to "40"))
    private val march = TimeRange.currentMonth(TestData.clock)

    private fun TestScope.viewModel() = AnalyticsViewModel(
        settings,
        RatesSync(rates, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), TestData.clock),
        ObserveCategoryBreakdown(repo, settings, rates, TestData.clock),
    )

    @Test
    fun `slices are sorted by amount and limited to the current month`() = runTest {
        repo.addAll(
            listOf(
                expense(amountMinor = 1_000, category = Category.FOOD, timestamp = march.startInclusive),
                expense(amountMinor = 9_000, category = Category.HOUSING, timestamp = march.startInclusive),
                expense(amountMinor = 5_000, category = Category.HEALTH, timestamp = march.startInclusive),
                expense(amountMinor = 99_999, category = Category.FOOD, timestamp = march.startInclusive - 1),
            ),
        )
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(
            listOf(Category.HOUSING, Category.HEALTH, Category.FOOD),
            viewModel.state.value.breakdown!!.slices.map { it.category },
        )
    }

    @Test
    fun `mixed currencies are merged into one chart in the chosen currency`() = runTest {
        repo.addAll(
            listOf(
                expense(amountMinor = 4_000, category = Category.FOOD, timestamp = march.startInclusive),
                expense(amountMinor = 100, currency = Currency.USD, category = Category.FOOD, timestamp = march.startInclusive),
            ),
        )
        val viewModel = viewModel()
        keepCollecting(viewModel.state)

        assertEquals(8_000L, viewModel.state.value.breakdown!!.totalMinor)

        viewModel.onCurrencySelected(Currency.USD)
        assertEquals(Currency.USD, viewModel.state.value.breakdown!!.currency)
        assertEquals(200L, viewModel.state.value.breakdown!!.totalMinor)
    }

    @Test
    fun `empty month gives an empty chart rather than an error`() = runTest {
        val viewModel = viewModel()
        keepCollecting(viewModel.state)
        assertTrue(viewModel.state.value.breakdown!!.slices.isEmpty())
    }
}
