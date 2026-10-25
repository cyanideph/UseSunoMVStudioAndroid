package com.usesuno.mvstudio.render.export

import android.graphics.SurfaceTexture
import android.media.MediaRecorder
import android.media.MediaRecorder.OutputFormat
import android.view.Surface
import android.view.TextureView
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.ExportFormat
import com.usesuno.mvstudio.domain.audio.AudioAnalysis
import com.usesuno.mvstudio.render.engine.RenderEngine
import com.usesuno.mvstudio.render.engine.RendererFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time video recorder using MediaRecorder with TextureView
 * Fallback for when frame-by-frame export is not available
 */
class RealtimeVideoRecorder(
    private val project: Project,
    private val audioAnalysis: AudioAnalysis,
    private val audioFilePath: String,
    private val outputFile: File,
    private val progressCallback: (Double) -> Unit,
    private val cancellationToken: AtomicBoolean = AtomicBoolean(false),
) {

    private var mediaRecorder: MediaRecorder? = null
    private var textureView: TextureView? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var renderer: RenderEngine? = null
    private var recordingThread: Thread? = null
    private var audioPlayer: android.media.MediaPlayer? = null

    /**
     * Start recording - must be called from UI thread with TextureView
     */
    fun startRecording(textureView: TextureView) {
        this.textureView = textureView
        setupRecorder()
        setupRenderer()
        setupAudioPlayer()
    }

    private fun setupRecorder() {
        mediaRecorder = MediaRecorder().apply {
            // Video from surface
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            // Audio from file (will be mixed in post)
            // For real-time, we'd need to play audio and record microphone
            // This is simplified - real implementation uses MediaMuxer for audio

            when (project.exportFormat) {
                ExportFormat.MP4 -> setOutputFormat(OutputFormat.MPEG_4)
                ExportFormat.WEBM -> setOutputFormat(OutputFormat.WEBM)
            }

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(when (project.exportQuality) {
                com.usesuno.mvstudio.domain.model.ExportQuality.LOW -> 5_000_000
                com.usesuno.mvstudio.domain.model.ExportQuality.MEDIUM -> 15_000_000
                com.usesuno.mvstudio.domain.model.ExportQuality.HIGH -> 30_000_000
                com.usesuno.mvstudio.domain.model.ExportQuality.LOSSLESS -> 100_000_000
            })
            setVideoFrameRate(project.exportFps)
            setVideoSize(
                project.resolution.width,
                project.resolution.height,
            )
            setOutputFile(outputFile.absolutePath)

            // Set surface from texture view
            surfaceTexture = textureView?.surfaceTexture
            surface = Surface(surfaceTexture!!)
            setInputSurface(surface)

            try {
                prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupRenderer() {
        renderer = RendererFactory.createRenderer(project.template)
        renderer?.initialize(project, project.resolution.width, project.resolution.height)
    }

    private fun setupAudioPlayer() {
        audioPlayer = android.media.MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                setOnCompletionListener { stopRecording() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun start() {
        mediaRecorder?.start()
        audioPlayer?.start()

        recordingThread = Thread {
            val startTime = System.currentTimeMillis()
            val duration = audioAnalysis.duration * 1000 // ms

            while (!cancellationToken.get() && (System.currentTimeMillis() - startTime) < duration) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                val progress = elapsed / audioAnalysis.duration
                progressCallback(progress)

                // Render frame to surface
                renderFrame(elapsed)

                // Sleep to maintain frame rate
                Thread.sleep((1000 / project.exportFps).toLong())
            }
        }.apply { start() }
    }

    private fun renderFrame(time: Double) {
        surface?.let { s ->
            val canvas = s.lockCanvas(null)
            canvas?.let { c ->
                val bounds = android.graphics.Rect(0, 0, project.resolution.width, project.resolution.height)
                val audioFrame = audioAnalysis.frames.findLast { it.time <= time }
                renderer?.renderFrame(c, time, audioFrame, bounds)
                s.unlockCanvasAndPost(c)
            }
        }
    }

    fun stopRecording() {
        cancellationToken.set(true)

        mediaRecorder?.stop()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null

        audioPlayer?.stop()
        audioPlayer?.release()
        audioPlayer = null

        surface?.release()
        surface = null

        renderer?.dispose()
        renderer = null

        recordingThread?.interrupt()
    }

    fun cancel() {
        cancellationToken.set(true)
        stopRecording()
    }

    fun isRecording(): Boolean {
        return mediaRecorder != null && !cancellationToken.get()
    }
}

/**
 * TextureView-based recorder for Compose integration
 */
class ComposeRealtimeRecorder(
    project: Project,
    audioAnalysis: AudioAnalysis,
    audioFilePath: String,
    outputFile: File,
    progressCallback: (Double) -> Unit,
) : RealtimeVideoRecorder(project, audioAnalysis, audioFilePath, outputFile, progressCallback) {

    // This class would be used with a TextureView in Compose
    // via AndroidView interop
}