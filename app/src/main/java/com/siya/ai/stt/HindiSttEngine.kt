package com.siya.ai.stt

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-quality offline Hindi ASR engine using sherpa-onnx + IndicConformer CTC.
 * Inference is fully local once the model files have been installed.
 */
class HindiSttEngine(
    private val modelStore: SttModelStore,
    private val config: SttConfig = SttConfig(),
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val recognizer: OfflineRecognizer

    init {
        modelStore.validate().getOrThrow()
        val modelConfig = OfflineModelConfig(
            nemo = OfflineNemoEncDecCtcModelConfig(model = modelStore.modelPath()),
            tokens = modelStore.tokensPath(),
            numThreads = config.numThreads,
            debug = false,
            provider = config.provider,
        )
        recognizer = OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(
                    sampleRate = config.sampleRate,
                    featureDim = config.featureDim,
                ),
                modelConfig = modelConfig,
                decodingMethod = config.decodingMethod,
            )
        )
    }

    fun transcribe(pcm16: ShortArray, length: Int = pcm16.size): SttResult {
        check(!closed.get()) { "HindiSttEngine is closed" }
        require(length in 1..pcm16.size) { "Invalid PCM length" }

        val samples = FloatArray(length)
        for (i in 0 until length) samples[i] = pcm16[i] / 32768f

        val stream: OfflineStream = recognizer.createStream()
        return try {
            stream.acceptWaveform(samples, config.sampleRate)
            stream.inputFinished()
            recognizer.decode(stream)
            val result = recognizer.getResult(stream)
            SttResult(
                text = result.text.trim(),
                tokens = result.tokens.toList(),
                timestampsSeconds = result.timestamps.toList(),
                durationMs = length * 1000L / config.sampleRate,
                isFinal = true,
            )
        } finally {
            stream.release()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) recognizer.release()
    }
}
