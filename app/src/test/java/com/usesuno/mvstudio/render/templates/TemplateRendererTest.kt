package com.usesuno.mvstudio.render.templates

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.domain.audio.AudioFrame
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TemplateRendererTest {

    private fun createTestProject(template: Template): Project {
        return Project(
            template = template,
            audioDuration = 10.0,
            lines = listOf(
                LyricLine(text = "Test line 1", start = 0.0, end = 3.0),
                LyricLine(text = "Test line 2", start = 3.0, end = 6.0),
                LyricLine(text = "Test line 3", start = 6.0, end = 9.0),
            ),
        )
    }

    private fun createTestAudioFrame(): AudioFrame {
        return AudioFrame(
            time = 1.0,
            bass = 0.5f,
            lowMid = 0.4f,
            mid = 0.6f,
            vocal = 0.7f,
            treble = 0.3f,
            power = 0.5f,
        )
    }

    @Test
    fun testFumeRendererInitialization() {
        val project = createTestProject(Template.FUME)
        val renderer = com.usesuno.mvstudio.render.engine.RendererFactory.createRenderer(Template.FUME)

        renderer.initialize(project, 1920, 1080)
        assertEquals(Template.FUME, renderer.template)
        assertTrue(renderer.supportsAudioReactivity)

        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = Rect(0, 0, 1920, 1080)

        // Should not crash
        renderer.renderFrame(canvas, 1.0, createTestAudioFrame(), bounds)
        renderer.dispose()
    }

    @Test
    fun testClassicRendererInitialization() {
        val project = createTestProject(Template.CLASSIC)
        val renderer = com.usesuno.mvstudio.render.engine.RendererFactory.createRenderer(Template.CLASSIC)

        renderer.initialize(project, 1920, 1080)
        assertEquals(Template.CLASSIC, renderer.template)

        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = Rect(0, 0, 1920, 1080)

        renderer.renderFrame(canvas, 1.0, createTestAudioFrame(), bounds)
        renderer.dispose()
    }

    @Test
    fun testStillRendererInitialization() {
        val project = createTestProject(Template.STILL)
        val renderer = com.usesuno.mvstudio.render.engine.RendererFactory.createRenderer(Template.STILL)

        renderer.initialize(project, 1920, 1080)
        assertEquals(Template.STILL, renderer.template)

        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = Rect(0, 0, 1920, 1080)

        renderer.renderFrame(canvas, 1.0, createTestAudioFrame(), bounds)
        renderer.dispose()
    }

    @Test
    fun testAllRenderersCreateSuccessfully() {
        for (template in Template.values()) {
            val project = createTestProject(template)
            val renderer = com.usesuno.mvstudio.render.engine.RendererFactory.createRenderer(template)

            try {
                renderer.initialize(project, 1280, 720)
                assertEquals(template, renderer.template)

                val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val bounds = Rect(0, 0, 1280, 720)

                renderer.renderFrame(canvas, 1.0, createTestAudioFrame(), bounds)
                renderer.dispose()
            } catch (e: Exception) {
                fail("Renderer for $template failed: ${e.message}")
            }
        }
    }

    @Test
    fun testBackgroundRendererCreation() {
        for (background in Background.values()) {
            val renderer = com.usesuno.mvstudio.render.background.BackgroundRendererFactory.createRenderer(background)
            assertNotNull(renderer)

            val config = BackgroundConfig()
            renderer.initialize(config, 1280, 720, createTestProject(Template.FUME))

            val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bounds = Rect(0, 0, 1280, 720)

            renderer.render(canvas, 1.0, bounds, createTestAudioFrame())
            renderer.dispose()
        }
    }
}