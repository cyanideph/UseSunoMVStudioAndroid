package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer

/**
 * Astral template - 3D starfield with lyrics floating in space
 * Uses simple 3D projection (no external 3D engine)
 */
class AstralRenderer : Canvas2DRenderer(Template.ASTRAL) {

    private val stars = mutableListOf<Star>()
    private val starPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val linePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1f }
    private var rotationY = 0.0
    private var rotationX = 0.0

    data class Star(
        var x: Float,
        var y: Float,
        var z: Float,
        var size: Float,
        var hue: Float,
        var brightness: Float,
    ) {
        var screenX = 0f
        var screenY = 0f
        var screenSize = 0f
    }

    override fun onInitialize() {
        val numStars = 300
        val depth = 2000f

        repeat(numStars) {
            val theta = (Math.random() * 2 * PI).toFloat()
            val phi = (Math.random() * PI - PI / 2).toFloat()
            val r = (Math.random() * depth).toFloat()

            stars.add(Star(
                x = r * cos(phi) * cos(theta),
                y = r * sin(phi),
                z = r * cos(phi) * sin(theta),
                size = (Math.random() * 2 + 0.5).toFloat(),
                hue = (Math.random() * 60 + 200).toFloat(), // Blue-white
                brightness = (Math.random() * 0.5 + 0.5).toFloat(),
            ))
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f
        val audioTreble = audioFrame?.treble ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain
        val trebleInfluence = audioTreble * reactivity.trebleGain

        // Rotate starfield
        rotationY += (0.001 + bassInfluence * 0.01) * project.templateTuning.speed
        rotationX += (0.0005 + audioPower * 0.005) * project.templateTuning.speed

        val cosRY = cos(rotationY).toFloat()
        val sinRY = sin(rotationY).toFloat()
        val cosRX = cos(rotationX).toFloat()
        val sinRX = sin(rotationX).toFloat()

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val focalLength = 1000f

        // Project and sort stars by depth
        for (star in stars) {
            // Rotate Y
            val x1 = star.x * cosRY - star.z * sinRY
            val z1 = star.x * sinRY + star.z * cosRY

            // Rotate X
            val y1 = star.y * cosRX - z1 * sinRX
            val z2 = star.y * sinRX + z1 * cosRX

            // Perspective projection
            val scale = focalLength / (focalLength + z2)
            star.screenX = centerX + x1 * scale
            star.screenY = centerY + y1 * scale
            star.screenSize = star.size * scale * (1f + audioPower * 2f)
        }

        // Sort by depth (far to near)
        stars.sortByDescending { it.z }

        // Draw stars
        for (star in stars) {
            if (star.screenX < -50 || star.screenX > width + 50 ||
                star.screenY < -50 || star.screenY > height + 50) continue

            val alpha = (star.brightness * (1f + audioPower) * 255).toInt().coerceIn(0, 255)
            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(star.hue, 0.2f, 1f))
            starPaint.color = color

            canvas.drawCircle(star.screenX, star.screenY, star.screenSize, starPaint)

            // Draw connections to nearby stars (constellation lines)
            if (star.z < 500 && Math.random() < 0.001) {
                linePaint.color = android.graphics.Color.HSVToColor(30, floatArrayOf(star.hue, 0.1f, 1f))
                // Would connect to nearby stars
            }
        }

        // Draw lyrics floating in space
        drawSpaceLyrics(canvas, centerX, centerY, focalLength, cosRY, sinRY, cosRX, sinRX)
    }

    private fun drawSpaceLyrics(
        canvas: Canvas,
        centerX: Float, centerY: Float,
        focalLength: Float,
        cosRY: Float, sinRY: Float,
        cosRX: Float, sinRX: Float,
    ) {
        val visibleLines = project.lines.filter { it.start <= animationTime && it.end >= animationTime }
        val lineHeight = textPaint.textSize * project.lineHeight * 1.5f

        for ((index, line) in visibleLines.withIndex()) {
            // Position in 3D space
            val zPos = -200f + index * 100f
            val yPos = (index - visibleLines.size / 2f) * lineHeight

            // Rotate
            val x1 = 0f * cosRY - zPos * sinRY
            val z1 = 0f * sinRY + zPos * cosRY
            val y1 = yPos * cosRX - z1 * sinRX
            val z2 = yPos * sinRX + z1 * cosRX

            // Project
            val scale = focalLength / (focalLength + z2)
            val screenX = centerX + x1 * scale
            val screenY = centerY + y1 * scale
            val textScale = scale * (0.8f + 0.2f * introProgress)

            canvas.save()
            canvas.translate(screenX, screenY)
            canvas.scale(textScale, textScale)

            textPaint.alpha = (255 * introProgress * (0.5f + 0.5f * scale)).toInt()
            canvas.drawText(line.text, 0f, 0f, textPaint)

            canvas.restore()
        }
    }

    override fun dispose() {
        stars.clear()
    }
}