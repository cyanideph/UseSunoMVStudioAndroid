package com.usesuno.mvstudio.domain.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProjectModelTest {

    @Test
    fun testDefaultProject() {
        val project = Project()

        assertEquals("Untitled Project", project.name)
        assertEquals(1, project.version)
        assertEquals(Template.FUME, project.template)
        assertEquals(Background.CUSTOM, project.background)
        assertEquals(AspectRatio.RATIO_16_9, project.aspectRatio)
        assertEquals(Resolution.P1080, project.resolution)
        assertEquals(ExportFormat.MP4, project.exportFormat)
        assertEquals(ExportQuality.HIGH, project.exportQuality)
        assertEquals(30, project.exportFps)
    }

    @Test
    fun testProjectCopy() {
        val original = Project(name = "Test", template = Template.CLASSIC)
        val modified = original.copy(name = "Modified", template = Template.STILL)

        assertEquals("Modified", modified.name)
        assertEquals(Template.STILL, modified.template)
        assertEquals("Test", original.name) // Original unchanged
        assertEquals(Template.CLASSIC, original.template)
    }

    @Test
    fun testLyricLineParcelable() {
        val line = LyricLine(
            id = "test-1",
            text = "Test line",
            start = 1.5,
            end = 3.5,
            precision = Precision.WORD,
            words = listOf(
                LyricWord("Test", 1.5, 2.0),
                LyricWord("line", 2.0, 3.5),
            ),
        )

        val parcel = android.os.Parcel.obtain()
        line.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = LyricLine.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(line.id, restored.id)
        assertEquals(line.text, restored.text)
        assertEquals(line.start, restored.start, 0.001)
        assertEquals(line.end, restored.end, 0.001)
        assertEquals(line.precision, restored.precision)
        assertEquals(line.words.size, restored.words.size)
        assertEquals(line.words[0].text, restored.words[0].text)
    }

    @Test
    fun testTemplateTuningParcelable() {
        val tuning = TemplateTuning(
            speed = 1.5f,
            intensity = 0.8f,
            complexity = 0.6f,
            colorShift = 0.2f,
            customParams = mapOf("custom" to 1.0f),
        )

        val parcel = android.os.Parcel.obtain()
        tuning.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = TemplateTuning.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(tuning.speed, restored.speed, 0.001f)
        assertEquals(tuning.intensity, restored.intensity, 0.001f)
        assertEquals(tuning.complexity, restored.complexity, 0.001f)
        assertEquals(1.0f, restored.customParams["custom"] ?: Float.NaN, 0.001f)
    }

    @Test
    fun testAudioFrameParcelable() {
        val frame = AudioFrame(
            time = 1.23,
            bass = 0.5f,
            lowMid = 0.4f,
            mid = 0.6f,
            vocal = 0.3f,
            treble = 0.2f,
            power = 0.7f,
        )

        val parcel = android.os.Parcel.obtain()
        frame.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = AudioFrame.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(frame.time, restored.time, 0.001)
        assertEquals(frame.bass, restored.bass, 0.001f)
        assertEquals(frame.power, restored.power, 0.001f)
    }

    @Test
    fun testAllTemplatesPresent() {
        val templates = Template.values()
        assertEquals(20, templates.size)

        // Verify all expected templates exist
        val expected = setOf(
            "FUME", "CLASSIC", "CADENZA", "PARTITA", "TILT", "CLADDAGH",
            "MONET", "CAPPELLA", "DIORAMA", "AURORA_TRAVERSE", "PENDOLO",
            "TEMPERA", "SONNET", "STILL", "ASTRAL", "KYOTO", "CHROMA",
            "ELYSIUM", "SYZYGY", "LUMIERE"
        )
        val actual = templates.map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun testAllBackgroundsPresent() {
        val backgrounds = Background.values()
        assertEquals(7, backgrounds.size)

        val expected = setOf(
            "CUSTOM", "THEME_SPACE", "FLUID_LIGHT", "AURORA_NEBULA",
            "AURORA_CURTAIN", "LIGHT_TIDE", "SPACE_STARLIGHT"
        )
        val actual = backgrounds.map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun testResolutions() {
        assertEquals(1280, Resolution.P720.width)
        assertEquals(720, Resolution.P720.height)
        assertEquals(1920, Resolution.P1080.width)
        assertEquals(1080, Resolution.P1080.height)
        assertEquals(2560, Resolution.P1440.width)
        assertEquals(1440, Resolution.P1440.height)
        assertEquals(3840, Resolution.P2160.width)
        assertEquals(2160, Resolution.P2160.height)
    }
}