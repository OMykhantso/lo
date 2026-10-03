package com.example.expensetracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.money.AmountInput
import com.example.expensetracker.domain.repository.ExpenseRepository
import java.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val repository: ExpenseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _state = MutableStateFlow(AddExpenseUiState())
    val state: StateFlow<AddExpenseUiState> = _state.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events: Flow<AddExpenseEvent> = _events.receiveAsFlow()

    fun onAmountChange(text: String) = _state.update { it.copy(amountText = AmountInput.sanitize(text)) }

    fun onCurrencyChange(currency: Currency) = _state.update { it.copy(currency = currency) }

    /** Повторний тап по вибраній категорії знімає вибір (поведінка `FilterChip`). */
    fun onCategorySelect(category: Category) =
        _state.update { it.copy(category = if (it.category == category) null else category) }

    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun onSave() {
        val current = _state.value
        if (current.isSaving) return

        val validation = current.validation
        val amountMinor = validation.amountMinor
        val category = current.category
        if (!validation.isValid || amountMinor == null || category == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.add(
                    Expense(
                        amountMinor = amountMinor,
                        currency = current.currency,
                        category = category,
                        timestamp = clock.millis(),
                        note = current.note.trim(),
                    ),
                )
                // Форма скидається, але вибрана валюта лишається — зручно вносити кілька витрат поспіль.
                _state.value = AddExpenseUiState(currency = current.currency)
                _events.send(AddExpenseEvent.Saved)
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
