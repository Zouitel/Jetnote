package zouitel.neighborhood.speechtranscription

import zouitel.neighborhood.plugin.FeatureModule

/**
 * Entry point for the on-demand Speech & Transcription feature.
 *
 * The base app loads this class only after Play confirms that the module is
 * installed. Whisper runtime and model assets are added in follow-up changes.
 */
class SpeechTranscriptionFeature : FeatureModule {
    override val id: String = MODULE_NAME
    override val displayName: String = "Speech & Transcription"

    companion object {
        const val MODULE_NAME = "speech-transcription"
    }
}
