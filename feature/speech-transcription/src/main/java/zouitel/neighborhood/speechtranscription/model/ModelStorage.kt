package zouitel.neighborhood.speechtranscription.model

import android.content.Context
import java.io.File

/**
 * Owns model files in the app's private files directory. No storage permission
 * is required and downloaded models are not exposed to other apps.
 */
class ModelStorage(context: Context) {

    private val root = File(
        context.applicationContext.filesDir,
        "plugins/speech-transcription/models"
    )

    fun modelFile(modelId: String): File = File(modelDirectory(modelId), "model.bin")

    internal fun partialFile(modelId: String): File =
        File(modelDirectory(modelId), "model.bin.part")

    fun isInstalled(modelId: String): Boolean = modelFile(modelId).isFile

    fun delete(modelId: String): Boolean {
        val directory = modelDirectory(modelId)
        return !directory.exists() || directory.deleteRecursively()
    }

    private fun modelDirectory(modelId: String): File {
        require(MODEL_ID_PATTERN.matches(modelId)) { "Invalid model identifier." }
        return File(root, modelId)
    }

    private companion object {
        val MODEL_ID_PATTERN = Regex("[A-Za-z0-9._-]{1,80}")
    }
}
