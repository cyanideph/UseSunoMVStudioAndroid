package com.usesuno.mvstudio.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.AudioFrame
import com.usesuno.mvstudio.domain.model.AudioAnalysis
import com.usesuno.mvstudio.render.engine.RendererFactory
import com.usesuno.mvstudio.domain.audio.PlaybackClock

@Composable
fun StagePreview(
    project: Project,
    modifier: Modifier = Modifier,
    playbackClock: PlaybackClock? = null,
    audioAnalysis: AudioAnalysis? = null,
) {
    var renderer by remember { mutableStateOf<com.usesuno.mvstudio.render.engine.RenderEngine?>(null) }
    var currentTime by remember { mutableStateOf(0.0) }
    var currentAudioFrame by remember { mutableStateOf<AudioFrame?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bounds = android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt())
            drawIntoCanvas { canvas ->
                renderer?.renderFrame(canvas.nativeCanvas, currentTime, currentAudioFrame, bounds)
            }
        }
    }

    // Initialize renderer
    LaunchedEffect(project.template, project.resolution, project.aspectRatio) {
        renderer = RendererFactory.createRenderer(project.template)
        val width = project.resolution.width
        val height = project.resolution.height
        renderer?.initialize(project, width, height)
    }

    // Update renderer settings when project changes
    LaunchedEffect(project) {
        renderer?.updateSettings(project)
    }

    // Connect to playback clock
    LaunchedEffect(playbackClock) {
        playbackClock?.currentTime?.collect { time ->
            currentTime = time
        }
    }
}