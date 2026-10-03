package com.example.expensetracker.domain.validation

import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.money.AmountParseResult
import com.example.expensetracker.domain.money.AmountParser

sealed interface BudgetValidation {
    /** Порожнє поле — користувач прибирає ліміт. */
    data object Cleared : BudgetValidation

    data class Valid(val limitMinor: Long) : BudgetValidation

    data class Invalid(val error: AmountError) : BudgetValidation
}

/** Валідація місячного ліміту бюджету (гривні): порожньо = «без ліміту», інакше сума > 0 за правилами [AmountParser]. */
object BudgetValidator {
    fun validate(text: String): BudgetValidation {
        if (text.isBlank()) return BudgetValidation.Cleared
        return when (val parsed = AmountParser.parse(text)) {
            is AmountParseResult.Valid -> BudgetValidation.Valid(parsed.minor)
            is AmountParseResult.Invalid -> BudgetValidation.Invalid(parsed.error)
        }
    }
}
