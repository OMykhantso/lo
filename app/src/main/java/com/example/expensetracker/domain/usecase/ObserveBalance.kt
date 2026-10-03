package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Залишок місяця разом із контекстом, з якого його пораховано. */
data class BalanceState(
    val summary: BalanceSummary,
    /** Валюта, яку обрав користувач (може відрізнятись від `summary.currency`, якщо для неї немає курсу). */
    val requestedCurrency: Currency,
    val rates: RatesSnapshot,
)

/**
 * Реактивний баланс: перераховується при кожній зміні витрат, бюджету, обраної валюти або кешу курсів.
 * Працює офлайн — використовує останні збережені курси.
 */
class ObserveBalance(
    private val expenses: ExpenseRepository,
    private val settings: SettingsRepository,
    private val rates: RatesRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<BalanceState> = combine(
        ObserveMonthTotals(expenses, clock)(),
        settings.budgetLimitMinor,
        settings.displayCurrency,
        rates.rates,
    ) { totals, budget, requested, snapshot ->
        BalanceState(
            summary = BalanceCalculator.calculate(totals, budget, snapshot.table, requested),
            requestedCurrency = requested,
            rates = snapshot,
        )
    }
}
