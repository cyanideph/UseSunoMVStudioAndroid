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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Syzygy template - Planetary alignment with orbital mechanics
 */
class SyzygyRenderer : Canvas2DRenderer(Template.SYZYGY) {

    private val planets = mutableListOf<Planet>()
    private val orbits = mutableListOf<OrbitPath>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val orbitPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1f }

    override val foliaStyle = object : com.usesuno.mvstudio.render.engine.FoliaLyricStyle {
        override fun drawLineAccents(
            canvas: Canvas,
            line: LyricLine,
            centerX: Float,
            lineY: Float,
            lineWidth: Float,
            isActive: Boolean,
            distFromCenter: Float,
            lineAlpha: Float,
            audioFrame: AudioFrame?,
        ) {
            if (isActive && lineAlpha > 0.4f) {
                val orbitRadiusX = lineWidth * 0.6f
                val orbitRadiusY = textPaint.textSize * 0.45f
                val haloPaint = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    color = 0xFF88CCFF.toInt()
                    alpha = (65 * lineAlpha).toInt().coerceIn(0, 255)
                }
                val oval = android.graphics.RectF(
                    centerX - orbitRadiusX,
                    lineY - orbitRadiusY,
                    centerX + orbitRadiusX,
                    lineY + orbitRadiusY,
                )
                canvas.drawOval(oval, haloPaint)
            }
        }
    }

    data class Planet(
        var angle: Float,
        var distance: Float,
        var radius: Float,
        var hue: Float,
        var orbitalSpeed: Float,
        var name: String,
        var x: Float = 0f,
        var y: Float = 0f,
    )

    data class OrbitPath(
        var centerX: Float,
        var centerY: Float,
        var radius: Float,
        var hue: Float,
    )

    override fun onInitialize() {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        // Create central "sun"
        planets.add(Planet(
            angle = 0f,
            distance = 0f,
            radius = 40f,
            hue = 50f, // Yellow
            orbitalSpeed = 0f,
            name = "Sun",
        ))

        // Create orbiting planets
        val planetData = listOf(
            Planet(0f, 120f, 8f, 200f, 0.02f, "Mercury"),
            Planet(PI.toFloat() / 4f, 180f, 12f, 30f, 0.015f, "Venus"),
            Planet(PI.toFloat() / 2f, 250f, 15f, 220f, 0.01f, "Earth"),
            Planet(3f * PI.toFloat() / 4f, 330f, 10f, 10f, 0.008f, "Mars"),
            Planet(PI.toFloat(), 450f, 25f, 40f, 0.005f, "Jupiter"),
            Planet(5f * PI.toFloat() / 4f, 550f, 20f, 30f, 0.003f, "Saturn"),
        )

        for (p in planetData) {
            planets.add(p)
            orbits.add(OrbitPath(centerX, centerY, p.distance, p.hue))
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioBass = audioFrame?.bass ?: 0f
        val audioPower = audioFrame?.power ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        // Draw orbits
        for (orbit in orbits) {
            orbitPaint.color = android.graphics.Color.HSVToColor(50, floatArrayOf(orbit.hue, 0.3f, 0.5f))
            canvas.drawCircle(orbit.centerX, orbit.centerY, orbit.radius, orbitPaint)
        }

        // Update and draw planets
        for (planet in planets) {
            if (planet.distance > 0) {
                planet.angle += planet.orbitalSpeed * project.templateTuning.speed
                // Bass speeds up orbits
                planet.angle += bassInfluence * 0.01f
            }

            val x = centerX + cos(planet.angle) * planet.distance
            val y = centerY + sin(planet.angle) * planet.distance

            planet.x = x
            planet.y = y

            // Draw planet
            val alpha = if (planet.distance == 0f) 255 else 200
            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(planet.hue, 0.7f, 0.9f))
            paint.color = color

            canvas.drawCircle(x, y, planet.radius * (1f + bassInfluence * 0.5f), paint)

            // Draw glow for sun
            if (planet.distance == 0f) {
                val glowPaint = Paint(paint)
                glowPaint.color = android.graphics.Color.HSVToColor(60, floatArrayOf(planet.hue, 0.5f, 1f))
                canvas.drawCircle(x, y, planet.radius * 2f, glowPaint)
            }

            // Saturn ring
            if (planet.name == "Saturn") {
                val ringPaint = Paint(paint)
                ringPaint.style = Paint.Style.STROKE
                ringPaint.strokeWidth = 3f
                canvas.drawCircle(x, y, planet.radius * 1.8f, ringPaint)
            }
        }

        // Draw alignment lines (syzygy)
        drawAlignments(canvas, centerX, centerY)
    }

    private fun drawAlignments(canvas: Canvas, centerX: Float, centerY: Float) {
        val alignmentPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = android.graphics.Color.parseColor("#FFFFFF20")
        }

        // Check for near-alignments
        val orbitingPlanets = planets.filter { it.distance > 0 }

        for (i in 0 until orbitingPlanets.size) {
            for (j in i + 1 until orbitingPlanets.size) {
                val p1 = orbitingPlanets[i]
                val p2 = orbitingPlanets[j]

                val fullTurn = 2f * PI.toFloat()
                val angleDiff = abs(p1.angle - p2.angle) % fullTurn
                val aligned = angleDiff < 0.1f || angleDiff > fullTurn - 0.1f

                if (aligned) {
                    alignmentPaint.strokeWidth = 2f
                    alignmentPaint.color = android.graphics.Color.parseColor("#FFFFFF80")
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