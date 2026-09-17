package com.siya.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.siya.ai.R
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** Keeps large model downloads alive after the Activity is closed or recreated. */
class ModelDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running = false
    private var downloadJob: Job? = null

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_QWEN -> { cancelDownload(); return START_NOT_STICKY }
            ACTION_DOWNLOAD_QWEN -> startDownloadIfNeeded()
            else -> if (prefs(this).getBoolean(KEY_ACTIVE, false)) startDownloadIfNeeded()
        }
        return START_STICKY
    }

    private fun startDownloadIfNeeded() {
        if (running) return
        val store = LlmModelStore(this)
        if (store.isInstalled()) {
            setProgress(false, store.modelFile.length(), store.modelFile.length(), null)
            stopSelf()
            return
        }
        startForeground(NOTIFICATION_ID, notification("Starting Qwen model download", 0, 0L, 0L))
        running = true
        setProgress(true, existingBytes(store), 0L, null)
        downloadJob = scope.launch {
            try {
                LlmModelInstaller(store).downloadDirect { done, total ->
                    setProgress(true, done, total, null)
                    val percent = if (total > 0L) ((done * 100L) / total).toInt().coerceIn(0, 100) else 0
                    updateNotification("Downloading Qwen • $percent%", percent, done, total)
                }
                setProgress(false, store.modelFile.length(), store.modelFile.length(), null)
                updateNotification("Qwen model ready • SHA-256 verified", 100, store.modelFile.length(), store.modelFile.length(), ongoing = false)
            } catch (t: CancellationException) {
                setProgress(false, existingBytes(store), 0L, "Download cancelled")
                updateNotification("Qwen download stopped", 0, existingBytes(store), 0L, ongoing = false)
            } catch (t: Throwable) {
                setProgress(false, existingBytes(store), 0L, t.message ?: "Download failed")
                updateNotification("Qwen download failed • tap Models to retry", 0, 0L, 0L, ongoing = false)
            } finally {
                running = false
                downloadJob = null
                stopSelf()
            }
        }
    }

    private fun cancelDownload() {
        if (!running) {
            setProgress(false, prefs(this).getLong(KEY_DONE, 0L), prefs(this).getLong(KEY_TOTAL, 0L), "Download cancelled")
            stopSelf()
            return
        }
        downloadJob?.cancel()
    }

    private fun existingBytes(store: LlmModelStore): Long =
        File(store.modelDirectory(), "${LlmModelStore.MODEL_FILE}.download").takeIf { it.isFile }?.length() ?: 0L

    private fun setProgress(active: Boolean, done: Long, total: Long, error: String?) {
        prefs(this).edit().putBoolean(KEY_ACTIVE, active).putLong(KEY_DONE, done).putLong(KEY_TOTAL, total).putString(KEY_ERROR, error).apply()
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Siya Ai model downloads", NotificationManager.IMPORTANCE_LOW).apply { description = "Shows offline AI model download progress" }
        )
    }

    private fun notification(title: String, percent: Int, done: Long, total: Long, ongoing: Boolean = true): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.siya_logo)
            .setContentTitle(title)
            .setContentText(progressText(done, total, percent))
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(if (total > 0L) 100 else 0, percent.coerceIn(0, 100), total <= 0L)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

    private fun updateNotification(title: String, percent: Int, done: Long, total: Long, ongoing: Boolean = true) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(title, percent, done, total, ongoing))
    }

    private fun progressText(done: Long, total: Long, percent: Int): String {
        fun mb(value: Long) = String.format(Locale.US, "%.1f MB", value / 1_048_576.0)
        return if (total > 0L) "${mb(done)} / ${mb(total)} • $percent%" else "${mb(done)} downloaded"
    }

    override fun onDestroy() { downloadJob?.cancel(); scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_DOWNLOAD_QWEN = "com.siya.ai.action.DOWNLOAD_QWEN"
        const val ACTION_CANCEL_QWEN = "com.siya.ai.action.CANCEL_QWEN"
        const val KEY_ACTIVE = "model_download_active"
        const val KEY_DONE = "model_download_done"
        const val KEY_TOTAL = "model_download_total"
        const val KEY_ERROR = "model_download_error"
        private const val CHANNEL_ID = "siya_model_downloads"
        private const val NOTIFICATION_ID = 2001

        fun prefs(context: Context) = context.getSharedPreferences("siya_model_download", Context.MODE_PRIVATE)
        fun startQwen(context: Context) { androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, ModelDownloadService::class.java).setAction(ACTION_DOWNLOAD_QWEN)) }
        fun cancelQwen(context: Context) { context.startService(Intent(context, ModelDownloadService::class.java).setAction(ACTION_CANCEL_QWEN)) }
    }
}
