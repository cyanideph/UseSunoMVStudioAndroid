package com.usesuno.mvstudio.presentation

import android.content.Intent
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.usesuno.mvstudio.data.persistence.ProjectBundleSerializer
import com.usesuno.mvstudio.data.persistence.Result
import com.usesuno.mvstudio.di.UseSunoMVStudioApplication
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.Template
import com.usesuno.mvstudio.domain.lyrics.LyricsParser
import com.usesuno.mvstudio.data.db.AppDatabase
import com.usesuno.mvstudio.data.db.AssetEntity
import com.usesuno.mvstudio.domain.model.AssetType
import com.usesuno.mvstudio.domain.model.Precision
import com.usesuno.mvstudio.presentation.editor.EditorScreen
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importAudio(it) }
    }
    private val lyricsPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importLyrics(it) }
    }

    @Inject
    lateinit var projectSerializer: ProjectBundleSerializer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf("editor") }
            var timingAudioUri by remember { mutableStateOf<Uri?>(null) }
            val scope = rememberCoroutineScope()

            UseSunoMVStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (currentScreen == "timing") {
                        BackHandler {
                            currentScreen = "editor"
                        }
                        com.usesuno.mvstudio.presentation.timestamp.TimestampScreen(
                            project = viewModel.currentProject,
                            onProjectChange = { viewModel.updateProject(it) },
                            onDone = { currentScreen = "editor" },
                            audioUri = timingAudioUri ?: Uri.EMPTY
                        )
                    } else {
                        EditorScreen(
                            project = viewModel.currentProject,
                            onProjectChange = { viewModel.updateProject(it) },
                            onExportClick = { format, quality, fps -> viewModel.startExport(format, quality, fps) },
                            isAudioPlaying = viewModel.isAudioPlaying,
                            onPlayPauseClick = { viewModel.togglePlayback() },
                            onSettingsClick = { /* Open settings */ },
                            onImportAudioClick = { audioPicker.launch(arrayOf("audio/*")) },
                            onImportLyricsClick = { lyricsPicker.launch(arrayOf("text/*", "application/xml", "application/ttml+xml")) },
                            onLoadSampleAudioClick = { viewModel.loadSampleAudio() },
                            playbackTime = viewModel.playbackPosition,
                            onSeekTo = { viewModel.seekTo(it) },
                            onOpenTimingEditor = {
                                scope.launch {
                                    val uri = viewModel.getAudioFileUri()
                                    timingAudioUri = uri
                                    currentScreen = "timing"
                                }
                            }
                        )
                    }
                }
            }
        }

        // Handle incoming .lyricmv files
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        val uri = if (action == Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        } else data
        if ((action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) && uri != null) {
            lifecycleScope.launch {
                val result = projectSerializer.importProject(uri)
                if (result is Result.Success) {
                    viewModel.updateProject(result.value)
                } else {
                    Toast.makeText(this@MainActivity, "Could not import project bundle", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val projectSerializer: ProjectBundleSerializer,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    var currentProject by mutableStateOf(Project())
        private set

    var isAudioPlaying by mutableStateOf(false)
        private set
    var playbackPosition by mutableStateOf(0.0)
        private set
    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            val result = projectSerializer.loadLatestDraft()
            if (result is Result.Success) result.value?.let { currentProject = it }
        }
    }

    fun seekTo(seconds: Double) {
        playbackPosition = seconds
        try {
            mediaPlayer?.seekTo((seconds * 1000).toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlaybackTicker() {
        viewModelScope.launch {
            while (isAudioPlaying) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        playbackPosition = player.currentPosition / 1000.0
                    }
                }
                kotlinx.coroutines.delay(40)
            }
        }
    }

    suspend fun getAudioFileUri(): Uri? = withContext(Dispatchers.IO) {
        val audioId = currentProject.audioId ?: return@withContext null
        val asset = AppDatabase.getDatabase(context).assetDao().getById(audioId) ?: return@withContext null
        val extension = asset.name.substringAfterLast('.', "audio").filter { it.isLetterOrDigit() }.ifBlank { "audio" }
        val audioFile = File(context.cacheDir, "timing-${asset.id}.$extension").apply { writeBytes(asset.data) }
        Uri.fromFile(audioFile)
    }

    fun loadSampleAudio() {
        viewModelScope.launch {
            try {
                val assetId = java.util.UUID.randomUUID().toString()
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        context.assets.open("sample.mp3").use { it.readBytes() }
                    } catch (e: Exception) {
                        context.resources.openRawResource(com.usesuno.mvstudio.R.raw.sample).use { it.readBytes() }
                    }
                }
                val sampleFile = File(context.cacheDir, "sample-import.mp3").apply {
                    writeBytes(bytes)
                }
                val fileUri = Uri.fromFile(sampleFile)
                val audioImport = com.usesuno.mvstudio.utils.AudioFileImporter(context).importAudio(fileUri)

                val importedAsset = if (audioImport is com.usesuno.mvstudio.utils.AudioFileImporter.ImportResult.Success) {
                    audioImport.asset
                } else {
                    AssetEntity(
                        id = assetId,
                        projectId = currentProject.id,
                        name = "Neon Pulse (Sample).mp3",
                        type = AssetType.AUDIO,
                        mimeType = "audio/mpeg",
                        data = bytes
                    )
                }
                AppDatabase.getDatabase(context).assetDao().insert(
                    importedAsset.copy(projectId = currentProject.id)
                )

                val sampleLyrics = listOf(
                    com.usesuno.mvstudio.domain.model.LyricLine(
                        id = "line-1",
                        text = "Dancing in the neon lights",
                        start = 1.0,
                        end = 4.5,
                        words = listOf(
                            com.usesuno.mvstudio.domain.model.LyricWord("Dancing", 1.0, 1.8),
                            com.usesuno.mvstudio.domain.model.LyricWord("in", 1.9, 2.2),
                            com.usesuno.mvstudio.domain.model.LyricWord("the", 2.3, 2.6),
                            com.usesuno.mvstudio.domain.model.LyricWord("neon", 2.7, 3.4),
                            com.usesuno.mvstudio.domain.model.LyricWord("lights", 3.5, 4.5),
                        )
                    ),
                    com.usesuno.mvstudio.domain.model.LyricLine(
                        id = "line-2",
                        text = "Feel the rhythm of the city pulse",
                        start = 5.0,
                        end = 9.0,
                        words = listOf(
                            com.usesuno.mvstudio.domain.model.LyricWord("Feel", 5.0, 5.6),
                            com.usesuno.mvstudio.domain.model.LyricWord("the", 5.7, 6.0),
                            com.usesuno.mvstudio.domain.model.LyricWord("rhythm", 6.1, 7.0),
                            com.usesuno.mvstudio.domain.model.LyricWord("of", 7.1, 7.3),
                            com.usesuno.mvstudio.domain.model.LyricWord("the", 7.4, 7.6),
                            com.usesuno.mvstudio.domain.model.LyricWord("city", 7.7, 8.3),
                            com.usesuno.mvstudio.domain.model.LyricWord("pulse", 8.4, 9.0),
                        )
                    ),
                    com.usesuno.mvstudio.domain.model.LyricLine(
                        id = "line-3",
                        text = "Lost inside this vibrant sound",
                        start = 9.5,
                        end = 14.0,
                        words = listOf(
                            com.usesuno.mvstudio.domain.model.LyricWord("Lost", 9.5, 10.2),
                            com.usesuno.mvstudio.domain.model.LyricWord("inside", 10.3, 11.2),
                            com.usesuno.mvstudio.domain.model.LyricWord("this", 11.3, 11.7),
                            com.usesuno.mvstudio.domain.model.LyricWord("vibrant", 11.8, 12.8),
                            com.usesuno.mvstudio.domain.model.LyricWord("sound", 12.9, 14.0),
                        )
                    ),
                    com.usesuno.mvstudio.domain.model.LyricLine(
                        id = "line-4",
                        text = "Every beat is taking control",
                        start = 14.5,
                        end = 19.0,
                        words = listOf(
                            com.usesuno.mvstudio.domain.model.LyricWord("Every", 14.5, 15.2),
                            com.usesuno.mvstudio.domain.model.LyricWord("beat", 15.3, 16.0),
                            com.usesuno.mvstudio.domain.model.LyricWord("is", 16.1, 16.4),
                            com.usesuno.mvstudio.domain.model.LyricWord("taking", 16.5, 17.5),
                            com.usesuno.mvstudio.domain.model.LyricWord("control", 17.6, 19.0),
                        )
                    ),
                    com.usesuno.mvstudio.domain.model.LyricLine(
                        id = "line-5",
                        text = "We light up the night",
                        start = 19.5,
                        end = 25.0,
                        words = listOf(
                            com.usesuno.mvstudio.domain.model.LyricWord("We", 19.5, 20.0),
                            com.usesuno.mvstudio.domain.model.LyricWord("light", 20.1, 21.0),
                            com.usesuno.mvstudio.domain.model.LyricWord("up", 21.1, 21.5),
                            com.usesuno.mvstudio.domain.model.LyricWord("the", 21.6, 22.0),
                            com.usesuno.mvstudio.domain.model.LyricWord("night", 22.1, 25.0),
                        )
                    )
                )

                val duration = if (audioImport is com.usesuno.mvstudio.utils.AudioFileImporter.ImportResult.Success) {
                    audioImport.metadata.duration
                } else 25.0

                updateProject(currentProject.copy(
                    name = "Neon Pulse (Sample)",
                    audioId = importedAsset.id,
                    audioDuration = if (duration > 0) duration else 25.0,
                    lines = if (currentProject.lines.isEmpty()) sampleLyrics else currentProject.lines
                ))

                Toast.makeText(context, "Sample music loaded!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Could not load sample: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateProject(project: Project) {
        currentProject = project.copy(updatedAt = System.currentTimeMillis())
        // Auto-save draft
        viewModelScope.launch {
            projectSerializer.saveLocal(currentProject)
        }
    }

    fun startExport(
        format: com.usesuno.mvstudio.domain.model.ExportFormat,
        quality: com.usesuno.mvstudio.domain.model.ExportQuality,
        fps: Int,
    ) {
        val configuredProject = currentProject.copy(exportFormat = format, exportQuality = quality, exportFps = fps)
        val audioId = configuredProject.audioId
        if (audioId == null) {
            Toast.makeText(context, "Import an audio track before exporting", Toast.LENGTH_LONG).show()
            return
        }
        updateProject(configuredProject)
        val outputDirectory = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            Toast.makeText(context, "Could not create export folder", Toast.LENGTH_LONG).show()
            return
        }
        val outputFile = File(outputDirectory, "${configuredProject.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}-${System.currentTimeMillis()}.mp4")
        val intent = Intent(context, com.usesuno.mvstudio.render.export.ExportService::class.java).apply {
            putExtra(com.usesuno.mvstudio.render.export.ExportService.EXTRA_PROJECT, configuredProject)
            putExtra(com.usesuno.mvstudio.render.export.ExportService.EXTRA_OUTPUT_PATH, outputFile.absolutePath)
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
            Toast.makeText(context, "Video export started", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(context, "Could not start export: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    fun togglePlayback() {
        val player = mediaPlayer
        if (player?.isPlaying == true) {
            player.pause()
            isAudioPlaying = false
            return
        }
        if (player != null) {
            player.start()
            isAudioPlaying = true
            startPlaybackTicker()
            return
        }
        val audioId = currentProject.audioId ?: return
        viewModelScope.launch {
            val asset = AppDatabase.getDatabase(context).assetDao().getById(audioId) ?: return@launch
            val extension = asset.name.substringAfterLast('.', "audio").filter { it.isLetterOrDigit() }.ifBlank { "audio" }
            val audioFile = withContext(Dispatchers.IO) {
                File(context.cacheDir, "preview-${asset.id}.$extension").apply { writeBytes(asset.data) }
            }
            val newPlayer = MediaPlayer()
            try {
                newPlayer.setDataSource(audioFile.absolutePath)
                newPlayer.setOnCompletionListener {
                    isAudioPlaying = false
                    playbackPosition = 0.0
                }
                newPlayer.setOnErrorListener { p, _, _ ->
                    isAudioPlaying = false
                    p.release()
                    mediaPlayer = null
                    audioFile.delete()
                    true
                }
                newPlayer.prepare()
                mediaPlayer = newPlayer
                newPlayer.start()
                isAudioPlaying = true
                startPlaybackTicker()
            } catch (error: Exception) {
                newPlayer.release()
                audioFile.delete()
            }
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            val resolver = context.contentResolver
            val importedAudio = withContext(Dispatchers.IO) {
                val source = File(context.cacheDir, "audio-import-${java.util.UUID.randomUUID()}")
                val input = resolver.openInputStream(uri) ?: return@withContext null
                input.use { stream -> source.outputStream().use { stream.copyTo(it) } }
                source
            } ?: return@launch
            val audioImport = com.usesuno.mvstudio.utils.AudioFileImporter(context).importAudio(uri)
            if (audioImport !is com.usesuno.mvstudio.utils.AudioFileImporter.ImportResult.Success) {
                importedAudio.delete()
                return@launch
            }
            val importedAsset = audioImport.asset
            val assetId = importedAsset.id
            val project = currentProject
            AppDatabase.getDatabase(context).assetDao().insert(
                importedAsset.copy(projectId = project.id)
            )
            val importedLines = audioImport.embeddedLyrics?.let { embedded ->
                LyricsParser.parse(embedded.lyrics, Precision.LINE).lines
            }.orEmpty()
            importedAudio.delete()
            updateProject(project.copy(
                audioId = assetId,
                audioDuration = audioImport.metadata.duration,
                lines = importedLines.ifEmpty { project.lines },
                name = audioImport.metadata.title ?: importedAsset.name.substringBeforeLast('.', importedAsset.name),
            ))
        }
    }

    fun importLyrics(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } ?: return@launch
            val parsed = LyricsParser.parse(text, Precision.LINE)
            if (parsed.lines.isNotEmpty()) {
                updateProject(currentProject.copy(lines = parsed.lines))
            }
        }
    }

    fun createNewProject() {
        currentProject = Project()
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val result = projectSerializer.loadLocal(projectId)
            if (result is Result.Success) {
                result.value?.let { currentProject = it }
            }
        }
    }
}