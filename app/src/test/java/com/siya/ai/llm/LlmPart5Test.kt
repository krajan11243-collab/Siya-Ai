package com.siya.ai.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmPart5Test {
    @Test
    fun defaultConfigIsMobileAdaptiveAndFast() {
        val config = LlmConfig()
        assertEquals(2048, config.contextSize)
        assertEquals(256, config.maxTokens)
        assertTrue(config.threads in 2..6)
    }

    @Test
    fun modelContractTargetsQwenQ4Km() {
        assertTrue(LlmModelStore.MODEL_FILE.endsWith("q4_k_m.gguf"))
        assertEquals(64, LlmModelStore.MODEL_SHA256.length)
        assertTrue(LlmModelStore.MODEL_URL.contains("Qwen2.5-1.5B-Instruct-GGUF"))
    }

    @Test
    fun greetingFastPathAvoidsModelGeneration() {
        val expected = "Namaste! Main Siya hoon. Kaise madad karun?"
        assertEquals(expected, LlmFastPath.answer("Hi"))
        assertEquals(expected, LlmFastPath.answer("  HELLO   SIYA "))
        assertEquals(expected, LlmFastPath.answer("नमस्ते"))
        assertNull(LlmFastPath.answer("Aaj mausam kaisa hai?"))
    }
}
