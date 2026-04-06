package com.planora.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType { EVENT, BIRTHDAY, REMINDER, HOLIDAY }

/** Human-readable display name, e.g. BIRTHDAY → "Birthday". */
val EventType.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val date: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val type: EventType = EventType.EVENT,
    val color: String = "#4CAF50",
    val isAllDay: Boolean = false,
    val isYearly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
    // reminderMinutes removed  --  stored but never read or acted upon
)
