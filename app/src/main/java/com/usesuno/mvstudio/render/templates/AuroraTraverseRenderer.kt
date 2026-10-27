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
 * Aurora Traverse - Flowing aurora curtains with magnetic field lines
 */
class AuroraTraverseRenderer : Canvas2DRenderer(Template.AURORA_TRAVERSE) {

    private val curtains = mutableListOf<AuroraCurtain>()
    private val fieldLines = mutableListOf<FieldLine>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val glowPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var timeOffset = 0.0

    override val foliaStyle = object : com.usesuno.mvstudio.render.engine.FoliaLyricStyle {
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
            if (isActive) {
                // Aurora borealis green-cyan luminescence
                val auroraGlow = Paint(textPaint).apply {
                    color = 0xFF55FFAA.toInt()
                    alpha = (90 * lineAlpha).toInt().coerceIn(0, 255)
                    setShadowLayer(18f, 0f, 0f, 0xFF00FFCC.toInt())
                }
                canvas.drawText(word.text, wordX, baselineY, auroraGlow)
            }
            return false
        }
    }

    data class AuroraCurtain(
        var x: Float,
        var baseY: Float,
        var width: Float,
        var height: Float,
        var hue: Float,
        var phase: Float,
        var waveSpeed: Float,
        var intensity: Float,
    )

    data class FieldLine(
        var startX: Float,
        var startY: Float,
        var angle: Float,
        var length: Float,
        var hue: Float,
        var life: Float,
    )

    override fun onInitialize() {
        val numCurtains = 5
        for (i in 0 until numCurtains) {
            curtains.add(AuroraCurtain(
                x = (i * width / numCurtains).toFloat(),
                baseY = (Math.random() * height * 0.5f).toFloat(),
                width = (width / numCurtains * 1.5f).toFloat(),
                height = (Math.random() * height * 0.3f + height * 0.2f).toFloat(),
                hue = (Math.random() * 60 + 140).toFloat(), // Green-cyan
                phase = (Math.random() * 2 * PI).toFloat(),
                waveSpeed = (Math.random() * 0.5 + 0.5).toFloat(),
                intensity = (Math.random() * 0.5 + 0.5).toFloat(),
            ))
        }

        // Field lines
        repeat(20) { spawnFieldLine() }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f
        val audioMid = audioFrame?.mid ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain
        val midInfluence = audioMid * reactivity.midGain

        // Update curtains
        for (curtain in curtains) {
            curtain.phase += curtain.waveSpeed * 0.02f * project.templateTuning.speed
            curtain.intensity = max(0.3f, curtain.intensity + (midInfluence - curtain.intensity) * 0.1f)
            drawCurtain(canvas, curtain, bassInfluence)
        }

        // Update field lines
        for (line in fieldLines) {
            line.life -= 1f / 30f
            drawFieldLine(canvas, line)
        }

        fieldLines.removeAll { it.life <= 0 }
        if (bassInfluence > 0.4 && Math.random() < 0.2) {
            spawnFieldLine()
        }
    }

    private fun drawCurtain(canvas: Canvas, curtain: AuroraCurtain, bass: Float) {
        val path = Path()
        val segments = 50

        for (i in 0..segments) {
            val t = i / segments.toFloat()
            val x = curtain.x + (t - 0.5f) * curtain.width

            // Multiple wave layers
            var y = curtain.baseY + t * curtain.height

            // Main wave
            y += (sin(curtain.phase + t * 4 * PI) * curtain.height * 0.15f * curtain.intensity).toFloat()

            // Secondary waves
            y += (sin(curtain.phase * 1.7f + t * 8 * PI) * curtain.height * 0.08f).toFloat()
            y += (sin(curtain.phase * 0.7f + t * 2 * PI) * curtain.height * 0.12f).toFloat()

            // Bass distortion
            y += (sin(curtain.phase * 3 + x * 0.01f) * bass * 30).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw glow behind
        val glowPath = Path(path)
        val bottomY = curtain.baseY + curtain.height + 100f
        glowPath.lineTo(width.toFloat(), bottomY)
        glowPath.lineTo(0f, bottomY)
        glowPath.close()

        val glowColor = android.graphics.Color.HSVToColor(30, floatArrayOf(curtain.hue, 0.8f, 0.9f))
        glowPaint.color = glowColor
        canvas.drawPath(glowPath, glowPaint)

        // Draw curtain edge
        val edgeColor = android.graphics.Color.HSVToColor(200, floatArrayOf(curtain.hue, 0.9f, 1f))
        paint.color = edgeColor
        paint.strokeWidth = 2f + bass * 3f
        canvas.drawPath(path, paint)

        // Inner highlight
        val highlightPaint = Paint(paint)
        highlightPaint.color = android.graphics.Color.HSVToColor(150, floatArrayOf(curtain.hue, 0.5f, 1f))
        highlightPaint.strokeWidth = 1f
        canvas.drawPath(path, highlightPaint)
    }

    private fun spawnFieldLine() {
        fieldLines.add(FieldLine(
            startX = (Math.random() * width).toFloat(),
            startY = height.toFloat(),
            angle = (Math.random() * PI / 2 - PI / 4).toFloat(), // Upward
            length = (Math.random() * height * 0.5f + height * 0.2f).toFloat(),
            hue = (Math.random() * 60 + 140).toFloat(),
            life = (Math.random() * 3 + 2).toFloat(),
        ))
    }

    private fun drawFieldLine(canvas: Canvas, line: FieldLine) {
        val alpha = (line.life / 3f * 100).toInt()
        val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(line.hue, 0.6f, 0.8f))
        paint.color = color
        paint.strokeWidth = 1f

        val endX = line.startX + cos(line.angle) * line.length
        val endY = line.startY - sin(line.angle) * line.length

        canvas.drawLine(line.startX, line.startY, endX, endY, paint)
    }

    override fun dispose() {
        curtains.clear()
        fieldLines.clear()
    }
}