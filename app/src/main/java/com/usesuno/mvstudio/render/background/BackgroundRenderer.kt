package com.usesuno.mvstudio.render.background

import android.graphics.Canvas
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.Background
import com.usesuno.mvstudio.domain.model.BackgroundConfig
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.AudioReactivityConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin

/**
 * Background renderer interface
 */
interface BackgroundRenderer {
    fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project)
    fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?)
    fun updateConfig(config: BackgroundConfig)
    fun dispose()
}

/**
 * Background renderer factory
 */
object BackgroundRendererFactory {
    fun createRenderer(background: Background): BackgroundRenderer = when (background) {
        Background.CUSTOM -> CustomBackgroundRenderer()
        Background.THEME_SPACE -> ThemeSpaceRenderer()
        Background.FLUID_LIGHT -> FluidLightRenderer()
        Background.AURORA_NEBULA -> AuroraNebulaRenderer()
        Background.AURORA_CURTAIN -> AuroraCurtainRenderer()
        Background.LIGHT_TIDE -> LightTideRenderer()
        Background.SPACE_STARLIGHT -> SpaceStarlightRenderer()
    }
}

/**
 * Custom background - user image/video
 */
class CustomBackgroundRenderer : BackgroundRenderer {
    private var bitmap: android.graphics.Bitmap? = null
    private var videoPlayer: com.usesuno.mvstudio.domain.audio.MediaCodecAudioDecoder? = null
    private var config: BackgroundConfig = BackgroundConfig()

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
        // Load bitmap from project assets
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        if (bitmap != null) {
            val scaleX = bounds.width() / bitmap!!.width.toFloat()
            val scaleY = bounds.height() / bitmap!!.height.toFloat()
            val scale = max(scaleX, scaleY) * config.scale

            val scaledWidth = (bitmap!!.width * scale).toInt()
            val scaledHeight = (bitmap!!.height * scale).toInt()

            val left = (bounds.centerX() - scaledWidth / 2 + config.offsetX).toInt()
            val top = (bounds.centerY() - scaledHeight / 2 + config.offsetY).toInt()

            val dstRect = android.graphics.Rect(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(bitmap!!, null, dstRect, null)
        } else {
            // Solid color fallback
            canvas.drawColor(android.graphics.Color.parseColor(config.color))
        }
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {
        bitmap?.recycle()
        bitmap = null
    }
}

/**
 * Theme Space - Animated gradient space background
 */
class ThemeSpaceRenderer : BackgroundRenderer {
    private var timeOffset = 0.0
    private var config: BackgroundConfig = BackgroundConfig()
    private val paint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        timeOffset = time
        val audioPower = audioFrame?.power ?: 0f

        // Animated radial gradient
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val radius = max(bounds.width(), bounds.height()).toFloat() * 0.7f

        val colors = intArrayOf(
            android.graphics.Color.parseColor(config.color),
            android.graphics.Color.HSVToColor(255, floatArrayOf(((timeOffset * 30) % 360).toFloat(), 0.6f, 0.3f)),
            android.graphics.Color.HSVToColor(255, floatArrayOf(((timeOffset * 30 + 120) % 360).toFloat(), 0.5f, 0.2f)),
            android.graphics.Color.BLACK,
        )

        val positions = floatArrayOf(0f, 0.3f, 0.7f, 1f)
        val shader = android.graphics.RadialGradient(centerX, centerY, radius, colors, positions, android.graphics.Shader.TileMode.CLAMP)
        paint.shader = shader

        canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), paint)

        // Add twinkling stars
        drawStars(canvas, bounds, audioPower)
    }

    private fun drawStars(canvas: Canvas, bounds: Rect, audioPower: Float) {
        paint.shader = null
        paint.style = android.graphics.Paint.Style.FILL

        // Pseudo-random stars based on time
        for (i in 0..100) {
            val seed = (i * 12345 + (timeOffset * 1000).toInt()).toLong()
            val x = (pseudoRandom(seed) * bounds.width()).toFloat() + bounds.left
            val y = (pseudoRandom(seed * 7) * bounds.height()).toFloat() + bounds.top
            val brightness = pseudoRandom(seed * 13)
            val twinkle = (sin(timeOffset * 2 + seed) + 1) / 2

            paint.color = android.graphics.Color.argb(
                (brightness * twinkle * 255 * (1f + audioPower)).toInt(),
                255, 255, 255
            )
            canvas.drawCircle(x, y, brightness * 1.5f, paint)
        }
    }

    private fun pseudoRandom(seed: Long): Float {
        var s = seed
        s = s xor (s shl 13)
        s = s xor (s ushr 7)
        s = s xor (s shl 17)
        return (s and 0xFFFFFFFFL).toFloat() / 0xFFFFFFFFL
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {}
}

/**
 * Fluid Light - Flowing liquid light simulation
 */
class FluidLightRenderer : BackgroundRenderer {
    private val blobs = mutableListOf<FluidBlob>()
    private var config: BackgroundConfig = BackgroundConfig()
    private var audioReactivityConfig: AudioReactivityConfig = AudioReactivityConfig()
    private val paint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }

    data class FluidBlob(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var hue: Float,
        var targetHue: Float,
    )

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
        this.audioReactivityConfig = project.audioReactivityConfig
        repeat(8) { spawnBlob(width, height) }
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f

        val reactivity = audioReactivityConfig
        val bassInfluence = audioBass * reactivity.bassGain

        for (blob in blobs) {
            // Move
            blob.x += blob.vx
            blob.y += blob.vy

            // Bounce
            if (blob.x - blob.radius < bounds.left || blob.x + blob.radius > bounds.right) {
                blob.vx *= -1
            }
            if (blob.y - blob.radius < bounds.top || blob.y + blob.radius > bounds.bottom) {
                blob.vy *= -1
            }

            // Color shift
            blob.hue += (blob.targetHue - blob.hue) * 0.01f
            if (abs(blob.hue - blob.targetHue) < 5) {
                blob.targetHue = (Math.random() * 360).toFloat()
            }

            // Bass pulse
            blob.radius *= 1f + bassInfluence * 0.1f
            blob.radius = blob.radius.coerceIn(50f, 300f)

            // Draw with blur effect (multiple circles)
            for (layer in 0..3) {
                val layerRadius = blob.radius * (1f - layer * 0.2f)
                val alpha = (60 * (1f - layer * 0.25f) * (1f + audioPower)).toInt()
                paint.color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(blob.hue, 0.7f, 0.8f))
                canvas.drawCircle(blob.x, blob.y, layerRadius, paint)
            }
        }

        // Keep blobs in bounds
        blobs.forEach { blob ->
            blob.x = blob.x.coerceIn(bounds.left + blob.radius, bounds.right - blob.radius)
            blob.y = blob.y.coerceIn(bounds.top + blob.radius, bounds.bottom - blob.radius)
        }
    }

    private fun spawnBlob(width: Int, height: Int) {
        blobs.add(FluidBlob(
            x = (Math.random() * width).toFloat(),
            y = (Math.random() * height).toFloat(),
            vx = (Math.random() - 0.5).toFloat() * 20,
            vy = (Math.random() - 0.5).toFloat() * 20,
            radius = (Math.random() * 150 + 80).toFloat(),
            hue = (Math.random() * 360).toFloat(),
            targetHue = (Math.random() * 360).toFloat(),
        ))
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {
        blobs.clear()
    }
}

/**
 * Aurora Nebula - Nebula cloud with aurora colors
 */
