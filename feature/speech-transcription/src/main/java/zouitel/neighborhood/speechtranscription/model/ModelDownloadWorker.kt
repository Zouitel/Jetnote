package zouitel.neighborhood.speechtranscription.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Downloads a model into app-private storage.
 *
 * Input must come from a trusted, versioned model catalog. The worker accepts
 * only HTTPS URLs and verifies both the expected byte count and SHA-256 before
 * publishing the model file.
 */
class ModelDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?.takeIf { MODEL_ID_PATTERN.matches(it) }
            ?: return@withContext failure("Invalid model identifier.")

        val url = inputData.getString(KEY_URL)
            ?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?: return@withContext failure("A trusted HTTPS model URL is required.")

        val expectedBytes = inputData.getLong(KEY_EXPECTED_BYTES, -1L)
        val expectedSha256 = inputData.getString(KEY_SHA256)
            ?.lowercase()
            ?.takeIf { SHA256_PATTERN.matches(it) }

        if (expectedBytes <= 0L || expectedSha256 == null) {
            return@withContext failure("Model size or SHA-256 metadata is invalid.")
        }

        val storage = ModelStorage(applicationContext)
        val destination = storage.modelFile(modelId)
        val partial = storage.partialFile(modelId)
        destination.parentFile?.mkdirs()

        if (destination.isFile &&
            destination.length() == expectedBytes &&
            sha256(destination) == expectedSha256
        ) {
            return@withContext Result.success(
                workDataOf(KEY_MODEL_PATH to destination.absolutePath)
            )
        }

        if (destination.exists()) destination.delete()
        if (partial.length() > expectedBytes) partial.delete()

        val offset = partial.takeIf { it.isFile }?.length() ?: 0L
        val request = Request.Builder()
            .url(url)
            .apply { if (offset > 0L) header("Range", "bytes=$offset-") }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 408 || response.code == 429 || response.code >= 500) {
                    return@withContext Result.retry()
                }
                if (!response.isSuccessful) {
                    partial.delete()
                    return@withContext failure("Model server returned HTTP ${response.code}.")
                }

                val append = offset > 0L && response.code == 206
                if (append) {
                    val contentRange = response.header("Content-Range").orEmpty()
                    if (!contentRange.startsWith("bytes $offset-")) {
                        partial.delete()
                        return@withContext failure("Model server returned an invalid byte range.")
                    }
                } else if (offset > 0L) {
                    // Server ignored Range; restart cleanly rather than appending a full response.
                    partial.delete()
                }

                if (offset == 0L && response.code == 206) {
                    partial.delete()
                    return@withContext failure("Unexpected partial response from model server.")
                }

                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = if (append) {
                    updateDigestFromFile(partial, digest)
                    offset
                } else {
                    0L
                }

                val body = response.body ?: return@withContext failure("Model response was empty.")
                body.byteStream().use { input ->
                    FileOutputStream(partial, append).buffered().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            digest.update(buffer, 0, count)
                            downloaded += count

                            if (downloaded > expectedBytes) {
                                partial.delete()
                                return@withContext failure("Downloaded model exceeded its declared size.")
                            }

                            setProgress(
                                workDataOf(
                                    KEY_BYTES_DOWNLOADED to downloaded,
                                    KEY_TOTAL_BYTES to expectedBytes,
                                    KEY_MODEL_ID to modelId
                                )
                            )
                        }
                        output.flush()
                    }
                }

                if (downloaded != expectedBytes) {
                    return@withContext Result.retry()
                }

                val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                if (actualSha256 != expectedSha256) {
                    partial.delete()
                    return@withContext failure("Model integrity check failed (SHA-256 mismatch).")
                }

                if (!partial.renameTo(destination)) {
                    partial.delete()
                    return@withContext failure("Could not finalize downloaded model.")
                }

                Result.success(workDataOf(KEY_MODEL_PATH to destination.absolutePath))
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (io: IOException) {
            Result.retry()
        } catch (error: Exception) {
            failure(error.message ?: "Model download failed.")
        }
    }

    private fun failure(message: String): Result =
        Result.failure(workDataOf(KEY_ERROR to message))

    private fun updateDigestFromFile(file: File, digest: MessageDigest) {
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        updateDigestFromFile(file, digest)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_URL = "url"
        const val KEY_EXPECTED_BYTES = "expected_bytes"
        const val KEY_SHA256 = "sha256"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_MODEL_PATH = "model_path"
        const val KEY_ERROR = "error"

        private const val BUFFER_SIZE = 64 * 1024
        private val MODEL_ID_PATTERN = Regex("[A-Za-z0-9._-]{1,80}")
        private val SHA256_PATTERN = Regex("[a-f0-9]{64}")

        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(0, TimeUnit.MILLISECONDS)
                .build()
        }
    }
}
