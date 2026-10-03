package com.example.expensetracker.data

import app.cash.turbine.test
import com.example.expensetracker.data.remote.RatesFormatException
import com.example.expensetracker.data.remote.RatesRemoteDataSource
import com.example.expensetracker.data.remote.RatesServerException
import com.example.expensetracker.data.remote.RemoteRate
import com.example.expensetracker.data.repository.OfflineFirstRatesRepository
import com.example.expensetracker.data.repository.RatesLocalDataSource
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.ExchangeRate
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.repository.RefreshResult
import com.example.expensetracker.testutil.TestData
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeRatesLocal(initial: List<ExchangeRate> = emptyList()) : RatesLocalDataSource {
    val stored = MutableStateFlow(initial)
    override fun observeRates(): Flow<List<ExchangeRate>> = stored
    override suspend fun upsert(rates: List<ExchangeRate>) {
        stored.value = (stored.value.filterNot { old -> rates.any { it.currency == old.currency } } + rates)
    }
}

class OfflineFirstRatesRepositoryTest {
    private val remote = mockk<RatesRemoteDataSource>()
    private val date = LocalDate.of(2025, 3, 14)

    private fun cached(currency: Currency, rate: String) = ExchangeRate(currency, BigDecimal(rate), date, fetchedAtMillis = 1_000)

    private fun repo(local: FakeRatesLocal = FakeRatesLocal()) = OfflineFirstRatesRepository(local, remote, TestData.clock)

    @Test
    fun `successful refresh writes to the cache and the flow re-emits`() = runTest {
        val local = FakeRatesLocal()
        coEvery { remote.fetchRates() } returns listOf(
            RemoteRate(Currency.USD, BigDecimal("41.5"), date), RemoteRate(Currency.EUR, BigDecimal("48"), date),
        )
        val repository = repo(local)

        repository.rates.test {
            assertTrue(awaitItem().isEmpty)
            assertEquals(RefreshResult.Success(2), repository.refresh())

            val snapshot = awaitItem()
            assertEquals(BigDecimal("41.5"), snapshot.rates.getValue(Currency.USD).rateToUah)
            assertEquals(TestData.NOW.toEpochMilli(), snapshot.rates.getValue(Currency.EUR).fetchedAtMillis)
        }
    }

    @Test
    fun `offline refresh fails softly and keeps the cached rates`() = runTest {
        val local = FakeRatesLocal(listOf(cached(Currency.USD, "40")))
        val repository = repo(local)

        listOf(IOException(), UnknownHostException("bank.gov.ua"), SocketTimeoutException()).forEach { error ->
            coEvery { remote.fetchRates() } throws error
            assertEquals(RefreshResult.Failure(RefreshFailure.NO_NETWORK), repository.refresh())
        }

        assertEquals(BigDecimal("40"), repository.rates.first().rates.getValue(Currency.USD).rateToUah)
    }

    @Test
    fun `server errors and bad payloads are classified`() = runTest {
        val repository = repo()
        coEvery { remote.fetchRates() } throws RatesServerException(500)
        assertEquals(RefreshResult.Failure(RefreshFailure.SERVER_ERROR), repository.refresh())

        coEvery { remote.fetchRates() } throws RatesFormatException()
        assertEquals(RefreshResult.Failure(RefreshFailure.INVALID_DATA), repository.refresh())
    }

    @Test
    fun `an answer without usable rates does not wipe the cache`() = runTest {
        val local = FakeRatesLocal(listOf(cached(Currency.USD, "40")))
        coEvery { remote.fetchRates() } returns emptyList()

        assertEquals(RefreshResult.Failure(RefreshFailure.INVALID_DATA), repo(local).refresh())
        assertEquals(1, local.stored.value.size)
    }

    @Test
    fun `refresh updates only the returned currencies`() = runTest {
        val local = FakeRatesLocal(listOf(cached(Currency.USD, "40"), cached(Currency.EUR, "47")))
        coEvery { remote.fetchRates() } returns listOf(RemoteRate(Currency.USD, BigDecimal("41"), date))

        repo(local).refresh()

        val rates = repo(local).rates.first().rates
        assertEquals(BigDecimal("41"), rates.getValue(Currency.USD).rateToUah)
        assertEquals(BigDecimal("47"), rates.getValue(Currency.EUR).rateToUah)
    }

    @Test
    fun `cancellation is propagated, not swallowed as a failure`() = runTest {
        coEvery { remote.fetchRates() } throws CancellationException("cancelled")
        assertThrows(CancellationException::class.java) { kotlinx.coroutines.runBlocking { repo().refresh() } }
    }

    @Test
    fun `snapshot exposes a conversion table built from the cache`() = runTest {
        val snapshot = repo(FakeRatesLocal(listOf(cached(Currency.USD, "40")))).rates.map { it.table }.first()
        assertEquals(4_000L, snapshot.convert(100, Currency.USD, Currency.UAH))
    }
}
