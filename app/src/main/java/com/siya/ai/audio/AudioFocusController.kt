package com.siya.ai.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioFocusController(context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private var request: AudioFocusRequest? = null

    fun request(): Boolean {
        if (Build.VERSION.SDK_INT >= 26) {
            val r = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener { }
                .build()
            request = r
            return manager.requestAudioFocus(r) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        @Suppress("DEPRECATION")
        return manager.requestAudioFocus({}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandon() {
        if (Build.VERSION.SDK_INT >= 26) {
            request?.let { manager.abandonAudioFocusRequest(it) }
            request = null
        } else {
            @Suppress("DEPRECATION") manager.abandonAudioFocus(null)
        }
    }
}
