package com.siya.ai.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
private val Pink = Color(0xFFBE5CFF)
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
        Surface(Modifier.fillMaxSize(), color = Bg) {
            when (page) {
                "chat" -> ProChat({ page = "home" }, { page = "models" })
                "models" -> ProModels({ page = "settings" })
                "settings" -> ProSettings({ page = "home" }, { page = "chat" }, { page = "models" })
                else -> ProHome(
                    microphoneGranted = microphoneGranted,
                    running = running,
                    onChat = { page = "chat" },
                    onSettings = { page = "settings" },
                    onRequestPermissions = onRequestPermissions,
                    onVoice = {
                        if (microphoneGranted) {
                            if (running) onStopVoice() else onStartVoice()
                            running = !running
                        } else onRequestPermissions()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProHome(
    microphoneGranted: Boolean,
    running: Boolean,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onVoice: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(Icons.Default.ChatBubble, "Chat", Cyan, onChat)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Siya Ai", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("YOUR AI COMPANION", color = Muted, fontSize = 8.sp, letterSpacing = 3.sp)
            }
            Spacer(Modifier.weight(1f))
            GlowButton(Icons.Default.Settings, "Settings", Purple, onSettings)
        }
        Spacer(Modifier.height(8.dp))
        Text("LISTENS   •   UNDERSTANDS   •   CONTROLS", color = Cyan.copy(.88f), fontSize = 9.sp, letterSpacing = 2.2.sp)
        ProHologram(active = running, modifier = Modifier.fillMaxWidth().height(500.dp))
        Text(if (running) "Listening..." else "Tap to Speak", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Hindi  •  Hinglish  •  English", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.size(116.dp).clickable(onClick = onVoice),
            shape = CircleShape,
            color = if (running) Purple.copy(.24f) else Panel2,
            border = BorderStroke(2.dp, if (running) Cyan else Purple)
        ) { Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, "Microphone", tint = Color.White, modifier = Modifier.padding(32.dp)) }
        Text(
            if (!microphoneGranted) "Tap to allow microphone" else if (running) "Tap to stop" else "Tap to speak",
            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text("—   A L W A Y S   W I T H   Y O U   —", color = Blue, fontSize = 8.sp, letterSpacing = 2.5.sp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GlowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(50.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Panel,
        border = BorderStroke(1.dp, accent.copy(.75f))
    ) { Icon(icon, label, tint = accent, modifier = Modifier.padding(12.dp)) }
}

@Composable
private fun ProChat(onBack: () -> Unit, onModels: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var modelStatus by remember { mutableStateOf(if (installed) "Qwen 2.5 1.5B • Offline" else "Local model not installed") }
    DisposableEffect(Unit) { onDispose { engine.close() } }

    Column(Modifier.fillMaxSize().background(Bg).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("Siya Chat", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(modelStatus, color = if (installed) Cyan else Muted, fontSize = 10.sp)
            }
            Surface(Modifier.clickable(onClick = onModels), RoundedCornerShape(12.dp), Panel, border = BorderStroke(1.dp, Blue.copy(.65f))) {
                Text("MODEL", Modifier.padding(horizontal = 11.dp, vertical = 9.dp), Cyan, 9.sp, FontWeight.Bold)
            }
        }
        if (!installed) {
            Surface(Modifier.fillMaxWidth().clickable(onClick = onModels), RoundedCornerShape(16.dp), Panel, border = BorderStroke(1.dp, Purple.copy(.5f))) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, null, Purple); Spacer(Modifier.width(10.dp)); Text("Install Qwen for offline chat", Color.White, 12.sp, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, Muted)
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(Modifier.size(74.dp), CircleShape, Panel2, border = BorderStroke(1.dp, Purple.copy(.55f))) { Icon(Icons.Default.AutoAwesome, null, Cyan, Modifier.padding(22.dp)) }
                    Spacer(Modifier.height(13.dp)); Text("Offline Chat", Color.White, 19.sp, FontWeight.Bold); Text("Messages stay on this device", Muted, 12.sp)
                }
            }
            items(messages) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.first) Arrangement.End else Arrangement.Start) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (message.first) Purple.copy(.22f) else Panel,
                        border = BorderStroke(1.dp, if (message.first) Purple.copy(.45f) else Color.White.copy(.04f))
                    ) { Text(message.second, Modifier.padding(13.dp), Color.White, 14.sp) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Siya offline...", color = Muted) }, shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                        modelStatus = String.format(Locale.US, "Qwen 2.5 1.5B • Offline • %.1f tok/s", result.tokensPerSecond)
                    } catch (e: Exception) {
                        messages += false to "Local AI error: ${e.message ?: "model unavailable"}"
                        modelStatus = "Local model error"
                    } finally { busy = false }
                }
            }) { Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank() && !busy) Cyan else Muted) }
        }
    }
}

