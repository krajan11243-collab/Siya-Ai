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
    }

    /** Starts one cancellable generation used by all sentence chunks in the reply. */
    fun startGeneration(speed: Float = 1.0f): Long {
        check(!closed.get()) { "TTS engine is closed" }
        ensureLoaded()
        return audioQueue.newGeneration()
    }

    /** Generates one sentence and appends its PCM to the current generation. */
    fun generateChunk(text: String, generation: Long, speed: Float = 1.0f): Boolean {
        if (text.isBlank() || closed.get()) return true
        val localTts = tts ?: return false
        val config = GenerationConfig(
            speed = speed.coerceIn(0.5f, 2.0f),
            silenceScale = 0.2f,
        )
        localTts.generateWithConfigAndCallback(text.trim(), config) { samples ->
            if (closed.get() || audioQueue.currentGeneration() != generation) 0
            else if (audioQueue.enqueue(samples, localTts.sampleRate(), generation)) 1
            else 0
        }
        return audioQueue.currentGeneration() == generation
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
