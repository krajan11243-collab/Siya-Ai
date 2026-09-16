package com.siya.ai.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.service.VoiceSessionState
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

private val Bg = Color(0xFF02040A)
private val Panel = Color(0xFF08111F)
private val Panel2 = Color(0xFF101B31)
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
                else -> ResponsiveHome(microphoneGranted, voice, { page = "chat" }, { page = "settings" }) {
                    if (!microphoneGranted) onRequestPermissions() else if (voice.active) onStopVoice() else onStartVoice()
                }
            }
        }
    }
}

@Composable
private fun ResponsiveHome(microphoneGranted: Boolean, voice: VoiceSessionState.State, onChat: () -> Unit, onSettings: () -> Unit, onVoice: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 14.dp, vertical = 6.dp)) {
        val compact = maxHeight < 700.dp
        val narrow = maxWidth < 360.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderButton(Icons.Default.ChatBubble, "Chat", Cyan, onChat)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Siya Ai", color = Color.White, fontSize = if (narrow) 23.sp else 27.sp, fontWeight = FontWeight.Bold)
                    Text(text = "YOUR AI COMPANION", color = Muted, fontSize = 7.sp, letterSpacing = 2.5.sp)
                }
                Spacer(Modifier.weight(1f))
                HeaderButton(Icons.Default.Settings, "Settings", Purple, onSettings)
            }
            Text(text = "LISTENS   •   UNDERSTANDS   •   CONTROLS", modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Cyan.copy(alpha = .88f), fontSize = 8.sp, letterSpacing = 1.8.sp, textAlign = TextAlign.Center)
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = if (compact) 2.dp else 5.dp), contentAlignment = Alignment.Center) { ProHologram(active = voice.active, modifier = Modifier.fillMaxSize()) }
            val status = when (voice.phase) {
                VoiceSessionState.Phase.LISTENING -> "Listening…"
                VoiceSessionState.Phase.TRANSCRIBING -> "Understanding…"
                VoiceSessionState.Phase.THINKING -> "Thinking…"
                VoiceSessionState.Phase.READY -> "Ready"
                VoiceSessionState.Phase.ERROR -> "Needs attention"
                VoiceSessionState.Phase.IDLE -> "Tap to Speak"
            }
            Text(text = status, modifier = Modifier.fillMaxWidth(), color = Color.White, fontSize = if (compact) 18.sp else 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (voice.phase == VoiceSessionState.Phase.THINKING && voice.transcript.isNotBlank()) Text(text = voice.transcript, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
            if (voice.phase == VoiceSessionState.Phase.READY && voice.response.isNotBlank()) Surface(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), shape = RoundedCornerShape(13.dp), color = Panel, border = BorderStroke(1.dp, Cyan.copy(alpha = .22f))) { Text(text = voice.response, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 11.sp, maxLines = 2) }
            voice.error?.let { Text(text = it, modifier = Modifier.fillMaxWidth().padding(top = 5.dp), color = Color(0xFFFF7187), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2) }
            Text(text = "Hindi  •  Hinglish  •  English", modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
            Surface(modifier = Modifier.size(if (compact) 76.dp else 86.dp).align(Alignment.CenterHorizontally).clickable(onClick = onVoice), shape = CircleShape, color = if (voice.active) Purple.copy(alpha = .25f) else Panel2, border = BorderStroke(2.dp, if (voice.active) Cyan else Purple)) { Icon(imageVector = if (voice.active) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Microphone", tint = Color.White, modifier = Modifier.padding(if (compact) 21.dp else 24.dp)) }
            Text(text = if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak", modifier = Modifier.fillMaxWidth().padding(top = 3.dp), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text(text = "—   A L W A Y S   W I T H   Y O U   —", modifier = Modifier.fillMaxWidth().padding(top = 5.dp), color = Blue, fontSize = 6.sp, letterSpacing = 1.8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(alpha = .7f))) { Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.padding(10.dp)) }
}

@Composable
private fun ResponsiveChat(onBack: () -> Unit, onModels: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val voice by VoiceSessionState.state.collectAsState()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    DisposableEffect(Unit) { onDispose { engine.close() } }
    LaunchedEffect(Unit) { if (installed) runCatching { engine.load() } }
    LaunchedEffect(messages.size, busy) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    LaunchedEffect(voice.phase, voice.transcript, voice.response, voice.error) {
        when (voice.phase) {
            VoiceSessionState.Phase.THINKING -> { val text = voice.transcript.trim(); if (text.isNotBlank() && messages.none { it.user && it.text == text }) messages += ChatMessage(true, text); if (messages.none { it.thinking }) messages += ChatMessage(false, "Thinking…", true); busy = true }
            VoiceSessionState.Phase.READY -> { val answer = voice.response.trim(); val index = messages.indexOfLast { it.thinking }; if (answer.isNotBlank()) { if (index >= 0) messages[index] = ChatMessage(false, answer) else if (messages.none { !it.user && it.text == answer }) messages += ChatMessage(false, answer) }; busy = false }
            VoiceSessionState.Phase.ERROR -> { val index = messages.indexOfLast { it.thinking }; if (index >= 0) messages[index] = ChatMessage(false, voice.error ?: "Voice error"); busy = false }
            else -> Unit
        }
    }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
            Column(modifier = Modifier.weight(1f)) { Text(text = "Siya Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(text = if (installed) "Qwen 2.5 1.5B • Offline • Warm" else "Local model not installed", color = if (installed) Cyan else Muted, fontSize = 9.sp) }
            Surface(modifier = Modifier.clip(RoundedCornerShape(11.dp)).clickable(onClick = onModels), shape = RoundedCornerShape(11.dp), color = Panel, border = BorderStroke(1.dp, Blue.copy(alpha = .65f))) { Text(text = "MODEL", modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp), color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = listState, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (messages.isEmpty()) item { Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Cyan, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(9.dp)); Text(text = "Talk to Siya", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(text = "Fast private offline chat", color = Muted, fontSize = 10.sp) } }
            itemsIndexed(messages) { _, message -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.user) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(17.dp), color = if (message.user) Purple.copy(alpha = .22f) else Panel, border = BorderStroke(1.dp, if (message.user) Purple.copy(alpha = .4f) else Color.White.copy(alpha = .04f))) { Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { if (message.thinking) { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Cyan); Spacer(Modifier.width(8.dp)) }; Text(text = message.text, color = Color.White, fontSize = 13.sp) } } } }
        }
        Row(modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(top = 5.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text(text = "Ask Siya…", color = Muted) }, shape = RoundedCornerShape(18.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), minLines = 1, maxLines = 4, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Panel2, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan))
            Spacer(Modifier.width(4.dp))
            IconButton(enabled = input.isNotBlank() && !busy && installed, onClick = { val prompt = input.trim(); input = ""; messages += ChatMessage(true, prompt); messages += ChatMessage(false, "Thinking…", true); busy = true; scope.launch { try { check(store.isInstalled()) { "Qwen model is not installed" }; val result = engine.complete(prompt); val index = messages.indexOfLast { it.thinking }; if (index >= 0) messages[index] = ChatMessage(false, result.text.ifBlank { "I could not generate a response." }); installed = true } catch (e: Exception) { val index = messages.indexOfLast { it.thinking }; val errorText = "Local AI error: ${e.message ?: "model unavailable"}"; if (index >= 0) messages[index] = ChatMessage(false, errorText) else messages += ChatMessage(false, errorText) } finally { busy = false } } }) { Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = if (input.isNotBlank() && !busy && installed) Cyan else Muted, modifier = Modifier.size(27.dp)) }
        }
    }
}

