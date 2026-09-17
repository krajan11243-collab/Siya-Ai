package com.siya.ai.llm

/** Incrementally converts token fragments into short TTS-friendly phrases. */
class StreamingTextChunker(private val softLimit: Int = 80) {
    private val buffer = StringBuilder()

    fun append(token: String): List<String> {
        if (token.isNotEmpty()) buffer.append(token)
        val chunks = mutableListOf<String>()
        while (true) {
            val punctuation = buffer.indexOfFirst { it == '.' || it == '!' || it == '?' || it == '।' }
            if (punctuation >= 0) {
                chunks += buffer.substring(0, punctuation + 1).trim()
                buffer.deleteRange(0, punctuation + 1)
                trimLeadingWhitespace()
                continue
            }
            if (buffer.length >= softLimit) {
                val split = buffer.lastIndexOf(' ', softLimit - 1)
                if (split > 0) {
                    chunks += buffer.substring(0, split).trim()
                    buffer.deleteRange(0, split + 1)
                    trimLeadingWhitespace()
                    continue
                }
            }
            break
        }
        return chunks.filter { it.isNotBlank() }
    }

    fun flush(): String {
        val value = buffer.toString().trim()
        buffer.setLength(0)
        return value
    }

    fun clear() = buffer.setLength(0)

    private fun trimLeadingWhitespace() {
        while (buffer.isNotEmpty() && buffer.first().isWhitespace()) buffer.deleteCharAt(0)
    }
}
