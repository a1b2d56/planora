package com.planora.app.core.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalEncryptionManager @Inject constructor() {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    private val secureRandom = SecureRandom()

    class WrongPasswordException(message: String) : Exception(message)

    /**
     * Encrypts the payload with the given password.
     * Output format: [16 bytes SALT] + [12 bytes IV] + [Ciphertext containing GCM Tag]
     */
    fun encrypt(payload: String, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val cipherText = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))

        return ByteBuffer.allocate(salt.size + iv.size + cipherText.size)
            .put(salt)
            .put(iv)
            .put(cipherText)
            .array()
    }

    /**
     * Decrypts the raw byte array back into the original payload string.
     * Throws [WrongPasswordException] if decryption fails (e.g., bad password).
     */
    fun decrypt(encryptedData: ByteArray, password: CharArray): String {
        try {
            val byteBuffer = ByteBuffer.wrap(encryptedData)

            val salt = ByteArray(SALT_LENGTH_BYTES)
            byteBuffer.get(salt)

            val iv = ByteArray(IV_LENGTH_BYTES)
            byteBuffer.get(iv)

            val cipherText = ByteArray(byteBuffer.remaining())
            byteBuffer.get(cipherText)

            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val plainText = cipher.doFinal(cipherText)
            return String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            throw WrongPasswordException("Failed to decrypt file. The password may be incorrect or the file corrupted.")
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }
}
