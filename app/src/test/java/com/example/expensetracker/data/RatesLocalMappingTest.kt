package com.example.expensetracker.data

import com.example.expensetracker.data.local.db.ExchangeRateEntity
import com.example.expensetracker.data.local.db.RatesDao
import com.example.expensetracker.data.local.db.toDomainOrNull
import com.example.expensetracker.data.local.db.toEntity
import com.example.expensetracker.data.repository.RoomRatesLocalDataSource
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.ExchangeRate
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatesLocalMappingTest {
    private val rate = ExchangeRate(Currency.USD, BigDecimal("41.1783"), LocalDate.of(2025, 10, 3), 123L)

    @Test
    fun `entity round trip keeps precision`() {
        val entity = rate.toEntity()
        assertEquals(ExchangeRateEntity("USD", "41.1783", "2025-10-03", 123L), entity)
        assertEquals(rate, entity.toDomainOrNull())
    }

    @Test
    fun `corrupted cache rows are ignored instead of crashing`() {
        assertNull(ExchangeRateEntity("USD", "0", "2025-10-03", 1).toDomainOrNull())
        assertNull(ExchangeRateEntity("USD", "-3", "2025-10-03", 1).toDomainOrNull())
        assertNull(ExchangeRateEntity("USD", "abc", "2025-10-03", 1).toDomainOrNull())
        assertNull(ExchangeRateEntity("USD", "41", "03.10.2025", 1).toDomainOrNull())
        assertNull(ExchangeRateEntity("UAH", "1", "2025-10-03", 1).toDomainOrNull())
        assertNull(ExchangeRateEntity("XXX", "1", "2025-10-03", 1).toDomainOrNull())
    }

    @Test
    fun `data source filters bad rows and writes entities`() = runTest {
        val dao = mockk<RatesDao>(relaxed = true)
        every { dao.observeAll() } returns flowOf(listOf(rate.toEntity(), ExchangeRateEntity("EUR", "0", "2025-10-03", 1)))
        val source = RoomRatesLocalDataSource(dao)

        assertEquals(listOf(rate), source.observeRates().first())

        val saved = slot<List<ExchangeRateEntity>>()
        io.mockk.coEvery { dao.upsertAll(capture(saved)) } returns Unit
        source.upsert(listOf(rate))
        coVerify { dao.upsertAll(any()) }
        assertEquals("41.1783", saved.captured.single().rateToUah)
    }
}
