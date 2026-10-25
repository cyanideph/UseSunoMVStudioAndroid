package com.usesuno.mvstudio.presentation.editor

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

@Composable
fun EditorScreen(
    project: Project,
    onProjectChange: (Project) -> Unit,
    onExportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onImportAudioClick: () -> Unit,
    onImportLyricsClick: () -> Unit,
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
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = UseSunoMVStudioTheme.colorScheme.surfaceContainer
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
                        IconButton(onClick = { /* Show aspect ratio picker */ }) {
                            Icon(painterResource("ic_aspect_ratio"), contentDescription = "Aspect Ratio")
                        }

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
                            .background(UseSunoMVStudioTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        StagePreview(
                            project = project,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Right panel - Inspector
                    if (showInspector) {
                        InspectorPanel(
                            project = project,
                            onProjectChange = onProjectChange,
                            modifier = Modifier
                                .width(360.dp)
                                .fillMaxHeight()
                                .background(UseSunoMVStudioTheme.colorScheme.surfaceContainer),
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
                            .background(UseSunoMVStudioTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }

            // Export Dialog
            if (showExportDialog) {
                ExportDialog(
                    project = project,
                    onDismiss = { showExportDialog = false },
                    onExport = { format, quality ->
                        showExportDialog = false
                        onExportClick()
                    },
                )
            }
        }
    }
}