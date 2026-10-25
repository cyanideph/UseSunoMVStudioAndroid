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
 * Fume template renderer - flowing particle/smoke effect with audio reactivity
 * Ported from Folia's Fume template
 */
class FumeRenderer : Canvas2DRenderer(Template.FUME) {

    private val particles = mutableListOf<Particle>()
    private val trails = mutableListOf<Trail>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val trailPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }
    private var timeOffset = 0.0
    private var lastAudioPower = 0f

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var hue: Float,
        var life: Float,
        var maxLife: Float,
        var trail: MutableList<Point> = mutableListOf(),
    ) {
        data class Point(val x: Float, val y: Float, val alpha: Float)
    }

    data class Trail(
        var points: MutableList<Point> = mutableListOf(),
        var hue: Float,
        var width: Float,
    ) {
        data class Point(val x: Float, val y: Float, val alpha: Float)
    }

    override fun onInitialize() {
        spawnParticles(50)
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f
        val audioTreble = audioFrame?.treble ?: 0f

        // Audio reactivity
        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain
        val trebleInfluence = audioTreble * reactivity.trebleGain
        val powerInfluence = audioPower * reactivity.threshold

        // Spawn new particles based on audio
        if (powerInfluence > 0.3) {
            spawnParticles((powerInfluence * 10).toInt())
        }

        // Update and draw particles
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val radius = min(width, height) * 0.35f

        for (particle in particles) {
            updateParticle(particle, time, bassInfluence, trebleInfluence, centerX, centerY, radius)
            drawParticle(canvas, particle)
        }

        // Update and draw trails
        for (trail in trails) {
            updateTrail(trail)
            drawTrail(canvas, trail)
        }

        // Clean up dead particles
        particles.removeAll { it.life <= 0 }
        trails.removeAll { it.points.size < 2 }

        // Limit particle count
        if (particles.size > 200) {
            particles.sortBy { it.life }
            particles.subList(0, particles.size - 200).clear()
        }
    }

    private fun spawnParticles(count: Int) {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val radius = min(width, height) * 0.3f

        repeat(count) {
            val angle = (Math.random() * 2 * PI).toFloat()
            val dist = (Math.random() * radius * 0.5).toFloat()
            val hue = (Math.random() * 360).toFloat()

            particles.add(Particle(
                x = centerX + cos(angle) * dist,
                y = centerY + sin(angle) * dist,
                vx = (Math.random() - 0.5).toFloat() * 50,
                vy = (Math.random() - 0.5).toFloat() * 50,
                size = (Math.random() * 20 + 5).toFloat(),
                hue = hue,
                life = (Math.random() * 2 + 1).toFloat(),
                maxLife = (Math.random() * 3 + 2).toFloat(),
            ))
        }
    }

    private fun updateParticle(
        particle: Particle,
        time: Double,
        bass: Float,
        treble: Float,
        centerX: Float,
        centerY: Float,
        radius: Float,
    ) {
        val dt = 1.0 / 30.0 // Fixed timestep

        // Audio-reactive forces
        val dx = particle.x - centerX
        val dy = particle.y - centerY
        val dist = sqrt(dx * dx + dy * dy)

        // Bass pushes outward, treble pulls inward
        val force = (bass * 200 - treble * 100) * project.templateTuning.intensity
        if (dist > 0) {
            particle.vx += (dx / dist) * force * dt
            particle.vy += (dy / dist) * force * dt
        }

        // Swirl motion
        val swirlAngle = (time * 0.5 + particle.hue / 360.0 * PI * 2).toFloat()
        val swirlForce = 30f * project.templateTuning.speed
        particle.vx += cos(swirlAngle) * swirlForce * dt
        particle.vy += sin(swirlAngle) * swirlForce * dt

        // Damping
        particle.vx *= 0.98f
        particle.vy *= 0.98f

        // Update position
        particle.x += particle.vx * dt
        particle.y += particle.vy * dt

        // Boundary constraint
        val newDist = sqrt((particle.x - centerX).pow(2) + (particle.y - centerY).pow(2))
        if (newDist > radius) {
            val nx = (particle.x - centerX) / newDist
            val ny = (particle.y - centerY) / newDist
            particle.x = centerX + nx * radius
            particle.y = centerY + ny * radius
            particle.vx *= -0.5f
            particle.vy *= -0.5f
        }

        // Update trail
        particle.trail.add(Particle.Point(particle.x, particle.y, particle.life / particle.maxLife))
        if (particle.trail.size > 20) particle.trail.removeAt(0)

        // Aging
        particle.life -= dt
        particle.size *= 0.995f
    }

    private fun drawParticle(canvas: Canvas, particle: Particle) {
        val alpha = (particle.life / particle.maxLife * 255).toInt()
        val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(particle.hue, 0.8f, 1f))
        paint.color = color

        canvas.drawCircle(particle.x, particle.y, particle.size, paint)

        // Draw trail
        if (particle.trail.size > 1) {
            val path = Path()
            var first = true
            for (point in particle.trail) {
                val trailAlpha = (point.alpha * alpha * 0.5).toInt()
                if (trailAlpha < 10) continue
                val trailColor = android.graphics.Color.HSVToColor(trailAlpha, floatArrayOf(particle.hue, 0.6f, 1f))
                trailPaint.color = trailColor
                trailPaint.strokeWidth = particle.size * point.alpha * 0.5f

                if (first) {
                    path.moveTo(point.x, point.y)
                    first = false
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            canvas.drawPath(path, trailPaint)
        }
    }

    private fun updateTrail(trail: Trail) {
        // Trails fade out
        trail.points = trail.points.map { it.copy(alpha = it.alpha * 0.95f) }.toMutableList()
        trail.points.removeAll { it.alpha < 0.05f }
    }

    private fun drawTrail(canvas: Canvas, trail: Trail) {
        if (trail.points.size < 2) return

        val path = Path()
        var first = true
        for (point in trail.points) {
            val alpha = (point.alpha * 255).toInt()
            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(trail.hue, 0.5f, 1f))
            trailPaint.color = color
            trailPaint.strokeWidth = trail.width * point.alpha

            if (first) {
                path.moveTo(point.x, point.y)
                first = false
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        canvas.drawPath(path, trailPaint)
    }

    override fun dispose() {
        particles.clear()
        trails.clear()
    }
}