package com.siya.ai.llm

/** Immediate answers for trivial conversational turns; avoids loading the GGUF model for greetings. */
object LlmFastPath {
    fun answer(prompt: String): String? {
        val normalized = prompt.trim().lowercase().replace(Regex("\\s+"), " ")
        return when (normalized) {
            "hi", "hello", "hey", "hi siya", "hello siya", "hey siya" ->
                "Namaste! Main Siya hoon. Kaise madad karun?"
            "namaste", "namaste siya", "नमस्ते", "नमस्ते सिया" ->
                "Namaste! Main Siya hoon. Kaise madad karun?"
            else -> null
        }
    }
}
