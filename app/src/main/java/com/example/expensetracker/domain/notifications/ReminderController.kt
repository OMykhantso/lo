package com.example.expensetracker.domain.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Керування щоденним нагадуванням. Android-реалізація — WorkManager (`notifications.ReminderScheduler`). */
interface ReminderController {
    /** Вмикає нагадування: розклад будується заново, щоб перше спрацювання було о найближчих 20:00. */
    fun enable()

    fun disable()

    /** Створює розклад, якщо його немає; наявний не скидає. Безпечно викликати на кожному запуску. */
    fun ensureScheduled()

    /** Перебудовує розклад під поточний час і часову зону. */
    fun realign()
}

/** Стан і запит дозволу на сповіщення (Android 13+: `POST_NOTIFICATIONS`). */
interface NotificationPermission {
    /** Чи можуть сповіщення застосунку реально показуватись (дозвіл + не вимкнено в системних налаштуваннях). */
    val granted: StateFlow<Boolean>

    /** Показує системний діалог дозволу, а якщо його вже відхилено — відкриває налаштування сповіщень застосунку. */
    fun request()
}

/** Дозвіл, що завжди надано, — для тестів і прев’ю. */
class FakeNotificationPermission(initial: Boolean = true) : NotificationPermission {
    private val state = MutableStateFlow(initial)
    override val granted: StateFlow<Boolean> = state
    var requests = 0
        private set

    override fun request() {
        requests++
    }
}
