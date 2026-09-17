package com.siya.ai.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** Part 6 PCM output queue with generation-based hard cancellation. */
class TtsPcmAudioQueue : AutoCloseable {
    private data class Chunk(val samples: FloatArray, val sampleRate: Int, val generation: Long)

    private val closed = AtomicBoolean(false)
    private val lock = Any()
    private val queue = LinkedBlockingQueue<Chunk>()
    private var generation = 0L
    private var track: AudioTrack? = null
    private var worker: Thread? = null
    private var sampleRate = 0

    fun enqueue(samples: FloatArray, rate: Int, generationId: Long): Boolean {
        if (samples.isEmpty() || rate <= 0 || closed.get()) return false
        synchronized(lock) {
            if (generationId != generation) return false
            ensureWorkerLocked()
            queue.offer(Chunk(samples.copyOf(), rate, generationId))
        }
        return true
    }

    fun newGeneration(): Long = synchronized(lock) {
        generation += 1
        queue.clear()
        stopTrackLocked()
        generation
    }

    fun currentGeneration(): Long = synchronized(lock) { generation }

    private fun ensureWorkerLocked() {
        if (worker?.isAlive == true) return
        worker = Thread(::runWorker, "Siya-TTS-AudioTrack").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    private fun runWorker() {
        while (!closed.get()) {
            val chunk = try {
                queue.take()
            } catch (_: InterruptedException) {
                if (closed.get()) return
                continue
            }

            var stale = false
            synchronized(lock) {
                if (chunk.generation != generation) {
                    stale = true
                } else {
                    try {
                        ensureTrackLocked(chunk.sampleRate)
                    } catch (_: Throwable) {
                        queue.clear()
                        stale = true
                    }
                }
            }
            if (stale) continue

            val shorts = ShortArray(chunk.samples.size) {
                (chunk.samples[it].coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
            }
            var offset = 0
            var cancelled = false
            while (offset < shorts.size && !closed.get()) {
                synchronized(lock) {
                    if (chunk.generation != generation) cancelled = true
                }
                if (cancelled) break
                val written = try {
                    track?.write(shorts, offset, shorts.size - offset, AudioTrack.WRITE_BLOCKING) ?: -1
                } catch (_: Throwable) {
                    -1
                }
                if (written <= 0) break
                offset += written
            }
        }
    }

    private fun ensureTrackLocked(rate: Int) {
        if (track != null && sampleRate == rate) return
        stopTrackLocked()
        val min = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(rate / 10)
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(min)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()
        sampleRate = rate
    }

    private fun stopTrackLocked() {
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
        sampleRate = 0
    }

    fun stop() {
        synchronized(lock) {
            generation += 1
            queue.clear()
            stopTrackLocked()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(lock) {
            generation += 1
            queue.clear()
            stopTrackLocked()
            worker?.interrupt()
            worker = null
        }
    }
}
