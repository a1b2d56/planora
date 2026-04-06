package com.planora.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }

// Category lists live with the entity, not in the UI layer
val EXPENSE_CATEGORIES = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Health", "Education", "Other")
val INCOME_CATEGORIES  = listOf("Salary", "Freelance", "Investment", "Gift", "Business", "Deposit", "Other")

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val linkedSavingsGoalId: Long? = null
    // createdAt removed  --  never read or used anywhere; date serves as the record timestamp
)
