package com.planora.app.core.data.database.dao

import androidx.room.*
import com.planora.app.core.data.database.entities.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>


    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions ORDER BY date ASC")
    suspend fun getAllForExport(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long


    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE linkedSavingsGoalId = :goalId")
    suspend fun deleteByLinkedGoalId(goalId: Long)

    @Query("SELECT * FROM transactions WHERE amount = :amount AND date BETWEEN :start AND :end AND merchant = :merchant LIMIT 1")
    suspend fun getDuplicateTransaction(amount: Double, start: Long, end: Long, merchant: String): Transaction?
}
