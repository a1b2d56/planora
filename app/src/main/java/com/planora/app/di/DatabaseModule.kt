package com.planora.app.di

import android.content.Context
import androidx.room.Room
import com.planora.app.core.data.database.DatabaseKeyManager
import com.planora.app.core.data.database.MIGRATION_5_6
import com.planora.app.core.data.database.PlanoraDatabase
import com.planora.app.core.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Suppress("unused") @Provides @Singleton
    fun provideKeyManager(@ApplicationContext context: Context): DatabaseKeyManager =
        DatabaseKeyManager(context)

    @Suppress("unused") @Provides @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): PlanoraDatabase {
        System.loadLibrary("sqlcipher")

        val passphrase = keyManager.getPassphrase()
        val dbFile = context.getDatabasePath("Planora_db")

        if (dbFile.exists()) {
            try {
                val db = net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    passphrase,
                    null,
                    net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                    null
                )
                db.close()
            } catch (e: Exception) {
                android.util.Log.e("SQLCipher", "Key mismatch or corrupted DB, wiping.", e)
                dbFile.delete()
                context.getDatabasePath("Planora_db-wal").delete()
                context.getDatabasePath("Planora_db-journal").delete()
                context.getDatabasePath("Planora_db-shm").delete()
            }
        }

        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(context, PlanoraDatabase::class.java, "Planora_db")
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_5_6)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Suppress("unused") @Provides @Singleton fun provideTaskDao(db: PlanoraDatabase): TaskDao                   = db.taskDao()
    @Suppress("unused") @Provides @Singleton fun provideTransactionDao(db: PlanoraDatabase): TransactionDao     = db.transactionDao()
    @Suppress("unused") @Provides @Singleton fun provideSavingsGoalDao(db: PlanoraDatabase): SavingsGoalDao     = db.savingsGoalDao()
    @Suppress("unused") @Provides @Singleton fun provideCalendarEventDao(db: PlanoraDatabase): CalendarEventDao = db.calendarEventDao()
    @Suppress("unused") @Provides @Singleton fun provideNoteDao(db: PlanoraDatabase): NoteDao                   = db.noteDao()
    @Suppress("unused") @Provides @Singleton fun provideAccountDao(db: PlanoraDatabase): AccountDao             = db.accountDao()
    @Suppress("unused") @Provides @Singleton fun provideBudgetDao(db: PlanoraDatabase): BudgetDao               = db.budgetDao()
    @Suppress("unused") @Provides @Singleton fun provideSyncDao(db: PlanoraDatabase): SyncDao                   = db.syncDao()
}
