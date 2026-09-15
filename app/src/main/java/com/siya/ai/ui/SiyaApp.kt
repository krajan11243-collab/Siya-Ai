package com.siya.ai.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val SiyaBlack = Color(0xFF02040A)
private val SiyaPanel = Color(0xFF080F1F)
private val SiyaPanel2 = Color(0xFF0D1730)
private val SiyaPurple = Color(0xFF8B5CF6)
private val SiyaBlue = Color(0xFF168BFF)
private val SiyaCyan = Color(0xFF21D4FF)
private val SiyaPink = Color(0xFFB55CFF)
private val SiyaMuted = Color(0xFF8E98AE)

@Composable
fun SiyaApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    var running by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }
    MaterialTheme(colorScheme = darkColorScheme(primary = SiyaPurple, background = SiyaBlack, surface = SiyaPanel)) {
        Surface(Modifier.fillMaxSize(), color = SiyaBlack) {
            when (page) {
                "settings" -> SettingsScreen(onBack = { page = "home" })
                "models" -> ModelsScreen(onBack = { page = "home" })
                "chat" -> ChatScreen(onBack = { page = "home" })
                else -> HomeScreen(
                    microphoneGranted = microphoneGranted,
                    running = running,
                    onSettings = { page = "settings" },
                    onModels = { page = "models" },
                    onChat = { page = "chat" },
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
    onChat: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleVoice: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "hologram")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing)), label = "rotation")
    val pulse by transition.animateFloat(0.82f, 1.08f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "pulse")

    Column(
        Modifier.fillMaxSize().background(SiyaBlack).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Siya Ai", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                Text("YOUR PRIVATE AI COMPANION", color = SiyaMuted, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Surface(
                Modifier.size(48.dp).clickable(onClick = onSettings),
                CircleShape,
                color = SiyaPanel,
                border = androidx.compose.foundation.BorderStroke(1.dp, SiyaPurple.copy(.7f))
            ) { Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.padding(12.dp)) }
        }

        Spacer(Modifier.height(12.dp))
        Text("LISTENS   •   UNDERSTANDS   •   CONTROLS", color = SiyaBlue, fontSize = 10.sp, letterSpacing = 2.sp)

        Box(Modifier.fillMaxWidth().height(470.dp), contentAlignment = Alignment.Center) {
            HologramOrb(rotation = rotation, pulse = pulse)
        }

        Text(if (running) "Listening..." else "Tap to speak", Modifier.fillMaxWidth(), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 5.dp), color = SiyaMuted, fontSize = 13.sp, textAlign = TextAlign.Center)

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("Chat", "Offline AI chat", Icons.Default.ChatBubble, SiyaPurple, Modifier.weight(1f), onChat)
            ActionCard("AI Models", "Offline model manager", Icons.Default.Memory, SiyaBlue, Modifier.weight(1f), onModels)
        }

        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.size(86.dp).clickable { if (microphoneGranted) onToggleVoice() else onRequestPermissions() },
                CircleShape,
                color = if (running) SiyaPurple else SiyaPanel2,
                border = androidx.compose.foundation.BorderStroke(2.dp, if (running) SiyaCyan else SiyaPurple)
            ) { Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, if (running) "Stop" else "Speak", tint = Color.White, modifier = Modifier.padding(25.dp)) }
        }
        Text(if (!microphoneGranted) "Allow microphone" else if (running) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth().padding(top = 7.dp), color = SiyaMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun HologramOrb(rotation: Float, pulse: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = minOf(size.width, size.height) * .36f
        val glow = Brush.radialGradient(listOf(SiyaCyan.copy(.22f), SiyaBlue.copy(.10f), Color.Transparent))
        drawCircle(glow, radius = base * 1.45f, center = Offset(cx, cy))
        rotate(rotation, Offset(cx, cy)) {
            drawOval(SiyaCyan.copy(.75f), Offset(cx - base * 1.25f, cy - base * .38f), Size(base * 2.5f, base * .76f), style = Stroke(2f))
            drawOval(SiyaPurple.copy(.7f), Offset(cx - base * 1.18f, cy - base * .62f), Size(base * 2.36f, base * 1.24f), style = Stroke(1.6f))
            drawArc(SiyaPink.copy(.95f), 18f, 92f, false, Offset(cx - base, cy - base), Size(base * 2f, base * 2f), style = Stroke(5f, cap = StrokeCap.Round))
            drawArc(SiyaCyan.copy(.95f), 202f, 105f, false, Offset(cx - base, cy - base), Size(base * 2f, base * 2f), style = Stroke(5f, cap = StrokeCap.Round))
        }
        val r = base * pulse
        drawCircle(SiyaBlue.copy(.10f), radius = r, center = Offset(cx, cy))
        drawCircle(SiyaCyan.copy(.8f), radius = r, center = Offset(cx, cy), style = Stroke(2f))
        drawCircle(SiyaPurple.copy(.55f), radius = r * .73f, center = Offset(cx, cy), style = Stroke(1.4f))
        drawCircle(SiyaBlue.copy(.35f), radius = r * .48f, center = Offset(cx, cy), style = Stroke(1.2f))
        drawLine(SiyaBlue.copy(.8f), Offset(cx, cy - base * 1.48f), Offset(cx, cy + base * 1.48f), 1.5f)
        drawLine(SiyaPurple.copy(.65f), Offset(cx - base * 1.48f, cy), Offset(cx + base * 1.48f, cy), 1.5f)
        for (i in 0 until 20) {
            val a = Math.toRadians((i * 18.0 + rotation * .15))
            val rr = base * (1.05f + (i % 3) * .12f)
            val x = cx + cos(a).toFloat() * rr
            val y = cy + sin(a).toFloat() * rr
            drawCircle(if (i % 2 == 0) SiyaCyan else SiyaPurple, radius = 2.5f, center = Offset(x, y))
        }
        drawCircle(Color.White, radius = 6f, center = Offset(cx, cy))
        drawCircle(SiyaCyan, radius = 13f, center = Offset(cx, cy), style = Stroke(2f))
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), RoundedCornerShape(22.dp), color = SiyaPanel, border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(.65f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), RoundedCornerShape(14.dp), color = accent.copy(.13f)) { Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = SiyaMuted, fontSize = 10.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = SiyaMuted)
        }
    }
}

