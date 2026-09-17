package com.siya.ai.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.service.ModelDownloadService
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.service.VoiceSessionState
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.launch

private val Bg = Color(0xFF01050D)
private val Panel = Color(0xFF071321)
private val Purple = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF20D9FF)
private val Blue = Color(0xFF168BFF)
private val Green = Color(0xFF29F0B2)
private val Red = Color(0xFFFF4D78)
private val Muted = Color(0xFF8B98AD)

private data class ChatMsg(val user: Boolean, val text: String, val thinking: Boolean = false)

@Composable
fun SiyaResponsiveAppV3(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf("home") }
    val voice by VoiceSessionState.state.collectAsState()
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            when (page) {
                "models" -> ModelHub { page = "home" }
                "chat" -> ChatScreen({ page = "home" }, { page = "models" })
                "settings" -> SettingsScreen({ page = "home" }, { page = "models" }, { page = "chat" })
                else -> HomeScreen(microphoneGranted, voice, { page = "chat" }, { page = "models" }, { page = "settings" }) {
                    if (!microphoneGranted) onRequestPermissions() else if (voice.active) onStopVoice() else onStartVoice()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(granted: Boolean, voice: VoiceSessionState.State, onChat: () -> Unit, onModels: () -> Unit, onSettings: () -> Unit, onVoice: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderButton(Icons.Default.ChatBubble, Cyan, onChat)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Siya Ai", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("Y O U R   A I   C O M P A N I O N", color = Muted, fontSize = 6.sp, letterSpacing = 1.1.sp)
            }
            Spacer(Modifier.weight(1f))
            HeaderButton(Icons.Default.Settings, Purple, onSettings)
        }
        Text("L I S T E N S   •   U N D E R S T A N D S   •   C O N T R O L S", Modifier.fillMaxWidth(), color = Cyan.copy(.85f), fontSize = 6.sp, letterSpacing = 1.1.sp, textAlign = TextAlign.Center)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Hologram(voice.active) }
        val status = when (voice.phase) {
            VoiceSessionState.Phase.LISTENING -> "Listening"
            VoiceSessionState.Phase.TRANSCRIBING -> "Understanding"
            VoiceSessionState.Phase.THINKING -> "Thinking…"
            VoiceSessionState.Phase.READY -> "Ready"
            VoiceSessionState.Phase.ERROR -> "Needs attention"
            VoiceSessionState.Phase.IDLE -> "Ready for you"
        }
        Text(status, Modifier.fillMaxWidth(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        voice.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 5.dp), color = Red, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2) }
        Spacer(Modifier.height(5.dp))
        Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth(), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Surface(Modifier.size(82.dp).align(Alignment.CenterHorizontally).clickable(onClick = onVoice), CircleShape, color = if (voice.active) Purple.copy(.25f) else Panel, border = BorderStroke(2.dp, if (voice.active) Cyan else Purple)) {
            Icon(if (voice.active) Icons.Default.Stop else Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.padding(21.dp))
        }
        Text(if (!granted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth(), color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onModels, Modifier.fillMaxWidth().height(40.dp), border = BorderStroke(1.dp, Cyan.copy(.5f)), shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Memory, null, tint = Cyan, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Offline Models", color = Color.White, fontSize = 11.sp) }
        Spacer(Modifier.height(6.dp))
        Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth(), color = Blue, fontSize = 6.sp, letterSpacing = 1.7.sp, textAlign = TextAlign.Center)
    }
}

@Composable private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), RoundedCornerShape(14.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(.7f))) { Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp)) }
}

@Composable private fun Hologram(active: Boolean) {
    Canvas(Modifier.fillMaxSize().padding(18.dp)) {
        val c = center
        val r = size.minDimension * .27f
        drawCircle(Cyan.copy(.10f), r * 1.45f, c)
        drawCircle(Cyan.copy(.9f), r, c, style = Stroke(4f))
        drawCircle(Purple.copy(.7f), r * .68f, c, style = Stroke(2f))
        drawOval(Blue.copy(.7f), androidx.compose.ui.geometry.Rect(c.x - r * 1.35f, c.y - r * .28f, c.x + r * 1.35f, c.y + r * .28f), style = Stroke(2f))
        drawArc(Purple, -150f, 95f, false, androidx.compose.ui.geometry.Rect(c.x-r*1.25f,c.y-r*1.25f,c.x+r*1.25f,c.y+r*1.25f), style = Stroke(4f, cap = StrokeCap.Round))
        drawArc(Cyan, 25f, 105f, false, androidx.compose.ui.geometry.Rect(c.x-r*1.25f,c.y-r*1.25f,c.x+r*1.25f,c.y+r*1.25f), style = Stroke(4f, cap = StrokeCap.Round))
        drawCircle(if (active) Cyan else Blue, 6f, c)
        drawOval(Purple.copy(.7f), androidx.compose.ui.geometry.Rect(c.x-r*1.6f,c.y+r*1.25f,c.x+r*1.6f,c.y+r*1.55f), style = Stroke(2f))
    }
}

