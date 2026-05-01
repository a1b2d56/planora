package com.planora.app.core.sms

import android.content.Context
import android.net.Uri
import com.planora.app.core.data.repository.TransactionRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfImportManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: TransactionRepository
) {
    private val parsers = listOf(GPayPdfParser(), PhonePePdfParser())

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun importFromPdf(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()

                val parser = parsers.find { it.canHandle(text) }
                    ?: return@withContext Result.failure(Exception("Unsupported statement format"))

                val transactions = parser.parse(text)
                if (transactions.isEmpty()) {
                    return@withContext Result.failure(Exception("No transactions found in statement"))
                }

                var importedCount = 0
                transactions.forEach { txn ->
                    // Check for duplicates (Simple check by date, amount, merchant)
                    val exists = repository.isDuplicate(txn.amount, txn.date, txn.merchant)
                    if (!exists) {
                        repository.insertTransaction(txn)
                        importedCount++
                    }
                }

                Result.success(importedCount)
            } ?: Result.failure(Exception("Failed to open PDF file"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
