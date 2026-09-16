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
import com.siya.ai.audio.AudioEngine
import com.siya.ai.llm.LlmExecutor
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.stt.HindiSttEngine
import com.siya.ai.stt.SttExecutor
import com.siya.ai.stt.SttModelStore
import com.siya.ai.stt.SttPipeline
import com.siya.ai.vad.SileroVadEngine
import com.siya.ai.vad.VadModelStore
import com.siya.ai.vad.VadPcmProcessor
import com.siya.ai.vad.VadState

/** Foreground microphone host for local audio + VAD + Hindi STT + local LLM. */
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
    private var sttExecutor: SttExecutor? = null
    private var sttPipeline: SttPipeline? = null
    private var llmExecutor: LlmExecutor? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Preparing offline voice models"))
        startVadWorker()
        ensureModelsInitialized()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            VoiceSessionState.error("Microphone permission is required")
            stopSelf()
            return START_NOT_STICKY
        }

        // Models can be installed while this service instance is alive. Re-check them on every start.
        ensureModelsInitialized()
        if (audioEngine?.isRunning() == true) {
            VoiceSessionState.listening()
            return START_STICKY
        }

        if (vad == null || sttPipeline == null || llmExecutor == null) {
            val missing = buildList {
                if (vad == null) add("Silero VAD")
                if (sttPipeline == null) add("Hindi STT")
                if (llmExecutor == null) add("Qwen LLM")
            }.joinToString(", ")
            VoiceSessionState.error("Install required offline models: $missing")
            updateNotification("Missing offline model: $missing")
            return START_NOT_STICKY
        }

        val engine = audioEngine ?: AudioEngine(this, onPcm = ::onPcm).also { audioEngine = it }
        if (!engine.start()) {
            VoiceSessionState.error("Microphone unavailable on this device")
            updateNotification("Microphone unavailable")
            stopSelf()
            return START_NOT_STICKY
        }
        VoiceSessionState.listening()
        updateNotification("Listening • VAD + Hindi STT + local AI ready")
        return START_STICKY
    }

    private fun ensureModelsInitialized() {
        initializeLlmIfInstalled()
        initializeSttIfInstalled()
        initializeVadIfInstalled()
        llmExecutor?.warmUp()
    }

    private fun initializeLlmIfInstalled() {
        if (llmExecutor != null) return
        val store = LlmModelStore(this)
        if (!store.isInstalled()) return
        llmExecutor = LlmExecutor(engineFactory = { LocalLlmEngine(store) }) { result ->
            result.onSuccess { answer ->
                if (answer.text.isNotBlank()) {
                    VoiceSessionState.ready(answer.text)
                    updateNotification("Siya: ${answer.text.take(120)}")
                } else {
                    VoiceSessionState.error("Siya returned an empty response")
                    updateNotification("Siya returned empty text")
                }
            }.onFailure { error ->
                VoiceSessionState.error(error.message ?: "Local AI error")
                updateNotification("Local AI error")
            }
        }
    }

    private fun initializeSttIfInstalled() {
        if (sttPipeline != null) return
        val store = SttModelStore(this)
        if (!store.isInstalled()) return
        runCatching {
            val executor = SttExecutor { HindiSttEngine(store) }
            sttExecutor = executor
            sttPipeline = SttPipeline(executor) { result ->
                result.onSuccess { stt ->
                    if (stt.text.isNotBlank()) {
                        VoiceSessionState.thinking(stt.text)
                        updateNotification("Thinking about: ${stt.text.take(70)}")
                        llmExecutor?.submit(stt.text)
                    } else {
                        VoiceSessionState.error("I could not understand the speech")
                        updateNotification("Speech was not understood")
                    }
                }.onFailure { error ->
                    VoiceSessionState.error(error.message ?: "Hindi STT error")
                    updateNotification("Hindi STT error")
                }
            }
        }.onFailure {
            sttExecutor?.close()
            sttExecutor = null
            sttPipeline = null
        }
    }

    private fun initializeVadIfInstalled() {
        if (vad != null && vadProcessor != null) return
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

    private fun startVadWorker() {
        if (vadThread != null) return
        val thread = HandlerThread("Siya-VAD", Thread.NORM_PRIORITY).apply { start() }
        vadThread = thread
        vadHandler = Handler(thread.looper)
    }

    private fun onPcm(buffer: ShortArray, length: Int) {
        if (length <= 0) return
        // STT receives the raw PCM first so its 320 ms pre-roll includes the VAD trigger frame.
        sttPipeline?.onAudio(buffer, length)
        val handler = vadHandler ?: return
        val copy = buffer.copyOf(length)
        handler.post {
            val processor = vadProcessor ?: return@post
            runCatching {
                processor.accept(copy, copy.size) { result ->
                    sttPipeline?.onVad(result)
                    when (result.event?.type) {
                        VadEventType.SPEECH_START -> {
                            VoiceSessionState.listening()
                            updateNotification("Speech detected")
                        }
                        VadEventType.SPEECH_END -> {
                            VoiceSessionState.transcribing()
                            updateNotification("Transcribing Hindi…")
                        }
                        null -> if (result.state == VadState.ENDING) Unit
                    }
                }
            }.onFailure {
                VoiceSessionState.error(it.message ?: "VAD error")
                updateNotification("VAD error")
            }
        }
    }

    override fun onDestroy() {
        sttPipeline?.close(); sttPipeline = null
        sttExecutor?.close(); sttExecutor = null
        llmExecutor?.close(); llmExecutor = null
        audioEngine?.release(); audioEngine = null
        vadHandler?.removeCallbacksAndMessages(null)
        vadThread?.quitSafely(); vadHandler = null; vadThread = null
        vadProcessor = null
        vad?.close(); vad = null
        VoiceSessionState.stopped()
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

    private fun buildNotification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Siya Ai")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setOngoing(true)
        .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }
}