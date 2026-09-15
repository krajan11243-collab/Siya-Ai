package com.siya.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SiyaBlack = Color(0xFF05060A)
private val SiyaPanel = Color(0xFF10131A)
private val SiyaPanel2 = Color(0xFF171B24)
private val SiyaAccent = Color(0xFF8B5CF6)
private val SiyaTextMuted = Color(0xFF8D96A8)

@Composable
fun SiyaApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    var running by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }

    MaterialTheme(colorScheme = darkColorScheme(primary = SiyaAccent, background = SiyaBlack, surface = SiyaPanel)) {
        Surface(Modifier.fillMaxSize(), color = SiyaBlack) {
            when (page) {
                "settings" -> SettingsScreen(onBack = { page = "home" })
                "models" -> ModelsScreen(onBack = { page = "home" })
                else -> HomeScreen(
                    microphoneGranted = microphoneGranted,
                    running = running,
                    onSettings = { page = "settings" },
                    onModels = { page = "models" },
                    onRequestPermissions = onRequestPermissions,
                    onToggleVoice = {
                        if (running) onStopVoice() else onStartVoice()
                        running = !running
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    microphoneGranted: Boolean,
    running: Boolean,
    onSettings: () -> Unit,
    onModels: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleVoice: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Siya Ai", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Private intelligence, on your device", color = SiyaTextMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
        }

        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = SiyaPanel)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (running) "Listening" else "Ready when you are", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(if (microphoneGranted) "Voice engine is under your control" else "Microphone permission is required", color = SiyaTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(26.dp))
                Surface(Modifier.size(142.dp), shape = CircleShape, color = if (running) SiyaAccent else SiyaPanel2) {
                    Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, null, Modifier.padding(42.dp), tint = Color.White)
                }
                Spacer(Modifier.height(18.dp))
                Text(if (running) "Tap to stop" else "Tap to talk", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard("OFFLINE", "Local-first", Modifier.weight(1f))
            StatusCard("MIC", if (microphoneGranted) "Ready" else "Required", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Card(onClick = onModels, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SiyaPanel)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint = SiyaAccent)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) { Text("AI Models", color = Color.White, fontWeight = FontWeight.SemiBold); Text("Model packs and readiness", color = SiyaTextMuted, fontSize = 12.sp) }
                Icon(Icons.Default.ChevronRight, null, tint = SiyaTextMuted)
            }
        }

        Spacer(Modifier.weight(1f))
        if (!microphoneGranted) {
            Button(onClick = onRequestPermissions, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) { Text("Allow Microphone", fontWeight = FontWeight.Bold) }
        } else {
            Button(onClick = onToggleVoice, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) { Text(if (running) "Stop Siya" else "Start Siya", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Privacy-first • Core AI is planned for on-device execution", Modifier.fillMaxWidth(), color = SiyaTextMuted, fontSize = 11.sp)
    }
}

@Composable private fun StatusCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SiyaPanel)) {
        Column(Modifier.padding(16.dp)) { Text(title, color = SiyaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(value, color = Color.White, fontSize = 13.sp) }
    }
}

@Composable private fun SettingsScreen(onBack: () -> Unit) {
    SimplePage("Settings", onBack) {
        SettingRow("Voice mode", "Voice-first assistant")
        SettingRow("Privacy", "On-device processing target")
        SettingRow("Language", "Hindi / Hinglish")
        SettingRow("Background", "Controlled by Android permissions")
    }
}

@Composable private fun ModelsScreen(onBack: () -> Unit) {
    SimplePage("AI Models", onBack) {
        SettingRow("VAD", "Not installed yet")
        SettingRow("Hindi STT", "Not installed yet")
        SettingRow("Local LLM", "Not installed yet")
        SettingRow("TTS", "Not installed yet")
    }
}

@Composable private fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }; Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(20.dp)); content()
    }
}

@Composable private fun SettingRow(title: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SiyaPanel)) {
        Column(Modifier.padding(17.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(value, color = SiyaTextMuted, fontSize = 12.sp) }
    }
}
