package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer

/**
 * Still template - Minimal static lyrics display
 */
class StillRenderer : Canvas2DRenderer(Template.STILL) {

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val lineHeight = textPaint.textSize * project.lineHeight

        val visibleLines = project.lines.filter { it.start <= time && it.end >= time }

        for ((index, line) in visibleLines.withIndex()) {
            val y = centerY + (index - visibleLines.size / 2f) * lineHeight

            // Simple fade in/out at line boundaries
            val fadeInDuration = 0.3
            val fadeOutDuration = 0.3
            var alpha = 1f

            if (time - line.start < fadeInDuration) {
                alpha = ((time - line.start) / fadeInDuration).toFloat()
            } else if (line.end - time < fadeOutDuration) {
                alpha = ((line.end - time) / fadeOutDuration).toFloat()
            }

            alpha *= introProgress
            textPaint.alpha = (255 * alpha).toInt()

            canvas.drawText(line.text, centerX, y, textPaint)
        }
    }
}