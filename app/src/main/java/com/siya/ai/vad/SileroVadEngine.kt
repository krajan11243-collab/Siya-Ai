package com.siya.ai.vad

import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig

/** Silero VAD wrapper backed by the already bundled Sherpa-ONNX runtime. */
class SileroVadEngine(
    private val modelPath: String,
    private val config: VadConfig = VadConfig(),
) : AutoCloseable {
    private val machine = SpeechStateMachine(config)
    private val vad: Vad
    private var closed = false

    init {
        require(modelPath.isNotBlank()) { "Silero VAD model path is empty" }
        val modelConfig = VadModelConfig(
            sileroVadModelConfig = SileroVadModelConfig(
                model = modelPath,
                threshold = config.threshold,
                minSilenceDuration = config.minSilenceDurationMs / 1000f,
                minSpeechDuration = config.minSpeechDurationMs / 1000f,
                windowSize = config.windowSamples,
            ),
            sampleRate = config.sampleRate,
            numThreads = 1,
            provider = "cpu",
        )
        vad = Vad(config = modelConfig)
    }

    @Synchronized
    fun reset() {
        check(!closed) { "VAD engine is closed" }
        vad.reset()
        machine.reset()
    }

    @Synchronized
    fun process(pcm16: ShortArray, length: Int = pcm16.size): VadFrameResult {
        check(!closed) { "VAD engine is closed" }
        require(length == config.windowSamples) {
            "Silero VAD requires exactly ${config.windowSamples} samples at ${config.sampleRate} Hz"
        }
        require(length <= pcm16.size)
        val samples = FloatArray(length) { pcm16[it] / 32768f }
        val probability = vad.compute(samples)
        return machine.accept(probability, length)
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        vad.release()
    }
}
