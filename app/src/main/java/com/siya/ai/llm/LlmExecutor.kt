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

    fun submit(prompt: String) {
        if (closed.get()) return
        scope.launch {
            val result = runCatching {
                val loaded = engine ?: engineFactory().also {
                    it.load()
                    engine = it
                }
                loaded.complete(prompt)
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
