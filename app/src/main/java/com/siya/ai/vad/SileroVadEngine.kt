package com.siya.ai.vad

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/**
 * Silero VAD v5 ONNX streaming wrapper.
 * v5 uses 512 samples at 16 kHz plus 64 samples of previous context.
 */
class SileroVadEngine(
    private val modelBytes: ByteArray,
    private val config: VadConfig = VadConfig()
) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = environment.createSession(modelBytes, OrtSession.SessionOptions())
    private val state = FloatArray(2 * 128)
    private val context = FloatArray(config.contextSamples)
    private val machine = SpeechStateMachine(config)

    init {
        require(modelBytes.isNotEmpty()) { "Silero VAD model is empty" }
    }

    fun reset() {
        java.util.Arrays.fill(state, 0f)
        java.util.Arrays.fill(context, 0f)
        machine.reset()
    }

    fun process(pcm16: ShortArray, length: Int = pcm16.size): VadFrameResult {
        require(length == config.windowSamples) { "Silero v5 requires exactly 512 samples at 16 kHz" }
        require(length <= pcm16.size)

        val input = FloatArray(config.contextSamples + config.windowSamples)
        for (i in context.indices) input[i] = context[i]
        for (i in 0 until length) input[config.contextSamples + i] = pcm16[i] / 32768f

        val inputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(1, input.size.toLong())
        )
        val stateTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(state),
            longArrayOf(2, 1, 128)
        )
        val sampleRateTensor = OnnxTensor.createTensor(environment, longArrayOf(config.sampleRate.toLong()))

        try {
            val inputs = mapOf("input" to inputTensor, "state" to stateTensor, "sr" to sampleRateTensor)
            session.run(inputs).use { result ->
                val probability = firstFloat(result[0].value)
                val nextState = result[1].value as Array<*>
                flattenState(nextState, state)
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
        is FloatArray -> value.first()
        is Array<*> -> firstFloat(value.firstOrNull())
        else -> error("Unexpected Silero output type: ${value?.javaClass}")
    }

    private fun flattenState(value: Array<*>, target: FloatArray) {
        var index = 0
        fun visit(v: Any?) {
            when (v) {
                is FloatArray -> for (x in v) target[index++] = x
                is Array<*> -> for (child in v) visit(child)
                else -> error("Unexpected Silero state type: ${v?.javaClass}")
            }
        }
        visit(value)
        require(index == target.size) { "Unexpected Silero state size: $index" }
    }

    override fun close() {
        session.close()
    }
}
