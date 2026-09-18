package com.siya.ai.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechSegmentBufferTest {
    @Test
    fun preRollIsIncludedWhenSpeechStarts() {
        val buffer = SpeechSegmentBuffer(preRollMs = 10)
        buffer.append(ShortArray(160) { 1 })
        buffer.start()
        buffer.append(ShortArray(80) { 2 })
        val result = buffer.finish()

        assertEquals(240, result.size)
        assertTrue(result.take(160).all { it == 1.toShort() })
        assertTrue(result.drop(160).all { it == 2.toShort() })
    }

    @Test
    fun resetClearsActiveSegment() {
        val buffer = SpeechSegmentBuffer()
        buffer.start()
        buffer.append(shortArrayOf(1, 2, 3))
        buffer.reset()
        assertEquals(0, buffer.finish().size)
        assertTrue(!buffer.isActive())
    }
    @Test
    fun speechStartKeepsPreRollUntilFinish() {
        val buffer = SpeechSegmentBuffer(preRollMs = 10)
        buffer.append(ShortArray(160) { 7 })
        buffer.start()
        buffer.append(ShortArray(160) { 9 })
        val result = buffer.finish()
        assertEquals(320, result.size)
        assertEquals(7.toShort(), result.first())
        assertEquals(9.toShort(), result.last())
    }

}

