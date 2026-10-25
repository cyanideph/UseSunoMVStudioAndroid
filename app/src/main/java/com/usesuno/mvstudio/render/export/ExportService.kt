package com.usesuno.mvstudio.render.export

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import androidx.annotation.RequiresApi
import com.usesuno.mvstudio.domain.model.Project
import com.usesuno.mvstudio.domain.audio.AudioAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service for background video export
 */
@RequiresApi(Build.VERSION_CODES.O)
class ExportService : Service() {

    private var exportJob: kotlinx.coroutines.Job? = null
    private val cancellationToken = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val project = intent?.getParcelableExtra<Project>("project")
        val audioAnalysis = intent?.getParcelableExtra<AudioAnalysis>("audioAnalysis")
        val audioFilePath = intent?.getStringExtra("audioFilePath")
        val outputPath = intent?.getStringExtra("outputPath")
        val notificationId = intent?.getIntExtra("notificationId", 1) ?: 1

        if (project != null && audioAnalysis != null && audioFilePath != null && outputPath != null) {
            startForegroundExport(notificationId, project, audioAnalysis, audioFilePath, File(outputPath))
        }

        return START_NOT_STICKY
    }

    private fun startForegroundExport(
        notificationId: Int,
        project: Project,
        audioAnalysis: AudioAnalysis,
        audioFilePath: String,
        outputFile: File,
    ) {
        val notification = createProgressNotification(notificationId, 0.0)
        startForeground(notificationId, notification)

        exportJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val exporter = VideoExporter(
                    project = project,
                    audioAnalysis = audioAnalysis,
                    audioFilePath = audioFilePath,
                    outputFile = outputFile,
                    progressCallback = { progress ->
                        updateNotification(notificationId, progress)
                    },
                    cancellationToken = cancellationToken,
                )

                val result = exporter.export()

                stopForeground(true)
                when (result) {
                    is Result.Success -> {
                        showCompletionNotification(notificationId, outputFile)
                    }
                    is Result.Failure -> {
                        showErrorNotification(notificationId, result.exception.message)
                    }
                }
            } catch (e: Exception) {
                stopForeground(true)
                showErrorNotification(notificationId, e.message)
            }
        }
    }

    private fun createNotificationChannel() {
        // Create notification channel for Android O+
    }

    private fun createProgressNotification(id: Int, progress: Double): android.app.Notification {
        return android.app.Notification.Builder(this, "export_channel")
            .setContentTitle("Exporting Video")
            .setContentText("${(progress * 100).toInt()}%")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(id: Int, progress: Double) {
        val notification = createProgressNotification(id, progress)
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(id, notification)
    }

    private fun showCompletionNotification(id: Int, file: File) {
        val notification = android.app.Notification.Builder(this, "export_channel")
            .setContentTitle("Export Complete")
            .setContentText("Saved to ${file.name}")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(id, notification)
    }

    private fun showErrorNotification(id: Int, message: String?) {
        val notification = android.app.Notification.Builder(this, "export_channel")
            .setContentTitle("Export Failed")
            .setContentText(message ?: "Unknown error")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(id, notification)
    }

    override fun onDestroy() {
        cancellationToken.set(true)
        exportJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}