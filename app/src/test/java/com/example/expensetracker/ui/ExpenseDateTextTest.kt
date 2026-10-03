package com.example.expensetracker.ui

import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.ui.util.ExpenseDateText
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseDateTextTest {
    private val clock = TestData.clock // 2025-03-15 12:00 Київ

    private fun at(text: String) = LocalDateTime.parse(text).atZone(TestData.KYIV).toInstant().toEpochMilli()

    @Test
    fun `today and yesterday are relative with time`() {
        assertEquals("Сьогодні, 09:30", ExpenseDateText.format(at("2025-03-15T09:30:00"), clock))
        assertEquals("Вчора, 23:59", ExpenseDateText.format(at("2025-03-14T23:59:00"), clock))
    }

    @Test
    fun `midnight boundary`() {
        assertEquals("Сьогодні, 00:00", ExpenseDateText.format(at("2025-03-15T00:00:00"), clock))
        assertEquals("Вчора, 23:59", ExpenseDateText.format(at("2025-03-14T23:59:59"), clock))
    }

    @Test
    fun `older dates in the same year use day and month name`() {
        assertEquals("13 березня", ExpenseDateText.format(at("2025-03-13T10:00:00"), clock))
        assertEquals("2 січня", ExpenseDateText.format(at("2025-01-02T10:00:00"), clock))
    }

    @Test
    fun `dates from another year include the year`() {
        assertEquals("5 грудня 2024", ExpenseDateText.format(at("2024-12-05T10:00:00"), clock))
    }

    @Test
    fun `month title is capitalised`() {
        assertEquals("Березень 2025", ExpenseDateText.monthTitle(clock))
    }
}
