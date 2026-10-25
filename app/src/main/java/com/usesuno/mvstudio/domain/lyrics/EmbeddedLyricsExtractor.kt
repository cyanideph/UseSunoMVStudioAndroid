package com.usesuno.mvstudio.domain.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.usesuno.mvstudio.domain.model.LyricLine
import com.usesuno.mvstudio.domain.model.Precision
import java.util.UUID

/**
 * Extract embedded lyrics from audio files (ID3 tags)
 * Ported from import/embedded.ts
 */
class EmbeddedLyricsExtractor {

    data class ExtractedLyrics(
        val lyrics: String,
        val format: LyricsFormat,
        val coverArt: ByteArray? = null,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
    )

    enum class LyricsFormat {
        LRC, SYNCED, UNSYNCED, TTML, QRC, YRC, KRC, UNKNOWN
    }

    fun extract(context: Context, uri: Uri): ExtractedLyrics? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            // Android exposes standard metadata keys as integer constants, not ID3 frame names.
            // Scan available metadata slots and accept only values that resemble lyrics.
            var lyrics: String? = null
            var format = LyricsFormat.UNKNOWN

            for (i in 0..20) {
                val value = runCatching { retriever.extractMetadata(i) }.getOrNull()
                if (value != null && value.isNotBlank() && looksLikeLyrics(value)) {
                    lyrics = value
                    format = detectFormat(value)
                    break
                }
            }

            // Extract cover art
            val coverArt = retriever.getEmbeddedPicture()

            // Extract basic metadata
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            return if (lyrics != null) {
                ExtractedLyrics(
                    lyrics = lyrics,
                    format = format,
                    coverArt = coverArt,
                    title = title,
                    artist = artist,
                    album = album,
                )
            } else null

        } finally {
            retriever.release()
        }
    }

    private fun detectFormat(text: String): LyricsFormat {
        return when {
            text.contains("[") && text.contains("]") && text.lines().any { it.matches("\\[\\d{2}:\\d{2}[.:]\\d{2,3}\\]".toRegex()) } -> {
                if (text.contains("<") && text.contains(">") && text.matches(".*<\\d{2}:\\d{2}[.:]\\d{2,3}>.*".toRegex())) {
                    LyricsFormat.SYNCED
                } else {
                    LyricsFormat.LRC
                }
            }
            text.trim().startsWith("<?xml") || text.contains("<ttml") -> LyricsFormat.TTML
            text.contains("[") && text.contains("]") && text.matches(".*\\[\\d{2}:\\d{2}\\.\\d{3}\\]".toRegex()) -> LyricsFormat.QRC
            text.contains("yrc") -> LyricsFormat.YRC
            text.contains("krc") -> LyricsFormat.KRC
            else -> LyricsFormat.UNSYNCED
        }
    }

    private fun looksLikeLyrics(text: String): Boolean {
        val lines = text.lines().filter { it.isNotBlank() }.take(10)
        return lines.any { it.matches(".*\\[\\d{2}:\\d{2}[.:]\\d{2,3}\\].*".toRegex()) } ||
               lines.size > 3 && lines.all { it.length < 200 }
    }

    /**
     * Convert extracted lyrics to LyricLine objects
     */
    fun toLyricLines(extracted: ExtractedLyrics): List<LyricLine> {
        return when (extracted.format) {
            LyricsFormat.LRC, LyricsFormat.SYNCED -> LyricsParser.parseLrc(extracted.lyrics).lines
            LyricsFormat.TTML -> LyricsParser.parseTtml(extracted.lyrics).lines
            else -> LyricsParser.parsePlainText(extracted.lyrics, Precision.LINE).lines
        }
    }
}