@Composable
private fun ProSettings(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
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
private fun ProModels(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp)) {
        PageHeader("AI Models", onBack)
        Spacer(Modifier.height(12.dp))
        ModelCard("Qwen 2.5 1.5B Instruct", "Q4_K_M • ~1.12 GB • Local LLM", installed, Purple)
        Spacer(Modifier.height(10.dp))
        if (downloading && total > 0) LinearProgressIndicator(progress = { (done.toFloat() / total).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Cyan)
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = !downloading && !installed,
            onClick = {
                scope.launch {
                    downloading = true; error = null; done = 0; total = 0
                    try { LlmModelInstaller(store).download { d, t -> done = d; total = t }; installed = true }
                    catch (e: Exception) { error = e.message ?: "Download failed" }
                    finally { downloading = false }
                }
            }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text(if (downloading) "Downloading…" else if (installed) "Model Installed" else "Download Qwen Model", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(9.dp))
        OutlinedButton(enabled = !downloading, onClick = { launcher.launch(arrayOf("application/octet-stream", "application/*")) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Import GGUF from device")
        }
        if (error != null) Text(error!!, color = Color(0xFFFF7187), fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(18.dp))
        Text("Installed models are kept in the app-private model directory. Downloads use a temporary file and SHA-256 verification before activation.", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
        Text("Pipeline", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Audio → VAD → Hindi STT → Local Qwen → TTS", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), RoundedCornerShape(20.dp), Panel, border = BorderStroke(1.dp, Color.White.copy(.035f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), RoundedCornerShape(13.dp), Panel2) { Icon(icon, null, tint = if (title == "AI Models") Cyan else Purple, modifier = Modifier.padding(11.dp)) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, Color.White, 16.sp, FontWeight.SemiBold); Spacer(Modifier.height(3.dp)); Text(subtitle, Muted, 12.sp) }
            if (onClick != null) Icon(Icons.Default.ChevronRight, null, Muted)
        }
    }
}

@Composable
private fun ModelCard(title: String, subtitle: String, installed: Boolean, accent: Color) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), Panel, border = BorderStroke(1.dp, accent.copy(.35f))) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Memory, null, accent); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, Color.White, 16.sp, FontWeight.Bold); Text(subtitle, Muted, 11.sp) }; Text(if (installed) "READY" else "NOT INSTALLED", color = if (installed) Cyan else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private suspend fun importModel(context: Context, uri: Uri, store: LlmModelStore) = withContext(Dispatchers.IO) {
    val temp = File(store.modelFile.parentFile, "${store.modelFile.name}.import")
    context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "Unable to read selected file" }; FileOutputStreamCompat.copy(input, temp) }
    require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "GGUF file is incomplete" }
    val digest = MessageDigest.getInstance("SHA-256")
    temp.inputStream().use { input -> val buffer = ByteArray(1024 * 1024); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
    val hash = digest.digest().joinToString("") { "%02x".format(it) }
    require(hash.equals(LlmModelStore.MODEL_SHA256, true)) { "SHA-256 mismatch: this is not the expected Qwen Q4_K_M model" }
    require(temp.renameTo(store.modelFile)) { "Could not activate imported model" }
    store.validate().getOrThrow()
}

private object FileOutputStreamCompat {
    fun copy(input: java.io.InputStream, target: File) {
        target.outputStream().use { output -> input.copyTo(output, 1024 * 1024); output.fd.sync() }
    }
}
