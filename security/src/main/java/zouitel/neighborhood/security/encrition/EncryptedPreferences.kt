package zouitel.neighborhood.security.encrition

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles encryption/decryption for DataStore values
 */
class EncryptedPreferences(context: Context) {

    private companion object {
        const val KEY_ALIAS = "jetnote_settings_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE = 128
    }

    private val secretKey: SecretKey = getOrCreateKey()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        // Try to get existing key
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        // Create new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt string value
     */
    fun encryptString(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV + encrypted data
        val combined = iv + encrypted

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt string value
     */
    fun decryptString(encryptedData: String): String {
        try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

            // Split IV and encrypted data
            val iv = combined.copyOfRange(0, IV_SIZE)
            val encrypted = combined.copyOfRange(IV_SIZE, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails, return empty string
            // This handles migration from unencrypted data
            return ""
        }
    }

    /**
     * Encrypt boolean (stored as string)
     */
    fun encryptBoolean(value: Boolean): String {
        return encryptString(value.toString())
    }

    /**
     * Decrypt boolean
     */
    fun decryptBoolean(encryptedData: String, defaultValue: Boolean = false): Boolean {
        return try {
            decryptString(encryptedData).toBooleanStrictOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
}