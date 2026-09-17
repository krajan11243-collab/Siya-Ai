package com.siya.ai.ui

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.siya.ai.llm.LlmModelInstaller
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

private val Bg = Color(0xFF01040B)
private val Panel = Color(0xFF07111F)
private val Panel2 = Color(0xFF0D1930)
private val Purple = Color(0xFF9B5CFF)
private val Blue = Color(0xFF168BFF)
private val Cyan = Color(0xFF21D4FF)
private val Muted = Color(0xFF8995AB)
private data class ChatMessage(val user: Boolean, val text: String, val thinking: Boolean = false)

@Composable
fun SiyaResponsiveApp(microphoneGranted: Boolean, onRequestPermissions: () -> Unit, onStartVoice: () -> Unit, onStopVoice: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("home") }
    val voice by VoiceSessionState.state.collectAsState()
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            when (page) {
                "chat" -> ResponsiveChat({ page = "home" }, { page = "models" })
                "models" -> ResponsiveModels { page = "settings" }
                "settings" -> ResponsiveSettings({ page = "home" }, { page = "chat" }, { page = "models" })
                else -> ResponsiveHome(microphoneGranted, voice, { page = "chat" }, { page = "settings" }) { if (!microphoneGranted) onRequestPermissions() else if (voice.active) onStopVoice() else onStartVoice() }
            }
        }
    }
}

@Composable
private fun ResponsiveHome(microphoneGranted: Boolean, voice: VoiceSessionState.State, onChat: () -> Unit, onSettings: () -> Unit, onVoice: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp, vertical = 5.dp)) {
        val compact = maxHeight < 700.dp
        val veryCompact = maxHeight < 620.dp
        val narrow = maxWidth < 360.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(if (compact) 52.dp else 60.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderButton(Icons.Default.ChatBubble, "Chat", Cyan, onChat); Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Siya Ai", color = Color.White, fontSize = if (narrow) 25.sp else 30.sp, fontWeight = FontWeight.ExtraBold); Text("Y O U R   A I   C O M P A N I O N", color = Muted, fontSize = 6.sp, letterSpacing = 1.2.sp) }
                Spacer(Modifier.weight(1f)); HeaderButton(Icons.Default.Settings, "Settings", Purple, onSettings)
            }
            Text("L I S T E N S     •     U N D E R S T A N D S     •     C O N T R O L S", Modifier.fillMaxWidth(), color = Cyan.copy(.9f), fontSize = if (narrow) 6.sp else 7.sp, letterSpacing = 1.1.sp, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { ProHologram(active = voice.active, modifier = Modifier.fillMaxSize()) }
            val status = when (voice.phase) { VoiceSessionState.Phase.LISTENING -> "Listening"; VoiceSessionState.Phase.TRANSCRIBING -> "Understanding"; VoiceSessionState.Phase.THINKING -> "Thinking…"; VoiceSessionState.Phase.READY -> "Ready"; VoiceSessionState.Phase.ERROR -> "Needs attention"; VoiceSessionState.Phase.IDLE -> "Tap to Speak" }
            Text(status, Modifier.fillMaxWidth(), color = Color.White, fontSize = if (compact) 17.sp else 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (voice.phase == VoiceSessionState.Phase.THINKING && voice.transcript.isNotBlank()) Text(voice.transcript, Modifier.fillMaxWidth(), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
            if (voice.phase == VoiceSessionState.Phase.READY && voice.response.isNotBlank()) Surface(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp), color = Panel, border = BorderStroke(1.dp, Cyan.copy(.25f))) { Text(voice.response, Modifier.padding(10.dp), color = Color.White, fontSize = 10.sp, maxLines = 2) }
            voice.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 3.dp), color = Color(0xFFFF7187), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2) }
            Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 3.dp), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center); Spacer(Modifier.height(if (veryCompact) 2.dp else 5.dp))
            Surface(modifier = Modifier.size(if (veryCompact) 72.dp else if (compact) 78.dp else 86.dp).align(Alignment.CenterHorizontally).clickable(onClick = onVoice), shape = CircleShape, color = if (voice.active) Purple.copy(.24f) else Panel2, border = BorderStroke(2.dp, if (voice.active) Cyan else Purple)) { Icon(if (voice.active) Icons.Default.Stop else Icons.Default.Mic, "Microphone", tint = Color.White, modifier = Modifier.padding(if (veryCompact) 19.dp else 22.dp)) }
            Text(if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth(), color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center); Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth(), color = Blue, fontSize = 6.sp, letterSpacing = 1.7.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) { Surface(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(.8f))) { Icon(icon, label, tint = accent, modifier = Modifier.padding(11.dp)) } }

