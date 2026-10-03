package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.time.TimeRange
import kotlinx.coroutines.flow.Flow

/** Єдине джерело правди про витрати (реалізація — Room; у тестах і прев’ю — in-memory). */
interface ExpenseRepository {
    /** Останні [limit] витрат, нові першими. */
    fun observeRecent(limit: Int): Flow<List<Expense>>

    /** Уся історія (нові першими), за потреби лише одна категорія. */
    fun observeHistory(category: Category?): Flow<List<Expense>>

    /** Сума витрат за [range], згрупована за категорією та валютою (`SUM(amount) GROUP BY`). */
    fun observeCategoryTotals(range: TimeRange): Flow<List<CategoryTotal>>

    suspend fun add(expense: Expense): Long

    suspend fun addAll(expenses: List<Expense>)

    suspend fun delete(id: Long)

    suspend fun clear()
}
