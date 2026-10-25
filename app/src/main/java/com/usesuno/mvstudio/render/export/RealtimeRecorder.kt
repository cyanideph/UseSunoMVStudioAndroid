package com.usesuno.mvstudio.render.export

import android.media.MediaRecorder
import android.media.MediaRecorder.OutputFormat
import android.view.Surface
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.ExportFormat
import com.usesuno.mvstudio.render.engine.RenderEngine
import com.usesuno.mvstudio.render.engine.RendererFactory
import com.usesuno.mvstudio.domain.audio.AudioAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time video recorder using MediaRecorder
 * Fallback for when frame-by-frame export is not available
 */
class RealtimeRecorder(
    private val project: Project,
    private val audioAnalysis: AudioAnalysis,
    private val audioFilePath: String,
    private val outputFile: File,
    private val progressCallback: (Double) -> Unit,
    private val cancellationToken: AtomicBoolean = AtomicBoolean(false),
) {

    private var mediaRecorder: MediaRecorder? = null
    private var renderer: RenderEngine? = null
    private var recordingThread: Thread? = null

    suspend fun record(): Result<File> = withContext(Dispatchers.IO) {
        try {
            setupRecorder()
            setupRenderer()

            mediaRecorder?.start()

            // Start playback and recording
            val startTime = System.currentTimeMillis()
            val duration = audioAnalysis.duration * 1000 // ms

            recordingThread = Thread {
                var lastTime = 0.0
                while (!cancellationToken.get() && (System.currentTimeMillis() - startTime) < duration) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    val progress = elapsed / audioAnalysis.duration
                    progressCallback(progress)

                    // Render frame to surface
                    renderFrame(elapsed)

                    Thread.sleep(33) // ~30fps
                }
            }.apply { start() }

            recordingThread?.join()

            mediaRecorder?.stop()
            mediaRecorder?.release()

            Result.success(outputFile)
        } catch (e: Exception) {
            cleanup()
            Result.failure(e)
        }
    }

    private fun setupRecorder() {
        mediaRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC) // Will be replaced with audio file

            when (project.exportFormat) {
                ExportFormat.MP4 -> setOutputFormat(OutputFormat.MPEG_4)
                ExportFormat.WEBM -> setOutputFormat(OutputFormat.WEBM)
            }

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(15_000_000)
            setVideoFrameRate(project.exportFps)
            setVideoSize(project.resolution.width, project.resolution.height)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }
    }

    private fun setupRenderer() {
        renderer = RendererFactory.createRenderer(project.template)
        renderer?.initialize(project, project.resolution.width, project.resolution.height)
    }

    private fun renderFrame(time: Double) {
        // Would need a Surface to draw to
        // This is a simplified version - real implementation needs SurfaceTexture + OpenGL
    }

    private fun cleanup() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        renderer?.dispose()
        cancellationToken.set(true)
    }

    fun cancel() {
        cancellationToken.set(true)
    }
}

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun <T> failure(exception: Exception): Result<T> = Failure(exception)
    }
}