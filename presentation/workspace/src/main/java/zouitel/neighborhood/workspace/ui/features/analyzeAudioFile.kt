package zouitel.neighborhood.workspace.ui.features

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

suspend fun audioAmplitudes(context: Context, audioUri: Uri, callback: (List<Float>) -> Unit) = withContext(Dispatchers.IO) {
    val amplitudes = mutableListOf<Float>()
    var extractor: MediaExtractor? = null
    var codec: MediaCodec? = null
    val timeOutUs = 1000L

    try {
        extractor = MediaExtractor()
        extractor.setDataSource(context, audioUri, null)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val currentFormat = extractor.getTrackFormat(i)
            val mime = currentFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                trackIndex = i
                format = currentFormat
                break
            }
        }

        if (trackIndex == -1 || format == null) {
            return@withContext emptyList<Float>()
        }

        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        var codecOutputBuffers = codec.outputBuffers // Deprecated

        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var sawOutputEOS = false

        // --- Decoding Loop ---
        while (!sawOutputEOS) {
            if (!sawInputEOS) {
                val inputBufIndex = codec.dequeueInputBuffer(timeOutUs)
                if (inputBufIndex >= 0) {
                    val inputBuf = codec.getInputBuffer(inputBufIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuf, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEOS = true
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0)
                        extractor.advance()
                    }
                }
            }

            // Process output data
            val outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, timeOutUs)
            if (outputBufIndex >= 0) {
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    codec.releaseOutputBuffer(outputBufIndex, false)
                    continue
                }

                if (bufferInfo.size > 0) {
                    val outputBuf = codec.getOutputBuffer(outputBufIndex)!!
                    outputBuf.position(bufferInfo.offset)
                    outputBuf.limit(bufferInfo.offset + bufferInfo.size)

                    // --- Process PCM NotificationData ---
                    // Assuming 16-bit PCM (common)
                    val shortBuffer = outputBuf.asShortBuffer()
                    var sumSquare = 0.0
                    var peak = 0
                    val count = shortBuffer.remaining()

                    if (count > 0) {
                         for (i in 0 until count) {
                             val sample = shortBuffer.get(i).toInt()
                             sumSquare += sample * sample
                             val absSample = abs(sample)
                             if (absSample > peak) {
                                 peak = absSample
                             }
                         }
                         val normalizedPeak = peak / 32767.0f
                         amplitudes.add(normalizedPeak)
                     }
                }

                codec.releaseOutputBuffer(outputBufIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    sawOutputEOS = true
                }
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                 codecOutputBuffers = codec.outputBuffers
            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                codec.outputFormat
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        runCatching {
            codec?.stop()
            codec?.release()
            extractor?.release()
        }
    }

    println("Analysis complete. Got ${amplitudes.size} amplitude points.")
    callback(amplitudes)
}