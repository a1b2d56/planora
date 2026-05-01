package com.planora.app.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.Budget
import com.planora.app.core.data.database.entities.CalendarEvent
import com.planora.app.core.data.database.entities.Note
import com.planora.app.core.data.database.entities.SavingsGoal
import com.planora.app.core.data.database.entities.Task
import com.planora.app.core.data.database.entities.Transaction as DBTransaction

/** DAO for bulk cloud sync operations. */
@Dao
interface SyncDao {
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<Task>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<DBTransaction>
    
    @Query("SELECT * FROM savings_goals")
    suspend fun getAllSavingsGoals(): List<SavingsGoal>
    
    @Query("SELECT * FROM calendar_events")
    suspend fun getAllCalendarEvents(): List<CalendarEvent>
    
    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<Account>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<DBTransaction>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoal>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvents(events: List<CalendarEvent>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()
    
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()
    
    @Query("DELETE FROM savings_goals")
    suspend fun clearSavingsGoals()
    
    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()
    
    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    /** Securely replaces all active data in a single transaction. */
    @Transaction
    suspend fun applySyncData(
        tasks: List<Task>,
        transactions: List<DBTransaction>,
        accounts: List<Account>,
        budgets: List<Budget>,
        goals: List<SavingsGoal>,
        events: List<CalendarEvent>,
        notes: List<Note>
    ) {
        clearTasks()
        clearTransactions()
        clearAccounts()
        clearBudgets()
        clearSavingsGoals()
        clearCalendarEvents()
        clearNotes()

        insertTasks(tasks)
        insertTransactions(transactions)
        insertAccounts(accounts)
        insertBudgets(budgets)
        insertSavingsGoals(goals)
        insertCalendarEvents(events)
        insertNotes(notes)
    }
}
