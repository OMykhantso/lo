package com.example.expensetracker.domain.money

import com.example.expensetracker.domain.model.Currency
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
}
