package com.example.expensetracker.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.MoneyAmount
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.usecase.ObserveMonthTotals
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class OverviewViewModel(
    repository: ExpenseRepository,
    clock: Clock,
) : ViewModel() {
    private val monthTotals = ObserveMonthTotals(repository, clock)

    val state: StateFlow<OverviewUiState> = combine(
        monthTotals(),
        repository.observeRecent(RECENT_LIMIT),
    ) { totals, recent ->
        OverviewUiState(
            isLoading = false,
            monthSpent = totals.groupBy { it.currency }
                .map { (currency, rows) -> MoneyAmount(rows.sumOf { it.totalMinor }, currency) }
                .sortedBy { it.currency },
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OverviewUiState())

    companion object {
        const val RECENT_LIMIT = 10
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
