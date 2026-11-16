package zouitel.neighborhood.speechtranscription.engine

import androidx.annotation.Keep

@Keep
internal object WhisperNative {
    init {
        System.loadLibrary("whisper_jni")
    }

    /**
     * Runs whisper.cpp against an installed model using mono 16 kHz float PCM.
     * Returns JSON with the detected language and timestamped segments.
     */
    external fun transcribe(
        modelPath: String,
        samples: FloatArray,
        language: String
    ): String
}
