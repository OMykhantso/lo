package com.example.expensetracker.ui.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Людський запис дати витрати: «Сьогодні, 14:05», «Вчора, 09:30», «12 березня», «5 грудня 2024». */
object ExpenseDateText {
    private val locale: Locale = Locale.forLanguageTag("uk-UA")
    private val time = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", locale)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
    private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", locale)

    fun format(timestamp: Long, clock: Clock): String {
        val moment = Instant.ofEpochMilli(timestamp).atZone(clock.zone)
        val today = LocalDate.now(clock)
        val date = moment.toLocalDate()
        return when {
            date == today -> "Сьогодні, ${moment.format(time)}"
            date == today.minusDays(1) -> "Вчора, ${moment.format(time)}"
            date.year == today.year -> moment.format(dayMonth)
            else -> moment.format(dayMonthYear)
        }
    }

    /** «Березень 2025» — заголовок поточного місяця. */
    fun monthTitle(clock: Clock): String =
        LocalDate.now(clock).format(monthYear).replaceFirstChar { it.titlecase(locale) }
}
