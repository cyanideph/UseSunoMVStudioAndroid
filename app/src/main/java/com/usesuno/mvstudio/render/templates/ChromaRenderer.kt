package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import com.usesuno.mvstudio.render.engine.FoliaLyricStyle
import kotlin.math.PI
import kotlin.math.sin

/**
 * Chroma template - Folia RGB color channel separation and glitch transients
 */
class ChromaRenderer : Canvas2DRenderer(Template.CHROMA) {

    private val glitchLines = mutableListOf<GlitchLine>()
    private val redFilter = PorterDuffColorFilter(0xFFFF2255.toInt(), PorterDuff.Mode.SRC_IN)
    private val blueFilter = PorterDuffColorFilter(0xFF2288FF.toInt(), PorterDuff.Mode.SRC_IN)
    private val greenFilter = PorterDuffColorFilter(0xFF22FF88.toInt(), PorterDuff.Mode.SRC_IN)
    private val channelPaint = Paint().apply { isAntiAlias = true }

    data class GlitchLine(
        var y: Float,
        var offsetX: Float,
        var width: Float,
        var life: Float,
        var channel: Int, // 0=R, 1=G, 2=B
    )

    override val foliaStyle = object : FoliaLyricStyle {
        override fun drawWord(
            canvas: Canvas,
            word: LyricWord,
            wordX: Float,
            baselineY: Float,
            wordWidth: Float,
            isActive: Boolean,
            isSpoken: Boolean,
            wordProgress: Float,
            lineAlpha: Float,
            textPaint: Paint,
            audioFrame: AudioFrame?,
        ): Boolean {
            val bass = audioFrame?.bass ?: 0f
            val treble = audioFrame?.treble ?: 0f
            val reactivity = project.audioReactivityConfig
            val bassInfluence = bass * reactivity.bassGain
            val trebleInfluence = treble * reactivity.trebleGain
            val intensity = project.templateTuning.intensity

            val separation = (bassInfluence * 14f + trebleInfluence * 20f) * intensity

            channelPaint.set(textPaint)

            if (isActive) {
                val activeScale = 1.0f + 0.05f * sin(wordProgress * PI.toFloat()) + treble * 0.03f
                canvas.save()
                val centerX = wordX + wordWidth / 2f
                canvas.translate(centerX, baselineY)
                canvas.scale(activeScale, activeScale)
                canvas.translate(-centerX, -baselineY)

                val alphaVal = (255 * lineAlpha).toInt().coerceIn(0, 255)

                // Red channel shifted left
                channelPaint.colorFilter = redFilter
                channelPaint.alpha = (alphaVal * 0.85f).toInt().coerceIn(0, 255)
                canvas.drawText(word.text, wordX - separation, baselineY, channelPaint)

                // Blue channel shifted right
                channelPaint.colorFilter = blueFilter
                channelPaint.alpha = (alphaVal * 0.85f).toInt().coerceIn(0, 255)
                canvas.drawText(word.text, wordX + separation, baselineY, channelPaint)

                // Crisp Center Luminescent channel
                channelPaint.colorFilter = null
                channelPaint.alpha = alphaVal
                channelPaint.setShadowLayer(16f, 0f, 0f, Color.WHITE)
                canvas.drawText(word.text, wordX, baselineY, channelPaint)

                canvas.restore()
                return true
            } else if (separation > 4f) {
                // Audio transient glitch on other words
                val baseAlpha = if (isSpoken) 0.90f else 0.42f
                val alphaVal = (255 * lineAlpha * baseAlpha).toInt().coerceIn(0, 255)

                channelPaint.colorFilter = redFilter
                channelPaint.alpha = (alphaVal * 0.6f).toInt().coerceIn(0, 255)
                canvas.drawText(word.text, wordX - separation * 0.5f, baselineY, channelPaint)

                channelPaint.colorFilter = blueFilter
                channelPaint.alpha = (alphaVal * 0.6f).toInt().coerceIn(0, 255)
                canvas.drawText(word.text, wordX + separation * 0.5f, baselineY, channelPaint)

                channelPaint.colorFilter = null
                channelPaint.alpha = alphaVal
                canvas.drawText(word.text, wordX, baselineY, channelPaint)
                return true
            }

            return false // Allow default Folia rendering
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioTreble = audioFrame?.treble ?: 0f
        val reactivity = project.audioReactivityConfig
        val trebleInfluence = audioTreble * reactivity.trebleGain

        // Spawn horizontal scan glitch lines on treble transients
        if (trebleInfluence > 0.45 && Math.random() < 0.25) {
            spawnGlitch(trebleInfluence)
        }

        // Draw glitch lines
        for (glitch in glitchLines) {
            glitch.life -= 1f / 30f
            drawGlitchLine(canvas, glitch)
        }
        glitchLines.removeAll { it.life <= 0 }
    }

    private fun spawnGlitch(treble: Float) {
        val y = (Math.random() * height).toFloat()
        glitchLines.add(
            GlitchLine(
                y = y,
                offsetX = (Math.random() - 0.5).toFloat() * 60 * treble,
                width = (Math.random() * 8 + 2).toFloat(),
                life = (Math.random() * 0.3 + 0.1).toFloat(),
                channel = (Math.random() * 3).toInt(),
            )
        )
    }

    private fun drawGlitchLine(canvas: Canvas, glitch: GlitchLine) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = glitch.width
            color = when (glitch.channel) {
                0 -> 0xFFFF2255.toInt()
                1 -> 0xFF22FF88.toInt()
                else -> 0xFF2288FF.toInt()
            }
            alpha = (glitch.life * 255).toInt().coerceIn(0, 255)
        }

        val path = Path()
        path.moveTo(0f, glitch.y)
        val midX = width / 2f
        path.lineTo(midX + glitch.offsetX, glitch.y)
        path.lineTo(width.toFloat(), glitch.y)

        canvas.drawPath(path, paint)
    }

    override fun dispose() {
        super.dispose()
        glitchLines.clear()
    }
}