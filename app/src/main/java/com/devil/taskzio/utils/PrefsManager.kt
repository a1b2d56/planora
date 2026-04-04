package com.devil.taskzio.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.devil.taskzio.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taskzio_prefs")

@Singleton
class PrefsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val APP_THEME             = stringPreferencesKey("app_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENCY_SYMBOL       = stringPreferencesKey("currency_symbol")
        val USER_NAME             = stringPreferencesKey("user_name")
        val USER_PHOTO_URL        = stringPreferencesKey("user_photo_url")
        val USER_EMAIL            = stringPreferencesKey("user_email")
        val HAS_ONBOARDED         = booleanPreferencesKey("has_onboarded")
    }

    // Helper for safe DataStore flows
    private fun <T> safeFlow(transform: (Preferences) -> T): Flow<T> =
        context.dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map(transform)

    val appTheme: Flow<AppTheme> = safeFlow { prefs ->
        val name = prefs[Keys.APP_THEME] ?: AppTheme.MIDNIGHT.name
        try { AppTheme.valueOf(name) } catch (_: Exception) { AppTheme.MIDNIGHT }
    }
    val notificationsEnabled: Flow<Boolean> = safeFlow { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val currencySymbol: Flow<String>        = safeFlow { it[Keys.CURRENCY_SYMBOL] ?: "$" }
    val userName: Flow<String>              = safeFlow { it[Keys.USER_NAME] ?: "" }
    val userPhotoUrl: Flow<String>          = safeFlow { it[Keys.USER_PHOTO_URL] ?: "" }
    val userEmail: Flow<String>             = safeFlow { it[Keys.USER_EMAIL] ?: "" }
    val hasOnboarded: Flow<Boolean>         = safeFlow { it[Keys.HAS_ONBOARDED] ?: false }

    suspend fun setAppTheme(theme: AppTheme)              = edit { it[Keys.APP_THEME] = theme.name }
    suspend fun setNotificationsEnabled(enabled: Boolean) = edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setCurrencySymbol(symbol: String)         = edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    suspend fun setUserName(name: String)                 = edit { it[Keys.USER_NAME] = name }
    suspend fun setUserPhotoUrl(url: String)              = edit { it[Keys.USER_PHOTO_URL] = url }
    suspend fun setUserEmail(email: String)           = edit { it[Keys.USER_EMAIL] = email }
    suspend fun setHasOnboarded(done: Boolean)            = edit { it[Keys.HAS_ONBOARDED] = done }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
