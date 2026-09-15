package com.siya.ai.stt

/** Configuration for offline Hindi/Hinglish speech recognition. */
data class SttConfig(
    val sampleRate: Int = 16_000,
    val featureDim: Int = 80,
    val numThreads: Int = 2,
    val decodingMethod: String = "greedy_search",
    val provider: String = "cpu",
    val language: String = "hi",
) {
    init {
        require(sampleRate == 16_000) { "Siya STT expects 16 kHz PCM input" }
        require(featureDim > 0)
        require(numThreads in 1..8)
        require(decodingMethod == "greedy_search" || decodingMethod == "modified_beam_search")
        require(provider.isNotBlank())
        require(language.isNotBlank())
    }
}
