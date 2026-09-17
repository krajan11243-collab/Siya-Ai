package com.siya.ai.audio

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Part 6 PCM playback queue. Local neural TTS backends can enqueue PCM chunks here.
 * Cancellation clears pending audio and interrupts the current blocking write.
 */
class AudioTrackQueue(
    private val output: AudioOutput = AudioOutput(),
) {
    private val queue = LinkedBlockingQueue<ShortArray>()
    private val running = AtomicBoolean(false)
    @Volatile private var worker: Thread? = null

    @Synchronized
    fun start() {
        if (running.getAndSet(true)) return
        worker = Thread({ drain() }, "Siya-TTS-AudioTrack").apply { start() }
    }

    fun enqueue(pcm: ShortArray) {
        if (pcm.isEmpty()) return
        start()
        queue.offer(pcm.copyOf())
    }

    @Synchronized
    fun cancel() {
        queue.clear()
        running.set(false)
        worker?.interrupt()
        worker = null
        output.stop()
        output.flush()
    }

    fun pendingChunks(): Int = queue.size

    @Synchronized
    fun release() {
        cancel()
        output.release()
    }

    private fun drain() {
        try {
            while (running.get() && !Thread.currentThread().isInterrupted) {
                val pcm = queue.poll(250, TimeUnit.MILLISECONDS) ?: continue
                if (!running.get()) break
                output.play(pcm)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            output.stop()
        }
    }
}
