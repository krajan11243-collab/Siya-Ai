package com.siya.ai.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val Black = Color(0xFF010309)
private val Panel = Color(0xFF07101F)
private val Panel2 = Color(0xFF0B1830)
private val Purple = Color(0xFF9B5CFF)
private val Blue = Color(0xFF168BFF)
private val Cyan = Color(0xFF21D4FF)
private val Pink = Color(0xFFBE5CFF)
private val Muted = Color(0xFF8995AB)

@Composable
fun SiyaApp(
    microphoneGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit
) {
    var running by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Black, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Black) {
            when (page) {
                "settings" -> SettingsScreen({ page = "home" }, { page = "chat" }, { page = "models" })
                "chat" -> ChatScreen({ page = "home" }, { page = "models" })
                "models" -> ModelsScreen { page = "settings" }
                else -> HomeScreen(
                    microphoneGranted, running,
                    onChat = { page = "chat" },
                    onSettings = { page = "settings" },
                    onRequestPermissions = onRequestPermissions,
                    onToggleVoice = { if (running) onStopVoice() else onStartVoice(); running = !running }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    microphoneGranted: Boolean,
    running: Boolean,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleVoice: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "hologram")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing)), label = "rotation")
    val pulse by transition.animateFloat(.96f, 1.05f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse")

    Column(
        Modifier.fillMaxSize().background(Black).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(62.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onChat,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Panel)
                    .border(1.dp, Cyan.copy(.7f), RoundedCornerShape(16.dp))
            ) { Icon(Icons.Default.ChatBubble, "Chat", tint = Cyan, modifier = Modifier.size(25.dp)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Siya Ai", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                    Text("YOUR AI COMPANION", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
                }
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Panel)
                    .border(1.dp, Purple.copy(.75f), RoundedCornerShape(16.dp))
            ) { Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(27.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Text("LISTENS   •   UNDERSTANDS   •   CONTROLS", Modifier.fillMaxWidth(), Cyan.copy(.9f), 10.sp, letterSpacing = 2.2.sp, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(480.dp), contentAlignment = Alignment.Center) { HologramOrb(rotation, pulse, running) }
        Text(if (running) "Listening..." else "Tap to Speak", Modifier.fillMaxWidth(), Color.White, 24.sp, FontWeight.Bold, textAlign = TextAlign.Center)
        Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 5.dp), Muted, 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(15.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                Modifier.size(106.dp).clickable { if (microphoneGranted) onToggleVoice() else onRequestPermissions() },
                CircleShape, if (running) Purple.copy(.24f) else Panel2,
                border = androidx.compose.foundation.BorderStroke(2.dp, if (running) Cyan else Purple)
            ) { Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, "Microphone", Color.White, Modifier.padding(31.dp)) }
        }
        Text(if (!microphoneGranted) "Allow microphone" else if (running) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth().padding(top = 7.dp), Muted, 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth(), Blue, 9.sp, letterSpacing = 2.5.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HologramOrb(rotation: Float, pulse: Float, running: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f; val cy = size.height / 2f; val base = minOf(size.width, size.height) * .34f
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(if (running) .27f else .20f), Blue.copy(.09f), Color.Transparent)), base * 1.65f, Offset(cx, cy))
        rotate(rotation, Offset(cx, cy)) {
            drawOval(Cyan.copy(.8f), Offset(cx - base * 1.38f, cy - base * .36f), Size(base * 2.76f, base * .72f), style = Stroke(2.2f))
            drawOval(Purple.copy(.72f), Offset(cx - base * 1.27f, cy - base * .66f), Size(base * 2.54f, base * 1.32f), style = Stroke(1.7f))
            drawOval(Blue.copy(.5f), Offset(cx - base * 1.05f, cy - base * .92f), Size(base * 2.1f, base * 1.84f), style = Stroke(1.2f))
            drawArc(Pink, 15f, 105f, false, Offset(cx - base, cy - base), Size(base * 2f, base * 2f), style = Stroke(6f, cap = StrokeCap.Round))
            drawArc(Cyan, 198f, 112f, false, Offset(cx - base, cy - base), Size(base * 2f, base * 2f), style = Stroke(6f, cap = StrokeCap.Round))
        }
        val r = base * pulse
        drawCircle(Blue.copy(.08f), r, Offset(cx, cy)); drawCircle(Cyan.copy(.72f), r, Offset(cx, cy), style = Stroke(2f))
        drawCircle(Purple.copy(.52f), r * .73f, Offset(cx, cy), style = Stroke(1.5f)); drawCircle(Cyan.copy(.32f), r * .48f, Offset(cx, cy), style = Stroke(1.2f))
        drawLine(Blue.copy(.72f), Offset(cx, cy - base * 1.55f), Offset(cx, cy + base * 1.55f), 1.5f)
        drawLine(Purple.copy(.58f), Offset(cx - base * 1.55f, cy), Offset(cx + base * 1.55f, cy), 1.5f)
        for (i in 0 until 32) {
            val a = Math.toRadians(i * 11.25 + rotation * .12); val rr = base * (1.05f + (i % 4) * .11f)
            drawCircle(if (i % 2 == 0) Cyan else Purple, if (i % 5 == 0) 3.2f else 2f, Offset(cx + cos(a).toFloat() * rr, cy + sin(a).toFloat() * rr))
        }
        drawCircle(Cyan.copy(.20f), 28f, Offset(cx, cy)); drawCircle(Color.White, 5.5f, Offset(cx, cy)); drawCircle(Cyan, 12f, Offset(cx, cy), style = Stroke(2f))
    }
}

