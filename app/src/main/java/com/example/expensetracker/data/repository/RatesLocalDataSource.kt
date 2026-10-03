package com.example.expensetracker.data.repository

import com.example.expensetracker.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/** Локальний кеш курсів (реалізація — Room). */
interface RatesLocalDataSource {
    fun observeRates(): Flow<List<ExchangeRate>>

    /** Додає або оновлює курси (за ключем — валюта). */
    suspend fun upsert(rates: List<ExchangeRate>)
}
