package com.usesuno.mvstudio.domain.audio

import com.usesuno.mvstudio.domain.model.AudioAnalysis

/**
 * Waveform extraction for timeline display
 */
object WaveformExtractor {
    /**
     * Extract waveform peaks for timeline visualization
     * Returns normalized peaks (0.0 to 1.0) at regular intervals
     */
    fun extractWaveform(audioData: FloatArray, numPoints: Int = 2000): List<Float> {
        if (audioData.isEmpty()) return List(numPoints) { 0f }

        val pointsPerSample = audioData.size.toDouble() / numPoints
        val peaks = mutableListOf<Float>()

        for (i in 0 until numPoints) {
            val start = (i * pointsPerSample).toInt()
            val end = min(((i + 1) * pointsPerSample).toInt(), audioData.size)
            var maxVal = 0f

            for (j in start until end) {
                maxVal = max(maxVal, abs(audioData[j]))
            }
            peaks.add(maxVal)
        }

        // Normalize to 0-1
        val globalMax = peaks.maxOrNull() ?: 1f
        return if (globalMax > 0) peaks.map { it / globalMax } else peaks
    }

    /**
     * Extract RMS waveform for more accurate visualization
     */
    fun extractRmsWaveform(audioData: FloatArray, numPoints: Int = 2000): List<Float> {
        if (audioData.isEmpty()) return List(numPoints) { 0f }

        val pointsPerSample = audioData.size.toDouble() / numPoints
        val rmsValues = mutableListOf<Float>()

        for (i in 0 until numPoints) {
            val start = (i * pointsPerSample).toInt()
            val end = min(((i + 1) * pointsPerSample).toInt(), audioData.size)
            var sumSq = 0.0
            var count = 0

            for (j in start until end) {
                sumSq += (audioData[j].toDouble() * audioData[j])
                count++
            }

            val rms = if (count > 0) sqrt(sumSq / count) else 0.0
            rmsValues.add(rms.toFloat())
        }

        // Normalize
        val globalMax = rmsValues.maxOrNull() ?: 1f
        return if (globalMax > 0) rmsValues.map { it / globalMax } else rmsValues
    }
}