@Composable
private fun ResponsiveChat(onBack: () -> Unit, onModels: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val store = remember { LlmModelStore(context) }; val engine = remember { LocalLlmEngine(store) }; val voice by VoiceSessionState.state.collectAsState(); val messages = remember { mutableStateListOf<ChatMessage>() }; val listState = rememberLazyListState(); var input by rememberSaveable { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; var installed by remember { mutableStateOf(store.isInstalled()) }
    DisposableEffect(Unit) { onDispose { engine.close() } }; LaunchedEffect(installed) { if (installed) runCatching { engine.load() } }; LaunchedEffect(messages.size, busy) { if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex) }
    LaunchedEffect(voice.phase, voice.transcript, voice.response, voice.error) { when (voice.phase) { VoiceSessionState.Phase.THINKING -> { val text = voice.transcript.trim(); if (text.isNotBlank() && messages.none { it.user && it.text == text }) messages += ChatMessage(true, text); if (messages.none { it.thinking }) messages += ChatMessage(false, "Thinking…", true); busy = true }; VoiceSessionState.Phase.READY -> { val answer = voice.response.trim(); val index = messages.indexOfLast { it.thinking }; if (answer.isNotBlank()) { if (index >= 0) messages[index] = ChatMessage(false, answer) else messages += ChatMessage(false, answer) }; busy = false }; VoiceSessionState.Phase.ERROR -> { val index = messages.indexOfLast { it.thinking }; if (index >= 0) messages[index] = ChatMessage(false, voice.error ?: "Voice error") else if (voice.error != null) messages += ChatMessage(false, voice.error!!); busy = false }; else -> Unit } }
    Scaffold(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), containerColor = Bg, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(10.dp), verticalAlignment = Alignment.Bottom) { OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask Siya…", color = Muted) }, shape = RoundedCornerShape(18.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), maxLines = 4, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Panel2, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan)); IconButton(enabled = input.isNotBlank() && !busy && installed, onClick = { val prompt = input.trim(); input = ""; messages += ChatMessage(true, prompt); messages += ChatMessage(false, "Thinking…", true); busy = true; scope.launch { try { check(store.isInstalled()) { "Qwen model is not installed" }; val result = engine.complete(prompt); val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = ChatMessage(false, result.text.ifBlank { "I could not generate a response." }) } catch (e: Exception) { val i = messages.indexOfLast { it.thinking }; val t = "Local AI error: ${e.message ?: "model unavailable"}"; if (i >= 0) messages[i] = ChatMessage(false, t) else messages += ChatMessage(false, t) } finally { busy = false } } }) { Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank() && !busy && installed) Cyan else Muted) } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Column(Modifier.weight(1f)) { Text("Siya Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(if (installed) "Qwen 2.5 1.5B • Offline • Ready" else "Local model not installed", color = if (installed) Cyan else Muted, fontSize = 9.sp) }; Surface(modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable(onClick = onModels), shape = RoundedCornerShape(11.dp), color = Panel, border = BorderStroke(1.dp, Blue.copy(.65f))) { Text("MODEL", Modifier.padding(9.dp), color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { if (messages.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AutoAwesome, null, tint = Cyan, modifier = Modifier.size(40.dp)); Text("Talk to Siya", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Fast private offline chat", color = Muted, fontSize = 10.sp) } }; itemsIndexed(messages) { _, m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.user) Arrangement.End else Arrangement.Start) { Surface(modifier = Modifier.wrapContentWidth(), shape = RoundedCornerShape(17.dp), color = if (m.user) Purple.copy(.22f) else Panel, border = BorderStroke(1.dp, if (m.user) Purple.copy(.4f) else Color.White.copy(.04f))) { Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { if (m.thinking) { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Cyan); Spacer(Modifier.width(8.dp)) }; Text(m.text, color = Color.White, fontSize = 13.sp) } } } } }
        }
    }
}

@Composable private fun ResponsiveSettings(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) { Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(18.dp)) { PageHeader("Settings", onBack); SettingCard("Chat", "Offline text chat with Local LLM", Icons.Default.ChatBubble, onChat); SettingCard("AI Models", "Install Qwen + VAD + Hindi STT", Icons.Default.Memory, onModels); SettingCard("Voice mode", "Microphone → VAD → Hindi STT → Qwen", Icons.Default.Mic, null); SettingCard("Privacy", "Audio and AI inference stay on device", Icons.Default.Lock, null); SettingCard("Language", "Hindi / Hinglish / English", Icons.Default.Language, null); SettingCard("Background", "Controlled by Android foreground-service rules", Icons.Default.BatterySaver, null) } }

