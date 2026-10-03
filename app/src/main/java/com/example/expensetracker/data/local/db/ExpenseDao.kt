package com.example.expensetracker.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query(SqlQueries.RECENT)
    fun observeRecent(limit: Int): Flow<List<ExpenseEntity>>

    @Query(SqlQueries.HISTORY_ALL)
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query(SqlQueries.HISTORY_BY_CATEGORY)
    fun observeByCategory(category: String): Flow<List<ExpenseEntity>>

    @Query(SqlQueries.CATEGORY_TOTALS)
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotalRow>>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query(SqlQueries.DELETE_BY_ID)
    suspend fun deleteById(id: Long)

    @Query(SqlQueries.DELETE_ALL)
    suspend fun deleteAll()
}
