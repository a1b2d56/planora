package com.planora.app.core.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.AccountType
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.repository.AccountRepository
import com.planora.app.core.data.repository.TransactionRepository
import com.planora.app.core.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class SmsImportManager(
    private val context: Context,
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val prefsManager: PrefsManager
) {
    suspend fun scanSmsForTransactions(): List<Transaction> = withContext(Dispatchers.IO) {
        val rawParsed = mutableListOf<Pair<String, Transaction>>()
        val detectedCurrencies = mutableListOf<String>()
        val smsBodies = mutableListOf<String>()

        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val thirtyDaysAgo = cal.timeInMillis

        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())

        val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} DESC")

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val address = it.getString(addressIndex) ?: continue
                val body = it.getString(bodyIndex) ?: continue
                val date = it.getLong(dateIndex)

                val bankName = BankDetector.detectBank(address)
                val parsed = SmsParser.parse(address, body, date)
                if (parsed != null) {
                    rawParsed.add(bankName to parsed)
                    smsBodies.add(body)
                    UniversalPatterns.detectCurrency(body)?.let { sym -> detectedCurrencies.add(sym) }
                }
            }
        }

        // Auto-set currency to the most frequently detected one
        if (detectedCurrencies.isNotEmpty()) {
            val dominant = detectedCurrencies.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            if (dominant != null) {
                prefsManager.setCurrencySymbol(dominant)
            }
        }

        // Auto-create wallets for any detected banks that don't exist yet
        val detectedBanks = rawParsed.map { it.first }.filter { it != "Bank" }.distinct()
        val existingAccounts = accountRepository.getAll().first()

        for (bankName in detectedBanks) {
            val alreadyExists = existingAccounts.any { it.name.equals(bankName, ignoreCase = true) }
            if (!alreadyExists) {
                accountRepository.insert(Account(name = bankName, type = AccountType.BANK))
            }
        }

        // Re-fetch accounts after potential inserts
        val updatedAccounts = accountRepository.getAll().first()

        // Map transactions to their correct account IDs
        val transactions = rawParsed.map { (bankName, tx) ->
            val matched = updatedAccounts.find { it.name.equals(bankName, ignoreCase = true) }
            tx.copy(accountId = matched?.id ?: 1L)
        }

        // Deduplicate against existing transactions in DB
        val existing = repository.getAllForExport()
        val existingWindow = 60000L * 5

        return@withContext transactions.filter { newTx ->
            val isDuplicate = existing.any { exTx ->
                val amtMatches = exTx.amount == newTx.amount
                val timeMatches = kotlin.math.abs(exTx.date - newTx.date) < existingWindow
                amtMatches && timeMatches
            }
            !isDuplicate
        }
    }
}