@Composable
private fun ResponsiveSettings(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(18.dp)) { PageHeader("Settings", onBack); Spacer(Modifier.height(10.dp)); SettingCard("Chat", "Offline text chat with Local LLM", Icons.Default.ChatBubble, onChat); SettingCard("AI Models", "Install Qwen + VAD + Hindi STT", Icons.Default.Memory, onModels); SettingCard("Voice mode", "Microphone → VAD → Hindi STT → Qwen", Icons.Default.Mic, null); SettingCard("Privacy", "Audio and AI inference stay on device", Icons.Default.Lock, null); SettingCard("Language", "Hindi / Hinglish / English", Icons.Default.Language, null); SettingCard("Background", "Controlled by Android foreground-service rules", Icons.Default.BatterySaver, null) }
}

@Composable
private fun ResponsiveModels(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val llmStore = remember { LlmModelStore(context) }; val vadStore = remember { VadModelStore(context) }; val sttStore = remember { SttModelStore(context) }; val voiceInstaller = remember { VoiceModelInstaller(vadStore, sttStore) }
    var llmInstalled by remember { mutableStateOf(llmStore.isInstalled()) }; var vadInstalled by remember { mutableStateOf(vadStore.isInstalled()) }; var sttInstalled by remember { mutableStateOf(sttStore.isInstalled()) }; var busy by remember { mutableStateOf(false) }; var status by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }; var done by remember { mutableStateOf(0L) }; var total by remember { mutableStateOf(0L) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) scope.launch { busy = true; error = null; try { importModel(context, uri, llmStore); llmInstalled = true; status = "Qwen model imported and verified" } catch (e: Exception) { error = e.message ?: "Import failed" } finally { busy = false } } }
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(18.dp)) {
        PageHeader("AI Models", onBack); Spacer(Modifier.height(10.dp)); ModelCard("Qwen 2.5 1.5B Instruct", "Q4_K_M • ~1.12 GB • local reasoning", llmInstalled)
        ModelActionButton(if (llmInstalled) "Qwen Ready" else "Download Qwen", Icons.Default.Download, !busy && !llmInstalled) { scope.launch { busy = true; error = null; status = "Downloading Qwen…"; done = 0; total = 0; try { LlmModelInstaller(llmStore).download { d, t -> done = d; total = t }; llmInstalled = true; status = "Qwen ready" } catch (e: Exception) { error = e.message ?: "Qwen download failed" } finally { busy = false } } }
        Spacer(Modifier.height(7.dp)); OutlinedButton(enabled = !busy, onClick = { launcher.launch(arrayOf("application/octet-stream", "application/*")) }, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp)) { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null); Spacer(Modifier.width(7.dp)); Text(text = "Import Qwen GGUF") }
        Spacer(Modifier.height(14.dp)); ModelCard("Silero VAD v5", "~2 MB • voice activity + end-of-turn", vadInstalled); ModelActionButton(if (vadInstalled) "VAD Ready" else "Download VAD", Icons.Default.RecordVoiceOver, !busy && !vadInstalled) { scope.launch { busy = true; error = null; status = "Downloading Silero VAD…"; done = 0; total = 0; try { voiceInstaller.downloadVad { d, t -> done = d; total = t }; vadInstalled = true; status = "VAD ready" } catch (e: Exception) { error = e.message ?: "VAD download failed" } finally { busy = false } } }
        Spacer(Modifier.height(14.dp)); ModelCard("Hindi STT", "IndicConformer int8 • ~150–200 MB • offline", sttInstalled); ModelActionButton(if (sttInstalled) "Hindi STT Ready" else "Download Hindi STT", Icons.Default.Mic, !busy && !sttInstalled) { scope.launch { busy = true; error = null; status = "Downloading Hindi STT + tokens…"; done = 0; total = 0; try { voiceInstaller.downloadHindiStt { d, t -> done = d; total = t }; sttInstalled = true; status = "Hindi STT ready" } catch (e: Exception) { error = e.message ?: "Hindi STT download failed" } finally { busy = false } } }
        if (busy && total > 0) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Cyan) }
        status?.let { Text(text = it, modifier = Modifier.padding(top = 9.dp), color = Cyan, fontSize = 10.sp) }; error?.let { Text(text = it, modifier = Modifier.padding(top = 7.dp), color = Color(0xFFFF7187), fontSize = 10.sp) }
        Spacer(Modifier.height(16.dp)); Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Panel, border = BorderStroke(1.dp, Cyan.copy(alpha = .15f))) { Column(modifier = Modifier.padding(13.dp)) { Text(text = "Voice pipeline", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(text = "AudioRecord → Silero VAD → Hindi STT → Qwen → response", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp)); Text(text = "All three voice models must be READY before live mic conversation can respond.", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp)) } }
    }
}

