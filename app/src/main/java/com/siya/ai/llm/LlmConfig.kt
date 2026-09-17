package com.siya.ai.llm

import kotlin.math.min

/** Device-adaptive defaults for concise but complete voice/chat turns. */
data class LlmConfig(
    val contextSize: Int = 2048,
    val threads: Int = min(Runtime.getRuntime().availableProcessors().coerceAtLeast(2), 6),
    val maxTokens: Int = 256,
    val temperature: Float = 0.25f,
    val topP: Float = 0.9f,
) {
    init {
        require(contextSize in 512..8192)
        require(threads in 1..8)
        require(maxTokens in 1..1024)
        require(temperature in 0f..2f)
        require(topP in 0f..1f)
    }
}
