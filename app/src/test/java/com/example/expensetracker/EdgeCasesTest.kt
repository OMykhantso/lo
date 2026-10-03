package com.example.expensetracker

import app.cash.turbine.test
import com.example.expensetracker.data.remote.RatesRemoteDataSource
import com.example.expensetracker.data.remote.RemoteRate
import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.data.repository.InMemorySettingsRepository
import com.example.expensetracker.data.repository.OfflineFirstRatesRepository
import com.example.expensetracker.data.repository.RatesLocalDataSource
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.ExchangeRate
import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.money.AmountParseResult
import com.example.expensetracker.domain.money.AmountParser
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.money.RateTable
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.repository.RefreshResult
import com.example.expensetracker.domain.usecase.BalanceCalculator
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.domain.usecase.ObserveBalance
import com.example.expensetracker.domain.validation.BudgetValidation
import com.example.expensetracker.domain.validation.BudgetValidator
import com.example.expensetracker.domain.validation.ExpenseDraft
import com.example.expensetracker.domain.validation.ExpenseValidator
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.testutil.TestData.expense
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.UnknownHostException
import java.time.LocalDate
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lab 6, AI-завдання: граничні випадки наскрізь через шари.
 * Розділи: 1) від’ємні й нульові суми, 2) нульовий/некоректний курс, 3) відсутність інтернету, 4) fuzz.
 */
class EdgeCasesTest {

    // ---- 1. Від’ємні й нульові суми -------------------------------------------------------------

    @Test
    fun `a negative or zero amount can never become an Expense`() {
        listOf(0L, -1L, -100L, Long.MIN_VALUE).forEach { amount ->
            assertThrows("amount=$amount", IllegalArgumentException::class.java) { expense(amountMinor = amount) }
        }
        assertNotNull(expense(amountMinor = 1))
    }

    @Test
    fun `negative input is rejected by the form validator with the dedicated error`() {
        listOf("-1", "-0,01", "-1000000", "-0", " - 5").forEach { text ->
            val result = ExpenseValidator.validate(ExpenseDraft(text, Category.FOOD))
            assertTrue("«$text» must not be valid", !result.isValid)
            assertNull(result.amountMinor)
            assertEquals("«$text»", AmountError.NEGATIVE, result.amountError)
        }
    }

    @Test
    fun `a negative budget limit is rejected and clearing is the only way to remove it`() {
        assertEquals(BudgetValidation.Invalid(AmountError.NEGATIVE), BudgetValidator.validate("-5000"))
        assertEquals(BudgetValidation.Invalid(AmountError.ZERO), BudgetValidator.validate("0,00"))
        assertEquals(BudgetValidation.Cleared, BudgetValidator.validate(""))
    }

    @Test
    fun `extreme amounts neither overflow nor lose precision`() {
        assertEquals(AmountParseResult.Valid(AmountParser.MAX_MINOR), AmountParser.parse("999999999.99"))
        assertEquals(AmountParseResult.Invalid(AmountError.TOO_MANY_DECIMALS), AmountParser.parse("999999999.999"))
        assertEquals(AmountParseResult.Invalid(AmountError.TOO_LARGE), AmountParser.parse("1000000000"))
        assertEquals(AmountParseResult.Invalid(AmountError.TOO_LARGE), AmountParser.parse("9".repeat(40)))
        assertEquals("999 999 999,99".replace(' ', '\u00A0'), MoneyFormatter.formatNumber(AmountParser.MAX_MINOR))
    }

    // ---- 2. Нульовий / некоректний курс ----------------------------------------------------------

    @Test
    fun `a zero or negative rate cannot enter the conversion table`() {
        assertThrows(IllegalArgumentException::class.java) { RateTable(mapOf(Currency.USD to BigDecimal.ZERO)) }
        assertThrows(IllegalArgumentException::class.java) { RateTable(mapOf(Currency.USD to BigDecimal("-0.0001"))) }
        assertThrows(IllegalArgumentException::class.java) { ExchangeRate(Currency.USD, BigDecimal.ZERO, LocalDate.now(), 0) }
    }

