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
 * Cappella template - Vocal harmony visualization with expanding rings
 */
class CappellaRenderer : Canvas2DRenderer(Template.CAPPELLA) {

    private val rings = mutableListOf<HarmonyRing>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var lastVocalLevel = 0f

    data class HarmonyRing(
        var radius: Float,
        var maxRadius: Float,
        var hue: Float,
        var thickness: Float,
        var life: Float,
        var vocalTrigger: Float,
    )

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioVocal = audioFrame?.vocal ?: 0f
        val audioPower = audioFrame?.power ?: 0f

        val reactivity = project.audioReactivityConfig
        val vocalInfluence = audioVocal * reactivity.vocalGain

        // Trigger new ring on vocal onset
        if (vocalInfluence > 0.4 && vocalInfluence > lastVocalLevel + 0.15) {
            spawnRing(vocalInfluence)
        }
        lastVocalLevel = vocalInfluence

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        for (ring in rings) {
            ring.radius += 30f * (1f / 30f) * project.templateTuning.speed
            ring.life -= 1f / 30f

            val progress = ring.radius / ring.maxRadius
            val alpha = ((1f - progress) * 255).toInt()
            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(ring.hue, 0.6f, 1f))
            val fillColor = android.graphics.Color.HSVToColor(alpha / 4, floatArrayOf(ring.hue, 0.3f, 0.8f))

            paint.color = color
            paint.strokeWidth = ring.thickness * (1f - progress * 0.5f)
            canvas.drawCircle(centerX, centerY, ring.radius, paint)

            fillPaint.color = fillColor
            canvas.drawCircle(centerX, centerY, ring.radius * 0.9f, fillPaint)
        }

        // Clean up
        rings.removeAll { it.life <= 0 || it.radius >= it.maxRadius }
        if (rings.size > 50) {
            rings.sortBy { it.life }
            rings.subList(0, rings.size - 50).clear()
        }
    }

    private fun spawnRing(vocalLevel: Float) {
        rings.add(HarmonyRing(
            radius = 10f,
            maxRadius = min(width, height) * 0.6f,
            hue = (Math.random() * 60 + 280).toFloat(), // Purple-magenta
            thickness = (Math.random() * 3 + 1).toFloat(),
            life = 4f,
            vocalTrigger = vocalLevel,
        ))
    }

    override fun dispose() {
        rings.clear()
    }
}