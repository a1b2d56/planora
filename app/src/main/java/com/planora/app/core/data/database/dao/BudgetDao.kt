package com.planora.app.core.data.database.dao

import androidx.room.*
import com.planora.app.core.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year ORDER BY category ASC")
    fun getForMonth(month: Int, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<Budget>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget): Long

    @Delete
    suspend fun delete(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
