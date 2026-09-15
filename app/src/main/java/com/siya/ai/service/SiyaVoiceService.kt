package com.siya.ai.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.siya.ai.R
import com.siya.ai.audio.AudioEngine
import com.siya.ai.vad.SileroVadEngine
import com.siya.ai.vad.VadModelStore
import com.siya.ai.vad.VadPcmProcessor
import com.siya.ai.vad.VadState

/** Foreground microphone host for the local audio + optional VAD pipeline. */
class SiyaVoiceService : Service() {
    companion object {
        private const val CHANNEL_ID = "siya_voice"
        private const val NOTIFICATION_ID = 1001
    }

    private var audioEngine: AudioEngine? = null
    private var vad: SileroVadEngine? = null
    private var vadProcessor: VadPcmProcessor? = null
    private var vadThread: HandlerThread? = null
    private var vadHandler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Audio engine starting"))
        startVadWorker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (audioEngine?.isRunning() == true) return START_STICKY

        val engine = audioEngine ?: AudioEngine(this, onPcm = ::onPcm).also { audioEngine = it }
        if (!engine.start()) {
            updateNotification("Microphone unavailable")
            stopSelf()
            return START_NOT_STICKY
        }
        updateNotification(if (vad != null) "Microphone + VAD active" else "Microphone active • VAD loading/not installed")
        return START_STICKY
    }

    private fun startVadWorker() {
        val thread = HandlerThread("Siya-VAD", Thread.NORM_PRIORITY).apply { start() }
        vadThread = thread
        val handler = Handler(thread.looper)
        vadHandler = handler
        handler.post { initializeVadIfInstalled() }
    }

    private fun initializeVadIfInstalled() {
        val store = VadModelStore(this)
        if (!store.isInstalled()) return
        runCatching {
            val engine = SileroVadEngine(store.readBytes())
            vad = engine
            vadProcessor = VadPcmProcessor(engine)
        }.onFailure {
            vad?.close()
            vad = null
            vadProcessor = null
        }
    }

    private fun onPcm(buffer: ShortArray, length: Int) {
        val handler = vadHandler ?: return
        if (length <= 0) return
        // AudioInput reuses its capture buffer, so never hand that mutable array to another thread.
        val copy = buffer.copyOf(length)
        handler.post {
            val processor = vadProcessor ?: return@post
            runCatching {
                processor.accept(copy, copy.size) { result ->
                    when (result.event?.type) {
                        com.siya.ai.vad.VadEventType.SPEECH_START -> updateNotification("Speech detected • Siya Ai ready")
                        com.siya.ai.vad.VadEventType.SPEECH_END -> updateNotification("Listening idle • waiting for speech")
                        null -> if (result.state == VadState.ENDING) Unit
                    }
                }
            }.onFailure {
                // A malformed model must not terminate the microphone service.
                updateNotification("VAD error • microphone still active")
            }
        }
    }

    override fun onDestroy() {
        audioEngine?.release()
        audioEngine = null
        vadHandler?.removeCallbacksAndMessages(null)
        vadThread?.quitSafely()
        vadHandler = null
        vadThread = null
        vadProcessor = null
        vad?.close()
        vad = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Siya Ai Voice", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Siya Ai")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }
}
