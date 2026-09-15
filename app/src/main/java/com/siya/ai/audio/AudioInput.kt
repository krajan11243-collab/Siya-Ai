package com.siya.ai.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

fun interface Pcm16Listener {
    fun onAudio(buffer: ShortArray, length: Int, timestampNanos: Long)
}

class AudioInput(
    private val context: Context,
    private val config: AudioEngineConfig = AudioEngineConfig()
) {
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private val running = AtomicBoolean(false)

    val isRunning: Boolean get() = running.get()

    fun start(listener: Pcm16Listener): Boolean {
        if (running.get()) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        val minBytes = AudioRecord.getMinBufferSize(config.sampleRate, config.channelMask, config.encoding)
        if (minBytes <= 0) return false
        val bufferBytes = maxOf(minBytes, config.sampleRate * config.bytesPerSample / 5)

        val created = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                config.sampleRate,
                config.channelMask,
                config.encoding,
                bufferBytes
            )
        } catch (_: Exception) {
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    config.sampleRate,
                    config.channelMask,
                    config.encoding,
                    bufferBytes
                )
            } catch (_: Exception) {
                null
            }
        }

        if (created == null || created.state != AudioRecord.STATE_INITIALIZED) {
            created?.release()
            return false
        }

        recorder = created
        running.set(true)
        worker = Thread {
            val samples = ShortArray(bufferBytes / config.bytesPerSample)
            try {
                created.startRecording()
                if (created.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    running.set(false)
                    return@Thread
                }
                while (running.get()) {
                    val count = created.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    if (count > 0) listener.onAudio(samples, count, System.nanoTime())
                    else if (count == AudioRecord.ERROR_DEAD_OBJECT || count == AudioRecord.ERROR_INVALID_OPERATION) break
                }
            } catch (_: SecurityException) {
                // Permission can be revoked while the service is alive.
            } finally {
                running.set(false)
                try { created.stop() } catch (_: Exception) { }
            }
        }.apply {
            name = "Siya-AudioInput"
            isDaemon = true
            start()
        }
        return true
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        worker?.interrupt()
        try { recorder?.stop() } catch (_: Exception) { }
        worker?.join(500)
        worker = null
    }

    fun release() {
        stop()
        recorder?.release()
        recorder = null
    }
}
