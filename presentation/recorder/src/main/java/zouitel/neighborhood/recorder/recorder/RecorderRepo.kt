package zouitel.neighborhood.recorder.recorder

import java.io.File

interface RecorderRepo {
    fun start(outPutFile: File)
    fun stop()
    fun resume()
    fun pause()
}