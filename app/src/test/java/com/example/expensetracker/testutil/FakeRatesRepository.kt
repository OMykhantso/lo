package com.example.expensetracker.testutil

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.ExchangeRate
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.RefreshResult
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Фейк кешу курсів: тест змінює [snapshot] і керує результатом [refresh]. */
class FakeRatesRepository(initial: RatesSnapshot = RatesSnapshot()) : RatesRepository {
    val snapshot = MutableStateFlow(initial)
    var result: RefreshResult = RefreshResult.Success(2)
    var refreshCalls = 0
        private set

    override val rates: Flow<RatesSnapshot> = snapshot

    override suspend fun refresh(): RefreshResult {
        refreshCalls++
        return result
    }

    companion object {
        fun snapshotOf(
            vararg rates: Pair<Currency, String>,
            fetchedAt: Long = TestData.NOW.toEpochMilli(),
            date: LocalDate = LocalDate.of(2025, 3, 14),
        ) = RatesSnapshot(rates.associate { (currency, rate) -> currency to ExchangeRate(currency, BigDecimal(rate), date, fetchedAt) })
    }
}
