package com.example.expensetracker.ui.overview

import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.model.MoneyAmount

data class OverviewUiState(
    val isLoading: Boolean = true,
    /** Витрачено цього місяця, окремо за кожною валютою (зведення в одну валюту — Lab 4). */
    val monthSpent: List<MoneyAmount> = emptyList(),
    val recent: List<Expense> = emptyList(),
)
