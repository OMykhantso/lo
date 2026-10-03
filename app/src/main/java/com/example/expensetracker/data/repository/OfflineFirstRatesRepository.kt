package com.example.expensetracker.data.repository

import com.example.expensetracker.data.remote.RatesFormatException
import com.example.expensetracker.data.remote.RatesRemoteDataSource
import com.example.expensetracker.data.remote.RatesServerException
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.ExchangeRate
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.repository.RefreshResult
import java.io.IOException
import java.time.Clock
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-First: читання — завжди з локальної БД (Single Source of Truth), мережа лише наповнює БД.
 * Якщо оновлення не вдалося, UI й далі працює зі збереженими курсами.
 */
class OfflineFirstRatesRepository(
    private val local: RatesLocalDataSource,
    private val remote: RatesRemoteDataSource,
    private val clock: Clock,
) : RatesRepository {

    override val rates: Flow<RatesSnapshot> =
        local.observeRates().map { list -> RatesSnapshot(list.associateBy { it.currency }) }

    override suspend fun refresh(): RefreshResult {
        val fetched = try {
            remote.fetchRates()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return RefreshResult.Failure(RefreshFailure.NO_NETWORK)
        } catch (e: RatesServerException) {
            return RefreshResult.Failure(RefreshFailure.SERVER_ERROR)
        } catch (e: RatesFormatException) {
            return RefreshResult.Failure(RefreshFailure.INVALID_DATA)
        }

        // Друга лінія захисту після NbuRateMapper: нульовий/від’ємний курс не має потрапити в кеш
        // (він «отруїв» би всі перерахунки), навіть якщо джерело помилилось.
        val usable = fetched.filter { it.currency != Currency.BASE && it.rate.signum() > 0 }
        if (usable.isEmpty()) return RefreshResult.Failure(RefreshFailure.INVALID_DATA)

        val now = clock.millis()
        local.upsert(usable.map { ExchangeRate(it.currency, it.rate, it.date, now) })
        return RefreshResult.Success(usable.size)
    }
}
