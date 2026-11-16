package zouitel.neighborhood.plugin

/**
 * Timestamped output shared by audio-note transcription and video captions.
 * Times are milliseconds from the start of the source audio.
 */
data class TranscriptSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

data class TranscriptionResult(
    val language: String?,
    val segments: List<TranscriptSegment>
) {
    val text: String
        get() = segments.joinToString(separator = " ") { it.text }.trim()
}

/**
 * Receives mono, 16 kHz, normalized float PCM samples. Decoding/resampling
 * media URIs belongs in the Android integration layer, not in the native API.
 */
interface TranscriptionEngine {
    suspend fun transcribe(
        modelId: String,
        samples: FloatArray,
        language: String
    ): TranscriptionResult
}

interface TranscriptionFeatureModule : FeatureModule {
    fun createTranscriptionEngine(context: android.content.Context): TranscriptionEngine
}
