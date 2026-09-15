package com.siya.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SiyaBlack = Color(0xFF05060A)
private val SiyaPanel = Color(0xFF10131A)
private val SiyaPanel2 = Color(0xFF171B24)
private val SiyaAccent = Color(0xFF8B5CF6)
private val SiyaAccentSoft = Color(0xFFB794FF)
private val SiyaTextMuted = Color(0xFF8D96A8)
private val SiyaSuccess = Color(0xFF38D996)

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
    Column(
        Modifier
            .fillMaxSize()
            .background(SiyaBlack)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Siya Ai", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Private intelligence, on your device", color = SiyaTextMuted, fontSize = 12.sp)
            }
            Surface(
                modifier = Modifier.size(46.dp).clickable(onClick = onSettings),
                shape = CircleShape,
                color = SiyaPanel
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("●  ${if (running) "LISTENING" else "READY"}", if (running) SiyaAccentSoft else SiyaSuccess)
            StatusPill("OFFLINE", SiyaTextMuted)
            StatusPill(if (microphoneGranted) "MIC ON" else "MIC REQUIRED", SiyaTextMuted)
        }

        Spacer(Modifier.height(18.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SiyaPanel)
        ) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (running) "I'm listening" else "Talk to Siya",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    if (running) "Speak naturally. Tap the orb to stop."
                    else "Tap the orb and start speaking",
                    color = SiyaTextMuted,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(26.dp))

                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .clip(CircleShape)
                        .background(
                            if (running) Brush.radialGradient(listOf(SiyaAccentSoft, SiyaAccent, Color(0xFF37215E)))
                            else Brush.radialGradient(listOf(Color(0xFF252036), SiyaPanel2))
                        )
                        .border(
                            width = 1.dp,
                            color = if (running) SiyaAccentSoft.copy(alpha = .65f) else Color(0xFF292E3A),
                            shape = CircleShape
                        )
                        .clickable(onClick = {
                            if (microphoneGranted) onToggleVoice() else onRequestPermissions()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = if (running) .18f else .08f)
                    ) {
                        Icon(
                            if (running) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (running) "Stop Siya" else "Start Siya",
                            tint = Color.White,
                            modifier = Modifier.padding(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(17.dp))
                Text(
                    if (microphoneGranted) {
                        if (running) "Listening • tap to stop" else "Tap to start"
                    } else "Tap to allow microphone",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            onClick = onModels,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SiyaPanel)
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = SiyaAccent.copy(alpha = .14f)) {
                    Icon(Icons.Default.Memory, null, tint = SiyaAccentSoft, modifier = Modifier.padding(11.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("AI Models", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text("VAD  •  Hindi STT  •  Local LLM  •  TTS", color = SiyaTextMuted, fontSize = 11.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = SiyaTextMuted)
            }
        }

        Spacer(Modifier.weight(1f))

        if (!microphoneGranted) {
            TextButton(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Allow microphone access", color = SiyaAccentSoft, fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            "Private by design • Audio stays on your device",
            Modifier.fillMaxWidth(),
            color = SiyaTextMuted,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatusPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = SiyaPanel,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .05f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    SimplePage("Settings", onBack) {
        SettingRow("Voice mode", "Voice-first assistant")
        SettingRow("Privacy", "On-device processing target")
        SettingRow("Language", "Hindi / Hinglish")
        SettingRow("Background", "Controlled by Android permissions")
    }
}

@Composable
private fun ModelsScreen(onBack: () -> Unit) {
    SimplePage("AI Models", onBack) {
        SettingRow("VAD", "Not installed yet")
        SettingRow("Hindi STT", "Not installed yet")
        SettingRow("Local LLM", "Not installed yet")
        SettingRow("TTS", "Not installed yet")
    }
}

@Composable
private fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SiyaPanel)
    ) {
        Column(Modifier.padding(17.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(value, color = SiyaTextMuted, fontSize = 12.sp)
        }
    }
}
