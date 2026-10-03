package com.example.expensetracker.domain

import com.example.expensetracker.domain.money.MoneyFormatter
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterExtrasTest {
    @Test
    fun `plain format for input fields`() {
        assertEquals("0", MoneyFormatter.formatPlain(0))
        assertEquals("30000", MoneyFormatter.formatPlain(3_000_000))
        assertEquals("30000.50", MoneyFormatter.formatPlain(3_000_050))
        assertEquals("0.05", MoneyFormatter.formatPlain(5))
        assertEquals("999999999.99", MoneyFormatter.formatPlain(99_999_999_999))
    }

    @Test
    fun `plain format round trips through the parser`() {
        listOf(1L, 99L, 100L, 12_345L, 3_000_050L).forEach { minor ->
            val text = MoneyFormatter.formatPlain(minor)
            assertEquals(minor, (com.example.expensetracker.domain.money.AmountParser.parse(text) as com.example.expensetracker.domain.money.AmountParseResult.Valid).minor)
        }
    }

    @Test
    fun `rate has two decimals with a comma and rounds half up`() {
        assertEquals("41,18", MoneyFormatter.formatRate(BigDecimal("41.1783")))
        assertEquals("41,50", MoneyFormatter.formatRate(BigDecimal("41.5")))
        assertEquals("48,01", MoneyFormatter.formatRate(BigDecimal("48.005")))
        assertEquals("40,00", MoneyFormatter.formatRate(BigDecimal("40")))
    }

    @Test
    fun `percent rounds to whole numbers`() {
        assertEquals("0%", MoneyFormatter.formatPercent(0f))
        assertEquals("55%", MoneyFormatter.formatPercent(0.55f))
        assertEquals("100%", MoneyFormatter.formatPercent(1f))
        assertEquals("113%", MoneyFormatter.formatPercent(1.13f))
    }
}
