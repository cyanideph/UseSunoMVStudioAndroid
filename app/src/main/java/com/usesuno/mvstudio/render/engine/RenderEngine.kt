package com.usesuno.mvstudio.render.engine

import android.graphics.Canvas
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.AudioFrame
import com.usesuno.mvstudio.domain.model.Project
import kotlinx.coroutines.flow.StateFlow

/**
 * Render engine interface for different template renderers
 */
interface RenderEngine {
    /**
     * Initialize the renderer with project settings
     */
    fun initialize(project: Project, width: Int, height: Int)

    /**
     * Render a frame at the given time
     */
    fun renderFrame(
        canvas: Canvas,
        time: Double,
        audioFrame: AudioFrame?,
        bounds: Rect,
    )

    /**
     * Update project settings (template tuning, colors, etc.)
     */
    fun updateSettings(project: Project)

    /**
     * Clean up resources
     */
    fun dispose()

    /**
     * Get the current template being rendered
     */
    val template: com.usesuno.mvstudio.domain.model.Template

    /**
     * Whether this renderer supports audio reactivity
     */
    val supportsAudioReactivity: Boolean
}

/**
 * Render context passed to renderers
 */
data class RenderContext(
    val project: Project,
    val width: Int,
    val height: Int,
    val time: Double,
    val audioFrame: AudioFrame?,
    val bounds: Rect,
    val audioReactivityConfig: com.usesuno.mvstudio.domain.model.AudioReactivityConfig,
)