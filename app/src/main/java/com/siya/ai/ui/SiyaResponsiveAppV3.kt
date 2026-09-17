package com.siya.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

private val Bg = Color(0xFF030712)
private val Panel = Color(0xFF071321)
private val Purple = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF20D9FF)
private val Blue = Color(0xFF168BFF)
private val Green = Color(0xFF29F0B2)
private val Red = Color(0xFFFF4D78)
private val Muted = Color(0xFF8B98AD)

private data class ChatMsg(val user: Boolean, val text: String)

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
                "chat" -> ChatScreen { page = "home" }
                "settings" -> SettingsScreen({ page = "home" }, { page = "models" }, { page = "chat" })
                else -> HomeScreen(
                    granted = microphoneGranted,
                    voice = voice,
                    onChat = { page = "chat" },
                    onModels = { page = "models" },
                    onSettings = { page = "settings" },
                    onVoice = {
                        if (!microphoneGranted) onRequestPermissions()
                        else if (voice.active) onStopVoice() else onStartVoice()
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    granted: Boolean,
    voice: VoiceSessionState.State,
    onChat: () -> Unit,
    onModels: () -> Unit,
    onSettings: () -> Unit,
    onVoice: () -> Unit,
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(14.dp)) {
        Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderButton(Icons.Default.ChatBubble, Cyan, onChat)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Siya Ai", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Text("YOUR AI COMPANION", color = Muted, fontSize = 7.sp, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.weight(1f))
            HeaderButton(Icons.Default.Settings, Purple, onSettings)
        }
        Text("LISTENS  •  UNDERSTANDS  •  CONTROLS", Modifier.fillMaxWidth(), color = Cyan.copy(.8f), fontSize = 7.sp, letterSpacing = 1.2.sp, textAlign = TextAlign.Center)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Hologram(voice.active) }
        val status = when (voice.phase) {
            VoiceSessionState.Phase.LISTENING -> "Listening"
            VoiceSessionState.Phase.TRANSCRIBING -> "Understanding"
            VoiceSessionState.Phase.THINKING -> "Thinking…"
            VoiceSessionState.Phase.SPEAKING -> "Speaking"
            VoiceSessionState.Phase.INTERRUPTING -> "Interrupting"
            VoiceSessionState.Phase.READY -> "Ready"
            VoiceSessionState.Phase.ERROR -> "Needs attention"
            VoiceSessionState.Phase.IDLE -> "Ready for you"
        }
        Text(status, Modifier.fillMaxWidth(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        voice.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 5.dp), color = Red, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2) }
        Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 5.dp), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Surface(Modifier.size(82.dp).align(Alignment.CenterHorizontally).clickable(onClick = onVoice), CircleShape, color = if (voice.active) Purple.copy(.25f) else Panel, border = BorderStroke(2.dp, if (voice.active) Cyan else Purple)) {
            Icon(if (voice.active) Icons.Default.Stop else Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.padding(21.dp))
        }
        Text(if (!granted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth(), color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onModels, Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Cyan.copy(.5f))) {
            Icon(Icons.Default.Memory, null, tint = Cyan, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Offline Models", color = Color.White)
        }
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), RoundedCornerShape(14.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(.7f))) { Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp)) }
}

@Composable
private fun Hologram(active: Boolean) {
    Canvas(Modifier.fillMaxSize().padding(18.dp)) {
        val c = center
        val r = size.minDimension * .27f
        val box = androidx.compose.ui.geometry.Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        drawCircle(Cyan.copy(.10f), r * 1.45f, c)
        drawCircle(if (active) Cyan else Blue, r, c, style = Stroke(4f))
        drawCircle(Purple.copy(.7f), r * .68f, c, style = Stroke(2f))
        drawArc(Purple, -150f, 95f, false, topLeft = box.topLeft, size = box.size, style = Stroke(4f, cap = StrokeCap.Round))
        drawArc(Cyan, 25f, 105f, false, topLeft = box.topLeft, size = box.size, style = Stroke(4f, cap = StrokeCap.Round))
        drawCircle(Color.White, 4f, c)
    }
}

