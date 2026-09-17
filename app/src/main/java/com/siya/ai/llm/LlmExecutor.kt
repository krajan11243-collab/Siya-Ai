package com.siya.ai.llm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class LlmExecutor(
    private val engineFactory: () -> LocalLlmEngine,
    private val onResult: (Result<LlmResult>) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var engine: LocalLlmEngine? = null

    /** Warm the GGUF model once so the first spoken request does not pay the full load cost. */
    fun warmUp() {
        if (closed.get()) return
        scope.launch {
            runCatching {
                val loaded = engine ?: engineFactory().also {
                    it.load()
                    engine = it
                }
                loaded
            }
        }
    }

    fun submit(prompt: String) {
        if (closed.get()) return
        scope.launch {
            val fast = LlmFastPath.answer(prompt)
            val result = if (fast != null) {
                Result.success(LlmResult(text = fast, tokensPerSecond = Float.POSITIVE_INFINITY))
            } else {
                runCatching {
                    val loaded = engine ?: engineFactory().also {
                        it.load()
                        engine = it
                    }
                    loaded.complete(prompt)
                }
            }
            onResult(result)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            scope.cancel()
            engine?.close()
            engine = null
        }
    }
}
