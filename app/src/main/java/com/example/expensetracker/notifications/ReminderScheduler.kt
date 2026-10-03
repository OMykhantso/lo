package com.example.expensetracker.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.domain.notifications.NextReminderTime
import com.example.expensetracker.domain.notifications.ReminderController
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Щоденне нагадування на `WorkManager` (`PeriodicWorkRequest`, період 24 год).
 *
 * Перший запуск відкладається рівно до найближчих 20:00 за місцевим часом ([NextReminderTime]), далі WorkManager
 * повторює щодобово. Розклад зберігається в базі WorkManager і переживає перезапуск процесу та перезавантаження.
 *
 * Про точність: WorkManager не обіцяє запуск «секунда в секунду» — режими енергозбереження можуть зсунути
 * виконання (зазвичай на хвилини). Для щоденного нагадування це прийнятно; точні будильники потребували б
 * `SCHEDULE_EXACT_ALARM`, який з Android 14 за замовчуванням не надається.
 */
class ReminderScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ReminderController {
    private val workManager: WorkManager = WorkManager.getInstance(context.applicationContext)

    override fun enable() = schedule(ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)

    override fun disable() {
        workManager.cancelUniqueWork(UNIQUE_NAME)
    }

    override fun ensureScheduled() = schedule(ExistingPeriodicWorkPolicy.KEEP)

    override fun realign() = schedule(ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)

    private fun schedule(policy: ExistingPeriodicWorkPolicy) {
        val delay = NextReminderTime.delayFrom(ZonedDateTime.now(clock))
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(REPEAT_INTERVAL_HOURS, TimeUnit.HOURS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, policy, request)
    }

    companion object {
        const val UNIQUE_NAME = "daily_expense_reminder"
        private const val REPEAT_INTERVAL_HOURS = 24L
    }
}
