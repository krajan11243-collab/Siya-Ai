package com.siya.ai.llm

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Single-turn local LLM executor with turn-generation cancellation semantics. */
class LlmExecutor(
    private val engineFactory: () -> LocalLlmEngine,
    private val onResult: (Result<LlmResult>) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val turnId = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var engine: LocalLlmEngine? = null
    @Volatile private var currentJob: Job? = null

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
        if (closed.get() || prompt.isBlank()) return
        val myTurn = turnId.incrementAndGet()
        currentJob?.cancel()
        currentJob = scope.launch {
            val result = try {
                val fast = LlmFastPath.answer(prompt)
                if (fast != null) {
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
            } catch (cancelled: CancellationException) {
                return@launch
            } catch (error: Throwable) {
                Result.failure(error)
            }
            if (!closed.get() && myTurn == turnId.get() && currentCoroutineContext().isActive) {
                onResult(result)
            }
        }
    }

    /** Invalidates the active turn immediately. Older native work can finish, but its result is discarded. */
    fun cancelCurrent() {
        turnId.incrementAndGet()
        currentJob?.cancel()
        currentJob = null
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            turnId.incrementAndGet()
            currentJob?.cancel()
            currentJob = null
            scope.cancel()
            engine?.close()
            engine = null
        }
    }
}
