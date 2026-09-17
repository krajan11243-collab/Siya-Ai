package com.siya.ai.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Part 6 TTS adapter.
 * Uses the Android device TTS engine as the first local adapter; the interface is
 * intentionally isolated so a Kokoro/Sherpa local backend can replace it later.
 */
class LocalTtsEngine(
    context: Context,
    private val onReady: (Boolean) -> Unit = {},
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val result = tts?.setLanguage(Locale.forLanguageTag("hi-IN")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
        }
        onReady(ready)
    }

    @Synchronized
    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        val chunks = text.replace("\n", " ").split(Regex("(?<=[.!?।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        chunks.forEachIndexed { index, chunk ->
            tts?.speak(
                chunk,
                if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "siya-$index"
            )
        }
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
