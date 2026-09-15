package com.siya.ai.agent.llm

/** Runtime knobs for the local LLM. Values are intentionally conservative for mobile. */
data class LlmConfig(
    val contextSize: Int = 2048,
    val threads: Int = 4,
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f
) {
    init {
        require(contextSize in 512..8192) { "contextSize must be between 512 and 8192" }
        require(threads in 1..16) { "threads must be between 1 and 16" }
        require(maxTokens in 1..2048) { "maxTokens must be between 1 and 2048" }
        require(temperature in 0f..2f) { "temperature must be between 0 and 2" }
    }
}
