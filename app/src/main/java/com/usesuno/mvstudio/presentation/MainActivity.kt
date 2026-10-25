package com.usesuno.mvstudio.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Material3
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.ViewModel
import androidx.lifecycle.viewmodel.viewModelScope
import com.usesuno.mvstudio.data.persistence.ProjectBundleSerializer
import com.usesuno.mvstudio.di.UseSunoMVStudioApplication
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.model.Template
import com.usesuno.mvstudio.presentation.editor.EditorScreen
import com.usesuno.mvstudio.ui.theme.UseSunoMVStudioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by hiltViewModel()

    @Inject
    lateinit var projectSerializer: ProjectBundleSerializer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UseSunoMVStudioTheme {
                Material3 {
                    Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    ) {
                        EditorScreen(
                            project = viewModel.currentProject,
                            onProjectChange = { viewModel.updateProject(it) },
                            onExportClick = { viewModel.startExport() },
                            onSettingsClick = { /* Open settings */ },
                            onImportAudioClick = { viewModel.importAudio() },
                            onImportLyricsClick = { viewModel.importLyrics() },
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
        if (action == Intent.ACTION_VIEW && data != null) {
            val file = data.toFile()
            if (file.extension == "lyricmv") {
                lifecycleScope.launch {
                    val result = projectSerializer.importProject(file)
                    if (result is ProjectBundleSerializer.Result.Success) {
                        viewModel.updateProject(result.value)
                    }
                }
            }
        }
    }
}

@AndroidEntryPoint
class MainViewModel @Inject constructor(
    private val projectSerializer: ProjectBundleSerializer,
) : ViewModel() {

    var currentProject by mutableStateOf(Project())
        private set

    fun updateProject(project: Project) {
        currentProject = project.copy(updatedAt = System.currentTimeMillis())
        // Auto-save draft
        viewModelScope.launch {
            projectSerializer.saveLocal(currentProject)
        }
    }

    fun startExport() {
        // TODO: Start export service
    }

    fun importAudio() {
        // TODO: Launch audio picker
    }

    fun importLyrics() {
        // TODO: Launch lyrics picker
    }

    fun createNewProject() {
        currentProject = Project()
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val result = projectSerializer.loadLocal(projectId)
            if (result is ProjectBundleSerializer.Result.Success) {
                result.value?.let { currentProject = it }
            }
        }
    }
}