package zouitel.neighborhood.speechtranscription.engine

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zouitel.neighborhood.plugin.TranscriptSegment
import zouitel.neighborhood.plugin.TranscriptionEngine
import zouitel.neighborhood.plugin.TranscriptionResult
import zouitel.neighborhood.speechtranscription.model.ModelStorage

class WhisperEngine(context: Context) : TranscriptionEngine {

    private val storage = ModelStorage(context.applicationContext)

    override suspend fun transcribe(
        modelId: String,
        samples: FloatArray,
        language: String
    ): TranscriptionResult {
        require(samples.isNotEmpty()) { "Audio contains no samples." }
        require(language.isNotBlank()) { "A language code is required." }

        val model = storage.modelFile(modelId)
        check(model.isFile) { "The selected Whisper model is not installed." }

        val response = withContext(Dispatchers.Default) {
            WhisperNative.transcribe(model.absolutePath, samples, language)
        }
        val json = JSONObject(response)
        check(!json.has("error")) { json.optString("error", "Whisper transcription failed.") }

        val segmentsJson = json.getJSONArray("segments")
        val segments = buildList {
            for (index in 0 until segmentsJson.length()) {
                val item = segmentsJson.getJSONObject(index)
                add(
                    TranscriptSegment(
                        text = item.getString("text"),
                        startMs = item.getLong("startMs"),
                        endMs = item.getLong("endMs")
                    )
                )
            }
        }
        return TranscriptionResult(
            language = json.optString("language").takeIf(String::isNotBlank),
            segments = segments
        )
    }
}
