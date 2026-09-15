package com.siya.ai.vad

/** Pure state machine, kept separate so VAD timing can be unit-tested without ONNX. */
class SpeechStateMachine(private val config: VadConfig = VadConfig()) {
    private var state = VadState.IDLE
    private var speechSamples = 0L
    private var silenceSamples = 0L
    private var positionSamples = 0L

    fun reset() {
        state = VadState.IDLE
        speechSamples = 0
        silenceSamples = 0
        positionSamples = 0
    }

    fun state(): VadState = state

    fun accept(probability: Float, frameSamples: Int = config.windowSamples): VadFrameResult {
        require(frameSamples > 0)
        val p = probability.coerceIn(0f, 1f)
        val frameStart = positionSamples
        positionSamples += frameSamples
        when (state) {
            VadState.IDLE -> {
                if (p >= config.threshold) {
                    speechSamples += frameSamples
                    if (speechSamples >= msToSamples(config.minSpeechDurationMs)) {
                        state = VadState.SPEECH
                        silenceSamples = 0
                        return VadFrameResult(p, state, VadEvent(VadEventType.SPEECH_START, frameStart))
                    }
                } else {
                    speechSamples = 0
                }
            }
            VadState.SPEECH, VadState.ENDING -> {
                if (p >= config.threshold) {
                    state = VadState.SPEECH
                    silenceSamples = 0
                } else {
                    silenceSamples += frameSamples
                    if (silenceSamples >= msToSamples(config.minSilenceDurationMs)) {
                        state = VadState.IDLE
                        speechSamples = 0
                        silenceSamples = 0
                        return VadFrameResult(p, state, VadEvent(VadEventType.SPEECH_END, frameStart))
                    }
                    state = VadState.ENDING
                }
            }
        }
        return VadFrameResult(p, state)
    }

    private fun msToSamples(ms: Long): Long = ms * config.sampleRate / 1000L
}
