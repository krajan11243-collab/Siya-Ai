package com.siya.ai.ui

import androidx.compose.runtime.Composable

/** Compatibility entry point for older callers. */
@Composable
fun SiyaApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    SiyaProApp(
        microphoneGranted = microphoneGranted,
        onRequestPermissions = onRequestPermissions,
        onStartVoice = onStartVoice,
        onStopVoice = onStopVoice
    )
}