@Composable
private fun ModelHub(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llm = remember { LlmModelStore(context) }
    val vad = remember { VadModelStore(context) }
    val stt = remember { SttModelStore(context) }
    var installed by remember { mutableStateOf(llm.isInstalled()) }
    var downloading by remember { mutableStateOf(ModelDownloadService.prefs(context).getBoolean(ModelDownloadService.KEY_ACTIVE, false)) }
    var done by remember { mutableStateOf(0L) }
    var total by remember { mutableStateOf(0L) }
    var busy by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(downloading) {
        while (downloading) {
            delay(350)
            downloading = ModelDownloadService.prefs(context).getBoolean(ModelDownloadService.KEY_ACTIVE, false)
            done = ModelDownloadService.prefs(context).getLong(ModelDownloadService.KEY_DONE, 0L)
            total = ModelDownloadService.prefs(context).getLong(ModelDownloadService.KEY_TOTAL, 0L)
            installed = llm.isInstalled()
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)) {
        Row(Modifier.fillMaxWidth().height(55.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) { Text("AI Model Hub", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold); Text("Real offline models", color = Muted, fontSize = 9.sp) }
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            item { ModelCard("Qwen 2.5 1.5B", "Local LLM • ~1.12 GB • GGUF", installed, downloading, done, total, { downloading = true; ModelDownloadService.startQwen(context) }, { ModelDownloadService.cancelQwen(context); downloading = false }, { if (llm.delete()) installed = false }, Purple) }
            item { ModelCard("Silero VAD", "Speech detection • on-device", vad.isInstalled(), false, 0, 0, { busy = "vad"; scope.launch { runCatching { VoiceModelInstaller(vad, stt).downloadVad() }.onFailure { message = it.message }.onSuccess { message = "Silero VAD ready" }; busy = "" } }, {}, {}, Cyan, busy == "vad") }
            item { ModelCard("Hindi STT • Sherpa-ONNX", "Hindi + Hinglish speech-to-text", stt.isInstalled(), false, 0, 0, { busy = "stt"; scope.launch { runCatching { VoiceModelInstaller(vad, stt).downloadHindiStt() }.onFailure { message = it.message }.onSuccess { message = "Hindi STT ready" }; busy = "" } }, {}, {}, Blue, busy == "stt") }
            item { ModelCard("Local TTS", "Part 6 adapter • Android fallback + neural backend", true, false, 0, 0, {}, {}, {}, Green) }
            message?.let { item { Text(it, color = if (it.contains("ready", true)) Green else Red, fontSize = 9.sp) } }
        }
    }
}

@Composable
private fun ModelCard(
    name: String, subtitle: String, installed: Boolean, downloading: Boolean, done: Long, total: Long,
    onDownload: () -> Unit, onCancel: () -> Unit, onDelete: () -> Unit, accent: Color, busy: Boolean = false,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(.45f))) {
        Column(Modifier.padding(13.dp)) {
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(9.dp))
            when {
                downloading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val pct = if (total > 0) (done * 100 / total).toInt() else 0
                        Text("Downloading $pct%", color = Cyan, fontSize = 9.sp)
                        LinearProgressIndicator(progress = { if (total > 0) done.toFloat() / total else 0f }, Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                    IconButton(onClick = onCancel) { Icon(Icons.Default.StopCircle, "Stop", tint = Red) }
                }
                installed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("READY", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f)); if (name.startsWith("Qwen")) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Red) }
                }
                else -> Button(onClick = onDownload, enabled = !busy, shape = RoundedCornerShape(11.dp)) { if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp) else { Icon(Icons.Default.Download, null, Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("Download") } }
            }
        }
    }
}

@Composable
private fun ChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val messages = remember { mutableStateListOf<ChatMsg>() }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val installed = store.isInstalled()
    DisposableEffect(Unit) { onDispose { engine.close() } }
    Scaffold(containerColor = Bg, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Talk to Siya…") }, maxLines = 4, shape = RoundedCornerShape(16.dp))
            IconButton(enabled = installed && input.isNotBlank() && !busy, onClick = {
                val prompt = input.trim(); input = ""; messages += ChatMsg(true, prompt); busy = true
                scope.launch { try { messages += ChatMsg(false, engine.complete(prompt).text) } catch (e: Throwable) { messages += ChatMsg(false, "Local AI error: ${e.message ?: "model unavailable"}") } finally { busy = false } }
            }) { Icon(Icons.Default.Send, "Send", tint = if (installed && input.isNotBlank() && !busy) Cyan else Muted) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Column { Text("Siya Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(if (installed) "Qwen • Offline" else "Install Qwen from Models", color = Muted, fontSize = 8.sp) } }
            LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }
            LazyColumn(Modifier.fillMaxSize(), state = list, verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                if (messages.isEmpty()) item { Text(if (installed) "Private offline chat is ready." else "Download Qwen to start.", Modifier.fillMaxWidth().padding(top = 50.dp), color = Muted, textAlign = TextAlign.Center) }
                items(messages) { msg -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.user) Arrangement.End else Arrangement.Start) { Surface(color = if (msg.user) Purple.copy(.22f) else Panel, shape = RoundedCornerShape(16.dp)) { Text(msg.text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(11.dp)) } } }
            }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onModels: () -> Unit, onChat: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Text("Settings", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        Setting("AI Models", "Qwen + VAD + Hindi STT", Icons.Default.Memory, onModels)
        Setting("Chat", "Offline local conversation", Icons.Default.ChatBubble, onChat)
        Setting("Voice", "Full duplex • barge-in • local TTS", Icons.Default.RecordVoiceOver) {}
        Spacer(Modifier.weight(1f)); Text("Siya Ai • privacy-first on-device architecture", Modifier.fillMaxWidth(), color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Setting(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), RoundedCornerShape(15.dp), color = Panel, border = BorderStroke(1.dp, Color.White.copy(.05f))) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Cyan, Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = Muted, fontSize = 8.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted) }
    }
}
