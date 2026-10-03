package com.example.expensetracker.testutil

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Expense
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

object TestData {
    val KYIV: ZoneId = ZoneId.of("Europe/Kyiv")

    /** 15 березня 2025, 12:00 за Києвом. */
    val NOW: Instant = Instant.parse("2025-03-15T10:00:00Z")

    val clock: Clock = Clock.fixed(NOW, KYIV)

    fun expense(
        amountMinor: Long = 10_000,
        currency: Currency = Currency.UAH,
        category: Category = Category.FOOD,
        timestamp: Long = NOW.toEpochMilli(),
        note: String = "",
        id: Long = 0,
    ) = Expense(id, amountMinor, currency, category, timestamp, note)
}
