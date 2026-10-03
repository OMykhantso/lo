package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.time.TimeRange
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/** Підсумки за категоріями для **поточного** календарного місяця (межі рахуються в момент підписки). */
class ObserveMonthTotals(
    private val repository: ExpenseRepository,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<CategoryTotal>> =
        flow { emit(TimeRange.currentMonth(clock)) }.flatMapLatest { repository.observeCategoryTotals(it) }
}
