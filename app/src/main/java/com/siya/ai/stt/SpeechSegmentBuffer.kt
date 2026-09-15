package com.siya.ai.stt

/**
 * Collects PCM16 frames between VAD speech-start and speech-end events.
 * Uses a bounded in-memory buffer so a broken VAD cannot grow memory without limit.
 */
class SpeechSegmentBuffer(
    private val sampleRate: Int = 16_000,
    private val maxDurationMs: Long = 30_000L,
) {
    private val maxSamples = (sampleRate * maxDurationMs / 1000L).toInt()
    private var data = ShortArray(minOf(sampleRate * 2, maxSamples))
    private var size = 0
    private var active = false

    fun start() {
        size = 0
        active = true
    }

    fun append(samples: ShortArray, length: Int = samples.size) {
        if (!active || length <= 0) return
        require(length <= samples.size)
        val accepted = minOf(length, maxSamples - size)
        ensureCapacity(size + accepted)
        if (accepted > 0) {
            System.arraycopy(samples, 0, data, size, accepted)
            size += accepted
        }
    }

    fun finish(): ShortArray {
        if (!active) return ShortArray(0)
        active = false
        return data.copyOf(size)
    }

    fun reset() {
        active = false
        size = 0
    }

    fun isActive(): Boolean = active
    fun sampleCount(): Int = size

    private fun ensureCapacity(required: Int) {
        if (required <= data.size) return
        var capacity = data.size.coerceAtLeast(1)
        while (capacity < required) {
            capacity = minOf(maxSamples, capacity * 2)
            if (capacity == maxSamples) break
        }
        data = data.copyOf(capacity)
    }
}
