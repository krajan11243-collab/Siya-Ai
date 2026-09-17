package com.siya.ai.service

import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local neural TTS adapter. Sherpa-ONNX generates Float PCM and streams it directly
 * into the cancellable AudioTrack queue; no cloud service is involved.
 */
class SherpaKokoroTtsEngine(
    private val store: TtsModelStore,
    private val audioQueue: TtsPcmAudioQueue,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private var tts: OfflineTts? = null

    fun isInstalled(): Boolean = store.isInstalled()

    fun ensureLoaded() {
        check(!closed.get()) { "TTS engine is closed" }
        if (tts != null) return
        store.validate().getOrThrow()
        val dir = store.directory()
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = store.modelPath().absolutePath,
                    voices = store.voicesPath().absolutePath,
                    tokens = store.tokensPath().absolutePath,
                    dataDir = store.dataDir().absolutePath,
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 1,
        )
        tts = OfflineTts(config = config)
        check(dir.exists()) { "TTS model directory disappeared" }
    }

    /**
     * Returns the generation id used by the producer. Calling stopGeneration() invalidates
     * the callback so a sentence that is currently being synthesized cannot enqueue stale audio.
     */
    fun speak(text: String, speed: Float = 1.0f): Long {
        if (text.isBlank() || closed.get()) return audioQueue.currentGeneration()
        ensureLoaded()
        val generation = audioQueue.newGeneration()
        val generationConfig = GenerationConfig(speed = speed.coerceIn(0.5f, 2.0f), silenceScale = 0.2f)
        tts!!.generateWithConfigAndCallback(text.trim(), generationConfig) { samples ->
            if (closed.get() || audioQueue.currentGeneration() != generation) 0
            else if (audioQueue.enqueue(samples, tts!!.sampleRate(), generation)) 1
            else 0
        }
        return generation
    }

    fun stopGeneration() {
        audioQueue.newGeneration()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        audioQueue.stop()
        tts?.release()
        tts = null
    }
}
