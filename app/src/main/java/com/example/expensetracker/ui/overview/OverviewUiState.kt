package com.example.expensetracker.ui.overview

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.usecase.BalanceSummary
import com.example.expensetracker.domain.usecase.SyncState

data class OverviewUiState(
    val isLoading: Boolean = true,
    val balance: BalanceSummary? = null,
    /** Валюта, яку обрав користувач (може відрізнятись від валюти `balance`, якщо немає курсу). */
    val requestedCurrency: Currency = Currency.UAH,
    val recent: List<Expense> = emptyList(),
    val rates: RatesSnapshot = RatesSnapshot(),
    val sync: SyncState = SyncState.Idle,
)