@Composable
private fun ChatScreen(onBack: () -> Unit, onModels: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope(); val store = remember { LlmModelStore(context) }; val engine = remember { LocalLlmEngine(store) }
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() }; var input by rememberSaveable { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(store.isInstalled()) }
    var status by remember { mutableStateOf(if (ready) "Qwen 2.5 1.5B • Offline" else "Local model not installed") }
    DisposableEffect(Unit) { onDispose { engine.close() } }
    Column(Modifier.fillMaxSize().background(Black).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) { Text("Siya Chat", Color.White, 23.sp, FontWeight.Bold); Text(status, if (ready) Cyan else Muted, 10.sp) }
            Surface(Modifier.clickable(onClick = onModels), RoundedCornerShape(12.dp), Panel, border = androidx.compose.foundation.BorderStroke(1.dp, Blue.copy(.6f))) { Text("MODEL", Modifier.padding(10.dp), Cyan, 9.sp, FontWeight.Bold) }
        }
        if (!ready) {
            Surface(Modifier.fillMaxWidth().clickable(onClick = onModels), RoundedCornerShape(16.dp), Panel, border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(.45f))) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudDownload, null, Purple); Spacer(Modifier.width(9.dp)); Text("Install Qwen for offline chat", Color.White, 12.sp, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, Muted) }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (messages.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(top = 55.dp), horizontalAlignment = Alignment.CenterHorizontally) { Surface(Modifier.size(76.dp), CircleShape, Panel2, border = androidx.compose.foundation.BorderStroke(1.dp, Purple.copy(.55f))) { Icon(Icons.Default.AutoAwesome, null, Cyan, Modifier.padding(23.dp)) }; Spacer(Modifier.height(14.dp)); Text("Talk to Siya", Color.White, 19.sp, FontWeight.Bold); Text("Private offline conversation", Muted, 12.sp) } }
            items(messages) { (user, text) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { Surface(if (user) Purple.copy(.22f) else Panel, RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (user) Purple.copy(.4f) else Color.White.copy(.04f))) { Text(text, Modifier.padding(13.dp), Color.White, 14.sp) } } }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Ask Siya offline...", color = Muted) }, shape = RoundedCornerShape(20.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Panel2, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Cyan))
            Spacer(Modifier.width(6.dp)); IconButton(enabled = input.isNotBlank() && !busy, onClick = { val prompt = input.trim(); input = ""; messages += true to prompt; busy = true; scope.launch { try { if (!store.isInstalled()) error("Qwen model is not installed"); val r = engine.complete(prompt); messages += false to r.text; ready = true; status = String.format(Locale.US, "Qwen 2.5 1.5B • Offline • %.1f tok/s", r.tokensPerSecond) } catch (e: Exception) { messages += false to "Local AI error: ${e.message ?: "model unavailable"}"; status = "Local model error" } finally { busy = false } } }) { Icon(Icons.Default.Send, "Send", tint = if (input.isNotBlank() && !busy) Cyan else Muted) }
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onChat: () -> Unit, onModels: () -> Unit) {
    SimplePage("Settings", onBack) {
        SettingRow("Chat", "Offline text chat with Local LLM", Icons.Default.ChatBubble, onChat)
        SettingRow("AI Models", "Download or import offline models", Icons.Default.Memory, onModels)
        SettingRow("Voice mode", "Voice-first assistant", Icons.Default.Mic, null)
        SettingRow("Privacy", "On-device processing", Icons.Default.Lock, null)
        SettingRow("Language", "Hindi / Hinglish / English", Icons.Default.Language, null)
        SettingRow("Background", "Controlled by Android permissions", Icons.Default.BatterySaver, null)
    }
}

