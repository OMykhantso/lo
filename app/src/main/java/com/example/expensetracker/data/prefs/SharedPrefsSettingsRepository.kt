package com.example.expensetracker.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Несекретні налаштування в звичайних SharedPreferences. Значення дзеркалюються в `StateFlow`,
 * тож UI реагує на зміни миттєво, а запис на диск іде асинхронно (`apply`).
 */
class SharedPrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val budget = MutableStateFlow(readBudget())
    private val currency = MutableStateFlow(readCurrency())

    override val budgetLimitMinor: Flow<Long?> = budget
    override val displayCurrency: Flow<Currency> = currency

    override suspend fun setBudgetLimit(minorUah: Long?) {
        prefs.edit().apply {
            if (minorUah == null) remove(KEY_BUDGET) else putLong(KEY_BUDGET, minorUah)
        }.apply()
        budget.value = minorUah
    }

    override suspend fun setDisplayCurrency(currency: Currency) {
        prefs.edit().putString(KEY_CURRENCY, currency.code).apply()
        this.currency.value = currency
    }

    private fun readBudget(): Long? = if (prefs.contains(KEY_BUDGET)) prefs.getLong(KEY_BUDGET, 0L) else null

    private fun readCurrency(): Currency = Currency.fromCode(prefs.getString(KEY_CURRENCY, null)) ?: Currency.UAH

    private companion object {
        const val FILE_NAME = "settings"
        const val KEY_BUDGET = "budget_limit_minor"
        const val KEY_CURRENCY = "display_currency"
    }
}
