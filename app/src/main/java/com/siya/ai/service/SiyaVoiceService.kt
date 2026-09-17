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
import com.siya.ai.agent.AgentActionExecutor
import com.siya.ai.agent.AgentIntentRouter
import com.siya.ai.agent.RoutedIntent
import com.siya.ai.audio.AudioEngine
import com.siya.ai.llm.LlmExecutor
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.stt.HindiSttEngine
import com.siya.ai.stt.SttExecutor
import com.siya.ai.stt.SttModelStore
import com.siya.ai.stt.SttPipeline
import com.siya.ai.vad.SileroVadEngine
import com.siya.ai.vad.VadEventType
import com.siya.ai.vad.VadModelStore
import com.siya.ai.vad.VadPcmProcessor
import com.siya.ai.vad.VadState

/** Foreground host for the local full-duplex VAD -> STT -> action/LLM -> TTS loop. */
class SiyaVoiceService : Service() {
    companion object {
        private const val CHANNEL_ID = "siya_voice"
        private const val NOTIFICATION_ID = 1001
        private const val TURN_TIMEOUT_MS = 45_000L
    }

    private var audioEngine: AudioEngine? = null
    private var vad: SileroVadEngine? = null
    private var vadProcessor: VadPcmProcessor? = null
    private var vadThread: HandlerThread? = null
    private var vadHandler: Handler? = null
    private var sttExecutor: SttExecutor? = null
    private var sttPipeline: SttPipeline? = null
    private var llmExecutor: LlmExecutor? = null
    private var tts: LocalTtsEngine? = null
    private var turnTimeout: Runnable? = null
    private lateinit var actionExecutor: AgentActionExecutor
    private var pendingConfirmation: RoutedIntent? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        actionExecutor = AgentActionExecutor(this)
        startForeground(NOTIFICATION_ID, buildNotification("Preparing offline voice models"))
        startVadWorker()
        tts = runCatching { LocalTtsEngine(this) }.getOrNull()
        ensureModelsInitialized()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            VoiceSessionState.error("Microphone permission is required")
            stopSelf()
            return START_NOT_STICKY
        }
        return try {
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
            updateNotification("Listening • VAD + Hindi STT + local AI + TTS ready")
            START_STICKY
        } catch (error: Throwable) {
            val message = error.message ?: error.javaClass.simpleName
            VoiceSessionState.error("Voice engine could not start: $message")
            updateNotification("Voice engine error — check Models")
            stopSelf()
            START_NOT_STICKY
        }
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
        llmExecutor = runCatching {
            LlmExecutor(engineFactory = { LocalLlmEngine(store) }) { result ->
                result.onSuccess { answer ->
                    if (answer.text.isNotBlank()) {
                        cancelTurnTimeout()
                        VoiceSessionState.speaking(answer.text)
                        tts?.speak(answer.text)
                        updateNotification("Siya is speaking")
                    } else {
                        cancelTurnTimeout()
                        VoiceSessionState.error("Siya returned an empty response")
                        updateNotification("Siya returned empty text")
                    }
                }.onFailure { error ->
                    cancelTurnTimeout()
                    VoiceSessionState.error(error.message ?: "Local AI error")
                    updateNotification("Local AI error")
                }
            }
        }.onFailure {
            VoiceSessionState.error("Qwen engine error: ${it.message ?: it.javaClass.simpleName}")
            null
        }.getOrNull()
    }

    private fun initializeSttIfInstalled() {
        if (sttPipeline != null) return
        val store = SttModelStore(this)
        if (!store.isInstalled()) return
        runCatching {
            val executor = SttExecutor { HindiSttEngine(store) }
            sttExecutor = executor
            sttPipeline = SttPipeline(executor) { result ->
                result.onSuccess { stt -> handleTranscript(stt.text) }
                    .onFailure { error ->
                        cancelTurnTimeout()
                        VoiceSessionState.error(error.message ?: "Hindi STT error")
                        updateNotification("Hindi STT error")
                    }
            }
        }.onFailure { error ->
            sttExecutor?.close()
            sttExecutor = null
            sttPipeline = null
            VoiceSessionState.error("Hindi STT unavailable: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun handleTranscript(transcript: String) {
        if (transcript.isBlank()) {
            cancelTurnTimeout()
            VoiceSessionState.error("I could not understand the speech")
            updateNotification("Speech was not understood")
            return
        }
        val normalized = transcript.trim().lowercase()
        pendingConfirmation?.let { pending ->
            when {
                normalized in setOf("yes", "haan", "ha", "हाँ", "करो", "कर दीजिए") -> {
                    pendingConfirmation = null
                    val result = actionExecutor.execute(pending, confirmed = true)
                    cancelTurnTimeout()
                    speakActionResult(result.message, result.executed)
                    return
                }
                normalized in setOf("no", "nahi", "nahin", "नहीं", "मत करो", "cancel", "रद्द") -> {
                    pendingConfirmation = null
                    cancelTurnTimeout()
                    speakActionResult("ठीक है, action cancel कर दिया।", true)
                    return
                }
            }
        }

        val routed = AgentIntentRouter.route(transcript)
        if (routed != null) {
            if (routed.confirmationRequired) {
                pendingConfirmation = routed
                cancelTurnTimeout()
                val prompt = "क्या मैं यह action करूँ? पुष्टि के लिए हाँ बोलें, मना करने के लिए नहीं।"
                VoiceSessionState.speaking(prompt)
                tts?.speak(prompt)
                updateNotification("Confirmation required")
            } else {
                val result = actionExecutor.execute(routed)
                cancelTurnTimeout()
                speakActionResult(result.message, result.executed)
            }
            return
        }

        VoiceSessionState.thinking(transcript)
        updateNotification("Thinking about: ${transcript.take(70)}")
        startTurnTimeout()
        llmExecutor?.submit(transcript)
    }

    private fun speakActionResult(message: String, success: Boolean) {
        if (success) {
            VoiceSessionState.speaking(message)
            tts?.speak(message)
            updateNotification(message)
        } else {
            VoiceSessionState.error(message)
            updateNotification(message)
        }
    }

    private fun initializeVadIfInstalled() {
        if (vad != null && vadProcessor != null) return
        val store = VadModelStore(this)
        store.restoreFromShared()
        if (!store.isInstalled()) return
        runCatching {
            val engine = SileroVadEngine(store.modelFile.absolutePath)
            vad = engine
            vadProcessor = VadPcmProcessor(engine)
        }.onFailure { error ->
            vad?.close()
            vad = null
            vadProcessor = null
            VoiceSessionState.error("Silero VAD unavailable: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun startVadWorker() {
        if (vadThread != null) return
        val thread = HandlerThread("Siya-VAD", Thread.NORM_PRIORITY).apply { start() }
        vadThread = thread
        vadHandler = Handler(thread.looper)
    }

    private fun startTurnTimeout() {
        val handler = vadHandler ?: return
        cancelTurnTimeout()
        val timeout = Runnable {
            llmExecutor?.cancelCurrent()
            tts?.stop()
            VoiceSessionState.error("This turn timed out. Please try again.")
            updateNotification("Turn timeout — listening again")
            VoiceSessionState.listening()
        }
        turnTimeout = timeout
        handler.postDelayed(timeout, TURN_TIMEOUT_MS)
    }

    private fun cancelTurnTimeout() {
        val handler = vadHandler ?: return
        turnTimeout?.let(handler::removeCallbacks)
        turnTimeout = null
    }

    private fun onPcm(buffer: ShortArray, length: Int) {
        if (length <= 0) return
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
                            VoiceSessionState.interrupting()
                            cancelTurnTimeout()
                            llmExecutor?.cancelCurrent()
                            tts?.stop()
                            VoiceSessionState.listening()
                            updateNotification("Speech detected • previous turn cancelled")
                        }
                        VadEventType.SPEECH_END -> {
                            VoiceSessionState.transcribing()
                            updateNotification("Transcribing Hindi…")
                        }
                        null -> if (result.state == VadState.ENDING) Unit
                    }
                }
            }.onFailure {
                cancelTurnTimeout()
                VoiceSessionState.error(it.message ?: "VAD error")
                updateNotification("VAD error")
            }
        }
    }

    override fun onDestroy() {
        cancelTurnTimeout()
        pendingConfirmation = null
        tts?.stop()
        tts?.close()
        tts = null
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
