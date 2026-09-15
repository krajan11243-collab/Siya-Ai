package com.siya.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SiyaBlack = Color(0xFF030712)
private val SiyaPurple = Color(0xFF8B5CF6)
private val SiyaPanel = Color(0xFF111827)

@Composable
fun SiyaApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    var running by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = SiyaBlack) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Siya Ai", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("On-device voice agent", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(48.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(SiyaPanel).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (!microphoneGranted) "Microphone permission required" else if (running) "Siya is listening" else "Siya is ready",
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(28.dp))
                    Surface(modifier = Modifier.size(128.dp), shape = CircleShape, color = SiyaPurple) {
                        Icon(
                            if (running) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.padding(38.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        if (running) "Voice pipeline active" else "Tap start to begin",
                        color = Color(0xFFD1D5DB), fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                if (!microphoneGranted) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SiyaPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Allow Microphone", fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = {
                            if (running) { onStopVoice() } else { onStartVoice() }
                            running = !running
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SiyaPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text(if (running) "Stop Siya" else "Start Siya", fontWeight = FontWeight.Bold) }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Privacy-first • Processing is designed for on-device execution",
                    color = Color(0xFF6B7280), fontSize = 12.sp
                )
            }
        }
    }
}
