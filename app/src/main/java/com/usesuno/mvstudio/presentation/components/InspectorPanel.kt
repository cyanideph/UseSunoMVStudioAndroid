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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
fun InspectorPanel(
    project: Project,
    onProjectChange: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedSections by remember { mutableStateOf(
        setOf("template", "background", "typography", "intro", "audio", "export")
    ) }

    UseSunoMVStudioTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Template Section
            InspectorSection(
                title = "Template",
                expanded = "template" in expandedSections,
                onToggle = { if ("template" in expandedSections) expandedSections -= "template" else expandedSections += "template" },
            ) {
                TemplateSelector(
                    selectedTemplate = project.template,
                    onTemplateChange = { template ->
                        onProjectChange(project.copy(template = template))
                    },
                )

                TemplateTuningControls(
                    tuning = project.templateTuning,
                    onTuningChange = { tuning ->
                        onProjectChange(project.copy(templateTuning = tuning))
                    },
                )
            }

            // Background Section
            InspectorSection(
                title = "Background",
                expanded = "background" in expandedSections,
                onToggle = { if ("background" in expandedSections) expandedSections -= "background" else expandedSections += "background" },
            ) {
                BackgroundSelector(
                    background = project.background,
                    config = project.backgroundConfig,
                    onBackgroundChange = { bg ->
                        onProjectChange(project.copy(background = bg))
                    },
                    onConfigChange = { config ->
                        onProjectChange(project.copy(backgroundConfig = config))
                    },
                )
            }

            // Typography Section
            InspectorSection(
                title = "Typography",
                expanded = "typography" in expandedSections,
                onToggle = { if ("typography" in expandedSections) expandedSections -= "typography" else expandedSections += "typography" },
            ) {
                TypographyControls(
                    project = project,
                    onProjectChange = onProjectChange,
                )
            }

            // Intro Section
            InspectorSection(
                title = "Intro",
                expanded = "intro" in expandedSections,
                onToggle = { if ("intro" in expandedSections) expandedSections -= "intro" else expandedSections += "intro" },
            ) {
                IntroControls(
                    enabled = project.introEnabled,
                    duration = project.introDuration,
                    style = project.introStyle,
                    onEnabledChange = { onProjectChange(project.copy(introEnabled = it)) },
                    onDurationChange = { onProjectChange(project.copy(introDuration = it)) },
                    onStyleChange = { onProjectChange(project.copy(introStyle = it)) },
                )
            }

            // Audio Reactivity Section
            InspectorSection(
                title = "Audio Reactivity",
                expanded = "audio" in expandedSections,
                onToggle = { if ("audio" in expandedSections) expandedSections -= "audio" else expandedSections += "audio" },
            ) {
                AudioReactivityControls(
                    enabled = project.audioReactivityEnabled,
                    config = project.audioReactivityConfig,
                    onEnabledChange = { onProjectChange(project.copy(audioReactivityEnabled = it)) },
                    onConfigChange = { onProjectChange(project.copy(audioReactivityConfig = it)) },
                )
            }

            // Export Section
            InspectorSection(
                title = "Export",
                expanded = "export" in expandedSections,
                onToggle = { if ("export" in expandedSections) expandedSections -= "export" else expandedSections += "export" },
            ) {
                ExportSettingsControls(
                    format = project.exportFormat,
                    fps = project.exportFps,
                    quality = project.exportQuality,
                    onFormatChange = { onProjectChange(project.copy(exportFormat = it)) },
                    onFpsChange = { onProjectChange(project.copy(exportFps = it)) },
                    onQualityChange = { onProjectChange(project.copy(exportQuality = it)) },
                )
            }
        }
    }
}

@Composable
fun InspectorSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onToggle) {
                Icon(
                    painterResource(if (expanded) "ic_expand_less" else "ic_expand_more"),
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
fun TemplateSelector(
    selectedTemplate: Template,
    onTemplateChange: (Template) -> Unit,
) {
    ExposedDropdownMenuBox(
        modifier = Modifier.fillMaxWidth(),
    ) { expanded ->
        androidx.compose.material3.Text(
            text = selectedTemplate.name,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { /* handled by ExposedDropdownMenuBox */ },
        ) {
            Template.values().forEach { template ->
                DropdownMenuItem(
                    text = { Text(template.name) },
                    onClick = { onTemplateChange(template) },
                )
            }
        }
    }
}

@Composable
fun TemplateTuningControls(
    tuning: TemplateTuning,
    onTuningChange: (TemplateTuning) -> Unit,
) {
    Column {
        SliderRow("Speed", tuning.speed, 0.1f, 3f, { v -> onTuningChange(tuning.copy(speed = v)) })
        SliderRow("Intensity", tuning.intensity, 0f, 2f, { v -> onTuningChange(tuning.copy(intensity = v)) })
        SliderRow("Complexity", tuning.complexity, 0f, 1f, { v -> onTuningChange(tuning.copy(complexity = v)) })
        SliderRow("Color Shift", tuning.colorShift, -1f, 1f, { v -> onTuningChange(tuning.copy(colorShift = v)) })
    }
}

@Composable
fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.width(100.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f),
        )
        Text(String.format("%.2f", value), modifier = Modifier.width(50.dp))
    }
}

