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
 * Claddagh template - Celtic knot inspired flowing lines
 */
class CladdaghRenderer : Canvas2DRenderer(Template.CLADDAGH) {

    private val knots = mutableListOf<CelticKnot>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    data class CelticKnot(
        var centerX: Float,
        var centerY: Float,
        var radius: Float,
        var phase: Float,
        var hue: Float,
        var rotationSpeed: Float,
        var numLoops: Int = 3,
    )

    override fun onInitialize() {
        repeat(4) { i ->
            knots.add(CelticKnot(
                centerX = bounds.centerX().toFloat() + (i - 1.5f) * 200f,
                centerY = bounds.centerY().toFloat(),
                radius = (Math.random() * 80 + 60).toFloat(),
                phase = (Math.random() * 2 * PI).toFloat(),
                hue = (Math.random() * 60 + 180).toFloat(), // Cyan-green
                rotationSpeed = (Math.random() * 0.5 + 0.2).toFloat(),
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

        for (knot in knots) {
            knot.phase += knot.rotationSpeed * 0.02f * project.templateTuning.speed
            knot.radius = max(30f, knot.radius + (bassInfluence * 50 - knot.radius) * 0.1f)

            drawCelticKnot(canvas, knot, trebleInfluence)
        }
    }

    private fun drawCelticKnot(canvas: Canvas, knot: CelticKnot, treble: Float) {
        val path = Path()
        val numPoints = 200

        for (i in 0..numPoints) {
            val t = i * 2 * PI / numPoints
            val r = knot.radius * (1 + 0.3f * sin(knot.numLoops * t + knot.phase))

            // Celtic knot parametric equations
            val x = r * cos(t + knot.phase)
            val y = r * sin(t + knot.phase) * 0.6f

            // Add interweaving
            val weave = 20f * sin(knot.numLoops * 2 * t + knot.phase)

            if (i == 0) {
                path.moveTo(knot.centerX + x.toFloat(), knot.centerY + (y + weave).toFloat())
            } else {
                path.lineTo(knot.centerX + x.toFloat(), knot.centerY + (y + weave).toFloat())
            }
        }
        path.close()

        // Audio-reactive color
        val hue = (knot.hue + treble * 40).toFloat()
        val color = android.graphics.Color.HSVToColor(200, floatArrayOf(hue, 0.8f, 0.9f))
        val fillColor = android.graphics.Color.HSVToColor(40, floatArrayOf(hue, 0.5f, 0.8f))

        paint.color = color
        paint.strokeWidth = 2f + treble * 4f
        canvas.drawPath(path, paint)

        fillPaint.color = fillColor
        canvas.drawPath(path, fillPaint)
    }

    override fun dispose() {
        knots.clear()
    }
}