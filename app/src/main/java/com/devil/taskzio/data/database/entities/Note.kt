package com.devil.taskzio.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Note color options (hex, display name) — dark-first palette for AMOLED
val NOTE_COLORS = listOf(
    "#1E1E1E" to "Default",
    "#2D2000" to "Yellow",
    "#0D2B1A" to "Green",
    "#0D1B33" to "Blue",
    "#2D0D1E" to "Pink",
    "#1A0D2E" to "Purple",
    "#FFFFFF" to "White"
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val color: String = "#1E1E1E",   // dark default — matches NoteEditorScreen
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
    // tags removed — was never read or written anywhere
)
