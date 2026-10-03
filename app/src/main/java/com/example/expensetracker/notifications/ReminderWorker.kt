package com.example.expensetracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.ExpenseTrackerApp
import kotlinx.coroutines.flow.first

/** Виконується WorkManager-ом щодня близько 20:00: показує нагадування, якщо воно ввімкнене в налаштуваннях. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ExpenseTrackerApp).container
        if (container.settingsRepository.reminderEnabled.first()) {
            NotificationHelper.showDailyReminder(applicationContext)
        }
        return Result.success()
    }
}
