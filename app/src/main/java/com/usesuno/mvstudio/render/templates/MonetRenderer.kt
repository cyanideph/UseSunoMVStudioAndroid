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
 * Monet template - Impressionist brush stroke particles
 */
class MonetRenderer : Canvas2DRenderer(Template.MONET) {

    private val strokes = mutableListOf<BrushStroke>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var timeOffset = 0.0

    data class BrushStroke(
        var x: Float,
        var y: Float,
        var angle: Float,
        var length: Float,
        var hue: Float,
        var saturation: Float,
        var brightness: Float,
        var life: Float,
        var maxLife: Float,
        var speed: Float,
        var thickness: Float,
    )

    override fun onInitialize() {
        // Create initial strokes
        repeat(30) { spawnStroke() }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f
        val audioMid = audioFrame?.mid ?: 0f

        val reactivity = project.audioReactivityConfig
        val midInfluence = audioMid * reactivity.midGain
        val powerInfluence = audioPower * reactivity.threshold

        // Spawn new strokes on mid frequencies
        if (midInfluence > 0.3 && Math.random() < 0.2) {
            repeat((midInfluence * 5).toInt()) { spawnStroke() }
        }

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        for (stroke in strokes) {
            updateStroke(stroke, time, midInfluence, centerX, centerY)
            drawStroke(canvas, stroke)
        }

        // Clean up
        strokes.removeAll { it.life <= 0 }
        if (strokes.size > 150) {
            strokes.sortBy { it.life }
            strokes.subList(0, strokes.size - 150).clear()
        }
    }

    private fun spawnStroke() {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val radius = min(width, height) * 0.4f

        val angle = (Math.random() * 2 * PI).toFloat()
        val dist = (Math.random() * radius).toFloat()

        strokes.add(BrushStroke(
            x = centerX + cos(angle) * dist,
            y = centerY + sin(angle) * dist,
            angle = (Math.random() * 2 * PI).toFloat(),
            length = (Math.random() * 100 + 30).toFloat(),
            hue = (Math.random() * 60 + 30).toFloat(), // Warm colors
            saturation = (Math.random() * 0.4 + 0.4).toFloat(),
            brightness = (Math.random() * 0.3 + 0.7).toFloat(),
            life = (Math.random() * 3 + 2).toFloat(),
            maxLife = (Math.random() * 4 + 3).toFloat(),
            speed = (Math.random() * 20 + 10).toFloat(),
            thickness = (Math.random() * 3 + 1).toFloat(),
        ))
    }

    private fun updateStroke(
        stroke: BrushStroke,
        time: Double,
        midInfluence: Float,
        centerX: Float,
        centerY: Float,
    ) {
        val dt = 1.0f / 30.0f

        // Drift with noise
        stroke.x += cos((stroke.angle + time * 0.5).toFloat()) * stroke.speed * dt
        stroke.y += sin((stroke.angle + time * 0.5).toFloat()) * stroke.speed * dt

        // Rotate slowly
        stroke.angle += 0.1f * dt

        // Audio influence
        stroke.length += (midInfluence * 50 - stroke.length) * 0.05f
        stroke.thickness += (midInfluence * 3 - stroke.thickness) * 0.05f

        // Fade
        stroke.life -= dt
    }

    private fun drawStroke(canvas: Canvas, stroke: BrushStroke) {
        val alpha = (stroke.life / stroke.maxLife * 255).toInt()
        val color = android.graphics.Color.HSVToColor(
            alpha,
            floatArrayOf(stroke.hue, stroke.saturation, stroke.brightness)
        )

        paint.color = color
        paint.strokeWidth = stroke.thickness

        val endX = stroke.x + cos(stroke.angle) * stroke.length
        val endY = stroke.y + sin(stroke.angle) * stroke.length

        // Draw with slight curve
        val path = Path()
        path.moveTo(stroke.x, stroke.y)
        val ctrlX = stroke.x + cos(stroke.angle + 0.5f) * stroke.length * 0.5f
        val ctrlY = stroke.y + sin(stroke.angle + 0.5f) * stroke.length * 0.5f
        path.quadTo(ctrlX, ctrlY, endX, endY)

        canvas.drawPath(path, paint)
    }

    override fun dispose() {
        strokes.clear()
    }
}