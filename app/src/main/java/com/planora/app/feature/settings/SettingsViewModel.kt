package com.planora.app.feature.settings

import android.app.Activity
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.planora.app.core.data.auth.AuthManager
import com.planora.app.core.data.backup.CloudBackupManager
import com.planora.app.core.data.database.PlanoraDatabase
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.utils.PrefsManager
import com.planora.app.core.worker.SavingsReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appTheme: AppTheme = AppTheme.MIDNIGHT,
    val notificationsEnabled: Boolean = true,
    val currencySymbol: String = "$",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val isGoogleSignedIn: Boolean = false,
    val backupStatus: BackupStatus = BackupStatus.Idle
)

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Loading : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

/** Tracks which cloud operation is pending consent. */
private enum class PendingCloudOp { NONE, BACKUP, RESTORE }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val workManager: WorkManager,
    private val cloudBackupManager: CloudBackupManager,
    private val offlineBackupManager: com.planora.app.core.data.backup.OfflineBackupManager,
    val authManager: AuthManager,
    private val database: PlanoraDatabase
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    
    /** Emits an IntentSenderRequest when Drive consent is needed. The UI observes this. */
    private val _driveConsentRequired = MutableSharedFlow<IntentSenderRequest>()
    val driveConsentRequired: SharedFlow<IntentSenderRequest> = _driveConsentRequired.asSharedFlow()
    
    private var pendingOp = PendingCloudOp.NONE

    // Combine preferences first
    private val prefsState = combine(
        prefsManager.appTheme,
        prefsManager.notificationsEnabled,
        prefsManager.currencySymbol,
        prefsManager.userName,
        prefsManager.userPhotoUrl
    ) { theme, notifications, currency, name, photo ->
        SettingsUiState(
            appTheme = theme, 
            notificationsEnabled = notifications, 
            currencySymbol = currency, 
            userName = name,
            userPhotoUrl = photo
        )
    }

    // Then combine with dynamic status flows
    val uiState: StateFlow<SettingsUiState> = combine(
        prefsState,
        _backupStatus
    ) { partialState, backupStatus ->
        partialState.copy(
            isGoogleSignedIn = authManager.currentUser != null,
            backupStatus = backupStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setAppTheme(theme: AppTheme)          = viewModelScope.launch { prefsManager.setAppTheme(theme) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsManager.setNotificationsEnabled(enabled)
        if (enabled) SavingsReminderWorker.schedule(workManager) else SavingsReminderWorker.cancel(workManager)
    }
    fun setCurrencySymbol(symbol: String)     = viewModelScope.launch { prefsManager.setCurrencySymbol(symbol) }
    fun setUserName(name: String)             = viewModelScope.launch { prefsManager.setUserName(name) }

    /** Refreshes UI based on the current Firebase user and stored preferences */
    fun refreshAuthStatus() {
        val user = authManager.currentUser
        if (user != null) {
            viewModelScope.launch {
                user.photoUrl?.let { prefsManager.setUserPhotoUrl(it.toString()) }

            }
        }
    }

    /**
     * Gets a Drive access token, handling the consent flow if needed.
     * Returns the token string, or null if authorization failed or consent is pending.
     */
    private suspend fun getDriveToken(activity: Activity): String? {
        val authResult = authManager.authorizeDrive(activity)
        if (authResult == null) {
            _backupStatus.value = BackupStatus.Error("Drive authorization failed. Please sign in again.")
            return null
        }

        if (authResult.hasResolution()) {
            // User hasn't granted Drive consent yet — launch the consent dialog
            Log.d("SettingsVM", "Drive consent needed, launching resolution intent...")
            authResult.pendingIntent?.intentSender?.let { sender ->
                _driveConsentRequired.emit(
                    IntentSenderRequest.Builder(sender).build()
                )
            }
            return null // Operation will be retried after consent
        }

        val token = authResult.accessToken
        if (token.isNullOrBlank()) {
            _backupStatus.value = BackupStatus.Error("Drive authorization succeeded but no token was returned.")
            return null
        }
        return token
    }

    /** Triggers a cloud backup upload */
    fun backupToCloud(activity: Activity) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Loading
        pendingOp = PendingCloudOp.BACKUP

        val token = getDriveToken(activity) ?: return@launch

        cloudBackupManager.backupToCloud(activity, database, prefsManager, token).onSuccess {
            _backupStatus.value = BackupStatus.Success("Backup successfully synced to Google Drive!")
        }.onFailure {
            _backupStatus.value = BackupStatus.Error(it.message ?: "Failed to upload backup")
        }
    }

    /** Triggers a cloud backup restore */
    fun restoreFromCloud(activity: Activity) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Loading
        pendingOp = PendingCloudOp.RESTORE

        val token = getDriveToken(activity) ?: return@launch

        cloudBackupManager.restoreFromCloud(activity, database, prefsManager, token).onSuccess {
            _backupStatus.value = BackupStatus.Success("Restore complete! Settings and data have been applied instantly.")
        }.onFailure {
            _backupStatus.value = BackupStatus.Error(it.message ?: "Failed to restore backup")
        }
    }

    /** Triggers a local offline encrypted backup */
    fun exportToOfflineBackup(context: android.content.Context, destinationUri: android.net.Uri, password: CharArray) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Loading
        offlineBackupManager.exportOfflineBackup(context, database, prefsManager, destinationUri, password).onSuccess {
            password.fill('\u0000') // Clear password array from memory
            _backupStatus.value = BackupStatus.Success("Offline backup successfully exported and encrypted!")
        }.onFailure {
            password.fill('\u0000')
            _backupStatus.value = BackupStatus.Error(it.message ?: "Failed to export offline backup")
        }
    }

    /** Triggers a local offline encrypted restore */
    fun importFromOfflineBackup(context: android.content.Context, sourceUri: android.net.Uri, password: CharArray) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Loading
        offlineBackupManager.importOfflineBackup(context, database, prefsManager, sourceUri, password).onSuccess {
            password.fill('\u0000') // Clear password array from memory
            _backupStatus.value = BackupStatus.Success("Offline restore complete! Settings and data have been applied instantly.")
        }.onFailure {
            password.fill('\u0000')
            _backupStatus.value = BackupStatus.Error(it.message ?: "Failed to decrypt or restore backup")
        }
    }

    /** Called after the user grants Drive consent via the system dialog. */
    fun retryPendingCloudOperation(activity: Activity) {
        when (pendingOp) {
            PendingCloudOp.BACKUP -> backupToCloud(activity)
            PendingCloudOp.RESTORE -> restoreFromCloud(activity)
            PendingCloudOp.NONE -> {}
        }
        pendingOp = PendingCloudOp.NONE
    }

    /** Signs in using the unified Credential Manager flow */
    fun signInWithGoogle(activity: Activity, onComplete: () -> Unit = {}) = viewModelScope.launch {
        val googleCredential = authManager.getGoogleIdCredential(activity)
        if (googleCredential != null) {
            try {
                authManager.signInWithGoogle(googleCredential.idToken)
                
                // Pre-authorize Drive scope (will show consent if needed)
                authManager.authorizeDrive(activity)
                
                // Capture first name for personal touch if no name exists
                val firstRunName = googleCredential.displayName?.substringBefore(" ")
                if (firstRunName != null && (prefsManager.userName.first().isBlank() || prefsManager.userName.first() == "User")) {
                    prefsManager.setUserName(firstRunName)
                }
                
                refreshAuthStatus()
                onComplete()
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus.Error("Authentication failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
            }
        } else {
            // Silence silent failures (e.g. user canceled), but ensure loading stops
            onComplete()
        }
    }

    /** Signs out and clears locally stored identity data */
    fun signOut() = viewModelScope.launch {
        authManager.signOut()

        prefsManager.setUserPhotoUrl("")
    }

    fun clearBackupStatus() {
        if (_backupStatus.value is BackupStatus.Success || _backupStatus.value is BackupStatus.Error) {
            _backupStatus.value = BackupStatus.Idle
        }
    }
}
