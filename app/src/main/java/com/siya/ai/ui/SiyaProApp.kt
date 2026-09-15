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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale

private val Bg = Color(0xFF010309)
private val Panel = Color(0xFF07101F)
private val Panel2 = Color(0xFF0B1830)
private val Purple = Color(0xFF9B5CFF)
private val Blue = Color(0xFF168BFF)
private val Cyan = Color(0xFF21D4FF)
private val Muted = Color(0xFF8995AB)

@Composable
fun SiyaProApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    var page by rememberSaveable { mutableStateOf("home") }
    var running by rememberSaveable { mutableStateOf(false) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
            when (page) {
                "chat" -> ChatPage({ page = "home" }, { page = "models" })
                "models" -> ModelsPage({ page = "settings" })
                "settings" -> SettingsPage({ page = "home" }, { page = "chat" }, { page = "models" })
                else -> HomePage(
                    microphoneGranted = microphoneGranted,
                    running = running,
                    onChat = { page = "chat" },
                    onSettings = { page = "settings" },
                    onVoice = {
                        if (!microphoneGranted) onRequestPermissions()
                        else { if (running) onStopVoice() else onStartVoice(); running = !running }
                    }
                )
            }
        }
    }
}

@Composable
private fun HomePage(
    microphoneGranted: Boolean,
    running: Boolean,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onVoice: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
            IconSquare(Icons.Default.ChatBubble, "Chat", Cyan, onChat)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Siya Ai", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text(text = "YOUR AI COMPANION", color = Muted, fontSize = 8.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.weight(1f))
            IconSquare(Icons.Default.Settings, "Settings", Purple, onSettings)
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "LISTENS   •   UNDERSTANDS   •   CONTROLS", color = Cyan.copy(.88f), fontSize = 9.sp, letterSpacing = 2.1.sp)
        ProHologram(active = running, modifier = Modifier.fillMaxWidth().height(510.dp))
        Text(text = if (running) "Listening..." else "Tap to Speak", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text = "Hindi  •  Hinglish  •  English", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(17.dp))
        Surface(
            modifier = Modifier.size(112.dp).clickable(onClick = onVoice),
            shape = CircleShape,
            color = if (running) Purple.copy(.22f) else Panel2,
            border = BorderStroke(2.dp, if (running) Cyan else Purple)
        ) {
            Icon(imageVector = if (running) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Microphone", tint = Color.White, modifier = Modifier.padding(30.dp))
        }
        Text(text = if (!microphoneGranted) "Tap to allow microphone" else if (running) "Tap to stop" else "Tap to speak", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        Spacer(Modifier.height(12.dp))
        Text(text = "—   A L W A Y S   W I T H   Y O U   —", color = Blue, fontSize = 8.sp, letterSpacing = 2.4.sp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun IconSquare(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, accent: Color, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(50.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Panel, border = BorderStroke(1.dp, accent.copy(.75f))) {
        Icon(imageVector = icon, contentDescription = description, tint = accent, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun ChatPage(onBack: () -> Unit, onModels: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var status by remember { mutableStateOf(if (installed) "Qwen 2.5 1.5B • Offline" else "Local model not installed") }
    DisposableEffect(Unit) { onDispose { engine.close() } }
    Column(modifier = Modifier.fillMaxSize().background(Bg).padding(15.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Siya Chat", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(text = status, color = if (installed) Cyan else Muted, fontSize = 10.sp)
            }
            Surface(modifier = Modifier.clickable(onClick = onModels), shape = RoundedCornerShape(12.dp), color = Panel, border = BorderStroke(1.dp, Blue.copy(.65f))) {
                Text(text = "MODEL", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp))
            }
        }
        if (!installed) {
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onModels), shape = RoundedCornerShape(16.dp), color = Panel, border = BorderStroke(1.dp, Purple.copy(.5f))) {
                Row(modifier = Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, null, tint = Purple)
                    Spacer(Modifier.width(10.dp))
                    Text(text = "Install Qwen for offline chat", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(74.dp), shape = CircleShape, color = Panel2, border = BorderStroke(1.dp, Purple.copy(.55f))) { Icon(Icons.Default.AutoAwesome, null, tint = Cyan, modifier = Modifier.padding(22.dp)) }
                    Spacer(Modifier.height(13.dp))
                    Text(text = "Offline Chat", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Messages stay on this device", color = Muted, fontSize = 12.sp)
                }
            }
            items(messages) { message ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.first) Arrangement.End else Arrangement.Start) {
                    Surface(shape = RoundedCornerShape(18.dp), color = if (message.first) Purple.copy(.22f) else Panel, border = BorderStroke(1.dp, if (message.first) Purple.copy(.45f) else Color.White.copy(.04f))) {
                        Text(text = message.second, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(13.dp))
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text(text = "Ask Siya offline...", color = Muted) }, shape = RoundedCornerShape(20.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Panel2, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan))
            Spacer(Modifier.width(5.dp))
            IconButton(enabled = input.isNotBlank() && !busy, onClick = {
                val prompt = input.trim(); input = ""; messages += true to prompt; busy = true
                scope.launch {
                    try {
                        check(store.isInstalled()) { "Qwen model is not installed" }
                        val result = engine.complete(prompt)
                        messages += false to result.text
                        installed = true
                        status = String.format(Locale.US, "Qwen 2.5 1.5B • Offline • %.1f tok/s", result.tokensPerSecond)
                    } catch (e: Exception) {
                        messages += false to "Local AI error: ${e.message ?: "model unavailable"}"
                        status = "Local model error"
                    } finally { busy = false }
                }
            }) { Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank() && !busy) Cyan else Muted) }
        }
    }
}

