package com.example.expensetracker.data

import app.cash.turbine.test
import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.testutil.TestData.expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryExpenseRepositoryTest {
    private val hour = 60 * 60 * 1000L
    private val now = TestData.NOW.toEpochMilli()

    @Test
    fun `assigns increasing ids and returns the newest first`() = runTest {
        val repo = InMemoryExpenseRepository()
        val first = repo.add(expense(timestamp = now - 2 * hour, note = "old"))
        val second = repo.add(expense(timestamp = now, note = "new"))
        assertTrue(second > first)

        val recent = repo.observeRecent(10).first()
        assertEquals(listOf("new", "old"), recent.map { it.note })
    }

    @Test
    fun `recent respects the limit`() = runTest {
        val repo = InMemoryExpenseRepository()
        repeat(5) { repo.add(expense(timestamp = now - it * hour, note = "$it")) }
        assertEquals(listOf("0", "1", "2"), repo.observeRecent(3).first().map { it.note })
    }

    @Test
    fun `history can be filtered by category`() = runTest {
        val repo = InMemoryExpenseRepository()
        repo.addAll(listOf(expense(category = Category.FOOD), expense(category = Category.HEALTH), expense(category = Category.FOOD)))
        assertEquals(3, repo.observeHistory(null).first().size)
        assertEquals(2, repo.observeHistory(Category.FOOD).first().size)
        assertEquals(0, repo.observeHistory(Category.HOUSING).first().size)
    }

    @Test
    fun `totals group by category and currency inside the range only`() = runTest {
        val repo = InMemoryExpenseRepository()
        val march = TimeRange.currentMonth(TestData.clock)
        repo.addAll(
            listOf(
                expense(amountMinor = 10_000, category = Category.FOOD, timestamp = now),
                expense(amountMinor = 2_550, category = Category.FOOD, timestamp = now - hour),
                expense(amountMinor = 500, category = Category.FOOD, currency = Currency.USD, timestamp = now),
                expense(amountMinor = 7_000, category = Category.HEALTH, timestamp = march.startInclusive),
                // поза діапазоном: останній момент лютого та перша мить квітня
                expense(amountMinor = 9_999, category = Category.FOOD, timestamp = march.startInclusive - 1),
                expense(amountMinor = 8_888, category = Category.FOOD, timestamp = march.endExclusive),
            ),
        )

        val totals = repo.observeCategoryTotals(march).first()

        assertEquals(3, totals.size)
        val foodUah = totals.single { it.category == Category.FOOD && it.currency == Currency.UAH }
        assertEquals(12_550L, foodUah.totalMinor)
        assertEquals(2, foodUah.count)
        assertEquals(500L, totals.single { it.category == Category.FOOD && it.currency == Currency.USD }.totalMinor)
        assertEquals(7_000L, totals.single { it.category == Category.HEALTH }.totalMinor)
    }

    @Test
    fun `flows are reactive - a new expense is re-emitted`() = runTest {
        val repo = InMemoryExpenseRepository()
        repo.observeRecent(10).test {
            assertEquals(0, awaitItem().size)
            repo.add(expense())
            assertEquals(1, awaitItem().size)
            repo.add(expense())
            assertEquals(2, awaitItem().size)
        }
    }

    @Test
    fun `delete and clear`() = runTest {
        val repo = InMemoryExpenseRepository()
        val id = repo.add(expense(note = "a"))
        repo.add(expense(note = "b"))
        repo.delete(id)
        assertEquals(listOf("b"), repo.observeHistory(null).first().map { it.note })
        repo.delete(id) // повторне видалення не падає
        repo.clear()
        assertTrue(repo.observeHistory(null).first().isEmpty())
    }
}
