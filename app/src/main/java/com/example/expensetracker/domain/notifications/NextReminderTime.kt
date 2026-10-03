package com.example.expensetracker.domain.notifications

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Точний розрахунок моменту наступного нагадування (Lab 5, AI-завдання).
 *
 * Береться **найближчий строго майбутній** момент із локальним часом [REMINDER_TIME] у часовій зоні користувача.
 * Обчислення в «настінному» часі, а не додаванням 24 годин, тому коректне в дні переходу на літній/зимовий час
 * (доба може тривати 23 або 25 годин), у кінці місяця та на 29 лютого.
 */
object NextReminderTime {
    val REMINDER_TIME: LocalTime = LocalTime.of(20, 0)

    fun after(now: ZonedDateTime, time: LocalTime = REMINDER_TIME): ZonedDateTime {
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        // `isAfter`, а не `!isBefore`: рівно о 20:00:00.000 наступне нагадування — завтрашнє, без дубля.
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }

    /** Початкова затримка для `PeriodicWorkRequest`: реальний (не «настінний») час до наступного нагадування. */
    fun delayFrom(now: ZonedDateTime, time: LocalTime = REMINDER_TIME): Duration =
        Duration.between(now, after(now, time))
}
