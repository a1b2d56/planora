package com.planora.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE, TRANSFER, INVESTMENT }

val EXPENSE_CATEGORIES = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Health", "Education", "Subscription", "Other")
val INVESTMENT_CATEGORIES = listOf("Stocks", "Mutual Funds", "Crypto", "Bonds", "Real Estate", "Gold", "Other")
val INCOME_CATEGORIES  = listOf("Salary", "Freelance", "Investment", "Gift", "Business", "Deposit", "Other")

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val linkedSavingsGoalId: Long? = null,
    val accountId: Long = 1,
    val toAccountId: Long? = null, // Used for TRANSFER type
    val merchant: String = "",
    val isRecurring: Boolean = false,
    val currency: String = "INR",
    val balanceAfter: Double? = null // Snapshot for running balance tracking
)
