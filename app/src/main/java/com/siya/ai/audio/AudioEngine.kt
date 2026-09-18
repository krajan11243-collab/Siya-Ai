package com.siya.ai.audio

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

class AudioEngine(
    context: Context,
    private val config: AudioEngineConfig = AudioEngineConfig(),
    private val onPcm: ((ShortArray, Int) -> Unit)? = null
) {
    private val effects = AudioEffectsController()
    private val input = AudioInput(context, config, effects)
    private val output = AudioOutput(config)
    private val focus = AudioFocusController(context)
    private val running = AtomicBoolean(false)

    @Volatile var lastRms: Float = 0f
        private set
    @Volatile var lastPeak: Float = 0f
        private set

    fun start(): Boolean {
        if (running.get()) return true
        if (!focus.request()) return false
        // Voice input must not depend on speaker-output initialization.
        // TTS owns its own playback path; requiring AudioTrack here can make
        // a valid microphone unusable on some devices/routes.
        output.initialize()
        val started = input.start(Pcm16Listener { pcm, length, _ ->
            var sum = 0.0
            var peak = 0
            for (i in 0 until length) {
                val value = kotlin.math.abs(pcm[i].toInt())
                sum += value.toDouble() * value.toDouble()
                if (value > peak) peak = value
            }
            lastRms = if (length == 0) 0f else (kotlin.math.sqrt(sum / length) / Short.MAX_VALUE).toFloat()
            lastPeak = peak.toFloat() / Short.MAX_VALUE
            onPcm?.invoke(pcm, length)
        })
        if (!started) {
            output.release()
            focus.abandon()
            return false
        }
        running.set(true)
        return true
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        input.stop()
        effects.release()
        output.stop()
        output.flush()
        focus.abandon()
    }

    fun release() {
        running.set(false)
        input.release()
        output.release()
        effects.release()
        focus.abandon()
    }

    fun isRunning(): Boolean = running.get() && input.isRunning
    fun isOutputReady(): Boolean = output.isReady
    fun config(): AudioEngineConfig = config
}
