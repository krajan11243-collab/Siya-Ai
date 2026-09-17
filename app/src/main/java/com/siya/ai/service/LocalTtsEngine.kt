package com.siya.ai.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Part 6 local TTS facade.
 *
 * Preferred backend: installed Sherpa-ONNX Kokoro -> Float PCM -> AudioTrack queue.
 * Fallback: Android's device-local TTS engine, still offline when its language data is local.
 */
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
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.setSpeechRate(settings.speechRate)
            tts?.setPitch(settings.pitch)
            val requested = Locale.forLanguageTag(settings.localeTag)
            val result = tts?.setLanguage(requested) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        }
        onReady(ready || neural.isInstalled())
    }

    fun isReady(): Boolean = ready || neural.isInstalled()

    fun isNeuralModelInstalled(): Boolean = neural.isInstalled()

    fun voiceModelVersion(): String = if (neural.isInstalled()) {
        TtsModelStore.MODEL_VERSION
    } else {
        "Android local TTS fallback"
    }

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
                }.onFailure { onReady(false) }
            }
            return
        }
        if (!ready) return
        chunkText(text).forEachIndexed { index, chunk ->
            tts?.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "siya-${System.nanoTime()}-$index"
            )
        }
    }

    @Synchronized
    fun stop() {
        neuralJob?.cancel(true)
        neuralJob = null
        neural.stopGeneration()
        tts?.stop()
    }

    /** Pure chunking helper, kept public for Part 6 acceptance tests. */
    companion object {
        fun chunkText(text: String): List<String> =
            text.replace('\n', ' ')
                .split(Regex("(?<=[.!?।])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
    }

    fun close() {
        stop()
        neural.close()
        worker.shutdownNow()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
