package com.siya.ai.stt

/**
 * Collects PCM16 utterances with a small pre-roll so VAD decision latency does not
 * cut off the first phonemes of a sentence.
 */
class SpeechSegmentBuffer(
    private val sampleRate: Int = 16_000,
    private val maxDurationMs: Long = 30_000L,
    private val preRollMs: Long = 320L,
) {
    private val maxSamples = (sampleRate * maxDurationMs / 1000L).toInt()
    private val preRollSamples = (sampleRate * preRollMs / 1000L).toInt()
    private var data = ShortArray(minOf(sampleRate * 2, maxSamples))
    private var ring = ShortArray(preRollSamples.coerceAtLeast(1))
    private var ringSize = 0
    private var ringWrite = 0
    private var size = 0
    private var active = false

    fun start() {
        size = 0
        active = true
        if (ringSize > 0) {
            ensureCapacity(ringSize)
            val first = (ringWrite - ringSize + ring.size) % ring.size
            val firstCount = minOf(ringSize, ring.size - first)
            System.arraycopy(ring, first, data, 0, firstCount)
            if (firstCount < ringSize) {
                System.arraycopy(ring, 0, data, firstCount, ringSize - firstCount)
            }
            size = ringSize
        }
    }

    fun append(samples: ShortArray, length: Int = samples.size) {
        if (length <= 0) return
        require(length <= samples.size)
        appendToRing(samples, length)
        if (!active) return
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
        ringSize = 0
        ringWrite = 0
    }

    fun isActive(): Boolean = active
    fun sampleCount(): Int = size

    private fun appendToRing(samples: ShortArray, length: Int) {
        var offset = 0
        while (offset < length) {
            val count = minOf(length - offset, ring.size - ringWrite)
            System.arraycopy(samples, offset, ring, ringWrite, count)
            ringWrite = (ringWrite + count) % ring.size
            ringSize = minOf(ring.size, ringSize + count)
            offset += count
        }
    }

    private fun ensureCapacity(required: Int) {
        if (required <= data.size) return
        var capacity = data.size.coerceAtLeast(1)
        while (capacity < required) {
            val next = minOf(maxSamples, capacity * 2)
            if (next == capacity) break
            capacity = next
        }
        data = data.copyOf(capacity)
    }
}
