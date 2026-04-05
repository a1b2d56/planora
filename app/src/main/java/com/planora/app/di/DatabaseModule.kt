package com.planora.app.di

import android.content.Context
import androidx.room.Room
import com.planora.app.data.database.DatabaseKeyManager
import com.planora.app.data.database.PlanoraDatabase
import com.planora.app.data.database.dao.*
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
        val factory = SupportOpenHelperFactory(keyManager.getPassphrase())
        return Room.databaseBuilder(context, PlanoraDatabase::class.java, "Planora_db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    // DAOs  --  @Singleton ensures one instance per app lifetime
    @Suppress("unused") @Provides @Singleton fun provideTaskDao(db: PlanoraDatabase): TaskDao                   = db.taskDao()
    @Suppress("unused") @Provides @Singleton fun provideTransactionDao(db: PlanoraDatabase): TransactionDao     = db.transactionDao()
    @Suppress("unused") @Provides @Singleton fun provideSavingsGoalDao(db: PlanoraDatabase): SavingsGoalDao     = db.savingsGoalDao()
    @Suppress("unused") @Provides @Singleton fun provideCalendarEventDao(db: PlanoraDatabase): CalendarEventDao = db.calendarEventDao()
    @Suppress("unused") @Provides @Singleton fun provideNoteDao(db: PlanoraDatabase): NoteDao                   = db.noteDao()
}
