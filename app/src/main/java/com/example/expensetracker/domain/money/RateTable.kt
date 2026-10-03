package com.example.expensetracker.domain.money

import com.example.expensetracker.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Таблиця курсів «гривень за одиницю валюти» та конвертація між валютами через гривню.
 *
 * Інваріанти: курс завжди > 0 (нуль/від’ємне значення — помилка конструювання), гривня завжди = 1.
 * Відсутність курсу — це не помилка, а `null` у [convert]: UI має показати «немає курсу».
 */
class RateTable(rates: Map<Currency, BigDecimal>) {
    private val toUah: Map<Currency, BigDecimal>

    init {
        rates.forEach { (currency, rate) ->
            require(rate.signum() > 0) { "Rate for $currency must be positive, was $rate" }
        }
        toUah = rates + (Currency.BASE to BigDecimal.ONE)
    }

    fun supports(currency: Currency): Boolean = currency in toUah

    /** Якщо запитана валюта недоступна (немає курсу) — повертає гривню, яка завжди доступна. */
    fun resolveDisplayCurrency(requested: Currency): Currency = if (supports(requested)) requested else Currency.BASE

    /**
     * Конвертує [minor] (копійки/центи) з [from] в [to] з округленням HALF_UP до мінімальної одиниці.
     * @return `null`, якщо немає курсу будь-якої з валют або результат не вміщується в `Long`.
     */
    fun convert(minor: Long, from: Currency, to: Currency): Long? {
        if (from == to) return minor
        val fromRate = toUah[from] ?: return null
        val toRate = toUah[to] ?: return null
        return try {
            BigDecimal.valueOf(minor).multiply(fromRate).divide(toRate, 0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    companion object {
        val EMPTY = RateTable(emptyMap())
    }
}
