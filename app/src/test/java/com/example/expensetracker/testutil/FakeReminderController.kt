package com.example.expensetracker.testutil

import com.example.expensetracker.domain.notifications.ReminderController

/** Фейк планувальника: запам’ятовує, які команди отримав. */
class FakeReminderController : ReminderController {
    val calls = mutableListOf<String>()

    override fun enable() { calls += "enable" }
    override fun disable() { calls += "disable" }
    override fun ensureScheduled() { calls += "ensureScheduled" }
    override fun realign() { calls += "realign" }
}
