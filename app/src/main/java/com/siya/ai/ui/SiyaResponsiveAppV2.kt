package com.siya.ai.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.service.ModelDownloadService
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.service.VoiceSessionState
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

private val V2Bg = Color(0xFF01040B)
private val V2Panel = Color(0xFF07111F)
private val V2Purple = Color(0xFF9B5CFF)
private val V2Blue = Color(0xFF168BFF)
private val V2Cyan = Color(0xFF21D4FF)
private val V2Muted = Color(0xFF8995AB)

private data class V2ChatMessage(val user: Boolean, val text: String, val thinking: Boolean = false)
private data class ModelInfo(
    val number: Int,
    val name: String,
    val description: String,
    val size: String,
    val ram: String,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val qwenBackend: Boolean = false,
)

private val modelCatalog = listOf(
    ModelInfo(1, "GPT-4o Mini", "Fast & capable model", "~3.2 GB", "RAM ~5 GB", Color(0xFF19E6B5), Icons.Default.AutoAwesome),
    ModelInfo(2, "GPT-4o", "Reasoning & chat model", "~7.6 GB", "RAM ~10 GB", V2Cyan, Icons.Default.AutoAwesome),
    ModelInfo(3, "Gemini 1.5 Flash", "Fast multimodal model", "~2.1 GB", "RAM ~4 GB", V2Purple, Icons.Default.Language),
    ModelInfo(4, "Gemini 1.5 Pro", "Advanced multimodal model", "~4.8 GB", "RAM ~7 GB", V2Blue, Icons.Default.Language),
    ModelInfo(5, "Llama 3.2 1B", "Lightweight & mobile friendly", "~1.1 GB", "RAM ~2.5 GB", V2Blue, Icons.Default.AllInclusive),
    ModelInfo(6, "Llama 3.1 8B", "Powerful open source model", "~4.7 GB", "RAM ~7 GB", V2Blue, Icons.Default.AllInclusive),
    ModelInfo(7, "Qwen 2.5 0.5B", "Efficient coding & chat", "~0.8 GB", "RAM ~2 GB", V2Purple, Icons.Default.Code),
    ModelInfo(8, "Qwen 2.5 1.5B", "Balanced coding & chat model", "~1.12 GB", "RAM ~3 GB", V2Purple, Icons.Default.Memory, true),
    ModelInfo(9, "DeepSeek Coder 1.3B", "Specialized for code generation", "~1.3 GB", "RAM ~3 GB", V2Cyan, Icons.Default.Code),
    ModelInfo(10, "DeepSeek Chat 7B", "General purpose chat model", "~4.1 GB", "RAM ~6 GB", V2Cyan, Icons.Default.Chat),
    ModelInfo(11, "Mistral 7B Instruct", "High quality open model", "~4.0 GB", "RAM ~6 GB", V2Purple, Icons.Default.Memory),
    ModelInfo(12, "Mixtral 8x7B", "Advanced mixture of experts", "~26 GB", "RAM ~32 GB", V2Purple, Icons.Default.Memory),
    ModelInfo(13, "Phi 3 Mini", "Small & highly capable", "~2.3 GB", "RAM ~4 GB", V2Cyan, Icons.Default.AutoAwesome),
    ModelInfo(14, "Phi 3 Medium", "Better reasoning & performance", "~5.6 GB", "RAM ~8 GB", V2Purple, Icons.Default.AutoAwesome),
    ModelInfo(15, "Yi 1.5 6B", "Multilingual model", "~3.9 GB", "RAM ~6 GB", V2Purple, Icons.Default.Cube),
)

@Composable
fun SiyaResponsiveAppV2(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf("home") }
    val voice by VoiceSessionState.state.collectAsState()
    MaterialTheme(colorScheme = darkColorScheme(primary = V2Purple, background = V2Bg, surface = V2Panel)) {
        Surface(Modifier.fillMaxSize(), color = V2Bg) {
            when (page) {
                "chat" -> V2Chat({ page = "home" }, { page = "models" })
                "models" -> V2Models { page = "home" }
                "settings" -> V2Settings({ page = "home" }, { page = "chat" }, { page = "models" })
                else -> V2Home(microphoneGranted, voice, { page = "chat" }, { page = "settings" }) {
                    if (!microphoneGranted) onRequestPermissions() else if (voice.active) onStopVoice() else onStartVoice()
                }
            }
        }
    }
}

