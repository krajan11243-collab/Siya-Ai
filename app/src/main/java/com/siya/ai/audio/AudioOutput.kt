package com.siya.ai.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean

class AudioOutput(private val config: AudioEngineConfig = AudioEngineConfig()) {
    private var track: AudioTrack? = null
    private val started = AtomicBoolean(false)

    val isReady: Boolean get() = track?.state == AudioTrack.STATE_INITIALIZED

    fun initialize(): Boolean {
        if (isReady) return true
        val minBytes = AudioTrack.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            config.encoding
        )
        if (minBytes <= 0) return false
        val bufferBytes = maxOf(minBytes, config.sampleRate * config.bytesPerSample / 5)
        val created = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(config.sampleRate)
                        .setEncoding(config.encoding)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) { null }
        if (created == null || created.state != AudioTrack.STATE_INITIALIZED) {
            created?.release()
            return false
        }
        track = created
        return true
    }

    fun play(pcm: ShortArray, offset: Int = 0, length: Int = pcm.size): Int {
        if (!initialize()) return 0
        if (!started.getAndSet(true)) track?.play()
        return track?.write(pcm, offset, length, AudioTrack.WRITE_BLOCKING) ?: 0
    }

    fun stop() {
        started.set(false)
        try { track?.pause() } catch (_: Exception) { }
    }

    fun flush() {
        try { track?.flush() } catch (_: Exception) { }
    }

    fun release() {
        stop()
        flush()
        track?.release()
        track = null
    }
}
