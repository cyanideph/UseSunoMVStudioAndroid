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

/**
 * Chroma template - Color channel separation with glitch effects
 */
class ChromaRenderer : Canvas2DRenderer(Template.CHROMA) {

    private val glitchLines = mutableListOf<GlitchLine>()
    private val rgbLayers = arrayOf(
        Paint().apply { isAntiAlias = true; colorFilter = android.graphics.PorterDuffColorFilter(0xFFFF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_IN) },
        Paint().apply { isAntiAlias = true; colorFilter = android.graphics.PorterDuffColorFilter(0xFF00FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_IN) },
        Paint().apply { isAntiAlias = true; colorFilter = android.graphics.PorterDuffColorFilter(0xFF0000FF.toInt(), android.graphics.PorterDuff.Mode.SRC_IN) },
    )
    private var glitchTimer = 0.0

    data class GlitchLine(
        var y: Float,
        var offsetX: Float,
        var width: Float,
        var hue: Float,
        var life: Float,
        var channel: Int, // 0=R, 1=G, 2=B
    )

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioTreble = audioFrame?.treble ?: 0f
        val audioBass = audioFrame?.bass ?: 0f

        val reactivity = project.audioReactivityConfig
        val trebleInfluence = audioTreble * reactivity.trebleGain
        val bassInfluence = audioBass * reactivity.bassGain

        glitchTimer += 1.0 / 30.0

        // Spawn glitch lines on treble transients
        if (trebleInfluence > 0.5 && Math.random() < 0.2) {
            spawnGlitch(trebleInfluence)
        }

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        // Draw RGB separated text
        drawChromaText(canvas, centerX, centerY, bassInfluence, trebleInfluence)

        // Draw glitch lines
        for (glitch in glitchLines) {
            glitch.life -= 1f / 30f
            drawGlitchLine(canvas, glitch)
        }

        glitchLines.removeAll { it.life <= 0 }
    }

    private fun spawnGlitch(treble: Float) {
        val y = (Math.random() * height).toFloat()
        glitchLines.add(GlitchLine(
            y = y,
            offsetX = (Math.random() - 0.5).toFloat() * 50 * treble,
            width = (Math.random() * 20 + 5).toFloat(),
            hue = (Math.random() * 360).toFloat(),
            life = (Math.random() * 0.5 + 0.1).toFloat(),
            channel = (Math.random() * 3).toInt(),
        ))
    }

    private fun drawChromaText(canvas: Canvas, centerX: Float, centerY: Float, bass: Float, treble: Float) {
        val visibleLines = project.lines.filter { it.start <= animationTime && it.end >= animationTime }
        val lineHeight = textPaint.textSize * project.lineHeight

        val separation = (bass * 20 + treble * 30) * project.templateTuning.intensity

        for ((index, line) in visibleLines.withIndex()) {
            val y = centerY + (index - visibleLines.size / 2f) * lineHeight

            // Red channel (left)
            canvas.save()
            canvas.translate(-separation, 0f)
            textPaint.colorFilter = android.graphics.PorterDuffColorFilter(0xFFFF0000.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
            textPaint.alpha = (255 * introProgress).toInt()
            canvas.drawText(line.text, centerX, y, textPaint)
            canvas.restore()

            // Green channel (center)
            textPaint.colorFilter = android.graphics.PorterDuffColorFilter(0xFF00FF00.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
            textPaint.alpha = (255 * introProgress).toInt()
            canvas.drawText(line.text, centerX, y, textPaint)

            // Blue channel (right)
            canvas.save()
            canvas.translate(separation, 0f)
            textPaint.colorFilter = android.graphics.PorterDuffColorFilter(0xFF0000FF.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
            textPaint.alpha = (255 * introProgress).toInt()
            canvas.drawText(line.text, centerX, y, textPaint)
            canvas.restore()

            // Reset color filter
            textPaint.colorFilter = null
        }
    }

    private fun drawGlitchLine(canvas: Canvas, glitch: GlitchLine) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = glitch.width
            color = when (glitch.channel) {
                0 -> android.graphics.Color.RED
                1 -> android.graphics.Color.GREEN
                else -> android.graphics.Color.BLUE
            }
            alpha = (glitch.life * 255).toInt().coerceIn(0, 255)
        }

        val path = Path()
        path.moveTo(0f, glitch.y)
        path.lineTo(width.toFloat(), glitch.y)

        // Add horizontal offset in middle
        val midX = width / 2f
        path.moveTo(0f, glitch.y)
        path.lineTo(midX + glitch.offsetX, glitch.y)
        path.lineTo(width.toFloat(), glitch.y)

        canvas.drawPath(path, paint)
    }

    override fun dispose() {
        glitchLines.clear()
    }
}