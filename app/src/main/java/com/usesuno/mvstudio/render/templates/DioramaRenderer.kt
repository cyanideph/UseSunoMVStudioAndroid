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
 * Diorama renderer - 3D scene with depth layers (simulated 3D with 2D canvas)
 * Full 3D would use Filament/SceneView, this is a 2.5D approximation
 */
class DioramaRenderer : Canvas2DRenderer(Template.DIORAMA) {

    private val layers = mutableListOf<DioramaLayer>()
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private var rotationY = 0.0
    private var cameraZ = -500.0

    data class DioramaLayer(
        var z: Float, // Depth (negative = behind camera)
        var elements: MutableList<LayerElement> = mutableListOf(),
        var parallaxFactor: Float = 1f,
    ) {
        data class LayerElement(
            var x: Float,
            var y: Float,
            var width: Float,
            var height: Float,
            var hue: Float,
            var shapeType: Int, // 0=rect, 1=circle, 2=triangle
        )
    }

    override fun onInitialize() {
        // Background layer (far)
        val bgLayer = DioramaLayer(z = -800f, parallaxFactor = 0.2f)
        repeat(20) { i ->
            bgLayer.elements.add(DioramaLayer.LayerElement(
                x = (Math.random() * width * 2 - width).toFloat(),
                y = (Math.random() * height * 2 - height).toFloat(),
                width = (Math.random() * 200 + 50).toFloat(),
                height = (Math.random() * 200 + 50).toFloat(),
                hue = (Math.random() * 60 + 220).toFloat(), // Cool blues
                shapeType = (Math.random() * 3).toInt(),
            ))
        }
        layers.add(bgLayer)

        // Mid layer
        val midLayer = DioramaLayer(z = -300f, parallaxFactor = 0.6f)
        repeat(15) { i ->
            midLayer.elements.add(DioramaLayer.LayerElement(
                x = (Math.random() * width * 1.5f - width * 0.75f).toFloat(),
                y = (Math.random() * height * 1.5f - height * 0.75f).toFloat(),
                width = (Math.random() * 150 + 80).toFloat(),
                height = (Math.random() * 150 + 80).toFloat(),
                hue = (Math.random() * 60 + 160).toFloat(), // Teal-green
                shapeType = (Math.random() * 3).toInt(),
            ))
        }
        layers.add(midLayer)

        // Foreground layer
        val fgLayer = DioramaLayer(z = -50f, parallaxFactor = 1.2f)
        repeat(10) { i ->
            fgLayer.elements.add(DioramaLayer.LayerElement(
                x = (Math.random() * width - width / 2).toFloat(),
                y = (Math.random() * height - height / 2).toFloat(),
                width = (Math.random() * 100 + 60).toFloat(),
                height = (Math.random() * 100 + 60).toFloat(),
                hue = (Math.random() * 60 + 30).toFloat(), // Warm
                shapeType = (Math.random() * 3).toInt(),
            ))
        }
        layers.add(fgLayer)
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioBass = audioFrame?.bass ?: 0f
        val audioPower = audioFrame?.power ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        rotationY += (0.0005 + bassInfluence * 0.005) * project.templateTuning.speed
        cameraZ += (audioPower - 0.5) * 10 * project.templateTuning.speed

        // Sort layers by depth (far to near)
        layers.sortBy { it.z }

        for (layer in layers) {
            drawLayer(canvas, layer, rotationY, cameraZ)
        }
    }

    private fun drawLayer(canvas: Canvas, layer: DioramaLayer, rotationY: Double, cameraZ: Double) {
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val focalLength = 1000f

        val cosRY = cos(rotationY).toFloat()
        val sinRY = sin(rotationY).toFloat()

        for (element in layer.elements) {
            // Apply rotation
            val x1 = element.x * cosRY
            val z1 = layer.z + element.x * sinRY

            // Apply camera
            val z2 = z1 - cameraZ.toFloat()

            if (z2 <= 0) continue // Behind camera

            // Perspective projection
            val scale = focalLength / (focalLength + z2) * layer.parallaxFactor

            val screenX = centerX + x1 * scale
            val screenY = centerY + element.y * scale
            val screenW = element.width * scale
            val screenH = element.height * scale

            // Alpha based on depth
            val depthAlpha = (1f - z2 / 1000f).coerceIn(0.2f, 1f)
            val alpha = (depthAlpha * 200).toInt()

            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(element.hue, 0.6f, 0.7f))
            paint.color = color

            when (element.shapeType) {
                0 -> canvas.drawRect(screenX - screenW/2, screenY - screenH/2, screenX + screenW/2, screenY + screenH/2, paint)
                1 -> canvas.drawCircle(screenX, screenY, min(screenW, screenH)/2, paint)
                2 -> drawTriangle(canvas, screenX, screenY, screenW, paint)
            }
        }
    }

    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
        val h = size * 0.866f // Equilateral triangle height
        path.moveTo(cx, cy - h * 2/3)
        path.lineTo(cx - size/2, cy + h/3)
        path.lineTo(cx + size/2, cy + h/3)
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun renderLyrics(canvas: Canvas, time: Double) {
        // Override to render lyrics at specific depth
        val lyricsLayerZ = -150f
        val focalLength = 1000f
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()

        val visibleLines = project.lines.filter { it.start <= time && it.end >= time }
        val lineHeight = textPaint.textSize * project.lineHeight * 1.5f

        for ((index, line) in visibleLines.withIndex()) {
            val z2 = lyricsLayerZ - cameraZ.toFloat()
            if (z2 <= 0) continue

            val scale = focalLength / (focalLength + z2)
            val screenY = centerY + (index - visibleLines.size / 2f) * lineHeight * scale

            canvas.save()
            canvas.translate(centerX, screenY)
            canvas.scale(scale, scale)

            textPaint.alpha = (255 * introProgress).toInt()
            canvas.drawText(line.text, 0f, 0f, textPaint)

            canvas.restore()
        }
    }

    override fun dispose() {
        layers.clear()
    }
}