package com.example.expensetracker.data.local.db

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.ExchangeRate
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Пошкоджений або невідомий запис кешу ігнорується (повертає `null`), а не валить застосунок. */
internal fun ExchangeRateEntity.toDomainOrNull(): ExchangeRate? {
    val currency = Currency.fromCode(currency)?.takeIf { it != Currency.BASE } ?: return null
    val rate = rateToUah.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
    val date = try {
        LocalDate.parse(rateDate)
    } catch (e: DateTimeParseException) {
        return null
    }
    return ExchangeRate(currency, rate, date, fetchedAt)
}

internal fun ExchangeRate.toEntity() = ExchangeRateEntity(
    currency = currency.code,
    rateToUah = rateToUah.toPlainString(),
    rateDate = rateDate.toString(),
    fetchedAt = fetchedAtMillis,
)
