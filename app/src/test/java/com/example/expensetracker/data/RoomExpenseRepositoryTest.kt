package com.example.expensetracker.data

import com.example.expensetracker.data.local.db.CategoryTotalRow
import com.example.expensetracker.data.local.db.ExpenseDao
import com.example.expensetracker.data.local.db.ExpenseEntity
import com.example.expensetracker.data.repository.RoomExpenseRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.testutil.TestData.expense
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomExpenseRepositoryTest {
    private val dao = mockk<ExpenseDao>(relaxed = true)
    private val repository = RoomExpenseRepository(dao)

    private fun entity(category: String = "FOOD", currency: String = "UAH", id: Long = 1) =
        ExpenseEntity(id, 12_550, currency, category, 1_000, "note")

    @Test
    fun `maps entities to domain`() = runTest {
        every { dao.observeRecent(10) } returns flowOf(listOf(entity()))

        val result = repository.observeRecent(10).first().single()

        assertEquals(1L, result.id)
        assertEquals(12_550L, result.amountMinor)
        assertEquals(Currency.UAH, result.currency)
        assertEquals(Category.FOOD, result.category)
        assertEquals("note", result.note)
    }

    @Test
    fun `unknown category falls back to OTHER, unknown currency is dropped`() = runTest {
        every { dao.observeAll() } returns flowOf(
            listOf(entity(category = "FUTURE_CATEGORY", id = 1), entity(currency = "JPY", id = 2)),
        )
        val result = repository.observeHistory(null).first()
        assertEquals(1, result.size)
        assertEquals(Category.OTHER, result.single().category)
    }

    @Test
    fun `history picks the query by category`() = runTest {
        every { dao.observeAll() } returns flowOf(listOf(entity(id = 1)))
        every { dao.observeByCategory("HEALTH") } returns flowOf(listOf(entity(category = "HEALTH", id = 2)))

        assertEquals(1L, repository.observeHistory(null).first().single().id)
        assertEquals(2L, repository.observeHistory(Category.HEALTH).first().single().id)
    }

    @Test
    fun `totals pass the half open range and map rows`() = runTest {
        every { dao.observeCategoryTotals(100L, 200L) } returns flowOf(
            listOf(CategoryTotalRow("FOOD", "USD", 999, 2), CategoryTotalRow("FOOD", "XXX", 1, 1)),
        )

        val totals = repository.observeCategoryTotals(TimeRange(100, 200)).first()

        assertEquals(1, totals.size)
        assertEquals(999L, totals.single().totalMinor)
        assertEquals(Currency.USD, totals.single().currency)
        assertEquals(2, totals.single().count)
    }

    @Test
    fun `add stores the amount as minor units under the amount column`() = runTest {
        val saved = slot<ExpenseEntity>()
        coEvery { dao.insert(capture(saved)) } returns 7L

        val id = repository.add(expense(amountMinor = 4_200, currency = Currency.EUR, category = Category.HOUSING, note = "x"))

        assertEquals(7L, id)
        assertEquals(4_200L, saved.captured.amount)
        assertEquals("EUR", saved.captured.currency)
        assertEquals("HOUSING", saved.captured.category)
    }

    @Test
    fun `delete and clear delegate to the dao`() = runTest {
        repository.delete(5)
        repository.clear()
        coVerify { dao.deleteById(5) }
        coVerify { dao.deleteAll() }
    }
}
