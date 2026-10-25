package com.usesuno.mvstudio.utils

import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.lyrics.LyricsParser
import com.usesuno.mvstudio.domain.lyrics.LyricsSegmentation
import org.junit.Assert.*
import org.junit.Test

class LyricsParserIntegrationTest {

    @Test
    fun testFullLyricsWorkflow() {
        // Simulate full workflow: paste text -> auto segment -> parse -> edit -> export
        val rawText = """
            Verse 1
            This is the first line of the song
            And this is the second line
            
            Chorus
            This is the chorus
            It repeats several times
            Very catchy melody
            
            Verse 2
            Back to the verse again
            Different lyrics but same tune
            
            Outro
            Fading out now
            The end
        """.trimIndent()

        // 1. Auto-segment plain text
        val segmented = LyricsSegmentation.autoSegment(rawText, 3.0)
        assertTrue(segmented.size >= 6)

        // 2. Parse as plain text (already segmented)
        val parsed = LyricsParser.parsePlainText(rawText, com.usesuno.mvstudio.domain.model.Precision.LINE)
        assertEquals(8, parsed.lines.size) // 8 non-empty lines

        // 3. Apply global offset
        val offset = LyricsSegmentation.applyGlobalOffset(parsed.lines, 0.5)
        assertEquals(0.5, offset[0].start, 0.01)

        // 4. Stretch to fit duration
        val stretched = LyricsSegmentation.stretchToDuration(offset, 30.0)
        assertEquals(0.5, stretched[0].start, 0.01)
        assertTrue(stretched.last().end <= 30.5)

        // 5. Create project
        val project = Project(
            name = "Test Song",
            lines = stretched,
            audioDuration = 30.0,
        )

        assertEquals("Test Song", project.name)
        assertEquals(8, project.lines.size)
        assertEquals(30.0, project.audioDuration, 0.01)
    }

    @Test
    fun testLrcWorkflow() {
        val lrc = """
            [00:10.00]First verse starts here
            [00:15.50]Second line of verse
            [00:21.00]Chorus begins now
            [00:26.30]Sing along with me
            [00:31.80]Back to verse
        """.trimIndent()

        val result = LyricsParser.parseLrc(lrc)

        assertEquals(5, result.lines.size)
        assertEquals("First verse starts here", result.lines[0].text)
        assertEquals(10.0, result.lines[0].start, 0.01)
        assertEquals(15.5, result.lines[1].start, 0.01)

        // Verify end times are set correctly
        assertEquals(15.5, result.lines[0].end, 0.01)
        assertEquals(21.0, result.lines[1].end, 0.01)
    }

    @Test
    fun testEnhancedLrcWorkflow() {
        val lrc = """
            [00:00.00]<00:00.00>Hello<00:00.50> world<00:01.00>
            [00:02.00]<00:02.00>This<00:02.25> is<00:02.50> a<00:02.75> test<00:03.00>
        """.trimIndent()

        val result = LyricsParser.parseEnhancedLrc(lrc)

        assertEquals(2, result.lines.size)
        assertEquals(com.usesuno.mvstudio.domain.model.Precision.WORD, result.lines[0].precision)
        assertEquals(2, result.lines[0].words.size)
        assertEquals(5, result.lines[1].words.size)

        // Verify word timings
        assertEquals("Hello", result.lines[0].words[0].text)
        assertEquals(0.0, result.lines[0].words[0].start, 0.01)
        assertEquals(0.5, result.lines[0].words[0].end, 0.01)

        assertEquals("This", result.lines[1].words[0].text)
        assertEquals(2.0, result.lines[1].words[0].start, 0.01)
    }
}