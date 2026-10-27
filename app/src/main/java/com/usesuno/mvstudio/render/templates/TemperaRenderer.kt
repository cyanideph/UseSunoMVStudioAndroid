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
 * Tempera template - Paint texture with color blending
 */
class TemperaRenderer : Canvas2DRenderer(Template.TEMPERA) {

    private val paintDrops = mutableListOf<PaintDrop>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var timeOffset = 0.0

    override val foliaStyle = object : com.usesuno.mvstudio.render.engine.FoliaLyricStyle {
        override fun drawWord(
            canvas: Canvas,
            word: LyricWord,
            wordX: Float,
            baselineY: Float,
            wordWidth: Float,
            isActive: Boolean,
            isSpoken: Boolean,
            wordProgress: Float,
            lineAlpha: Float,
            textPaint: Paint,
            audioFrame: AudioFrame?,
        ): Boolean {
            if (isActive) {
                // Soft watercolor pigment bloom around active word
                val bloomPaint = Paint(textPaint).apply {
                    alpha = (75 * lineAlpha).toInt().coerceIn(0, 255)
                    setShadowLayer(18f, 0f, 0f, 0xFFFF8844.toInt())
                }
                canvas.drawText(word.text, wordX, baselineY, bloomPaint)
            }
            return false
        }
    }

    data class PaintDrop(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var hue: Float,
        var saturation: Float,
        var alpha: Float,
        var life: Float,
        var maxLife: Float,
        var isWet: Boolean = true,
    )

    override fun onInitialize() {
        repeat(20) { spawnDrop() }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f
        val audioMid = audioFrame?.mid ?: 0f
        val audioTreble = audioFrame?.treble ?: 0f

        val reactivity = project.audioReactivityConfig
        val midInfluence = audioMid * reactivity.midGain
        val trebleInfluence = audioTreble * reactivity.trebleGain

        // Spawn drops on transients
        if (trebleInfluence > 0.5 && Math.random() < 0.15) {
            repeat((trebleInfluence * 3).toInt()) { spawnDrop() }
        }

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        for (drop in paintDrops) {
            updateDrop(drop, time, midInfluence, centerX, centerY)
            drawDrop(canvas, drop)
        }

        // Blend overlapping drops
        blendDrops(canvas)

        // Clean up
        paintDrops.removeAll { it.life <= 0 }
        if (paintDrops.size > 100) {
            paintDrops.sortBy { it.life }
            paintDrops.subList(0, paintDrops.size - 100).clear()
        }
    }

    private fun spawnDrop() {
        paintDrops.add(PaintDrop(
            x = (Math.random() * width).toFloat(),
            y = (Math.random() * height).toFloat(),
            vx = (Math.random() - 0.5).toFloat() * 20,
            vy = (Math.random() - 0.5).toFloat() * 20,
            radius = (Math.random() * 80 + 40).toFloat(),
            hue = (Math.random() * 360).toFloat(),
            saturation = (Math.random() * 0.5 + 0.5).toFloat(),
            alpha = 1f,
            life = (Math.random() * 5 + 3).toFloat(),
            maxLife = (Math.random() * 6 + 4).toFloat(),
        ))
    }

    private fun updateDrop(
        drop: PaintDrop,
        time: Double,
        midInfluence: Float,
        centerX: Float,
        centerY: Float,
    ) {
        val dt = 1.0f / 30.0f

        // Drift
        drop.x += drop.vx * dt
        drop.y += drop.vy * dt

        // Slow down
        drop.vx *= 0.98f
        drop.vy *= 0.98f

        // Mid frequencies expand drops
        drop.radius += (midInfluence * 100 - drop.radius) * 0.02f

        // Fade and dry
        drop.life -= dt
        drop.alpha = drop.life / drop.maxLife

        if (drop.life < drop.maxLife * 0.7) {
            drop.isWet = false
        }
    }

    private fun drawDrop(canvas: Canvas, drop: PaintDrop) {
        val color = android.graphics.Color.HSVToColor(
            (drop.alpha * 200).toInt(),
            floatArrayOf(drop.hue, drop.saturation, 0.9f)
        )
        paint.color = color

        canvas.drawCircle(drop.x, drop.y, drop.radius, paint)

        // Wet edge highlight
        if (drop.isWet) {
            val edgePaint = Paint(paint)
            edgePaint.style = Paint.Style.STROKE
            edgePaint.strokeWidth = 3f
            edgePaint.color = android.graphics.Color.HSVToColor(
                (drop.alpha * 150).toInt(),
                floatArrayOf(drop.hue, drop.saturation * 0.5f, 1f)
            )
            canvas.drawCircle(drop.x, drop.y, drop.radius, edgePaint)
        }
    }

    private fun blendDrops(canvas: Canvas) {
        // Simple blend - draw overlapping areas with mixed colors
        // Full implementation would use PorterDuff modes
    }

    override fun dispose() {
        paintDrops.clear()
    }
}