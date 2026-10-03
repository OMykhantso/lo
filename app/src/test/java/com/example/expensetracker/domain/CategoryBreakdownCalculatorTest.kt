package com.example.expensetracker.domain

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.RateTable
import com.example.expensetracker.domain.usecase.CategoryBreakdownCalculator
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBreakdownCalculatorTest {
    private val table = RateTable(mapOf(Currency.USD to BigDecimal("40")))

    private fun total(category: Category, currency: Currency, minor: Long, count: Int = 1) =
        CategoryTotal(category, currency, minor, count)

    @Test
    fun `merges one category across currencies and sorts by amount`() {
        val rows = listOf(
            total(Category.FOOD, Currency.UAH, 10_000, 2),
            total(Category.FOOD, Currency.USD, 500),     // $5 = 200 грн
            total(Category.HOUSING, Currency.UAH, 35_000),
            total(Category.HEALTH, Currency.UAH, 10_000),
        )
        val result = CategoryBreakdownCalculator.calculate(rows, table, Currency.UAH)

        assertEquals(Currency.UAH, result.currency)
        assertEquals(75_000L, result.totalMinor)
        assertEquals(listOf(Category.HOUSING, Category.FOOD, Category.HEALTH), result.slices.map { it.category })
        assertEquals(30_000L, result.slices.single { it.category == Category.FOOD }.totalMinor) // 10 000 + $5 = 20 000
        assertEquals(3, result.slices.single { it.category == Category.FOOD }.count)
    }

    @Test
    fun `fractions add up to one`() {
        val rows = Category.entries.mapIndexed { i, c -> total(c, Currency.UAH, 1_000L * (i + 1)) }
        val result = CategoryBreakdownCalculator.calculate(rows, table, Currency.UAH)
        assertEquals(1.0f, result.slices.sumOf { it.fraction.toDouble() }.toFloat(), 0.0001f)
        assertTrue(result.slices.all { it.fraction in 0f..1f })
    }

    @Test
    fun `empty input gives an empty breakdown`() {
        val result = CategoryBreakdownCalculator.calculate(emptyList(), table, Currency.UAH)
        assertEquals(0L, result.totalMinor)
        assertTrue(result.slices.isEmpty())
    }

    @Test
    fun `categories without a convertible amount are skipped and counted`() {
        val rows = listOf(
            total(Category.FOOD, Currency.UAH, 1_000),
            total(Category.HEALTH, Currency.EUR, 900, count = 4), // немає курсу EUR
        )
        val result = CategoryBreakdownCalculator.calculate(rows, table, Currency.UAH)
        assertEquals(listOf(Category.FOOD), result.slices.map { it.category })
        assertEquals(4, result.unconvertibleCount)
        assertEquals(1.0f, result.slices.single().fraction, 0.0001f)
    }

    @Test
    fun `zero-sum categories do not appear on the chart`() {
        val rows = listOf(total(Category.FOOD, Currency.UAH, 0), total(Category.HEALTH, Currency.UAH, 500))
        val result = CategoryBreakdownCalculator.calculate(rows, table, Currency.UAH)
        assertEquals(listOf(Category.HEALTH), result.slices.map { it.category })
    }

    @Test
    fun `shows the chart in another currency`() {
        val rows = listOf(total(Category.FOOD, Currency.UAH, 4_000), total(Category.HEALTH, Currency.USD, 100))
        val result = CategoryBreakdownCalculator.calculate(rows, table, Currency.USD)
        assertEquals(Currency.USD, result.currency)
        assertEquals(100L, result.slices.single { it.category == Category.FOOD }.totalMinor)
        assertEquals(100L, result.slices.single { it.category == Category.HEALTH }.totalMinor)
        assertEquals(0.5f, result.slices.first().fraction, 0.0001f)
    }
}
