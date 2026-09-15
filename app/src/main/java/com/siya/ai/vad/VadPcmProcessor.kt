package com.siya.ai.vad

/** Converts arbitrary PCM callback sizes into Silero's exact 512-sample frames. */
class VadPcmProcessor(private val vad: SileroVadEngine, private val config: VadConfig = VadConfig()) {
    private val frame = ShortArray(config.windowSamples)
    private var frameSize = 0

    fun reset() {
        frameSize = 0
        vad.reset()
    }

    fun accept(input: ShortArray, length: Int = input.size, onResult: (VadFrameResult) -> Unit) {
        require(length in 0..input.size)
        var offset = 0
        while (offset < length) {
            val copy = minOf(config.windowSamples - frameSize, length - offset)
            System.arraycopy(input, offset, frame, frameSize, copy)
            frameSize += copy
            offset += copy
            if (frameSize == config.windowSamples) {
                onResult(vad.process(frame, config.windowSamples))
                frameSize = 0
            }
        }
    }
}
