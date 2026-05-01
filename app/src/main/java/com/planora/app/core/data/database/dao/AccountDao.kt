package com.planora.app.core.data.database.dao

import androidx.room.*
import com.planora.app.core.data.database.entities.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isDefault DESC, name ASC")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun adjustBalance(id: Long, amount: Double)

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefault()
}
