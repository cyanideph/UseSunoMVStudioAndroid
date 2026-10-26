package com.usesuno.mvstudio.render.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.usesuno.mvstudio.data.db.AppDatabase
import com.usesuno.mvstudio.domain.audio.AudioAnalyzer
import com.usesuno.mvstudio.domain.audio.MediaCodecAudioDecoder
import com.usesuno.mvstudio.domain.model.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import android.widget.Toast

class ExportService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cancellationToken = AtomicBoolean(false)
    private var exportJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val project = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_PROJECT, Project::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_PROJECT)
        }
        val outputPath = intent?.getStringExtra(EXTRA_OUTPUT_PATH)
        if (project == null || project.audioId == null || outputPath == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        cancellationToken.set(false)
        startForeground(NOTIFICATION_ID, notification("Preparing audio…", 0, true))
        exportJob?.cancel()
        exportJob = serviceScope.launch {
            val audioFile = File(cacheDir, "export-${project.id}.audio")
            try {
                val asset = AppDatabase.getDatabase(this@ExportService).assetDao().getById(project.audioId)
                    ?: throw IllegalArgumentException("Project audio asset was not found")
                audioFile.writeBytes(asset.data)
                val decoded = MediaCodecAudioDecoder().decodeAudio(
                    this@ExportService,
                    android.net.Uri.fromFile(audioFile),
                )
                val analysis = AudioAnalyzer().analyze(decoded)
                val output = File(outputPath)
                val result = VideoExporter(
                    project = project,
                    audioAnalysis = analysis,
                    audioFilePath = audioFile.absolutePath,
                    outputFile = output,
                    progressCallback = { progress ->
                        val percent = (progress * 100).toInt().coerceIn(0, 100)
                        getSystemService(NotificationManager::class.java)
                            .notify(NOTIFICATION_ID, notification("Exporting video…", percent, true))
                    },
                    cancellationToken = cancellationToken,
                ).export()

                when (result) {
                    is com.usesuno.mvstudio.render.export.Result.Success -> notifyFinished("Export complete: ${output.name}", false)
                    is com.usesuno.mvstudio.render.export.Result.Failure -> {
                        val error = result.exception
                        if (error is InterruptedException) notifyFinished("Export cancelled", false)
                        else notifyFinished("Export failed: ${error.message ?: "Unknown error"}", false)
                    }
                }
            } catch (cancelled: CancellationException) {
                cancellationToken.set(true)
                notifyFinished("Export cancelled", false)
            } catch (error: Exception) {
                File(outputPath).delete()
                notifyFinished("Export failed: ${error.message ?: "Unknown error"}", false)
            } finally {
                audioFile.delete()
                if (project.exportFormat != com.usesuno.mvstudio.domain.model.ExportFormat.MP4) {
                    Toast.makeText(this@ExportService, "WebM export is not supported yet", Toast.LENGTH_LONG).show()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancellationToken.set(true)
        exportJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Video export", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(text: String, progress: Int, ongoing: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UseSuno MV Studio")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setProgress(100, progress, false)
            .setOngoing(ongoing)
            .build()

    private fun notifyFinished(text: String, ongoing: Boolean) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            notification(text, 100, ongoing),
        )
    }

    companion object {
        const val EXTRA_PROJECT = "project"
        const val EXTRA_OUTPUT_PATH = "outputPath"
        private const val CHANNEL_ID = "export_channel"
        private const val NOTIFICATION_ID = 4102
    }
}
