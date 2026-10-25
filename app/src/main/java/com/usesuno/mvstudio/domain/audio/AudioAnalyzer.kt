package com.usesuno.mvstudio.domain.audio

import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.sqrt
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/**
 * Audio analysis ported from UseSunoMVStudio's audio/analysis.ts
 * Performs offline FFT analysis to extract 5 frequency bands + power envelope
 */
class AudioAnalyzer(
    private val sampleRate: Int = 44100,
    private val fftSize: Int = 2048,
    private val hopSize: Int = 512,
    private val targetFps: Int = 30,
) {
    private val fft = DoubleFFT_1D(fftSize.toLong())
    private val window = createHannWindow(fftSize)

    /**
     * Analyze audio buffer and return frequency bands at ~30fps
     * Matches the web implementation: 5 bands (bass, lowMid, mid, vocal, treble) + power
     */
    fun analyze(audioData: FloatArray): AudioAnalysis {
        val duration = audioData.size.toDouble() / sampleRate
        val framesPerSecond = targetFps.toDouble()
        val samplesPerFrame = (sampleRate / framesPerSecond).toInt()
        val numFrames = max(1, (duration * framesPerSecond).toInt())

        val frames = mutableListOf<AudioFrame>()
        val waveform = mutableListOf<Float>()

        // Downsample for waveform (1000 points max)
        val waveformStride = max(1, audioData.size / 1000)
        for (i in 0 until audioData.size step waveformStride) {
            waveform.add(audioData[i])
        }

        val buffer = DoubleArray(fftSize)
        val magnitudes = DoubleArray(fftSize / 2)

        for (frameIndex in 0 until numFrames) {
            val sampleStart = (frameIndex * samplesPerFrame).toInt()
            val sampleEnd = min(sampleStart + fftSize, audioData.size)
            val validSamples = sampleEnd - sampleStart

            // Clear buffer
            buffer.fill(0.0)

            // Copy audio data and apply window
            for (i in 0 until validSamples) {
                buffer[i] = audioData[sampleStart + i].toDouble() * window[i]
            }

            // Perform FFT
            fft.realForward(buffer)

            // Compute magnitudes
            for (i in 0 until fftSize / 2) {
                val real = buffer[2 * i]
                val imag = buffer[2 * i + 1]
                magnitudes[i] = sqrt(real * real + imag * imag)
            }

            // Extract frequency bands
            val frame = extractBands(magnitudes, frameIndex, framesPerSecond)
            frames.add(frame)
        }

        return AudioAnalysis(
            sampleRate = sampleRate,
            duration = duration,
            frames = frames,
            waveform = waveform,
        )
    }

    private fun extractBands(magnitudes: DoubleArray, frameIndex: Int, fps: Double): AudioFrame {
        val time = frameIndex / fps
        val nyquist = sampleRate / 2.0
        val binWidth = nyquist / (fftSize / 2)

        // Frequency band ranges (matching web implementation)
        val bands = listOf(
            20.0 to 150.0,     // bass
            150.0 to 400.0,    // lowMid
            400.0 to 2500.0,   // mid
            2500.0 to 5000.0,  // vocal
            5000.0 to 20000.0, // treble
        )

        val bandValues = bands.map { (lowFreq, highFreq) ->
            val lowBin = max(1, (lowFreq / binWidth).toInt())
            val highBin = min(magnitudes.size - 1, (highFreq / binWidth).toInt())
            var sum = 0.0
            var count = 0
            for (i in lowBin..highBin) {
                sum += magnitudes[i]
                count++
            }
            if (count > 0) sum / count else 0.0
        }

        // Normalize to 0-1 range with dB scaling
        val normalized = bandValues.map { mag ->
            val db = 20.0 * log10(max(mag, 1e-10))
            // Map from -80dB to 0dB -> 0.0 to 1.0
            max(0.0, min(1.0, (db + 80.0) / 80.0)).toFloat()
        }

        // Power envelope (RMS of all bins)
        var powerSum = 0.0
        for (i in 0 until magnitudes.size) {
            powerSum += magnitudes[i] * magnitudes[i]
        }
        val rms = sqrt(powerSum / magnitudes.size)
        val powerDb = 20.0 * log10(max(rms, 1e-10))
        val power = max(0.0, min(1.0, (powerDb + 80.0) / 80.0)).toFloat()

        return AudioFrame(
            time = time,
            bass = normalized[0],
            lowMid = normalized[1],
            mid = normalized[2],
            vocal = normalized[3],
            treble = normalized[4],
            power = power,
        )
    }

    private fun createHannWindow(size: Int): DoubleArray {
        val window = DoubleArray(size)
        for (i in 0 until size) {
            window[i] = 0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))
        }
        return window
    }

    companion object {
        private const val PI = 3.141592653589793

        /**
         * Decode audio file to FloatArray (mono, normalized to -1.0..1.0)
         */
        suspend fun decodeAudioFile(context: android.content.Context, uri: android.net.Uri): FloatArray {
            return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(context, uri)

                // For full decoding, we'd use MediaExtractor + AudioDecoder
                // This is a simplified version - in production use ExoPlayer's decoder
                val audioFormat = android.media.MediaFormat()
                audioFormat.setString(android.media.MediaFormat.KEY_MIME, "audio/raw")
                audioFormat.setInteger(android.media.MediaFormat.KEY_SAMPLE_RATE, 44100)
                audioFormat.setInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT, 1)
                audioFormat.setInteger(android.media.MediaFormat.KEY_BIT_RATE, 128000)

                // Extract raw PCM data using ExoPlayer's ProgressiveMediaExtractor would be better
                // For now, return placeholder - actual implementation needs MediaCodec decoder
                FloatArray(44100 * 60) // 1 minute placeholder
            }
        }
    }
}