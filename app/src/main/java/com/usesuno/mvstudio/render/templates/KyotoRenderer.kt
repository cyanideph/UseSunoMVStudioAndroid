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
 * Kyoto template - Japanese ink wash (sumi-e) style with flowing brush strokes
 */
class KyotoRenderer : Canvas2DRenderer(Template.KYOTO) {

    private val brushStrokes = mutableListOf<InkStroke>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
    private var timeOffset = 0.0

    data class InkStroke(
        var points: MutableList<Point> = mutableListOf(),
        var hue: Float,
        var baseWidth: Float,
        var inkFlow: Float,
        var life: Float,
        var maxLife: Float,
    ) {
        data class Point(var x: Float, var y: Float, val pressure: Float)
    }

    override fun onInitialize() {
        repeat(5) { spawnStroke() }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f
        val audioLowMid = audioFrame?.lowMid ?: 0f
        val audioMid = audioFrame?.mid ?: 0f

        val reactivity = project.audioReactivityConfig
        val lowMidInfluence = audioLowMid * reactivity.lowMidGain
        val midInfluence = audioMid * reactivity.midGain

        // New strokes on low-mid
        if (lowMidInfluence > 0.3 && Math.random() < 0.1) {
            spawnStroke()
        }

        for (stroke in brushStrokes) {
            updateStroke(stroke, time, midInfluence)
            drawStroke(canvas, stroke)
        }

        brushStrokes.removeAll { it.life <= 0 }
        if (brushStrokes.size > 30) {
            brushStrokes.sortBy { it.life }
            brushStrokes.subList(0, brushStrokes.size - 30).clear()
        }
    }

    private fun spawnStroke() {
        val startX = (Math.random() * width * 0.2).toFloat()
        val startY = (Math.random() * height).toFloat()

        val points = mutableListOf<InkStroke.Point>()
        var x = startX
        var y = startY
        var angle = (Math.random() * PI - PI / 2).toFloat() // Mostly horizontal

        // Generate stroke path
        for (i in 0..20) {
            val pressure = (Math.random() * 0.5 + 0.5).toFloat()
            points.add(InkStroke.Point(x, y, pressure))

            x += cos(angle) * (10 + Math.random() * 20).toFloat()
            y += sin(angle) * (Math.random() - 0.5).toFloat() * 30
            angle += (Math.random() - 0.5).toFloat() * 0.3f
        }

        brushStrokes.add(InkStroke(
            points = points,
            hue = (Math.random() * 30 + 10).toFloat(), // Warm sepia
            baseWidth = (Math.random() * 3 + 1).toFloat(),
            inkFlow = (Math.random() * 0.5 + 0.5).toFloat(),
            life = (Math.random() * 8 + 5).toFloat(),
            maxLife = (Math.random() * 10 + 8).toFloat(),
        ))
    }

    private fun updateStroke(stroke: InkStroke, time: Double, midInfluence: Float) {
        val dt = 1f / 30f

        // Flow horizontally
        for (point in stroke.points) {
            point.x += 20f * dt * stroke.inkFlow * project.templateTuning.speed
        }

        // Remove off-screen points
        stroke.points.removeAll { it.x > width + 50 }

        // Mid frequencies affect ink flow
        stroke.inkFlow += (midInfluence * 2 - stroke.inkFlow) * 0.05f

        // Fade
        stroke.life -= dt
    }

    private fun drawStroke(canvas: Canvas, stroke: InkStroke) {
        if (stroke.points.size < 2) return

        val path = Path()
        val alpha = (stroke.life / stroke.maxLife * 200).toInt()
        val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(stroke.hue, 0.3f, 0.3f))
        paint.color = color

        var first = true
        for (i in 0 until stroke.points.size - 1) {
            val p1 = stroke.points[i]
            val p2 = stroke.points[i + 1]

            val midX = (p1.x + p2.x) / 2
            val midY = (p1.y + p2.y) / 2
            val width = stroke.baseWidth * p1.pressure * (stroke.life / stroke.maxLife)

            paint.strokeWidth = width

            if (first) {
                path.moveTo(p1.x, p1.y)
                first = false
            }
            path.quadTo(p1.x, p1.y, midX, midY)
        }

        canvas.drawPath(path, paint)

        // Add ink bleed effect
        val bleedPaint = Paint(paint)
        bleedPaint.style = Paint.Style.FILL
        bleedPaint.color = android.graphics.Color.HSVToColor(alpha / 4, floatArrayOf(stroke.hue, 0.2f, 0.2f))

        for (point in stroke.points) {
            canvas.drawCircle(point.x, point.y, stroke.baseWidth * point.pressure * 1.5f, bleedPaint)
        }
    }

    override fun dispose() {
        brushStrokes.clear()
    }
}