    @Test
    fun `without a rate there is no number - never a division by zero or an Infinity`() {
        assertNull(RateTable.EMPTY.convert(100, Currency.USD, Currency.UAH))
        assertNull(RateTable.EMPTY.convert(100, Currency.UAH, Currency.EUR))
    }

    @Test
    fun `a misbehaving remote returning a zero rate cannot poison the cache`() = runTest {
        val local = InMemoryRates(listOf(ExchangeRate(Currency.USD, BigDecimal("40"), LocalDate.of(2025, 3, 14), 1)))
        val remote = FakeRemote { listOf(RemoteRate(Currency.USD, BigDecimal.ZERO, LocalDate.of(2025, 3, 15))) }

        val result = OfflineFirstRatesRepository(local, remote, TestData.clock).refresh()

        assertEquals(RefreshResult.Failure(RefreshFailure.INVALID_DATA), result)
        assertEquals(BigDecimal("40"), local.stored.value.single().rateToUah) // стара коректна ціна збережена
    }

    @Test
    fun `a bad rate next to a good one only drops the bad one`() = runTest {
        val local = InMemoryRates()
        val remote = FakeRemote {
            listOf(
                RemoteRate(Currency.USD, BigDecimal("-1"), LocalDate.of(2025, 3, 15)),
                RemoteRate(Currency.EUR, BigDecimal("48"), LocalDate.of(2025, 3, 15)),
            )
        }
        assertEquals(RefreshResult.Success(1), OfflineFirstRatesRepository(local, remote, TestData.clock).refresh())
        assertEquals(listOf(Currency.EUR), local.stored.value.map { it.currency })
    }

    @Test
    fun `a huge rate times a huge amount reports no result instead of wrapping around`() {
        val table = RateTable(mapOf(Currency.USD to BigDecimal("99999")))
        assertNull(table.convert(Long.MAX_VALUE / 2, Currency.USD, Currency.UAH))
    }

    // ---- 3. Відсутність інтернету ----------------------------------------------------------------

    @Test
    fun `offline first launch - hryvnia works, foreign expenses are flagged instead of guessed`() {
        val s = BalanceCalculator.calculate(
            listOf(
                com.example.expensetracker.domain.model.CategoryTotal(Category.FOOD, Currency.UAH, 50_000, 3),
                com.example.expensetracker.domain.model.CategoryTotal(Category.FOOD, Currency.USD, 999, 1),
            ),
            budgetUahMinor = 100_000,
            table = RateTable.EMPTY,
            displayCurrency = Currency.UAH,
        )
        assertEquals(50_000L, s.spentMinor)
        assertEquals(1, s.unconvertibleCount)
        assertEquals(BudgetStatus.OK, s.status)
    }

    @Test
    fun `offline with a warm cache - the balance is still recalculated in another currency`() = runTest {
        val cache = InMemoryRates(listOf(ExchangeRate(Currency.USD, BigDecimal("40"), LocalDate.of(2025, 3, 14), TestData.NOW.toEpochMilli())))
        val offline = FakeRemote { throw UnknownHostException("bank.gov.ua") }
        val rates = OfflineFirstRatesRepository(cache, offline, TestData.clock)
        val expenses = InMemoryExpenseRepository(listOf(expense(amountMinor = 40_000, timestamp = TestData.NOW.toEpochMilli())))
        val settings = InMemorySettingsRepository(budgetLimitMinor = 100_000, displayCurrency = Currency.USD)

        // мережа недоступна…
        assertEquals(RefreshResult.Failure(RefreshFailure.NO_NETWORK), rates.refresh())

        // …але баланс у доларах рахується з кешу
        val balance = ObserveBalance(expenses, settings, rates, TestData.clock)().first().summary
        assertEquals(Currency.USD, balance.currency)
        assertEquals(1_000L, balance.spentMinor)
        assertEquals(1_500L, balance.remainingMinor)
    }

