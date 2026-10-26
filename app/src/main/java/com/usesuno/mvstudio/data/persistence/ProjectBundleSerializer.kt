package com.usesuno.mvstudio.data.persistence

import android.content.Context
import android.net.Uri
import com.usesuno.mvstudio.domain.model.*
import com.usesuno.mvstudio.data.db.AppDatabase
import com.usesuno.mvstudio.data.db.ProjectEntity
import com.usesuno.mvstudio.data.db.AssetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Project bundle serializer for .lyricmv format
 * Compatible with UseSunoMVStudio's export/project.ts
 */
class ProjectBundleSerializer(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val database = AppDatabase.getDatabase(context)

    /**
     * Export project to .lyricmv file
     */
    suspend fun exportProject(project: Project, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val zipOutputStream = ZipOutputStream(FileOutputStream(outputFile))
            zipOutputStream.setLevel(6) // Compression level

            // Write project.json
            val projectJson = json.encodeToString(project)
            zipOutputStream.putNextEntry(ZipEntry("project.json"))
            zipOutputStream.write(projectJson.toByteArray())
            zipOutputStream.closeEntry()

            // Write audio file if exists
            project.audioId?.let { audioId ->
                val audioAsset = database.assetDao().getById(audioId)
                audioAsset?.let { asset ->
                    zipOutputStream.putNextEntry(ZipEntry("audio${getExtension(asset.mimeType)}"))
                    zipOutputStream.write(asset.data)
                    zipOutputStream.closeEntry()
                }
            }

            // Write assets
            val assets = database.assetDao().getByProjectId(project.id).firstOrNull() ?: emptyList()
            for (asset in assets) {
                val entryName = "assets/${asset.id}${getExtension(asset.mimeType)}"
                zipOutputStream.putNextEntry(ZipEntry(entryName))
                zipOutputStream.write(asset.data)
                zipOutputStream.closeEntry()
            }

            zipOutputStream.finish()
            zipOutputStream.close()

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import project from .lyricmv file
     */
    suspend fun importProject(uri: Uri): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Unable to open project bundle")
            val zipInputStream = ZipInputStream(inputStream)
            var project: Project? = null
            val deferredAssets = mutableListOf<Pair<String, ByteArray>>()

            var entry: ZipEntry? = zipInputStream.nextEntry
            while (entry != null) {
                val name = entry.name
                val buffer = java.io.ByteArrayOutputStream()
                val data = ByteArray(4096)
                var len = zipInputStream.read(data)
                while (len > 0) {
                    buffer.write(data, 0, len)
                    len = zipInputStream.read(data)
                }
                val bytes = buffer.toByteArray()

                when {
                    name == "project.json" -> {
                        project = json.decodeFromString<Project>(String(bytes, Charsets.UTF_8))
                    }
                    name.startsWith("audio.") || name.startsWith("assets/") -> {
                        deferredAssets.add(name to bytes)
                    }
                }

                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }

            zipInputStream.close()

            project?.let { p ->
                // Save to database
                val projectEntity = ProjectEntity(
                    id = p.id,
                    name = p.name,
                    version = p.version,
                    createdAt = p.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    projectJson = json.encodeToString(p),
                    audioId = p.audioId,
                    audioDuration = p.audioDuration,
                    template = p.template.name,
                    background = p.background.name,
                    aspectRatio = p.aspectRatio.name,
                    resolution = p.resolution.name,
                )

                database.projectDao().insert(projectEntity)
                val assets = deferredAssets.mapNotNull { (name, bytes) ->
                    val isAudioEntry = name.startsWith("audio.")
                    val assetId = if (isAudioEntry) p.audioId else name.substringAfterLast("/").substringBeforeLast(".")
                    assetId?.let {
                        AssetEntity(
                            id = it,
                            projectId = p.id,
                            type = if (isAudioEntry) AssetType.AUDIO else guessAssetType(name),
                            mimeType = getMimeType(name),
                            data = bytes,
                            name = name,
                        )
                    }
                }
                if (assets.isNotEmpty()) database.assetDao().insertAll(assets)

                Result.success(p)
            } ?: Result.failure(IllegalArgumentException("No project.json found in bundle"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save project to local database (auto-save)
     */
    suspend fun saveLocal(project: Project): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val projectEntity = ProjectEntity(
                id = project.id,
                name = project.name,
                version = project.version,
                createdAt = project.createdAt,
                updatedAt = System.currentTimeMillis(),
                projectJson = json.encodeToString(project),
                audioId = project.audioId,
                audioDuration = project.audioDuration,
                template = project.template.name,
                background = project.background.name,
                aspectRatio = project.aspectRatio.name,
                resolution = project.resolution.name,
                isDraft = true,
            )

            database.projectDao().insert(projectEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load latest draft from local database
     */
    suspend fun loadLatestDraft(): Result<Project?> = withContext(Dispatchers.IO) {
        try {
            val entity = database.projectDao().getLatestDraft()
            entity?.let {
                val project = json.decodeFromString<Project>(it.projectJson)
                Result.success(project)
            } ?: Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load project by ID from local database
     */
    suspend fun loadLocal(projectId: String): Result<Project?> = withContext(Dispatchers.IO) {
        try {
            val entity = database.projectDao().getById(projectId)
            entity?.let {
                val project = json.decodeFromString<Project>(it.projectJson)
                Result.success(project)
            } ?: Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List all saved projects
     */
    fun getAllProjects() = database.projectDao().getAllProjects()

    private fun getExtension(mimeType: String): String {
        return when (mimeType) {
            "audio/mpeg" -> ".mp3"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/mp4", "audio/m4a" -> ".m4a"
            "audio/flac" -> ".flac"
            "audio/ogg" -> ".ogg"
            "audio/opus" -> ".opus"
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "image/webp" -> ".webp"
            "video/mp4" -> ".mp4"
            "video/webm" -> ".webm"
            "font/ttf", "application/x-font-ttf" -> ".ttf"
            "font/otf", "application/x-font-otf" -> ".otf"
            "font/woff" -> ".woff"
            "font/woff2" -> ".woff2"
            else -> ".bin"
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast(".").lowercase()
        return when (ext) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "opus" -> "audio/opus"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "ttf" -> "font/ttf"
            "otf" -> "font/otf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
    }

    private fun guessAssetType(fileName: String): AssetType {
        val ext = fileName.substringAfterLast(".").lowercase()
        return when (ext) {
            "mp3", "wav", "m4a", "flac", "ogg", "opus" -> AssetType.AUDIO
            "jpg", "jpeg", "png", "gif", "webp" -> AssetType.IMAGE
            "mp4", "webm" -> AssetType.VIDEO
            "ttf", "otf", "woff", "woff2" -> AssetType.FONT
            else -> AssetType.IMAGE
        }
    }
}

sealed class Result<out T> {
    data class Success<out T>(val value: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)
        fun <T> failure(exception: Exception): Result<T> = Failure(exception)
    }
}