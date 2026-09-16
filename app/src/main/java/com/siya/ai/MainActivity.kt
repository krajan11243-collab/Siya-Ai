package com.siya.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.siya.ai.service.SiyaVoiceService
import com.siya.ai.ui.SiyaFastApp

class MainActivity : ComponentActivity() {
    private var microphoneGranted by mutableStateOf(false)

    private val microphoneLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { microphoneGranted = it }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        // Ask for notifications immediately so the foreground voice-service notification can work.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            SiyaFastApp(
                microphoneGranted = microphoneGranted,
                onRequestMicrophone = ::requestMicrophone,
                onStartVoice = ::startVoiceService,
                onStopVoice = ::stopVoiceService,
            )
        }
    }

    private fun requestMicrophone() {
        if (microphoneGranted) return
        microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startVoiceService() {
        if (!microphoneGranted) return
        ContextCompat.startForegroundService(this, Intent(this, SiyaVoiceService::class.java))
    }

    private fun stopVoiceService() {
        stopService(Intent(this, SiyaVoiceService::class.java))
    }
}
