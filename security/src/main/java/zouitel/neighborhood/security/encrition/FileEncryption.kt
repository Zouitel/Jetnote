package zouitel.neighborhood.security.encrition

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File
import java.security.SecureRandom

/**
 * Encrypted file-backed secret storage.
 *
 * The AndroidX Security Crypto API used here is retained for on-disk compatibility;
 * migration to a replacement format must preserve existing encrypted files.
 */
class FileEncryption(private val context: Context) {
    fun getCrypticPass(): ByteArray {
        val file = File(context.filesDir, PASS)
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return if (file.exists()) {
            encryptedFile.openFileInput().use { it.readBytes() }
        } else {
            generateCryptic().also { cryptic ->
                encryptedFile.openFileOutput().use { it.write(cryptic) }
            }
        }
    }

    private fun generateCryptic(): ByteArray = ByteArray(32).also(SecureRandom()::nextBytes)

    companion object {
        const val PASS = "crypticpass.bin"
    }
}
