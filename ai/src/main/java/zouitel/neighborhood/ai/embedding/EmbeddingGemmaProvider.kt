package zouitel.neighborhood.ai.embedding

import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.retrieval.universalembedder.UniversalEmbedder
import com.google.mediapipe.tasks.retrieval.universalembedder.UniversalEmbedderOptions
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local EmbeddingGemma 2 text provider backed by MediaPipe UniversalEmbedder.
 *
 * The model must first be installed by [EmbeddingGemmaModelInstaller]. Inference
 * is CPU-backed by default for broad device compatibility and never makes a
 * network request.
 */
class EmbeddingGemmaProvider(
    private val modelFile: File,
) : TextEmbeddingProvider, AutoCloseable {
    private val lock = Any()

    @Volatile
    private var embedder: UniversalEmbedder? = null

    private val modelDigest: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        require(modelFile.isFile) { "EmbeddingGemma model has not been installed." }
        MessageDigest.getInstance("SHA-256").digest(modelFile.inputStream().use { input ->
            val digestInput = java.security.DigestInputStream(input, MessageDigest.getInstance("SHA-256"))
            digestInput.copyTo(java.io.OutputStream.nullOutputStream())
            digestInput.messageDigest.digest()
        }).joinToString("") { byte -> "%02x".format(byte) }
    }

    override val model: EmbeddingModel
        get() = EmbeddingModel(
            id = "$MODEL_ID:$modelDigest",
            dimensions = EMBEDDING_DIMENSIONS,
            maxSequenceLength = MAX_SEQUENCE_LENGTH,
            normalizedOutput = true,
        )

    override suspend fun embed(text: String): EmbeddingVector = embedQuery(text)

    override suspend fun embedDocument(text: String): EmbeddingVector =
        embedWithInstruction(DOCUMENT_PREFIX, text)

    override suspend fun embedQuery(text: String): EmbeddingVector =
        embedWithInstruction(QUERY_PREFIX, text)

    private suspend fun embedWithInstruction(
        instruction: String,
        text: String,
    ): EmbeddingVector {
        require(text.isNotBlank()) { "Cannot embed blank text." }
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                val result = getEmbedder().embedText("$instruction${text.trim()}")
                val values = result.embeddings().firstOrNull()?.floatEmbedding()
                    ?: error("EmbeddingGemma returned no text embedding.")
                check(values.size == EMBEDDING_DIMENSIONS) {
                    "EmbeddingGemma returned an unexpected vector dimension."
                }
                EmbeddingVector(modelId = model.id, values = values)
            }
        }
    }

    private fun getEmbedder(): UniversalEmbedder {
        embedder?.let { return it }
        check(modelFile.isFile) { "EmbeddingGemma model has not been installed." }

        val options = UniversalEmbedderOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(modelFile.absolutePath)
                    .build()
            )
            .setTextDelegate(Delegate.CPU)
            .setL2Normalize(true)
            .build()

        return UniversalEmbedder.createFromOptions(modelFile.contextForTask(), options)
            .also { embedder = it }
    }

    override fun close() {
        synchronized(lock) {
            embedder?.close()
            embedder = null
        }
    }

    private fun File.contextForTask(): android.content.Context =
        throw UnsupportedOperationException("Use the Context-based provider constructor.")

    companion object {
        const val MODEL_ID = "embeddinggemma-2-text-270m"
        const val EMBEDDING_DIMENSIONS = 768
        const val MAX_SEQUENCE_LENGTH = 2048
        const val DOCUMENT_PREFIX = "task: search result | text: "
        const val QUERY_PREFIX = "task: search query | text: "
    }
}
