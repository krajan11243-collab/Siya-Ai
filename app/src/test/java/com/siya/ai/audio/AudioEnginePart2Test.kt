package com.siya.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEnginePart2Test {
    @Test fun defaultConfigIs16kMonoPcm16() {
        val config = AudioEngineConfig()
        assertEquals(16_000, config.sampleRate)
        assertEquals(1, config.channelCount)
        assertEquals(2, config.bytesPerSample)
    }

    @Test fun ringBufferPreservesOrder() {
        val buffer = PcmRingBuffer(8)
        val input = shortArrayOf(1, 2, 3, 4)
        assertEquals(4, buffer.write(input))
        val output = ShortArray(4)
        assertEquals(4, buffer.read(output))
        assertTrue(input.contentEquals(output))
    }

    @Test fun ringBufferDropsOldestOnOverflow() {
        val buffer = PcmRingBuffer(3)
        buffer.write(shortArrayOf(1, 2, 3, 4))
        val output = ShortArray(3)
        buffer.read(output)
        assertTrue(shortArrayOf(2, 3, 4).contentEquals(output))
    }

    @Test fun ringBufferPartialReadWorks() {
        val buffer = PcmRingBuffer(4)
        buffer.write(shortArrayOf(5, 6, 7))
        val output = ShortArray(2)
        assertEquals(2, buffer.read(output))
        assertTrue(shortArrayOf(5, 6).contentEquals(output))
        assertEquals(1, buffer.available())
    }
}
