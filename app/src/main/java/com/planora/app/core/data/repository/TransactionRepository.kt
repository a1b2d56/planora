package com.planora.app.core.data.repository

import com.planora.app.core.data.database.dao.TransactionDao
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val accountDao: com.planora.app.core.data.database.dao.AccountDao
) {
    fun getAllTransactions(): Flow<List<Transaction>>               = dao.getAllTransactions()

    suspend fun getTransactionById(id: Long): Transaction?         = dao.getTransactionById(id)
    suspend fun getAllForExport(): List<Transaction>               = dao.getAllForExport()

    /**
     * Safely inserts a transaction and handles all cross-account balance adjustments.
     * Returns the transaction ID on success, or -1 on failure.
     */
    suspend fun insertTransaction(t: Transaction): Long = runCatching {
        // Handle Balance Adjustments
        val account = accountDao.getById(t.accountId)
        val currentBalance = account?.balance ?: 0.0
        
        val snapshot = when (t.type) {
            TransactionType.INCOME -> currentBalance + t.amount
            TransactionType.EXPENSE -> currentBalance - t.amount
            TransactionType.TRANSFER -> currentBalance - t.amount
            TransactionType.INVESTMENT -> currentBalance - t.amount
        }
        
        val finalT = t.copy(balanceAfter = snapshot)
        val id = dao.insertTransaction(finalT)
        
        // Update Source Account
        accountDao.adjustBalance(t.accountId, if (t.type == TransactionType.INCOME) t.amount else -t.amount)
        
        // Update Target Account for Transfers
        if (t.type == TransactionType.TRANSFER && t.toAccountId != null) {
            accountDao.adjustBalance(t.toAccountId, t.amount)
        }
        
        id
    }.getOrDefault(-1L)
    
    suspend fun updateTransaction(t: Transaction)                  = dao.updateTransaction(t)
    
    suspend fun deleteTransaction(t: Transaction) {
        // Reverse balance update
        val impact = when (t.type) {
            TransactionType.INCOME -> -t.amount
            TransactionType.EXPENSE -> t.amount
            TransactionType.TRANSFER -> t.amount
            TransactionType.INVESTMENT -> t.amount
        }
        accountDao.adjustBalance(t.accountId, impact)
        
        if (t.type == TransactionType.TRANSFER && t.toAccountId != null) {
            accountDao.adjustBalance(t.toAccountId, -t.amount)
        }
        
        dao.deleteTransaction(t)
    }


    suspend fun deleteByLinkedGoalId(goalId: Long)                 = dao.deleteByLinkedGoalId(goalId)

    suspend fun isDuplicate(amount: Double, date: Long, merchant: String): Boolean {
        // Simple logic to detect possible duplicates imported again (e.g. within 1 min of same txn)
        return dao.getDuplicateTransaction(amount, date - 60000, date + 60000, merchant) != null
    }
}
