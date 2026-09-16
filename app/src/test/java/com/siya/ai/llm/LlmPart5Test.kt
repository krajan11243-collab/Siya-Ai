package com.siya.ai.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmPart5Test {
    @Test
    fun defaultConfigIsMobileAdaptiveAndFast() {
        val config = LlmConfig()
        assertEquals(512, config.contextSize)
        assertEquals(12, config.maxTokens)
        assertTrue(config.threads in 2..6)
    }

    @Test
    fun modelContractTargetsQwenQ4Km() {
        assertTrue(LlmModelStore.MODEL_FILE.endsWith("q4_k_m.gguf"))
        assertEquals(64, LlmModelStore.MODEL_SHA256.length)
        assertTrue(LlmModelStore.MODEL_URL.contains("Qwen2.5-1.5B-Instruct-GGUF"))
    }
}
