package com.example.expensetracker.ui.analytics

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.usecase.CategoryBreakdown
import com.example.expensetracker.domain.usecase.SyncState

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val breakdown: CategoryBreakdown? = null,
    val requestedCurrency: Currency = Currency.UAH,
    val rates: RatesSnapshot = RatesSnapshot(),
    val sync: SyncState = SyncState.Idle,
)
