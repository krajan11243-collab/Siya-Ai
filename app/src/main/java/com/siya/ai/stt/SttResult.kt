package com.siya.ai.stt

data class SttResult(
    val text: String,
    val tokens: List<String> = emptyList(),
    val timestampsSeconds: List<Float> = emptyList(),
    val durationMs: Long = 0L,
    val isFinal: Boolean = true,
)
