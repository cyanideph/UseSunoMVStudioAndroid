package com.usesuno.mvstudio.render.templates

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.engine.Canvas2DRenderer
import com.usesuno.mvstudio.render.engine.FoliaLyricStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Classic template - Folia's flagship pristine typography with punchy audio-reactive dynamics
 */
class ClassicRenderer : Canvas2DRenderer(Template.CLASSIC) {

    private val accentPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    override val foliaStyle = object : FoliaLyricStyle {
        override fun drawLineAccents(
            canvas: Canvas,
            line: LyricLine,
            centerX: Float,
            lineY: Float,
            lineWidth: Float,
            isActive: Boolean,
            distFromCenter: Float,
            lineAlpha: Float,
            audioFrame: AudioFrame?,
        ) {
            if (isActive && lineAlpha > 0.4f) {
                // Elegant subtle audio-reactive baseline accent bar
                val bass = audioFrame?.bass ?: 0f
                val barWidth = (lineWidth * 0.4f + bass * 80f) * project.templateTuning.intensity
                accentPaint.color = Color.WHITE
                accentPaint.alpha = (80 * lineAlpha * (1f + bass * 0.5f)).toInt().coerceIn(0, 255)
                val barY = lineY + textPaint.textSize * 0.35f
                canvas.drawLine(centerX - barWidth / 2f, barY, centerX + barWidth / 2f, barY, accentPaint)
            }
        }
    }

    override fun renderTemplate(canvas: Canvas, time: Double, audioFrame: AudioFrame?) {
        // Classic relies on pure Folia typography with audio bass bounce
    }
}