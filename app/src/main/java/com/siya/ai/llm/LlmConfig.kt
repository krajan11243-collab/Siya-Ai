package com.siya.ai.llm

data class LlmConfig(
    val contextSize: Int = 1024,
    val threads: Int = 4,
    val maxTokens: Int = 48,
    val temperature: Float = 0.55f,
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
