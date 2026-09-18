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
                if (engine == null) engineFactory().also { it.load() }.let { loaded ->
                    if (!closed.get()) engine = loaded else loaded.close()
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
            val result = try {
                currentCoroutineContext().ensureActive()
                // If warm-up is still loading, wait for it rather than starting
                // another native model load.
                warmupJob?.join()
                currentCoroutineContext().ensureActive()

                val fast = LlmFastPath.answer(prompt)
                if (fast != null) {
                    onToken(fast)
                    Result.success(LlmResult(text = fast, tokensPerSecond = Float.POSITIVE_INFINITY))
                } else {
                    val loaded = engine ?: engineFactory().also {
                        it.load()
                        engine = it
                    }
                    loaded.stream(prompt, onToken = onToken)
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
