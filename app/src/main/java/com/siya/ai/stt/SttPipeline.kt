package com.siya.ai.stt

import com.siya.ai.vad.VadEventType
import com.siya.ai.vad.VadFrameResult
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Connects Part 3 VAD events to Part 4 offline ASR.
 * Audio frames are retained only while an utterance is active.
 */
class SttPipeline(
    private val executor: SttExecutor,
    private val segment: SpeechSegmentBuffer = SpeechSegmentBuffer(),
    private val onResult: (Result<SttResult>) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private var pending: Future<*>? = null

    fun onAudio(samples: ShortArray, length: Int) {
        if (closed.get()) return
        segment.append(samples, length)
    }

    fun onVad(result: VadFrameResult) {
        if (closed.get()) return
        when (result.event?.type) {
            VadEventType.SPEECH_START -> segment.start()
            VadEventType.SPEECH_END -> {
                val utterance = segment.finish()
                if (utterance.isEmpty()) return
                pending?.cancel(false)
                pending = executor.submit(utterance, utterance.size, onResult)
            }
            null -> Unit
        }
    }

    /** Cancel an already-finished ASR job while preserving the current live utterance. */
    fun cancelPending() {
        pending?.cancel(true)
        pending = null
    }

    fun reset() {
        cancelPending()
        segment.reset()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            pending?.cancel(true)
            pending = null
            segment.reset()
        }
    }
}
