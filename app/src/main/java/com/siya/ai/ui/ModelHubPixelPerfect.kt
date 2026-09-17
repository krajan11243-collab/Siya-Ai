package com.siya.ai.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.service.ModelDownloadService
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import com.siya.ai.llm.LlmModelStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PPBg = Color(0xFF020713)
private val PPPanel = Color(0xFF061426)
private val PPWhite = Color(0xFFF7F8FF)
private val PPMuted = Color(0xFFAAB9D6)
private val PPPurple = Color(0xFF8B35FF)
private val PPCyan = Color(0xFF13D9FF)
private val PPBlue = Color(0xFF0785FF)
private val PPGreen = Color(0xFF18F0B0)
private val PPRed = Color(0xFFFF365C)

private data class DemoModel(val name: String, val description: String, val size: String, val accent: Color, val icon: ImageVector, val downloaded: Boolean = false)

private val demoModels = listOf(
    DemoModel("GPT-4o Mini", "Fast & capable model", "~3.2 GB", PPGreen, Icons.Default.AutoAwesome),
    DemoModel("GPT-4o", "Most powerful model for reasoning", "~7.6 GB", PPCyan),
    DemoModel("Gemini 1.5 Flash", "Fast multimodal model by Google", "~2.1 GB", PPPurple, Icons.Default.Language),
    DemoModel("Gemini 1.5 Pro", "Advanced multimodal model", "~4.8 GB", PPBlue, Icons.Default.Language),
    DemoModel("Llama 3.2 1B", "Lightweight & mobile friendly", "~1.1 GB", PPBlue, Icons.Default.AllInclusive, true),
    DemoModel("Llama 3.1 8B", "Powerful open source model", "~4.7 GB", PPBlue, Icons.Default.AllInclusive),
    DemoModel("Qwen 2.5 0.5B", "Efficient coding & chat model", "~0.8 GB", PPPurple, Icons.Default.Code),
    DemoModel("Qwen 2.5 1.5B", "Balanced coding model", "~1.4 GB", PPCyan, Icons.Default.Code),
    DemoModel("DeepSeek Coder 1.3B", "Specialized for code generation", "~1.3 GB", PPCyan, Icons.Default.Code, true),
    DemoModel("DeepSeek Chat 7B", "General purpose chat model", "~4.1 GB", PPCyan, Icons.Default.Chat),
    DemoModel("Mistral 7B Instruct", "High quality open model", "~4.0 GB", PPPurple, Icons.Default.Memory),
    DemoModel("Mixtral 8x7B", "Advanced mixture of experts", "~26 GB", PPPurple, Icons.Default.Memory, true),
    DemoModel("Phi 3 Mini", "Small & highly capable", "~2.3 GB", PPCyan, Icons.Default.AutoAwesome),
    DemoModel("Phi 3 Medium", "Better reasoning & performance", "~5.6 GB", PPPurple, Icons.Default.AutoAwesome),
    DemoModel("Yi 1.5 6B", "Multilingual model", "~3.9 GB", PPPurple, Icons.Default.Cube, true)
)