@Composable private fun ModelHub(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llm = remember { LlmModelStore(context) }
    val vad = remember { VadModelStore(context) }
    val stt = remember { SttModelStore(context) }
    var installed by remember { mutableStateOf(llm.isInstalled()) }
    var downloading by remember { mutableStateOf(ModelDownloadService.prefs(context).getBoolean(ModelDownloadService.KEY_ACTIVE, false)) }
    var done by remember { mutableStateOf(ModelDownloadService.prefs(context).getLong(ModelDownloadService.KEY_DONE, 0L)) }
    var total by remember { mutableStateOf(ModelDownloadService.prefs(context).getLong(ModelDownloadService.KEY_TOTAL, 0L)) }
    var speechBusy by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(downloading) {
        while (downloading) {
            kotlinx.coroutines.delay(350)
            downloading = ModelDownloadService.prefs(context).getBoolean(ModelDownloadService.KEY_ACTIVE, false)
            done = ModelDownloadService.prefs(context).getLong(ModelDownloadService.KEY_DONE, 0L)
            total = ModelDownloadService.prefs(context).getLong(ModelDownloadService.KEY_TOTAL, 0L)
            installed = llm.isInstalled()
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("AI Model Hub", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("Real available offline models • no demo cards", color = Muted, fontSize = 9.sp)
            }
            Surface(Modifier.size(40.dp), RoundedCornerShape(13.dp), color = Panel, border = BorderStroke(1.dp, Cyan.copy(.55f))) { Icon(Icons.Default.Memory, null, tint = Cyan, modifier = Modifier.padding(9.dp)) }
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 5.dp, bottom = 12.dp)) {
            item { Text("LOCAL LLM", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp) }
            item {
                QwenCard(
                    installed = installed,
                    downloading = downloading,
                    done = done,
                    total = total,
                    onDownload = { downloading = true; ModelDownloadService.startQwen(context) },
                    onCancel = { ModelDownloadService.cancelQwen(context); downloading = false },
                    onDelete = { if (llm.delete()) { installed = false; message = "Qwen model deleted" } },
                )
            }
            item { Text("VOICE MODELS", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 7.dp)) }
            item { SpeechCard("Silero VAD", "~2 MB", "Speech detection", vad.isInstalled(), speechBusy == "vad") { speechBusy = "vad"; scope.launch { runCatching { VoiceModelInstaller(vad, stt).downloadVad() }.onFailure { message = it.message }.onSuccess { message = "Silero VAD ready" }; speechBusy = "" } } }
            item { SpeechCard("Hindi STT • Sherpa-ONNX", "~80 MB", "Hindi + Hinglish speech-to-text", stt.isInstalled(), speechBusy == "stt") { speechBusy = "stt"; scope.launch { runCatching { VoiceModelInstaller(vad, stt).downloadHindiStt() }.onFailure { message = it.message }.onSuccess { message = "Hindi STT ready" }; speechBusy = "" } } }
            item { TtsStatusCard() }
            message?.let { item { Text(it, color = if (it.contains("ready", true)) Green else Red, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp)) } }
        }
    }
}

@Composable private fun QwenCard(installed: Boolean, downloading: Boolean, done: Long, total: Long, onDownload: () -> Unit, onCancel: () -> Unit, onDelete: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = Panel, border = BorderStroke(1.3.dp, Purple.copy(.7f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(54.dp), RoundedCornerShape(15.dp), color = Color(0xFF130C2C), border = BorderStroke(1.dp, Purple.copy(.8f))) { QwenLogo(Modifier.fillMaxSize().padding(8.dp)) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Qwen 2.5 1.5B", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(7.dp)); Badge("LLM", Cyan) }
                    Text("Balanced local coding & chat brain", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    Text("~1.12 GB  •  RAM ~3 GB  •  GGUF  •  Offline", color = Cyan.copy(.8f), fontSize = 8.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
            Spacer(Modifier.height(9.dp))
            when {
                installed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.weight(1f).height(40.dp), RoundedCornerShape(12.dp), color = Green.copy(.13f), border = BorderStroke(1.dp, Green.copy(.8f))) { Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Downloaded • Ready", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                    Spacer(Modifier.width(7.dp)); IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Delete", tint = Red) }
                }
                downloading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val pct = if (total > 0) (done * 100 / total).toInt() else 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Downloading…", color = Cyan, fontSize = 9.sp); Text("$pct%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        LinearProgressIndicator({ if (total > 0) done.toFloat() / total else 0f }, Modifier.fillMaxWidth().height(5.dp), color = Cyan, trackColor = Color.White.copy(.08f))
                        Text(if (total > 0) "${done / 1_048_576} / ${total / 1_048_576} MB" else "Preparing download…", color = Muted, fontSize = 8.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Spacer(Modifier.width(7.dp)); IconButton(onClick = onCancel, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.StopCircle, "Stop download", tint = Red) }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Verified GGUF • SHA-256", color = Muted, fontSize = 8.sp, modifier = Modifier.weight(1f))
                    Button(onClick = onDownload, Modifier.height(40.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue), contentPadding = PaddingValues(horizontal = 14.dp)) { Icon(Icons.Default.Download, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Download", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable private fun QwenLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * .31f
        val c = center
        drawArc(Cyan, 20f, 135f, false, androidx.compose.ui.geometry.Rect(c.x-r,c.y-r,c.x+r,c.y+r), style = Stroke(4f, cap = StrokeCap.Round))
        drawArc(Purple, 205f, 135f, false, androidx.compose.ui.geometry.Rect(c.x-r,c.y-r,c.x+r,c.y+r), style = Stroke(4f, cap = StrokeCap.Round))
        drawLine(Cyan, androidx.compose.ui.geometry.Offset(c.x-r,c.y), androidx.compose.ui.geometry.Offset(c.x+r,c.y), strokeWidth = 3f, cap = StrokeCap.Round)
        drawCircle(Color.White, 3f, c)
    }
}

@Composable private fun SpeechCard(name: String, size: String, desc: String, installed: Boolean, busy: Boolean, onDownload: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), color = Panel, border = BorderStroke(1.dp, Cyan.copy(.3f))) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(43.dp), RoundedCornerShape(12.dp), color = Cyan.copy(.08f), border = BorderStroke(1.dp, Cyan.copy(.55f))) { Icon(Icons.Default.RecordVoiceOver, null, tint = Cyan, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(desc, color = Muted, fontSize = 9.sp); Text(size + "  •  On-device", color = Cyan.copy(.75f), fontSize = 8.sp) }
            if (installed) Text("READY", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            else Button(onClick = onDownload, enabled = !busy, Modifier.height(34.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 10.dp)) { if (busy) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp) else Text("Download", fontSize = 9.sp) }
        }
    }
}

