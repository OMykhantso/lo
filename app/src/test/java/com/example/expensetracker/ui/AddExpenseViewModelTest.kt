package com.example.expensetracker.ui

import app.cash.turbine.test
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.usecase.BalanceCalculator
import com.example.expensetracker.domain.usecase.BalanceState
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.testutil.FakeRatesRepository.Companion.snapshotOf
import com.example.expensetracker.domain.validation.CategoryError
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.ui.add.AddExpenseEvent
import com.example.expensetracker.ui.add.AddExpenseViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddExpenseViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = mockk<ExpenseRepository>()
    private val viewModel by lazy { AddExpenseViewModel(repository, TestData.clock) }

    @Test
    fun `saving an empty form shows errors and does not touch the repository`() = runTest {
        viewModel.onSave()

        val state = viewModel.state.value
        assertTrue(state.showErrors)
        assertEquals(AmountError.EMPTY, state.validation.amountError)
        assertEquals(CategoryError.NOT_SELECTED, state.validation.categoryError)
        coVerify(exactly = 0) { repository.add(any()) }
    }

    @Test
    fun `zero is rejected and a minus sign is filtered out of the input`() = runTest {
        viewModel.onCategorySelect(Category.FOOD)

        viewModel.onAmountChange("0")
        viewModel.onSave()
        assertEquals(AmountError.ZERO, viewModel.state.value.validation.amountError)

        coVerify(exactly = 0) { repository.add(any()) }

        // мінус відсікається ще фільтром введення; від’ємні значення додатково відхиляє AmountParser
        viewModel.onAmountChange("-5")
        assertEquals("5", viewModel.state.value.amountText)
    }

    @Test
    fun `valid form is stored with parsed minor units, trimmed note and current time`() = runTest {
        val saved = slot<Expense>()
        coEvery { repository.add(capture(saved)) } returns 1L

        viewModel.onAmountChange("125,50")
        viewModel.onCurrencyChange(Currency.USD)
        viewModel.onCategorySelect(Category.TRANSPORT)
        viewModel.onNoteChange("  Таксі  ")

        viewModel.events.test {
            viewModel.onSave()
            assertEquals(AddExpenseEvent.Saved, awaitItem())
        }

        assertEquals(12_550L, saved.captured.amountMinor)
        assertEquals(Currency.USD, saved.captured.currency)
        assertEquals(Category.TRANSPORT, saved.captured.category)
        assertEquals("Таксі", saved.captured.note)
        assertEquals(TestData.NOW.toEpochMilli(), saved.captured.timestamp)
    }

    @Test
    fun `form is reset after saving but the currency is kept`() = runTest {
        coEvery { repository.add(any()) } returns 1L
        viewModel.onAmountChange("10")
        viewModel.onCurrencyChange(Currency.EUR)
        viewModel.onCategorySelect(Category.FOOD)

        viewModel.onSave()

        val state = viewModel.state.value
        assertEquals("", state.amountText)
        assertNull(state.category)
        assertEquals(Currency.EUR, state.currency)
        assertFalse(state.showErrors)
        assertFalse(state.isSaving)
    }

    @Test
    fun `tapping the selected category again clears the selection`() {
        viewModel.onCategorySelect(Category.HEALTH)
        assertEquals(Category.HEALTH, viewModel.state.value.category)
        viewModel.onCategorySelect(Category.HEALTH)
        assertNull(viewModel.state.value.category)
        viewModel.onCategorySelect(Category.FOOD)
        viewModel.onCategorySelect(Category.OTHER)
        assertEquals(Category.OTHER, viewModel.state.value.category)
    }

    @Test
    fun `errors stay hidden until the first save attempt`() {
        viewModel.onAmountChange("abc")
        assertFalse(viewModel.state.value.showErrors)
        viewModel.onAmountChange("1.234")
        assertEquals(AmountError.TOO_MANY_DECIMALS, viewModel.state.value.validation.amountError)
        assertFalse(viewModel.state.value.showErrors)
    }

    // --- Вплив на бюджет --------------------------------------------------------------------------

    private fun balanceState(spent: Long, budget: Long?): BalanceState {
        val rates = snapshotOf(Currency.USD to "40")
        val totals = listOf(com.example.expensetracker.domain.model.CategoryTotal(Category.FOOD, Currency.UAH, spent, 1))
        return BalanceState(BalanceCalculator.calculate(totals, budget, rates.table, Currency.UAH), Currency.UAH, rates)
    }

    @Test
    fun `shows what remains from the budget once a valid amount is typed`() = runTest {
        val vm = AddExpenseViewModel(repository, TestData.clock, kotlinx.coroutines.flow.flowOf(balanceState(50_000, 100_000)))

        assertNull(vm.state.value.budgetImpact) // сума ще не введена
        vm.onAmountChange("100")

        val impact = vm.state.value.budgetImpact!!
        assertEquals(40_000L, impact.remainingAfterMinor)
        assertEquals(BudgetStatus.OK, impact.status)
    }

    @Test
    fun `warns when the new expense would exceed the budget`() = runTest {
        val vm = AddExpenseViewModel(repository, TestData.clock, kotlinx.coroutines.flow.flowOf(balanceState(90_000, 100_000)))
        vm.onAmountChange("200")
        assertEquals(BudgetStatus.EXCEEDED, vm.state.value.budgetImpact!!.status)
        assertEquals(-10_000L, vm.state.value.budgetImpact!!.remainingAfterMinor)
    }

    @Test
    fun `foreign currency is converted for the estimate`() = runTest {
        val vm = AddExpenseViewModel(repository, TestData.clock, kotlinx.coroutines.flow.flowOf(balanceState(0, 100_000)))
        vm.onAmountChange("10")
        vm.onCurrencyChange(Currency.USD) // $10 = 400 грн
        assertEquals(60_000L, vm.state.value.budgetImpact!!.remainingAfterMinor)
    }

    @Test
    fun `no hint without a budget, without a valid amount, or without a rate`() = runTest {
        val noBudget = AddExpenseViewModel(repository, TestData.clock, kotlinx.coroutines.flow.flowOf(balanceState(0, null)))
        noBudget.onAmountChange("10")
        assertNull(noBudget.state.value.budgetImpact)

        val vm = AddExpenseViewModel(repository, TestData.clock, kotlinx.coroutines.flow.flowOf(balanceState(0, 100_000)))
        vm.onAmountChange("0")
        assertNull(vm.state.value.budgetImpact)
        vm.onAmountChange("5")
        vm.onCurrencyChange(Currency.EUR) // курсу EUR немає
        assertNull(vm.state.value.budgetImpact)
    }
}
