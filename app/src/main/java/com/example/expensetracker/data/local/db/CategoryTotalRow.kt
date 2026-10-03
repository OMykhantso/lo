package com.example.expensetracker.data.local.db

/** Рядок результату `SqlQueries.CATEGORY_TOTALS`; назви полів збігаються з аліасами колонок. */
data class CategoryTotalRow(
    val category: String,
    val currency: String,
    val total: Long,
    val count: Int,
)
