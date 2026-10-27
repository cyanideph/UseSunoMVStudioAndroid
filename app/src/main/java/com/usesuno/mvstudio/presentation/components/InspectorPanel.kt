package com.usesuno.mvstudio.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorPanel(
    project: Project,
    onProjectChange: (Project) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Template", "Background", "Typography", "Audio FX", "Export")

    UseSunoMVStudioTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Category Tabs for Mobile
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Scrollable Settings Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Template & Tuning
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Visual Template", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                TemplateSelector(
                                    selectedTemplate = project.template,
                                    onTemplateChange = { onProjectChange(project.copy(template = it)) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Fine Tuning", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                TemplateTuningControls(
                                    tuning = project.templateTuning,
                                    onTuningChange = { onProjectChange(project.copy(templateTuning = it)) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Background
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Background Style", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                BackgroundSelector(
                                    background = project.background,
                                    config = project.backgroundConfig,
                                    onBackgroundChange = { onProjectChange(project.copy(background = it)) },
                                    onConfigChange = { onProjectChange(project.copy(backgroundConfig = it)) }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Typography
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Typography & Text", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                TypographyControls(
                                    project = project,
                                    onProjectChange = onProjectChange
                                )
                            }
                        }
                    }

                    3 -> {
                        // Audio FX & Intro
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Audio Reactivity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                AudioReactivityControls(
                                    enabled = project.audioReactivityEnabled,
                                    config = project.audioReactivityConfig,
                                    onEnabledChange = { onProjectChange(project.copy(audioReactivityEnabled = it)) },
                                    onConfigChange = { onProjectChange(project.copy(audioReactivityConfig = it)) }
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Intro Sequence", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                IntroControls(
                                    enabled = project.introEnabled,
                                    duration = project.introDuration,
                                    style = project.introStyle,
                                    onEnabledChange = { onProjectChange(project.copy(introEnabled = it)) },
                                    onDurationChange = { onProjectChange(project.copy(introDuration = it)) },
                                    onStyleChange = { onProjectChange(project.copy(introStyle = it)) }
                                )
                            }
                        }
                    }

                    4 -> {
                        // Export Settings
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Export Configuration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(12.dp))
                                ExportSettingsControls(
                                    format = project.exportFormat,
                                    fps = project.exportFps,
                                    quality = project.exportQuality,
                                    onFormatChange = { onProjectChange(project.copy(exportFormat = it)) },
                                    onFpsChange = { onProjectChange(project.copy(exportFps = it)) },
                                    onQualityChange = { onProjectChange(project.copy(exportQuality = it)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateSelector(
    selectedTemplate: Template,
    onTemplateChange: (Template) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        // Selected Template Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACTIVE TEMPLATE", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        formatTemplateName(selectedTemplate),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    painter = painterResource(if (expanded) "ic_expand_less" else "ic_expand_more"),
                    contentDescription = "Select Template",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                Template.values().forEach { template ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                formatTemplateName(template),
                                fontWeight = if (template == selectedTemplate) FontWeight.Bold else FontWeight.Normal,
                                color = if (template == selectedTemplate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onTemplateChange(template)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Pick Row of Popular Templates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickTemplates = listOf(
                Template.FUME, Template.ASTRAL, Template.KYOTO,
                Template.CLASSIC, Template.CADENZA, Template.AURORA_TRAVERSE,
                Template.MONET, Template.ELYSIUM, Template.LUMIERE
            )
            quickTemplates.forEach { template ->
                FilterChip(
                    selected = selectedTemplate == template,
                    onClick = { onTemplateChange(template) },
                    label = { Text(formatTemplateName(template), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SliderRow(
            label = "Animation Speed",
            value = tuning.speed,
            min = 0.1f,
            max = 3.0f,
            onChange = { onTuningChange(tuning.copy(speed = it)) },
            valueFormatter = { String.format("%.1fx", it) }
        )
        SliderRow(
            label = "Motion Intensity",
            value = tuning.intensity,
            min = 0f,
            max = 2.0f,
            onChange = { onTuningChange(tuning.copy(intensity = it)) },
            valueFormatter = { "${(it * 100).toInt()}%" }
        )
        SliderRow(
            label = "Visual Complexity",
            value = tuning.complexity,
            min = 0f,
            max = 1.0f,
            onChange = { onTuningChange(tuning.copy(complexity = it)) },
            valueFormatter = { "${(it * 100).toInt()}%" }
        )
        SliderRow(
            label = "Color Shift",
            value = tuning.colorShift,
            min = -1.0f,
            max = 1.0f,
            onChange = { onTuningChange(tuning.copy(colorShift = it)) },
            valueFormatter = { String.format("%+.2f", it) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackgroundSelector(
    background: Background,
    config: BackgroundConfig,
    onBackgroundChange: (Background) -> Unit,
    onConfigChange: (BackgroundConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Theme Presets", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Presets Chip Cloud
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Background.values().forEach { bg ->
                FilterChip(
                    selected = background == bg,
                    onClick = { onBackgroundChange(bg) },
                    label = { Text(formatBackgroundName(bg), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Display Tuning", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SliderRow(
            label = "Opacity",
            value = config.opacity,
            min = 0f,
            max = 1f,
            onChange = { onConfigChange(config.copy(opacity = it)) },
            valueFormatter = { "${(it * 100).toInt()}%" }
        )
        SliderRow(
            label = "Blur Effect",
            value = config.blur,
            min = 0f,
            max = 30f,
            onChange = { onConfigChange(config.copy(blur = it)) },
            valueFormatter = { "${it.toInt()} px" }
        )
        SliderRow(
            label = "Scale",
            value = config.scale,
            min = 0.5f,
            max = 2.0f,
            onChange = { onConfigChange(config.copy(scale = it)) },
            valueFormatter = { String.format("%.2fx", it) }
        )
        SliderRow(
            label = "Speed",
            value = config.animationSpeed,
            min = 0.1f,
            max = 3.0f,
            onChange = { onConfigChange(config.copy(animationSpeed = it)) },
            valueFormatter = { String.format("%.1fx", it) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypographyControls(
    project: Project,
    onProjectChange: (Project) -> Unit,
) {
    val fontOptions = listOf("Inter", "Roboto", "Cinzel", "Space Mono", "Montserrat", "Playfair")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Font Family", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fontOptions.forEach { font ->
                FilterChip(
                    selected = project.fontFamily == font,
                    onClick = { onProjectChange(project.copy(fontFamily = font)) },
                    label = { Text(font, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        OutlinedTextField(
            value = project.fontFamily,
            onValueChange = { onProjectChange(project.copy(fontFamily = it)) },
            label = { Text("Custom Font Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val weightLabel = when {
            project.fontWeight <= 200 -> "Thin"
            project.fontWeight <= 300 -> "Light"
            project.fontWeight <= 400 -> "Regular"
            project.fontWeight <= 500 -> "Medium"
            project.fontWeight <= 700 -> "Bold"
            else -> "Extra Bold"
        }

        SliderRow(
            label = "Font Weight: $weightLabel",
            value = project.fontWeight.toFloat(),
            min = 100f,
            max = 900f,
            onChange = { onProjectChange(project.copy(fontWeight = (it / 100).toInt() * 100)) },
            valueFormatter = { "${it.toInt()}" }
        )

        SliderRow(
            label = "Font Size Scale",
            value = project.fontSize,
            min = 0.5f,
            max = 3.0f,
            onChange = { onProjectChange(project.copy(fontSize = it)) },
            valueFormatter = { String.format("%.2fx", it) }
        )

        SliderRow(
            label = "Line Height",
            value = project.lineHeight,
            min = 0.8f,
            max = 2.5f,
            onChange = { onProjectChange(project.copy(lineHeight = it)) },
            valueFormatter = { String.format("%.2fx", it) }
        )

        SliderRow(
            label = "Letter Spacing",
            value = project.letterSpacing,
            min = -0.5f,
            max = 1.0f,
            onChange = { onProjectChange(project.copy(letterSpacing = it)) },
            valueFormatter = { String.format("%+.2f", it) }
        )

        // Color Palette Selector
        Text("Text Color", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val colorPresets = listOf(
            "#FFFFFF" to "White",
            "#BB86FC" to "Purple",
            "#03DAC6" to "Teal",
            "#FFD54F" to "Gold",
            "#FF4081" to "Pink",
            "#E0E0E0" to "Silver"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            colorPresets.forEach { (hex, name) ->
                val isSelected = project.textColor.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .clickable { onProjectChange(project.copy(textColor = hex)) }
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            painter = painterResource("ic_check"),
                            contentDescription = name,
                            tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Reactivity Active", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }

        if (enabled) {
            SliderRow("Bass Gain", config.bassGain, 0f, 3f, { onConfigChange(config.copy(bassGain = it)) }, { String.format("%.1fx", it) })
            SliderRow("Low-Mid Gain", config.lowMidGain, 0f, 3f, { onConfigChange(config.copy(lowMidGain = it)) }, { String.format("%.1fx", it) })
            SliderRow("Mid Gain", config.midGain, 0f, 3f, { onConfigChange(config.copy(midGain = it)) }, { String.format("%.1fx", it) })
            SliderRow("Vocal Gain", config.vocalGain, 0f, 3f, { onConfigChange(config.copy(vocalGain = it)) }, { String.format("%.1fx", it) })
            SliderRow("Treble Gain", config.trebleGain, 0f, 3f, { onConfigChange(config.copy(trebleGain = it)) }, { String.format("%.1fx", it) })
            SliderRow("Smoothing", config.smoothing, 0f, 1f, { onConfigChange(config.copy(smoothing = it)) }, { "${(it * 100).toInt()}%" })
            SliderRow("Threshold", config.threshold, 0f, 1f, { onConfigChange(config.copy(threshold = it)) }, { "${(it * 100).toInt()}%" })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntroControls(
    enabled: Boolean,
    duration: Double,
    style: IntroStyle,
    onEnabledChange: (Boolean) -> Unit,
    onDurationChange: (Double) -> Unit,
    onStyleChange: (IntroStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Intro Title Sequence", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }

        if (enabled) {
            SliderRow(
                label = "Duration",
                value = duration.toFloat(),
                min = 0.5f,
                max = 8.0f,
                onChange = { onDurationChange(it.toDouble()) },
                valueFormatter = { String.format("%.1fs", it) }
            )

            Text("Animation Style", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IntroStyle.values().forEach { s ->
                    FilterChip(
                        selected = style == s,
                        onClick = { onStyleChange(s) },
                        label = { Text(s.name.replace('_', ' '), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportSettingsControls(
    format: ExportFormat,
    fps: Int,
    quality: ExportQuality,
    onFormatChange: (ExportFormat) -> Unit,
    onFpsChange: (Int) -> Unit,
    onQualityChange: (ExportQuality) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Video Format", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportFormat.values().forEach { f ->
                FilterChip(
                    selected = format == f,
                    onClick = { onFormatChange(f) },
                    label = { Text(f.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Text("Target Frame Rate", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(24, 30, 60).forEach { targetFps ->
                FilterChip(
                    selected = fps == targetFps,
                    onClick = { onFpsChange(targetFps) },
                    label = { Text("$targetFps FPS") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Text("Quality Profile", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportQuality.values().forEach { q ->
                FilterChip(
                    selected = quality == q,
                    onClick = { onQualityChange(q) },
                    label = { Text(q.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { String.format("%.2f", it) },
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueFormatter(value),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

private fun formatTemplateName(template: Template): String {
    return template.name.split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun formatBackgroundName(bg: Background): String {
    return bg.name.split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}