@Composable private fun TtsStatusCard() {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), color = Panel, border = BorderStroke(1.dp, Purple.copy(.3f))) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.RecordVoiceOver, null, tint = Purple, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Part 6 • Local TTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("Adapter ready; Android local TTS now speaks Siya replies. Kokoro backend remains swappable.", color = Muted, fontSize = 8.sp) }; Text("READY", color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun ChatScreen(onBack: () -> Unit, onModels: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMsg>() }
    val list = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val installed = store.isInstalled()
    DisposableEffect(Unit) { onDispose { engine.close() } }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }
    Scaffold(containerColor = Bg, contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = {
        Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Talk to Siya…", color = Muted) }, maxLines = 4, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            IconButton(enabled = input.isNotBlank() && installed && !busy, onClick = { val prompt = input.trim(); input = ""; messages += ChatMsg(true, prompt); messages += ChatMsg(false, "Thinking…", true); busy = true; scope.launch { try { val result = engine.complete(prompt); val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = ChatMsg(false, result.text.ifBlank { "No response generated." }) } catch (e: Throwable) { val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = ChatMsg(false, "Local AI error: ${e.message ?: "model unavailable"}") } finally { busy = false } } }) { Icon(Icons.Default.Send, null, tint = if (input.isNotBlank() && installed && !busy) Cyan else Muted) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Column(Modifier.weight(1f)) { Text("Siya Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(if (installed) "Qwen 2.5 1.5B • Offline" else "Install Qwen from Models", color = if (installed) Cyan else Muted, fontSize = 8.sp) }; TextButton(onClick = onModels) { Text("MODELS", color = Cyan, fontSize = 8.sp) } }
            LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                if (messages.isEmpty()) item { Text(if (installed) "Private offline chat is ready." else "Download the Qwen model to start offline chat.", Modifier.fillMaxWidth().padding(top = 50.dp), color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp) }
                items(messages) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.user) Arrangement.End else Arrangement.Start) { Surface(color = if (m.user) Purple.copy(.22f) else Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (m.user) Purple.copy(.4f) else Color.White.copy(.04f))) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { if (m.thinking) { CircularProgressIndicator(Modifier.size(13.dp), strokeWidth = 2.dp, color = Cyan); Spacer(Modifier.width(6.dp)) }; Text(m.text, color = Color.White, fontSize = 12.sp) } } } }
            }
        }
    }
}

@Composable private fun SettingsScreen(onBack: () -> Unit, onModels: () -> Unit, onChat: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(17.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("Settings", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        SettingRow("AI Models", "Qwen 2.5 1.5B + voice models", Icons.Default.Memory, onModels)
        SettingRow("Chat", "Offline local conversation", Icons.Default.ChatBubble, onChat)
        SettingRow("Voice", "VAD • Hindi STT • Part 6 TTS", Icons.Default.RecordVoiceOver) {}
        Spacer(Modifier.weight(1f)); Text("Siya Ai • On-device architecture", Modifier.fillMaxWidth(), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

@Composable private fun SettingRow(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), RoundedCornerShape(15.dp), color = Panel, border = BorderStroke(1.dp, Color.White.copy(.05f))) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Cyan, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(sub, color = Muted, fontSize = 8.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted) } } }

@Composable private fun Badge(text: String, color: Color) { Surface(shape = RoundedCornerShape(8.dp), color = color.copy(.08f), border = BorderStroke(1.dp, color.copy(.7f))) { Text(text, color = color, fontSize = 7.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) } }
