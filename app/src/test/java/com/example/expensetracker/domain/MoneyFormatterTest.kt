package com.example.expensetracker.domain

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    private val nbsp = ' '

    @Test
    fun `formats with grouping, comma and symbol`() {
        assertEquals("0,00", MoneyFormatter.formatNumber(0))
        assertEquals("0,05", MoneyFormatter.formatNumber(5))
        assertEquals("12,50", MoneyFormatter.formatNumber(1_250))
        assertEquals("1${nbsp}234,56", MoneyFormatter.formatNumber(123_456))
        assertEquals("999${nbsp}999${nbsp}999,99", MoneyFormatter.formatNumber(99_999_999_999))
        assertEquals("1${nbsp}234,56${nbsp}₴", MoneyFormatter.format(123_456, Currency.UAH))
        assertEquals("9,99${nbsp}$", MoneyFormatter.format(999, Currency.USD))
    }

    @Test
    fun `negative values keep the sign`() {
        assertEquals("-1${nbsp}000,00", MoneyFormatter.formatNumber(-100_000))
        assertEquals("-0,01", MoneyFormatter.formatNumber(-1))
    }
}
