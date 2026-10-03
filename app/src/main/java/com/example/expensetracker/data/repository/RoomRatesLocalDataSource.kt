package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.db.RatesDao
import com.example.expensetracker.data.local.db.toDomainOrNull
import com.example.expensetracker.data.local.db.toEntity
import com.example.expensetracker.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRatesLocalDataSource(private val dao: RatesDao) : RatesLocalDataSource {
    override fun observeRates(): Flow<List<ExchangeRate>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun upsert(rates: List<ExchangeRate>) = dao.upsertAll(rates.map { it.toEntity() })
}
