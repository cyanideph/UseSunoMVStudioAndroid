package com.usesuno.mvstudio.domain.audio

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.net.Uri
import android.content.Context
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer

/**
 * Audio decoder using MediaCodec for full PCM extraction
 */
class MediaCodecAudioDecoder {

    /**
     * Decode audio file to mono FloatArray at target sample rate
     */
    suspend fun decodeAudio(
        context: Context,
        uri: Uri,
        targetSampleRate: Int = 44100,
    ): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        try {
            // Find audio track
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                throw IllegalArgumentException("No audio track found")
            }

            extractor.selectTrack(audioTrackIndex)
            val trackFormat = extractor.getTrackFormat(audioTrackIndex)
            val sourceSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // Configure decoder
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)!!
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()

            val outputBufferInfo = MediaCodec.BufferInfo()
            val outputFormat = decoder.outputFormat
            val outputSampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val outputChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val pcmData = java.util.ArrayList<Float>()
            val inputBuffer = ByteBuffer.allocateDirect(16384)
            var sawInputEOS = false
            var sawOutputEOS = false

            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputIndex = decoder.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inputIndex)!!
                        buffer.clear()
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = decoder.dequeueOutputBuffer(outputBufferInfo, 10000)
                if (outputIndex >= 0) {
                    if ((outputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    } else {
                        val buffer = decoder.getOutputBuffer(outputIndex)!!
                        buffer.position(outputBufferInfo.offset)
                        buffer.limit(outputBufferInfo.offset + outputBufferInfo.size)

                        // Convert to float array (assuming 16-bit PCM)
                        val shortBuffer = buffer.asShortBuffer()
                        val shorts = ShortArray(shortBuffer.remaining())
                        shortBuffer.get(shorts)

                        // Convert to mono float (-1.0 to 1.0)
                        if (outputChannels == 2) {
                            for (i in 0 until shorts.size step 2) {
                                val left = shorts[i].toFloat() / 32768.0f
                                val right = if (i + 1 < shorts.size) shorts[i + 1].toFloat() / 32768.0f else 0f
                                pcmData.add((left + right) * 0.5f)
                            }
                        } else {
                            for (s in shorts) {
                                pcmData.add(s.toFloat() / 32768.0f)
                            }
                        }

                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Format changed, handle if needed
                }
            }

            decoder.stop()
            decoder.release()
            extractor.release()

            // Resample if needed
            val result = pcmData.toFloatArray()
            if (outputSampleRate != targetSampleRate) {
                resample(result, outputSampleRate, targetSampleRate)
            } else {
                result
            }
        } catch (e: Exception) {
            extractor.release()
            throw e
        }
    }

    private fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return input

        val ratio = fromRate.toDouble() / toRate
        val outputSize = (input.size / ratio).toInt()
        val output = FloatArray(outputSize)

        for (i in 0 until outputSize) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val frac = srcPos - srcIndex

            if (srcIndex + 1 < input.size) {
                output[i] = input[srcIndex] * (1 - frac) + input[srcIndex + 1] * frac
            } else if (srcIndex < input.size) {
                output[i] = input[srcIndex]
            }
        }

        return output
    }
}