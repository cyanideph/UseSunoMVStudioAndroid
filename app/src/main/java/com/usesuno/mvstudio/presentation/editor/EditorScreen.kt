package com.usesuno.mvstudio.presentation.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usesuno.mvstudio.domain.model.AspectRatio
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.Template
import com.usesuno.mvstudio.presentation.components.ExportDialog
import com.usesuno.mvstudio.presentation.components.InspectorPanel
import com.usesuno.mvstudio.presentation.components.StagePreview
import com.usesuno.mvstudio.presentation.components.Timeline
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme

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
    playbackTime: Double = 0.0,
    onSeekTo: (Double) -> Unit = {},
    onOpenTimingEditor: () -> Unit = {},
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedDeckTab by remember { mutableIntStateOf(0) } // 0: Visual Inspector, 1: Timeline & Lyrics

    UseSunoMVStudioTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = project.name.ifBlank { "UseSuno MV Studio" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Music Video Generator",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    actions = {
                        // Aspect Ratio Picker Button
                        AspectRatioPicker(
                            selected = project.aspectRatio,
                            onSelected = { onProjectChange(project.copy(aspectRatio = it)) }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Import Audio Button
                        IconButton(
                            onClick = onImportAudioClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (project.audioId != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                painter = painterResource("ic_play_arrow"),
                                contentDescription = "Import Audio",
                                tint = if (project.audioId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Import Lyrics Button
                        IconButton(
                            onClick = onImportLyricsClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (project.lines.isNotEmpty()) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                painter = painterResource("ic_check"),
                                contentDescription = "Import Lyrics",
                                tint = if (project.lines.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Prominent Export Button
                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )

                // Responsive Layout: Phone Vertical Stack vs Tablet 2-Column
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val isWideScreen = maxWidth >= 720.dp

                    if (isWideScreen) {
                        // Wide Screen Layout (Tablets / Foldables)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Column: Stage Preview + Timeline
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StagePreviewCard(
                                    project = project,
                                    playbackTime = playbackTime,
                                    isAudioPlaying = isAudioPlaying,
                                    onPlayPauseClick = onPlayPauseClick,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )

                                Timeline(
                                    project = project,
                                    onProjectChange = onProjectChange,
                                    playbackTime = playbackTime,
                                    onSeekTo = onSeekTo,
                                    onOpenTimingEditor = onOpenTimingEditor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                                )
                            }

                            // Right Column: Full Inspector
                            InspectorPanel(
                                project = project,
                                onProjectChange = onProjectChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                            )
                        }
                    } else {
                        // Phone Layout (Vertical Stack with Studio Tabs)
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Stage Preview Area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                StagePreviewCard(
                                    project = project,
                                    playbackTime = playbackTime,
                                    isAudioPlaying = isAudioPlaying,
                                    onPlayPauseClick = onPlayPauseClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp)
                                )
                            }

                            // Studio Deck Tab Bar
                            TabRow(
                                selectedTabIndex = selectedDeckTab,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = selectedDeckTab == 0,
                                    onClick = { selectedDeckTab = 0 },
                                    text = {
                                        Text(
                                            "Visual Studio",
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedDeckTab == 0) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                                Tab(
                                    selected = selectedDeckTab == 1,
                                    onClick = { selectedDeckTab = 1 },
                                    text = {
                                        Text(
                                            "Lyrics & Timeline (${project.lines.size})",
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedDeckTab == 1) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                            }

                            // Tab Content taking remaining height smoothly
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (selectedDeckTab == 0) {
                                    InspectorPanel(
                                        project = project,
                                        onProjectChange = onProjectChange,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Timeline(
                                        project = project,
                                        onProjectChange = onProjectChange,
                                        playbackTime = playbackTime,
                                        onSeekTo = onSeekTo,
                                        onOpenTimingEditor = onOpenTimingEditor,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
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

@Composable
private fun StagePreviewCard(
    project: Project,
    playbackTime: Double,
    isAudioPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Video Preview Canvas
            StagePreview(
                project = project,
                currentTime = playbackTime,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )

            // Bottom Transport Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause Circle Button
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            painter = painterResource(if (isAudioPlaying) "ic_close" else "ic_play_arrow"),
                            contentDescription = if (isAudioPlaying) "Pause" else "Play",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Status Information
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format("%02d:%02d", (playbackTime / 60).toInt(), (playbackTime % 60).toInt()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "/",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = String.format("%02d:%02d", (project.audioDuration / 60).toInt(), (project.audioDuration % 60).toInt()),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Template & Aspect Tag
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${project.template.name} · ${aspectRatioLabel(project.aspectRatio)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AspectRatioPicker(
    selected: AspectRatio,
    onSelected: (AspectRatio) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = {
                Text(
                    text = aspectRatioLabel(selected),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                selectedLabelColor = MaterialTheme.colorScheme.primary
            ),
            trailingIcon = {
                Icon(
                    painter = painterResource("ic_expand_more"),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AspectRatio.values().forEach { ratio ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = aspectRatioLabel(ratio),
                            fontWeight = if (ratio == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (ratio == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelected(ratio)
                        expanded = false
                    },
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