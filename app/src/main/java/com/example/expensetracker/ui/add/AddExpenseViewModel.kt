package com.example.expensetracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.money.AmountInput
import com.example.expensetracker.domain.money.AmountParseResult
import com.example.expensetracker.domain.money.AmountParser
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.usecase.BalanceState
import com.example.expensetracker.domain.usecase.BudgetImpact
import com.example.expensetracker.domain.usecase.BudgetImpactCalculator
import java.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @param balance поточний баланс місяця — з нього форма показує, чи не перевищить нова витрата бюджет.
 */
class AddExpenseViewModel(
    private val repository: ExpenseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    balance: Flow<BalanceState> = emptyFlow(),
) : ViewModel() {

    private val form = MutableStateFlow(AddExpenseUiState())
    private val currentBalance = MutableStateFlow<BalanceState?>(null)

    /** Стан форми + оцінка впливу на бюджет (перераховується при зміні суми, валюти, витрат чи курсів). */
    val state: StateFlow<AddExpenseUiState> = combine(form, currentBalance) { form, balance ->
        form.copy(budgetImpact = budgetImpact(form, balance))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, form.value)

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events: Flow<AddExpenseEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { balance.collect { currentBalance.value = it } }
    }

    fun onAmountChange(text: String) = form.update { it.copy(amountText = AmountInput.sanitize(text)) }

    fun onCurrencyChange(currency: Currency) = form.update { it.copy(currency = currency) }

    /** Повторний тап по вибраній категорії знімає вибір (поведінка `FilterChip`). */
    fun onCategorySelect(category: Category) =
        form.update { it.copy(category = if (it.category == category) null else category) }

    fun onNoteChange(note: String) = form.update { it.copy(note = note) }

    fun onSave() {
        val current = form.value
        if (current.isSaving) return

        val validation = current.validation
        val amountMinor = validation.amountMinor
        val category = current.category
        if (!validation.isValid || amountMinor == null || category == null) {
            form.update { it.copy(showErrors = true) }
            return
        }

        form.update { it.copy(isSaving = true) }
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
                form.value = AddExpenseUiState(currency = current.currency)
                _events.send(AddExpenseEvent.Saved)
            } finally {
                form.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun budgetImpact(form: AddExpenseUiState, balance: BalanceState?): BudgetImpact? {
        balance ?: return null
        val amount = (AmountParser.parse(form.amountText) as? AmountParseResult.Valid)?.minor ?: return null
        return BudgetImpactCalculator.evaluate(amount, form.currency, balance.summary, balance.rates.table)
    }
}
