package com.siya.ai.agent.llm

/** Small prompt layer kept independent from the inference engine. */
object PromptFormatter {
    private val systemPrompt = """
        You are Siya Ai, a helpful private voice assistant running locally on an Android device.
        Answer naturally and concisely. Never claim to have performed an Android action unless the action layer confirms it.
        If an action is unavailable offline, explain the limitation briefly.
    """.trimIndent()

    fun singleTurn(userText: String): String {
        val clean = userText.trim()
        require(clean.isNotEmpty()) { "userText cannot be empty" }
        return buildString {
            append("<|im_start|>system\n")
            append(systemPrompt)
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(clean)
            append("<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }
}
