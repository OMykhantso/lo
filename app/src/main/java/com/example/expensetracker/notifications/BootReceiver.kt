package com.example.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.domain.notifications.ReminderBootPolicy
import com.example.expensetracker.domain.notifications.RescheduleMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Відновлення розкладу нагадування (`RECEIVE_BOOT_COMPLETED`): після перезавантаження, оновлення застосунку,
 * зміни часу або часової зони. Політику вибору дії див. [ReminderBootPolicy].
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mode = ReminderBootPolicy.modeFor(intent.action)
        if (mode == RescheduleMode.IGNORE) return

        val container = (context.applicationContext as ExpenseTrackerApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (container.settingsRepository.reminderEnabled.first()) {
                    when (mode) {
                        RescheduleMode.ENSURE_SCHEDULED -> container.reminderController.ensureScheduled()
                        RescheduleMode.REALIGN -> container.reminderController.realign()
                        RescheduleMode.IGNORE -> Unit
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