@Composable
private fun V2Home(
    microphoneGranted: Boolean,
    voice: VoiceSessionState.State,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onVoice: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp, vertical = 4.dp)) {
        val compact = maxHeight < 700.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(if (compact) 50.dp else 54.dp), verticalAlignment = Alignment.CenterVertically) {
                ThinHeaderButton(Icons.Default.ChatBubble, V2Cyan, onChat)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Siya Ai", color = Color.White, fontSize = if (maxWidth < 360.dp) 25.sp else 29.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Y O U R   A I   C O M P A N I O N", color = V2Muted, fontSize = 6.sp, letterSpacing = 1.15.sp)
                }
                Spacer(Modifier.weight(1f))
                ThinHeaderButton(Icons.Default.Settings, V2Purple, onSettings)
            }
            Text("L I S T E N S   •   U N D E R S T A N D S   •   C O N T R O L S", Modifier.fillMaxWidth(), color = V2Cyan.copy(.9f), fontSize = 6.sp, letterSpacing = 1.05.sp, textAlign = TextAlign.Center)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { ProHologram(voice.active, Modifier.fillMaxSize()) }
            val status = when (voice.phase) {
                VoiceSessionState.Phase.LISTENING -> "Listening"
                VoiceSessionState.Phase.TRANSCRIBING -> "Understanding"
                VoiceSessionState.Phase.THINKING -> "Thinking…"
                VoiceSessionState.Phase.READY -> "Ready"
                VoiceSessionState.Phase.ERROR -> "Needs attention"
                VoiceSessionState.Phase.IDLE -> "Tap to Speak"
            }
            Text(status, Modifier.fillMaxWidth(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            voice.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 4.dp), color = Color(0xFFFF7187), fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2) }
            Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 4.dp), color = V2Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Surface(Modifier.size(if (compact) 78.dp else 84.dp).align(Alignment.CenterHorizontally).clickable(onClick = onVoice), CircleShape, color = if (voice.active) V2Purple.copy(.24f) else V2Panel, border = BorderStroke(2.dp, if (voice.active) V2Cyan else V2Purple)) {
                Icon(if (voice.active) Icons.Default.Stop else Icons.Default.Mic, "Microphone", tint = Color.White, modifier = Modifier.padding(21.dp))
            }
            Text(if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth(), color = V2Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
            Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth(), color = V2Blue, fontSize = 6.sp, letterSpacing = 1.7.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ThinHeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), RoundedCornerShape(14.dp), color = V2Panel, border = BorderStroke(1.dp, accent.copy(.75f))) {
        Icon(icon, null, tint = accent, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun V2Chat(onBack: () -> Unit, onModels: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val voice by VoiceSessionState.state.collectAsState()
    val messages = remember { mutableStateListOf<V2ChatMessage>() }
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    DisposableEffect(Unit) { onDispose { engine.close() } }
    LaunchedEffect(messages.size, busy) { if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex) }
    LaunchedEffect(voice.phase, voice.transcript, voice.response, voice.error) {
        when (voice.phase) {
            VoiceSessionState.Phase.THINKING -> { if (voice.transcript.isNotBlank()) messages += V2ChatMessage(true, voice.transcript); messages += V2ChatMessage(false, "Thinking…", true); busy = true }
            VoiceSessionState.Phase.READY -> { val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = V2ChatMessage(false, voice.response); busy = false }
            VoiceSessionState.Phase.ERROR -> { val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = V2ChatMessage(false, voice.error ?: "Voice error"); busy = false }
            else -> Unit
        }
    }
    Scaffold(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), containerColor = V2Bg, contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = {
        Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(9.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask Siya…", color = V2Muted) }, shape = RoundedCornerShape(17.dp), maxLines = 4, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = V2Purple, unfocusedBorderColor = V2Panel, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = V2Cyan))
            IconButton(enabled = input.isNotBlank() && !busy && installed, onClick = {
                val prompt = input.trim(); input = ""; messages += V2ChatMessage(true, prompt); messages += V2ChatMessage(false, "Thinking…", true); busy = true
                scope.launch { try { check(store.isInstalled()) { "Qwen model is not installed" }; val r = engine.complete(prompt); val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = V2ChatMessage(false, r.text.ifBlank { "No response generated." }); installed = true } catch (e: Exception) { val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = V2ChatMessage(false, "Local AI error: ${e.message ?: "model unavailable"}") } finally { busy = false } }
            }) { Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank() && !busy && installed) V2Cyan else V2Muted) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Column(Modifier.weight(1f)) { Text("Siya Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(if (installed) "Qwen 2.5 1.5B • Offline • Ready" else "Local model not installed", color = if (installed) V2Cyan else V2Muted, fontSize = 9.sp) }; OutlinedButton(onClick = onModels, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp), modifier = Modifier.height(34.dp)) { Text("MODEL", fontSize = 8.sp, color = V2Cyan) } }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (messages.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AutoAwesome, null, tint = V2Cyan, modifier = Modifier.size(40.dp)); Text("Talk to Siya", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Fast private offline chat", color = V2Muted, fontSize = 10.sp) } }
                items(messages) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.user) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(17.dp), color = if (m.user) V2Purple.copy(.22f) else V2Panel, border = BorderStroke(1.dp, if (m.user) V2Purple.copy(.4f) else Color.White.copy(.04f))) { Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { if (m.thinking) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = V2Cyan); Spacer(Modifier.width(7.dp)) }; Text(m.text, color = Color.White, fontSize = 13.sp) } } } }
            }
        }
    }
}

