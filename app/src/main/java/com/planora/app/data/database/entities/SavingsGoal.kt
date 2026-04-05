package com.planora.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val durationDays: Int,
    val startDate: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val dailySaving: Double get() = if (durationDays > 0) targetAmount / durationDays else 0.0
    val remainingAmount: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
    val progressPercent: Float get() = if (targetAmount > 0) (currentAmount / targetAmount * 100).toFloat().coerceIn(0f, 100f) else 0f
    val deadlineDate: Long get() = startDate + (durationDays.toLong() * 24 * 60 * 60 * 1000)
}
