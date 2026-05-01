package com.planora.app.core.data.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.planora.app.core.data.database.dao.*
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.AccountType
import com.planora.app.core.data.database.entities.Budget
import com.planora.app.core.data.database.entities.CalendarEvent
import com.planora.app.core.data.database.entities.EventType
import com.planora.app.core.data.database.entities.Note
import com.planora.app.core.data.database.entities.Priority
import com.planora.app.core.data.database.entities.SavingsGoal
import com.planora.app.core.data.database.entities.Task
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType

class Converters {
    @Suppress("unused") @TypeConverter fun fromPriority(v: Priority): String               = v.name
    @Suppress("unused") @TypeConverter fun toPriority(v: String): Priority                 = Priority.valueOf(v)
    @Suppress("unused") @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @Suppress("unused") @TypeConverter fun toTransactionType(v: String): TransactionType   = TransactionType.valueOf(v)
    @Suppress("unused") @TypeConverter fun fromEventType(v: EventType): String             = v.name
    @Suppress("unused") @TypeConverter fun toEventType(v: String): EventType               = EventType.valueOf(v)
    @Suppress("unused") @TypeConverter fun fromAccountType(v: AccountType): String         = v.name
    @Suppress("unused") @TypeConverter fun toAccountType(v: String): AccountType           = AccountType.valueOf(v)
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Extend transactions table
        db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE transactions ADD COLUMN merchant TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'INR'")

        // Create accounts table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0.0,
                currency TEXT NOT NULL DEFAULT 'INR',
                colorHex TEXT NOT NULL DEFAULT '#4CAF50',
                icon TEXT NOT NULL DEFAULT 'wallet',
                isDefault INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // Seed the default Cash wallet (id=1 so existing transactions link correctly)
        db.execSQL("""
            INSERT INTO accounts (id, name, type, balance, currency, colorHex, icon, isDefault, createdAt)
            VALUES (1, 'Cash', 'CASH', 0.0, 'INR', '#4CAF50', 'wallet', 1, ${System.currentTimeMillis()})
        """.trimIndent())

        // Create budgets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                category TEXT NOT NULL,
                monthlyLimit REAL NOT NULL,
                month INTEGER NOT NULL,
                year INTEGER NOT NULL,
                alertAt REAL NOT NULL DEFAULT 0.8
            )
        """.trimIndent())

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category_month_year ON budgets (category, month, year)")
    }
}

@Database(
    entities = [Task::class, Transaction::class, SavingsGoal::class, CalendarEvent::class, Note::class, Account::class, Budget::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlanoraDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun noteDao(): NoteDao
    abstract fun syncDao(): SyncDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
}
