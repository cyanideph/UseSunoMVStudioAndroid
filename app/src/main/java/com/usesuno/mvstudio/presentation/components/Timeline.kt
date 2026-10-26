package com.usesuno.mvstudio.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usesuno.mvstudio.domain.model.AspectRatio
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.Template
import com.usesuno.mvstudio.domain.model.Background
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme

@Composable
fun Timeline(
    project: Project,
    onProjectChange: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubPosition by remember { mutableStateOf(0.0f) }
    var isScrubbing by remember { mutableStateOf(false) }

    UseSunoMVStudioTheme {
        Column(modifier = modifier.padding(16.dp)) {
            // Track header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Lyrics · ${project.lines.size}", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (project.lines.isNotEmpty()) {
                        androidx.compose.material3.TextButton(onClick = { onProjectChange(project.copy(lines = emptyList())) }) {
                            Text("Clear")
                        }
                    }
                    Text(formatTime(project.audioDuration), fontSize = 12.sp, color = UseSunoMVStudioTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Waveform and lyric tracks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                // Waveform preview on the left
                Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    drawWaveformPreview(canvas.nativeCanvas, width = size.width.toInt(), height = 64, project = project)
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp, horizontal = 8.dp)) {
                    items(project.lines, key = { it.id }) { line ->
                        LineRow(
                            line = line,
                            onDelete = {
                                onProjectChange(project.copy(lines = project.lines.filterNot { it.id == line.id }))
                            },
                            onToggleSelect = { isSelected -> }
                        )
                    }
                }
            }

            // Scrubber with waveform preview integration
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Timeline position indicator
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .background(UseSunoMVStudioTheme.colorScheme.primary),
                )

                // Scrubber
                Slider(
                    value = scrubPosition,
                    onValueChange = { scrubPosition = it },
                    onValueChangeFinished = {
                        isScrubbing = false
                        // Seek to position with animation
                        onProjectChange(project.copy(globalOffset = scrubPosition.toDouble() * project.audioDuration))
                    },
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = UseSunoMVStudioTheme.colorScheme.primary,
                        activeTrackColor = UseSunoMVStudioTheme.colorScheme.primary,
                        inactiveTrackColor = UseSunoMVStudioTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.weight(1f),
                )

                // End position
                Text(formatTime(project.audioDuration), fontSize = 12.sp, style = androidx.compose.ui.text.font.FontWeight.Normal)
            }

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(scrubPosition.toDouble() * project.audioDuration), fontSize = 12.sp)
                Text(formatTime(project.audioDuration), fontSize = 12.sp)
            }
        }
    }
}

private fun formatTime(seconds: Double): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    val millis = ((seconds * 100) % 100).toInt()
    return String.format("%02d:%02d.%02d", mins, secs, millis)
}

@Composable
fun LineRow(
    line: com.usesuno.mvstudio.domain.model.LyricLine,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    var selected by remember { mutableStateOf(false) }
    // Ripple effect on tap
    val interactionSource = remember { InteractionSource() }
    var tapped by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            .rippleAlpha(0.1f)
            .pointerInput(interactionSource) {
                onClick = { tapped = true }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = if (tapped) 4.dp else 1.dp,
    ) {
        // Animate elevation on tap
        animateElevation(tapped = tapped) {
            // Card content
        }
        .tickedWhilePressed(tapped) {
            // Ripple effect
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Line indicator circle - taps to select
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(8.dp)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    .border(2.dp, PaintStyle.Stroke, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                )
                .pointerInput(interactionSource) {
                    onClick = { onSelect() }
                }

            // Line text
            OutlinedTextField(
                value = line.text,
                onValueChange = { text ->
                                    // Handle text edit
                                },
                modifier = Modifier.weight(1f).padding(4.dp),
                singleLine = true,
                enabled = true,
                textStyle = MaterialTheme.typography.bodySmall,
                label = { Text("Line text") },
            )

            // Time labels
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                Text(formatTime(line.start), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                Text(formatTime(line.end), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Delete button with icon toggle
            IconButton(onClick = onDelete, enabled = !selected) {
                Icon("ic_close", contentDescription = "Delete line", contentColor = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun drawWaveformPreview(
    canvas: android.graphics.Canvas,
    width: Int,
    height: Int,
    project: com.usesuno.mvstudio.domain.model.Project,
) {
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = MaterialTheme.colorScheme.onSurface
    }

    val centerY = height / 2f
    val maxAmplitude = height / 2f * 0.4f

    // Enhanced waveform with 80 bars for better detail
    val numBars = 80
    val barWidth = max(1f, width.toFloat() / numBars)

    // Get audio analysis data if available, otherwise use placeholder
    val amplitudes = if (project.lines.isNotEmpty()) {
        // Use line durations to simulate waveform activity
        val totalDuration = project.lines.sumBy { it.end - it.start }
        List(numBars) { index ->
            val progress = (index.toFloat() / numBars) * totalDuration
            // Find which line this bar corresponds to
            val correspondingLine = project.lines.find { it.start <= progress && it.end >= progress }
            correspondingLine?.let { line ->
                // Higher amplitude for lines with more words or longer duration
                (1.0f / (1.0f + (line.end - line.start) / totalDuration * 10)) * 0.5f + 0.1f
            } ?: 0.1f
        }
    } else {
        List(numBars) { 0.1f }
    }

    for (i in amplitudes.indices) {
        val amplitude = amplitudes[i]
        val barHeight = amplitude * maxAmplitude
        val x = i * barWidth
        val yStart = centerY - barHeight
        val yEnd = centerY + barHeight

        // Gradient effect - brighter near center
        val progress = i.toFloat() / amplitudes.size
        val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 255.toInt())

        canvas.drawRoundRect(
            RectF(x, yStart, x + barWidth, yEnd),
            2f, // corner radius
            paint.apply { color = barColor }
        )

        // Small highlight on top of active bars
        if (amplitude > 0.3f) {
            val highlightPaint = Paint(paint).apply {
                color = MaterialTheme.colorScheme.primary.copy(alpha = 180)
            }
            canvas.drawRect(x + 1f, yStart, x + barWidth - 1f, yStart + barHeight * 0.3f, highlightPaint)
        }
    }

    // Draw play position marker
    if (project.isAudioPlaying) {
        val markerPaint = Paint(paint).apply {
            color = MaterialTheme.colorScheme.error
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        // Placeholder position at 25%
        canvas.drawLine(width * 0.25f, 0f, width * 0.25f, height.toFloat(), markerPaint)
    }
}
