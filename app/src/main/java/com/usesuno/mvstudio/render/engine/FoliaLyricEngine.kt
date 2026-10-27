package com.usesuno.mvstudio.render.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Pluggable style configuration for template-specific lyric animations in Folia
 */
interface FoliaLyricStyle {
    /**
     * Line-level transformation hook (e.g. 3D tilt, perspective, baseline waves)
     */
    fun transformLine(
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
        canvas.translate(lineX, lineY)
        canvas.scale(scale, scale)
        drawContent(canvas)
        canvas.restore()
    }

    /**
     * Word-level custom drawing hook (e.g. Chroma RGB split, Kyoto ink bleed, Lumiere flare).
     * Return true if custom rendered, false to allow default Folia word luminescence.
     */
    fun drawWord(
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
    ): Boolean = false

    /**
     * Decorative background or foreground accents around lines (e.g. ripples, ribbons, staves)
     */
    fun drawLineAccents(
        canvas: Canvas,
        line: LyricLine,
        centerX: Float,
        lineY: Float,
        lineWidth: Float,
        isActive: Boolean,
        distFromCenter: Float,
        lineAlpha: Float,
        audioFrame: AudioFrame?,
    ) {}

    companion object {
        val DEFAULT = object : FoliaLyricStyle {}
    }
}

/**
 * FoliaLyricEngine - Exact port of Folia (UseSuno MV Studio) lyric animation engine.
 *
 * Implements:
 * 1. Continuous C1-smooth multi-line gliding (past lines translate up and dim, active line centered, upcoming lines wait below)
 * 2. Word-level luminescence and smooth continuous opacity scaling (spoken ~0.90, active 1.00 + subtle scale/glow pulse, upcoming ~0.42)
 * 3. Audio-reactive bass and treble dynamics
 * 4. Zero crude bounding-box karaoke artifacts
 */
class FoliaLyricEngine {

    private val glowPaint = Paint().apply {
        isAntiAlias = true
    }

    private val wordPaint = Paint().apply {
        isAntiAlias = true
    }

