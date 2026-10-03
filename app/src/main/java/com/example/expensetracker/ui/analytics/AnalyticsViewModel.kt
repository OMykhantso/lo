package com.example.expensetracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.usecase.ObserveMonthTotals
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(
    repository: ExpenseRepository,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<AnalyticsUiState> = ObserveMonthTotals(repository, clock)()
        .map { rows -> AnalyticsUiState(isLoading = false, rows = rows.sortedByDescending { it.totalMinor }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())
}