@Composable
private fun ModelActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) { Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(46.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(14.dp)) { Icon(imageVector = icon, contentDescription = null); Spacer(Modifier.width(7.dp)); Text(text = text, fontWeight = FontWeight.Bold) } }

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }; Text(text = title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) { Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(19.dp), color = Panel, border = BorderStroke(1.dp, Color.White.copy(alpha = .035f))) { Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = Panel2) { Icon(imageVector = icon, contentDescription = null, tint = if (title == "AI Models") Cyan else Purple, modifier = Modifier.padding(9.dp)) }; Spacer(Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(text = subtitle, color = Muted, fontSize = 9.sp) }; if (onClick != null) Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Muted) } } }

@Composable
private fun ModelCard(title: String, subtitle: String, installed: Boolean) { Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Panel, border = BorderStroke(1.dp, if (installed) Cyan.copy(alpha = .45f) else Purple.copy(alpha = .4f))) { Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(modifier = Modifier.size(46.dp), shape = RoundedCornerShape(13.dp), color = Panel2) { Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = Cyan, modifier = Modifier.padding(11.dp)) }; Spacer(Modifier.width(10.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(text = subtitle, color = Muted, fontSize = 9.sp) }; Text(text = if (installed) "READY" else "MISSING", color = if (installed) Cyan else Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }

private suspend fun importModel(context: Context, uri: Uri, store: LlmModelStore) {
    val target = store.modelFile; val temp = File(target.parentFile, "${target.name}.import")
    context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "Cannot read selected file" }; temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024); output.fd.sync() } }
    require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "GGUF file is too small or incomplete" }
    val digest = MessageDigest.getInstance("SHA-256"); temp.inputStream().use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) } }
    require(digest.digest().joinToString("") { "%02x".format(it) }.equals(LlmModelStore.MODEL_SHA256, true)) { "SHA-256 does not match the official Qwen Q4_K_M model" }
    require(temp.renameTo(target)) { "Could not install model atomically" }; store.validate().getOrThrow()
}
