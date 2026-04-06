package com.planora.app.core.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.planora.app.core.data.database.PlanoraDatabase
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles completely offline data export and import directly using URI content streams.
 * Uses military-grade AES-GCM encryption with PBKDF2 keys derived from a user password.
 */
@Singleton
class OfflineBackupManager @Inject constructor(
    private val encryptionManager: LocalEncryptionManager
) {

    /**
     * Aggregates database and preferences, serializes to JSON, encrypts using the given password,
     * and streams it out to the provided destination URI (e.g. downloaded .pla file).
     */
    suspend fun exportOfflineBackup(
        context: Context,
        database: PlanoraDatabase,
        prefsManager: PrefsManager,
        destinationUri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val syncDao = database.syncDao()

            val syncData = CloudSyncData(
                exportDateMs = System.currentTimeMillis(),
                tasks = syncDao.getAllTasks(),
                transactions = syncDao.getAllTransactions(),
                savingsGoals = syncDao.getAllSavingsGoals(),
                calendarEvents = syncDao.getAllCalendarEvents(),
                notes = syncDao.getAllNotes(),
                preferences = SyncPreferences(
                    themeName = prefsManager.appTheme.first().name,
                    notificationsEnabled = prefsManager.notificationsEnabled.first(),
                    currencySymbol = prefsManager.currencySymbol.first(),
                    userName = prefsManager.userName.first()
                )
            )

            val jsonString = Gson().toJson(syncData)

            // Encrypt
            val encryptedBytes = encryptionManager.encrypt(jsonString, password)

            // Write to local Storage Access Framework URI stream
            context.contentResolver.openOutputStream(destinationUri)?.use { stream ->
                stream.write(encryptedBytes)
            } ?: throw FileNotFoundException("Could not open output stream for URI: $destinationUri")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads bytes from the given local URI, decrypts the AES-GCM payload using the given password,
     * and parses the resulting JSON to restore into the active database.
     */
    suspend fun importOfflineBackup(
        context: Context,
        database: PlanoraDatabase,
        prefsManager: PrefsManager,
        sourceUri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Read Encrypted Bytes
            val encryptedBytes = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                stream.readBytes()
            } ?: throw FileNotFoundException("Could not open input stream for URI: $sourceUri")

            // Decrypt
            val jsonString = encryptionManager.decrypt(encryptedBytes, password)

            // Parse
            val syncData = Gson().fromJson(jsonString, CloudSyncData::class.java)
                ?: throw IllegalArgumentException("Backup data payload is corrupted.")

            // Apply Database (atomic transaction)
            database.syncDao().applySyncData(
                tasks = syncData.tasks,
                transactions = syncData.transactions,
                goals = syncData.savingsGoals,
                events = syncData.calendarEvents,
                notes = syncData.notes
            )

            // Restore Preferences
            prefsManager.setAppTheme(AppTheme.valueOf(syncData.preferences.themeName))
            prefsManager.setNotificationsEnabled(syncData.preferences.notificationsEnabled)
            prefsManager.setCurrencySymbol(syncData.preferences.currencySymbol)
            prefsManager.setUserName(syncData.preferences.userName)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