@Composable
fun PixelPerfectModelHome(onBack: () -> Unit, onAllModels: () -> Unit, onSpeechModels: () -> Unit, onImport: () -> Unit) {
    val t = rememberInfiniteTransition(label = "pp-home")
    val pulse by t.animateFloat(.70f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PPBg, Color(0xFF031022), PPBg))).padding(horizontal = 27.dp, vertical = 10.dp)) {
        PixelPerfectHeader("Models", "Download, Select & Use On-Device Models", onBack, PPPurple)
        Spacer(Modifier.height(17.dp))
        PixelHeroCard("AI All Model Download Select", "Download all required models for complete AI\nvoice assistant experience", PPPurple, Icons.Default.Memory, pulse, onAllModels)
        Spacer(Modifier.height(22.dp))
        PixelHeroCard("Download Speech Models", "Download VAD, STT and all related models\n(One Click)", PPCyan, Icons.Default.GraphicEq, pulse, onSpeechModels)
        Spacer(Modifier.height(22.dp))
        PixelHeroCard("Import GGUF", "Select and import your own model file", PPPurple, Icons.Default.FolderOpen, pulse, onImport)
        Spacer(Modifier.height(22.dp))
        Surface(Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(17.dp)), RoundedCornerShape(17.dp), color = Color.Transparent, border = BorderStroke(1.dp, PPBlue.copy(.72f))) {
            Row(Modifier.background(Brush.linearGradient(listOf(PPBlue.copy(.10f), PPPanel.copy(.92f)))).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = PPBlue, modifier = Modifier.size(31.dp).alpha(pulse)); Spacer(Modifier.width(11.dp))
                Text("Model downloads run in a foreground service and continue\nwhile Siya Ai is closed. Progress stays in the notification.", color = PPWhite.copy(.86f), fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
        // Intentionally empty below the information card to match the supplied reference.
    }
}

@Composable
private fun PixelPerfectHeader(title: String, subtitle: String, onBack: () -> Unit, accent: Color) {
    Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, Modifier.size(50.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, Modifier.size(34.dp)) }
        Column(Modifier.weight(1f).padding(start = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI", color = PPCyan, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(7.dp)); Text(title, color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(subtitle, color = PPMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(Modifier.size(48.dp).shadow(12.dp, RoundedCornerShape(16.dp)), RoundedCornerShape(16.dp), color = Color.Transparent, border = BorderStroke(1.dp, accent.copy(.78f))) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Smartphone, null, tint = Color(0xFFFF48E8), Modifier.size(27.dp))
                Box(Modifier.size(8.dp).align(Alignment.TopEnd).offset((-7).dp, 7.dp).background(PPGreen, CircleShape))
            }
        }
    }
}

@Composable
private fun PixelHeroCard(title: String, subtitle: String, accent: Color, icon: ImageVector, pulse: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(23.dp)
    Surface(Modifier.fillMaxWidth().height(169.dp).shadow(24.dp, shape).clickable(onClick = onClick), shape, Color.Transparent, border = BorderStroke(1.8.dp, accent.copy(.98f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(.29f), Color(0xFF06152F).copy(.98f), accent.copy(.07f))))) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.18f * pulse), Color.Transparent))))
            Row(Modifier.fillMaxSize().padding(horizontal = 23.dp, vertical = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(91.dp).shadow(18.dp, CircleShape), CircleShape, Color.Transparent, border = BorderStroke(1.8.dp, accent.copy(.92f))) {
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.25f), Color.Transparent))), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accent, Modifier.size(53.dp).alpha(pulse))
                    }
                }
                Spacer(Modifier.width(25.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Spacer(Modifier.height(6.dp)); Text(subtitle, color = Color.White.copy(.83f), fontSize = 12.sp, lineHeight = 18.sp, maxLines = 2)
                }
                Surface(Modifier.size(58.dp).shadow(14.dp, CircleShape), CircleShape, accent.copy(.09f), border = BorderStroke(1.5.dp, accent.copy(.92f))) {
                    Icon(Icons.Default.ChevronRight, null, tint = accent, Modifier.padding(11.dp).alpha(pulse))
                }
            }
        }
    }
}

@Composable
fun PixelPerfectAllModelsScreen(onBack: () -> Unit, onDownload: (DemoModel) -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "pp-list")
    val pulse by t.animateFloat(.72f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "list-pulse")
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PPBg, Color(0xFF031022), PPBg))).padding(horizontal = 15.dp, vertical = 8.dp)) {
        PixelPerfectHeader("All Model Download Select", "Choose and download AI models for offline use", PPPurple, onBack)
        LazyColumn(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(demoModels) { model -> PixelModelRow(model, pulse) { onDownload(model) } }
        }
    }
}

