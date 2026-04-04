@file:Suppress("DEPRECATION")
package com.devil.taskzio.data.database

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the SQLCipher encryption passphrase.
 * Stored in EncryptedSharedPreferences (Keystore-backed).
 */
@Singleton
class DatabaseKeyManager @Inject constructor(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "taskzio_db_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Get or create the passphrase for SQLCipher. */
    fun getPassphrase(): ByteArray {
        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) return stored.decodeHex()

        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit { putString(KEY_PASSPHRASE, passphrase.toHex()) }
        return passphrase
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun String.decodeHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        private const val KEY_PASSPHRASE = "db_passphrase"
    }
}
