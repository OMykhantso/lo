package com.example.expensetracker.data.repository

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Налаштування в пам’яті — для тестів і прев’ю. */
class InMemorySettingsRepository(
    budgetLimitMinor: Long? = null,
    displayCurrency: Currency = Currency.UAH,
) : SettingsRepository {
    private val budget = MutableStateFlow(budgetLimitMinor)
    private val currency = MutableStateFlow(displayCurrency)

    override val budgetLimitMinor: Flow<Long?> = budget
    override val displayCurrency: Flow<Currency> = currency

    override suspend fun setBudgetLimit(minorUah: Long?) {
        budget.value = minorUah
    }

    override suspend fun setDisplayCurrency(currency: Currency) {
        this.currency.value = currency
    }
}
