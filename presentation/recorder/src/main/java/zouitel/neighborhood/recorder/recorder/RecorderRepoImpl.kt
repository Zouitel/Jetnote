package zouitel.neighborhood.recorder.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class RecorderRepoImpl(private val context: Context) : RecorderRepo {
    private var recorder: MediaRecorder? = null
    private var outputStream: FileOutputStream? = null

    @Synchronized
    override fun start(outPutFile: File) {
        check(recorder == null) { "A recording is already active" }
        outPutFile.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) { "Unable to create recording directory" }
        }

        val stream = FileOutputStream(outPutFile)
        var mediaRecorder: MediaRecorder? = null
        try {
            val createdRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            mediaRecorder = createdRecorder
            createdRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(stream.fd)
                prepare()
                start()
            }
            outputStream = stream
            recorder = mediaRecorder
        } catch (error: Throwable) {
            runCatching { mediaRecorder?.release() }
            runCatching { stream.close() }
            throw error
        }
    }

    @Synchronized
    override fun stop() {
        val activeRecorder = recorder ?: run {
            closeOutput()
            return
        }
        recorder = null
        try {
            activeRecorder.stop()
        } finally {
            runCatching { activeRecorder.reset() }
            runCatching { activeRecorder.release() }
            closeOutput()
        }
    }

    @Synchronized
    override fun resume() {
        checkNotNull(recorder) { "No active recording to resume" }.resume()
    }

    @Synchronized
    override fun pause() {
        checkNotNull(recorder) { "No active recording to pause" }.pause()
    }

    private fun closeOutput() {
        runCatching { outputStream?.close() }
        outputStream = null
    }
}
