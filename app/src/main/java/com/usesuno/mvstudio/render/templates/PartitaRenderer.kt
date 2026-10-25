package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/**
 * Partita template - musical notation inspired visualization
 */
class PartitaRenderer : Canvas2DRenderer(Template.PARTITA) {

    private val notes = mutableListOf<Note>()
    private val staffLines = 5
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val staffPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1f }

    data class Note(
        var x: Float,
        var y: Float,
        var targetY: Float,
        var size: Float,
        var hue: Float,
        var velocity: Float,
        var life: Float,
    )

    override fun onInitialize() {
        // Initialize staff positions
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f
        val audioMid = audioFrame?.mid ?: 0f

        val reactivity = project.audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain
        val midInfluence = audioMid * reactivity.midGain

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val staffSpacing = 20f
        val staffTop = centerY - (staffLines - 1) * staffSpacing / 2

        // Draw staff lines
        staffPaint.color = android.graphics.Color.parseColor("#FFFFFF40")
        for (i in 0 until staffLines) {
            val y = staffTop + i * staffSpacing
            canvas.drawLine(0f, y, width.toFloat(), y, staffPaint)
        }

        // Spawn notes on bass hits
        if (bassInfluence > 0.4 && Math.random() < 0.3) {
            spawnNote(centerX, staffTop, staffSpacing, bassInfluence)
        }

        // Update and draw notes
        for (note in notes) {
            note.x -= 100f * (1f / 30f) * project.templateTuning.speed // Move left
            note.y += (note.targetY - note.y) * 0.1f
            note.life -= 1f / 30f
            note.velocity *= 0.98f

            val alpha = (min(1f, note.life) * 255).toInt()
            val color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(note.hue, 0.8f, 1f))
            paint.color = color

            // Draw note head
            canvas.drawCircle(note.x, note.y, note.size, paint)

            // Draw stem
            val stemPaint = Paint().apply {
                color = paint.color
                strokeWidth = 2f
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            val stemHeight = -40f
            canvas.drawLine(note.x + note.size, note.y, note.x + note.size, note.y + stemHeight, stemPaint)

            // Draw flag for eighth notes
            if (note.size < 10) {
                val flagPath = android.graphics.Path()
                flagPath.moveTo(note.x + note.size, note.y + stemHeight)
                flagPath.quadTo(note.x + note.size + 15, note.y + stemHeight - 10, note.x + note.size + 20, note.y + stemHeight - 5)
                canvas.drawPath(flagPath, stemPaint)
            }
        }

        // Clean up off-screen notes
        notes.removeAll { it.x < -50 || it.life <= 0 }
        if (notes.size > 100) {
            notes.sortBy { it.life }
            notes.subList(0, notes.size - 100).clear()
        }
    }

    private fun spawnNote(centerX: Float, staffTop: Float, staffSpacing: Float, intensity: Float) {
        val line = (Math.random() * staffLines).toInt()
        val noteY = staffTop + line * staffSpacing + (Math.random() - 0.5).toFloat() * staffSpacing * 0.5f

        notes.add(Note(
            x = width + 50f,
            y = noteY,
            targetY = noteY,
            size = (Math.random() * 8 + 6).toFloat(),
            hue = (Math.random() * 60 + 30).toFloat(), // Orange-gold
            velocity = intensity * 200,
            life = 5f,
        ))
    }

    override fun dispose() {
        notes.clear()
    }
}