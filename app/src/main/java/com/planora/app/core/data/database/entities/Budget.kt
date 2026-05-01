package com.planora.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category", "month", "year"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val alertAt: Float = 0.8f
)
