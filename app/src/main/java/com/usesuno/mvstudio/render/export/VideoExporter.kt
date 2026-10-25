package com.usesuno.mvstudio.render.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.RenderEngine
import com.usesuno.mvstudio.render.engine.RendererFactory
import com.usesuno.mvstudio.domain.audio.AudioAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Video exporter using MediaCodec for frame-by-frame encoding
 * Ported from export/video.ts and export/foliaFrames.ts
 */
class VideoExporter(
    private val project: Project,
    private val audioAnalysis: AudioAnalysis,
    private val audioFilePath: String,
    private val outputFile: File,
    private val progressCallback: (Double) -> Unit,
    private val cancellationToken: AtomicBoolean = AtomicBoolean(false),
) {

    private var renderer: RenderEngine? = null
    private var videoEncoder: MediaCodec? = null
    private var audioExtractor: android.media.MediaExtractor? = null
    private var audioDecoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false

    private val width: Int
    private val height: Int
    private val fps: Int
    private val bitrate: Int

    init {
        val resolution = project.resolution
        val aspectRatio = project.aspectRatio

        var w = when (resolution) {
            Resolution.P720 -> 1280
            Resolution.P1080 -> 1920
            Resolution.P1440 -> 2560
            Resolution.P2160 -> 3840
        }
        var h = when (resolution) {
            Resolution.P720 -> 720
            Resolution.P1080 -> 1080
            Resolution.P1440 -> 1440
            Resolution.P2160 -> 2160
        }

        // Adjust for aspect ratio
        when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> {
                val temp = w
                w = h
                h = temp
            }
            AspectRatio.RATIO_1_1 -> {
                w = kotlin.math.min(w, h)
                h = w
            }
            AspectRatio.RATIO_4_3 -> {
                h = (w * 3 / 4)
            }
            AspectRatio.RATIO_3_4 -> {
                w = (h * 3 / 4)
            }
            AspectRatio.RATIO_16_9 -> {
                // Keep 16:9 as default
            }
        }

        width = w
        height = h

        fps = project.exportFps
        bitrate = when (project.exportQuality) {
            ExportQuality.LOW -> 5_000_000
            ExportQuality.MEDIUM -> 15_000_000
            ExportQuality.HIGH -> 30_000_000
            ExportQuality.LOSSLESS -> 100_000_000
        }
    }

    /**
     * Export video with frame-by-frame rendering
     */
    suspend fun export(): Result<File> = withContext(Dispatchers.IO) {
        try {
            initializeEncoder()
            initializeMuxer()
            initializeAudio()

            val frameDurationUs = (1_000_000 / fps).toLong()
            val totalFrames = (audioAnalysis.duration * fps).toLong()

            renderer = RendererFactory.createRenderer(project.template)
            renderer?.initialize(project, width, height)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bounds = Rect(0, 0, width, height)

            var presentationTimeUs = 0L
            var frameIndex = 0L

            while (frameIndex < totalFrames && !cancellationToken.get()) {
                val time = frameIndex.toDouble() / fps

                // Get audio frame for this time
                val audioFrame = audioAnalysis.frames.findLast { it.time <= time }

                // Render frame
                canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                renderer?.renderFrame(canvas, time, audioFrame, bounds)

                // Encode frame
                encodeFrame(bitmap, presentationTimeUs)

                // Update progress
                val progress = frameIndex.toDouble() / totalFrames
                progressCallback(progress)

                presentationTimeUs += frameDurationUs
                frameIndex++
            }

            // Signal end of video stream
            signalEndOfVideoStream()

            // Drain audio
            drainAudio()

            // Finalize
            finalizeExport()

            renderer?.dispose()
            cleanup()

            Result.success(outputFile)
        } catch (e: Exception) {
            cleanup()
            Result.failure(e)
        }
    }

    private fun initializeEncoder() {
        val mime = when (project.exportFormat) {
            ExportFormat.MP4 -> MediaFormat.MIMETYPE_VIDEO_AVC
            ExportFormat.WEBM -> MediaFormat.MIMETYPE_VIDEO_VP8
        }

        val format = MediaFormat.createVideoFormat(mime, width, height)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41)

        videoEncoder = MediaCodec.createEncoderByType(mime)
        videoEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoEncoder!!.start()
    }

    private fun initializeMuxer() {
        val outputPath = outputFile.absolutePath
        muxer = MediaMuxer(outputPath, when (project.exportFormat) {
            ExportFormat.MP4 -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            ExportFormat.WEBM -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
        })
    }

    private fun initializeAudio() {
        audioExtractor = android.media.MediaExtractor()
        audioExtractor!!.setDataSource(audioFilePath)

        // Find audio track
        for (i in 0 until audioExtractor!!.trackCount) {
            val format = audioExtractor!!.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioExtractor!!.selectTrack(i)
                audioTrackIndex = i
                break
            }
        }

        if (audioTrackIndex >= 0) {
            val audioFormat = audioExtractor!!.getTrackFormat(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME)!!

            audioDecoder = MediaCodec.createDecoderByType(mime)
            audioDecoder!!.configure(audioFormat, null, null, 0)
            audioDecoder!!.start()
        }
    }

    private fun encodeFrame(bitmap: Bitmap, presentationTimeUs: Long) {
        val inputSurface = videoEncoder!!.createInputSurface()
        val eglCore = EglCore()
        eglCore.makeCurrent(inputSurface)

        try {
            // Alternative: Use byte buffer encoding
            encodeFrameToBuffer(bitmap, presentationTimeUs)
        } finally {
            eglCore.release()
            inputSurface.release()
        }
    }

    private fun encodeFrameToBuffer(bitmap: Bitmap, presentationTimeUs: Long) {
        // Get input buffer
        val inputBufferIndex = videoEncoder!!.dequeueInputBuffer(10000)
        if (inputBufferIndex < 0) return

        val inputBuffer = videoEncoder!!.getInputBuffer(inputBufferIndex)!!
        inputBuffer.clear()

        // Convert bitmap to YUV420 (simplified - in production use RenderScript or libyuv)
        val yuvData = bitmapToYUV420(bitmap)
        inputBuffer.put(yuvData)

        videoEncoder!!.queueInputBuffer(
            inputBufferIndex,
            0,
            yuvData.size,
            presentationTimeUs,
            0
        )

        // Drain output
        drainVideoEncoder()
    }

    private fun bitmapToYUV420(bitmap: Bitmap): ByteArray {
        // Simplified conversion - production should use RenderScript or libyuv
        val width = bitmap.width
        val height = bitmap.height
        val yuvSize = width * height * 3 / 2
        val yuv = ByteArray(yuvSize)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var yIndex = 0
        var uvIndex = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val rgb = pixels[j * width + i]
                val r = (rgb shr 16 and 0xFF).toDouble()
                val g = (rgb shr 8 and 0xFF).toDouble()
                val b = (rgb and 0xFF).toDouble()

                val y = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
                val u = (-0.169 * r - 0.331 * g + 0.5 * b + 128).toInt().coerceIn(0, 255)
                val v = (0.5 * r - 0.419 * g - 0.081 * b + 128).toInt().coerceIn(0, 255)

                yuv[yIndex++] = y.toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = u.toByte()
                    yuv[uvIndex++] = v.toByte()
                }
            }
        }

        return yuv
    }

    private fun drainVideoEncoder() {
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputIndex = videoEncoder!!.dequeueOutputBuffer(bufferInfo, 0)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) break
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = videoEncoder!!.outputFormat
                videoTrackIndex = muxer!!.addTrack(newFormat)
                if (!muxerStarted && audioTrackIndex >= 0) {
                    muxer!!.start()
                    muxerStarted = true
                }
            } else if (outputIndex >= 0) {
                val outputBuffer = videoEncoder!!.getOutputBuffer(outputIndex)!!
                if (!muxerStarted) {
                    muxer!!.start()
                    muxerStarted = true
                }
                muxer!!.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                videoEncoder!!.releaseOutputBuffer(outputIndex, false)
            }
        }
    }

    private fun signalEndOfVideoStream() {
        videoEncoder!!.signalEndOfInputStream()
        drainVideoEncoder()
    }

    private fun drainAudio() {
        if (audioDecoder == null || audioExtractor == null) return

        val bufferInfo = MediaCodec.BufferInfo()
        var sawEOS = false

        while (!sawEOS && !cancellationToken.get()) {
            // Feed input
            val inputIndex = audioDecoder!!.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val buffer = audioDecoder!!.getInputBuffer(inputIndex)!!
                val sampleSize = audioExtractor!!.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    audioDecoder!!.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    sawEOS = true
                } else {
                    val presentationTimeUs = audioExtractor!!.sampleTime
                    audioDecoder!!.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0)
                    audioExtractor!!.advance()
                }
            }

            // Drain output
            val outputIndex = audioDecoder!!.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawEOS = true
                } else {
                    val outputBuffer = audioDecoder!!.getOutputBuffer(outputIndex)!!
                    muxer!!.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                    audioDecoder!!.releaseOutputBuffer(outputIndex, false)
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = audioDecoder!!.outputFormat
                audioTrackIndex = muxer!!.addTrack(newFormat)
                if (!muxerStarted) {
                    muxer!!.start()
                    muxerStarted = true
                }
            }
        }
    }

    private fun finalizeExport() {
        muxer?.stop()
        muxer?.release()
    }

    private fun cleanup() {
        videoEncoder?.stop()
        videoEncoder?.release()
        audioDecoder?.stop()
        audioDecoder?.release()
        audioExtractor?.release()
    }

    fun cancel() {
        cancellationToken.set(true)
    }
}

/**
 * EGL core for surface rendering
 */
class EglCore {
    private var display: javax.microedition.khronos.egl.EGLDisplay = javax.microedition.khronos.egl.EGL10.EGL_NO_DISPLAY
    private var context: javax.microedition.khronos.egl.EGLContext = javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT
    private var surface: javax.microedition.khronos.egl.EGLSurface = javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE

    fun makeCurrent(surface: android.view.Surface) {
        // EGL initialization code here
    }

    fun release() {
        // Cleanup
    }
}