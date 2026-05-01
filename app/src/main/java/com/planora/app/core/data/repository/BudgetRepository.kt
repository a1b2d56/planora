package com.planora.app.core.data.repository

import com.planora.app.core.data.database.dao.BudgetDao
import com.planora.app.core.data.database.entities.Budget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(private val dao: BudgetDao) {

    fun getForMonth(month: Int, year: Int): Flow<List<Budget>> = dao.getForMonth(month, year)

    fun getAllBudgets(): Flow<List<Budget>> = dao.getAllBudgets()

    suspend fun upsert(budget: Budget): Long = dao.upsert(budget)

    suspend fun delete(budget: Budget) = dao.delete(budget)
}
