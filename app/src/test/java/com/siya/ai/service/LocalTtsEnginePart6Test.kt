package com.siya.ai.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalTtsEnginePart6Test {
    @Test
    fun shortReply_isOneChunk() {
        assertEquals(listOf("Namaste Siya."), LocalTtsEngine.chunkText("Namaste Siya."))
    }

    @Test
    fun longReply_isSplitIntoSentenceChunks() {
        val chunks = LocalTtsEngine.chunkText("Pehla sentence. Dusra sentence! Teesra sentence? Chautha vakya।")
        assertEquals(4, chunks.size)
        assertTrue(chunks.all { it.isNotBlank() })
    }

    @Test
    fun newlines_areNormalizedAndEmptyChunksRemoved() {
        assertEquals(
            listOf("Hello.", "Namaste."),
            LocalTtsEngine.chunkText("Hello.\n\nNamaste.")
        )
    }
}