@Composable
private fun ModelsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current; val scope = rememberCoroutineScope(); val store = remember { LlmModelStore(context) }
    var installed by remember { mutableStateOf(store.isInstalled()) }; var downloading by remember { mutableStateOf(false) }; var done by remember { mutableLongStateOf(0L) }; var total by remember { mutableLongStateOf(0L) }; var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch { downloading = true; error = null; try { context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input); val target = store.modelFile; val temp = File(target.parentFile, "${target.name}.import"); temp.outputStream().use { output -> input.copyTo(output, 1024 * 1024) }; require(temp.length() >= LlmModelStore.MIN_MODEL_BYTES) { "GGUF file is too small" }; require(temp.renameTo(target)) { "Could not install GGUF" } }; installed = store.isInstalled() } catch (e: Exception) { error = e.message ?: "Import failed" } finally { downloading = false } }
    }
    SimplePage("AI Models", onBack) {
        Text("OFFLINE MODEL MANAGER", Cyan, 10.sp, letterSpacing = 2.sp); Spacer(Modifier.height(12.dp))
        ModelRow("VAD", "Silero VAD • 16 kHz • ONNX", "Voice activity detection", "READY")
        ModelRow("Hindi STT", "IndicConformer • int8 ONNX", "Hindi speech-to-text", "READY")
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(44.dp), RoundedCornerShape(13.dp), Blue.copy(.12f)) { Icon(Icons.Default.Memory, null, Cyan, Modifier.padding(10.dp)) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text("Local LLM", Color.White, 15.sp, FontWeight.Bold); Text("Qwen 2.5 1.5B • Q4_K_M • ~1.12 GB", Muted, 10.sp) } }
                Spacer(Modifier.height(11.dp)); Text(if (installed) "INSTALLED • ready for offline chat" else "NOT INSTALLED • download once", if (installed) Cyan else Muted, 11.sp)
                if (downloading) { Spacer(Modifier.height(9.dp)); LinearProgressIndicator({ if (total > 0) done.toFloat() / total else 0f }, Modifier.fillMaxWidth(), color = Cyan, trackColor = Panel2); Text("${done / 1_048_576} MB / ${if (total > 0) total / 1_048_576 else "?"} MB", Muted, 10.sp) }
                Spacer(Modifier.height(11.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !downloading && !installed, modifier = Modifier.weight(1f), onClick = { downloading = true; done = 0; total = 0; error = null; scope.launch { try { LlmModelInstaller(store).download { d, t -> done = d; total = t }; installed = store.isInstalled() } catch (e: Exception) { error = e.message ?: "Download failed" } finally { downloading = false } } }, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("Download") }
                    OutlinedButton(enabled = !downloading, modifier = Modifier.weight(1f), onClick = { launcher.launch(arrayOf("application/octet-stream", "application/x-gguf", "*/*")) }) { Text("Import GGUF") }
                }
                if (error != null) { Spacer(Modifier.height(7.dp)); Text(error!!, Color(0xFFFF7B8A), 10.sp) }
            }
        }
        Spacer(Modifier.height(12.dp)); Text("Verified Qwen downloads are stored in private app storage so the model remains available after app restart.", Muted, 10.sp)
    }
}

@Composable
private fun ModelRow(title: String, model: String, description: String, status: String) {
    Card(Modifier.fillMaxWidth().padding(bottom = 9.dp), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, Color.White, 13.sp, FontWeight.SemiBold); Text(model, Muted, 10.sp); Text(description, Muted.copy(.7f), 9.sp) }; Text(status, Cyan, 8.sp, FontWeight.Bold) } }
}

@Composable
private fun SimplePage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Black).verticalScroll(rememberScrollState()).padding(18.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }; Text(title, Color.White, 24.sp, FontWeight.Bold) }; Spacer(Modifier.height(15.dp)); content() }
}

@Composable
private fun SettingRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth().padding(bottom = 9.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(41.dp), RoundedCornerShape(12.dp), Purple.copy(.1f)) { Icon(icon, null, Purple, Modifier.padding(9.dp)) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, Color.White, 13.sp, FontWeight.SemiBold); Text(value, Muted, 10.sp) }; if (onClick != null) Icon(Icons.Default.ChevronRight, null, Muted) } }
}
