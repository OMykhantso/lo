package com.example.expensetracker.domain.model

/** Результат агрегації `SUM(amount) GROUP BY category, currency`. */
data class CategoryTotal(
    val category: Category,
    val currency: Currency,
    val totalMinor: Long,
    val count: Int,
)
