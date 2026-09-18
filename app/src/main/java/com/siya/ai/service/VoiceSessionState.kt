package com.siya.ai.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-local UI state bridge between the foreground voice service and MainActivity. */
object VoiceSessionState {
    data class State(
        val active: Boolean = false,
        val phase: Phase = Phase.IDLE,
        val transcript: String = "",
        val response: String = "",
        val error: String? = null,
    )

    enum class Phase {
        IDLE, LISTENING, TRANSCRIBING, THINKING, SPEAKING, INTERRUPTING, READY, ERROR
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun listening() = _state.value.let { _state.value = it.copy(active = true, phase = Phase.LISTENING, error = null) }
    fun transcribing() = _state.value.let { _state.value = it.copy(active = true, phase = Phase.TRANSCRIBING, error = null) }
    fun thinking(text: String) = _state.value.let { _state.value = it.copy(active = true, phase = Phase.THINKING, transcript = text, error = null) }
    fun speaking(text: String) = _state.value.let { _state.value = it.copy(active = true, phase = Phase.SPEAKING, response = text, error = null) }
    fun interrupting() = _state.value.let { _state.value = it.copy(active = true, phase = Phase.INTERRUPTING, error = null) }
    fun ready(text: String) = _state.value.let { _state.value = it.copy(active = true, phase = Phase.READY, response = text, error = null) }
    fun transcript(text: String) = _state.value.let { _state.value = it.copy(active = true, phase = Phase.LISTENING, transcript = text, error = null) }
    fun error(message: String) = _state.value.let { _state.value = it.copy(active = false, phase = Phase.ERROR, error = message) }
    fun stopped() { _state.value = State() }
}
