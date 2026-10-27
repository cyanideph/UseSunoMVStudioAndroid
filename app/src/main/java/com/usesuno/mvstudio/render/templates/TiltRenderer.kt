package com.usesuno.mvstudio.render.templates

import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import com.usesuno.mvstudio.render.engine.FoliaLyricStyle
import kotlin.math.PI

/**
 * Tilt template - Folia 3D tilted text with spatial perspective projection and reactive grid
 */
class TiltRenderer : Canvas2DRenderer(Template.TILT) {

    private var rotationX = 0.0
    private var rotationY = 0.0
    private val gridPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1f }
    private val camera = Camera()
    private val matrix = Matrix()

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
            canvas.save()

            // Real 3D camera projection with perspective foreshortening
            val pitch = (rotationX * 180 / PI).toFloat().coerceIn(-45f, 45f)
            val yaw = (rotationY * 180 / PI).toFloat().coerceIn(-45f, 45f)

            camera.save()
            camera.rotateX(pitch)
            camera.rotateY(yaw)
            camera.getMatrix(matrix)
            camera.restore()

            matrix.preTranslate(-lineX, -lineY)
            matrix.postScale(scale, scale, lineX, lineY)
            matrix.postTranslate(lineX, lineY)

            canvas.concat(matrix)
            drawContent(canvas)
            canvas.restore()
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        // Smooth spatial tilt oscillation influenced by audio
        val speed = project.templateTuning.speed.toDouble()
        rotationY = kotlin.math.sin(time * 0.8 * speed) * 0.35 + (bassInfluence * 0.15)
        rotationX = kotlin.math.cos(time * 0.6 * speed) * 0.20 + (audioPower * 0.10)

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        // Draw 3D perspective grid plane
        drawGrid(canvas, centerX, centerY)
    }

    private fun drawGrid(canvas: Canvas, centerX: Float, centerY: Float) {
        gridPaint.color = android.graphics.Color.parseColor("#FFFFFF15")

        val spacing = 70f
        val gridSize = 8

        canvas.save()
        camera.save()
        camera.rotateX((rotationX * 180 / PI).toFloat().coerceIn(-45f, 45f))
        camera.rotateY((rotationY * 180 / PI).toFloat().coerceIn(-45f, 45f))
        camera.getMatrix(matrix)
        camera.restore()

        matrix.preTranslate(-centerX, -centerY)
        matrix.postTranslate(centerX, centerY)
        canvas.concat(matrix)

        for (i in -gridSize..gridSize) {
            val offset = i * spacing
            canvas.drawLine(-gridSize * spacing, offset, gridSize * spacing, offset, gridPaint)
            canvas.drawLine(offset, -gridSize * spacing, offset, gridSize * spacing, gridPaint)
        }

        canvas.restore()
    }
}