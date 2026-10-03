package com.example.expensetracker.data.local.db

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.Expense

/** Невідома валюта (дані з іншої версії застосунку) → `null`: показувати суму у хибній валюті гірше, ніж пропустити. */
internal fun ExpenseEntity.toDomainOrNull(): Expense? {
    val currency = Currency.fromCode(currency) ?: return null
    return Expense(
        id = id,
        amountMinor = amount,
        currency = currency,
        category = Category.fromKey(category) ?: Category.OTHER,
        timestamp = timestamp,
        note = note,
    )
}

internal fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amount = amountMinor,
    currency = currency.code,
    category = category.name,
    timestamp = timestamp,
    note = note,
)

internal fun CategoryTotalRow.toDomainOrNull(): CategoryTotal? {
    val currency = Currency.fromCode(currency) ?: return null
    return CategoryTotal(
        category = Category.fromKey(category) ?: Category.OTHER,
        currency = currency,
        totalMinor = total,
        count = count,
    )
}
