package com.usesuno.mvstudio.domain.audio

import com.usesuno.mvstudio.domain.model.AudioAnalysis
import com.usesuno.mvstudio.domain.model.AudioFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * Playback clock synchronized with audio - ported from audio/clock.ts
 */
class PlaybackClock(
    private val audioAnalysis: AudioAnalysis,
    private val onTimeUpdate: (Double) -> Unit,
    private val onFrameUpdate: (AudioFrame) -> Unit,
) {
    private var _currentTime = MutableStateFlow(0.0)
    val currentTime: StateFlow<Double> = _currentTime

    private var _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var _playbackRate = MutableStateFlow(1.0f)
    val playbackRate: StateFlow<Float> = _playbackRate

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate + kotlinx.coroutines.SupervisorJob())
    private var animationFrameJob: kotlinx.coroutines.Job? = null
    private var lastFrameTime = 0L
    private var currentFrameIndex = 0

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        lastFrameTime = System.nanoTime()
        animationFrameJob = scope.launch {
            loop()
        }
    }

    fun pause() {
        _isPlaying.value = false
        animationFrameJob?.cancel()
        animationFrameJob = null
    }

    fun seek(time: Double) {
        _currentTime.value = time.coerceIn(0.0, audioAnalysis.duration)
        currentFrameIndex = (time * audioAnalysis.frames.size / audioAnalysis.duration).toInt().coerceIn(0, audioAnalysis.frames.size - 1)
        onTimeUpdate(_currentTime.value)
        if (currentFrameIndex < audioAnalysis.frames.size) {
            onFrameUpdate(audioAnalysis.frames[currentFrameIndex])
        }
    }

    fun setPlaybackRate(rate: Float) {
        _playbackRate.value = rate.coerceIn(0.1f, 4.0f)
    }

    private suspend fun loop() {
        while (_isPlaying.value) {
            val now = System.nanoTime()
            val delta = (now - lastFrameTime) / 1_000_000_000.0 * _playbackRate.value
            lastFrameTime = now

            val newTime = (_currentTime.value + delta).coerceIn(0.0, audioAnalysis.duration)
            _currentTime.value = newTime
            onTimeUpdate(newTime)

            val targetFrameIndex = (newTime * audioAnalysis.frames.size / audioAnalysis.duration).toInt()
            if (targetFrameIndex != currentFrameIndex && targetFrameIndex < audioAnalysis.frames.size) {
                currentFrameIndex = targetFrameIndex
                onFrameUpdate(audioAnalysis.frames[currentFrameIndex])
            }

            if (newTime >= audioAnalysis.duration) {
                pause()
                break
            }

            // Yield to next frame
            kotlinx.coroutines.delay(16) // ~60fps UI update
        }
    }

    fun getFrameAtTime(time: Double): AudioFrame? {
        val index = (time * audioAnalysis.frames.size / audioAnalysis.duration).toInt()
        return if (index >= 0 && index < audioAnalysis.frames.size) {
            audioAnalysis.frames[index]
        } else null
    }

    fun getAudioValueAtTime(time: Double, band: AudioBand): Float {
        val frame = getFrameAtTime(time)
        return frame?.let { getBandValue(it, band) } ?: 0f
    }

    private fun getBandValue(frame: AudioFrame, band: AudioBand): Float = when (band) {
        AudioBand.BASS -> frame.bass
        AudioBand.LOW_MID -> frame.lowMid
        AudioBand.MID -> frame.mid
        AudioBand.VOCAL -> frame.vocal
        AudioBand.TREBLE -> frame.treble
        AudioBand.POWER -> frame.power
    }

    fun dispose() {
        pause()
        scope.cancel()
    }
}

enum class AudioBand {
    BASS, LOW_MID, MID, VOCAL, TREBLE, POWER
}