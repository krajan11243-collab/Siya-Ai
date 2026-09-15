package com.siya.ai.llm

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import java.util.concurrent.atomic.AtomicBoolean

class LocalLlmEngine(
    private val modelStore: LlmModelStore,
    private val config: LlmConfig = LlmConfig(),
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private var model: Any? = null

    suspend fun load() {
        check(!closed.get()) { "LocalLlmEngine is closed" }
        check(model == null) { "Qwen model is already loaded" }
        modelStore.validate().getOrThrow()
        model = Llama.loadModel(
            modelPath = modelStore.modelPath(),
            config = LlamaConfig(
                contextSize = config.contextSize,
                threads = config.threads,
            ),
        )
    }

    suspend fun complete(
        prompt: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    ): LlmResult {
        check(!closed.get()) { "LocalLlmEngine is closed" }
        require(prompt.isNotBlank())
        val loaded = model ?: error("Qwen model is not loaded")
        val result = Llama.complete(
            loaded,
            prompt = prompt,
            systemPrompt = systemPrompt,
            maxTokens = config.maxTokens,
        )
        return LlmResult(
            text = result.text.trim(),
            tokensPerSecond = result.tokensPerSecond,
        )
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            val loaded = model
            model = null
            if (loaded != null) {
                Llama.releaseModel(loaded)
            }
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
