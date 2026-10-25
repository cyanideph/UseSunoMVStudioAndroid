package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * Classic template - clean typography with subtle animations
 */
class ClassicRenderer : Canvas2DRenderer(Template.CLASSIC) {

    private val lineAnimations = mutableMapOf<String, LineAnimation>()

    data class LineAnimation(
        var progress: Float = 0f,
        var targetProgress: Float = 0f,
        var opacity: Float = 0f,
        var scale: Float = 0.8f,
        var offsetY: Float = 50f,
    )

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val lineHeight = textPaint.textSize * project.lineHeight

        val visibleLines = project.lines.filter { it.start <= time && it.end >= time }

        for ((index, line) in visibleLines.withIndex()) {
            val anim = lineAnimations.getOrPut(line.id) { LineAnimation() }

            // Calculate target progress based on line timing
            val lineProgress = when {
                time < line.start -> 0f
                time > line.end -> 1f
                else -> ((time - line.start) / (line.end - line.start)).toFloat()
            }
            anim.targetProgress = lineProgress

            // Smooth animation
            anim.progress += (anim.targetProgress - anim.progress) * 0.15f
            anim.opacity += (lineProgress - anim.opacity) * 0.2f
            anim.scale += (0.8f + 0.2f * lineProgress - anim.scale) * 0.15f
            anim.offsetY += (50f * (1f - lineProgress) - anim.offsetY) * 0.15f

            // Audio reactivity - subtle scale pulse on bass
            val bass = audioFrame?.bass ?: 0f
            val reactivity = project.audioReactivityConfig
            val bassPulse = 1f + bass * reactivity.bassGain * 0.05f

            val y = centerY + (index - visibleLines.size / 2f) * lineHeight - anim.offsetY
            val alpha = (255 * anim.opacity * introProgress).toInt()

            canvas.save()
            canvas.translate(centerX, y)
            canvas.scale(anim.scale * bassPulse, anim.scale * bassPulse)

            textPaint.alpha = alpha
            canvas.drawText(line.text, 0f, 0f, textPaint)

            canvas.restore()
        }

        // Clean up animations for lines no longer visible
        val visibleIds = visibleLines.map { it.id }.toSet()
        lineAnimations.keys.filter { it !in visibleIds }.forEach { lineAnimations.remove(it) }
    }

    override fun onTemplateChanged(oldTemplate: Template, newTemplate: Template) {
        lineAnimations.clear()
    }
}