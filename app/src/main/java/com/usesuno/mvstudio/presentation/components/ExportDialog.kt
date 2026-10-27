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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme

@Composable
fun ExportDialog(
    project: Project,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, ExportQuality, Int) -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(project.exportFormat) }
    var selectedQuality by remember { mutableStateOf(project.exportQuality) }
    var selectedFps by remember { mutableStateOf(project.exportFps) }
    var includeAudio by remember { mutableStateOf(true) }

    UseSunoMVStudioTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .width(420.dp)
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Title bar with close button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Export Video", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painterResource("ic_close"),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Form card inside export dialog
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(bottom = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Format selection
                        Text("Format", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExportFormat.values().forEach { format ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedFormat == format,
                                    onClick = { selectedFormat = format },
                                    label = { Text(format.name) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // Quality selection
                        Text("Quality", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ExportQuality.values().forEach { quality ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedQuality == quality,
                                    onClick = { selectedQuality = quality },
                                    label = { Text(quality.name) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        // FPS
                        Text("Frame Rate", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = selectedFps.toFloat(),
                            onValueChange = { selectedFps = it.toInt() },
                            valueRange = 15f..60f,
                            steps = (60 - 15) / 5,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Include audio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Include Audio", fontSize = 14.sp)
                            Switch(
                                checked = includeAudio,
                                onCheckedChange = { includeAudio = it },
                            )
                        }

                        // Resolution info
                        Text(
                            "Resolution: ${project.resolution.name} (${project.aspectRatio.name})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Estimated time
                        Text(
                            "Estimated export time: ~${estimateExportTime(project, selectedFps)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    androidx.compose.material3.Button(
                        onClick = { onExport(selectedFormat, selectedQuality, selectedFps) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text("Start Export")
                    }
                }
            }
        }
    }
}

private fun estimateExportTime(project: Project, fps: Int): String {
    val duration = project.audioDuration
    val totalFrames = (duration * fps).toLong()
    val complexityFactor = when (project.template) {
        Template.FUME, Template.DIORAMA, Template.ASTRAL, Template.ELYSIUM, Template.SYZYGY -> 3.0
        Template.CLASSIC, Template.STILL, Template.SONNET -> 1.0
        else -> 2.0
    }
    val resolutionFactor = when (project.resolution) {
        Resolution.P720 -> 1.0
        Resolution.P1080 -> 2.0
        Resolution.P1440 -> 4.0
        Resolution.P2160 -> 8.0
    }
    val estimatedSeconds = (totalFrames * complexityFactor * resolutionFactor / 30).toLong()
    return formatDuration(estimatedSeconds)
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}