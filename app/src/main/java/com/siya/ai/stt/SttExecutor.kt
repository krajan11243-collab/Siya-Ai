package com.siya.ai.stt

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keeps native ASR work off AudioRecord/VAD threads.
 * One serialized worker avoids concurrent native recognizer access and excess RAM pressure.
 */
class SttExecutor(
    private val engineFactory: () -> HindiSttEngine,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Siya-STT").apply { isDaemon = true }
    }
    private var engine: HindiSttEngine? = null

    fun submit(pcm16: ShortArray, length: Int, callback: (Result<SttResult>) -> Unit): Future<*> {
        check(!closed.get()) { "SttExecutor is closed" }
        require(length in 1..pcm16.size)
        val copy = pcm16.copyOf(length)
        return executor.submit {
            val result = runCatching {
                val recognizer = engine ?: engineFactory().also { engine = it }
                recognizer.transcribe(copy, copy.size)
            }
            callback(result)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow()
            engine?.close()
            engine = null
        }
    }
}