private fun modelMb(value: Long): String = String.format(java.util.Locale.US, "%.1f MB", value / 1_048_576.0)

@Composable
private fun ResponsiveModels(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val llmStore = remember { LlmModelStore(context) }
    val vadStore = remember { VadModelStore(context) }
    val sttStore = remember { SttModelStore(context) }
    val installer = remember { VoiceModelInstaller(vadStore, sttStore) }

    var llmInstalled by remember { mutableStateOf(llmStore.isInstalled()) }
    var vadInstalled by remember { mutableStateOf(vadStore.isInstalled()) }
    var sttInstalled by remember { mutableStateOf(sttStore.isInstalled()) }
    var busy by remember { mutableStateOf(false) }
    var progressDone by remember { mutableLongStateOf(0L) }
    var progressTotal by remember { mutableLongStateOf(0L) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var showSpeech by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val prefs = remember { ModelDownloadService.prefs(context) }
    var downloading by remember { mutableStateOf(prefs.getBoolean(ModelDownloadService.KEY_ACTIVE, false)) }

    LaunchedEffect(Unit) {
        while (true) {
            downloading = prefs.getBoolean(ModelDownloadService.KEY_ACTIVE, false)
            progressDone = prefs.getLong(ModelDownloadService.KEY_DONE, 0L)
            progressTotal = prefs.getLong(ModelDownloadService.KEY_TOTAL, 0L)
            if (!downloading && llmStore.isInstalled()) llmInstalled = true
            delay(500)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = null
                error = null
                try {
                    importModel(context, uri, llmStore)
                    llmInstalled = true
                    status = "GGUF imported successfully"
                } catch (e: Exception) {
                    error = e.message ?: "GGUF import failed"
                } finally {
                    busy = false
                }
            }
        }
    }

    if (showAll) {
        ModelHubFullScreen("AI All Model Download Select", "Choose and download AI models for offline use", Purple, { showAll = false }) {
            ActualLlmModelCard(llmInstalled, downloading, progressDone, progressTotal) { ModelDownloadService.startQwen(context) }
        }
        return
    }

    if (showSpeech) {
        ModelHubFullScreen("Download Speech Models", "Download VAD, Hindi STT and related speech models", Cyan, { showSpeech = false }) {
            ActualSpeechModelCard("Silero VAD", "16 kHz • On-device speech detection", Cyan, vadInstalled)
            Spacer(Modifier.height(12.dp))
            ActualSpeechModelCard("Hindi STT", "IndicConformer • Hindi • Offline", Blue, sttInstalled)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        status = null
                        error = null
                        try {
                            if (!vadInstalled) {
                                installer.downloadVad { d, t -> progressDone = d; progressTotal = t }
                                vadInstalled = true
                            }
                            if (!sttInstalled) {
                                installer.downloadHindiStt { d, t -> progressDone = d; progressTotal = t }
                                sttInstalled = true
                            }
                            status = "Speech models ready"
                        } catch (e: Exception) {
                            error = e.message ?: "Speech model download failed"
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && (!vadInstalled || !sttInstalled),
                modifier = Modifier.fillMaxWidth().height(52.dp).shadow(12.dp, RoundedCornerShape(16.dp), clip = true),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Downloading…" else "Download Speech Models", fontWeight = FontWeight.Bold)
            }
            status?.let { Text(it, color = Cyan, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) }
            error?.let { Text(it, color = Color(0xFFFF7187), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) }
            if (busy && progressTotal > 0L) {
                Spacer(Modifier.height(9.dp))
                val progress = (progressDone.toFloat() / progressTotal.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Cyan)
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Bg, Color(0xFF020A18), Bg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ModelHubHeader(onBack)
        Spacer(Modifier.height(14.dp))
        ModelHubActionCard("AI All Model Download Select", "Download all required models for complete AI voice assistant experience", Purple, Icons.Default.Memory) { showAll = true }
        Spacer(Modifier.height(12.dp))
        ModelHubActionCard("Download Speech Models", "Download VAD, STT and all related models (One Click)", Cyan, Icons.Default.GraphicEq) { showSpeech = true }
        Spacer(Modifier.height(12.dp))
        ModelHubActionCard("Import GGUF", "Select and import your own model file", Purple, Icons.Default.FolderOpen) { importLauncher.launch(arrayOf("application/octet-stream", "application/*")) }
        Spacer(Modifier.height(14.dp))
        ModelHubInfoCard("Model downloads run in a foreground service and continue while Siya Ai is closed. Progress stays in the notification.")
        if (busy) Text("Importing…", color = Cyan, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        status?.let { Text(it, color = Cyan, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) }
        error?.let { Text(it, color = Color(0xFFFF7187), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun ModelHubHeader(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(31.dp))
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI", color = Cyan, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(7.dp))
                Text("Models", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Download, Select & Use On-Device Models", color = Color.White.copy(.78f), fontSize = 11.sp, maxLines = 1)
        }
        Surface(modifier = Modifier.size(48.dp).shadow(10.dp, RoundedCornerShape(16.dp), clip = true), shape = RoundedCornerShape(16.dp), color = Panel.copy(.78f), border = BorderStroke(1.dp, Cyan.copy(.65f))) {
            Icon(Icons.Default.Smartphone, null, tint = Purple, modifier = Modifier.padding(10.dp))
        }
    }
}

@Composable
private fun ModelHubActionCard(title: String, subtitle: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(23.dp)
    Surface(modifier = Modifier.fillMaxWidth().height(128.dp).shadow(18.dp, shape, clip = true).clickable(onClick = onClick), shape = shape, color = Color.Transparent, border = BorderStroke(1.6.dp, accent.copy(.98f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(.30f), Panel.copy(.96f), accent.copy(.10f))))) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.13f), Color.Transparent))))
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(66.dp).shadow(10.dp, CircleShape, clip = true), shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.6.dp, accent.copy(.82f))) {
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.20f), Color.Transparent)))) {
                        Icon(icon, null, tint = accent, modifier = Modifier.padding(16.dp))
                    }
                }
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Text(subtitle, color = Color.White.copy(.80f), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 2)
                }
                Surface(modifier = Modifier.size(46.dp), shape = CircleShape, color = accent.copy(.10f), border = BorderStroke(1.2.dp, accent.copy(.76f))) {
                    Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ModelHubInfoCard(text: String) {
    Surface(modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(18.dp), clip = true), shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.dp, Blue.copy(.72f))) {
        Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Blue.copy(.10f), Panel.copy(.86f)))).padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = Blue.copy(.07f), border = BorderStroke(1.dp, Blue.copy(.72f))) {
                Icon(Icons.Default.Info, null, tint = Blue, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(text, color = Color.White.copy(.84f), fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ModelHubFullScreen(title: String, subtitle: String, accent: Color, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onBack) {
        Surface(modifier = Modifier.fillMaxSize().padding(5.dp), shape = RoundedCornerShape(26.dp), color = Color.Transparent, border = BorderStroke(1.2.dp, accent.copy(.65f))) {
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020711), Bg, Color(0xFF020A18)))).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(29.dp)) }
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        Text(subtitle, color = Color.White.copy(.72f), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
            }
        }
    }
}

