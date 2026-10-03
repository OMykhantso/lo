package com.example.expensetracker.data.mock

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Category.EDUCATION
import com.example.expensetracker.domain.model.Category.ENTERTAINMENT
import com.example.expensetracker.domain.model.Category.FOOD
import com.example.expensetracker.domain.model.Category.HEALTH
import com.example.expensetracker.domain.model.Category.HOUSING
import com.example.expensetracker.domain.model.Category.OTHER
import com.example.expensetracker.domain.model.Category.SHOPPING
import com.example.expensetracker.domain.model.Category.TRANSPORT
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Currency.EUR
import com.example.expensetracker.domain.model.Currency.UAH
import com.example.expensetracker.domain.model.Currency.USD
import com.example.expensetracker.domain.model.Expense
import java.time.Instant
import java.time.ZoneId

/**
 * Демонстраційні витрати (Lab 1, AI-завдання «згенерувати Mock-дані»).
 *
 * Дані детерміновані й задані відносно «сьогодні» (`daysAgo`), тому завжди охоплюють
 * поточний і попередній місяці та всі категорії. Суми — у копійках/центах.
 */
object MockExpenses {
    private data class Entry(
        val daysAgo: Int,
        val hour: Int,
        val category: Category,
        val amountMinor: Long,
        val note: String,
        val currency: Currency = UAH,
    )

    private val entries = listOf(
        Entry(0, 8, TRANSPORT, 2_000, "Метро"),
        Entry(0, 9, FOOD, 8_500, "Кава та круасан"),
        Entry(1, 13, FOOD, 32_000, "Обід у кафе"),
        Entry(1, 19, FOOD, 84_250, "Сільпо — продукти на тиждень"),
        Entry(2, 9, TRANSPORT, 15_000, "Таксі"),
        Entry(2, 18, ENTERTAINMENT, 25_000, "Квиток у кіно"),
        Entry(3, 20, SHOPPING, 189_900, "Кросівки"),
        Entry(3, 12, FOOD, 21_500, "Піца"),
        Entry(4, 12, HEALTH, 46_780, "Аптека"),
        Entry(5, 10, TRANSPORT, 120_000, "Заправка"),
        Entry(5, 21, ENTERTAINMENT, 999, "Netflix", USD),
        Entry(6, 11, FOOD, 54_320, "Атб — продукти"),
        Entry(7, 17, EDUCATION, 120_000, "Онлайн-курс Kotlin"),
        Entry(8, 9, HOUSING, 215_000, "Комунальні послуги"),
        Entry(9, 14, FOOD, 38_700, "Бізнес-ланч"),
        Entry(10, 10, HOUSING, 1_400_000, "Оренда квартири"),
        Entry(11, 16, SHOPPING, 64_900, "Подарунок другу"),
        Entry(12, 18, ENTERTAINMENT, 499, "Spotify", EUR),
        Entry(13, 8, TRANSPORT, 2_000, "Метро"),
        Entry(14, 19, FOOD, 91_040, "Сільпо — продукти"),
        Entry(15, 15, HEALTH, 85_000, "Візит до лікаря"),
        Entry(16, 12, OTHER, 30_000, "Стрижка"),
        Entry(17, 20, ENTERTAINMENT, 140_000, "Концерт"),
        Entry(19, 11, EDUCATION, 45_000, "Книги"),
        Entry(21, 9, TRANSPORT, 18_500, "Таксі"),
        Entry(23, 13, FOOD, 27_800, "Обід"),
        Entry(25, 10, SHOPPING, 249_900, "Куртка"),
        Entry(27, 18, FOOD, 76_300, "Продукти"),
        Entry(29, 14, OTHER, 12_000, "Поштові відправлення"),
        Entry(31, 10, HOUSING, 1_400_000, "Оренда квартири"),
        Entry(32, 9, HOUSING, 198_000, "Комунальні послуги"),
        Entry(34, 19, FOOD, 88_400, "Сільпо — продукти"),
        Entry(36, 12, TRANSPORT, 110_000, "Заправка"),
        Entry(38, 20, ENTERTAINMENT, 35_000, "Боулінг"),
        Entry(40, 11, HEALTH, 52_600, "Вітаміни"),
        Entry(42, 17, FOOD, 41_900, "Вечеря з друзями"),
        Entry(44, 10, EDUCATION, 120_000, "Онлайн-курс Kotlin"),
        Entry(46, 15, SHOPPING, 99_900, "Навушники"),
        Entry(48, 9, TRANSPORT, 2_000, "Метро"),
        Entry(50, 14, FOOD, 63_200, "Продукти"),
    )

    /**
     * @param now «зараз»; витрати, що за розкладом припадають на майбутнє (сьогодні, але пізніше за [now]), відкидаються.
     */
    fun generate(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): List<Expense> {
        val today = now.atZone(zone).toLocalDate()
        return entries
            .map { entry ->
                Expense(
                    amountMinor = entry.amountMinor,
                    currency = entry.currency,
                    category = entry.category,
                    timestamp = today.minusDays(entry.daysAgo.toLong()).atTime(entry.hour, 0).atZone(zone)
                        .toInstant().toEpochMilli(),
                    note = entry.note,
                )
            }
            .filter { it.timestamp <= now.toEpochMilli() }
    }
}
