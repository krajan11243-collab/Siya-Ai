package com.siya.ai.audio

import android.media.AcousticEchoCanceler
import android.media.AudioRecord
import android.media.NoiseSuppressor

/** Device-capability wrapper; effects are optional and never assumed to exist. */
class AudioEffectsController {
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    fun attach(record: AudioRecord): Status {
        release()
        val aec = if (AcousticEchoCanceler.isAvailable()) {
            runCatching { AcousticEchoCanceler.create(record.audioSessionId) }.getOrNull()
        } else null
        val ns = if (NoiseSuppressor.isAvailable()) {
            runCatching { NoiseSuppressor.create(record.audioSessionId) }.getOrNull()
        } else null
        echoCanceler = aec
        noiseSuppressor = ns
        return Status(
            echoCancellationAvailable = AcousticEchoCanceler.isAvailable(),
            echoCancellationEnabled = aec?.enabled == true,
            noiseSuppressionAvailable = NoiseSuppressor.isAvailable(),
            noiseSuppressionEnabled = ns?.enabled == true
        )
    }

    fun release() {
        runCatching { echoCanceler?.release() }
        runCatching { noiseSuppressor?.release() }
        echoCanceler = null
        noiseSuppressor = null
    }

    data class Status(
        val echoCancellationAvailable: Boolean,
        val echoCancellationEnabled: Boolean,
        val noiseSuppressionAvailable: Boolean,
        val noiseSuppressionEnabled: Boolean
    )
}
