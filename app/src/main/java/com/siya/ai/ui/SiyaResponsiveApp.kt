package com.siya.ai.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.service.VoiceSessionState
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private val Bg = Color(0xFF02040A)
private val Panel = Color(0xFF08111F)
private val Panel2 = Color(0xFF101B31)
private val Purple = Color(0xFF9B5CFF)
private val Blue = Color(0xFF168BFF)
private val Cyan = Color(0xFF21D4FF)
private val Muted = Color(0xFF8995AB)

@Composable
fun SiyaResponsiveApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    var page by rememberSaveable { mutableStateOf("home") }
    val voice by VoiceSessionState.state.collectAsState()
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            when (page) {
                "chat" -> ResponsiveChat(onBack = { page = "home" }, onModels = { page = "models" })
                "models" -> ResponsiveModels(onBack = { page = "settings" })
                "settings" -> ResponsiveSettings(
                    onBack = { page = "home" },
                    onChat = { page = "chat" },
                    onModels = { page = "models" }
                )
                else -> ResponsiveHome(
                    microphoneGranted = microphoneGranted,
                    voice = voice,
                    onChat = { page = "chat" },
                    onSettings = { page = "settings" },
                    onRequestPermissions = onRequestPermissions,
                    onVoice = {
                        if (!microphoneGranted) onRequestPermissions()
                        else if (voice.active) onStopVoice() else onStartVoice()
                    }
                )
            }
        }
    }
}

@Composable
private fun ResponsiveHome(
    microphoneGranted: Boolean,
    voice: VoiceSessionState.State,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onVoice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderButton(Icons.Default.ChatBubble, "Chat", Cyan, onChat)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Siya Ai", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("YOUR AI COMPANION", color = Muted, fontSize = 8.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.weight(1f))
            HeaderButton(Icons.Default.Settings, "Settings", Purple, onSettings)
        }

        Text(
            "LISTENS   •   UNDERSTANDS   •   CONTROLS",
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            color = Cyan.copy(.88f), fontSize = 9.sp, letterSpacing = 2.1.sp,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ProHologram(active = voice.active, modifier = Modifier.fillMaxSize())
        }

        val status = when (voice.phase) {
            VoiceSessionState.Phase.LISTENING -> "Listening…"
            VoiceSessionState.Phase.TRANSCRIBING -> "Understanding…"
            VoiceSessionState.Phase.THINKING -> "Siya is thinking…"
            VoiceSessionState.Phase.READY -> "Ready"
            VoiceSessionState.Phase.ERROR -> "Something needs attention"
            VoiceSessionState.Phase.IDLE -> "Tap to Speak"
        }
        Text(status, modifier = Modifier.fillMaxWidth(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

        if (voice.response.isNotBlank() && voice.phase == VoiceSessionState.Phase.READY) {
            Surface(
                Modifier.fillMaxWidth().padding(top = 7.dp),
                RoundedCornerShape(14.dp), Panel,
                border = BorderStroke(1.dp, Cyan.copy(.22f))
            ) { Text(text = voice.response, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = Color.White, fontSize = 12.sp, maxLines = 2) }
        } else if (voice.error != null) {
            Text(text = voice.error!!, modifier = Modifier.fillMaxWidth().padding(top = 7.dp), color = Color(0xFFFF7187), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2)
        }

        Text(text = "Hindi  •  Hinglish  •  English", modifier = Modifier.fillMaxWidth().padding(top = 5.dp), color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(9.dp))

        Surface(
            modifier = Modifier.size(88.dp).align(Alignment.CenterHorizontally).clickable(onClick = onVoice),
            shape = CircleShape,
            color = if (voice.active) Purple.copy(.25f) else Panel2,
            border = BorderStroke(2.dp, if (voice.active) Cyan else Purple)
        ) {
            Icon(imageVector = if (voice.active) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Microphone", tint = Color.White, modifier = Modifier.padding(25.dp))
        }
        Text(
            if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak",
            Modifier.fillMaxWidth().padding(top = 4.dp), Muted, 10.sp, textAlign = TextAlign.Center
        )
        Text(text = "—   A L W A Y S   W I T H   Y O U   —", modifier = Modifier.fillMaxWidth().padding(top = 7.dp), color = Blue, fontSize = 7.sp, letterSpacing = 2.1.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).clickable(onClick = onClick),
        RoundedCornerShape(15.dp), Panel,
        border = BorderStroke(1.dp, accent.copy(.7f))
    ) { Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.padding(11.dp)) }
}

