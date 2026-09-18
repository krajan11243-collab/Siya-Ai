package com.siya.ai.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** Part 6 local TTS facade plus Part 7 streaming reply support. */
class LocalTtsEngine(
    context: Context,
    private val onReady: (Boolean) -> Unit = {},
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val settings = TtsSettingsStore.load(appContext)
    private val ttsStore = TtsModelStore(appContext)
    private val audioQueue = TtsPcmAudioQueue()
    private val neural = SherpaKokoroTtsEngine(ttsStore, audioQueue)
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Siya-TTS-Producer").apply { priority = Thread.NORM_PRIORITY }
    }
    private var neuralJob: Future<*>? = null
    private var streamingGeneration: Long? = null
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    @Volatile private var ready = false
    @Volatile private var pendingSpeech: String? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.setSpeechRate(settings.speechRate)
            tts?.setPitch(settings.pitch)
            val requested = Locale.forLanguageTag(settings.localeTag)
            val result = tts?.setLanguage(requested) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) tts?.setLanguage(Locale.US)
        }
        onReady(ready || neural.isInstalled())
        if (ready) {
            val pending = synchronized(this) { pendingSpeech.also { pendingSpeech = null } }
            if (!pending.isNullOrBlank()) speak(pending)
        }
    }

    fun isReady(): Boolean = ready || neural.isInstalled()
    fun isNeuralModelInstalled(): Boolean = neural.isInstalled()
    fun voiceModelVersion(): String = if (neural.isInstalled()) TtsModelStore.MODEL_VERSION else "Android local TTS fallback"

    @Synchronized
    fun speak(text: String) {
        if (text.isBlank()) return
        stop()
        if (neural.isInstalled()) {
            neuralJob = worker.submit {
                runCatching {
                    val generation = neural.startGeneration(settings.speechRate)
                    chunkText(text).forEach { chunk ->
                        if (!neural.generateChunk(chunk, generation, settings.speechRate)) return@submit
                    }
                }.onFailure {
                    // A present-but-broken neural model must never make Siya silent.
                    // Fall back to Android's device-local TTS for this reply.
                    speakAndroidFallback(text)
                }
            }
            return
        }
        if (!ready) {
            pendingSpeech = text
            return
        }
        chunkText(text).forEachIndexed { index, chunk ->
            tts?.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "siya-${System.nanoTime()}-$index")
        }
    }

    @Synchronized
    fun beginStreaming() {
        stop()
        if (neural.isInstalled()) streamingGeneration = neural.startGeneration(settings.speechRate)
    }

    /** Queues one sentence/phrase into the same cancellable reply generation. */
    @Synchronized
    fun streamChunk(text: String): Boolean {
        if (text.isBlank()) return true
        if (neural.isInstalled()) {
            val generation = streamingGeneration ?: neural.startGeneration(settings.speechRate).also { streamingGeneration = it }
            neuralJob = worker.submit {
                runCatching { neural.generateChunk(text.trim(), generation, settings.speechRate) }
                    .onFailure {
                        // Keep the same text flowing through the local Android TTS path.
                        speakAndroidChunk(text.trim(), TextToSpeech.QUEUE_ADD)
                    }
            }
            return true
        }
        if (!ready) return false
        tts?.speak(text.trim(), TextToSpeech.QUEUE_ADD, null, "siya-stream-${System.nanoTime()}")
        return true
    }

    @Synchronized
    fun endStreaming() {
        streamingGeneration = null
    }

    @Synchronized
    fun stop() {
        neuralJob?.cancel(true)
        neuralJob = null
        streamingGeneration = null
        neural.stopGeneration()
        tts?.stop()
    }

    @Synchronized
    private fun speakAndroidFallback(text: String) {
        if (!ready) {
            pendingSpeech = text
            return
        }
        chunkText(text).forEachIndexed { index, chunk ->
            speakAndroidChunk(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            )
        }
    }

    private fun speakAndroidChunk(text: String, queueMode: Int) {
        if (text.isBlank() || !ready) return
        tts?.speak(
            text,
            queueMode,
            null,
            "siya-fallback-${System.nanoTime()}",
        )
    }

    companion object {
        fun chunkText(text: String): List<String> = text.replace('\n', ' ').split(Regex("(?<=[.!?।])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun close() {
        stop()
        neural.close()
        worker.shutdownNow()
        tts?.shutdown()
        tts = null
        pendingSpeech = null
        ready = false
    }
}
