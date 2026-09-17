package com.siya.ai.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingTextChunkerTest {
    @Test
    fun punctuationFlushesSentence() {
        val chunker = StreamingTextChunker()
        assertEquals(emptyList<String>(), chunker.append("Namaste Siya"))
        assertEquals(listOf("Namaste Siya."), chunker.append("" + "."))
    }

    @Test
    fun hindiDandaFlushesSentence() {
        val chunker = StreamingTextChunker()
        assertEquals(listOf("नमस्ते।"), chunker.append("नमस्ते। अगला"))
        assertEquals("अगला", chunker.flush())
    }

    @Test
    fun softLimitSplitsLongTextAtWhitespace() {
        val chunker = StreamingTextChunker(20)
        val chunks = chunker.append("one two three four five six")
        assertEquals(listOf("one two three four"), chunks)
        assertEquals("five six", chunker.flush())
    }

    @Test
    fun clearDropsPendingText() {
        val chunker = StreamingTextChunker()
        chunker.append("pending")
        chunker.clear()
        assertEquals("", chunker.flush())
    }
}
