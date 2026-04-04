package com.devil.taskzio.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.NoCredentialException
import com.devil.taskzio.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
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
            .setAutoSelectEnabled(true)
            .build()
    }

    /**
     * Builds a request for Google Drive authorization.
     * This is requested only when the user enables Cloud Backup.
     */
    fun getDriveAuthorizationRequest(): AuthorizationRequest {
        val scopes = listOf(Scope(DriveScopes.DRIVE_APPDATA))
        return AuthorizationRequest.Builder()
            .setRequestedScopes(scopes)
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
        val fastOption = getGoogleIdOption(onlyFilterAuthorized = true)
        val fastRequest = GetCredentialRequest.Builder()
            .addCredentialOption(fastOption)
            .build()
        
        return try {
            val result = credentialManager.getCredential(activity, fastRequest)
            processCredentialResult(result)
        } catch (e: NoCredentialException) {
            val fullOption = getGoogleIdOption(onlyFilterAuthorized = false)
            val fullRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fullOption)
                .build()
            try {
                val result = credentialManager.getCredential(activity, fullRequest)
                processCredentialResult(result)
            } catch (e2: Exception) {
                null
            }
        } catch (e: Exception) {
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