@Composable
private fun SettingsPage(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
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
private fun ModelsPage(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; error = null
            try { importVerifiedModel(context, uri, store); installed = true }
            catch (e: Exception) { error = e.message ?: "Import failed" }
            finally { busy = false }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
        PageHeader("AI Models", onBack)
        Spacer(Modifier.height(12.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Panel, border = BorderStroke(1.dp, Purple.copy(.38f))) {
            Column(modifier = Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, null, tint = Purple)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Qwen 2.5 1.5B Instruct", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Q4_K_M • ~1.12 GB • Local LLM", color = Muted, fontSize = 11.sp)
                    }
                    Text(text = if (installed) "READY" else "OFFLINE", color = if (installed) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (busy && total > 0) LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Cyan)
        Spacer(Modifier.height(8.dp))
        Button(enabled = !busy && !installed, onClick = {
            scope.launch {
                busy = true; error = null; done = 0; total = 0
                try { LlmModelInstaller(store).download { d, t -> done = d; total = t }; installed = true }
                catch (e: Exception) { error = e.message ?: "Download failed" }
                finally { busy = false }
            }
        }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
            Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(text = if (installed) "Model Installed" else if (busy) "Downloading…" else "Download Qwen Model", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        OutlinedButton(enabled = !busy, onClick = { launcher.launch(arrayOf("application/octet-stream", "application/*")) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text(text = "Import GGUF from device")
        }
        if (error != null) Text(text = error!!, color = Color(0xFFFF7187), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(18.dp))
        Text(text = "The verified model is stored in the app-private directory and is activated atomically only after SHA-256 verification.", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = "Pipeline", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = "Audio → VAD → Hindi STT → Local Qwen → TTS", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
        Text(text = title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(20.dp), color = Panel, border = BorderStroke(1.dp, Color.White.copy(.035f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(13.dp), color = Panel2) { Icon(imageVector = icon, contentDescription = null, tint = if (title == "AI Models") Cyan else Purple, modifier = Modifier.padding(11.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(text = subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)) }
            if (onClick != null) Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

private suspend fun importVerifiedModel(context: Context, uri: Uri, store: LlmModelStore) = withContext(Dispatchers.IO) {
    val temp = File(store.modelFile.parentFile, "${store.modelFile.name}.import")
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to read selected file" }
        temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024); output.fd.sync() }
    }
    require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "GGUF file is incomplete" }
    val digest = MessageDigest.getInstance("SHA-256")
    temp.inputStream().use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
    }
    val hash = digest.digest().joinToString("") { "%02x".format(it) }
    require(hash.equals(LlmModelStore.MODEL_SHA256, true)) { "SHA-256 mismatch: expected Qwen Q4_K_M" }
    require(temp.renameTo(store.modelFile)) { "Could not activate imported model" }
    store.validate().getOrThrow()
}
