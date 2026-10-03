package com.example.expensetracker.ui.navigation

import kotlinx.serialization.Serializable

/*
 * Типобезпечні маршрути (Navigation Compose 2.8+): кожен напрямок — @Serializable-тип,
 * аргументи — його поля. Рядкових шляхів і `arguments = listOf(navArgument(...))` немає.
 */

/** Головний екран: баланс і останні витрати (вкладка нижньої панелі). */
@Serializable
data object OverviewRoute

/** Аналітика за категоріями (вкладка нижньої панелі). */
@Serializable
data object AnalyticsRoute

/** Екран додавання витрати. Відкривається і через deep link з нагадування. */
@Serializable
data object AddExpenseRoute

/**
 * Історія витрат. [category] — ключ [com.example.expensetracker.domain.model.Category.name]
 * або `null` для всіх категорій.
 */
@Serializable
data class ExpenseHistoryRoute(val category: String? = null)

/** URI, що відкриває [AddExpenseRoute] (використовує сповіщення-нагадування, Lab 5). */
const val ADD_EXPENSE_DEEP_LINK = "expensetracker://add"
