package com.example.expensetracker.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Кеш курсів НБУ. Курс зберігається **рядком** (`BigDecimal.toPlainString`), щоб не втратити точність
 * на `REAL`; `rateDate` — ISO-8601 (`2025-10-03`), `fetchedAt` — epoch-мс.
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currency: String,
    val rateToUah: String,
    val rateDate: String,
    val fetchedAt: Long,
)
