package com.siya.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.stt.SttModelSource
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.launch

private val SpeechBg = Color(0xFF01040B)
private val SpeechPanel = Color(0xFF07111F)
private val SpeechCyan = Color(0xFF21D4FF)
private val SpeechBlue = Color(0xFF168BFF)
private val SpeechPurple = Color(0xFF9B5CFF)
private val SpeechGreen = Color(0xFF55F2B6)
private val SpeechMuted = Color(0xFF8995AB)

@Composable
fun SpeechModelsDetailScreen(onBack: () -> Unit, vadStore: VadModelStore, sttStore: SttModelStore, installer: VoiceModelInstaller) {
    val scope = rememberCoroutineScope()
    var vadInstalled by remember { mutableStateOf(vadStore.isInstalled()) }
    var sttInstalled by remember { mutableStateOf(sttStore.isInstalled()) }
    var busy by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun downloadVad() {
        if (busy || vadInstalled) return
        scope.launch {
            busy = true; current = "Silero VAD"; done = 0L; total = 0L; message = null; error = null
            try { installer.downloadVad { d, t -> done = d; total = t }; vadInstalled = true; message = "Silero VAD is ready." }
            catch (e: Exception) { error = e.message ?: "VAD download failed" }
            finally { busy = false; current = null }
        }
    }

    fun downloadStt() {
        if (busy || sttInstalled) return
        scope.launch {
            busy = true; current = "Hindi STT"; done = 0L; total = 0L; message = null; error = null
            try { installer.downloadHindiStt { d, t -> done = d; total = t }; sttInstalled = true; message = "Hindi STT model + tokens are ready." }
            catch (e: Exception) { error = e.message ?: "STT download failed" }
            finally { busy = false; current = null }
        }
    }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SpeechBg, Color(0xFF020A18), SpeechBg))).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f).padding(start = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Speech", color = SpeechCyan, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(7.dp)); Text("Models", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold) }
                Text("VAD • STT • Tokens • Offline voice pipeline", color = Color.White.copy(.72f), fontSize = 10.sp)
            }
            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = SpeechPanel, border = BorderStroke(1.dp, SpeechCyan.copy(.7f))) { Icon(Icons.Default.GraphicEq, null, tint = SpeechCyan, modifier = Modifier.padding(9.dp)) }
        }
        PipelineCard(); Spacer(Modifier.height(14.dp))
        SpeechAssetCard("Silero VAD", "VAD", "Detects when you start and stop speaking before STT runs.", listOf("Engine" to "Silero Voice Activity Detection", "Input" to "16 kHz audio", "Runtime" to "On-device / offline", "File" to "onnx/model.onnx", "Source" to "onnx-community/silero-vad"), SpeechCyan, vadInstalled, busy && current == "Silero VAD", done, total, ::downloadVad)
        Spacer(Modifier.height(12.dp))
        SpeechAssetCard("Hindi STT", "STT", "The actual Hindi speech-to-text model used by Siya's offline voice path.", listOf("Architecture" to SttModelSource.MODEL_ARCHITECTURE, "Quantization" to SttModelSource.QUANTIZATION, "Language" to "Hindi (hi)", "Sample rate" to "16 kHz", "Model" to "model.int8.onnx", "Tokenizer" to "tokens.txt", "Source" to SttModelSource.REPOSITORY), SpeechBlue, sttInstalled, busy && current == "Hindi STT", done, total, ::downloadStt)
        Spacer(Modifier.height(12.dp)); AssetInfoCard(); Spacer(Modifier.height(12.dp))
        Surface(modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(18.dp), clip = true), shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.dp, SpeechPurple.copy(.55f))) {
            Column(Modifier.background(Brush.linearGradient(listOf(SpeechPurple.copy(.10f), SpeechPanel.copy(.96f)))).padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Settings, null, tint = SpeechPurple, modifier = Modifier.size(23.dp)); Spacer(Modifier.width(9.dp)); Text("Voice pipeline", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(9.dp)); Text("Microphone → Silero VAD → Hindi STT → Qwen Local LLM → Siya response", color = Color.White.copy(.78f), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        message?.let { Text(it, color = SpeechGreen, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp)) }
        error?.let { Text(it, color = Color(0xFFFF7187), fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp)) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun PipelineCard() {
    Surface(modifier = Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(20.dp), clip = true), shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = BorderStroke(1.2.dp, SpeechCyan.copy(.7f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(SpeechCyan.copy(.14f), SpeechPanel.copy(.96f), SpeechPurple.copy(.07f)))).padding(15.dp)) {
            Text("ACTIVE OFFLINE SPEECH PIPELINE", color = SpeechCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                PipelineNode("MIC", SpeechCyan); Text("→", color = SpeechMuted, fontSize = 18.sp); PipelineNode("VAD", SpeechCyan); Text("→", color = SpeechMuted, fontSize = 18.sp); PipelineNode("STT", SpeechBlue); Text("→", color = SpeechMuted, fontSize = 18.sp); PipelineNode("LLM", SpeechPurple)
            }
            Spacer(Modifier.height(10.dp)); Text("Only the assets listed below are part of the current local speech implementation.", color = SpeechMuted, fontSize = 9.sp, lineHeight = 14.sp)
        }
    }
}