    @Test
    fun `network errors of every kind are soft failures`() = runTest {
        val errors = listOf(IOException("reset"), UnknownHostException(), java.net.SocketTimeoutException(), javax.net.ssl.SSLException("handshake"))
        errors.forEach { error ->
            val repo = OfflineFirstRatesRepository(InMemoryRates(), FakeRemote { throw error }, TestData.clock)
            assertEquals(error::class.simpleName, RefreshResult.Failure(RefreshFailure.NO_NETWORK), repo.refresh())
        }
    }

    @Test
    fun `the rates flow keeps emitting the cache while refresh keeps failing`() = runTest {
        val cache = InMemoryRates(listOf(ExchangeRate(Currency.EUR, BigDecimal("48"), LocalDate.of(2025, 3, 14), 1)))
        val repo = OfflineFirstRatesRepository(cache, FakeRemote { throw IOException() }, TestData.clock)
        repo.rates.test {
            assertEquals(1, awaitItem().rates.size)
            repeat(3) { repo.refresh() }
            expectNoEvents()
        }
    }

    // ---- 4. Fuzz (детермінований: фіксований seed) -----------------------------------------------

    @Test
    fun `the amount parser never throws and only accepts canonical numbers`() {
        val random = Random(2025)
        val alphabet = "0123456789.,- +eE abcXYZ٣ ₴$"
        repeat(20_000) {
            val text = buildString { repeat(random.nextInt(0, 16)) { append(alphabet[random.nextInt(alphabet.length)]) } }
            val result = AmountParser.parse(text) // не має кидати
            if (result is AmountParseResult.Valid) {
                assertTrue("«$text» -> ${result.minor}", result.minor in 1..AmountParser.MAX_MINOR)
                // успішно розібране число має відтворюватись форматером без втрат
                val again = AmountParser.parse(MoneyFormatter.formatPlain(result.minor))
                assertEquals("«$text»", result, again)
            }
        }
    }

    @Test
    fun `plain formatting round trips for random amounts`() {
        val random = Random(7)
        repeat(20_000) {
            val minor = random.nextLong(1, AmountParser.MAX_MINOR + 1)
            assertEquals(AmountParseResult.Valid(minor), AmountParser.parse(MoneyFormatter.formatPlain(minor)))
            // форматований із групуванням текст теж розбирається (пробіли ігноруються)
            assertEquals(AmountParseResult.Valid(minor), AmountParser.parse(MoneyFormatter.formatNumber(minor)))
        }
    }

    @Test
    fun `conversion matches an independent BigDecimal reference for random inputs`() {
        val random = Random(99)
        repeat(20_000) {
            val minor = random.nextLong(1, 1_000_000_000_00L)
            val rate = BigDecimal(random.nextDouble(0.01, 1_000.0)).setScale(4, RoundingMode.HALF_UP)
            if (rate.signum() == 0) return@repeat
            val table = RateTable(mapOf(Currency.USD to rate))

            val expected = BigDecimal(minor).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact()
            assertEquals("$minor × $rate", expected, table.convert(minor, Currency.USD, Currency.UAH))

            // туди-й-назад втрачає не більше однієї мінімальної одиниці «з боку більшої валюти»
            val there = table.convert(minor, Currency.UAH, Currency.USD)!!
            val back = table.convert(there, Currency.USD, Currency.UAH)!!
            val tolerance = rate.setScale(0, RoundingMode.CEILING).toLong() + 1
            assertTrue("$minor via $rate: back=$back", kotlin.math.abs(back - minor) <= tolerance)
        }
    }

    // ---- допоміжні фейки -------------------------------------------------------------------------

    private class InMemoryRates(initial: List<ExchangeRate> = emptyList()) : RatesLocalDataSource {
        val stored = MutableStateFlow(initial)
        override fun observeRates(): Flow<List<ExchangeRate>> = stored
        override suspend fun upsert(rates: List<ExchangeRate>) {
            stored.value = stored.value.filterNot { old -> rates.any { it.currency == old.currency } } + rates
        }
    }

    private class FakeRemote(private val block: () -> List<RemoteRate>) : RatesRemoteDataSource {
        override suspend fun fetchRates(): List<RemoteRate> = block()
    }
}
