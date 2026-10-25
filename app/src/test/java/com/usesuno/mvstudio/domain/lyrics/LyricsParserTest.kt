package com.usesuno.mvstudio.domain.lyrics

import com.usesuno.mvstudio.domain.model.LyricLine
import com.usesuno.mvstudio.domain.model.LyricWord
import com.usesuno.mvstudio.domain.model.Precision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LyricsParserTest {

    @Test
    fun testParseLrc() {
        val lrc = """
            [00:00.00]First line
            [00:03.50]Second line
            [00:07.00]Third line
        """.trimIndent()

        val result = LyricsParser.parseLrc(lrc)

        assertEquals(3, result.lines.size)
        assertEquals("First line", result.lines[0].text)
        assertEquals(0.0, result.lines[0].start, 0.01)
        assertEquals("Second line", result.lines[1].text)
        assertEquals(3.5, result.lines[1].start, 0.01)
        assertEquals("Third line", result.lines[2].text)
        assertEquals(7.0, result.lines[2].start, 0.01)
    }

    @Test
    fun testParseEnhancedLrc() {
        val lrc = """
            [00:00.00]<00:00.00>Hello<00:00.50> world<00:01.00>
            [00:03.00]<00:03.00>This<00:03.30> is<00:03.60> a<00:03.90> test<00:04.20>
        """.trimIndent()

        val result = LyricsParser.parseEnhancedLrc(lrc)

        assertEquals(2, result.lines.size)
        assertEquals(Precision.WORD, result.lines[0].precision)
        assertEquals(5, result.lines[1].words.size)
        assertEquals("Hello", result.lines[0].words[0].text)
        assertEquals(0.0, result.lines[0].words[0].start, 0.01)
        assertEquals(0.5, result.lines[0].words[0].end, 0.01)
    }

    @Test
    fun testParsePlainText() {
        val text = "Line one\nLine two\nLine three"

        val result = LyricsParser.parsePlainText(text, Precision.LINE)

        assertEquals(3, result.lines.size)
        assertEquals("Line one", result.lines[0].text)
        assertEquals(Precision.LINE, result.lines[0].precision)
    }

    @Test
    fun testAutoSegment() {
        val text = "This is a test. This is another sentence! And a third one?"
        val lines = LyricsSegmentation.autoSegment(text, 2.0)

        assertTrue(lines.size >= 3)
        for (line in lines) {
            assertTrue(line.text.isNotEmpty())
            assertTrue(line.end > line.start)
        }
    }

    @Test
    fun testSplitLineAtIndex() {
        val line = LyricLine(
            id = "test",
            text = "Hello world",
            start = 0.0,
            end = 2.0,
            precision = Precision.LINE,
        )

        val (before, after) = LyricsSegmentation.splitLineAtIndex(line, 6)

        assertEquals("Hello", before.text)
        assertEquals("world", after.text)
        assertEquals(0.0, before.start, 0.01)
        assertTrue(after.start > before.start)
    }

    @Test
    fun testMergeLines() {
        val lines = listOf(
            LyricLine(id = "1", text = "Hello", start = 0.0, end = 1.0),
            LyricLine(id = "2", text = "world", start = 1.0, end = 2.0),
            LyricLine(id = "3", text = "test", start = 2.0, end = 3.0),
        )

        val merged = LyricsSegmentation.mergeLines(lines)

        assertEquals("Hello world test", merged.text)
        assertEquals(0.0, merged.start, 0.01)
        assertEquals(3.0, merged.end, 0.01)
    }

    @Test
    fun testApplyGlobalOffset() {
        val lines = listOf(
            LyricLine(id = "1", text = "Line 1", start = 1.0, end = 2.0),
            LyricLine(id = "2", text = "Line 2", start = 3.0, end = 4.0),
        )

        val offset = LyricsSegmentation.applyGlobalOffset(lines, 0.5)

        assertEquals(1.5, offset[0].start, 0.01)
        assertEquals(2.5, offset[0].end, 0.01)
        assertEquals(3.5, offset[1].start, 0.01)
    }

    @Test
    fun testStretchToDuration() {
        val lines = listOf(
            LyricLine(id = "1", text = "Line 1", start = 0.0, end = 2.0),
            LyricLine(id = "2", text = "Line 2", start = 2.0, end = 4.0),
        )

        val stretched = LyricsSegmentation.stretchToDuration(lines, 8.0)

        assertEquals(0.0, stretched[0].start, 0.01)
        assertEquals(4.0, stretched[0].end, 0.01)
        assertEquals(4.0, stretched[1].start, 0.01)
        assertEquals(8.0, stretched[1].end, 0.01)
    }
}