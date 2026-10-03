package com.example.expensetracker.domain.validation

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.money.AmountParseResult
import com.example.expensetracker.domain.money.AmountParser

enum class CategoryError { NOT_SELECTED }

enum class NoteError { TOO_LONG }

/** Чернетка витрати, як її бачить форма. */
data class ExpenseDraft(
    val amountText: String,
    val category: Category?,
    val note: String = "",
)

data class ExpenseValidation(
    val amountMinor: Long?,
    val amountError: AmountError?,
    val categoryError: CategoryError?,
    val noteError: NoteError?,
) {
    val isValid: Boolean
        get() = amountMinor != null && amountError == null && categoryError == null && noteError == null
}

/** Базова валідація форми додавання витрати: сума > 0, обрана категорія, нотатка ≤ [MAX_NOTE_LENGTH]. */
object ExpenseValidator {
    const val MAX_NOTE_LENGTH = 120

    fun validate(draft: ExpenseDraft): ExpenseValidation {
        val amount = AmountParser.parse(draft.amountText)
        return ExpenseValidation(
            amountMinor = (amount as? AmountParseResult.Valid)?.minor,
            amountError = (amount as? AmountParseResult.Invalid)?.error,
            categoryError = if (draft.category == null) CategoryError.NOT_SELECTED else null,
            noteError = if (draft.note.trim().length > MAX_NOTE_LENGTH) NoteError.TOO_LONG else null,
        )
    }
}
