package com.example.expensetracker.ui.history

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Expense

data class HistoryUiState(
    val isLoading: Boolean = true,
    /** `null` — усі категорії. */
    val selectedCategory: Category? = null,
    val items: List<Expense> = emptyList(),
    /** Витрата, видалення якої очікує підтвердження. */
    val pendingDelete: Expense? = null,
)
