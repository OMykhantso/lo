package com.example.expensetracker.ui.analytics

import com.example.expensetracker.domain.model.CategoryTotal

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    /** Підсумки поточного місяця за парами «категорія + валюта» (зведення в одну валюту — Lab 4). */
    val rows: List<CategoryTotal> = emptyList(),
)
