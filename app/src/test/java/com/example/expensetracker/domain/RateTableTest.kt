package com.example.expensetracker.domain

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.RateTable
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RateTableTest {
    private fun table(usd: String? = "41.50", eur: String? = "48.00") = RateTable(
        buildMap {
            usd?.let { put(Currency.USD, BigDecimal(it)) }
            eur?.let { put(Currency.EUR, BigDecimal(it)) }
        },
    )

    @Test
    fun `converts foreign currency to hryvnia and back`() {
        val t = table()
        assertEquals(415_000L, t.convert(10_000, Currency.USD, Currency.UAH)) // $100 -> 4 150,00 грн
        assertEquals(10_000L, t.convert(415_000, Currency.UAH, Currency.USD))
    }

    @Test
    fun `converts between two foreign currencies through hryvnia`() {
        val t = RateTable(mapOf(Currency.USD to BigDecimal("40"), Currency.EUR to BigDecimal("50")))
        assertEquals(8_000L, t.convert(10_000, Currency.USD, Currency.EUR))
        assertEquals(12_500L, t.convert(10_000, Currency.EUR, Currency.USD))
    }

    @Test
    fun `same currency is returned untouched even without rates`() {
        assertEquals(12_345L, RateTable.EMPTY.convert(12_345, Currency.UAH, Currency.UAH))
        assertEquals(12_345L, RateTable.EMPTY.convert(12_345, Currency.USD, Currency.USD))
    }

    @Test
    fun `rounds half up to the minor unit`() {
        val t = RateTable(mapOf(Currency.USD to BigDecimal("10")))
        assertEquals(1L, t.convert(5, Currency.UAH, Currency.USD))  // 0,5 -> 1
        assertEquals(0L, t.convert(4, Currency.UAH, Currency.USD))  // 0,4 -> 0
        assertEquals(1L, t.convert(14, Currency.UAH, Currency.USD)) // 1,4 -> 1
        assertEquals(2L, t.convert(15, Currency.UAH, Currency.USD)) // 1,5 -> 2
    }

    @Test
    fun `negative amounts are rounded symmetrically`() {
        val t = RateTable(mapOf(Currency.USD to BigDecimal("10")))
        assertEquals(-1L, t.convert(-5, Currency.UAH, Currency.USD))
        assertEquals(-415_000L, table().convert(-10_000, Currency.USD, Currency.UAH))
    }

    @Test
    fun `zero converts to zero`() {
        assertEquals(0L, table().convert(0, Currency.USD, Currency.UAH))
    }

    @Test
    fun `missing rate yields null rather than a wrong number`() {
        val t = table(eur = null)
        assertNull(t.convert(100, Currency.EUR, Currency.UAH))
        assertNull(t.convert(100, Currency.UAH, Currency.EUR))
        assertNull(t.convert(100, Currency.EUR, Currency.USD))
        assertNull(RateTable.EMPTY.convert(100, Currency.USD, Currency.UAH))
    }

    @Test
    fun `zero or negative rates are rejected at construction`() {
        assertThrows(IllegalArgumentException::class.java) { RateTable(mapOf(Currency.USD to BigDecimal.ZERO)) }
        assertThrows(IllegalArgumentException::class.java) { RateTable(mapOf(Currency.USD to BigDecimal("-41.5"))) }
        assertThrows(IllegalArgumentException::class.java) { RateTable(mapOf(Currency.EUR to BigDecimal("0.00"))) }
    }

    @Test
    fun `result that does not fit into Long is reported as null`() {
        assertNull(table().convert(Long.MAX_VALUE, Currency.USD, Currency.UAH))
    }

    @Test
    fun `hryvnia is always supported and cannot be overridden`() {
        val t = RateTable(mapOf(Currency.UAH to BigDecimal("2"), Currency.USD to BigDecimal("40")))
        assertTrue(t.supports(Currency.UAH))
        assertEquals(4_000L, t.convert(100, Currency.USD, Currency.UAH))
        assertEquals(100L, t.convert(100, Currency.UAH, Currency.UAH))
    }

    @Test
    fun `display currency falls back to hryvnia without a rate`() {
        val t = table(eur = null)
        assertEquals(Currency.USD, t.resolveDisplayCurrency(Currency.USD))
        assertEquals(Currency.UAH, t.resolveDisplayCurrency(Currency.EUR))
        assertEquals(Currency.UAH, RateTable.EMPTY.resolveDisplayCurrency(Currency.USD))
        assertFalse(RateTable.EMPTY.supports(Currency.USD))
    }

    @Test
    fun `high precision rates keep their precision`() {
        val t = RateTable(mapOf(Currency.USD to BigDecimal("41.1783")))
        assertEquals(411_783L, t.convert(10_000, Currency.USD, Currency.UAH))
    }
}
