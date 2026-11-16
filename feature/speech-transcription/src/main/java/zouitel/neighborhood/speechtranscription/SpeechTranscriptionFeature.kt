package zouitel.neighborhood.speechtranscription

import android.content.Context
import zouitel.neighborhood.plugin.FeatureModule
import zouitel.neighborhood.plugin.TranscriptionEngine
import zouitel.neighborhood.plugin.TranscriptionFeatureModule
import zouitel.neighborhood.speechtranscription.engine.WhisperEngine

/**
 * Entry point for the on-demand Speech & Transcription feature.
 *
 * The base app loads this class only after Play confirms that the module is
 * installed. Model weights remain separate, user-selected downloads.
 */
class SpeechTranscriptionFeature : TranscriptionFeatureModule {
    override val id: String = MODULE_NAME
    override val displayName: String = "Speech & Transcription"

    override fun createTranscriptionEngine(context: Context): TranscriptionEngine =
        WhisperEngine(context.applicationContext)

    companion object {
        const val MODULE_NAME = "speech-transcription"
    }
}
