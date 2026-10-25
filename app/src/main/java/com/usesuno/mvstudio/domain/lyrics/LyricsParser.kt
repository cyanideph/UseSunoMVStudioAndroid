package com.usesuno.mvstudio.domain.lyrics

import com.usesuno.mvstudio.domain.model.LyricLine
import com.usesuno.mvstudio.domain.model.LyricWord
import com.usesuno.mvstudio.domain.model.Precision
import java.util.regex.Pattern
import java.util.UUID

/**
 * Lyrics parser ported from UseSunoMVStudio's import/lyrics.ts
 * Supports LRC, TXT, and basic TTML parsing
 */
object LyricsParser {

    private val LRC_LINE_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)")
    private val LRC_WORD_PATTERN = Pattern.compile("<(\\d{2}):(\\d{2})[.:](\\d{2,3})>([^<]+)")

    /**
     * Parse lyrics from text content (auto-detect format)
     */
    fun parse(text: String, defaultPrecision: Precision = Precision.LINE): ParseResult {
        return when {
            text.contains("[") && LRC_LINE_PATTERN.matcher(text).find() -> parseLrc(text)
            text.contains("<") && text.contains(">") && LRC_WORD_PATTERN.matcher(text).find() -> parseEnhancedLrc(text)
            text.trim().startsWith("<?xml") || text.contains("<ttml") -> parseTtml(text)
            else -> parsePlainText(text, defaultPrecision)
        }
    }

    /**
     * Parse standard LRC format
     * [mm:ss.xx]Line text
     * [mm:ss.xx]<mm:ss.xx>Word<mm:ss.xx> word
     */
    fun parseLrc(text: String): ParseResult {
        val lines = mutableListOf<LyricLine>()
        val lineMatcher = LRC_LINE_PATTERN.matcher(text)

        while (lineMatcher.find()) {
            val minutes = lineMatcher.group(1).toInt()
            val seconds = lineMatcher.group(2).toInt()
            val centiseconds = lineMatcher.group(3).toInt()
            val lineText = lineMatcher.group(4).trim()

            val time = minutes * 60.0 + seconds + centiseconds / 100.0

            val words = parseWordsFromLine(lineText)
            val precision = if (words.isNotEmpty()) Precision.WORD else Precision.LINE

            lines.add(LyricLine(
                id = UUID.randomUUID().toString(),
                text = lineText,
                start = time,
                end = time + 3.0, // Default duration, will be adjusted
                precision = precision,
                words = words,
            ))
        }

        // Fix end times based on next line start
        fixLineEndTimes(lines)

        return ParseResult(lines, Precision.LINE)
    }

    /**
     * Parse enhanced LRC with word-level timestamps
     * [mm:ss.xx]<mm:ss.xx>Word<mm:ss.xx> word
     */
    fun parseEnhancedLrc(text: String): ParseResult {
        val lines = mutableListOf<LyricLine>()
        val lineMatcher = LRC_LINE_PATTERN.matcher(text)

        while (lineMatcher.find()) {
            val minutes = lineMatcher.group(1).toInt()
            val seconds = lineMatcher.group(2).toInt()
            val centiseconds = lineMatcher.group(3).toInt()
            val lineContent = lineMatcher.group(4)

            val lineStart = minutes * 60.0 + seconds + centiseconds / 100.0

            // Parse words with timestamps
            val words = mutableListOf<LyricWord>()
            var plainText = ""
            var lastEnd = lineStart

            val wordMatcher = LRC_WORD_PATTERN.matcher(lineContent)
            while (wordMatcher.find()) {
                val wMin = wordMatcher.group(1).toInt()
                val wSec = wordMatcher.group(2).toInt()
                val wCenti = wordMatcher.group(3).toInt()
                val wordText = wordMatcher.group(4)

                val wordStart = wMin * 60.0 + wSec + wCenti / 100.0
                val wordEnd = wordStart + 0.5 // Default, will be fixed

                words.add(LyricWord(
                    text = wordText,
                    start = wordStart,
                    end = wordEnd,
                ))

                plainText += wordText
                lastEnd = wordEnd
            }

            // If no word timestamps found, treat as plain line
            if (words.isEmpty()) {
                val cleanText = lineContent.replace("<[^>]*>".toRegex(), "").trim()
                lines.add(LyricLine(
                    id = UUID.randomUUID().toString(),
                    text = cleanText,
                    start = lineStart,
                    end = lineStart + 3.0,
                    precision = Precision.LINE,
                ))
            } else {
                // Fix word end times
                for (i in 0 until words.size - 1) {
                    val w = words[i]
                    words[i] = w.copy(end = words[i + 1].start)
                }
                // Last word gets default duration
                val lastWord = words.last()
                words[words.lastIndex] = lastWord.copy(end = lastWord.start + 0.5)

                lines.add(LyricLine(
                    id = UUID.randomUUID().toString(),
                    text = plainText,
                    start = lineStart,
                    end = words.last().end,
                    precision = Precision.WORD,
                    words = words,
                ))
            }
        }

        fixLineEndTimes(lines)
        return ParseResult(lines, Precision.WORD)
    }

    /**
     * Parse plain text (one line per paragraph)
     */
    fun parsePlainText(text: String, defaultPrecision: Precision): ParseResult {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, lineText ->
                LyricLine(
                    id = UUID.randomUUID().toString(),
                    text = lineText,
                    start = index * 3.0, // 3 seconds per line default
                    end = (index + 1) * 3.0,
                    precision = defaultPrecision,
                )
            }.toMutableList()

        return ParseResult(lines, defaultPrecision)
    }

    /**
     * Parse TTML format (simplified)
     */
    fun parseTtml(text: String): ParseResult {
        // Simplified TTML parsing - in production use proper XML parser
        // This handles basic <p begin="..." end="...">Text</p> structure
        val lines = mutableListOf<LyricLine>()

        val pPattern = Pattern.compile("<p[^>]*begin=\"([^\"]*)\"[^>]*end=\"([^\"]*)\"[^>]*>([^<]*)</p>")
        val matcher = pPattern.matcher(text)

        while (matcher.find()) {
            val begin = parseTtmlTime(matcher.group(1))
            val end = parseTtmlTime(matcher.group(2))
            val content = matcher.group(3).trim()

            if (content.isNotEmpty()) {
                lines.add(LyricLine(
                    id = UUID.randomUUID().toString(),
                    text = content,
                    start = begin,
                    end = end,
                    precision = Precision.LINE,
                ))
            }
        }

        return ParseResult(lines, Precision.LINE)
    }

    private fun parseTtmlTime(timeStr: String): Double {
        // Handle formats like "1:23.45", "00:01:23.450", "83.45s"
        return if (timeStr.endsWith("s")) {
            timeStr.removeSuffix("s").toDouble()
        } else {
            val parts = timeStr.split(":")
            when (parts.size) {
                3 -> parts[0].toDouble() * 3600 + parts[1].toDouble() * 60 + parts[2].toDouble()
                2 -> parts[0].toDouble() * 60 + parts[1].toDouble()
                else -> timeStr.toDouble()
            }
        }
    }

    private fun parseWordsFromLine(line: String): List<LyricWord> {
        // Simple word splitting - in production use ICU BreakIterator
        val words = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return if (words.isEmpty()) emptyList() else {
            val duration = 3.0 / words.size
            words.mapIndexed { index, word ->
                LyricWord(
                    text = word,
                    start = index * duration,
                    end = (index + 1) * duration,
                )
            }
        }
    }

    private fun fixLineEndTimes(lines: MutableList<LyricLine>) {
        for (i in 0 until lines.size - 1) {
            val current = lines[i]
            val next = lines[i + 1]
            if (current.end > next.start) {
                lines[i] = current.copy(end = next.start)
            }
        }
        // Last line gets reasonable duration
        val last = lines.lastOrNull()
        if (last != null) {
            lines[lines.lastIndex] = last.copy(end = max(last.end, last.start + 2.0))
        }
    }

    data class ParseResult(
        val lines: List<LyricLine>,
        val detectedPrecision: Precision,
    )
}