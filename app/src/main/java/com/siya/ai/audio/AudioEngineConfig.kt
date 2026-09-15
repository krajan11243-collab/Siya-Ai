package com.siya.ai.audio

import android.media.AudioFormat

/** Immutable configuration for Siya's Part 2 PCM audio pipeline. */
data class AudioEngineConfig(
    val sampleRate: Int = 16_000,
    val channelMask: Int = AudioFormat.CHANNEL_IN_MONO,
    val encoding: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(channelMask == AudioFormat.CHANNEL_IN_MONO) { "Siya currently requires mono input" }
        require(encoding == AudioFormat.ENCODING_PCM_16BIT) { "Siya currently requires PCM 16-bit" }
    }

    val channelCount: Int = 1
    val bytesPerSample: Int = 2
}
