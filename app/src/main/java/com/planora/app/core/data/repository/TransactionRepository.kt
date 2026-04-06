package com.planora.app.core.data.repository

import com.planora.app.core.data.database.dao.TransactionDao
import com.planora.app.core.data.database.entities.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {
    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllTransactions()

    suspend fun getTransactionById(id: Long): Transaction? = dao.getTransactionById(id)
    suspend fun insertTransaction(t: Transaction): Long    = dao.insertTransaction(t)
    suspend fun updateTransaction(t: Transaction)          = dao.updateTransaction(t)
    suspend fun deleteTransaction(t: Transaction)          = dao.deleteTransaction(t)
    suspend fun deleteByLinkedGoalId(goalId: Long)         = dao.deleteByLinkedGoalId(goalId)
}