@Composable
private fun PixelModelRow(model: DemoModel, pulse: Float, onDownload: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(Modifier.fillMaxWidth().height(88.dp).shadow(8.dp, shape), shape, Color.Transparent, border = BorderStroke(1.dp, model.accent.copy(.48f))) {
        Row(Modifier.background(Brush.linearGradient(listOf(model.accent.copy(.075f), PPPanel.copy(.95f), Color.Transparent))).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(72.dp), RoundedCornerShape(15.dp), Color.Transparent, border = BorderStroke(1.2.dp, model.accent.copy(.72f))) {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(model.accent.copy(.15f), Color.Transparent))), contentAlignment = Alignment.Center) { Icon(model.icon, null, tint = model.accent, Modifier.size(42.dp).alpha(pulse)) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(model.name, color = PPWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Spacer(Modifier.width(7.dp)); Surface(RoundedCornerShape(8.dp), Color.Transparent, border = BorderStroke(1.dp, PPCyan.copy(.72f))) { Text("LLM", color = PPCyan, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) } }
                Text(model.description, color = PPCyan.copy(.82f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(model.size, color = PPMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 5.dp))
                if (model.downloaded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.height(40.dp), RoundedCornerShape(10.dp), Color.Transparent, border = BorderStroke(1.2.dp, PPGreen.copy(.86f))) { Row(Modifier.padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Check, null, tint = PPGreen, Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text("Downloaded", color = PPGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                        Spacer(Modifier.width(7.dp)); Surface(Modifier.size(40.dp), RoundedCornerShape(10.dp), Color.Transparent, border = BorderStroke(1.2.dp, PPRed.copy(.88f)).clickable(onClick = {})) { Icon(Icons.Default.DeleteOutline, null, tint = PPRed, Modifier.padding(9.dp)) }
                    }
                } else {
                    Surface(Modifier.height(42.dp).shadow(9.dp, RoundedCornerShape(11.dp)), RoundedCornerShape(11.dp), Color.Transparent, border = BorderStroke(1.6.dp, model.accent.copy(.95f)).clickable(onClick = onDownload)) {
                        Box(Modifier.background(Brush.linearGradient(listOf(model.accent.copy(.18f), Color.Transparent))).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Download, null, tint = model.accent, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text("Download", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                    }
                }
            }
        }
    }
}

@Composable
fun PixelPerfectSpeechModelsScreen(onBack: () -> Unit, vadStore: VadModelStore, sttStore: SttModelStore, installer: VoiceModelInstaller) {
    val scope = rememberCoroutineScope(); var vad by remember { mutableStateOf(vadStore.isInstalled()) }; var stt by remember { mutableStateOf(sttStore.isInstalled()) }; var active by remember { mutableStateOf<String?>(null) }; var done by remember { mutableLongStateOf(0L) }; var total by remember { mutableLongStateOf(0L) }; var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PPBg, Color(0xFF031022), PPBg))).padding(horizontal = 15.dp, vertical = 8.dp)) {
        PixelPerfectHeader("Speech Models", "VAD • STT • Tokens • Offline voice pipeline", onBack, PPCyan)
        LazyColumn(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { SpeechPipeline() }
            item { SpeechRow("Silero VAD", "VAD", "Voice activity detection before speech recognition", PPCyan, vad, active == "VAD", done, total) { if (active == null && !vad) scope.launch { active = "VAD"; try { installer.downloadVad { d,t -> done=d; total=t }; vad=true; message="Silero VAD ready" } finally { active=null } } } }
            item { SpeechRow("Hindi STT", "STT", "AI4Bharat IndicConformer Hindi speech-to-text", PPBlue, stt, active == "STT", done, total) { if (active == null && !stt) scope.launch { active = "STT"; try { installer.downloadHindiStt { d,t -> done=d; total=t }; stt=true; message="Hindi STT ready" } finally { active=null } } } }
            item { message?.let { Text(it, color = PPGreen, fontSize = 10.sp) } }
        }
    }
}

