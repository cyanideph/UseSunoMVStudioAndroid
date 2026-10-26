package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Tilt template - 3D tilted text with perspective effect
 */
class TiltRenderer : Canvas2DRenderer(Template.TILT) {

    private var rotationX = 0.0
    private var rotationY = 0.0
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        // Auto-rotate based on audio
        rotationY += (0.01 + bassInfluence * 0.05) * project.templateTuning.speed
        rotationX += (0.005 + audioPower * 0.02) * project.templateTuning.speed

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        // Draw background grid
        drawGrid(canvas, centerX, centerY, rotationX, rotationY)

        // Draw lyrics with 3D tilt
        drawTiltedLyrics(canvas, centerX, centerY, rotationX, rotationY)
    }

    private fun drawGrid(canvas: Canvas, centerX: Float, centerY: Float, rx: Double, ry: Double) {
        paint.color = android.graphics.Color.parseColor("#FFFFFF10")
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE

        val spacing = 60f
        val gridSize = 10

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate((ry * 180 / PI).toFloat(), 0f, 0f)
        canvas.rotate((rx * 180 / PI).toFloat(), 1f, 0f)

        for (i in -gridSize..gridSize) {
            val offset = i * spacing
            canvas.drawLine(-gridSize * spacing, offset, gridSize * spacing, offset, paint)
            canvas.drawLine(offset, -gridSize * spacing, offset, gridSize * spacing, paint)
        }

        canvas.restore()
    }

    private fun drawTiltedLyrics(canvas: Canvas, centerX: Float, centerY: Float, rx: Double, ry: Double) {
        val visibleLines = project.lines.filter { it.start <= animationTime && it.end >= animationTime }
        val lineHeight = textPaint.textSize * project.lineHeight

        for ((index, line) in visibleLines.withIndex()) {
            val y = centerY + (index - visibleLines.size / 2f) * lineHeight

            canvas.save()
            canvas.translate(centerX, y)
            canvas.rotate((ry * 180 / PI).toFloat(), 0f, 0f)
            canvas.rotate((rx * 180 / PI).toFloat(), 1f, 0f)
            canvas.scale(0.8f, 0.8f)

            textPaint.alpha = (255 * introProgress).toInt()
            canvas.drawText(line.text, 0f, 0f, textPaint)

            canvas.restore()
        }
    }
}