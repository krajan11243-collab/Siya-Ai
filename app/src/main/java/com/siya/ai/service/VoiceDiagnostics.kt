package com.siya.ai.service

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phone-side voice pipeline diagnostics.
 *
 * Stores metadata/events only: no raw microphone PCM and no audio recordings.
 * The log is kept in memory for the current app process and can be shared as plain text.
 */
object VoiceDiagnostics {
    data class Event(
        val time: String,
        val stage: String,
        val event: String,
        val detail: String = "",
    )

    private const val MAX_EVENTS = 300
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val lock = Any()
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    @Volatile
    private var turnId: Long = 0L

    fun newTurn(): Long {
        val id = synchronized(lock) {
            turnId += 1L
            turnId
        }
        log("TURN", "START", "turn=$id")
        return id
    }

    fun log(stage: String, event: String, detail: String = "") {
        val safeDetail = detail.replace("\n", " ").take(700)
        val item = Event(formatter.format(Date()), stage, event, safeDetail)
        synchronized(lock) {
            val next = (_events.value + item)
            _events.value = if (next.size > MAX_EVENTS) next.takeLast(MAX_EVENTS) else next
        }
        android.util.Log.i("SiyaVoiceDiag", "[${item.stage}] ${item.event} ${item.detail}".trim())
    }

    fun clear() {
        synchronized(lock) { _events.value = emptyList() }
        log("DIAGNOSTICS", "CLEARED")
    }

    fun snapshot(): List<Event> = _events.value

    fun exportText(context: Context): String {
        val state = snapshot()
        val header = buildString {
            appendLine("Siya Ai Voice Diagnostics")
            appendLine("Generated: ${Date()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App: ${context.packageName}")
            appendLine("Recording policy: event metadata only; raw microphone audio is not stored.")
            appendLine()
        }
        val body = state.joinToString("\n") {
            buildString {
                append(it.time)
                append(" | ")
                append(it.stage)
                append(" | ")
                append(it.event)
                if (it.detail.isNotBlank()) {
                    append(" | ")
                    append(it.detail)
                }
            }
        }
        return header + body
    }
}
