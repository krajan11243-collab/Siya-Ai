package com.siya.ai.vad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class VadPart3Test {
    @Test fun defaultConfigUsesSilero16kFrame() {
        val config = VadConfig()
        assertEquals(16_000, config.sampleRate)
        assertEquals(512, config.windowSamples)
        assertEquals(64, config.contextSamples)
    }

    @Test fun speechStartsAfterMinimumSpeechDuration() {
        val machine = SpeechStateMachine(VadConfig(minSpeechDurationMs = 64))
        assertNull(machine.accept(0.9f, 512).event)
        val result = machine.accept(0.9f, 512)
        assertNotNull(result.event)
        assertEquals(VadEventType.SPEECH_START, result.event?.type)
        assertEquals(VadState.SPEECH, result.state)
    }

    @Test fun silenceEndsSpeechAfterTimeout() {
        val machine = SpeechStateMachine(VadConfig(minSpeechDurationMs = 0, minSilenceDurationMs = 64))
        assertEquals(VadEventType.SPEECH_START, machine.accept(0.9f).event?.type)
        assertNull(machine.accept(0.1f, 512).event)
        assertEquals(VadEventType.SPEECH_END, machine.accept(0.1f, 512).event?.type)
        assertEquals(VadState.IDLE, machine.state())
    }

    @Test fun resetReturnsToIdle() {
        val machine = SpeechStateMachine(VadConfig(minSpeechDurationMs = 0))
        machine.accept(1f)
        machine.reset()
        assertEquals(VadState.IDLE, machine.state())
    }
}
