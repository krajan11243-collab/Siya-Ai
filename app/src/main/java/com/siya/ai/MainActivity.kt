package com.siya.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.siya.ai.service.SiyaVoiceService
import com.siya.ai.ui.SiyaResponsiveApp

class MainActivity : ComponentActivity() {
    private var microphoneGranted by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        microphoneGranted = results[Manifest.permission.RECORD_AUDIO] == true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        setContent {
            SiyaResponsiveApp(
                microphoneGranted = microphoneGranted,
                onRequestPermissions = ::requestRequiredPermissions,
                onStartVoice = ::startVoiceService,
                onStopVoice = ::stopVoiceService
            )
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }

    private fun startVoiceService() {
        if (!microphoneGranted) return
        ContextCompat.startForegroundService(this, Intent(this, SiyaVoiceService::class.java))
    }

    private fun stopVoiceService() {
        stopService(Intent(this, SiyaVoiceService::class.java))
    }
}
