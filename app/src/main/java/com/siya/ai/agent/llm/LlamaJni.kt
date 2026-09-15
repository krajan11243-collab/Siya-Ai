package com.siya.ai.agent.llm

/** JNI boundary for the optional llama.cpp native runtime.
 *
 * The native library is intentionally loaded only when the bundled native engine exists.
 * This keeps the Android app buildable before the llama.cpp sources/ABI binaries are added.
 */
internal object LlamaJni {
    private var loaded = false

    init {
        loaded = try {
            System.loadLibrary("siya_llama")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    fun isAvailable(): Boolean = loaded

    external fun create(modelPath: String, contextSize: Int, threads: Int): Long
    external fun generate(handle: Long, prompt: String, maxTokens: Int, temperature: Float): String
    external fun cancel(handle: Long)
    external fun destroy(handle: Long)
}
