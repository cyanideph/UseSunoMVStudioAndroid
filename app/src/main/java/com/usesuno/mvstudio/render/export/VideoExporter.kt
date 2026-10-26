package com.usesuno.mvstudio.render.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
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
    private var muxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false
    private var inputColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
    private var audioTrackFormat: MediaFormat? = null

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

        require(w % 2 == 0 && h % 2 == 0) { "Video dimensions must be even for YUV 4:2:0 encoding" }
        require(project.exportFps in 1..60) { "Frame rate must be between 1 and 60 fps" }
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
            require(audioAnalysis.duration > 0.0) { "Cannot export a project with no audio duration" }
            require(outputFile.parentFile?.let { it.exists() || it.mkdirs() } != false) { "Unable to create output directory" }
            initializeEncoder()
            initializeMuxer()
            initializeAudio()

            val frameDurationUs = 1_000_000L / fps
            val totalFrames = kotlin.math.ceil(audioAnalysis.duration * fps).toLong().coerceAtLeast(1L)

            renderer = RendererFactory.createRenderer(project.template)
            renderer?.initialize(project, width, height)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bounds = Rect(0, 0, width, height)

            var presentationTimeUs = 0L
            var frameIndex = 0L

            while (frameIndex < totalFrames && !cancellationToken.get()) {
                val time = frameIndex.toDouble() / fps

                // Analysis frames are ordered, so find the nearest preceding frame without
                // rescanning the entire analysis for every output frame.
                val audioFrame = audioAnalysis.frames.binarySearchBy(time) { it.time }
                    .let { index -> audioAnalysis.frames.getOrNull(if (index >= 0) index else (-index - 2)) }

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
            drainAudio()
            finalizeExport()

            if (cancellationToken.get()) {
                outputFile.delete()
                return@withContext Result.failure(InterruptedException("Export cancelled"))
            }
            progressCallback(1.0)
            Result.success(outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        } finally {
            renderer?.dispose()
            cleanup()
        }
    }

    private fun initializeEncoder() {
        require(project.exportFormat == ExportFormat.MP4) { "WebM export is not supported by the Android muxer path yet" }
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC

        val format = MediaFormat.createVideoFormat(mime, width, height)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        val codecInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                info.getCapabilitiesForType(mime).colorFormats.any {
                    it == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar ||
                        it == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                }
        } ?: throw IllegalStateException("No byte-buffer YUV encoder available for $mime")
        val capabilities = codecInfo.getCapabilitiesForType(mime)
        val colorFormat = when {
            capabilities.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            else -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        }
        inputColorFormat = colorFormat
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        if (mime == MediaFormat.MIMETYPE_VIDEO_AVC) {
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
        }

        videoEncoder = MediaCodec.createByCodecName(codecInfo.name)
        videoEncoder!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoEncoder!!.start()
    }

    private fun initializeMuxer() {
        val outputPath = outputFile.absolutePath
        muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    private fun initializeAudio() {
        audioExtractor = android.media.MediaExtractor()
        audioExtractor!!.setDataSource(audioFilePath)

        // Keep the source format until the video encoder reports its output format; muxer
        // tracks must be added before start, but source audio samples can be drained later.
        for (i in 0 until audioExtractor!!.trackCount) {
            val format = audioExtractor!!.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioExtractor!!.selectTrack(i)
                audioTrackFormat = format
                break
            }
        }

        if (audioTrackFormat == null) throw IllegalArgumentException("Audio file has no audio track")
    }

    private fun encodeFrame(bitmap: Bitmap, presentationTimeUs: Long) {
        encodeFrameToBuffer(bitmap, presentationTimeUs)
    }

    private fun encodeFrameToBuffer(bitmap: Bitmap, presentationTimeUs: Long) {
        // Get input buffer
        val inputBufferIndex = videoEncoder!!.dequeueInputBuffer(10000)
        if (inputBufferIndex < 0) throw IllegalStateException("Timed out waiting for video encoder input buffer")

        val inputBuffer = videoEncoder!!.getInputBuffer(inputBufferIndex)!!
        inputBuffer.clear()

        // Convert bitmap to YUV420 (simplified - in production use RenderScript or libyuv)
        val yuvData = bitmapToYUV420(bitmap, inputColorFormat)
        require(yuvData.size <= inputBuffer.remaining()) { "Encoded frame exceeds codec input buffer" }
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

    private fun bitmapToYUV420(bitmap: Bitmap, colorFormat: Int): ByteArray {
        // Simplified conversion - production should use RenderScript or libyuv
        val width = bitmap.width
        val height = bitmap.height
        val yuvSize = width * height * 3 / 2
        val yuv = ByteArray(yuvSize)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val ySize = width * height
        var yIndex = 0
        var uIndex = ySize
        var vIndex = ySize + ySize / 4
        var uvIndex = ySize

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
                    if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                        yuv[uvIndex++] = u.toByte()
                        yuv[uvIndex++] = v.toByte()
                    } else {
                        yuv[uIndex++] = u.toByte()
                        yuv[vIndex++] = v.toByte()
                    }
                }
            }
        }

        return yuv
    }

    private fun drainVideoEncoder(endOfStream: Boolean = false) {
        val bufferInfo = MediaCodec.BufferInfo()
        var sawEndOfStream = false
        while (!sawEndOfStream) {
            if (cancellationToken.get()) return
            val outputIndex = videoEncoder!!.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000 else 0)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
                continue
            }
            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = videoEncoder!!.outputFormat
                videoTrackIndex = muxer!!.addTrack(newFormat)
                val audioFormat = audioTrackFormat ?: throw IllegalStateException("Audio format is unavailable")
                audioTrackIndex = muxer!!.addTrack(audioFormat)
                muxer!!.start()
                muxerStarted = true
            } else if (outputIndex >= 0) {
                val outputBuffer = videoEncoder!!.getOutputBuffer(outputIndex)!!
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer!!.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                }
                sawEndOfStream = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                videoEncoder!!.releaseOutputBuffer(outputIndex, false)
            }
        }
    }

    private fun signalEndOfVideoStream() {
        var eosInputIndex = -1
        while (eosInputIndex < 0 && !cancellationToken.get()) {
            eosInputIndex = videoEncoder!!.dequeueInputBuffer(10_000)
        }
        if (eosInputIndex >= 0) {
            videoEncoder!!.queueInputBuffer(eosInputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }
        drainVideoEncoder(endOfStream = true)
    }

    private fun drainAudio() {
        val extractor = audioExtractor ?: return
        val trackFormat = audioTrackFormat ?: return
        val maxInputSize = if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(4096)
        } else 1024 * 1024
        val buffer = ByteBuffer.allocateDirect(maxInputSize)
        val bufferInfo = MediaCodec.BufferInfo()
        val audioEndUs = (audioAnalysis.duration * 1_000_000).toLong()
        while (!cancellationToken.get()) {
            buffer.clear()
            val size = extractor.readSampleData(buffer, 0)
            val sampleTimeUs = extractor.sampleTime
            if (size < 0 || sampleTimeUs < 0 || sampleTimeUs >= audioEndUs) break
            bufferInfo.set(0, size, sampleTimeUs, extractor.sampleFlags)
            buffer.position(0)
            buffer.limit(size)
            muxer!!.writeSampleData(audioTrackIndex, buffer, bufferInfo)
            extractor.advance()
        }
    }

    private fun finalizeExport() {
        if (muxerStarted) muxer?.stop()
        muxer?.release()
        muxer = null
        muxerStarted = false
    }

    private fun cleanup() {
        runCatching { videoEncoder?.stop() }
        videoEncoder?.release()
        videoEncoder = null
        audioExtractor?.release()
        audioExtractor = null
        if (muxer != null) runCatching { finalizeExport() }
    }

    fun cancel() {
        cancellationToken.set(true)
    }
}