@Composable
fun BackgroundSelector(
    background: Background,
    config: BackgroundConfig,
    onBackgroundChange: (Background) -> Unit,
    onConfigChange: (BackgroundConfig) -> Unit,
) {
    Column {
        ExposedDropdownMenuBox(modifier = Modifier.fillMaxWidth()) { expanded ->
            Text(background.name)
            DropdownMenu(expanded = expanded, onDismissRequest = {}) {
                Background.values().forEach { bg ->
                    DropdownMenuItem(text = { Text(bg.name) }, onClick = { onBackgroundChange(bg) })
                }
            }
        }

        if (background == Background.CUSTOM) {
            // Image/Video picker would go here
            Text("Custom background settings", fontSize = 12.sp, color = UseSunoMVStudioTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TypographyControls(
    project: Project,
    onProjectChange: (Project) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = project.fontFamily,
            onValueChange = { onProjectChange(project.copy(fontFamily = it)) },
            label = { Text("Font Family") },
            modifier = Modifier.fillMaxWidth(),
        )

        SliderRow("Font Weight", project.fontWeight.toFloat(), 100f, 900f,
            { v -> onProjectChange(project.copy(fontWeight = v.toInt())) })
        SliderRow("Font Size", project.fontSize, 0.5f, 3f,
            { v -> onProjectChange(project.copy(fontSize = v)) })
        SliderRow("Line Height", project.lineHeight, 0.5f, 3f,
            { v -> onProjectChange(project.copy(lineHeight = v)) })
        SliderRow("Letter Spacing", project.letterSpacing, -0.5f, 1f,
            { v -> onProjectChange(project.copy(letterSpacing = v)) })

        // Color pickers would go here
    }
}

@Composable
fun IntroControls(
    enabled: Boolean,
    duration: Double,
    style: IntroStyle,
    onEnabledChange: (Boolean) -> Unit,
    onDurationChange: (Double) -> Unit,
    onStyleChange: (IntroStyle) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enabled")
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.align(Alignment.End),
            )
        }

        if (enabled) {
            SliderRow("Duration", duration.toFloat(), 0.5f, 10f,
                { v -> onDurationChange(v.toDouble()) })

            ExposedDropdownMenuBox(modifier = Modifier.fillMaxWidth()) { expanded ->
                Text(style.name)
                DropdownMenu(expanded = expanded, onDismissRequest = {}) {
                    IntroStyle.values().forEach { s ->
                        DropdownMenuItem(text = { Text(s.name) }, onClick = { onStyleChange(s) })
                    }
                }
            }
        }
    }
}

@Composable
fun AudioReactivityControls(
    enabled: Boolean,
    config: AudioReactivityConfig,
    onEnabledChange: (Boolean) -> Unit,
    onConfigChange: (AudioReactivityConfig) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enabled")
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.align(Alignment.End),
            )
        }

        if (enabled) {
            SliderRow("Bass Gain", config.bassGain, 0f, 3f, { v -> onConfigChange(config.copy(bassGain = v)) })
            SliderRow("Low-Mid Gain", config.lowMidGain, 0f, 3f, { v -> onConfigChange(config.copy(lowMidGain = v)) })
            SliderRow("Mid Gain", config.midGain, 0f, 3f, { v -> onConfigChange(config.copy(midGain = v)) })
            SliderRow("Vocal Gain", config.vocalGain, 0f, 3f, { v -> onConfigChange(config.copy(vocalGain = v)) })
            SliderRow("Treble Gain", config.trebleGain, 0f, 3f, { v -> onConfigChange(config.copy(trebleGain = v)) })
            SliderRow("Smoothing", config.smoothing, 0f, 1f, { v -> onConfigChange(config.copy(smoothing = v)) })
            SliderRow("Threshold", config.threshold, 0f, 1f, { v -> onConfigChange(config.copy(threshold = v)) })
        }
    }
}

@Composable
fun ExportSettingsControls(
    format: ExportFormat,
    fps: Int,
    quality: ExportQuality,
    onFormatChange: (ExportFormat) -> Unit,
    onFpsChange: (Int) -> Unit,
    onQualityChange: (ExportQuality) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Format")
            androidx.compose.material3.SegmentedButton(
                selected = { format },
                onClick = onFormatChange,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ExportFormat.values().forEach { f ->
                    androidx.compose.material3.SegmentedButtonItem(
                        label = { Text(f.name) },
                        selected = format == f,
                        onClick = { onFormatChange(f) },
                    )
                }
            }
        }

        SliderRow("FPS", fps.toFloat(), 15f, 60f, { v -> onFpsChange(v.toInt()) })

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Quality")
            androidx.compose.material3.SegmentedButton(
                selected = { quality },
                onClick = onQualityChange,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ExportQuality.values().forEach { q ->
                    androidx.compose.material3.SegmentedButtonItem(
                        label = { Text(q.name) },
                        selected = quality == q,
                        onClick = { onQualityChange(q) },
                    )
                }
            }
        }
    }
}