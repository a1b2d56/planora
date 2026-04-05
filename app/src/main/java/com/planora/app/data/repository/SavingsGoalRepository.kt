package com.planora.app.data.repository

import com.planora.app.data.database.dao.SavingsGoalDao
import com.planora.app.data.database.entities.SavingsGoal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepository @Inject constructor(private val dao: SavingsGoalDao) {
    fun getAllGoals(): Flow<List<SavingsGoal>>             = dao.getAllGoals()
    fun getActiveGoals(): Flow<List<SavingsGoal>>         = dao.getActiveGoals()
    suspend fun getGoalById(id: Long): SavingsGoal?       = dao.getGoalById(id)
    suspend fun insertGoal(goal: SavingsGoal): Long       = dao.insertGoal(goal)
    suspend fun updateGoal(goal: SavingsGoal)             = dao.updateGoal(goal)
    suspend fun deleteGoal(goal: SavingsGoal)             = dao.deleteGoal(goal)
    suspend fun updateCurrentAmount(id: Long, amount: Double) = dao.updateCurrentAmount(id, amount)
    suspend fun updateCompleted(id: Long, completed: Boolean) = dao.updateCompleted(id, completed)
}
