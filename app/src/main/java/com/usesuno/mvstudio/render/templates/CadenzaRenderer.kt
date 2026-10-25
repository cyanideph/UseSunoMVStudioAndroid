package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/**
 * Cadenza template - flowing ribbon/wave visualization
 */
class CadenzaRenderer : Canvas2DRenderer(Template.CADENZA) {

    private val ribbons = mutableListOf<Ribbon>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    data class Ribbon(
        var phase: Float,
        var amplitude: Float,
        var frequency: Float,
        var hue: Float,
        var yOffset: Float,
        var speed: Float,
    )

    override fun onInitialize() {
        // Create multiple ribbon layers
        repeat(5) { i ->
            ribbons.add(Ribbon(
                phase = (Math.random() * 2 * PI).toFloat(),
                amplitude = (Math.random() * 80 + 40).toFloat(),
                frequency = (Math.random() * 0.02 + 0.01).toFloat(),
                hue = (Math.random() * 60 + 200).toFloat(), // Blue-purple range
                yOffset = (i - 2) * 120f,
                speed = (Math.random() * 0.5 + 0.5).toFloat(),
            ))
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f
        val audioTreble = audioFrame?.treble ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain
        val trebleInfluence = audioTreble * reactivity.trebleGain
        val powerInfluence = audioPower * reactivity.threshold

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        for (ribbon in ribbons) {
            // Update ribbon parameters based on audio
            ribbon.amplitude = max(20f, ribbon.amplitude + (bassInfluence * 100 - ribbon.amplitude) * 0.1f)
            ribbon.frequency = max(0.005f, ribbon.frequency + (trebleInfluence * 0.02 - ribbon.frequency) * 0.1f)
            ribbon.phase += ribbon.speed * 0.02f

            drawRibbon(canvas, ribbon, centerX, centerY, powerInfluence)
        }
    }

    private fun drawRibbon(canvas: Canvas, ribbon: Ribbon, centerX: Float, centerY: Float, power: Float) {
        val path = Path()
        val fillPath = Path()
        val width = bounds.width().toFloat()
        val step = width / 100f

        var first = true
        var fillFirst = true

        for (i in 0..100) {
            val x = i * step
            val normalizedX = x / width
            val wave = sin(ribbon.phase + normalizedX * ribbon.frequency * width * 2 * PI)
            val y = centerY + ribbon.yOffset + wave * ribbon.amplitude

            if (first) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
                first = false
                fillFirst = false
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Close fill path to bottom
        fillPath.lineTo(width, bounds.bottom.toFloat())
        fillPath.lineTo(0f, bounds.bottom.toFloat())
        fillPath.close()

        // Audio-reactive color
        val hue = (ribbon.hue + power * 60).toFloat()
        val color = android.graphics.Color.HSVToColor(200, floatArrayOf(hue, 0.7f, 0.9f))
        val fillColor = android.graphics.Color.HSVToColor(60, floatArrayOf(hue, 0.5f, 0.8f))

        paint.color = color
        paint.strokeWidth = 2f + power * 4f
        canvas.drawPath(path, paint)

        fillPaint.color = fillColor
        canvas.drawPath(fillPath, fillPaint)
    }

    override fun dispose() {
        ribbons.clear()
    }
}