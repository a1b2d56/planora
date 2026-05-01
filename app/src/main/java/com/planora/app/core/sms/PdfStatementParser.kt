package com.planora.app.core.sms

import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Interface for PDF Bank/UPI Statement Parsers.
 */
interface PdfStatementParser {
    fun canHandle(text: String): Boolean
    fun parse(text: String): List<Transaction>
}

/**
 * GPay PDF Statement Parser.
 */
class GPayPdfParser : PdfStatementParser {
    override fun canHandle(text: String): Boolean {
        return (text.contains("GPay", ignoreCase = true) || text.contains("Google Pay", ignoreCase = true)) 
                && text.contains("UPI Transaction ID", ignoreCase = true)
    }

    override fun parse(text: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val dateRegex = Regex(
            """(\d{1,2}\s+[A-Za-z]{3},?\s+\d{4})\s*(\d{1,2}:\d{2}\s+[AP]M)""",
            RegexOption.IGNORE_CASE
        )

        val matches = dateRegex.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        val amountRegex = Regex("""(?:₹|Rs\.?)\s*([0-9,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val row = text.substring(start, end).replace(Regex("""\s+"""), " ")

            val dateMatch = matches[i]
            val dateStr = dateMatch.groupValues[1]
            val timeStr = dateMatch.groupValues[2]

            val dateTime = try {
                val cleanedDate = dateStr.replace(",", "")
                LocalDateTime.parse("$cleanedDate $timeStr", DateTimeFormatter.ofPattern("d MMM yyyy h:mm a", Locale.ENGLISH))
            } catch (_: Exception) {
                continue
            }

            val amountMatch = amountRegex.find(row)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: continue
            val amount = amountStr.toDoubleOrNull() ?: continue

            val isIncome = row.contains("Received from", ignoreCase = true)
            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

            val merchantMatch = if (isIncome) {
                Regex("""(?:Received from|Paid by)\s+(.+?)(?=\s+UPI Transaction ID|Paid to|Paid by|$)""", RegexOption.IGNORE_CASE).find(row)
            } else {
                Regex("""Paid to\s+(.+?)(?=\s+UPI Transaction ID|Paid to|Paid by|$)""", RegexOption.IGNORE_CASE).find(row)
            }

            val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Unknown"

            transactions.add(
                Transaction(
                    amount = amount,
                    type = type,
                    category = "Other",
                    note = "Imported from GPay PDF",
                    date = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    merchant = merchant,
                    accountId = 1
                )
            )
        }
        return transactions
    }
}

/**
 * PhonePe PDF Statement Parser.
 */
class PhonePePdfParser : PdfStatementParser {
    override fun canHandle(text: String): Boolean {
        return text.contains("PhonePe", ignoreCase = true)
    }

    override fun parse(text: String): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val dateRegex = Regex("""(\d{1,2}\s+[A-Za-z]{3,10},?\s*\d{4}|[A-Za-z]{3,10}\s+\d{1,2},?\s*\d{4})\s*(\d{1,2}[^\d\n\r]{1,5}\d{2}\s*[ap]m)""", RegexOption.IGNORE_CASE)
        
        val matches = dateRegex.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        val amountRegex = Regex("""(?:₹|Rs\.?)\s*([0-9,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val row = text.substring(start, end).replace(Regex("""\s+"""), " ")

            val dateMatch = matches[i]
            val dateStr = dateMatch.groupValues[1]
            val timeStr = dateMatch.groupValues[2]
            
            val dateTime = try {
                val normalizedDateStr = dateStr.replace(Regex("""\bSept\b""", RegexOption.IGNORE_CASE), "Sep")
                val cleanedTime = timeStr.replace(Regex("""[^0-9\s[ap]m]+""", RegexOption.IGNORE_CASE), ":")
                val combined = "$normalizedDateStr $cleanedTime".replace(Regex("""\s+"""), " ")
                
                LocalDateTime.parse(combined, DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a", Locale.ENGLISH))
            } catch (_: Exception) {
                continue
            }
            
            val amountMatch = amountRegex.find(row)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: continue
            val amount = amountStr.toDoubleOrNull() ?: continue
            
            val isIncome = row.contains("CREDIT", ignoreCase = true)
            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            
            val merchantMatch = if (isIncome) {
                Regex("""(?:Received from|Cashback from|Credited by|Paid by|From)\s+(.+?)(?=\s+(?:Transact\s*ion|UTR|CREDIT|DEBIT|₹|Rs|$))""", RegexOption.IGNORE_CASE).find(row)
            } else {
                Regex("""Paid to\s+(.+?)(?=\s+(?:Transact\s*ion|UTR|CREDIT|DEBIT|₹|Rs|$))""", RegexOption.IGNORE_CASE).find(row)
            }
            
            val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Unknown"

            transactions.add(
                Transaction(
                    amount = amount,
                    type = type,
                    category = "Other",
                    note = "Imported from PhonePe PDF",
                    date = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    merchant = merchant,
                    accountId = 1
                )
            )
        }
        return transactions
    }
}
