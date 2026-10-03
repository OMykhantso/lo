package com.example.expensetracker.ui.add

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.validation.ExpenseDraft
import com.example.expensetracker.domain.validation.ExpenseValidation
import com.example.expensetracker.domain.validation.ExpenseValidator

data class AddExpenseUiState(
    val amountText: String = "",
    val currency: Currency = Currency.UAH,
    val category: Category? = null,
    val note: String = "",
    /** Помилки показуємо лише після першої спроби збереження, далі — вживу. */
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
) {
    val validation: ExpenseValidation
        get() = ExpenseValidator.validate(ExpenseDraft(amountText, category, note))
}

sealed interface AddExpenseEvent {
    data object Saved : AddExpenseEvent
}
