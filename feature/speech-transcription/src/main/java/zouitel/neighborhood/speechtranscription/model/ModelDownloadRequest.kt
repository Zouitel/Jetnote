package zouitel.neighborhood.speechtranscription.model

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf

/**
 * Creates a resumable, network-constrained model download request.
 * Callers must populate these fields from the trusted model catalog.
 */
object ModelDownloadRequest {

    fun create(
        modelId: String,
        httpsUrl: String,
        expectedBytes: Long,
        sha256: String
    ): OneTimeWorkRequest {
        require(modelId.matches(Regex("[A-Za-z0-9._-]{1,80}")))
        require(httpsUrl.startsWith("https://", ignoreCase = true))
        require(expectedBytes > 0L)
        require(sha256.matches(Regex("[a-fA-F0-9]{64}")))

        return OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                workDataOf(
                    ModelDownloadWorker.KEY_MODEL_ID to modelId,
                    ModelDownloadWorker.KEY_URL to httpsUrl,
                    ModelDownloadWorker.KEY_EXPECTED_BYTES to expectedBytes,
                    ModelDownloadWorker.KEY_SHA256 to sha256.lowercase()
                )
            )
            .build()
    }
}
