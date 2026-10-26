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
 * Sonnet template - Elegant flowing text with decorative flourishes
 */
class SonnetRenderer : Canvas2DRenderer(Template.SONNET) {

    private val flourishes = mutableListOf<Flourish>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

    data class Flourish(
        var x: Float,
        var y: Float,
        var angle: Float,
        var size: Float,
        var hue: Float,
        var phase: Float,
        var life: Float,
        var maxLife: Float,
    )

    override fun onInitialize() {
        repeat(8) { spawnFlourish() }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioMid = audioFrame?.mid ?: 0f
        val audioVocal = audioFrame?.vocal ?: 0f

        val reactivity = project.audioReactivityConfig
        val midInfluence = audioMid * reactivity.midGain
        val vocalInfluence = audioVocal * reactivity.vocalGain

        // Spawn flourishes on vocal/mid
        if ((midInfluence + vocalInfluence) > 0.4 && Math.random() < 0.1) {
            spawnFlourish()
        }

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        for (flourish in flourishes) {
            flourish.phase += 0.02f * project.templateTuning.speed
            flourish.life -= 1f / 30f

            drawFlourish(canvas, flourish, centerX, centerY)
        }

        // Clean up
        flourishes.removeAll { it.life <= 0 }
        if (flourishes.size > 30) {
            flourishes.sortBy { it.life }
            flourishes.subList(0, flourishes.size - 30).clear()
        }
    }

    private fun spawnFlourish() {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val radius = min(width, height) * 0.3f

        val angle = (Math.random() * 2 * PI).toFloat()
        val dist = (Math.random() * radius * 0.5).toFloat()

        flourishes.add(Flourish(
            x = centerX + cos(angle) * dist,
            y = centerY + sin(angle) * dist,
            angle = angle,
            size = (Math.random() * 60 + 40).toFloat(),
            hue = (Math.random() * 60 + 300).toFloat(), // Magenta-pink
            phase = (Math.random() * 2 * PI).toFloat(),
            life = (Math.random() * 4 + 3).toFloat(),
            maxLife = (Math.random() * 5 + 4).toFloat(),
        ))
    }

    private fun drawFlourish(canvas: Canvas, flourish: Flourish, centerX: Float, centerY: Float) {
        val progress = 1f - flourish.life / flourish.maxLife
        val alpha = (flourish.life / flourish.maxLife * 255).toInt()

        val path = Path()
        val petals = 6

        for (p in 0 until petals) {
            val petalAngle = flourish.angle + p * 2f * PI.toFloat() / petals + flourish.phase
            val r = flourish.size * (1f - progress * 0.5f) * (0.5f + 0.5f * sin(flourish.phase * 2 + p))

            val px = flourish.x + cos(petalAngle) * r
            val py = flourish.y + sin(petalAngle) * r

            if (p == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()

        val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(flourish.hue, 0.7f, 0.9f))
        val fillColor = android.graphics.Color.HSVToColor(alpha / 3, floatArrayOf(flourish.hue, 0.5f, 0.8f))

        paint.color = color
        paint.strokeWidth = 1.5f
        canvas.drawPath(path, paint)

        fillPaint.color = fillColor
        canvas.drawPath(path, fillPaint)
    }

    override fun dispose() {
        flourishes.clear()
    }
}