@Composable private fun PipelineNode(text: String, accent: Color) {
    Surface(modifier = Modifier.size(53.dp), shape = RoundedCornerShape(14.dp), color = accent.copy(.08f), border = BorderStroke(1.dp, accent.copy(.62f))) { Box(contentAlignment = Alignment.Center) { Text(text, color = accent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) } }
}

@Composable private fun SpeechAssetCard(title: String, tag: String, subtitle: String, details: List<Pair<String, String>>, accent: Color, installed: Boolean, downloading: Boolean, done: Long, total: Long, onDownload: () -> Unit) {
    val shape = RoundedCornerShape(21.dp)
    Surface(modifier = Modifier.fillMaxWidth().shadow(16.dp, shape, clip = true), shape = shape, color = Color.Transparent, border = BorderStroke(1.3.dp, accent.copy(.78f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(accent.copy(.14f), SpeechPanel.copy(.97f), accent.copy(.04f)))).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(57.dp), shape = RoundedCornerShape(16.dp), color = accent.copy(.07f), border = BorderStroke(1.2.dp, accent.copy(.72f))) { Icon(if (tag == "STT") Icons.Default.GraphicEq else Icons.Default.Speed, null, tint = accent, modifier = Modifier.padding(14.dp)) }
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.width(7.dp)); Surface(shape = RoundedCornerShape(7.dp), color = Color.Transparent, border = BorderStroke(1.dp, accent.copy(.72f))) { Text(tag, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
                    Text(subtitle, color = Color.White.copy(.72f), fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (installed) Icon(Icons.Default.CheckCircle, null, tint = SpeechGreen, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.height(13.dp))
            details.forEach { (key, value) -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) { Text(key, color = SpeechMuted, fontSize = 9.sp, modifier = Modifier.width(88.dp)); Text(value, color = Color.White.copy(.84f), fontSize = 9.sp, modifier = Modifier.weight(1f)) } }
            if (downloading && total > 0L) {
                val progress = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Spacer(Modifier.height(9.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Downloading…", color = Color.White.copy(.75f), fontSize = 9.sp); Text("${(progress * 100).toInt()}%", color = SpeechCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp), color = accent)
            }
            Spacer(Modifier.height(12.dp)); Button(onClick = onDownload, enabled = !installed && !downloading, modifier = Modifier.fillMaxWidth().height(48.dp).shadow(10.dp, RoundedCornerShape(14.dp), clip = true), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black, disabledContainerColor = Color(0xFF12302D), disabledContentColor = SpeechGreen)) { Icon(if (installed) Icons.Default.CheckCircle else Icons.Default.CloudDownload, null); Spacer(Modifier.width(7.dp)); Text(if (installed) "Downloaded & Ready" else if (downloading) "Downloading…" else "Download This Model", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable private fun AssetInfoCard() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.Transparent, border = BorderStroke(1.dp, SpeechBlue.copy(.62f))) {
        Row(Modifier.background(Brush.linearGradient(listOf(SpeechBlue.copy(.09f), SpeechPanel.copy(.9f)))).padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = SpeechBlue, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(10.dp)); Column {
                Text("What is actually downloaded?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text("VAD downloads the Silero ONNX model. Hindi STT downloads the IndicConformer INT8 ONNX model and its tokens.txt tokenizer. These are the speech assets wired into the current app implementation.", color = Color.White.copy(.72f), fontSize = 9.sp, lineHeight = 14.sp)
            }
        }
    }
}
