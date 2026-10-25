package com.usesuno.mvstudio.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.usesuno.mvstudio.data.db.AssetEntity
import com.usesuno.mvstudio.domain.model.AssetType
import com.usesuno.mvstudio.domain.lyrics.EmbeddedLyricsExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Audio file importer using MediaStore
 */
class AudioFileImporter(private val context: Context) {

    private val extractor = EmbeddedLyricsExtractor()

    /**
     * Create audio file picker launcher (caller must call launch())
     */
    fun createAudioFilePickerLauncher(activity: ComponentActivity, onResult: (Uri?) -> Unit): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                onResult(uri)
            } else {
                onResult(null)
            }
        }
    }

    /**
     * Create audio file picker intent
     */
    fun createAudioFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "audio/mpeg",
                "audio/wav",
                "audio/mp4",
                "audio/m4a",
                "audio/flac",
                "audio/ogg",
                "audio/opus",
            ))
        }
    }

    /**
     * Import audio file and extract metadata
     */
    suspend fun importAudio(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val resolver: ContentResolver = context.contentResolver

            // Get file info
            val cursor = resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                    val mimeIndex = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)

                    val name = if (nameIndex >= 0) c.getString(nameIndex) else "audio"
                    val size = if (sizeIndex >= 0) c.getLong(sizeIndex) else 0L
                    val mimeType = if (mimeIndex >= 0) c.getString(mimeIndex) else "audio/mpeg"

                    ImportFileInfo(name, size, mimeType)
                } else null
            }

            // Read file data
            val inputStream = resolver.openInputStream(uri)!!
            val data = readStreamToBytes(inputStream)
            inputStream.close()

            // Extract metadata and embedded lyrics
            val metadata = extractMetadata(uri)
            val embeddedLyrics = extractor.extract(context, uri)

            // Create asset entity
            val assetId = UUID.randomUUID().toString()
            val asset = AssetEntity(
                id = assetId,
                projectId = "", // Will be set when added to project
                type = AssetType.AUDIO,
                mimeType = cursor?.mimeType ?: "audio/mpeg",
                data = data,
                name = cursor?.name ?: "audio",
            )

            ImportResult.Success(asset, metadata, embeddedLyrics)
        } catch (e: Exception) {
            ImportResult.Failure(e)
        }
    }

    private suspend fun extractMetadata(uri: Uri): AudioMetadata {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull()?.div(1000) ?: 0L

                // Get cover art
                val coverArt = retriever.getEmbeddedPicture()

                AudioMetadata(
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration.toDouble(),
                    coverArt = coverArt,
                )
            } finally {
                retriever.release()
            }
        }
    }

    private fun readStreamToBytes(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(4096)
        var n = inputStream.read(data)
        while (n != -1) {
            buffer.write(data, 0, n)
            n = inputStream.read(data)
        }
        return buffer.toByteArray()
    }

    data class ImportFileInfo(
        val name: String,
        val size: Long,
        val mimeType: String,
    )

    data class AudioMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val duration: Double,
        val coverArt: ByteArray?,
    )

    sealed class ImportResult {
        data class Success(
            val asset: AssetEntity,
            val metadata: AudioMetadata,
            val embeddedLyrics: EmbeddedLyricsExtractor.ExtractedLyrics?,
        ) : ImportResult()

        data class Failure(val exception: Exception) : ImportResult()
    }
}

/**
 * Image/Video file importer for backgrounds
 */
class MediaFileImporter(private val context: Context) {

    fun createImageFilePickerLauncher(activity: ComponentActivity, onResult: (Uri?) -> Unit): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onResult(result.data?.data)
            } else {
                onResult(null)
            }
        }
    }

    fun createImageFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    fun createVideoFilePickerLauncher(activity: ComponentActivity, onResult: (Uri?) -> Unit): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onResult(result.data?.data)
            } else {
                onResult(null)
            }
        }
    }

    fun createVideoFilePickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    suspend fun importFile(uri: Uri, type: AssetType): Result<AssetEntity> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri)!!
            val data = readStreamToBytes(inputStream)
            inputStream.close()

            val cursor = resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val mimeIndex = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    c.getString(nameIndex ?: 0) to c.getString(mimeIndex ?: 0)
                } else "file" to "application/octet-stream"
            } ?: "file" to "application/octet-stream"

            val asset = AssetEntity(
                id = UUID.randomUUID().toString(),
                projectId = "",
                type = type,
                mimeType = cursor.second,
                data = data,
                name = cursor.first,
            )

            Result.Success(asset)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private fun readStreamToBytes(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(4096)
        var n = inputStream.read(data)
        while (n != -1) {
            buffer.write(data, 0, n)
            n = inputStream.read(data)
        }
        return buffer.toByteArray()
    }

    sealed class Result<out T> {
        data class Success<out T>(val value: T) : Result<T>()
        data class Failure(val exception: Exception) : Result<Nothing>()
    }
}