package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import com.usesuno.mvstudio.render.engine.FoliaLyricStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Astral template - 3D starfield space visualizer with perspective lyrics
 */
class AstralRenderer : Canvas2DRenderer(Template.ASTRAL) {

    private val stars = mutableListOf<Star>()
    private val starPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var rotationY = 0.0
    private var rotationX = 0.0
    private var cosRY = 1f
    private var sinRY = 0f
    private var cosRX = 1f
    private var sinRX = 0f

    data class Star(
        var x: Float,
        var y: Float,
        var z: Float,
        var size: Float,
        var hue: Float,
        var brightness: Float,
        var screenX: Float = 0f,
        var screenY: Float = 0f,
        var screenSize: Float = 0f,
    )

    override fun onInitialize() {
        stars.clear()
        val numStars = 200
        val maxDist = 2000f

        for (i in 0 until numStars) {
            stars.add(
                Star(
                    x = ((Math.random() - 0.5) * maxDist * 2).toFloat(),
                    y = ((Math.random() - 0.5) * maxDist * 2).toFloat(),
                    z = (Math.random() * maxDist).toFloat(),
                    size = (Math.random() * 2 + 1).toFloat(),
                    hue = (Math.random() * 60 + 200).toFloat(), // Blue to purple
                    brightness = (Math.random() * 0.5 + 0.5).toFloat(),
                )
            )
        }
    }

    override val foliaStyle = object : FoliaLyricStyle {
        override fun transformLine(
            canvas: Canvas,
            line: LyricLine,
            lineIndex: Int,
            distFromCenter: Float,
            lineX: Float,
            lineY: Float,
            scale: Float,
            alpha: Float,
            audioFrame: AudioFrame?,
            drawContent: (Canvas) -> Unit,
        ) {
            val zPos = -100f + distFromCenter * 150f
            val yPos = lineY - bounds.centerY().toFloat()

            val x1 = -zPos * sinRY
            val z1 = zPos * cosRY
            val y1 = yPos * cosRX - z1 * sinRX
            val z2 = yPos * sinRX + z1 * cosRX

            val focalLength = 1000f
            val projScale = (focalLength / (focalLength + z2)).coerceIn(0.5f, 1.8f)
            val screenX = bounds.centerX().toFloat() + x1 * projScale
            val screenY = bounds.centerY().toFloat() + y1 * projScale

            canvas.save()
            canvas.translate(screenX, screenY)
            canvas.scale(scale * projScale, scale * projScale)
            drawContent(canvas)
            canvas.restore()
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        val speed = project.templateTuning.speed
        rotationY += (0.002 + bassInfluence * 0.01) * speed
        rotationX += (0.001 + audioPower * 0.005) * speed

        cosRY = cos(rotationY).toFloat()
        sinRY = sin(rotationY).toFloat()
        cosRX = cos(rotationX).toFloat()
        sinRX = sin(rotationX).toFloat()

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val focalLength = 1000f

        // Update and project stars
        for (star in stars) {
            star.z -= (5f + bassInfluence * 20f) * speed
            if (star.z < 10f) {
                star.z = 2000f
                star.x = ((Math.random() - 0.5) * 4000).toFloat()
                star.y = ((Math.random() - 0.5) * 4000).toFloat()
            }

            val x1 = star.x * cosRY - star.z * sinRY
            val z1 = star.x * sinRY + star.z * cosRY
            val y1 = star.y * cosRX - z1 * sinRX
            val z2 = star.y * sinRX + z1 * cosRX

            val scale = focalLength / (focalLength + z2)
            star.screenX = centerX + x1 * scale
            star.screenY = centerY + y1 * scale
            star.screenSize = star.size * scale * (1f + audioPower * 1.5f)
        }

        // Draw stars
        for (star in stars) {
            if (star.screenX < -50 || star.screenX > width + 50 ||
                star.screenY < -50 || star.screenY > height + 50
            ) continue

            val alpha = (star.brightness * (1f + audioPower) * 255).toInt().coerceIn(0, 255)
            starPaint.color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(star.hue, 0.2f, 1f))
            canvas.drawCircle(star.screenX, star.screenY, star.screenSize, starPaint)
        }
    }

    override fun dispose() {
        super.dispose()
        stars.clear()
    }
}