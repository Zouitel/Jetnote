package zouitel.neighborhood.ai.embedding

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.EmbeddingEngine
import com.google.ai.edge.litertlm.EmbeddingEngineConfig
import com.google.ai.edge.litertlm.InputData
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local EmbeddingGemma 2 text provider backed directly by LiteRT-LM.
 *
 * The model must first be installed by [EmbeddingGemmaModelInstaller]. Inference
 * runs on the CPU for broad Android compatibility and never makes a network
 * request. The engine is initialized lazily and reused across searches.
 */
class EmbeddingGemmaProvider(
    private val modelFile: File,
) : TextEmbeddingProvider, AutoCloseable {
    private val lock = Any()

    @Volatile
    private var engine: EmbeddingEngine? = null

    private val modelDigest: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        require(modelFile.isFile) { "EmbeddingGemma model has not been installed." }
        val digest = MessageDigest.getInstance("SHA-256")
        modelFile.inputStream().buffered().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
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
                val result = getEngine().computeEmbedding(
                    listOf(InputData.Text("$instruction${text.trim()}"))
                )
                val values = result.embedding.toFloatArray()
                check(values.size == EMBEDDING_DIMENSIONS) {
                    "EmbeddingGemma returned an unexpected vector dimension."
                }
                EmbeddingVector(modelId = model.id, values = values)
            }
        }
    }

    private fun getEngine(): EmbeddingEngine {
        engine?.let { return it }
        check(modelFile.isFile) { "EmbeddingGemma model has not been installed." }

        val instance = EmbeddingEngine(
            EmbeddingEngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
            )
        )
        instance.initialize()
        engine = instance
        return instance
    }

    override fun close() {
        synchronized(lock) {
            engine?.close()
            engine = null
        }
    }

    companion object {
        const val MODEL_ID = "embeddinggemma-2-text-270m"
        const val EMBEDDING_DIMENSIONS = 768
        const val MAX_SEQUENCE_LENGTH = 2048
        const val DOCUMENT_PREFIX = "task: search result | text: "
        const val QUERY_PREFIX = "task: search query | text: "
    }
}
