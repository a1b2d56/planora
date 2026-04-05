package com.planora.app.data.database

import androidx.room.*
import com.planora.app.data.database.dao.CalendarEventDao
import com.planora.app.data.database.dao.NoteDao
import com.planora.app.data.database.dao.SavingsGoalDao
import com.planora.app.data.database.dao.TaskDao
import com.planora.app.data.database.dao.TransactionDao
import com.planora.app.data.database.entities.CalendarEvent
import com.planora.app.data.database.entities.EventType
import com.planora.app.data.database.entities.Note
import com.planora.app.data.database.entities.Priority
import com.planora.app.data.database.entities.SavingsGoal
import com.planora.app.data.database.entities.Task
import com.planora.app.data.database.entities.Transaction
import com.planora.app.data.database.entities.TransactionType

class Converters {
    @Suppress("unused")
    @TypeConverter fun fromPriority(v: Priority): String               = v.name
    @Suppress("unused")
    @TypeConverter fun toPriority(v: String): Priority                 = Priority.valueOf(v)
    @Suppress("unused")
    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @Suppress("unused")
    @TypeConverter fun toTransactionType(v: String): TransactionType   = TransactionType.valueOf(v)
    @Suppress("unused")
    @TypeConverter fun fromEventType(v: EventType): String             = v.name
    @Suppress("unused")
    @TypeConverter fun toEventType(v: String): EventType               = EventType.valueOf(v)
}

@Database(
    entities = [Task::class, Transaction::class, SavingsGoal::class, CalendarEvent::class, Note::class],
    version = 4,   // added Transaction.linkedSavingsGoalId
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlanoraDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun noteDao(): NoteDao
    abstract fun syncDao(): com.planora.app.data.database.dao.SyncDao
}
