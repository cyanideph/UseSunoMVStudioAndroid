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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Rect
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
    var scrubPosition by remember { mutableStateOf(0.0) }
    var isScrubbing by remember { mutableStateOf(false) }

    UseSunoMVStudioTheme {
        Column(modifier = modifier.padding(16.dp)) {
            // Track header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Timeline", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(formatTime(project.audioDuration), fontSize = 12.sp, color = UseSunoMVStudioTheme.colorScheme.onSurfaceVariant)
            }

            // Waveform and lyric tracks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Transparent),
            ) {
                // Waveform background
                // Would draw waveform here

                // Lyric line blocks
                // Would render lyric blocks as draggable items
            }

            // Scrubber
            Slider(
                value = scrubPosition,
                onValueChange = { scrubPosition = it },
                onValueChangeFinished = {
                    isScrubbing = false
                    // Seek to position
                    onProjectChange(project.copy(globalOffset = scrubPosition * project.audioDuration))
                },
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = UseSunoMVStudioTheme.colorScheme.primary,
                    activeTrackColor = UseSunoMVStudioTheme.colorScheme.primary,
                    inactiveTrackColor = UseSunoMVStudioTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatTime(scrubPosition * project.audioDuration), fontSize = 12.sp)
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