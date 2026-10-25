package com.usesuno.mvstudio.render.engine

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.render.background.BackgroundRenderer
import com.usesuno.mvstudio.render.background.BackgroundRendererFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Base Canvas 2D renderer for templates that use 2D canvas drawing
 */
abstract class Canvas2DRenderer(
    override val template: Template,
) : RenderEngine {

    protected var project: Project = Project()
    protected var width: Int = 1920
    protected var height: Int = 1080
    protected var bounds: Rect = Rect(0, 0, 1920, 1080)

    // Background renderer
    protected var backgroundRenderer: BackgroundRenderer? = null

    // Typography
    protected var textPaint: Paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    protected var strokePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Animation state
    protected var animationTime: Double = 0.0
    protected var introProgress: Float = 0f

    override fun initialize(project: Project, width: Int, height: Int) {
        this.project = project
        this.width = width
        this.height = height
        this.bounds = Rect(0, 0, width, height)
        updateTypography()
        initializeBackground()
        onInitialize()
    }

    private fun initializeBackground() {
        backgroundRenderer = BackgroundRendererFactory.createRenderer(project.background)
        backgroundRenderer?.initialize(project.backgroundConfig, width, height, project)
    }

    protected open fun onInitialize() {}

    override fun updateSettings(project: Project) {
        val oldTemplate = this.project.template
        val oldBackground = this.project.background
        this.project = project
        updateTypography()

        if (oldTemplate != project.template) {
            onTemplateChanged(oldTemplate, project.template)
        }

        if (oldBackground != project.background) {
            initializeBackground()
        } else {
            backgroundRenderer?.updateConfig(project.backgroundConfig)
        }
    }

    private fun updateTypography() {
        val scale = min(width, height) / 1080.0f
        textPaint.textSize = project.fontSize * 64 * scale
        textPaint.typeface = Typeface.create(project.fontFamily, project.fontWeight, false)
        textPaint.color = android.graphics.Color.parseColor(project.textColor)

        if (project.strokeWidth > 0) {
            strokePaint.color = android.graphics.Color.parseColor(project.strokeColor)
            strokePaint.strokeWidth = project.strokeWidth * scale
            textPaint.setStrokeWidth(strokePaint.strokeWidth)
            textPaint.style = Paint.Style.FILL_AND_STROKE
        } else {
            textPaint.style = Paint.Style.FILL
        }

        // Shadow
        if (project.shadowBlur > 0) {
            textPaint.setShadowLayer(
                project.shadowBlur * scale,
                project.shadowOffsetX * scale,
                project.shadowOffsetY * scale,
                android.graphics.Color.parseColor(project.shadowColor),
            )
        } else {
            textPaint.clearShadowLayer()
        }
    }

    protected open fun onTemplateChanged(oldTemplate: Template, newTemplate: Template) {}

    override fun renderFrame(
        canvas: Canvas,
        time: Double,
        audioFrame: AudioFrame?,
        bounds: Rect,
    ) {
        this.animationTime = time
        this.bounds = bounds

        // Render background
        backgroundRenderer?.render(canvas, time, bounds, audioFrame)

        // Calculate intro progress
        if (project.introEnabled && time < project.introDuration) {
            introProgress = (time / project.introDuration).toFloat()
        } else {
            introProgress = 1f
        }

        // Render template-specific content
        renderTemplate(canvas, time, audioFrame)

        // Render lyrics
        renderLyrics(canvas, time)
    }

    protected abstract fun renderTemplate(
        canvas: Canvas,
        time: Double,
        audioFrame: AudioFrame?,
    )

    protected open fun renderLyrics(canvas: Canvas, time: Double) {
        val visibleLines = project.lines.filter { it.start <= time && it.end >= time }
        if (visibleLines.isEmpty()) return

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val lineHeight = textPaint.textSize * project.lineHeight

        for ((index, line) in visibleLines.withIndex()) {
            val y = centerY + (index - visibleLines.size / 2f) * lineHeight

            // Apply intro animation
            val alpha = if (project.introEnabled && introProgress < 1f) {
                applyIntroAnimation(introProgress, index, visibleLines.size)
            } else 1f

            textPaint.alpha = (255 * alpha).toInt()

            // Draw line text
            canvas.drawText(line.text, centerX, y, textPaint)

            // Draw word highlights if word-level precision
            if (line.precision == Precision.WORD && line.words.isNotEmpty()) {
                renderWordHighlights(canvas, line, time, centerX, y)
            }
        }
    }

    protected open fun applyIntroAnimation(progress: Float, lineIndex: Int, totalLines: Int): Float {
        return when (project.introStyle) {
            IntroStyle.FADE -> progress
            IntroStyle.SLIDE_UP -> {
                val delay = lineIndex * 0.1
                max(0f, min(1f, (progress - delay) / (1f - delay)))
            }
            IntroStyle.SLIDE_DOWN -> {
                val delay = (totalLines - lineIndex - 1) * 0.1
                max(0f, min(1f, (progress - delay) / (1f - delay)))
            }
            IntroStyle.ZOOM -> progress * progress
            IntroStyle.TYPEWRITER -> {
                val charProgress = progress * line.text.length
                min(1f, charProgress / line.text.length)
            }
            else -> 1f
        }
    }

    protected open fun renderWordHighlights(
        canvas: Canvas,
        line: LyricLine,
        time: Double,
        centerX: Float,
        centerY: Float,
    ) {
        // Default: highlight current word
        val currentWord = line.words.find { it.start <= time && it.end >= time }
        currentWord?.let { word ->
            // Measure text up to this word
            val beforeText = line.words
                .takeWhile { it != word }
                .joinToString(" ") { it.text }
            val beforeWidth = if (beforeText.isEmpty()) 0f else textPaint.measureText(beforeText + " ")

            val wordWidth = textPaint.measureText(word.text)

            // Draw highlight background
            val highlightPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFFF0080") // Semi-transparent yellow
            }
            canvas.drawRect(
                centerX - textPaint.measureText(line.text) / 2 + beforeWidth,
                centerY - textPaint.textSize,
                centerX - textPaint.measureText(line.text) / 2 + beforeWidth + wordWidth,
                centerY + textPaint.descent(),
                highlightPaint,
            )

            // Redraw word on top
            textPaint.color = android.graphics.Color.parseColor("#000000")
            canvas.drawText(word.text, centerX - textPaint.measureText(line.text) / 2 + beforeWidth + wordWidth / 2, centerY, textPaint)
            textPaint.color = android.graphics.Color.parseColor(project.textColor)
        }
    }

    override fun dispose() {
        backgroundRenderer?.dispose()
        backgroundRenderer = null
    }

    override val supportsAudioReactivity: Boolean = true
}

/**
 * Factory for creating template renderers
 */
object RendererFactory {
    fun createRenderer(template: Template): RenderEngine = when (template) {
        Template.FUME -> FumeRenderer()
        Template.CLASSIC -> ClassicRenderer()
        Template.STILL -> StillRenderer()
        Template.CADENZA -> CadenzaRenderer()
        Template.PARTITA -> PartitaRenderer()
        Template.TILT -> TiltRenderer()
        Template.CLADDAGH -> CladdaghRenderer()
        Template.MONET -> MonetRenderer()
        Template.CAPPELLA -> CappellaRenderer()
        Template.DIORAMA -> DioramaRenderer()
        Template.AURORA_TRAVERSE -> AuroraTraverseRenderer()
        Template.PENDOLO -> PendoloRenderer()
        Template.TEMPERA -> TemperaRenderer()
        Template.SONNET -> SonnetRenderer()
        Template.ASTRAL -> AstralRenderer()
        Template.KYOTO -> KyotoRenderer()
        Template.CHROMA -> ChromaRenderer()
        Template.ELYSIUM -> ElysiumRenderer()
        Template.SYZYGY -> SyzygyRenderer()
        Template.LUMIERE -> LumiereRenderer()
    }
}