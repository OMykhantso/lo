package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.model.Currency
import kotlinx.coroutines.flow.Flow

/** Несекретні налаштування користувача (SharedPreferences). Секрети (PIN) — окремо, в зашифрованому сховищі. */
interface SettingsRepository {
    /** Місячний бюджет у гривнях (мінімальні одиниці) або `null`, якщо ліміт не задано. */
    val budgetLimitMinor: Flow<Long?>

    /** Валюта, у якій показуються баланс і аналітика. */
    val displayCurrency: Flow<Currency>

    /** Чи увімкнено щоденне нагадування о 20:00 (за замовчуванням — так). */
    val reminderEnabled: Flow<Boolean>

    /** Чи вже показували запит дозволу на сповіщення (щоб не набридати при кожному запуску). */
    val notificationPromptShown: Flow<Boolean>

    suspend fun setBudgetLimit(minorUah: Long?)

    suspend fun setDisplayCurrency(currency: Currency)

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setNotificationPromptShown()
}
