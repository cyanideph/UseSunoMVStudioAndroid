package com.usesuno.mvstudio.presentation.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.canvas.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.usesuno.mvstudio.domain.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WaveformView(
    audioUri: Uri,
    currentTime: Double,
    duration: Double,
    lines: List<LyricLine>,
    modifier: Modifier = Modifier,
) {
    var waveformData by remember { mutableStateOf<FloatArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(audioUri) {
        isLoading = true
        waveformData = extractWaveform(audioUri)
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading || waveformData == null) {
            // Loading state
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawWaveform(this.nativeCanvas, size.width.toInt(), size.height.toInt(), waveformData!!, currentTime, duration, lines)
            }
        }
    }
}

private fun extractWaveform(uri: Uri): FloatArray? {
    return try {
        // Use MediaMetadataRetriever to get audio data
        // For actual waveform, would need to decode audio file
        // This is a placeholder - real implementation uses MediaCodecAudioDecoder
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri.toString())
        // Generate placeholder waveform
        FloatArray(1000) { (Math.random() * 0.5 + 0.1).toFloat() }
    } catch (e: Exception) {
        null
    }
}

private fun drawWaveform(
    canvas: Canvas,
    width: Int,
    height: Int,
    waveform: FloatArray,
    currentTime: Double,
    duration: Double,
    lines: List<LyricLine>,
) {
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.White
    }

    val centerY = height / 2f
    val maxAmplitude = height / 2f * 0.8f
    val barWidth = max(1f, width.toFloat() / waveform.size)

    // Draw waveform bars
    for (i in waveform.indices) {
        val x = i * barWidth
        val barHeight = waveform[i] * maxAmplitude

        // Color based on position relative to current time
        val timePos = (i / waveform.size.toFloat()) * duration
        val alpha = if (abs(timePos - currentTime) < 0.5) 255 else 128
        paint.alpha = alpha

        canvas.drawRect(
            x,
            centerY - barHeight,
            x + barWidth,
            centerY + barHeight,
            paint,
        )
    }

    // Draw current time marker
    val currentX = (currentTime / duration * width).toFloat()
    val markerPaint = Paint(paint).apply {
        color = Color.Red
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 255
    }
    canvas.drawLine(currentX, 0f, currentX, height.toFloat(), markerPaint)

    // Draw lyric line markers
    val linePaint = Paint(paint).apply {
        color = Color.Cyan
        strokeWidth = 1f
        style = Paint.Style.STROKE
        alpha = 180
    }

    for (line in lines) {
        val startX = (line.start / duration * width).toFloat()
        val endX = (line.end / duration * width).toFloat()
        canvas.drawRect(startX, 0f, endX, height.toFloat(), linePaint)
    }
}