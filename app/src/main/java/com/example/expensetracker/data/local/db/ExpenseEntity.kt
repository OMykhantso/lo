package com.example.expensetracker.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Таблиця `expenses` (id, amount, category, timestamp, note) + `currency`.
 *
 * `amount` — сума в мінімальних одиницях валюти (копійки/центи), `timestamp` — epoch-мс (UTC),
 * `category`/`currency` — стабільні ключі enum-ів домену.
 */
@Entity(
    tableName = SqlQueries.TABLE,
    indices = [
        // Сортування за часом: ORDER BY timestamp DESC, id DESC LIMIT n.
        Index(value = ["timestamp"], name = "index_expenses_timestamp"),
        // Покривний індекс для агрегації за період (див. SqlQueries.CATEGORY_TOTALS).
        Index(value = ["timestamp", "category", "currency", "amount"], name = "index_expenses_aggregation"),
        // Історія однієї категорії: WHERE category = ? ORDER BY timestamp DESC.
        Index(value = ["category", "timestamp"], name = "index_expenses_category_timestamp"),
    ],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val currency: String,
    val category: String,
    val timestamp: Long,
    val note: String,
)
