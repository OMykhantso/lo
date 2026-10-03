package com.example.expensetracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.usecase.ObserveCategoryBreakdown
import com.example.expensetracker.domain.usecase.RatesSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val settings: SettingsRepository,
    private val ratesSync: RatesSync,
    observeBreakdown: ObserveCategoryBreakdown,
) : ViewModel() {

    val state: StateFlow<AnalyticsUiState> = combine(observeBreakdown(), ratesSync.state) { breakdown, sync ->
        AnalyticsUiState(
            isLoading = false,
            breakdown = breakdown.breakdown,
            requestedCurrency = breakdown.requestedCurrency,
            rates = breakdown.rates,
            sync = sync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun onCurrencySelected(currency: Currency) {
        viewModelScope.launch { settings.setDisplayCurrency(currency) }
    }

    fun onRefreshRates() = ratesSync.refresh(force = true)
}
