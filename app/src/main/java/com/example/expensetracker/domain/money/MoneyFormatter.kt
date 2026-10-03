package com.example.expensetracker.domain.money

import com.example.expensetracker.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/** Форматування сум для відображення: `12 345,60 ₴`. Не залежить від Locale пристрою. */
object MoneyFormatter {
    private const val NBSP = ' '

    /** Число без символу валюти: `-1 234,50`. */
    fun formatNumber(minor: Long): String {
        val sign = if (minor < 0) "-" else ""
        val absolute = abs(minor)
        val integer = (absolute / 100).toString()
        val fraction = (absolute % 100).toString().padStart(2, '0')
        val grouped = integer.reversed().chunked(3).joinToString(NBSP.toString()).reversed()
        return "$sign$grouped,$fraction"
    }

    fun format(minor: Long, currency: Currency): String = "${formatNumber(minor)}$NBSP${currency.symbol}"

    /** Для поля введення: `30000` або `30000.50` — без групування й символу валюти. */
    fun formatPlain(minor: Long): String {
        val integer = minor / 100
        val fraction = abs(minor % 100)
        return if (fraction == 0L) integer.toString() else "$integer.${fraction.toString().padStart(2, '0')}"
    }

    /** Курс НБУ з двома знаками: `41,18`. */
    fun formatRate(rate: BigDecimal): String = rate.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',')

    /** Частка як відсоток: `0.554` → `55%`. */
    fun formatPercent(fraction: Float): String = "${Math.round(fraction * 100)}%"
}
