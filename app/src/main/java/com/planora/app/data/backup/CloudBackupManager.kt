package com.planora.app.data.backup

import android.content.Context
import com.planora.app.data.database.PlanoraDatabase
import com.planora.app.theme.AppTheme
import com.planora.app.utils.PrefsManager
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
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
 * into a single JSON object. Uses an access token obtained from 
 * the Identity Authorization API for Drive access.
 */
@Singleton
class CloudBackupManager @Inject constructor() {
    
    companion object {
        private const val TAG = "PlanoraBackup"
        private const val CLOUD_FILE_NAME = "Planora_data_sync.json"
    }

    /**
     * Creates a Drive service using an OAuth2 access token obtained
     * from the Identity Authorization Client. This avoids the stale
     * GoogleAccountCredential path that doesn't share tokens with
     * the modern Identity API.
     */
    private fun getDriveService(accessToken: String): Drive {
        val credentials = GoogleCredentials.create(
            AccessToken(accessToken, null)
        )

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        ).setApplicationName("Planora Data Sync").build()
    }

    /**
     * Dumps all tables into a JSON file and pushes it to Drive.
     */
    suspend fun backupToCloud(
        context: Context, 
        database: PlanoraDatabase,
        prefsManager: PrefsManager,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
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
            val drive = getDriveService(accessToken)
            Log.d(TAG, "Drive service created, searching for existing backup...")
            
            val fileList = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$CLOUD_FILE_NAME'")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id
            val fileContent = FileContent("application/json", tempFile)

            // 4. Update or Create
            if (existingFileId != null) {
                Log.d(TAG, "Updating existing backup: $existingFileId")
                drive.files().update(existingFileId, null, fileContent).execute()
            } else {
                Log.d(TAG, "Creating new backup file...")
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = CLOUD_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                drive.files().create(fileMetadata, fileContent).execute()
            }

            tempFile.delete()
            Log.d(TAG, "Backup completed successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            
            val errorMessage = if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                "Google Drive API Error (${e.statusCode}): ${e.details?.message ?: e.message}"
            } else {
                "${e::class.java.simpleName}: ${e.message ?: "sync error"}"
            }
            
            Log.e(TAG, "BACKUP FAILURE: $errorMessage\n$sw")
            Result.failure(Exception(errorMessage, e))
        }
    }

    /**
     * Downloads the latest JSON bundle and restores it into the active database.
     */
    suspend fun restoreFromCloud(
        context: Context,
        database: PlanoraDatabase,
        prefsManager: PrefsManager,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = getDriveService(accessToken)
            Log.d(TAG, "Drive service created, searching for backup to restore...")
            
            // 1. Find the backup file
            val fileList = drive.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$CLOUD_FILE_NAME'")
                .execute()

            val backupFile = fileList.files.firstOrNull() 
                ?: throw FileNotFoundException("No cloud backup found on your Drive.")

            // 2. Download and read JSON
            Log.d(TAG, "Downloading backup: ${backupFile.id}")
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
            Log.d(TAG, "Restore completed successfully!")
            Result.success(Unit)
        } catch (e: Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            Log.e(TAG, "RESTORE FAILURE: ${e.message}\n$sw")
            Result.failure(Exception(e.message ?: "Restore failed", e))
        }
    }
}
