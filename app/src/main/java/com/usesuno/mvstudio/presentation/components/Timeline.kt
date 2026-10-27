package com.usesuno.mvstudio.presentation.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme
import kotlin.math.max

@Composable
fun Timeline(
    project: Project,
    onProjectChange: (Project) -> Unit,
    modifier: Modifier = Modifier,
    playbackTime: Double = 0.0,
    onSeekTo: (Double) -> Unit = {},
    onOpenTimingEditor: () -> Unit = {},
) {
    var isScrubbing by remember { mutableStateOf(false) }
    val duration = max(0.1, project.audioDuration)
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(playbackTime, isScrubbing) {
        if (!isScrubbing && duration > 0.0) {
            scrubPosition = (playbackTime / duration).toFloat().coerceIn(0f, 1f)
        }
    }

    UseSunoMVStudioTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header: Lyric Count + Timing Editor action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lyrics & Timing (${project.lines.size} lines)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Global Offset: ${String.format("%+.2fs", project.globalOffset)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onOpenTimingEditor,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            painter = painterResource("ic_settings"),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Timing Editor", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    if (project.lines.isNotEmpty()) {
                        TextButton(
                            onClick = { onProjectChange(project.copy(lines = emptyList())) }
                        ) {
                            Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform Preview Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(10.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas ->
                            drawWaveformPreview(canvas.nativeCanvas, width = size.width.toInt(), height = size.height.toInt(), project = project)
                        }
                    }

                    // Scrubber cursor line
                    val cursorFraction = if (duration > 0) (playbackTime / duration).toFloat().coerceIn(0f, 1f) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineX = size.width * cursorFraction
                            drawLine(
                                color = androidx.compose.ui.graphics.Color(0xFFBB86FC),
                                start = androidx.compose.ui.geometry.Offset(lineX, 0f),
                                end = androidx.compose.ui.geometry.Offset(lineX, size.height),
                                strokeWidth = 3f
                            )
                        }
                    }
                }
            }

            // Scrubber Slider & Timers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(playbackTime),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(48.dp)
                )

                Slider(
                    value = scrubPosition,
                    onValueChange = {
                        isScrubbing = true
                        scrubPosition = it
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        val targetTime = scrubPosition.toDouble() * duration
                        onSeekTo(targetTime)
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Text(
                    text = formatTime(duration),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Lyric Lines List
            if (project.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No lyrics imported yet",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap 'Import Lyrics' in the top bar to get started",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(project.lines, key = { it.id }) { line ->
                        LineRow(
                            line = line,
                            isCurrent = line.start <= playbackTime && line.end >= playbackTime,
                            onDelete = {
                                onProjectChange(project.copy(lines = project.lines.filterNot { it.id == line.id }))
                            },
                            onSelect = {
                                onSeekTo(line.start)
                            }
                        )
                    }
                }
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
    isCurrent: Boolean = false,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Line indicator / tap to seek
            IconButton(
                onClick = onSelect,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(if (isCurrent) "ic_play_arrow" else "ic_check"),
                    contentDescription = "Seek to line",
                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Line text & timing
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = line.text,
                    fontSize = 13.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatTime(line.start)} → ${formatTime(line.end)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource("ic_close"),
                    contentDescription = "Delete line",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun drawWaveformPreview(
    canvas: android.graphics.Canvas,
    width: Int,
    height: Int,
    project: com.usesuno.mvstudio.domain.model.Project,
) {
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = android.graphics.Color.GRAY
    }

    val centerY = height / 2f
    val maxAmplitude = height / 2f * 0.7f

    val numBars = 80
    val barWidth = max(1f, width.toFloat() / numBars)

    val amplitudes = if (project.lines.isNotEmpty()) {
        val totalDuration = max(1.0, project.lines.sumOf { it.end - it.start })
        List(numBars) { index ->
            val progress = (index.toFloat() / numBars) * totalDuration
            val correspondingLine = project.lines.find { it.start <= progress && it.end >= progress }
            correspondingLine?.let { line ->
                (1.0f / (1.0f + (line.end - line.start) / totalDuration * 10).toFloat()) * 0.5f + 0.15f
            } ?: 0.1f
        }
    } else {
        List(numBars) { 0.15f }
    }

    for (i in amplitudes.indices) {
        val amplitude = amplitudes[i]
        val barHeight = amplitude * maxAmplitude
        val x = i * barWidth
        val yStart = centerY - barHeight
        val yEnd = centerY + barHeight

        paint.color = android.graphics.Color.argb(160, 187, 134, 252)

        canvas.drawRoundRect(
            RectF(x + 1f, yStart, x + barWidth - 1f, yEnd),
            3f,
            3f,
            paint
        )
    }
}
