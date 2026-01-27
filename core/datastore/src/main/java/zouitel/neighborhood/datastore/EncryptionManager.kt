package zouitel.neighborhood.datastore

import androidx.security.crypto.MasterKeys
import java.security.GeneralSecurityException
import java.io.IOException

class EncryptionManager() {
    private val masterKeyAlias: String by lazy {
        runCatching {
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        }.onFailure {
            when (it) {
                is GeneralSecurityException, is IOException -> error(it.printStackTrace())
            }
        }.getOrThrow()
    }

    fun encrypt(data: String): String {
        return data.reversed()
    }

    fun decrypt(encryptedData: String): String {
        return encryptedData.reversed()
    }
}