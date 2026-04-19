package com.planora.app.core.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.planora.app.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles identity verification via Credential Manager and Firebase.
 * Following modern Android standards, it separates Authentication (Identity)
 * from Authorization (Google Drive permissions).
 */
@Singleton
class AuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)
    
    // Auto-generated Client ID from Firebase/Google Cloud Console
    private val webClientId = context.getString(R.string.default_web_client_id)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Configuration for the Credential Manager "Sign in with Google" sheet.
     * @param onlyFilterAuthorized If true, only shows accounts that have previously 
     * authorized this app. This is significantly faster for returning users.
     */
    fun getGoogleIdOption(onlyFilterAuthorized: Boolean = false): com.google.android.libraries.identity.googleid.GetGoogleIdOption {
        return com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(onlyFilterAuthorized)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false) // Never skip the UI choice
            .build()
    }

    /** Exchanges the Google ID token for a Firebase Auth session. */
    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    /** 
     * Performs the full Credential Manager flow (Identity layer). 
     * Handles optimized "fast path" for returning users.
     */
    suspend fun getGoogleIdCredential(activity: Activity): GoogleIdTokenCredential? {
        // Force the full picker every time as requested to ensure consideration for all accounts
        val fullOption = getGoogleIdOption(onlyFilterAuthorized = false)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(fullOption)
            .build()
        
        return try {
            val result = credentialManager.getCredential(activity, request)
            processCredentialResult(result)
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            Log.e("AuthManager", "Credential request failed: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("AuthManager", "Unexpected error: ${e.message}")
            null
        }
    }

    private fun processCredentialResult(result: androidx.credentials.GetCredentialResponse): GoogleIdTokenCredential? {
        val credential = result.credential
        return if (credential is CustomCredential && 
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenCredential.createFrom(credential.data)
        } else {
            null
        }
    }

    /** 
     * Requests authorization for the Google Drive AppData scope.
     * Returns the AuthorizationResult which contains either:
     * - An access token (if already authorized)
     * - A PendingIntent (if user consent is needed)
     */
    suspend fun authorizeDrive(activity: Activity): AuthorizationResult? {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
            .build()

        return try {
            val result = Identity.getAuthorizationClient(activity)
                .authorize(request)
                .await()
            Log.d("AuthManager", "Drive auth result: hasResolution=${result.hasResolution()}, token=${result.accessToken?.take(10)}...")
            result
        } catch (e: Exception) {
            Log.e("AuthManager", "Drive authorization failed", e)
            null
        }
    }

    /** Completely signs the user out of Firebase and clears the local credential state. */
    suspend fun signOut() {
        try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Log and suppress sign-out errors to ensure UI doesn't hang
        }
    }
}
