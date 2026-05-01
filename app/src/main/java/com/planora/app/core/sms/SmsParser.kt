package com.planora.app.core.sms

import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType


object SmsParser {

    fun parse(address: String, body: String, date: Long): Transaction? {
        val amount = UniversalPatterns.extractAmount(body) ?: return null
        val isExpense = UniversalPatterns.isExpense(body)
        val isIncome = UniversalPatterns.isIncome(body)
        
        // Skip if it doesn't look like a clear transaction (e.g. just balance info or OTP)
        if (!isExpense && !isIncome) return null
        
        val type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
        val merchant = UniversalPatterns.extractMerchant(body)
        val balance = UniversalPatterns.extractBalance(body)
        val bankName = BankDetector.detectBank(address)
        
        // Attempt simple category inference from merchant name
        val category = inferCategory(merchant, isExpense)

        val balanceNote = if (balance != null) "\nBalance after: $balance" else ""

        return Transaction(
            amount = amount,
            type = type,
            category = category,
            note = "Imported from $bankName ($address)$balanceNote",
            date = date,
            merchant = merchant,
            accountId = 1, // Placeholder — SmsImportManager reassigns this
            balanceAfter = balance
        )
    }

    @Suppress("SpellCheckingInspection")
    private fun inferCategory(merchant: String, isExpense: Boolean): String {
        val m = merchant.lowercase()
        return when {
            !isExpense -> "Salary"
            m.contains("zomato") || m.contains("swiggy") || m.contains("coffee") || m.contains("food") || m.contains("cafe") -> "Food"
            m.contains("uber") || m.contains("ola") || m.contains("irctc") || m.contains("metro") -> "Transport"
            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") || m.contains("store") -> "Shopping"
            m.contains("netflix") || m.contains("spotify") || m.contains("prime") || m.contains("hotstar") -> "Subscription"
            m.contains("jio") || m.contains("airtel") || m.contains("vi") || m.contains("bill") -> "Bills"
            m.contains("hospital") || m.contains("pharmacy") || m.contains("clinic") -> "Health"
            else -> "Other"
        }
    }
}
