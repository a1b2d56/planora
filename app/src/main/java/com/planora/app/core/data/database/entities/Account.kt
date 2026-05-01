package com.planora.app.core.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType { CASH, BANK, CREDIT, SAVINGS, DIGITAL }

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val currency: String = "INR",
    val colorHex: String = "#4CAF50",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
