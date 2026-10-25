package com.usesuno.mvstudio.domain.lyrics

import com.usesuno.mvstudio.domain.model.LyricLine
import com.usesuno.mvstudio.domain.model.LyricWord
import com.usesuno.mvstudio.domain.model.Precision
import java.util.UUID
import java.util.regex.Pattern
import kotlin.math.max

/**
 * Lyrics segmentation and manipulation utilities
 * Ported from import/segmentation.ts
 */
object LyricsSegmentation {

    /**
     * Split a line at a specific character index
     */
    fun splitLineAtIndex(line: LyricLine, charIndex: Int): Pair<LyricLine, LyricLine> {
        val text = line.text
        val beforeText = text.substring(0, charIndex).trim()
        val afterText = text.substring(charIndex).trim()

        val midTime = line.start + (line.end - line.start) * (charIndex.toDouble() / text.length)

        val beforeWords = if (line.precision == Precision.WORD) {
            splitWordsAtTime(line.words, midTime, true)
        } else emptyList()

        val afterWords = if (line.precision == Precision.WORD) {
            splitWordsAtTime(line.words, midTime, false)
        } else emptyList()

        val before = line.copy(
            id = UUID.randomUUID().toString(),
            text = beforeText,
            end = midTime,
            words = beforeWords,
        )

        val after = line.copy(
            id = UUID.randomUUID().toString(),
            text = afterText,
            start = midTime,
            words = afterWords,
        )

        return before to after
    }

    private fun splitWordsAtTime(words: List<LyricWord>, time: Double, before: Boolean): List<LyricWord> {
        return words.filter { word ->
            if (before) word.end <= time else word.start >= time
        }.map { word ->
            if (before && word.end > time) {
                word.copy(end = time)
            } else if (!before && word.start < time) {
                word.copy(start = time)
            } else word
        }
    }

    /**
     * Merge multiple lines into one
     */
    fun mergeLines(lines: List<LyricLine>): LyricLine {
        require(lines.isNotEmpty()) { "Cannot merge empty list" }

        val sorted = lines.sortedBy { it.start }
        val first = sorted.first()
        val last = sorted.last()

        val mergedText = sorted.joinToString(" ") { it.text }
        val mergedWords = if (first.precision == Precision.WORD) {
            sorted.flatMap { it.words }.sortedBy { it.start }
        } else emptyList()

        return first.copy(
            id = UUID.randomUUID().toString(),
            text = mergedText,
            end = last.end,
            words = mergedWords,
        )
    }

    /**
     * Delete a line and redistribute time to neighbors
     */
    fun deleteLine(lines: MutableList<LyricLine>, index: Int): List<LyricLine> {
        require(index in lines.indices) { "Index out of bounds" }

        val deleted = lines.removeAt(index)
        val deletedDuration = deleted.end - deleted.start

        // Redistribute time to previous and next lines
        when {
            index > 0 && index < lines.size -> {
                // Redistribute to both neighbors
                val prev = lines[index - 1]
                val next = lines[index]
                val halfDuration = deletedDuration / 2
                lines[index - 1] = prev.copy(end = prev.end + halfDuration)
                lines[index] = next.copy(start = next.start - halfDuration)
            }
            index > 0 -> {
                // Only previous neighbor
                val prev = lines[index - 1]
                lines[index - 1] = prev.copy(end = prev.end + deletedDuration)
            }
            index < lines.size -> {
                // Only next neighbor
                val next = lines[index]
                lines[index] = next.copy(start = next.start - deletedDuration)
            }
        }

        return lines
    }

    /**
     * Auto-segment plain text into lines based on punctuation and length
     */
    fun autoSegment(text: String, avgLineDuration: Double = 3.0): List<LyricLine> {
        // Split by punctuation first
        val segments = text.split(Pattern.compile("(?<=[.!?。！？])\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Further split long segments
        val lines = mutableListOf<String>()
        for (segment in segments) {
            if (segment.length <= 40) {
                lines.add(segment)
            } else {
                // Split by commas or at ~40 chars
                val subSegments = segment.split(Pattern.compile("(?<=[,，])\\s*"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                var current = ""
                for (sub in subSegments) {
                    if ((current + sub).length <= 40) {
                        current += (if (current.isEmpty()) "" else " ") + sub
                    } else {
                        if (current.isNotEmpty()) lines.add(current)
                        current = sub
                    }
                }
                if (current.isNotEmpty()) lines.add(current)
            }
        }

        // Assign timings
        return lines.mapIndexed { index, lineText ->
            LyricLine(
                id = UUID.randomUUID().toString(),
                text = lineText,
                start = index * avgLineDuration,
                end = (index + 1) * avgLineDuration,
                precision = Precision.LINE,
            )
        }
    }

    /**
     * Apply global offset to all lines
     */
    fun applyGlobalOffset(lines: List<LyricLine>, offset: Double): List<LyricLine> {
        return lines.map { line ->
            line.copy(
                start = max(0.0, line.start + offset),
                end = max(0.0, line.end + offset),
                words = line.words.map { word ->
                    word.copy(
                        start = max(0.0, word.start + offset),
                        end = max(0.0, word.end + offset),
                    )
                },
            )
        }
    }

    /**
     * Stretch/compress timing to fit duration
     */
    fun stretchToDuration(lines: List<LyricLine>, targetDuration: Double): List<LyricLine> {
        val currentDuration = lines.lastOrNull()?.end ?: 0.0
        if (currentDuration <= 0) return lines

        val factor = targetDuration / currentDuration
        return lines.map { line ->
            line.copy(
                start = line.start * factor,
                end = line.end * factor,
                words = line.words.map { word ->
                    word.copy(
                        start = word.start * factor,
                        end = word.end * factor,
                    )
                },
            )
        }
    }
}