package com.example.expensetracker.domain.notifications

/** Що робити з розкладом нагадування у відповідь на системну подію. */
enum class RescheduleMode {
    /** Нічого не робити. */
    IGNORE,

    /** Переконатися, що розклад існує (не скидаючи вже заплановане). */
    ENSURE_SCHEDULED,

    /** Перебудувати розклад під новий час/часову зону — 20:00 має лишатися 20:00 за місцевим часом. */
    REALIGN,
}

/**
 * Відновлення розкладу (`RECEIVE_BOOT_COMPLETED`):
 *  - перезавантаження / оновлення застосунку — гарантуємо, що розклад існує (WorkManager і сам його відновлює,
 *    це друга лінія захисту, наприклад на агресивних оболонках виробників);
 *  - зміна часу чи часової зони — звичайне відновлення не годиться: збережена затримка
 *    «до 20:00» рахувалась за старим часом, тож розклад потрібно перебудувати.
 */
object ReminderBootPolicy {
    const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    const val ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"
    const val ACTION_TIME_SET = "android.intent.action.TIME_SET"
    const val ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"

    fun modeFor(action: String?): RescheduleMode = when (action) {
        ACTION_BOOT_COMPLETED, ACTION_MY_PACKAGE_REPLACED -> RescheduleMode.ENSURE_SCHEDULED
        ACTION_TIME_SET, ACTION_TIMEZONE_CHANGED -> RescheduleMode.REALIGN
        else -> RescheduleMode.IGNORE
    }
}
