package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.RateTable

data class CategorySlice(
    val category: Category,
    val totalMinor: Long,
    /** Частка від загальної суми, 0..1. */
    val fraction: Float,
    val count: Int,
)

data class CategoryBreakdown(
    val currency: Currency,
    val totalMinor: Long,
    /** Від більшої до меншої; категорії з нульовою сумою відсутні. */
    val slices: List<CategorySlice>,
    val unconvertibleCount: Int,
)

/** Розподіл витрат за категоріями у спільній валюті — основа кругової діаграми. */
object CategoryBreakdownCalculator {
    fun calculate(totals: List<CategoryTotal>, table: RateTable, displayCurrency: Currency): CategoryBreakdown {
        val currency = table.resolveDisplayCurrency(displayCurrency)

        val perCategory = linkedMapOf<Category, Long>()
        val counts = linkedMapOf<Category, Int>()
        var unconvertible = 0
        for (row in totals) {
            val converted = table.convert(row.totalMinor, row.currency, currency)
            if (converted == null) {
                unconvertible += row.count
                continue
            }
            perCategory.merge(row.category, converted, Long::plus)
            counts.merge(row.category, row.count, Int::plus)
        }

        val total = perCategory.values.sum()
        val slices = perCategory
            .filterValues { it > 0 }
            .map { (category, amount) ->
                CategorySlice(category, amount, (amount.toDouble() / total).toFloat(), counts.getValue(category))
            }
            .sortedByDescending { it.totalMinor }

        return CategoryBreakdown(currency, slices.sumOf { it.totalMinor }, slices, unconvertible)
    }
}
