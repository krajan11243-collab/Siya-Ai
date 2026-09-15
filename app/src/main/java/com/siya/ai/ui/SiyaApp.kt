package com.siya.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF02040A)
private val Panel = Color(0xFF0A1020)
private val Panel2 = Color(0xFF10192C)
private val Purple = Color(0xFF8B5CF6)
private val Blue = Color(0xFF02A3FC)
private val Cyan = Color(0xFF22D3EE)
private val Muted = Color(0xFF929BB0)

@Composable
fun SiyaApp(microphoneGranted: Boolean, onRequestPermissions: () -> Unit, onStartVoice: () -> Unit, onStopVoice: () -> Unit) {
    var running by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Black, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Black) {
            when (page) {
                "settings" -> SettingsScreen { page = "home" }
                "models" -> ModelsScreen { page = "home" }
                "chat" -> ChatScreen { page = "home" }
                else -> HomeScreen(microphoneGranted, running, { page = "settings" }, { page = "models" }, { page = "chat" }, onRequestPermissions) {
                    if (running) onStopVoice() else onStartVoice()
                    running = !running
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(micGranted: Boolean, running: Boolean, onSettings: () -> Unit, onModels: () -> Unit, onChat: () -> Unit, requestMic: () -> Unit, toggleVoice: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Black).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Siya Ai", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                Text("YOUR PRIVATE AI COMPANION", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            IconButton(onClick = onSettings) {
                Surface(Modifier.size(46.dp), CircleShape, color = Panel, border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(.65f))) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.padding(11.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("LISTENS   •   UNDERSTANDS   •   CONTROLS", color = Blue, fontSize = 10.sp, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(8.dp))

        Box(Modifier.fillMaxWidth().height(390.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(285.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Blue.copy(.25f), Purple.copy(.15f), Color.Transparent))))
            Box(Modifier.size(258.dp).border(2.dp, Brush.sweepGradient(listOf(Cyan, Blue, Purple, Cyan)), CircleShape))
            Box(Modifier.size(218.dp).border(1.dp, Purple.copy(.9f), CircleShape))
            Box(Modifier.size(174.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF173B67), Color(0xFF070B16), Color.Transparent))).border(1.dp, Cyan.copy(.75f), CircleShape))
            Box(Modifier.width(2.dp).height(330.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Blue, Purple, Color.Transparent))))
            Box(Modifier.width(330.dp).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Blue, Purple, Color.Transparent))))
            Box(Modifier.width(290.dp).height(105.dp).border(1.dp, Purple.copy(.7f), CircleShape))
            Box(Modifier.width(315.dp).height(145.dp).border(1.dp, Blue.copy(.5f), CircleShape))
            Box(Modifier.size(16.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color.White, Cyan, Blue))))
        }

        Text(if (running) "Listening for you..." else "Tap to speak", Modifier.fillMaxWidth(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text("Hindi • Hinglish • English", Modifier.fillMaxWidth(), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeAction("Chat", "Offline AI chat", Icons.Default.ChatBubble, Purple, Modifier.weight(1f), onChat)
            HomeAction("AI Models", "Offline model manager", Icons.Default.Memory, Blue, Modifier.weight(1f), onModels)
        }
        Spacer(Modifier.height(15.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(Modifier.size(82.dp).clickable { if (micGranted) toggleVoice() else requestMic() }, CircleShape, color = if (running) Purple else Panel2, border = androidx.compose.foundation.BorderStroke(2.dp, if (running) Blue else Purple.copy(.8f))) {
                Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, if (running) "Stop" else "Speak", tint = Color.White, modifier = Modifier.padding(24.dp))
            }
        }
        Text(if (!micGranted) "Allow microphone" else if (running) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth(), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = Panel, border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(.18f))) {
            Column(Modifier.padding(16.dp)) { Text("Aapki privacy, hamari priority", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); Text("Offline • Secure • On your device", color = Muted, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun HomeAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), RoundedCornerShape(20.dp), color = Panel, border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(.45f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), RoundedCornerShape(13.dp), color = accent.copy(.13f)) { Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 10.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable private fun ChatScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Black).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Column { Text("Siya Chat", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Local LLM • Offline", color = Muted, fontSize = 11.sp) } }
        Spacer(Modifier.height(24.dp)); Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Offline chat\nLocal LLM testing screen", color = Muted, textAlign = TextAlign.Center) }
    }
}

@Composable private fun SettingsScreen(onBack: () -> Unit) = SimplePage("Settings", onBack) {
    SettingRow("Chat", "Offline text chat with Local LLM")
    SettingRow("Voice mode", "Voice-first assistant")
    SettingRow("Privacy", "On-device processing")
    SettingRow("Language", "Hindi / Hinglish")
    SettingRow("Background", "Controlled by Android permissions")
    SettingRow("AI Models", "VAD • Hindi STT • Qwen LLM • TTS")
}

@Composable private fun ModelsScreen(onBack: () -> Unit) = SimplePage("AI Models", onBack) {
    ModelRow("VAD", "Silero VAD • 16 kHz • ONNX", "Offline voice activity detection")
    ModelRow("Hindi STT", "IndicConformer • int8 ONNX", "Hindi speech-to-text")
    ModelRow("Local LLM", "Qwen 2.5 1.5B • Q4_K_M GGUF", "Offline language model • ~1.12 GB")
    ModelRow("TTS", "Hindi offline engine", "Text-to-speech")
}

@Composable private fun ModelRow(title: String, model: String, detail: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("NOT INSTALLED", color = Color(0xFFFF8B9E), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp)); Text(model, color = Purple, fontSize = 12.sp); Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable private fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Black).padding(18.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(20.dp)); content()
    }
}

@Composable private fun SettingRow(title: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(17.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(value, color = Muted, fontSize = 12.sp) }
    }
}
