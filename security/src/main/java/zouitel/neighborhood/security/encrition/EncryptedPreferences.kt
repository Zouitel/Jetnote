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
 * AES-256-GCM encryption backed by Android Keystore.
 *
 * Decryption errors are not converted to empty strings: callers must decide
 * whether to recover, migrate legacy values, or surface a data-integrity error.
 */
class EncryptedPreferences(context: Context) {

    private companion object {
        const val KEY_ALIAS = "jetnote_settings_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE = 128
        const val MIN_CIPHERTEXT_SIZE = IV_SIZE + TAG_SIZE / 8
    }

    private val secretKey: SecretKey = getOrCreateKey()

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encryptString(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decryptString(encryptedData: String): String {
        val combined = try {
            Base64.decode(encryptedData, Base64.NO_WRAP)
        } catch (error: IllegalArgumentException) {
            throw EncryptedValueException("Encrypted value is not valid Base64", error)
        }

        require(combined.size >= MIN_CIPHERTEXT_SIZE) {
            "Encrypted value is truncated or has an unsupported format"
        }

        return try {
            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (error: Exception) {
            throw EncryptedValueException(
                "Unable to decrypt value; the data may be corrupted or its Keystore key invalidated",
                error
            )
        }
    }

    /** Nullable migration helper; null means the stored value was not decryptable. */
    fun decryptStringOrNull(encryptedData: String): String? =
        runCatching { decryptString(encryptedData) }.getOrNull()

    fun encryptBoolean(value: Boolean): String = encryptString(value.toString())

    fun decryptBoolean(encryptedData: String, defaultValue: Boolean = false): Boolean =
        decryptString(encryptedData).toBooleanStrictOrNull() ?: defaultValue
}

class EncryptedValueException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
