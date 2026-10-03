package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.RateTable

/**
 * Як нова витрата вплине на бюджет (підказка у формі додавання).
 *
 * @property remainingAfterMinor залишок після витрати у валюті [currency]; від’ємний — бюджет буде перевищено
 */
data class BudgetImpact(
    val currency: Currency,
    val remainingAfterMinor: Long,
    val status: BudgetStatus,
)

object BudgetImpactCalculator {
    /**
     * @return `null`, якщо ліміт не задано або витрату неможливо перерахувати (немає курсу).
     */
    fun evaluate(
        newAmountMinor: Long,
        newCurrency: Currency,
        summary: BalanceSummary,
        table: RateTable,
    ): BudgetImpact? {
        val budget = summary.budgetMinor ?: return null
        val converted = table.convert(newAmountMinor, newCurrency, summary.currency) ?: return null
        val spentAfter = summary.spentMinor + converted
        return BudgetImpact(
            currency = summary.currency,
            remainingAfterMinor = budget - spentAfter,
            status = BalanceCalculator.statusOf(spentAfter, budget),
        )
    }
}