    /**
     * Calculate continuous virtual index for smooth vertical scrolling across lines
     */
    fun calculateVirtualIndex(lines: List<LyricLine>, currentTime: Double): Float {
        if (lines.isEmpty()) return 0f
        if (lines.size == 1) return 0f

        val firstLine = lines.first()
        val lastLine = lines.last()

        // Before first line
        if (currentTime < firstLine.start) {
            val leadIn = min(2.0, firstLine.start)
            val diff = (firstLine.start - currentTime).toFloat()
            return if (diff > leadIn) -1.2f else -1.2f * (diff / leadIn.toFloat())
        }

        // After last line
        if (currentTime > lastLine.end) {
            val leadOut = 2.0
            val diff = (currentTime - lastLine.end).toFloat()
            val extra = (diff / leadOut.toFloat()).coerceIn(0f, 1.5f)
            return (lines.size - 1).toFloat() + extra
        }

        // Inside lines
        for (i in lines.indices) {
            val line = lines[i]
            if (currentTime >= line.start && currentTime <= line.end) {
                // Inside this line
                if (i < lines.size - 1) {
                    val lineDur = line.end - line.start
                    val transWindow = min(0.6, lineDur * 0.35)
                    val remaining = line.end - currentTime
                    if (remaining < transWindow && transWindow > 0.01) {
                        // Smoothly transition into next line
                        val t = (1.0 - (remaining / transWindow)).toFloat().coerceIn(0f, 1f)
                        val eased = t * t * (3f - 2f * t)
                        return i.toFloat() + eased * 0.5f
                    }
                }
                return i.toFloat()
            }

            // Between line i and line i+1
            if (i < lines.size - 1) {
                val nextLine = lines[i + 1]
                if (currentTime > line.end && currentTime < nextLine.start) {
                    val gap = nextLine.start - line.end
                    val t = if (gap > 0.01) {
                        ((currentTime - line.end) / gap).toFloat().coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    val eased = t * t * (3f - 2f * t)
                    return i.toFloat() + 0.5f + eased * 0.5f
                }
            }
        }

        return 0f
    }

    /**
     * Main rendering loop for lyrics
     */
    fun render(
        canvas: Canvas,
        project: Project,
        time: Double,
        audioFrame: AudioFrame?,
        bounds: Rect,
        textPaint: Paint,
        strokePaint: Paint,
        introProgress: Float,
        style: FoliaLyricStyle = FoliaLyricStyle.DEFAULT,
    ) {
        val lines = project.lines
        if (lines.isEmpty()) return

        val virtualIndex = calculateVirtualIndex(lines, time)
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val scale = min(bounds.width(), bounds.height()) / 1080.0f
        val lineHeight = textPaint.textSize * max(1.1f, project.lineHeight)

        // Find visible lines around virtualIndex (-2 to +2 range)
        val minIdx = max(0, (virtualIndex - 2.5f).toInt())
        val maxIdx = min(lines.size - 1, (virtualIndex + 2.5f).toInt() + 1)

        val reactivity = project.audioReactivityConfig
        val bassPulse = 1f + (audioFrame?.bass ?: 0f) * reactivity.bassGain * 0.04f

        for (i in minIdx..maxIdx) {
            val line = lines[i]
            val dist = i - virtualIndex
            val absDist = abs(dist)
            if (absDist > 2.6f) continue

            // Continuous line alpha
            var lineAlpha = when {
                absDist < 0.25f -> 1.0f
                dist < 0f -> (1.0f - (absDist - 0.25f) * 0.40f).coerceIn(0f, 1f) * 0.85f
                else -> (1.0f - (dist - 0.25f) * 0.42f).coerceIn(0f, 1f) * 0.75f
            }
            lineAlpha *= introProgress
            if (lineAlpha <= 0.01f) continue

            // Continuous line scale
            val baseScale = (1.0f - absDist * 0.07f).coerceIn(0.78f, 1.05f)
            val lineScale = if (absDist < 0.35f) baseScale * bassPulse else baseScale

            val lineY = centerY + dist * lineHeight
            val isActiveLine = absDist < 0.35f

            // Template line accents hook (e.g. ribbons, ripples, staff lines)
            val estimatedLineWidth = textPaint.measureText(line.text)
            style.drawLineAccents(
                canvas = canvas,
                line = line,
                centerX = centerX,
                lineY = lineY,
                lineWidth = estimatedLineWidth,
                isActive = isActiveLine,
                distFromCenter = dist,
                lineAlpha = lineAlpha,
                audioFrame = audioFrame,
            )

            // Draw line through style transformation (supports 3D rotation, perspective, etc.)
            style.transformLine(
                canvas = canvas,
                line = line,
                lineIndex = i,
                distFromCenter = dist,
                lineX = centerX,
                lineY = lineY,
                scale = lineScale,
                alpha = lineAlpha,
                audioFrame = audioFrame,
            ) { lineCanvas ->
                if (line.words.isNotEmpty()) {
                    renderWordLevelLine(
                        canvas = lineCanvas,
                        line = line,
                        time = time,
                        lineAlpha = lineAlpha,
                        textPaint = textPaint,
                        audioFrame = audioFrame,
                        style = style,
                    )
                } else {
                    renderLineLevelLine(
                        canvas = lineCanvas,
                        line = line,
                        time = time,
                        isActive = isActiveLine,
                        lineAlpha = lineAlpha,
                        textPaint = textPaint,
                        audioFrame = audioFrame,
                    )
                }
            }
        }
    }

    /**
     * Folia Word-Level Synchronization & Luminescence:
     * - Spoken words: ~0.90 opacity, crisp typography
     * - Active word: 1.00 opacity + luminescence glow + subtle scale accent
     * - Upcoming words: ~0.42 opacity
     */
    private fun renderWordLevelLine(
        canvas: Canvas,
        line: LyricLine,
        time: Double,
        lineAlpha: Float,
        textPaint: Paint,
        audioFrame: AudioFrame?,
        style: FoliaLyricStyle,
    ) {
        val words = line.words
        val spaceWidth = textPaint.measureText(" ")
        val wordWidths = FloatArray(words.size) { textPaint.measureText(words[it].text) }
        var totalWidth = 0f
        for (w in wordWidths) totalWidth += w
        totalWidth += spaceWidth * max(0, words.size - 1)

        var curX = -totalWidth / 2f
        val baseColor = textPaint.color

        for (idx in words.indices) {
            val word = words[idx]
            val wWidth = wordWidths[idx]
            val isSpoken = time > word.end
            val isActive = time >= word.start && time <= word.end
            val wordProgress = if (isActive) {
                val dur = max(0.04, word.end - word.start)
                ((time - word.start) / dur).toFloat().coerceIn(0f, 1f)
            } else if (isSpoken) 1f else 0f

            // Check if style provides custom drawing for this word (e.g. Chroma RGB split)
            val handledByStyle = style.drawWord(
                canvas = canvas,
                word = word,
                wordX = curX,
                baselineY = 0f,
                wordWidth = wWidth,
                isActive = isActive,
                isSpoken = isSpoken,
                wordProgress = wordProgress,
                lineAlpha = lineAlpha,
                textPaint = textPaint,
                audioFrame = audioFrame,
            )

            if (!handledByStyle) {
                wordPaint.set(textPaint)

                when {
                    isActive -> {
                        // Active word: full luminescence, subtle scale bounce, glowing aura
                        val activeScale = 1.0f + 0.05f * sin(wordProgress * PI.toFloat()) + (audioFrame?.treble ?: 0f) * 0.02f
                        val activeAlpha = (255 * lineAlpha).toInt().coerceIn(0, 255)
                        wordPaint.alpha = activeAlpha

                        canvas.save()
                        val wordCenterX = curX + wWidth / 2f
                        canvas.translate(wordCenterX, 0f)
                        canvas.scale(activeScale, activeScale)
                        canvas.translate(-wordCenterX, 0f)

                        // Subtle luminescence bloom
                        glowPaint.set(wordPaint)
                        val glowAlpha = (160 * lineAlpha).toInt().coerceIn(0, 255)
                        glowPaint.alpha = glowAlpha
                        glowPaint.setShadowLayer(
                            18f + (audioFrame?.treble ?: 0f) * 12f,
                            0f,
                            0f,
                            Color.WHITE,
                        )
                        canvas.drawText(word.text, curX, 0f, glowPaint)

                        // Crisp foreground active word
                        wordPaint.clearShadowLayer()
                        canvas.drawText(word.text, curX, 0f, wordPaint)
                        canvas.restore()
                    }
                    isSpoken -> {
                        // Spoken words: warm, legible ~0.90 opacity
                        val spokenAlpha = (255 * 0.90f * lineAlpha).toInt().coerceIn(0, 255)
                        wordPaint.alpha = spokenAlpha
                        wordPaint.clearShadowLayer()
                        canvas.drawText(word.text, curX, 0f, wordPaint)
                    }
                    else -> {
                        // Upcoming words: subtle, non-distracting ~0.42 opacity
                        val upcomingAlpha = (255 * 0.42f * lineAlpha).toInt().coerceIn(0, 255)
                        wordPaint.alpha = upcomingAlpha
                        wordPaint.clearShadowLayer()
                        canvas.drawText(word.text, curX, 0f, wordPaint)
                    }
                }
            }

            curX += wWidth + spaceWidth
        }

        // Restore original paint state
        textPaint.color = baseColor
    }

    /**
     * Line-level fallback rendering with continuous opacity and bass pop
     */
    private fun renderLineLevelLine(
        canvas: Canvas,
        line: LyricLine,
        time: Double,
        isActive: Boolean,
        lineAlpha: Float,
        textPaint: Paint,
        audioFrame: AudioFrame?,
    ) {
        wordPaint.set(textPaint)
        val alphaMultiplier = if (isActive) 1.0f else 0.45f
        wordPaint.alpha = (255 * lineAlpha * alphaMultiplier).toInt().coerceIn(0, 255)

        if (isActive) {
            // Subtle luminescence on active line
            glowPaint.set(wordPaint)
            glowPaint.alpha = (140 * lineAlpha).toInt().coerceIn(0, 255)
            glowPaint.setShadowLayer(14f, 0f, 0f, Color.WHITE)
            canvas.drawText(line.text, 0f, 0f, glowPaint)
        }

        wordPaint.clearShadowLayer()
        canvas.drawText(line.text, 0f, 0f, wordPaint)
    }
}
