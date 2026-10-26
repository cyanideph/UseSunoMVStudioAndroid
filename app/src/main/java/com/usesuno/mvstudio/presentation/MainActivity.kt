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
            UseSunoMVStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    EditorScreen(
                        project = viewModel.currentProject,
                        onProjectChange = { viewModel.updateProject(it) },
                        onExportClick = { format, quality, fps -> viewModel.startExport(format, quality, fps) },
                        isAudioPlaying = viewModel.isAudioPlaying,
                        onPlayPauseClick = { viewModel.togglePlayback() },
                        onSettingsClick = { /* Open settings */ },
                        onImportAudioClick = { audioPicker.launch(arrayOf("audio/*")) },
                        onImportLyricsClick = { lyricsPicker.launch(arrayOf("text/*", "application/xml", "application/ttml+xml")) },
                    )
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
    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            val result = projectSerializer.loadLatestDraft()
            if (result is Result.Success) result.value?.let { currentProject = it }
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
                newPlayer.setOnCompletionListener { isAudioPlaying = false }
                newPlayer.setOnErrorListener { player, _, _ ->
                    isAudioPlaying = false
                    player.release()
                    mediaPlayer = null
                    audioFile.delete()
                    true
                }
                newPlayer.prepare()
                mediaPlayer = newPlayer
                newPlayer.start()
                isAudioPlaying = true
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