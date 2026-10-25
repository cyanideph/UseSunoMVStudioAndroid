package com.usesuno.mvstudio.domain.audio

import com.usesuno.mvstudio.domain.model.AudioFrame
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AudioAnalyzerTest {

    @Test
    fun testAnalyzeSilence() {
        val analyzer = AudioAnalyzer()
        val silence = FloatArray(44100) // 1 second of silence

        val result = analyzer.analyze(silence)

        assertEquals(44100, result.sampleRate)
        assertEquals(1.0, result.duration, 0.1)
        assertEquals(30, result.frames.size) // 30fps for 1 second

        for (frame in result.frames) {
            assertEquals(0f, frame.bass, 0.01f)
            assertEquals(0f, frame.lowMid, 0.01f)
            assertEquals(0f, frame.mid, 0.01f)
            assertEquals(0f, frame.vocal, 0.01f)
            assertEquals(0f, frame.treble, 0.01f)
            assertEquals(0f, frame.power, 0.01f)
        }
    }

    @Test
    fun testAnalyzeTone() {
        val analyzer = AudioAnalyzer()
        val sampleRate = 44100
        val duration = 1.0
        val frequency = 440.0 // A4 note
        val samples = (sampleRate * duration).toInt()
        val tone = FloatArray(samples) { i ->
            (Math.sin(2 * Math.PI * frequency * i / sampleRate) * 0.5).toFloat()
        }

        val result = analyzer.analyze(tone)

        assertEquals(sampleRate, result.sampleRate)
        assertEquals(duration, result.duration, 0.1)

        // Should have energy in mid frequency range
        var hasMidEnergy = false
        for (frame in result.frames) {
            if (frame.mid > 0.1 || frame.power > 0.1) {
                hasMidEnergy = true
                break
            }
        }
        assertTrue("Should detect mid frequency energy", hasMidEnergy)
    }

    @Test
    fun testExtractBands() {
        val analyzer = AudioAnalyzer()
        // Create a signal with known frequency content
        val sampleRate = 44100
        val bassFreq = 60.0
        val samples = 44100
        val signal = FloatArray(samples) { i ->
            (Math.sin(2 * Math.PI * bassFreq * i / sampleRate) * 0.5).toFloat()
        }

        val result = analyzer.analyze(signal)

        // Bass band should have highest energy
        var maxBass = 0f
        for (frame in result.frames) {
            maxBass = kotlin.math.max(maxBass, frame.bass)
        }
        assertTrue("Bass band should have energy", maxBass > 0.1)
    }
}