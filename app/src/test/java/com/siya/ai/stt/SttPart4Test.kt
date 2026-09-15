package com.siya.ai.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SttPart4Test {
    @Test
    fun defaultConfigTargetsMobilePcm() {
        val config = SttConfig()
        assertEquals(16_000, config.sampleRate)
        assertEquals(80, config.featureDim)
        assertEquals("greedy_search", config.decodingMethod)
        assertEquals("cpu", config.provider)
    }

    @Test
    fun resultKeepsTranscriptAndTiming() {
        val result = SttResult(
            text = "नमस्ते दुनिया",
            tokens = listOf("नमस्ते", "दुनिया"),
            timestampsSeconds = listOf(0f, 0.4f),
            durationMs = 800,
        )
        assertEquals("नमस्ते दुनिया", result.text)
        assertEquals(2, result.tokens.size)
        assertTrue(result.isFinal)
        assertEquals(800L, result.durationMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun configRejectsNon16kAudio() {
        SttConfig(sampleRate = 8_000)
    }
}
