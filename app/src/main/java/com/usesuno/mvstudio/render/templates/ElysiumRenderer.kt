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
 * Elysium template - Ethereal floating particles with golden ratio spirals
 */
class ElysiumRenderer : Canvas2DRenderer(Template.ELYSIUM) {

    private val particles = mutableListOf<ElysiumParticle>()
    private val spirals = mutableListOf<GoldenSpiral>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val spiralPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.5f }

    data class ElysiumParticle(
        var x: Float,
        var y: Float,
        var z: Float, // Depth for parallax
        var vx: Float,
        var vy: Float,
        var size: Float,
        var hue: Float,
        var brightness: Float,
        var life: Float,
        var maxLife: Float,
    )

    data class GoldenSpiral(
        var centerX: Float,
        var centerY: Float,
        var rotation: Float,
        var scale: Float,
        var hue: Float,
        var phase: Float,
        var points: Int = 100,
    )

    override fun onInitialize() {
        val phi = (1 + sqrt(5.0)) / 2 // Golden ratio

        // Create golden spirals
        repeat(3) { i ->
            spirals.add(GoldenSpiral(
                centerX = bounds.centerX() + (i - 1) * 300,
                centerY = bounds.centerY(),
                rotation = (i * 2 * PI / 3).toFloat(),
                scale = (80 + i * 40).toFloat(),
                hue = (30 + i * 20).toFloat(), // Golden tones
                phase = 0f,
            ))
        }

        // Initial particles
        repeat(50) { spawnParticle() }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f
        val audioMid = audioFrame?.mid ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain
        val midInfluence = audioMid * reactivity.midGain

        // Spawn particles
        if (midInfluence > 0.2 && Math.random() < 0.1) {
            repeat((midInfluence * 3).toInt()) { spawnParticle() }
        }

        // Update spirals
        for (spiral in spirals) {
            spiral.phase += 0.01f * project.templateTuning.speed
            spiral.rotation += 0.002f * project.templateTuning.speed
            drawSpiral(canvas, spiral, bassInfluence)
        }

        // Update particles
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        for (particle in particles) {
            updateParticle(particle, time, bassInfluence, centerX, centerY)
            drawParticle(canvas, particle)
        }

        particles.removeAll { it.life <= 0 }
        if (particles.size > 200) {
            particles.sortBy { it.life }
            particles.subList(0, particles.size - 200).clear()
        }
    }

    private fun spawnParticle() {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val phi = (1 + sqrt(5.0)) / 2

        // Spawn on golden spiral
        val t = (Math.random() * 4 * PI).toFloat()
        val r = 50f * pow(phi, t / (2 * PI)).toFloat()

        val angle = t + (Math.random() - 0.5).toFloat() * 0.5f
        val x = centerX + cos(angle) * r
        val y = centerY + sin(angle) * r

        particles.add(ElysiumParticle(
            x = x,
            y = y,
            z = (Math.random() * 500).toFloat(),
            vx = (Math.random() - 0.5).toFloat() * 10,
            vy = (Math.random() - 0.5).toFloat() * 10,
            size = (Math.random() * 8 + 2).toFloat(),
            hue = (Math.random() * 40 + 20).toFloat(), // Gold-orange
            brightness = (Math.random() * 0.3 + 0.7).toFloat(),
            life = (Math.random() * 6 + 4).toFloat(),
            maxLife = (Math.random() * 8 + 6).toFloat(),
        ))
    }

    private fun updateParticle(
        particle: ElysiumParticle,
        time: Double,
        bass: Float,
        centerX: Float,
        centerY: Float,
    ) {
        val dt = 1.0 / 30.0

        // Drift outward slowly
        val dx = particle.x - centerX
        val dy = particle.y - centerY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 0) {
            particle.vx += dx / dist * 5 * dt
            particle.vy += dy / dist * 5 * dt
        }

        // Bass pushes outward
        particle.vx += (Math.random() - 0.5).toFloat() * bass * 20 * dt
        particle.vy += (Math.random() - 0.5).toFloat() * bass * 20 * dt

        particle.x += particle.vx * dt
        particle.y += particle.vy * dt

        // Fade
        particle.life -= dt
        particle.size *= 0.998f
    }

    private fun drawParticle(canvas: Canvas, particle: ElysiumParticle) {
        val alpha = (particle.life / particle.maxLife * particle.brightness * 255).toInt()
        val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(particle.hue, 0.6f, particle.brightness))
        paint.color = color

        canvas.drawCircle(particle.x, particle.y, particle.size, paint)

        // Glow
        val glowPaint = Paint(paint)
        glowPaint.color = android.graphics.Color.HSVToColor(alpha / 3, floatArrayOf(particle.hue, 0.3f, 1f))
        canvas.drawCircle(particle.x, particle.y, particle.size * 2, glowPaint)
    }

    private fun drawSpiral(canvas: Canvas, spiral: GoldenSpiral, bass: Float) {
        val path = Path()
        val phi = (1 + sqrt(5.0)) / 2
        val turns = 3
        val points = spiral.points

        for (i in 0..points) {
            val t = i * turns * 2 * PI / points + spiral.phase
            val r = spiral.scale * pow(phi, t / (2 * PI)).toFloat() * (1f + bass * 0.3f)

            val x = spiral.centerX + cos(t + spiral.rotation) * r
            val y = spiral.centerY + sin(t + spiral.rotation) * r

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        val color = android.graphics.Color.HSVToColor(100, floatArrayOf(spiral.hue, 0.8f, 0.9f))
        spiralPaint.color = color
        spiralPaint.strokeWidth = 1f + bass * 2f
        canvas.drawPath(path, spiralPaint)
    }

    override fun dispose() {
        particles.clear()
        spirals.clear()
    }

    private fun pow(base: Double, exp: Double): Double = Math.pow(base, exp)
}