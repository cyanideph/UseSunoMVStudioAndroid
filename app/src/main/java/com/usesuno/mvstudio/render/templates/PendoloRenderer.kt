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
 * Pendolo template - Pendulum wave simulation
 */
class PendoloRenderer : Canvas2DRenderer(Template.PENDOLO) {

    private val pendulums = mutableListOf<Pendulum>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val linePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }

    data class Pendulum(
        var angle: Float,
        var angularVelocity: Float,
        var length: Float,
        var hue: Float,
        var bobRadius: Float,
        var x: Float,
        var y: Float,
    )

    override fun onInitialize() {
        val numPendulums = 15
        val baseLength = 100f
        val lengthStep = 30f

        for (i in 0 until numPendulums) {
            val length = baseLength + i * lengthStep
            // Period = 2π * sqrt(L/g), so frequency proportional to 1/sqrt(L)
            val frequency = 1f / sqrt(length / 100f)

            pendulums.add(Pendulum(
                angle = (Math.random() * 0.5 - 0.25).toFloat(),
                angularVelocity = 0f,
                length = length,
                hue = (i * 360f / numPendulums).toFloat(),
                bobRadius = (15f - i * 0.5f).coerceAtLeast(5f),
                x = 0f,
                y = 0f,
            ))
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioBass = audioFrame?.bass ?: 0f
        val audioPower = audioFrame?.power ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        val centerX = bounds.centerX()
        val topY = 100f
        val dt = 1f / 30f
        val gravity = 9.8f * 50f // Scaled for pixels

        for ((i, pendulum) in pendulums.withIndex()) {
            // Physics: d²θ/dt² = -g/L * sin(θ)
            val angularAccel = -gravity / pendulum.length * sin(pendulum.angle)
            pendulum.angularVelocity += angularAccel * dt
            pendulum.angularVelocity *= 0.999f // Damping

            // Audio influence - bass adds energy
            pendulum.angularVelocity += (Math.random() - 0.5).toFloat() * bassInfluence * 0.5f * dt

            pendulum.angle += pendulum.angularVelocity * dt

            // Position
            pendulum.x = centerX + sin(pendulum.angle) * pendulum.length
            pendulum.y = topY + cos(pendulum.angle) * pendulum.length

            // Draw string
            linePaint.color = android.graphics.Color.parseColor("#FFFFFF40")
            canvas.drawLine(centerX, topY, pendulum.x, pendulum.y, linePaint)

            // Draw bob
            val color = android.graphics.Color.HSVToColor(255, floatArrayOf(pendulum.hue, 0.8f, 1f))
            paint.color = color
            canvas.drawCircle(pendulum.x, pendulum.y, pendulum.bobRadius, paint)

            // Draw trail
            drawTrail(canvas, pendulum, i)
        }
    }

    private fun drawTrail(canvas: Canvas, pendulum: Pendulum, index: Int) {
        // Simple trail effect - draw previous positions
        val trailPaint = Paint(paint)
        trailPaint.style = Paint.Style.STROKE
        trailPaint.strokeWidth = pendulum.bobRadius * 0.5f
        trailPaint.color = android.graphics.Color.HSVToColor(60, floatArrayOf(pendulum.hue, 0.6f, 1f))

        // Would need to store position history for proper trail
    }

    override fun dispose() {
        pendulums.clear()
    }
}