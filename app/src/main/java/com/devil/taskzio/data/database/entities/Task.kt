package com.devil.taskzio.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { LOW, MEDIUM, HIGH }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
