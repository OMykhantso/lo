package com.example.expensetracker.data.remote

import com.example.expensetracker.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate

/** Курс, отриманий із мережі й уже перевірений (валюта відома, курс > 0, дата розібрана). */
data class RemoteRate(val currency: Currency, val rate: BigDecimal, val date: LocalDate)

/**
 * Джерело курсів у мережі. Мережеві збої — `java.io.IOException`; відповідь сервера з помилкою —
 * [RatesServerException]; нерозбірлива відповідь — [RatesFormatException].
 */
interface RatesRemoteDataSource {
    suspend fun fetchRates(): List<RemoteRate>
}

class RatesServerException(val httpCode: Int) : Exception("Rates server answered HTTP $httpCode")

class RatesFormatException(cause: Throwable? = null) : Exception("Rates response could not be parsed", cause)
