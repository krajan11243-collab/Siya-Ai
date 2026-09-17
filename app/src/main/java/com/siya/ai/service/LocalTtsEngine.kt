package com.siya.ai.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Part 6 supported local TTS adapter.
 * Uses the device-local Android TTS engine today; its lifecycle and text chunking
 * are isolated so a Kokoro/Sherpa neural backend can plug in without changing the voice service.
 */
class LocalTtsEngine(
    context: Context,
    private val onReady: (Boolean) -> Unit = {},
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val settings = TtsSettingsStore.load(appContext)
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
        onReady(ready)
    }

    @Synchronized
    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        chunkText(text).forEachIndexed { index, chunk ->
            tts?.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "siya-${System.nanoTime()}-$index"
            )
        }
    }

    /** Pure chunking helper, kept public for Part 6 acceptance tests. */
    companion object {
        fun chunkText(text: String): List<String> =
            text.replace('\n', ' ')
                .split(Regex("(?<=[.!?।])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
    }

    @Synchronized
    fun stop() {
        tts?.stop()
    }

    fun isReady(): Boolean = ready

    fun close() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
