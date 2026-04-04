package com.devil.taskzio.data.backup

import android.accounts.Account
import android.content.Context
import com.devil.taskzio.data.database.TaskzioDatabase
import com.devil.taskzio.theme.AppTheme
import com.devil.taskzio.utils.PrefsManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles seamless cloud backup by serializing the entire database
 * into a single JSON object. This removes any dependency on rigid SQLite files
 * and eliminates native C++ crashes during restores.
 */
@Singleton
class CloudBackupManager @Inject constructor() {
    
    companion object {
        private const val CLOUD_FILE_NAME = "taskzio_data_sync.json"
    }

    private fun getDriveService(context: Context, email: String): Drive {
        if (email.isEmpty()) {
            throw IllegalStateException("User email not found. Please sign in again.")
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { 
            selectedAccount = Account(email, "com.google")
        }
        
        Log.d("TaskzioBackup", "Initializing Drive service for: $email")

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Taskzio Data Sync").build()
    }

    /**
     * Dumps all tables into a JSON file and pushes it to Drive.
     */
    suspend fun backupToCloud(
        context: Context, 
        database: TaskzioDatabase,
        prefsManager: PrefsManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val email = prefsManager.userEmail.first()
            val syncDao = database.syncDao()
            
            // 1. Gather all current state
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

            // 2. Serialize to JSON
            val jsonString = Gson().toJson(syncData)
            val tempFile = File(context.cacheDir, "export_temp.json")
            tempFile.writeText(jsonString)

            // 3. Connect to Drive and find existing backup
            val drive = getDriveService(context, email)
            val fileList = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$CLOUD_FILE_NAME'")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id
            val fileContent = FileContent("application/json", tempFile)

            // 4. Update or Create
            if (existingFileId != null) {
                drive.files().update(existingFileId, null, fileContent).execute()
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = CLOUD_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(fileMetadata, fileContent).execute()
            }

            tempFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            
            val errorMessage = if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                "Google Drive API Error (${e.statusCode}): ${e.details?.message ?: e.message}"
            } else {
                "${e::class.java.simpleName}: ${e.message ?: "sync error"}"
            }
            
            Log.e("TaskzioBackup", "CRITICAL FAILURE: $errorMessage")
            Result.failure(e)
        }
    }

    /**
     * Downloads the latest JSON bundle and restores it into the active database.
     */
    suspend fun restoreFromCloud(
        context: Context,
        database: TaskzioDatabase,
        prefsManager: PrefsManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val email = prefsManager.userEmail.first()
            val drive = getDriveService(context, email)
            
            // 1. Find the backup file
            val fileList = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$CLOUD_FILE_NAME'")
                .execute()

            val backupFile = fileList.files.firstOrNull() 
                ?: throw FileNotFoundException("No cloud backup found on your Drive.")

            // 2. Download and read JSON
            val tempFile = File(context.cacheDir, "import_temp.json")
            FileOutputStream(tempFile).use { outputStream ->
                drive.files().get(backupFile.id).executeMediaAndDownloadTo(outputStream)
            }
            
            val jsonString = tempFile.readText()
            val syncData = Gson().fromJson(jsonString, CloudSyncData::class.java)
                ?: throw IllegalArgumentException("Backup data is corrupted.")

            // 3. Apply to Database within a single atomic transaction 
            database.syncDao().applySyncData(
                tasks = syncData.tasks,
                transactions = syncData.transactions,
                goals = syncData.savingsGoals,
                events = syncData.calendarEvents,
                notes = syncData.notes
            )

            // 4. Restore Preferences
            prefsManager.setAppTheme(AppTheme.valueOf(syncData.preferences.themeName))
            prefsManager.setNotificationsEnabled(syncData.preferences.notificationsEnabled)
            prefsManager.setCurrencySymbol(syncData.preferences.currencySymbol)
            prefsManager.setUserName(syncData.preferences.userName)

            tempFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
