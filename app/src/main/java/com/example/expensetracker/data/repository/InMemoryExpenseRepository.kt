package com.example.expensetracker.data.repository

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.time.TimeRange
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Реалізація в пам’яті: для Compose-прев’ю, юніт-тестів і як еталон поведінки Room
 * (ті самі правила сортування та групування, що й у SQL-запитах DAO).
 */
class InMemoryExpenseRepository(initial: List<Expense> = emptyList()) : ExpenseRepository {
    private val nextId = AtomicLong(1)
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())

    init {
        expenses.value = initial.map { it.copy(id = nextId.getAndIncrement()) }
    }

    override fun observeRecent(limit: Int): Flow<List<Expense>> =
        expenses.map { list -> list.newestFirst().take(limit) }

    override fun observeHistory(category: Category?): Flow<List<Expense>> =
        expenses.map { list -> list.filter { category == null || it.category == category }.newestFirst() }

    override fun observeCategoryTotals(range: TimeRange): Flow<List<CategoryTotal>> =
        expenses.map { list ->
            list.filter { it.timestamp in range }
                .groupBy { it.category to it.currency }
                .map { (key, group) ->
                    CategoryTotal(key.first, key.second, group.sumOf { it.amountMinor }, group.size)
                }
                .sortedWith(compareBy({ it.category.name }, { it.currency.name }))
        }

    override suspend fun add(expense: Expense): Long {
        val id = nextId.getAndIncrement()
        expenses.update { it + expense.copy(id = id) }
        return id
    }

    override suspend fun addAll(expenses: List<Expense>) {
        val withIds = expenses.map { it.copy(id = nextId.getAndIncrement()) }
        this.expenses.update { it + withIds }
    }

    override suspend fun delete(id: Long) {
        expenses.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun clear() {
        expenses.value = emptyList()
    }

    private fun List<Expense>.newestFirst() = sortedWith(compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id })
}
