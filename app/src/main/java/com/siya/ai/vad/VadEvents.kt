package com.siya.ai.vad

enum class VadState { IDLE, SPEECH, ENDING }

enum class VadEventType { SPEECH_START, SPEECH_END }

data class VadEvent(
    val type: VadEventType,
    val samplePosition: Long
)

data class VadFrameResult(
    val probability: Float,
    val state: VadState,
    val event: VadEvent? = null
)
