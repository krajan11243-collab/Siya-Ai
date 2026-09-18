package com.siya.ai.llm

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import org.codeshipping.llamakotlin.LlamaModel
import java.util.concurrent.atomic.AtomicBoolean

/** Local GGUF engine with real llama.cpp Flow token streaming. */
class LocalLlmEngine(
    private val modelStore: LlmModelStore,
    private val config: LlmConfig = LlmConfig(),
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private var model: LlamaModel? = null

    suspend fun load() {
        check(!closed.get()) { "LocalLlmEngine is closed" }
        if (model != null) return
        modelStore.restoreFromShared()
        modelStore.validate().getOrThrow()
        model = LlamaModel.load(modelStore.modelPath()) {
            contextSize = config.contextSize
            threads = config.threads
            threadsBatch = config.threads
            batchSize = 256
            temperature = config.temperature
            topP = config.topP
            topK = 40
            repeatPenalty = 1.1f
            maxTokens = config.maxTokens
            useMmap = true
            useMlock = false
            gpuLayers = 0
        }
        // The installer already maintains the shared backup; never copy the 1+ GB GGUF during model load.
    }

    suspend fun complete(
        prompt: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    ): LlmResult = stream(prompt, systemPrompt) { }

    /** Streams every generated token. The callback runs on the generation coroutine. */
    suspend fun stream(
        prompt: String,
        systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        onToken: (String) -> Unit,
    ): LlmResult {
        check(!closed.get()) { "LocalLlmEngine is closed" }
        require(prompt.isNotBlank())
        load()
        val loaded = model ?: error("Qwen model could not be loaded")
        val formatted = buildChatPrompt(systemPrompt, prompt)
        val output = StringBuilder()
        val started = System.nanoTime()
        var tokenCount = 0
        loaded.generateStream(formatted).collect { token ->
            currentCoroutineContext().ensureActive()
            if (token.isNotEmpty()) {
                output.append(token)
                tokenCount++
                onToken(token)
            }
        }
        val elapsedSeconds = ((System.nanoTime() - started).coerceAtLeast(1L)) / 1_000_000_000.0
        val tps = if (tokenCount == 0) 0f else (tokenCount / elapsedSeconds).toFloat()
        return LlmResult(text = output.toString().trim(), tokensPerSecond = tps)
    }

    fun cancelGeneration() {
        runCatching { model?.cancelGeneration() }
    }

    /** Qwen2.5-Instruct chat template, built locally so the Android binding does not need template APIs. */
    private fun buildChatPrompt(systemPrompt: String, userPrompt: String): String = buildString {
        append("<|im_start|>system\n")
        append(systemPrompt.trim())
        append("<|im_end|>\n")
        append("<|im_start|>user\n")
        append(userPrompt.trim())
        append("<|im_end|>\n")
        append("<|im_start|>assistant\n")
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            cancelGeneration()
            runCatching { model?.close() }
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
