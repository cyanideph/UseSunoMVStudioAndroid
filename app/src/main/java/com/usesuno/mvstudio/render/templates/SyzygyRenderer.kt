package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import com.usesuno.mvstudio.domain.model.AudioFrame
import com.usesuno.mvstudio.domain.model.Template
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Planetary alignment with orbital mechanics. */
class SyzygyRenderer : Canvas2DRenderer(Template.SYZYGY) {
    private val planets = mutableListOf<Planet>()
    private val orbits = mutableListOf<OrbitPath>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val orbitPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1f }

    data class Planet(
        var angle: Float,
        val distance: Float,
        val radius: Float,
        val hue: Float,
        val orbitalSpeed: Float,
        val name: String,
        var x: Float = 0f,
        var y: Float = 0f,
    )

    data class OrbitPath(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val hue: Float,
    )

    override fun onInitialize() {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        planets.clear()
        orbits.clear()
        planets.add(Planet(0f, 0f, 40f, 50f, 0f, "Sun"))
        val planetData = listOf(
            Planet(0f, 120f, 8f, 200f, 0.02f, "Mercury"),
            Planet(PI.toFloat() / 4f, 180f, 12f, 30f, 0.015f, "Venus"),
            Planet(PI.toFloat() / 2f, 250f, 15f, 220f, 0.01f, "Earth"),
            Planet(3f * PI.toFloat() / 4f, 330f, 10f, 10f, 0.008f, "Mars"),
            Planet(PI.toFloat(), 450f, 25f, 40f, 0.005f, "Jupiter"),
            Planet(5f * PI.toFloat() / 4f, 550f, 20f, 30f, 0.003f, "Saturn"),
        )
        planets.addAll(planetData)
        planetData.forEach { orbits.add(OrbitPath(centerX, centerY, it.distance, it.hue)) }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val bass = (audioFrame?.bass ?: 0f) * project.audioReactivityConfig.bassGain
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        for (orbit in orbits) {
            orbitPaint.color = android.graphics.Color.HSVToColor(
                50, floatArrayOf(orbit.hue, 0.3f, 0.5f),
            )
            canvas.drawCircle(orbit.centerX, orbit.centerY, orbit.radius, orbitPaint)
        }

        for (planet in planets) {
            if (planet.distance > 0f) {
                planet.angle += planet.orbitalSpeed * project.templateTuning.speed + bass * 0.01f
            }
            planet.x = centerX + cos(planet.angle) * planet.distance
            planet.y = centerY + sin(planet.angle) * planet.distance

            val alpha = if (planet.distance == 0f) 255 else 200
            paint.color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(planet.hue, 0.7f, 0.9f))
            canvas.drawCircle(planet.x, planet.y, planet.radius * (1f + bass * 0.5f), paint)

            if (planet.distance == 0f) {
                val glowPaint = Paint(paint).apply {
                    color = android.graphics.Color.HSVToColor(60, floatArrayOf(planet.hue, 0.5f, 1f))
                }
                canvas.drawCircle(planet.x, planet.y, planet.radius * 2f, glowPaint)
            }
            if (planet.name == "Saturn") {
                val ringPaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawCircle(planet.x, planet.y, planet.radius * 1.8f, ringPaint)
            }
        }
        drawAlignments(canvas)
    }

    private fun drawAlignments(canvas: Canvas) {
        val alignmentPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = android.graphics.Color.parseColor("#20FFFFFF")
        }
        val orbiting = planets.filter { it.distance > 0f }
        for (i in 0 until orbiting.size) {
            for (j in i + 1 until orbiting.size) {
                val p1 = orbiting[i]
                val p2 = orbiting[j]
                val diff = abs(p1.angle - p2.angle) % (2f * PI.toFloat())
                if (diff < 0.1f || diff > 2f * PI.toFloat() - 0.1f) {
                    alignmentPaint.strokeWidth = 2f
                    alignmentPaint.color = android.graphics.Color.parseColor("#80FFFFFF")
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, alignmentPaint)
                }
            }
        }
    }

    override fun dispose() {
        planets.clear()
        orbits.clear()
    }
}
