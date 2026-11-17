package zouitel.neighborhood.ai.embedding

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Installs the small text-only EmbeddingGemma 2 model into app-private storage.
 * The model is downloaded once; all subsequent inference is fully on-device.
 */
class EmbeddingGemmaModelInstaller(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .callTimeout(10, TimeUnit.MINUTES)
        .build(),
) {
    private val modelDirectory = File(context.filesDir, "ai/embeddings")
    val modelFile: File
        get() = File(modelDirectory, MODEL_FILE_NAME)

    val isInstalled: Boolean
        get() = modelFile.isFile && modelFile.length() >= MIN_MODEL_BYTES

    suspend fun install(): File = withContext(Dispatchers.IO) {
        if (isInstalled) return@withContext modelFile

        check(modelDirectory.exists() || modelDirectory.mkdirs()) {
            "Unable to create the local embedding model directory."
        }
        val temporaryFile = File(modelDirectory, "$MODEL_FILE_NAME.download")
        temporaryFile.delete()

        val request = Request.Builder()
            .url(MODEL_URL)
            .header("User-Agent", "Jetnote-Android")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Embedding model download failed: HTTP ${response.code}.")
                }
                val body = response.body
                    ?: throw IOException("Embedding model download returned an empty body.")
                if (body.contentLength() in 1L until MIN_MODEL_BYTES) {
                    throw IOException("Embedding model download is smaller than expected.")
                }

                body.byteStream().use { input ->
                    FileOutputStream(temporaryFile).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
            }

            if (temporaryFile.length() < MIN_MODEL_BYTES) {
                throw IOException("Downloaded embedding model is incomplete.")
            }
            if (modelFile.exists() && !modelFile.delete()) {
                throw IOException("Unable to replace the previous embedding model.")
            }
            if (!temporaryFile.renameTo(modelFile)) {
                throw IOException("Unable to move the embedding model into place.")
            }
            modelFile
        } catch (error: Throwable) {
            temporaryFile.delete()
            throw error
        }
    }

    companion object {
        const val MODEL_FILE_NAME = "embeddinggemma-2-text-270m.litertlm"
        const val MODEL_URL =
            "https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm/resolve/main/embeddinggemma-2-text-270m.litertlm"
        private const val MIN_MODEL_BYTES = 150L * 1024L * 1024L
    }
}
