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

/** Foreground host for the local full-duplex VAD -> STT -> streaming LLM -> TTS loop. */
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
    private val streamLock = Any()
    private val streamBuffer = StringBuilder()
    private var firstTtsChunk = true
    @Volatile private var suppressVadUntilMs = 0L

    override fun onCreate() {
        super.onCreate()
        VoiceDiagnostics.log("SERVICE", "CREATED")
        createChannel()
        actionExecutor = AgentActionExecutor(this)
        startForeground(NOTIFICATION_ID, buildNotification("Preparing offline voice models"))
        startVadWorker()
        tts = runCatching { LocalTtsEngine(this) }.getOrNull()
        ensureModelsInitialized()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        VoiceDiagnostics.log("SERVICE", "START_COMMAND", "startId=$startId")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            VoiceDiagnostics.log("MIC", "PERMISSION_MISSING")
            VoiceSessionState.error("Microphone permission is required")
            stopSelf()
            return START_NOT_STICKY
        }
        return try {
            ensureModelsInitialized()
            if (audioEngine?.isRunning() == true) {
                VoiceDiagnostics.log("MIC", "ALREADY_RUNNING")
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
            VoiceDiagnostics.log("MIC", "STARTING")
            if (!engine.start()) {
                VoiceDiagnostics.log("MIC", "START_FAILED", "AudioRecord could not start")
                VoiceSessionState.error("Microphone unavailable on this device")
                updateNotification("Microphone unavailable")
                stopSelf()
                return START_NOT_STICKY
            }
            VoiceDiagnostics.log("MIC", "STARTED", "AudioRecord callback active")
            VoiceSessionState.listening()
            updateNotification("Listening • VAD + Hindi STT + streaming AI + TTS ready")
            START_STICKY
        } catch (error: Throwable) {
            VoiceDiagnostics.log("SERVICE", "START_ERROR", error.message ?: error.javaClass.simpleName)
            val message = error.message ?: error.javaClass.simpleName
            VoiceSessionState.error("Voice engine could not start: $message")
            updateNotification("Voice engine error — check Models")
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun ensureModelsInitialized() {
        VoiceDiagnostics.log("MODELS", "CHECKING")
        initializeLlmIfInstalled()
        initializeSttIfInstalled()
        initializeVadIfInstalled()
        llmExecutor?.warmUp()
    }

    private fun initializeLlmIfInstalled() {
        if (llmExecutor != null) return
        val store = LlmModelStore(this)
        if (!store.isInstalled()) {
            VoiceDiagnostics.log("LLM", "MODEL_MISSING")
            return
        }
        VoiceDiagnostics.log("LLM", "MODEL_FOUND")
        llmExecutor = runCatching {
            LlmExecutor(engineFactory = { LocalLlmEngine(store) }) { result ->
                result.onSuccess { answer ->
                    VoiceDiagnostics.log("LLM", "COMPLETE", "chars=${answer.text.length}")
                    flushLlmTts(answer.text)
                    cancelTurnTimeout()
                    if (answer.text.isNotBlank()) {
                        VoiceSessionState.speaking(answer.text)
                        updateNotification("Siya is speaking")
                    } else {
                        val recovery = "माफ़ कीजिए, मैं अभी जवाब नहीं बना पाई। कृपया फिर से बोलिए।"
                        speakResponse(recovery)
                        VoiceSessionState.speaking(recovery)
                        updateNotification("Empty local AI response • recovery spoken")
                    }
                }.onFailure { error ->
                    VoiceDiagnostics.log("LLM", "ERROR", error.message ?: error.javaClass.simpleName)
                    cancelTurnTimeout()
                    tts?.stop()
                    val recovery = "माफ़ कीजिए, लोकल AI में अभी समस्या आई है। कृपया दोबारा बोलिए।"
                    speakResponse(recovery)
                    VoiceSessionState.error(error.message ?: "Local AI error")
                    updateNotification("Local AI error • recovery spoken")
                }
            }
        }.onFailure {
            VoiceDiagnostics.log("LLM", "INIT_ERROR", it.message ?: it.javaClass.simpleName)
            VoiceSessionState.error("Qwen engine error: ${it.message ?: it.javaClass.simpleName}")
            null
        }.getOrNull()
    }

    private fun initializeSttIfInstalled() {
        if (sttPipeline != null) return
        val store = SttModelStore(this)
        if (!store.isInstalled()) {
            VoiceDiagnostics.log("STT", "MODEL_MISSING")
            return
        }
        VoiceDiagnostics.log("STT", "MODEL_FOUND")
        runCatching {
            val executor = SttExecutor { HindiSttEngine(store) }
            sttExecutor = executor
            sttPipeline = SttPipeline(executor) { result ->
                result.onSuccess { stt ->
                    VoiceDiagnostics.log("STT", "RESULT", "text=${stt.text.take(300)}")
                    handleTranscript(stt.text)
                }
                    .onFailure { error ->
                        VoiceDiagnostics.log("STT", "ERROR", error.message ?: error.javaClass.simpleName)
                        cancelTurnTimeout()
                        VoiceSessionState.error(error.message ?: "Hindi STT error")
                        updateNotification("Hindi STT error")
                    }
            }
        }.onFailure { error ->
            VoiceDiagnostics.log("STT", "INIT_ERROR", error.message ?: error.javaClass.simpleName)
            sttExecutor?.close()
            sttExecutor = null
            sttPipeline = null
            VoiceSessionState.error("Hindi STT unavailable: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun handleTranscript(transcript: String) {
        VoiceDiagnostics.log("STT", "TRANSCRIPT", transcript.take(300))
        if (transcript.isBlank()) {
            cancelTurnTimeout()
            val recovery = "मैं आपकी बात साफ़ नहीं सुन पाई। कृपया फिर से बोलिए।"
            VoiceSessionState.speaking(recovery)
            speakResponse(recovery)
            updateNotification("Speech was not understood • listening again")
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
                speakResponse(prompt)
                updateNotification("Confirmation required")
            } else {
                val result = actionExecutor.execute(routed)
                cancelTurnTimeout()
                speakActionResult(result.message, result.executed)
            }
            return
        }

        val turn = VoiceDiagnostics.newTurn()
        VoiceDiagnostics.log("LLM", "SUBMIT", "turn=$turn chars=${transcript.length}")
        synchronized(streamLock) {
            streamBuffer.setLength(0)
            firstTtsChunk = true
        }
        tts?.beginStreaming()
        VoiceSessionState.thinking(transcript)
        updateNotification("Thinking • streaming local response")
        startTurnTimeout()
        runCatching { llmExecutor?.submit(transcript, onToken = ::onLlmToken) }
            .onFailure { VoiceDiagnostics.log("LLM", "SUBMIT_ERROR", it.message ?: it.javaClass.simpleName) }
    }

    private fun onLlmToken(token: String) {
        if (token.isBlank()) return
        VoiceDiagnostics.log("LLM", "TOKEN", token.take(160))
        val chunks = mutableListOf<String>()
        synchronized(streamLock) {
            streamBuffer.append(token)
            while (true) {
                val end = streamBuffer.indexOfFirst { it == '.' || it == '!' || it == '?' || it == '।' }
                if (end < 0) {
                    if (streamBuffer.length < 80) break
                    val split = streamBuffer.lastIndexOf(' ', 79)
                    if (split <= 0) break
                    chunks += streamBuffer.substring(0, split).trim()
                    streamBuffer.deleteRange(0, split + 1)
                } else {
                    chunks += streamBuffer.substring(0, end + 1).trim()
                    streamBuffer.deleteRange(0, end + 1)
                    while (streamBuffer.isNotEmpty() && streamBuffer.first().isWhitespace()) streamBuffer.deleteCharAt(0)
                }
            }
        }
        chunks.filter { it.isNotBlank() }.forEach { chunk ->
            VoiceDiagnostics.log("TTS", "STREAM_CHUNK", chunk.take(220))
            speakStreamChunk(chunk)
            synchronized(streamLock) { firstTtsChunk = false }
            VoiceSessionState.speaking(chunk)
        }
    }

    private fun flushLlmTts(fullText: String) {
        val remaining: String
        synchronized(streamLock) {
            if (streamBuffer.isNotEmpty()) streamBuffer.append(' ')
            if (fullText.isNotBlank() && streamBuffer.isEmpty()) streamBuffer.append(fullText)
            remaining = streamBuffer.toString().trim()
            streamBuffer.setLength(0)
            firstTtsChunk = false
        }
        if (remaining.isNotBlank()) {
            VoiceDiagnostics.log("TTS", "FLUSH", remaining.take(300))
            speakStreamChunk(remaining)
        }
        tts?.endStreaming()
    }

    private fun speakActionResult(message: String, success: Boolean) {
        VoiceDiagnostics.log("ACTION", if (success) "SUCCESS" else "FAILED", message.take(300))
        if (success) {
            tts?.endStreaming()
            VoiceSessionState.speaking(message)
            speakResponse(message)
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
        if (!store.isInstalled()) {
            VoiceDiagnostics.log("VAD", "MODEL_MISSING")
            return
        }
        VoiceDiagnostics.log("VAD", "MODEL_FOUND")
        runCatching {
            val engine = SileroVadEngine(store.modelFile.absolutePath)
            vad = engine
            vadProcessor = VadPcmProcessor(engine)
            VoiceDiagnostics.log("VAD", "READY")
        }.onFailure { error ->
            VoiceDiagnostics.log("VAD", "INIT_ERROR", error.message ?: error.javaClass.simpleName)
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
            synchronized(streamLock) { streamBuffer.setLength(0); firstTtsChunk = true }
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

    private fun estimateTtsGuardMs(text: String): Long {
        return (1_200L + text.length.coerceAtMost(180) * 110L).coerceAtMost(21_000L)
    }

    private fun guardVadDuringTts(text: String) {
        val until = System.currentTimeMillis() + estimateTtsGuardMs(text)
        if (until > suppressVadUntilMs) suppressVadUntilMs = until
    }

    private fun speakResponse(text: String) {
        if (text.isBlank()) return
        guardVadDuringTts(text)
        tts?.speak(text)
    }

    private fun speakStreamChunk(text: String) {
        if (text.isBlank()) return
        guardVadDuringTts(text)
        tts?.streamChunk(text)
    }

    private fun onPcm(buffer: ShortArray, length: Int) {
        if (length <= 0) return
        VoiceDiagnostics.log("MIC", "PCM", "samples=$length")
        if (System.currentTimeMillis() < suppressVadUntilMs) return
        val handler = vadHandler ?: return
        val copy = buffer.copyOf(length)
        // Keep audio append + VAD event handling on the same serial worker.
        // This prevents a race where SPEECH_END could finish the STT segment
        // before the final PCM frame has been appended.
        handler.post {
            val pipeline = sttPipeline
            val processor = vadProcessor
            if (pipeline == null || processor == null) return@post
            runCatching {
                pipeline.onAudio(copy, copy.size)
                processor.accept(copy, copy.size) { result ->
                    pipeline.onVad(result)
                    when (result.event?.type) {
                        VadEventType.SPEECH_START -> {
                            VoiceDiagnostics.log("VAD", "SPEECH_START")
                            VoiceSessionState.interrupting()
                            cancelTurnTimeout()
                            llmExecutor?.cancelCurrent()
                            sttPipeline?.cancelPending()
                            tts?.stop()
                            synchronized(streamLock) { streamBuffer.setLength(0); firstTtsChunk = true }
                            VoiceSessionState.listening()
                            updateNotification("Listening • previous turn cancelled")
                        }
                        VadEventType.SPEECH_END -> {
                            VoiceDiagnostics.log("VAD", "SPEECH_END")
                            VoiceSessionState.transcribing()
                            updateNotification("Transcribing Hindi…")
                        }
                        null -> if (result.state == VadState.ENDING) Unit
                    }
                }
            }.onFailure {
                VoiceDiagnostics.log("VAD", "ERROR", it.message ?: it.javaClass.simpleName)
                cancelTurnTimeout()
                VoiceSessionState.error(it.message ?: "VAD error")
                updateNotification("VAD error")
            }
        }
    }

    override fun onDestroy() {
        VoiceDiagnostics.log("SERVICE", "DESTROYED")
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
