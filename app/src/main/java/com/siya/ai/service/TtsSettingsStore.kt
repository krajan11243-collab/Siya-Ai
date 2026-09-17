package com.siya.ai.service

import android.content.Context

/** Persistent Part 6 voice preferences. Values are intentionally bounded for safe on-device use. */
object TtsSettingsStore {
    private const val PREFS = "siya_tts_settings"
    private const val KEY_LOCALE = "locale"
    private const val KEY_RATE = "rate"
    private const val KEY_PITCH = "pitch"

    data class Settings(
        val localeTag: String = "hi-IN",
        val speechRate: Float = 1.0f,
        val pitch: Float = 1.0f,
    )

    fun load(context: Context): Settings {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Settings(
            localeTag = p.getString(KEY_LOCALE, "hi-IN") ?: "hi-IN",
            speechRate = p.getFloat(KEY_RATE, 1.0f).coerceIn(0.5f, 2.0f),
            pitch = p.getFloat(KEY_PITCH, 1.0f).coerceIn(0.5f, 2.0f),
        )
    }

    fun save(context: Context, settings: Settings) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, settings.localeTag)
            .putFloat(KEY_RATE, settings.speechRate.coerceIn(0.5f, 2.0f))
            .putFloat(KEY_PITCH, settings.pitch.coerceIn(0.5f, 2.0f))
            .apply()
    }
}
