package com.planora.app.ui.viewmodels

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.planora.app.data.auth.AuthManager
import com.planora.app.data.backup.CloudBackupManager
import com.planora.app.data.database.PlanoraDatabase
import com.planora.app.theme.AppTheme
import com.planora.app.utils.PrefsManager
import com.planora.app.workers.SavingsReminderWorker
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val workManager: WorkManager,
    private val cloudBackupManager: CloudBackupManager,
    val authManager: AuthManager,
    private val database: PlanoraDatabase
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)

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
                user.email?.let { prefsManager.setUserEmail(it) }
            }
        }
    }

    /** Triggers a cloud backup upload */
    fun backupToCloud(context: Context) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Loading
        cloudBackupManager.backupToCloud(context, database, prefsManager).onSuccess {
            _backupStatus.value = BackupStatus.Success("Backup successfully synced to Google Drive!")
        }.onFailure {
            _backupStatus.value = BackupStatus.Error(it.localizedMessage ?: "Failed to upload backup")
        }
    }

    /** Triggers a cloud backup restore */
    fun restoreFromCloud(context: Context) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Loading
        cloudBackupManager.restoreFromCloud(context, database, prefsManager).onSuccess {
            _backupStatus.value = BackupStatus.Success("Restore complete! Settings and data have been applied instantly.")
        }.onFailure {
            _backupStatus.value = BackupStatus.Error(it.localizedMessage ?: "Failed to restore backup")
        }
    }

    /** Signs in using the unified Credential Manager flow */
    fun signInWithGoogle(activity: Activity, onComplete: () -> Unit = {}) = viewModelScope.launch {
        val googleCredential = authManager.getGoogleIdCredential(activity)
        if (googleCredential != null) {
            try {
                authManager.signInWithGoogle(googleCredential.idToken)
                
                // Capture first name for personal touch if no name exists
                val firstRunName = googleCredential.displayName?.substringBefore(" ")
                if (firstRunName != null && prefsManager.userName.first().isBlank()) {
                    prefsManager.setUserName(firstRunName)
                }
                
                refreshAuthStatus()
                onComplete()
            } catch (e: Exception) {
                _backupStatus.value = BackupStatus.Error("Authentication failed: ${e.message}")
            }
        } else {
            // Silence silent failures (e.g. user canceled), but ensure loading stops
            onComplete()
        }
    }

    /** Signs out and clears locally stored identity data */
    fun signOut() = viewModelScope.launch {
        authManager.signOut()
        prefsManager.setUserEmail("")
        prefsManager.setUserPhotoUrl("")
    }

    fun clearBackupStatus() {
        if (_backupStatus.value is BackupStatus.Success || _backupStatus.value is BackupStatus.Error) {
            _backupStatus.value = BackupStatus.Idle
        }
    }
}