@Composable
private fun ResponsiveChat(onBack: () -> Unit, onModels: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val voice by VoiceSessionState.state.collectAsState()
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    DisposableEffect(Unit) { onDispose { engine.close() } }

    LaunchedEffect(voice.phase, voice.transcript, voice.response) {
        if (voice.phase == VoiceSessionState.Phase.THINKING && voice.transcript.isNotBlank()) {
            if (messages.none { it.first && it.second == voice.transcript }) messages += true to voice.transcript
        }
        if (voice.phase == VoiceSessionState.Phase.READY && voice.response.isNotBlank()) {
            if (messages.none { !it.first && it.second == voice.response }) messages += false to voice.response
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 15.dp, vertical = 7.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(text = "Siya Chat", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = if (installed) "Qwen 2.5 1.5B • Offline" else "Local model not installed", color = if (installed) Cyan else Muted, fontSize = 10.sp)
            }
            Surface(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onModels), RoundedCornerShape(12.dp), Panel, border = BorderStroke(1.dp, Blue.copy(.65f))) {
                Text(text = "MODEL", modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 35.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Cyan, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp)); Text(text = "Talk to Siya", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(text = "Private offline conversation", color = Muted, fontSize = 11.sp)
                }
            }
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.first) Arrangement.End else Arrangement.Start) {
                    Surface(RoundedCornerShape(18.dp), if (message.first) Purple.copy(.22f) else Panel, border = BorderStroke(1.dp, if (message.first) Purple.copy(.4f) else Color.White.copy(.04f))) {
                        Text(text = message.second, modifier = Modifier.padding(12.dp), color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, Modifier.weight(1f),
                placeholder = { Text("Ask Siya offline…", color = Muted) }, shape = RoundedCornerShape(19.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = false,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Panel2, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan)
            )
            Spacer(Modifier.width(5.dp))
            IconButton(enabled = input.isNotBlank() && !busy, onClick = {
                val prompt = input.trim(); input = ""; messages += true to prompt; busy = true
                scope.launch {
                    try {
                        check(store.isInstalled()) { "Qwen model is not installed" }
                        val result = engine.complete(prompt)
                        messages += false to result.text
                        installed = true
                    } catch (e: Exception) {
                        messages += false to "Local AI error: ${e.message ?: "model unavailable"}"
                    } finally { busy = false }
                }
            }) { Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = if (input.isNotBlank() && !busy) Cyan else Muted, modifier = Modifier.size(29.dp)) }
        }
    }
}

@Composable
private fun ResponsiveSettings(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(20.dp)) {
        PageHeader("Settings", onBack)
        Spacer(Modifier.height(12.dp))
        SettingCard("Chat", "Offline text chat with Local LLM", Icons.Default.ChatBubble, onChat)
        SettingCard("AI Models", "Download or import offline models", Icons.Default.Memory, onModels)
        SettingCard("Voice mode", "Voice-first assistant", Icons.Default.Mic, null)
        SettingCard("Privacy", "On-device processing", Icons.Default.Lock, null)
        SettingCard("Language", "Hindi / Hinglish / English", Icons.Default.Language, null)
        SettingCard("Background", "Controlled by Android permissions", Icons.Default.BatterySaver, null)
    }
}

@Composable
private fun ResponsiveModels(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var downloading by remember { mutableStateOf(false) }
    var done by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            downloading = true; error = null
            try { importModel(context, uri, store); installed = true }
            catch (e: Exception) { error = e.message ?: "Import failed" }
            finally { downloading = false }
        }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(20.dp)) {
        PageHeader("AI Models", onBack)
        Spacer(Modifier.height(12.dp))
        ModelCard("Qwen 2.5 1.5B Instruct", "Q4_K_M • ~1.12 GB • Local LLM", installed)
        Spacer(Modifier.height(10.dp))
        if (downloading && total > 0) LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = Cyan)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { scope.launch { downloading = true; error = null; done = 0; total = 0; try { LlmModelInstaller(store).download { d, t -> done = d; total = t }; installed = true } catch (e: Exception) { error = e.message ?: "Download failed" } finally { downloading = false } } },
            enabled = !downloading && !installed,
            modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(15.dp)
        ) { Icon(imageVector = Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(7.dp)); Text(if (downloading) "Downloading…" else if (installed) "Model Installed" else "Download Qwen Model", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { launcher.launch(arrayOf("application/octet-stream", "application/*")) }, enabled = !downloading, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(15.dp)) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null); Spacer(Modifier.width(7.dp)); Text("Import GGUF from device")
        }
        error?.let { Text(text = it, color = Color(0xFFFF7187), fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp)) }
        Spacer(Modifier.height(18.dp))
        Text(text = "Pipeline", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = "Audio → VAD → Hindi STT → Local Qwen → TTS", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Text(text = title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), RoundedCornerShape(20.dp), Panel, border = BorderStroke(1.dp, Color.White.copy(.035f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(43.dp), RoundedCornerShape(13.dp), Panel2) { Icon(icon, null, tint = if (title == "AI Models") Cyan else Purple, modifier = Modifier.padding(10.dp)) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(text = subtitle, color = Muted, fontSize = 10.sp) }
            if (onClick != null) Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
private fun ModelCard(title: String, subtitle: String, installed: Boolean) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), Panel, border = BorderStroke(1.dp, if (installed) Cyan.copy(.45f) else Purple.copy(.45f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), RoundedCornerShape(14.dp), Panel2) { Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = Cyan, modifier = Modifier.padding(12.dp)) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold); Text(text = subtitle, color = Muted, fontSize = 10.sp) }
            Text(text = if (installed) "READY" else "OFFLINE", color = if (installed) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private suspend fun importModel(context: Context, uri: Uri, store: LlmModelStore) {
    val target = store.modelFile
    val temp = File(target.parentFile, "${target.name}.import")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Cannot read selected file" }
        temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024); output.fd.sync() }
    }
    require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "GGUF file is too small or incomplete" }
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    temp.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
    }
    require(digest.digest().joinToString("") { "%02x".format(it) }.equals(LlmModelStore.MODEL_SHA256, true)) { "SHA-256 does not match the official Qwen Q4_K_M model" }
    require(temp.renameTo(target)) { "Could not install model atomically" }
    store.validate().getOrThrow()
}
