package com.siya.ai.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSessionStatePart7Test {
    @Test
    fun duplexStatesFollowListeningThinkingSpeakingInterrupting() {
        VoiceSessionState.stopped()

        VoiceSessionState.listening()
        assertEquals(VoiceSessionState.Phase.LISTENING, VoiceSessionState.state.value.phase)

        VoiceSessionState.thinking("कैसे हो?")
        assertEquals(VoiceSessionState.Phase.THINKING, VoiceSessionState.state.value.phase)
        assertEquals("कैसे हो?", VoiceSessionState.state.value.transcript)

        VoiceSessionState.speaking("मैं ठीक हूँ।")
        assertEquals(VoiceSessionState.Phase.SPEAKING, VoiceSessionState.state.value.phase)
        assertEquals("मैं ठीक हूँ।", VoiceSessionState.state.value.response)

        VoiceSessionState.interrupting()
        assertEquals(VoiceSessionState.Phase.INTERRUPTING, VoiceSessionState.state.value.phase)
        assertTrue(VoiceSessionState.state.value.active)

        VoiceSessionState.listening()
        assertEquals(VoiceSessionState.Phase.LISTENING, VoiceSessionState.state.value.phase)
        assertTrue(VoiceSessionState.state.value.active)

        VoiceSessionState.stopped()
        assertEquals(VoiceSessionState.Phase.IDLE, VoiceSessionState.state.value.phase)
    }
}