class AuroraNebulaRenderer : BackgroundRenderer {
    private val clouds = mutableListOf<NebulaCloud>()
    private var config: BackgroundConfig = BackgroundConfig()
    private val paint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }

    data class NebulaCloud(
        var x: Float,
        var y: Float,
        var radius: Float,
        var hue: Float,
        var rotation: Float,
        var rotationSpeed: Float,
    )

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
        repeat(12) { spawnCloud(width, height) }
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f

        for (cloud in clouds) {
            cloud.rotation += cloud.rotationSpeed

            // Draw multi-layer cloud
            for (layer in 0..4) {
                val layerRadius = cloud.radius * (1f - layer * 0.18f)
                val alpha = (40 * (1f - layer * 0.2f) * (1f + audioPower * 0.5f)).toInt()
                paint.color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(cloud.hue, 0.8f, 0.7f))

                // Offset each layer slightly for depth
                val offsetX = cos(cloud.rotation + layer) * layer * 10
                val offsetY = sin(cloud.rotation + layer) * layer * 10

                canvas.drawCircle(cloud.x + offsetX, cloud.y + offsetY, layerRadius, paint)
            }
        }
    }

    private fun spawnCloud(width: Int, height: Int) {
        clouds.add(NebulaCloud(
            x = (Math.random() * width).toFloat(),
            y = (Math.random() * height).toFloat(),
            radius = (Math.random() * 200 + 100).toFloat(),
            hue = (Math.random() * 120 + 180).toFloat(), // Cyan to purple
            rotation = (Math.random() * 2 * PI).toFloat(),
            rotationSpeed = (Math.random() * 0.002 + 0.0005).toFloat(),
        ))
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {
        clouds.clear()
    }
}

/**
 * Aurora Curtain - Vertical curtain-like aurora
 */
class AuroraCurtainRenderer : BackgroundRenderer {
    private val curtains = mutableListOf<Curtain>()
    private var config: BackgroundConfig = BackgroundConfig()
    private val paint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f }

    data class Curtain(
        var x: Float,
        var phase: Float,
        var hue: Float,
        var width: Float,
        var speed: Float,
    )

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
        val numCurtains = 6
        for (i in 0 until numCurtains) {
            curtains.add(Curtain(
                x = (i * width / numCurtains + (Math.random() * width / numCurtains * 0.5f)).toFloat(),
                phase = (Math.random() * 2 * PI).toFloat(),
                hue = (Math.random() * 60 + 140).toFloat(), // Green
                width = (width / numCurtains * 1.2f).toFloat(),
                speed = (Math.random() * 0.003 + 0.001).toFloat(),
            ))
        }
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f

        for (curtain in curtains) {
            curtain.phase += curtain.speed * (1f + audioPower)

            val path = android.graphics.Path()
            val segments = 30

            for (i in 0..segments) {
                val t = i / segments.toFloat()
                val y = t * bounds.height()
                val waveX = (curtain.x + sin(curtain.phase + t * 6 * PI) * curtain.width * 0.3f
                    + sin(curtain.phase * 1.3f + t * 12 * PI) * curtain.width * 0.15f
                    + sin(curtain.phase * 0.7f + t * 3 * PI) * curtain.width * 0.1f).toFloat()

                if (i == 0) {
                    path.moveTo(waveX, y)
                } else {
                    path.lineTo(waveX, y)
                }
            }

            // Glow
            paint.style = android.graphics.Paint.Style.FILL
            val glowPath = android.graphics.Path(path)
            glowPath.lineTo(bounds.right.toFloat(), bounds.bottom.toFloat())
            glowPath.lineTo(bounds.left.toFloat(), bounds.bottom.toFloat())
            glowPath.close()
            paint.color = android.graphics.Color.HSVToColor(20, floatArrayOf(curtain.hue, 0.9f, 0.8f))
            canvas.drawPath(glowPath, paint)

            // Edge
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 2f + audioPower * 3f
            paint.color = android.graphics.Color.HSVToColor(180, floatArrayOf(curtain.hue, 0.8f, 1f))
            canvas.drawPath(path, paint)
        }
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {
        curtains.clear()
    }
}

/**
 * Light Tide - Flowing light waves
 */
