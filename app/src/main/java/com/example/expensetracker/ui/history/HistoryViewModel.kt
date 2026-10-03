package com.example.expensetracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @param initialCategory значення з `ExpenseHistoryRoute.category`; невідомий ключ трактується як «усі».
 */
class HistoryViewModel(
    private val repository: ExpenseRepository,
    initialCategory: Category?,
) : ViewModel() {
    private val selected = MutableStateFlow(initialCategory)
    private val pendingDelete = MutableStateFlow<Expense?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryUiState> = combine(
        selected,
        selected.flatMapLatest { repository.observeHistory(it) },
        pendingDelete,
    ) { category, items, pending ->
        HistoryUiState(isLoading = false, selectedCategory = category, items = items, pendingDelete = pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState(selectedCategory = initialCategory))

    fun onCategorySelected(category: Category?) = selected.update { category }

    fun onDeleteRequested(expense: Expense) = pendingDelete.update { expense }

    fun onDeleteDismissed() = pendingDelete.update { null }

    fun onDeleteConfirmed() {
        val expense = pendingDelete.value ?: return
        pendingDelete.update { null }
        viewModelScope.launch { repository.delete(expense.id) }
    }
}
