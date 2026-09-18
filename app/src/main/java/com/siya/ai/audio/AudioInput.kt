package com.siya.ai.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

fun interface Pcm16Listener {
    fun onAudio(buffer: ShortArray, length: Int, timestampNanos: Long)
}

class AudioInput(
    private val context: Context,
    private val config: AudioEngineConfig = AudioEngineConfig(),
    private val effects: AudioEffectsController? = null
) {
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null
    private val running = AtomicBoolean(false)

    val isRunning: Boolean get() = running.get()

    fun start(listener: Pcm16Listener): Boolean {
        if (running.get()) return true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false

        val minBytes = AudioRecord.getMinBufferSize(config.sampleRate, config.channelMask, config.encoding)
        if (minBytes <= 0) return false

        // Keep enough headroom for scheduler jitter while staying latency-friendly.
        val targetBytes = config.sampleRate * config.bytesPerSample / 5
        val bufferBytes = maxOf(minBytes, targetBytes)
        val created = createRecorder(bufferBytes) ?: return false

        try {
            created.startRecording()
            if (created.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                created.release()
                return false
            }
        } catch (_: Exception) {
            created.release()
            return false
        }

        recorder = created
        effects?.attach(created)
        running.set(true)

        worker = Thread {
            val samples = ShortArray(maxOf(1, bufferBytes / config.bytesPerSample))
            try {
                while (running.get() && !Thread.currentThread().isInterrupted) {
                    val count = created.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    when {
                        count > 0 -> listener.onAudio(samples, count, System.nanoTime())
                        count == AudioRecord.ERROR_DEAD_OBJECT -> {
                            running.set(false)
                            break
                        }
                        count == AudioRecord.ERROR_INVALID_OPERATION -> {
                            running.set(false)
                            break
                        }
                        count < 0 -> {
                            // Unknown read failure: stop rather than spin at 100% CPU.
                            running.set(false)
                            break
                        }
                    }
                }
            } catch (_: SecurityException) {
                running.set(false)
            } catch (_: IllegalStateException) {
                running.set(false)
            } finally {
                try { created.stop() } catch (_: Exception) { }
                running.set(false)
            }
        }.apply {
            name = "Siya-AudioInput"
            isDaemon = true
            start()
        }
        return true
    }

    private fun createRecorder(bufferBytes: Int): AudioRecord? {
        // Prefer communication capture so Android can apply the device's
        // acoustic echo-cancellation path while Siya is speaking.
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        for (source in sources) {
            val candidate = try {
                AudioRecord(source, config.sampleRate, config.channelMask, config.encoding, bufferBytes)
            } catch (_: Exception) {
                null
            }
            if (candidate?.state == AudioRecord.STATE_INITIALIZED) return candidate
            candidate?.release()
        }
        return null
    }

    fun stop() {
        running.set(false)
        worker?.interrupt()
        try { recorder?.stop() } catch (_: Exception) { }
        worker?.join(750)
        worker = null
    }

    fun release() {
        stop()
        effects?.release()
        recorder?.release()
        recorder = null
    }
}
