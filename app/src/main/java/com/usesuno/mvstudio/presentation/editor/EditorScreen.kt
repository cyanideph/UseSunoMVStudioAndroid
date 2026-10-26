package com.usesuno.mvstudio.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usesuno.mvstudio.domain.model.AspectRatio
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.Template
import com.usesuno.mvstudio.presentation.components.TemplatePicker
import com.usesuno.mvstudio.presentation.components.Timeline
import com.usesuno.mvstudio.presentation.components.InspectorPanel
import com.usesuno.mvstudio.presentation.components.StagePreview
import com.usesuno.mvstudio.presentation.components.ExportDialog
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AspectRatioPicker(
    selected: AspectRatio,
    onSelected: (AspectRatio) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(aspectRatioLabel(selected))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AspectRatio.values().forEach { ratio ->
                DropdownMenuItem(
                    text = { Text(aspectRatioLabel(ratio)) },
                    onClick = { onSelected(ratio); expanded = false },
                )
            }
        }
    }
}

private fun aspectRatioLabel(ratio: AspectRatio): String = when (ratio) {
    AspectRatio.RATIO_16_9 -> "16:9"
    AspectRatio.RATIO_9_16 -> "9:16"
    AspectRatio.RATIO_1_1 -> "1:1"
    AspectRatio.RATIO_4_3 -> "4:3"
    AspectRatio.RATIO_3_4 -> "3:4"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: Project,
    onProjectChange: (Project) -> Unit,
    onExportClick: (com.usesuno.mvstudio.domain.model.ExportFormat, com.usesuno.mvstudio.domain.model.ExportQuality, Int) -> Unit,
    onSettingsClick: () -> Unit,
    onImportAudioClick: () -> Unit,
    onImportLyricsClick: () -> Unit,
    isAudioPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
) {
    var showInspector by remember { mutableStateOf(true) }
    var showTimeline by remember { mutableStateOf(true) }
    var showExportDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf<Template?>(null) }

    UseSunoMVStudioTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = { Text(project.name, fontSize = 18.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = { /* Navigate back */ }) {
                            Icon(painterResource("ic_arrow_back"), contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Template picker
                        IconButton(onClick = { expandedMenu = if (expandedMenu == project.template) null else project.template }) {
                            Icon(painterResource("ic_palette"), contentDescription = "Template")
                        }

                        DropdownMenu(
                            expanded = expandedMenu != null,
                            onDismissRequest = { expandedMenu = null },
                        ) {
                            TemplatePicker(
                                selectedTemplate = project.template,
                                onTemplateSelected = { template ->
                                    onProjectChange(project.copy(template = template))
                                    expandedMenu = null
                                },
                            )
                        }

                        // Aspect ratio
                    AspectRatioPicker(
                        selected = project.aspectRatio,
                        onSelected = { ratio -> onProjectChange(project.copy(aspectRatio = ratio)) },
                    )

                        // Import audio
                        IconButton(onClick = onImportAudioClick) {
                            Icon(painterResource("ic_audiotrack"), contentDescription = "Import Audio")
                        }

                        // Import lyrics
                        IconButton(onClick = onImportLyricsClick) {
                            Icon(painterResource("ic_lyrics"), contentDescription = "Import Lyrics")
                        }

                        // Export
                        Button(onClick = { showExportDialog = true }) {
                            Text("Export")
                        }

                        // Settings
                        IconButton(onClick = onSettingsClick) {
                            Icon(painterResource("ic_settings"), contentDescription = "Settings")
                        }
                    }
                )

                // Main content area
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Left panel - Stage preview
                    Box(
                        modifier = Modifier
                            .weight(1f, true)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            StagePreview(
                                project = project,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            androidx.compose.material3.IconButton(onClick = onPlayPauseClick) {
                                Icon(
                                    painterResource(if (isAudioPlaying) "ic_pause" else "ic_play_arrow"),
                                    contentDescription = if (isAudioPlaying) "Pause audio" else "Play audio",
                                )
                            }
                        }
                    }

                    // Right panel - Inspector
                    if (showInspector) {
                        InspectorPanel(
                            project = project,
                            onProjectChange = onProjectChange,
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                        )
                    }
                }

                // Bottom panel - Timeline
                if (showTimeline) {
                    Timeline(
                        project = project,
                        onProjectChange = onProjectChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }

            // Export Dialog
            if (showExportDialog) {
                ExportDialog(
                    project = project,
                    onDismiss = { showExportDialog = false },
                    onExport = { format, quality, fps ->
                        showExportDialog = false
                        onExportClick(format, quality, fps)
                    },
                )
            }
        }
    }
}