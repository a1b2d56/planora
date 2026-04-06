package com.planora.app.core.data.backup

import com.planora.app.core.data.database.entities.*

/**
 * A perfectly clean JSON-friendly bundle containing the complete state of the app.
 * By exporting data into this bundle, we completely bypass file-level locks and
 * raw SQLite database overwrites.
 */
data class CloudSyncData(
    val exportDateMs: Long,
    val tasks: List<Task>,
    val transactions: List<Transaction>,
    val savingsGoals: List<SavingsGoal>,
    val calendarEvents: List<CalendarEvent>,
    val notes: List<Note>,
    val preferences: SyncPreferences
)

data class SyncPreferences(
    val themeName: String,
    val notificationsEnabled: Boolean,
    val currencySymbol: String,
    val userName: String
)