@Composable
private fun V2Settings(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(17.dp)) {
        V2PageHeader("Settings", onBack)
        V2Setting("Chat", "Offline text chat with Local LLM", Icons.Default.ChatBubble, onChat)
        V2Setting("AI Models", "Install Qwen + VAD + Hindi STT", Icons.Default.Memory, onModels)
        V2Setting("Voice mode", "Microphone → VAD → Hindi STT → Qwen", Icons.Default.Mic, null)
        V2Setting("Privacy", "Audio and AI inference stay on device", Icons.Default.Lock, null)
        V2Setting("Language", "Hindi / Hinglish / English", Icons.Default.Language, null)
    }
}

@Composable
private fun V2Models(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llmStore = remember { LlmModelStore(context) }
    val vadStore = remember { VadModelStore(context) }
    val sttStore = remember { SttModelStore(context) }
    val installer = remember { VoiceModelInstaller(vadStore, sttStore) }
    var page by rememberSaveable { mutableStateOf("hub") }
    var installed by remember { mutableStateOf(llmStore.isInstalled()) }
    var vadInstalled by remember { mutableStateOf(vadStore.isInstalled()) }
    var sttInstalled by remember { mutableStateOf(sttStore.isInstalled()) }
    var downloading by remember { mutableStateOf(false) }
    var done by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var speechBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val prefs = remember { ModelDownloadService.prefs(context) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { selected -> scope.launch { try { importModelV2(context, selected, llmStore); installed = true; message = "GGUF imported successfully" } catch (e: Exception) { message = e.message ?: "GGUF import failed" } } } }

    LaunchedEffect(Unit) {
        while (true) {
            downloading = prefs.getBoolean(ModelDownloadService.KEY_ACTIVE, false)
            done = prefs.getLong(ModelDownloadService.KEY_DONE, 0L)
            total = prefs.getLong(ModelDownloadService.KEY_TOTAL, 0L)
            if (!downloading) installed = llmStore.isInstalled()
            delay(500)
        }
    }

    if (page == "hub") {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(V2Bg, Color(0xFF020A18), V2Bg))).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 13.dp, vertical = 5.dp)) {
            Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(27.dp)) }; Column(Modifier.weight(1f).padding(start = 5.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("AI", color = V2Cyan, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(6.dp)); Text("Models", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold) }; Text("Download, Select & Use On-Device Models", color = Color.White.copy(.76f), fontSize = 10.sp) }; Surface(Modifier.size(44.dp), RoundedCornerShape(14.dp), color = V2Panel, border = BorderStroke(1.dp, V2Cyan.copy(.65f))) { Icon(Icons.Default.Smartphone, null, tint = V2Purple, modifier = Modifier.padding(9.dp)) } }
            Spacer(Modifier.height(8.dp))
            HubCard("AI All Model Download Select", "Download and manage offline AI models", V2Purple, Icons.Default.Memory) { page = "all" }
            Spacer(Modifier.height(9.dp))
            HubCard("Download Speech Models", "VAD, STT and related speech models", V2Cyan, Icons.Default.GraphicEq) { page = "speech" }
            Spacer(Modifier.height(9.dp))
            HubCard("Import GGUF", "Select and import your own model file", V2Purple, Icons.Default.FolderOpen) { importLauncher.launch(arrayOf("application/octet-stream", "application/*")) }
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), color = V2Panel.copy(.72f), border = BorderStroke(1.dp, V2Blue.copy(.65f))) { Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = V2Blue, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(9.dp)); Text("Downloads continue in foreground service. Progress stays in notification.", color = Color.White.copy(.82f), fontSize = 9.sp, lineHeight = 14.sp) } }
            message?.let { Text(it, color = V2Cyan, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp)) }
        }
        return
    }

    if (page == "speech") {
        Column(Modifier.fillMaxSize().background(V2Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp, vertical = 5.dp)) {
            V2PageHeader("Download Speech Models", { page = "hub" })
            SpeechRow("Silero VAD", "16 kHz • On-device speech detection", vadInstalled, V2Cyan)
            Spacer(Modifier.height(8.dp))
            SpeechRow("Hindi STT", "IndicConformer • Hindi • Offline", sttInstalled, V2Blue)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { scope.launch { speechBusy = true; try { if (!vadInstalled) { installer.downloadVad(); vadInstalled = true }; if (!sttInstalled) { installer.downloadHindiStt(); sttInstalled = true }; message = "Speech models ready" } catch (e: Exception) { message = e.message ?: "Speech download failed" } finally { speechBusy = false } } }, enabled = !speechBusy && (!vadInstalled || !sttInstalled), modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = V2Cyan, contentColor = Color.Black)) { Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text(if (speechBusy) "Downloading…" else "Download Speech Models", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            message?.let { Text(it, color = V2Cyan, fontSize = 9.sp, modifier = Modifier.padding(top = 7.dp)) }
        }
        return
    }

    if (page == "all") {
        val filtered = modelCatalog.filter { it.name.contains(query, true) || it.description.contains(query, true) }
        Column(Modifier.fillMaxSize().background(V2Bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 10.dp, vertical = 4.dp)) {
            V2PageHeader("AI All Model Download Select", { page = "hub" }, "Choose and download AI models for offline use")
            Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it }, Modifier.weight(1f).height(45.dp), placeholder = { Text("Search models…", color = V2Muted, fontSize = 11.sp) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = V2Cyan, modifier = Modifier.size(18.dp)) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = V2Cyan, unfocusedBorderColor = V2Panel, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 8.dp)) { items(filtered) { model -> ModelCompactCard(model, model.qwenBackend, installed, downloading, done, total, onDownload = { ModelDownloadService.startQwen(context) }, onCancel = { ModelDownloadService.cancelQwen(context) }, onDelete = { scope.launch { llmStore.delete(); installed = false; message = "Qwen model deleted" } }) } }
        }
        return
    }
}

