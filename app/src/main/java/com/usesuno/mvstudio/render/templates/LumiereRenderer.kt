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
 * Lumiere template - Cinematic light rays and lens flares
 */
class LumiereRenderer : Canvas2DRenderer(Template.LUMIERE) {

    private val lightRays = mutableListOf<LightRay>()
    private val flares = mutableListOf<LensFlare>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var timeOffset = 0.0

    data class LightRay(
        var angle: Float,
        var startRadius: Float,
        var endRadius: Float,
        var hue: Float,
        var intensity: Float,
        var rotationSpeed: Float,
    )

    data class LensFlare(
        var x: Float,
        var y: Float,
        var baseSize: Float,
        var hue: Float,
        var elements: List<FlareElement>,
    ) {
        data class FlareElement(
            var offset: Float, // Along ray
            var size: Float,
            var hue: Float,
            var shape: Int, // 0=circle, 1=hexagon, 2=ring
        )
    }

    override fun onInitialize() {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        // Create light rays
        repeat(12) { i ->
            lightRays.add(LightRay(
                angle = i * 2 * PI / 12f,
                startRadius = 50f,
                endRadius = max(width, height).toFloat() * 0.8f,
                hue = (i * 30f).toFloat(),
                intensity = (Math.random() * 0.5 + 0.5).toFloat(),
                rotationSpeed = (Math.random() * 0.001 + 0.0005).toFloat(),
            ))
        }

        // Create lens flares
        repeat(3) { i ->
            val elements = mutableListOf<LensFlare.FlareElement>()
            for (j in 0..5) {
                elements.add(LensFlare.FlareElement(
                    offset = j * 0.15f,
                    size = (Math.random() * 30 + 10).toFloat(),
                    hue = (Math.random() * 60 + 30).toFloat(),
                    shape = (Math.random() * 3).toInt(),
                ))
            }
            flares.add(LensFlare(
                x = centerX + (i - 1) * 300,
                y = centerY,
                baseSize = (Math.random() * 50 + 30).toFloat(),
                hue = (Math.random() * 60 + 30).toFloat(),
                elements = elements,
            ))
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f
        val audioTreble = audioFrame?.treble ?: 0f

        val reactivity = project.audioReactivityConfig
        val trebleInfluence = audioTreble * reactivity.trebleGain
        val powerInfluence = audioPower * reactivity.threshold

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        // Draw light rays
        for (ray in lightRays) {
            ray.angle += ray.rotationSpeed * project.templateTuning.speed
            ray.intensity = max(0.3f, ray.intensity + (powerInfluence - ray.intensity) * 0.1f)

            drawLightRay(canvas, ray, centerX, centerY, trebleInfluence)
        }

        // Draw lens flares
        for (flare in flares) {
            drawLensFlare(canvas, flare, centerX, centerY, trebleInfluence)
        }

        // Draw lyrics with cinematic lighting
        drawCinematicLyrics(canvas, centerX, centerY)
    }

    private fun drawLightRay(canvas: Canvas, ray: LightRay, cx: Float, cy: Float, treble: Float) {
        val path = Path()
        val innerRadius = ray.startRadius
        val outerRadius = ray.endRadius * (1f + treble * 0.5f)

        val cosA = cos(ray.angle)
        val sinA = sin(ray.angle)
        val cosA90 = cos(ray.angle + PI / 2)
        val sinA90 = sin(ray.angle + PI / 2)

        val width = 30f * ray.intensity * (1f + treble)

        path.moveTo(cx + cosA * innerRadius - cosA90 * width / 2, cy + sinA * innerRadius - sinA90 * width / 2)
        path.lineTo(cx + cosA * outerRadius - cosA90 * width / 2, cy + sinA * outerRadius - sinA90 * width / 2)
        path.lineTo(cx + cosA * outerRadius + cosA90 * width / 2, cy + sinA * outerRadius + sinA90 * width / 2)
        path.lineTo(cx + cosA * innerRadius + cosA90 * width / 2, cy + sinA * innerRadius + sinA90 * width / 2)
        path.close()

        val alpha = (ray.intensity * 100).toInt()
        val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(ray.hue, 0.8f, 1f))
        paint.color = color
        canvas.drawPath(path, paint)
    }

    private fun drawLensFlare(canvas: Canvas, flare: LensFlare, cx: Float, cy: Float, treble: Float) {
        // Animate flare position
        val flareX = flare.x + sin(timeOffset * 0.5 + flare.hue / 360f * 2 * PI) * 20 * treble
        val flareY = flare.y + cos(timeOffset * 0.3 + flare.hue / 360f * 2 * PI) * 15 * treble

        for (element in flare.elements) {
            val ex = flareX + cos(timeOffset + element.offset * 2 * PI) * element.size * 2
            val ey = flareY + sin(timeOffset + element.offset * 2 * PI) * element.size

            val alpha = (100 * (1f + treble)).toInt()
            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(element.hue, 0.6f, 1f))
            paint.color = color

            when (element.shape) {
                0 -> canvas.drawCircle(ex, ey, element.size, paint) // Circle
                1 -> drawHexagon(canvas, ex, ey, element.size, paint) // Hexagon
                2 -> drawRing(canvas, ex, ey, element.size, paint) // Ring
            }
        }
    }

    private fun drawHexagon(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val path = Path()
        for (i in 0..5) {
            val angle = i * 2 * PI / 6f
            val x = cx + cos(angle) * radius
            val y = cy + sin(angle) * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val ringPaint = Paint(paint)
        ringPaint.style = Paint.Style.STROKE
        ringPaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, radius, ringPaint)
    }

    private fun drawCinematicLyrics(canvas: Canvas, centerX: Float, centerY: Float) {
        val visibleLines = project.lines.filter { it.start <= animationTime && it.end >= animationTime }
        val lineHeight = textPaint.textSize * project.lineHeight * 1.2f

        for ((index, line) in visibleLines.withIndex()) {
            val y = centerY + (index - visibleLines.size / 2f) * lineHeight

            // Add vignette-aware alpha
            val vignette = 1f - abs(index - visibleLines.size / 2f) / (visibleLines.size / 2f + 1) * 0.3f
            textPaint.alpha = (255 * introProgress * vignette).toInt()

            canvas.drawText(line.text, centerX, y, textPaint)
        }
    }

    override fun dispose() {
        lightRays.clear()
        flares.clear()
    }
}