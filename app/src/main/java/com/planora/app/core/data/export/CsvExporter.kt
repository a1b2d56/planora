package com.planora.app.core.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.planora.app.core.data.database.entities.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles professional CSV export for financial data.
 */
@Singleton
class CsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportTransactions(transactions: List<Transaction>): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = "Planora_Export_${System.currentTimeMillis()}.csv"
            val exportFile = File(context.cacheDir, fileName)
            
            FileOutputStream(exportFile).use { output ->
                val writer = output.bufferedWriter()
                
                // Header
                writer.write("Date,Category,Merchant,Type,Amount,Currency,Note,Account ID\n")
                
                transactions.forEach { t ->
                    val row = StringBuilder().apply {
                        append(escapeCsv(dateFormatter.format(Date(t.date)))); append(",")
                        append(escapeCsv(t.category)); append(",")
                        append(escapeCsv(t.merchant)); append(",")
                        append(escapeCsv(t.type.name)); append(",")
                        append(t.amount); append(",")
                        append(escapeCsv(t.currency)); append(",")
                        append(escapeCsv(t.note)); append(",")
                        append(t.accountId)
                        append("\n")
                    }.toString()
                    writer.write(row)
                }
                writer.flush()
            }
            
            FileProvider.getUriForFile(context, "${context.packageName}.provider", exportFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        val sanitized = value.replace("\"", "\"\"")
        return if (sanitized.contains(",") || sanitized.contains("\n") || sanitized.contains("\"")) {
            "\"$sanitized\""
        } else {
            sanitized
        }
    }
}
