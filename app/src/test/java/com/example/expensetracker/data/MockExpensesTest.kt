package com.example.expensetracker.data

import com.example.expensetracker.data.mock.MockExpenses
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.AmountParser
import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.domain.validation.ExpenseValidator
import com.example.expensetracker.testutil.TestData
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockExpensesTest {
    private val data = MockExpenses.generate(TestData.NOW, TestData.KYIV)

    @Test
    fun `every mock expense is valid`() {
        assertTrue(data.isNotEmpty())
        data.forEach {
            assertTrue("amount of $it", it.amountMinor in 1..AmountParser.MAX_MINOR)
            assertTrue("note of $it", it.note.length <= ExpenseValidator.MAX_NOTE_LENGTH)
            assertTrue("timestamp of $it is in the future", it.timestamp <= TestData.NOW.toEpochMilli())
        }
    }

    @Test
    fun `covers all categories and every currency`() {
        assertEquals(Category.entries.toSet(), data.map { it.category }.toSet())
        assertEquals(Currency.entries.toSet(), data.map { it.currency }.toSet())
    }

    @Test
    fun `spans current and previous month`() {
        val thisMonth = TimeRange.currentMonth(TestData.clock)
        val prevMonth = TimeRange.monthOf(Instant.ofEpochMilli(thisMonth.startInclusive - 1), TestData.KYIV)
        assertTrue(data.any { it.timestamp in thisMonth })
        assertTrue(data.any { it.timestamp in prevMonth })
    }

    @Test
    fun `is deterministic`() {
        assertEquals(data, MockExpenses.generate(TestData.NOW, TestData.KYIV))
    }

    @Test
    fun `drops entries that would fall later today than now`() {
        val earlyMorning = Instant.parse("2025-03-15T04:00:00Z") // 06:00 за Києвом
        val early = MockExpenses.generate(earlyMorning, TestData.KYIV)
        assertTrue(early.all { it.timestamp <= earlyMorning.toEpochMilli() })
        assertTrue(early.size < data.size)
        assertFalse(early.isEmpty())
    }
}