@Composable private fun SpeechPipeline() {
    Surface(Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(20.dp)), RoundedCornerShape(20.dp), Color.Transparent, border = BorderStroke(1.3.dp, PPCyan.copy(.70f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(PPCyan.copy(.12f), PPPanel.copy(.97f), PPPurple.copy(.06f)))).padding(14.dp)) {
            Text("ACTIVE OFFLINE SPEECH PIPELINE", color = PPCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(11.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                listOf("MIC" to PPCyan, "VAD" to PPCyan, "STT" to PPBlue, "LLM" to PPPurple).forEachIndexed { i, item -> if (i > 0) Text("→", color = PPMuted, fontSize = 15.sp); Surface(Modifier.size(49.dp), RoundedCornerShape(13.dp), item.second.copy(.08f), border = BorderStroke(1.dp, item.second.copy(.65f))) { Box(contentAlignment = Alignment.Center) { Text(item.first, color = item.second, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) } } }
            }
        }
    }
}

@Composable private fun SpeechRow(title: String, tag: String, description: String, accent: Color, installed: Boolean, downloading: Boolean, done: Long, total: Long, onDownload: () -> Unit) {
    Surface(Modifier.fillMaxWidth().shadow(13.dp, RoundedCornerShape(20.dp)), RoundedCornerShape(20.dp), Color.Transparent, border = BorderStroke(1.3.dp, accent.copy(.72f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(accent.copy(.13f), PPPanel.copy(.97f)))).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(61.dp), RoundedCornerShape(16.dp), accent.copy(.06f), border = BorderStroke(1.2.dp, accent.copy(.70f))) { Icon(if (tag == "STT") Icons.Default.GraphicEq else Icons.Default.Speed, null, tint = accent, Modifier.padding(14.dp)) }
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = PPWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(7.dp)); Surface(RoundedCornerShape(7.dp), Color.Transparent, border = BorderStroke(1.dp, accent.copy(.75f))) { Text(tag, color = accent, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) } }; Text(description, color = PPWhite.copy(.72f), fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp)) }
                if (installed) Icon(Icons.Default.CheckCircle, null, tint = PPGreen, Modifier.size(25.dp))
            }
            Spacer(Modifier.height(12.dp))
            if (tag == "VAD") { SpeechDetail("Engine", "Silero Voice Activity Detection"); SpeechDetail("Input", "16 kHz audio"); SpeechDetail("Runtime", "On-device / offline"); SpeechDetail("File", "onnx/model.onnx") } else { SpeechDetail("Architecture", "IndicConformer"); SpeechDetail("Quantization", "INT8"); SpeechDetail("Language", "Hindi (hi)"); SpeechDetail("Sample rate", "16 kHz"); SpeechDetail("Model", "model.int8.onnx"); SpeechDetail("Tokenizer", "tokens.txt") }
            if (downloading && total > 0) { val p=(done.toFloat()/total.toFloat()).coerceIn(0f,1f); Spacer(Modifier.height(7.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("Downloading…", color=PPMuted, fontSize=9.sp); Text("${(p*100).toInt()}%", color=PPCyan, fontSize=9.sp, fontWeight=FontWeight.Bold) }; LinearProgressIndicator(progress={p}, Modifier.fillMaxWidth().padding(top=5.dp), color=accent) }
            Spacer(Modifier.height(10.dp)); Button(onClick=onDownload, enabled=!installed&&!downloading, Modifier.fillMaxWidth().height(48.dp), RoundedCornerShape(13.dp), colors=ButtonDefaults.buttonColors(containerColor=accent, contentColor=Color.Black)) { Icon(if(installed) Icons.Default.Check else Icons.Default.Download, null); Spacer(Modifier.width(7.dp)); Text(if(installed) "Downloaded & Ready" else if(downloading) "Downloading…" else "Download This Model", fontWeight=FontWeight.Bold) }
        }
    }
}

@Composable private fun SpeechDetail(key: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical=2.dp)) { Text(key, color=PPMuted, fontSize=9.sp, Modifier.width(92.dp)); Text(value, color=PPWhite.copy(.86f), fontSize=9.sp, Modifier.weight(1f)) } }
