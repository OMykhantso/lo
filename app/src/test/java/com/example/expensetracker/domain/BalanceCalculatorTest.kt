package com.example.expensetracker.domain

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.RateTable
import com.example.expensetracker.domain.usecase.BalanceCalculator
import com.example.expensetracker.domain.usecase.BudgetStatus
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BalanceCalculatorTest {
    private val table = RateTable(mapOf(Currency.USD to BigDecimal("40"), Currency.EUR to BigDecimal("50")))

    private fun total(category: Category, currency: Currency, minor: Long, count: Int = 1) =
        CategoryTotal(category, currency, minor, count)

    private val mixed = listOf(
        total(Category.FOOD, Currency.UAH, 10_000, 2),
        total(Category.HOUSING, Currency.UAH, 5_000),
        total(Category.FOOD, Currency.USD, 1_000), // $10 = 400 грн
    )

    @Test
    fun `remaining balance in hryvnia converts foreign expenses`() {
        val s = BalanceCalculator.calculate(mixed, budgetUahMinor = 100_000, table, Currency.UAH)

        assertEquals(Currency.UAH, s.currency)
        assertEquals(55_000L, s.spentMinor)
        assertEquals(100_000L, s.budgetMinor)
        assertEquals(45_000L, s.remainingMinor)
        assertEquals(0.55f, s.usedFraction!!, 0.0001f)
        assertEquals(BudgetStatus.OK, s.status)
        assertEquals(0, s.unconvertibleCount)
    }

    @Test
    fun `recalculates in another display currency`() {
        val s = BalanceCalculator.calculate(mixed, budgetUahMinor = 100_000, table, Currency.USD)

        assertEquals(Currency.USD, s.currency)
        assertEquals(1_375L, s.spentMinor)   // 150 грн / 40 = $3,75 + $10
        assertEquals(2_500L, s.budgetMinor)  // 1000 грн / 40 = $25
        assertEquals(1_125L, s.remainingMinor)
    }

    @Test
    fun `status thresholds - ok, warning at 80 percent, exactly spent, exceeded`() {
        fun status(spent: Long) = BalanceCalculator.calculate(
            listOf(total(Category.FOOD, Currency.UAH, spent)), 100_000, table, Currency.UAH,
        )
        assertEquals(BudgetStatus.OK, status(79_999).status)
        assertEquals(BudgetStatus.WARNING, status(80_000).status)
        assertEquals(BudgetStatus.WARNING, status(100_000).status)
        assertEquals(0L, status(100_000).remainingMinor)
        assertEquals(BudgetStatus.EXCEEDED, status(100_001).status)
        assertEquals(-1L, status(100_001).remainingMinor)
    }

    @Test
    fun `no budget means no remaining, fraction or status`() {
        val s = BalanceCalculator.calculate(mixed, budgetUahMinor = null, table, Currency.UAH)
        assertEquals(55_000L, s.spentMinor)
        assertNull(s.budgetMinor)
        assertNull(s.remainingMinor)
        assertNull(s.usedFraction)
        assertEquals(BudgetStatus.NO_BUDGET, s.status)
    }

    @Test
    fun `a zero budget is treated as not set`() {
        val s = BalanceCalculator.calculate(mixed, budgetUahMinor = 0, table, Currency.UAH)
        assertEquals(BudgetStatus.NO_BUDGET, s.status)
        assertNull(s.usedFraction)
    }

    @Test
    fun `empty month`() {
        val s = BalanceCalculator.calculate(emptyList(), 100_000, table, Currency.UAH)
        assertEquals(0L, s.spentMinor)
        assertEquals(100_000L, s.remainingMinor)
        assertEquals(BudgetStatus.OK, s.status)
    }

    @Test
    fun `offline without any cached rates only hryvnia expenses count and the rest is reported`() {
        val s = BalanceCalculator.calculate(mixed, 100_000, RateTable.EMPTY, Currency.UAH)
        assertEquals(15_000L, s.spentMinor)
        assertEquals(1, s.unconvertibleCount)
        assertEquals(85_000L, s.remainingMinor)
    }

    @Test
    fun `requested currency without a rate falls back to hryvnia`() {
        val onlyUsd = RateTable(mapOf(Currency.USD to BigDecimal("40")))
        val s = BalanceCalculator.calculate(mixed, 100_000, onlyUsd, Currency.EUR)
        assertEquals(Currency.UAH, s.currency)
        assertEquals(55_000L, s.spentMinor)
    }

    @Test
    fun `unconvertible expenses are counted by transactions, not by rows`() {
        val rows = listOf(
            total(Category.FOOD, Currency.EUR, 500, count = 3),
            total(Category.HEALTH, Currency.EUR, 700, count = 2),
        )
        val s = BalanceCalculator.calculate(rows, 100_000, RateTable.EMPTY, Currency.UAH)
        assertEquals(5, s.unconvertibleCount)
        assertEquals(0L, s.spentMinor)
    }

    @Test
    fun `rounding is applied once per currency group`() {
        // Три витрати по 1 грн в USD при курсі 40: сума 3 грн = 7,5 цента -> 8, а не 3 × 2,5 -> 3 × 3 = 9.
        val rows = listOf(total(Category.FOOD, Currency.UAH, 300, count = 3))
        val s = BalanceCalculator.calculate(rows, null, table, Currency.USD)
        assertEquals(8L, s.spentMinor)
    }
}