@Composable
private fun ChatScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (store.isInstalled()) "Offline model ready" else "Install Qwen model from AI Models") }
    val engine = remember { LocalLlmEngine(store) }
    DisposableEffect(Unit) { onDispose { engine.close() } }

    Column(Modifier.fillMaxSize().background(SiyaBlack).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) { Text("Siya Chat", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text(status, color = SiyaMuted, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { (user, text) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                    Surface(color = if (user) SiyaPurple.copy(.22f) else SiyaPanel, shape = RoundedCornerShape(18.dp)) { Text(text, Modifier.padding(13.dp), color = Color.White, fontSize = 14.sp) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask Siya offline...", color = SiyaMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SiyaPurple, unfocusedBorderColor = SiyaPanel2, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            Spacer(Modifier.width(8.dp))
            IconButton(enabled = input.isNotBlank() && !busy, onClick = {
                val prompt = input.trim(); input = ""; messages += true to prompt; busy = true
                scope.launch {
                    try {
                        if (!store.isInstalled()) error("Qwen model is not installed")
                        engine.load()
                        val result = engine.complete(prompt)
                        messages += false to result.text
                        status = String.format(Locale.US, "Offline • %.1f tok/s", result.tokensPerSecond)
                    } catch (e: Exception) { messages += false to "Offline AI error: ${e.message ?: "model unavailable"}"; status = "Model unavailable" }
                    finally { busy = false }
                }
            }) { Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank() && !busy) SiyaCyan else SiyaMuted) }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    SimplePage("Settings", onBack) {
        SettingRow("Chat", "Offline text chat with Local LLM")
        SettingRow("Voice mode", "Voice-first assistant")
        SettingRow("Privacy", "On-device processing")
        SettingRow("Language", "Hindi / Hinglish / English")
        SettingRow("Background", "Controlled by Android permissions")
        SettingRow("AI Models", "Manage VAD, Hindi STT, Qwen LLM and TTS")
    }
}

@Composable
private fun ModelsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var downloading by remember { mutableStateOf(false) }
    var downloaded by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    SimplePage("AI Models", onBack) {
        ModelRow("VAD", "Silero VAD • 16 kHz • ONNX", "Offline voice detection", "READY")
        ModelRow("Hindi STT", "IndicConformer • int8 ONNX", "Hindi speech-to-text", "READY")
        ModelRow("Local LLM", "Qwen 2.5 1.5B • Q4_K_M GGUF", "Offline language model • ~1.12 GB", if (installed) "INSTALLED" else "NOT INSTALLED")
        if (downloading) {
            LinearProgressIndicator(progress = { if (total > 0) downloaded.toFloat() / total else 0f }, Modifier.fillMaxWidth().padding(vertical = 10.dp), color = SiyaCyan)
            Text("Downloading ${downloaded / (1024 * 1024)} MB / ${if (total > 0) total / (1024 * 1024) else "?"} MB", color = SiyaMuted, fontSize = 11.sp)
        }
        if (!installed) {
            Spacer(Modifier.height(8.dp))
            Button(enabled = !downloading, onClick = {
                error = null; downloading = true; downloaded = 0; total = 0
                scope.launch {
                    try { LlmModelInstaller(store).download { done, size -> downloaded = done; total = size }; installed = store.isInstalled() }
                    catch (e: Exception) { error = e.message ?: "Model download failed" }
                    finally { downloading = false }
                }
            }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SiyaPurple)) { Text(if (downloading) "Installing Qwen…" else "Install Qwen Offline Model") }
        }
        error?.let { Text(it, Modifier.padding(top = 8.dp), color = Color(0xFFFF718B), fontSize = 11.sp) }
        Spacer(Modifier.height(8.dp))
        ModelRow("TTS", "Hindi offline engine", "Text-to-speech", "PLANNED")
    }
}

@Composable
private fun ModelRow(title: String, model: String, detail: String, status: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SiyaPanel)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(status, color = if (status == "INSTALLED" || status == "READY") SiyaCyan else Color(0xFFFF8B9E), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp)); Text(model, color = SiyaPurple, fontSize = 12.sp); Text(detail, color = SiyaMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(SiyaBlack).padding(18.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(20.dp)); content()
    }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = SiyaPanel)) {
        Column(Modifier.padding(17.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(value, color = SiyaMuted, fontSize = 12.sp) }
    }
}
