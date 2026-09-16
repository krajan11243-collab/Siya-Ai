package com.siya.ai.llm

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import java.util.concurrent.atomic.AtomicBoolean

class LocalLlmEngine(
    private val modelStore: LlmModelStore,
    private val config: LlmConfig = LlmConfig(),
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private var model: LlamaModel? = null

    /** Loads the GGUF model once and restores/backups the model across app-data loss. */
    suspend fun load() {
        check(!closed.get()) { "LocalLlmEngine is closed" }
        if (model != null) return
        modelStore.restoreFromShared()
        modelStore.validate().getOrThrow()
        model = Llama.loadModel(
            modelPath = modelStore.modelPath(),
            config = LlamaConfig(
                contextSize = config.contextSize,
                threads = config.threads,
                gpuLayers = 0,
                temperature = config.temperature,
                topP = config.topP,
                topK = 40,
            ),
        )
        // Existing app-private models are migrated to the shared Siya Ai folder once.
        modelStore.backupToShared()
    }

    suspend fun complete(
        prompt: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    ): LlmResult {
        check(!closed.get()) { "LocalLlmEngine is closed" }
        require(prompt.isNotBlank())
        load()
        val loaded = model ?: error("Qwen model could not be loaded")
        val result = Llama.complete(
            loaded,
            prompt = prompt,
            systemPrompt = systemPrompt,
            maxTokens = config.maxTokens,
        )
        return LlmResult(text = result.text.trim(), tokensPerSecond = result.tokensPerSecond)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            model?.let(Llama::releaseModel)
            model = null
        }
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are Siya Ai, a private offline voice assistant. " +
                "Reply naturally and concisely. Prefer Hindi when the user speaks Hindi."
    }
}

data class LlmResult(
    val text: String,
    val tokensPerSecond: Float,
)
