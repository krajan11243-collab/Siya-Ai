package com.siya.ai.vad

/** Streaming Silero VAD v5 configuration for 16 kHz mono audio. */
data class VadConfig(
    val sampleRate: Int = 16_000,
    val threshold: Float = 0.5f,
    val minSpeechDurationMs: Long = 250,
    val minSilenceDurationMs: Long = 100,
    val speechPadMs: Long = 30
) {
    init {
        require(sampleRate == 16_000) { "Part 3 currently targets Silero 16 kHz mode" }
        require(threshold in 0f..1f)
        require(minSpeechDurationMs >= 0)
        require(minSilenceDurationMs >= 0)
        require(speechPadMs >= 0)
    }

    val windowSamples: Int = 512
    val contextSamples: Int = 64
}
