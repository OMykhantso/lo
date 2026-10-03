package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.db.ExpenseDao
import com.example.expensetracker.data.local.db.toDomainOrNull
import com.example.expensetracker.data.local.db.toEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.time.TimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Реалізація [ExpenseRepository] поверх Room: усі читання — реактивні `Flow`, що перевипускаються після кожної зміни таблиці. */
class RoomExpenseRepository(private val dao: ExpenseDao) : ExpenseRepository {

    override fun observeRecent(limit: Int): Flow<List<Expense>> =
        dao.observeRecent(limit).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeHistory(category: Category?): Flow<List<Expense>> {
        val rows = if (category == null) dao.observeAll() else dao.observeByCategory(category.name)
        return rows.map { list -> list.mapNotNull { it.toDomainOrNull() } }
    }

    override fun observeCategoryTotals(range: TimeRange): Flow<List<CategoryTotal>> =
        dao.observeCategoryTotals(range.startInclusive, range.endExclusive)
            .map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun add(expense: Expense): Long = dao.insert(expense.toEntity())

    override suspend fun addAll(expenses: List<Expense>) = dao.insertAll(expenses.map { it.toEntity() })

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun clear() = dao.deleteAll()
}
