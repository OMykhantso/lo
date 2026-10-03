package com.example.expensetracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.usecase.ObserveBalance
import com.example.expensetracker.domain.usecase.RatesSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Єдине джерело стану екрана «Огляд» (ViewModel + StateFlow): баланс, останні витрати та стан курсів
 * зводяться `combine` у `OverviewUiState` і перераховуються реактивно при будь-якій зміні в БД чи налаштуваннях.
 */
class OverviewViewModel(
    repository: ExpenseRepository,
    private val settings: SettingsRepository,
    private val ratesSync: RatesSync,
    observeBalance: ObserveBalance,
) : ViewModel() {

    val state: StateFlow<OverviewUiState> = combine(
        observeBalance(),
        repository.observeRecent(RECENT_LIMIT),
        ratesSync.state,
    ) { balance, recent, sync ->
        OverviewUiState(
            isLoading = false,
            balance = balance.summary,
            requestedCurrency = balance.requestedCurrency,
            recent = recent,
            rates = balance.rates,
            sync = sync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OverviewUiState())

    fun onCurrencySelected(currency: Currency) {
        viewModelScope.launch { settings.setDisplayCurrency(currency) }
    }

    fun onRefreshRates() = ratesSync.refresh(force = true)

    companion object {
        const val RECENT_LIMIT = 10
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