class LightTideRenderer : BackgroundRenderer {
    private val waves = mutableListOf<LightWave>()
    private var config: BackgroundConfig = BackgroundConfig()
    private val paint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }

    data class LightWave(
        var y: Float,
        var amplitude: Float,
        var wavelength: Float,
        var phase: Float,
        var hue: Float,
        var speed: Float,
    )

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
        repeat(8) { i ->
            waves.add(LightWave(
                y = (i * height / 8f + (Math.random() * height / 16f)).toFloat(),
                amplitude = (Math.random() * 50 + 30).toFloat(),
                wavelength = (Math.random() * 300 + 200).toFloat(),
                phase = (Math.random() * 2 * PI).toFloat(),
                hue = (Math.random() * 60 + 30).toFloat(), // Warm
                speed = (Math.random() * 0.02 + 0.01).toFloat(),
            ))
        }
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f
        val audioBass = audioFrame?.bass ?: 0f

        for (wave in waves) {
            wave.phase += wave.speed * (1f + audioPower * 2)

            val path = android.graphics.Path()
            val segments = 100

            for (i in 0..segments) {
                val x = (i * bounds.width() / segments.toFloat())
                val y = (wave.y + sin(wave.phase + x / wave.wavelength * 2 * PI) * wave.amplitude * (1f + audioBass)).toFloat()

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            // Close to bottom
            path.lineTo(bounds.right.toFloat(), bounds.bottom.toFloat())
            path.lineTo(bounds.left.toFloat(), bounds.bottom.toFloat())
            path.close()

            val alpha = (80 * (1f + audioPower * 0.5f)).toInt()
            paint.color = android.graphics.Color.HSVToColor(alpha, floatArrayOf(wave.hue, 0.7f, 0.9f))
            canvas.drawPath(path, paint)
        }
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {
        waves.clear()
    }
}

/**
 * Space Starlight - Starfield with depth
 */
class SpaceStarlightRenderer : BackgroundRenderer {
    private val stars = mutableListOf<Star>()
    private var config: BackgroundConfig = BackgroundConfig()
    private val paint = android.graphics.Paint().apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }

    data class Star(
        var x: Float,
        var y: Float,
        var z: Float, // Depth 0-1
        var size: Float,
        var brightness: Float,
        var twinklePhase: Float,
        var color: Int,
    )

    override fun initialize(config: BackgroundConfig, width: Int, height: Int, project: Project) {
        this.config = config
        val numStars = 500

        repeat(numStars) {
            stars.add(Star(
                x = (Math.random() * width).toFloat(),
                y = (Math.random() * height).toFloat(),
                z = (Math.random() * 0.8f + 0.2f).toFloat(),
                size = (Math.random() * 2 + 0.5).toFloat(),
                brightness = (Math.random() * 0.5 + 0.5).toFloat(),
                twinklePhase = (Math.random() * 2 * PI).toFloat(),
                color = when {
                    Math.random() < 0.6 -> (0xFFFFFFFF).toInt() // White
                    Math.random() < 0.8 -> (0xFFFFFFAA).toInt() // Yellow
                    Math.random() < 0.9 -> (0xFFAAAAFF).toInt() // Blue
                    else -> (0xFFFF8888).toInt() // Red
                },
            ))
        }
    }

    override fun render(canvas: Canvas, time: Double, bounds: Rect, audioFrame: com.usesuno.mvstudio.domain.model.AudioFrame?) {
        val audioPower = audioFrame?.power ?: 0f

        for (star in stars) {
            val twinkle = (sin(time * 3 + star.twinklePhase) + 1) / 2
            val alpha = (star.brightness * twinkle * 255 * star.z * (1f + audioPower * 0.3f)).toInt()
            val size = star.size * star.z * (1f + audioPower * 0.5f)

            paint.color = (star.color and 0xFFFFFF) or (alpha shl 24)
            canvas.drawCircle(star.x, star.y, size, paint)
        }

        // Occasional shooting star
        if (Math.random() < 0.002) {
            spawnShootingStar(bounds.width(), bounds.height())
        }
    }

    private fun spawnShootingStar(width: Int, height: Int) {
        // Would add a temporary shooting star
    }

    override fun updateConfig(config: BackgroundConfig) {
        this.config = config
    }

    override fun dispose() {
        stars.clear()
    }
}