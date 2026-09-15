package com.siya.ai.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmPart5Test {
    @Test
    fun defaultConfigIsMobileSafe() {
        val config = LlmConfig()
        assertEquals(2048, config.contextSize)
        assertEquals(4, config.threads)
        assertEquals(256, config.maxTokens)
    }

    @Test
    fun modelContractTargetsQwenQ4Km() {
        assertTrue(LlmModelStore.MODEL_FILE.endsWith("q4_k_m.gguf"))
        assertEquals(64, LlmModelStore.MODEL_SHA256.length)
        assertTrue(LlmModelStore.MODEL_URL.contains("Qwen2.5-1.5B-Instruct-GGUF"))
    }
}
