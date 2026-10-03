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
    private val reminder = MutableStateFlow(true)
    private val promptShown = MutableStateFlow(false)

    override val budgetLimitMinor: Flow<Long?> = budget
    override val displayCurrency: Flow<Currency> = currency
    override val reminderEnabled: Flow<Boolean> = reminder
    override val notificationPromptShown: Flow<Boolean> = promptShown

    override suspend fun setBudgetLimit(minorUah: Long?) {
        budget.value = minorUah
    }

    override suspend fun setDisplayCurrency(currency: Currency) {
        this.currency.value = currency
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        reminder.value = enabled
    }

    override suspend fun setNotificationPromptShown() {
        promptShown.value = true
    }
}
