package com.usesuno.mvstudio.presentation.timestamp

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.lifecycleScope
import com.usesuno.mvstudio.domain.model.LyricLine
import com.usesuno.mvstudio.domain.model.LyricWord
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.presentation.components.WaveformView
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TimestampScreen(
    project: Project,
    onProjectChange: (Project) -> Unit,
    onDone: () -> Unit,
    audioUri: Uri,
) {
    var currentTime by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0.0) }
    var isPlaying by remember { mutableStateOf(false) }
    var selectedLineIndex by remember { mutableStateOf(-1) }
    var selectedWordIndex by remember { mutableStateOf(-1) }
    var globalOffset by remember { mutableStateOf(project.globalOffset) }
    var showWordTiming by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }

    LaunchedEffect(audioUri) {
        try {
            mediaPlayer.setDataSource(audioUri.toString())
            mediaPlayer.prepare()
            duration = mediaPlayer.duration / 1000.0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && currentTime < duration) {
                currentTime = mediaPlayer.currentPosition / 1000.0
                delay(50)
            }
            if (currentTime >= duration) {
                isPlaying = false
            }
        }
    }

    UseSunoMVStudioTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Enhanced Top App Bar with better styling
            TopAppBar(
                title = { Text("Word Timing Editor", fontSize = 20.sp, fontWeight = MaterialTheme.typography.titleLarge.fontWeight ?: FontWeight.SemiBold) },
                modifier = Modifier.fillMaxWidth(),
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(painterResource("ic_check"), contentDescription = "Done", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    // Mode toggle with improved icon
                    IconButton(onClick = { showWordTiming = !showWordTiming }) {
                        Icon(
                            painterResource(if (showWordTiming) "ic_text_fields" else "ic_format_list_numbered"),
                            contentDescription = "Toggle Word/Line",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Auto-segment button
                    IconButton(onClick = { /* Auto-segment */ }) {
                        Icon(painterResource("ic_auto_awesome"), contentDescription = "Auto Segment", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    // Settings button
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(painterResource("ic_settings"), contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                // Left panel - Waveform + Transport controls
                Box(
                    modifier = Modifier
                        .weight(1f, true)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Waveform section
                        WaveformView(
                            audioUri = audioUri,
                            currentTime = currentTime,
                            duration = duration,
                            lines = project.lines,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                        )

                        // Global offset and timing info
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Global Offset: ${String.format("%.2f", globalOffset)}s", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${formatTime(currentTime)} / ${formatTime(duration)}", fontSize = 12.sp)
                            }
                            Slider(
                                value = (globalOffset + 10).toFloat(), // -10 to +10
                                onValueChange = { v ->
                                    globalOffset = (v - 10).toDouble()
                                    onProjectChange(project.copy(globalOffset = (v - 10).toDouble()))
                                },
                                valueRange = 0f..20f,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                }

                // Right: Lyric Editor area
                Card(
                    modifier = Modifier
                        .width(420.dp)
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Mode selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(if (showWordTiming) "Word Timing" else "Line Timing", fontSize = 16.sp, fontWeight = MaterialTheme.typography.titleMedium.fontWeight ?: FontWeight.Normal)
                            Switch(
                                checked = showWordTiming,
                                onCheckedChange = { showWordTiming = it },
                            )
                        }

                        // Lyric lines list
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            project.lines.forEachIndexed { lineIndex, line ->
                                LyricLineItem(
                                    line = line,
                                    lineIndex = lineIndex,
                                    isSelected = lineIndex == selectedLineIndex,
                                    currentTime = currentTime,
                                    showWordTiming = showWordTiming,
                                    selectedWordIndex = selectedWordIndex,
                                    onClick = {
                                        selectedLineIndex = if (selectedLineIndex == lineIndex) -1 else lineIndex
                                        selectedWordIndex = -1
                                    },
                                    onWordClick = { wordIndex ->
                                        selectedWordIndex = if (selectedWordIndex == wordIndex) -1 else wordIndex
                                    },
                                    onTimeSet = { start, end ->
                                        val newLines = project.lines.toMutableList()
                                        newLines[lineIndex] = line.copy(start = start, end = end)
                                        onProjectChange(project.copy(lines = newLines))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Bottom: Transport controls with improved styling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Rewind 10s
                    IconButton(onClick = { mediaPlayer.seekTo(0); currentTime = 0.0 }) {
                        Icon(painterResource("ic_replay_10"), contentDescription = "Restart", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Play/Pause with nice animation
                    IconButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(
                            painter = painterResource(if (isPlaying) "ic_pause" else "ic_play_arrow"),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Forward 10s
                    IconButton(onClick = { mediaPlayer.seekTo((duration * 1000).toInt()); currentTime = duration }) {
                        Icon(painterResource("ic_forward_10"), contentDescription = "End", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Time display
                    Text(
                        "${formatTime(currentTime)} / ${formatTime(duration)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Tap timing - prominent button
                    Button(
                        onClick = { setCurrentTimeForSelected() },
                        modifier = Modifier.size(64.dp, 64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("TAP", fontSize = 16.sp, fontWeight = MaterialTheme.typography.labelLarge.fontWeight ?: FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LyricLineItem(
    line: LyricLine,
    lineIndex: Int,
    isSelected: Boolean,
    currentTime: Double,
    showWordTiming: Boolean,
    selectedWordIndex: Int,
    onClick: () -> Unit,
    onWordClick: (Int) -> Unit,
    onTimeSet: (Double, Double) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Line ${lineIndex + 1}", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("${formatTime(line.start)} - ${formatTime(line.end)}", fontSize = 12.sp)
            }

            Text(line.text, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))

            if (showWordTiming && line.words.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    line.words.forEachIndexed { wordIndex, word ->
                        val isWordSelected = wordIndex == selectedWordIndex
                        val isCurrentWord = word.start <= currentTime && word.end >= currentTime

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = when {
                                    isWordSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                                    isCurrentWord -> androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                                    else -> androidx.compose.material3.MaterialTheme.colorScheme.surface
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(word.text, fontSize = 13.sp)
                                Text("${formatTime(word.start)} - ${formatTime(word.end)}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Set start/end buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Button(onClick = { onTimeSet(currentTime, line.end) }) {
                    Text("Set Start: ${formatTime(currentTime)}", fontSize = 11.sp)
                }
                androidx.compose.material3.Button(onClick = { onTimeSet(line.start, currentTime) }) {
                    Text("Set End: ${formatTime(currentTime)}", fontSize = 11.sp)
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

private fun setCurrentTimeForSelected() {
    // Implementation would set start/end time for selected line/word
}