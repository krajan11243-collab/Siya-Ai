package com.siya.ai.llm

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Single-turn local LLM executor with streaming tokens and safe background model warm-up. */
class LlmExecutor(
    private val engineFactory: () -> LocalLlmEngine,
    private val onResult: (Result<LlmResult>) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val turnId = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var engine: LocalLlmEngine? = null
    @Volatile private var currentJob: Job? = null
    @Volatile private var warmupJob: Job? = null

    /**
     * Loads the GGUF model in the background without being cancelled when the user
     * starts a turn. This avoids restarting a 1+ GB model load at the exact moment
     * the first voice request arrives.
     */
    fun warmUp() {
        if (closed.get() || engine != null || warmupJob?.isActive == true) return
        warmupJob = scope.launch {
            runCatching {
                currentCoroutineContext().ensureActive()
                if (engine == null) {
                    val loaded: LocalLlmEngine = engineFactory()
                    loaded.load()
                    if (!closed.get()) {
                        engine = loaded
                    } else {
                        loaded.close()
                    }
                }
            }
        }
    }

    fun isModelLoaded(): Boolean = engine != null && !closed.get()

    fun submit(prompt: String, onToken: (String) -> Unit = {}) {
        if (closed.get() || prompt.isBlank()) return
        val myTurn = turnId.incrementAndGet()
        // Cancel only an active inference. Never cancel model warm-up.
        engine?.cancelGeneration()
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                currentCoroutineContext().ensureActive()

                // Simple greetings must answer immediately. Do not make them wait
                // for the 1+ GB GGUF warm-up.
                val fastAnswer = LlmFastPath.answer(prompt)
                if (fastAnswer != null) {
                    onToken(fastAnswer)
                    val result = LlmResult(
                        text = fastAnswer,
                        tokensPerSecond = Float.POSITIVE_INFINITY,
                    )
                    if (!closed.get() && myTurn == turnId.get() && currentCoroutineContext().isActive) {
                        onResult(Result.success(result))
                    }
                    return@launch
                }

                // For a real generation, wait for the single background warm-up
                // instead of loading the native model twice.
                warmupJob?.join()
                currentCoroutineContext().ensureActive()

                val result: LlmResult = run {
                    val loaded = engine ?: engineFactory().also {
                        it.load()
                        engine = it
                    }
                    loaded.stream(prompt, onToken = onToken)
                }

                if (!closed.get() && myTurn == turnId.get() && currentCoroutineContext().isActive) {
                    onResult(Result.success(result))
                }
            } catch (cancelled: CancellationException) {
                // Expected on barge-in / a newer turn. Do not surface a fake error.
            } catch (error: Throwable) {
                if (!closed.get() && myTurn == turnId.get() && currentCoroutineContext().isActive) {
                    onResult(Result.failure(error))
                }
            }
        }
    }

    fun cancelCurrent() {
        turnId.incrementAndGet()
        engine?.cancelGeneration()
        currentJob?.cancel()
        currentJob = null
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            turnId.incrementAndGet()
            engine?.cancelGeneration()
            currentJob?.cancel()
            warmupJob?.cancel()
            currentJob = null
            warmupJob = null
            scope.cancel()
            engine?.close()
            engine = null
        }
    }
}
