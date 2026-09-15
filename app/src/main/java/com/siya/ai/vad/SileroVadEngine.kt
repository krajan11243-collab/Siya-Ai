package com.siya.ai.vad

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** Silero VAD v5 ONNX streaming wrapper for 16 kHz mono PCM16. */
class SileroVadEngine(
    private val modelBytes: ByteArray,
    private val config: VadConfig = VadConfig()
) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val state = FloatArray(2 * 128)
    private val context = FloatArray(config.contextSamples)
    private val machine = SpeechStateMachine(config)
    private var closed = false

    init {
        require(modelBytes.isNotEmpty()) { "Silero VAD model is empty" }
        val options = OrtSession.SessionOptions().apply {
            setInterOpNumThreads(1)
            setIntraOpNumThreads(1)
        }
        session = try {
            environment.createSession(modelBytes, options)
        } catch (t: Throwable) {
            options.close()
            throw IllegalArgumentException("Unable to load Silero VAD ONNX model", t)
        } finally {
            options.close()
        }
    }

    @Synchronized
    fun reset() {
        check(!closed) { "VAD engine is closed" }
        java.util.Arrays.fill(state, 0f)
        java.util.Arrays.fill(context, 0f)
        machine.reset()
    }

    @Synchronized
    fun process(pcm16: ShortArray, length: Int = pcm16.size): VadFrameResult {
        check(!closed) { "VAD engine is closed" }
        require(length == config.windowSamples) {
            "Silero v5 requires exactly ${config.windowSamples} samples at ${config.sampleRate} Hz"
        }
        require(length <= pcm16.size)

        val input = FloatArray(config.contextSamples + config.windowSamples)
        for (i in context.indices) input[i] = context[i]
        for (i in 0 until length) input[config.contextSamples + i] = pcm16[i] / 32768f

        val inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), longArrayOf(1, input.size.toLong()))
        val stateTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(state), longArrayOf(2, 1, 128))
        val sampleRateTensor = OnnxTensor.createTensor(
            environment,
            LongBuffer.wrap(longArrayOf(config.sampleRate.toLong())),
            longArrayOf(1)
        )

        try {
            val inputs = mapOf("input" to inputTensor, "state" to stateTensor, "sr" to sampleRateTensor)
            session.run(inputs).use { result ->
                require(result.size() >= 2) { "Silero VAD returned fewer than 2 outputs" }
                val probability = firstFloat(result[0].value)
                flattenState(result[1].value, state)
                System.arraycopy(input, input.size - context.size, context, 0, context.size)
                return machine.accept(probability, length)
            }
        } finally {
            inputTensor.close()
            stateTensor.close()
            sampleRateTensor.close()
        }
    }

    private fun firstFloat(value: Any?): Float = when (value) {
        is FloatArray -> value.firstOrNull() ?: error("Silero output is empty")
        is Array<*> -> firstFloat(value.firstOrNull())
        else -> error("Unexpected Silero output type: ${value?.javaClass}")
    }.coerceIn(0f, 1f)

    private fun flattenState(value: Any?, target: FloatArray) {
        var index = 0
        fun visit(v: Any?) {
            when (v) {
                is FloatArray -> for (x in v) {
                    require(index < target.size) { "Silero state output is too large" }
                    target[index++] = x
                }
                is Array<*> -> for (child in v) visit(child)
                else -> error("Unexpected Silero state output type: ${v?.javaClass}")
            }
        }
        visit(value)
        require(index == target.size) { "Unexpected Silero state size: $index" }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        session.close()
    }
}
