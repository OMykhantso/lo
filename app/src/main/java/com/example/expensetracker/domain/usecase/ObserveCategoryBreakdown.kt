package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BreakdownState(
    val breakdown: CategoryBreakdown,
    val requestedCurrency: Currency,
    val rates: RatesSnapshot,
)

/** Розподіл витрат поточного місяця за категоріями у валюті відображення (для діаграми). */
class ObserveCategoryBreakdown(
    private val expenses: ExpenseRepository,
    private val settings: SettingsRepository,
    private val rates: RatesRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<BreakdownState> = combine(
        ObserveMonthTotals(expenses, clock)(),
        settings.displayCurrency,
        rates.rates,
    ) { totals, requested, snapshot ->
        BreakdownState(
            breakdown = CategoryBreakdownCalculator.calculate(totals, snapshot.table, requested),
            requestedCurrency = requested,
            rates = snapshot,
        )
    }
}