@Composable
private fun ActualLlmModelCard(installed: Boolean, downloading: Boolean, done: Long, total: Long, onDownload: () -> Unit) {
    val accent = Purple
    val shape = RoundedCornerShape(20.dp)
    Surface(modifier = Modifier.fillMaxWidth().shadow(16.dp, shape, clip = true), shape = shape, color = Color.Transparent, border = BorderStroke(1.3.dp, accent.copy(.78f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(accent.copy(.18f), Panel.copy(.96f), Cyan.copy(.05f)))).padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(54.dp), shape = RoundedCornerShape(15.dp), color = accent.copy(.08f), border = BorderStroke(1.dp, accent.copy(.68f))) {
                    Icon(Icons.Default.Memory, null, tint = accent, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Qwen 2.5 1.5B", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(7.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.Transparent, border = BorderStroke(1.dp, Cyan.copy(.72f))) {
                            Text("LLM", color = Cyan, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("Balanced coding & chat model", color = Color.White.copy(.72f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Q4_K_M • ~1.12 GB • Local GGUF", color = Cyan.copy(.88f), fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (downloading && total > 0L) {
                val progress = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Downloading…", color = Color.White.copy(.72f), fontSize = 10.sp)
                    Text("${(progress * 100).toInt()}%", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp), color = Cyan)
            }
            Spacer(Modifier.height(11.dp))
            Button(onClick = onDownload, enabled = !installed && !downloading, modifier = Modifier.fillMaxWidth().height(50.dp).shadow(10.dp, RoundedCornerShape(14.dp), clip = true), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = Color(0xFF164238), disabledContentColor = Color(0xFF55F2B6))) {
                Icon(if (installed) Icons.Default.Check else Icons.Default.Download, null)
                Spacer(Modifier.width(7.dp))
                Text(if (installed) "Downloaded" else if (downloading) "Downloading" else "Download", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActualSpeechModelCard(title: String, subtitle: String, accent: Color, installed: Boolean) {
    val shape = RoundedCornerShape(19.dp)
    Surface(modifier = Modifier.fillMaxWidth().shadow(13.dp, shape, clip = true), shape = shape, color = Color.Transparent, border = BorderStroke(1.2.dp, accent.copy(.65f))) {
        Row(Modifier.background(Brush.linearGradient(listOf(accent.copy(.14f), Panel.copy(.95f)))).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(52.dp), shape = RoundedCornerShape(14.dp), color = accent.copy(.07f), border = BorderStroke(1.dp, accent.copy(.62f))) {
                Icon(Icons.Default.GraphicEq, null, tint = accent, modifier = Modifier.padding(11.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color.White.copy(.70f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text(if (installed) "DOWNLOADED" else "READY", color = if (installed) Color(0xFF55F2B6) else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun ModelCard(title: String, subtitle: String, installed: Boolean, accent: Color) { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(.28f))) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Memory, null, tint = accent, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 9.sp) }; Text(if (installed) "READY" else "MISSING", color = if (installed) Cyan else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun PageHeader(title: String, onBack: () -> Unit) { Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)) } }
@Composable private fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) { Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(16.dp), color = Panel, border = BorderStroke(1.dp, Color.White.copy(.05f))) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Cyan, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 10.sp) }; if (onClick != null) Icon(Icons.Default.ChevronRight, null, tint = Muted) } } }

private suspend fun importModel(context: Context, uri: Uri, store: LlmModelStore) { val temp = File(context.cacheDir, "qwen-import-${System.currentTimeMillis()}.gguf"); context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "Cannot read selected GGUF" }; temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024) } }; try { require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "Selected GGUF is too small" }; val digest = MessageDigest.getInstance("SHA-256"); temp.inputStream().use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }; require(digest.digest().joinToString("") { "%02x".format(it) }.equals(LlmModelStore.MODEL_SHA256, true)) { "Qwen GGUF SHA-256 mismatch" }; store.modelFile.parentFile?.mkdirs(); require(temp.renameTo(store.modelFile)) { "Could not install GGUF atomically" }; store.validate().getOrThrow(); store.backupToShared() } finally { if (temp.exists()) temp.delete() } }
