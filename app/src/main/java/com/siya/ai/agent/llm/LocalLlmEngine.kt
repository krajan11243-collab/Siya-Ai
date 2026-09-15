package com.siya.ai.agent.llm

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LlmAvailability {
    data object Ready : LlmAvailability
    data object NativeEngineMissing : LlmAvailability
    data object ModelMissing : LlmAvailability
}

/**
 * Lifecycle-safe local LLM facade. UI/service code talks to this class, never directly to JNI.
 */
class LocalLlmEngine(
    private val modelFile: File,
    private val config: LlmConfig = LlmConfig()
) {
    private var handle: Long = 0L

    val availability: LlmAvailability
        get() = when {
            !LlamaJni.isAvailable() -> LlmAvailability.NativeEngineMissing
            !modelFile.isFile || modelFile.length() == 0L -> LlmAvailability.ModelMissing
            else -> LlmAvailability.Ready
        }

    fun isLoaded(): Boolean = handle != 0L

    suspend fun load(): Result<Unit> = withContext(Dispatchers.Default) {
        if (availability != LlmAvailability.Ready) {
            return@withContext Result.failure(IllegalStateException("LLM is not ready: $availability"))
        }
        if (handle != 0L) return@withContext Result.success(Unit)
        runCatching {
            handle = LlamaJni.create(modelFile.absolutePath, config.contextSize, config.threads)
            check(handle != 0L) { "Native LLM returned an invalid handle" }
        }
    }

    suspend fun generate(userText: String): Result<String> = withContext(Dispatchers.Default) {
        if (userText.isBlank()) return@withContext Result.failure(IllegalArgumentException("Prompt is empty"))
        if (handle == 0L) {
            val loaded = load()
            if (loaded.isFailure) return@withContext Result.failure(loaded.exceptionOrNull()!!)
        }
        runCatching {
            LlamaJni.generate(
                handle,
                PromptFormatter.singleTurn(userText),
                config.maxTokens,
                config.temperature
            ).trim()
        }
    }

    fun cancel() {
        if (handle != 0L) LlamaJni.cancel(handle)
    }

    fun unload() {
        if (handle != 0L) {
            LlamaJni.destroy(handle)
            handle = 0L
        }
    }
}