@Composable
private fun HubCard(title: String, subtitle: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(19.dp)
    Surface(Modifier.fillMaxWidth().height(106.dp).clickable(onClick = onClick), shape, color = Color.Transparent, border = BorderStroke(1.4.dp, accent.copy(.95f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(.25f), V2Panel.copy(.97f), accent.copy(.07f))))) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(58.dp), CircleShape, color = accent.copy(.08f), border = BorderStroke(1.4.dp, accent.copy(.78f))) { Icon(icon, null, tint = accent, modifier = Modifier.padding(14.dp)) }
                Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2); Text(subtitle, color = Color.White.copy(.75f), fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 2) }
                Surface(Modifier.size(40.dp), CircleShape, color = accent.copy(.08f), border = BorderStroke(1.dp, accent.copy(.72f))) { Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.padding(7.dp)) }
            }
        }
    }
}

@Composable
private fun ModelCompactCard(model: ModelInfo, isBackend: Boolean, installed: Boolean, downloading: Boolean, done: Long, total: Long, onDownload: () -> Unit, onCancel: () -> Unit, onDelete: () -> Unit) {
    val realInstalled = isBackend && installed
    val realDownloading = isBackend && downloading
    val shape = RoundedCornerShape(13.dp)
    Surface(Modifier.fillMaxWidth(), shape, color = V2Panel.copy(.90f), border = BorderStroke(1.dp, model.accent.copy(.46f))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), RoundedCornerShape(11.dp), color = model.accent.copy(.07f), border = BorderStroke(1.dp, model.accent.copy(.58f))) { Icon(model.icon, null, tint = model.accent, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Model ${model.number.toString().padStart(2, '0')}", color = model.accent, fontSize = 8.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Text(model.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1); Spacer(Modifier.width(5.dp)); Surface(shape = RoundedCornerShape(6.dp), color = Color.Transparent, border = BorderStroke(1.dp, V2Cyan.copy(.55f))) { Text("LLM", color = V2Cyan, fontSize = 6.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) } }
                Text(model.description, color = Color.White.copy(.66f), fontSize = 8.sp, maxLines = 1)
                Text("${model.size}  •  ${model.ram}  •  Local GGUF", color = V2Cyan.copy(.82f), fontSize = 7.sp, maxLines = 1)
                if (realDownloading && total > 0L) { val p = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f); LinearProgressIndicator({ p }, Modifier.fillMaxWidth().height(3.dp), color = V2Cyan); Text("Downloading ${((p * 100).toInt())}%", color = V2Cyan, fontSize = 7.sp) }
            }
            Spacer(Modifier.width(6.dp))
            if (realDownloading) {
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Stop download", tint = Color(0xFFFF7187), modifier = Modifier.size(19.dp)) }
            } else if (realInstalled) {
                Row(verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF124A3D), border = BorderStroke(1.dp, Color(0xFF42E8B0))) { Text("Complete", color = Color(0xFF55F2B6), fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)) }; IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Delete, "Delete model", tint = Color(0xFFFF4D6D), modifier = Modifier.size(18.dp)) } }
            } else if (isBackend) {
                Surface(Modifier.height(32.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick = onDownload), RoundedCornerShape(9.dp), color = model.accent.copy(.13f), border = BorderStroke(1.dp, model.accent.copy(.78f))) { Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Download, null, tint = model.accent, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Download", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
            } else {
                Text("Source", color = V2Muted, fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun SpeechRow(title: String, subtitle: String, installed: Boolean, accent: Color) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(13.dp), color = V2Panel, border = BorderStroke(1.dp, accent.copy(.5f))) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.GraphicEq, null, tint = accent, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V2Muted, fontSize = 9.sp) }; Text(if (installed) "READY" else "MISSING", color = if (installed) Color(0xFF55F2B6) else V2Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun V2PageHeader(title: String, onBack: () -> Unit, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(27.dp)) }; Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); subtitle?.let { Text(it, color = V2Muted, fontSize = 9.sp) } } }
}

@Composable
private fun V2Setting(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) { Surface(Modifier.fillMaxWidth().padding(vertical = 4.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), RoundedCornerShape(15.dp), color = V2Panel, border = BorderStroke(1.dp, Color.White.copy(.06f))) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = V2Cyan, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = V2Muted, fontSize = 9.sp) }; if (onClick != null) Icon(Icons.Default.ChevronRight, null, tint = V2Muted) } } }

private suspend fun importModelV2(context: Context, uri: Uri, store: LlmModelStore) {
    val temp = File(context.cacheDir, "qwen-import-${System.currentTimeMillis()}.gguf")
    context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "Cannot read selected GGUF" }; temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024) } }
    try {
        require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "Selected GGUF is too small" }
        val digest = MessageDigest.getInstance("SHA-256")
        temp.inputStream().use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        require(digest.digest().joinToString("") { "%02x".format(it) }.equals(LlmModelStore.MODEL_SHA256, true)) { "Qwen GGUF SHA-256 mismatch" }
        store.modelFile.parentFile?.mkdirs(); require(temp.renameTo(store.modelFile)) { "Could not install GGUF atomically" }; store.validate().getOrThrow(); store.backupToShared()
    } finally { if (temp.exists()) temp.delete() }
}
