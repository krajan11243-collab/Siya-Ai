package com.siya.ai.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import java.util.Locale

private val FastBg = Color(0xFF01040B)
private val FastPanel = Color(0xFF07111F)
private val FastPurple = Color(0xFF9B5CFF)
private val FastCyan = Color(0xFF21D4FF)
private val FastBlue = Color(0xFF168BFF)
private val FastMuted = Color(0xFF8995AB)
private data class FastMessage(val user: Boolean, val text: String, val thinking: Boolean = false)

@Composable
fun SiyaFastApp(
    microphoneGranted: Boolean,
    onRequestMicrophone: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
) {
    val context = LocalContext.current
    val modelStore = remember { LlmModelStore(context) }
    val llm = remember { LocalLlmEngine(modelStore) }
    var llmReady by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }
    val voice by VoiceSessionState.state.collectAsState()

    LaunchedEffect(Unit) {
        if (modelStore.isInstalled()) {
            runCatching { llm.load(); llmReady = true }
        }
    }
    DisposableEffect(Unit) { onDispose { llm.close() } }

    MaterialTheme(colorScheme = darkColorScheme(primary = FastPurple, background = FastBg, surface = FastPanel)) {
        Surface(Modifier.fillMaxSize(), color = FastBg) {
            when (page) {
                "chat" -> FastChat(llm, llmReady, { page = "home" }, { page = "models" })
                "models" -> FastModels { page = "home" }
                "settings" -> FastSettings({ page = "home" }, { page = "chat" }, { page = "models" })
                else -> FastHome(microphoneGranted, voice, { page = "chat" }, { page = "settings" }) {
                    if (!microphoneGranted) onRequestMicrophone() else if (voice.active) onStopVoice() else onStartVoice()
                }
            }
        }
    }
}

@Composable
private fun FastHome(
    microphoneGranted: Boolean,
    voice: VoiceSessionState.State,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onMic: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 14.dp, vertical = 6.dp)) {
        val compact = maxHeight < 700.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(if (compact) 54.dp else 62.dp), verticalAlignment = Alignment.CenterVertically) {
                TopButton(Icons.Default.ChatBubble, FastCyan, onChat)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Siya Ai", color = Color.White, fontSize = if (maxWidth < 360.dp) 27.sp else 31.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Y O U R   A I   C O M P A N I O N", color = FastMuted, fontSize = 6.sp, letterSpacing = 1.2.sp)
                }
                Spacer(Modifier.weight(1f))
                TopButton(Icons.Default.Settings, FastPurple, onSettings)
            }
            Text("L I S T E N S   •   U N D E R S T A N D S   •   C O N T R O L S", Modifier.fillMaxWidth(), color = FastCyan, fontSize = 7.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { FastHologram(voice.active, Modifier.fillMaxSize()) }
            val status = when (voice.phase) {
                VoiceSessionState.Phase.LISTENING -> "Listening"
                VoiceSessionState.Phase.TRANSCRIBING -> "Understanding"
                VoiceSessionState.Phase.THINKING -> "Thinking…"
                VoiceSessionState.Phase.READY -> "Ready"
                VoiceSessionState.Phase.ERROR -> "Needs attention"
                VoiceSessionState.Phase.IDLE -> "Tap to Speak"
            }
            Text(status, Modifier.fillMaxWidth(), color = Color.White, fontSize = if (compact) 18.sp else 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            voice.error?.let { Text(it, Modifier.fillMaxWidth().padding(top = 4.dp), color = Color(0xFFFF7187), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2) }
            Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 3.dp), color = FastMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            Surface(Modifier.size(if (compact) 80.dp else 88.dp).align(Alignment.CenterHorizontally).clickable(onClick = onMic), CircleShape, color = if (voice.active) FastPurple.copy(.25f) else FastPanel, border = BorderStroke(2.dp, if (voice.active) FastCyan else FastPurple)) {
                Icon(if (voice.active) Icons.Default.Stop else Icons.Default.Mic, "Microphone", tint = Color.White, modifier = Modifier.padding(22.dp))
            }
            Text(if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak", Modifier.fillMaxWidth().padding(top = 3.dp), color = FastMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
            Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth().padding(top = 4.dp), color = FastBlue, fontSize = 6.sp, letterSpacing = 1.6.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TopButton(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, click: () -> Unit) {
    Surface(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).clickable(onClick = click), RoundedCornerShape(15.dp), FastPanel, border = BorderStroke(1.dp, accent.copy(.85f))) { Icon(icon, null, tint = accent, modifier = Modifier.padding(11.dp)) }
}

@Composable
private fun FastHologram(active: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "holo")
    val pulse by infinite.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse")
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * .48f
        val r = minOf(size.width, size.height) * .27f * pulse
        drawCircle(Color(0xFF0B4FFF).copy(alpha = .16f), r * 1.35f, Offset(cx, cy), blendMode = BlendMode.Screen)
        drawCircle(Color.Transparent, r, Offset(cx, cy), style = Stroke(width = 3.dp.toPx()), color = FastCyan)
        drawCircle(Color.Transparent, r * .76f, Offset(cx, cy), style = Stroke(width = 2.dp.toPx()), color = FastPurple.copy(.9f))
        drawCircle(Color.Transparent, r * .9f, Offset(cx, cy), style = Stroke(width = 1.dp.toPx()), color = FastBlue.copy(.8f))
        drawOval(Color.Transparent, Rect(cx-r*1.45f, cy-r*.18f, cx+r*1.45f, cy+r*.18f), style = Stroke(width = 2.dp.toPx()), color = FastCyan.copy(.75f))
        drawOval(Color.Transparent, Rect(cx-r*1.35f, cy-r*.16f, cx+r*1.35f, cy+r*.16f), style = Stroke(width = 1.dp.toPx()), color = FastPurple.copy(.8f))
        drawLine(FastCyan.copy(.35f), Offset(cx, cy-r*1.75f), Offset(cx, cy+r*1.75f), 1.dp.toPx())
        for (i in 1..5) drawLine(FastBlue.copy(.10f), Offset(cx-r*1.35f+i*r*.55f, cy-r*1.1f), Offset(cx-r*1.35f+i*r*.55f, cy+r*1.1f), 1.dp.toPx())
        drawCircle(Color.White, 4.dp.toPx(), Offset(cx, cy), blendMode = BlendMode.Screen)
        drawOval(Color.Transparent, Rect(cx-r*1.75f, cy+r*1.5f, cx+r*1.75f, cy+r*1.85f), style = Stroke(width = 2.dp.toPx()), color = FastPurple)
        drawOval(Color.Transparent, Rect(cx-r*1.55f, cy+r*1.58f, cx+r*1.55f, cy+r*1.78f), style = Stroke(width = 1.dp.toPx()), color = FastCyan)
        if (active) drawCircle(FastCyan.copy(.8f), r*.12f, Offset(cx, cy), blendMode = BlendMode.Screen)
    }
}

@Composable
private fun FastChat(engine: LocalLlmEngine, ready: Boolean, onBack: () -> Unit, onModels: () -> Unit) {
    val scope = rememberCoroutineScope(); val voice by VoiceSessionState.state.collectAsState(); val list = rememberLazyListState(); val messages = remember { mutableStateListOf<FastMessage>() }
    var input by rememberSaveable { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    fun submit() {
        if (!ready || busy || input.isBlank()) return
        val prompt = input.trim(); input = ""; messages += FastMessage(true, prompt); messages += FastMessage(false, "Thinking…", true); busy = true
        scope.launch {
            try { val result = engine.complete(prompt); val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = FastMessage(false, result.text.ifBlank { "I’m ready." }) }
            catch (e: Exception) { val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = FastMessage(false, "Local AI error: ${e.message ?: "model unavailable"}") }
            finally { busy = false }
        }
    }
    LaunchedEffect(messages.size, busy) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }
    LaunchedEffect(voice.phase, voice.transcript, voice.response, voice.error) {
        when (voice.phase) {
            VoiceSessionState.Phase.THINKING -> { val t = voice.transcript.trim(); if (t.isNotBlank() && messages.none { it.user && it.text == t }) messages += FastMessage(true, t); if (messages.none { it.thinking }) messages += FastMessage(false, "Thinking…", true); busy = true }
            VoiceSessionState.Phase.READY -> { val i = messages.indexOfLast { it.thinking }; if (i >= 0 && voice.response.isNotBlank()) messages[i] = FastMessage(false, voice.response.trim()); busy = false }
            VoiceSessionState.Phase.ERROR -> { val i = messages.indexOfLast { it.thinking }; if (i >= 0) messages[i] = FastMessage(false, voice.error ?: "Voice error"); busy = false }
            else -> Unit
        }
    }
    Scaffold(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), containerColor = FastBg, contentWindowInsets = WindowInsets(0,0,0,0), bottomBar = {
        Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text(if (ready) "Ask Siya…" else "Warming local AI…", color = FastMuted) }, enabled = ready && !busy, maxLines = 4, singleLine = false, shape = RoundedCornerShape(18.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { submit() }), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FastPurple, unfocusedBorderColor = FastPanel, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = FastCyan))
            IconButton(enabled = ready && !busy && input.isNotBlank(), onClick = ::submit) { Icon(Icons.Default.Send, null, tint = if (input.isNotBlank() && ready && !busy) FastCyan else FastMuted) }
        }
    }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 10.dp)) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Column(Modifier.weight(1f)) { Text("Siya Chat", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(if (ready) "Qwen 2.5 1.5B • Offline • Ready" else "Preparing local model…", color = if (ready) FastCyan else FastMuted, fontSize = 9.sp) }; TextButton(onClick = onModels) { Text("MODEL", color = FastCyan, fontSize = 8.sp) } }
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (messages.isEmpty()) item { Text(if (ready) "Fast private offline chat" else "Local model is warming up…", Modifier.fillMaxWidth().padding(top = 45.dp), color = FastMuted, textAlign = TextAlign.Center, fontSize = 11.sp) }
                itemsIndexed(messages) { _, m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.user) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(17.dp), color = if (m.user) FastPurple.copy(.22f) else FastPanel, border = BorderStroke(1.dp, if (m.user) FastPurple.copy(.4f) else Color.White.copy(.04f))) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { if (m.thinking) { CircularProgressIndicator(Modifier.size(14.dp), color = FastCyan, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }; Text(m.text, color = Color.White, fontSize = 13.sp) } } } }
            }
        }
    }
}

@Composable
private fun FastModels(onBack: () -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); val llm = remember { LlmModelStore(context) }; val vad = remember { VadModelStore(context) }; val stt = remember { SttModelStore(context) }; val voiceInstaller = remember { VoiceModelInstaller(vad, stt) }
    var qwen by remember { mutableStateOf(llm.isInstalled()) }; var vadReady by remember { mutableStateOf(vad.isInstalled()) }; var sttReady by remember { mutableStateOf(stt.isInstalled()) }; var busy by remember { mutableStateOf(false) }; var done by remember { mutableLongStateOf(0L) }; var total by remember { mutableLongStateOf(0L) }; var status by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }; Text("AI Models", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
        FastModelCard("Qwen 2.5 1.5B", "Q4_K_M • ~1.12 GB", qwen, FastPurple); Spacer(Modifier.height(8.dp)); FastModelCard("Silero VAD", "16 kHz speech detection", vadReady, FastCyan); Spacer(Modifier.height(8.dp)); FastModelCard("Hindi STT", "IndicConformer • Offline", sttReady, FastBlue); Spacer(Modifier.height(14.dp))
        if (busy) { val p = if (total > 0) (done.toDouble()/total*100.0).coerceIn(0.0,100.0) else 0.0; LinearProgressIndicator({ (p/100).toFloat() }, Modifier.fillMaxWidth(), color = FastCyan); Text(String.format(Locale.US, "%.1f%%  •  %s / %s MB", p, done/1048576, if (total > 0) total/1048576 else "?"), Modifier.fillMaxWidth().padding(top=5.dp), color = FastCyan, fontSize = 10.sp, textAlign = TextAlign.Center) }
        Button(enabled = !busy && !qwen, onClick = { scope.launch { busy = true; status = ""; error = ""; try { LlmModelInstaller(llm).download { d,t -> done=d; total=t }; qwen=true; status="Qwen downloaded and verified" } catch(e: Exception) { error=e.message ?: "Qwen download failed" } finally { busy=false } } }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = FastPurple)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(7.dp)); Text(if(qwen) "Qwen Installed" else "Download Qwen") }
        Spacer(Modifier.height(8.dp)); OutlinedButton(enabled = !busy && (!vadReady || !sttReady), onClick = { scope.launch { busy=true; status=""; error=""; try { if(!vadReady) { voiceInstaller.downloadVad { d,t -> done=d; total=t }; vadReady=true }; if(!sttReady) { voiceInstaller.downloadHindiStt { d,t -> done=d; total=t }; sttReady=true }; status="Voice models downloaded and verified" } catch(e:Exception) { error=e.message ?: "Voice model download failed" } finally { busy=false } } }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.RecordVoiceOver, null); Spacer(Modifier.width(7.dp)); Text(if(vadReady && sttReady) "Voice Models Installed" else "Download VAD + Hindi STT") }
        if(status.isNotBlank()) Text(status, Modifier.padding(top=9.dp), color=FastCyan, fontSize=10.sp); if(error.isNotBlank()) Text(error, Modifier.padding(top=7.dp), color=Color(0xFFFF7187), fontSize=10.sp)
        Spacer(Modifier.height(14.dp)); Text("Model storage", color=Color.White, fontSize=14.sp, fontWeight=FontWeight.Bold); Text(llm.modelDirectory().absolutePath, color=FastMuted, fontSize=9.sp, modifier=Modifier.padding(top=4.dp)); Text("Models remain available across normal APK updates. Android does not require broad 'all files' access for this app-private model folder.", color=FastMuted, fontSize=9.sp, modifier=Modifier.padding(top=8.dp))
    }
}

@Composable private fun FastModelCard(title:String, sub:String, ready:Boolean, accent:Color) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), FastPanel, border=BorderStroke(1.dp, accent.copy(.3f))) { Row(Modifier.padding(13.dp), verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Memory,null,tint=accent,modifier=Modifier.size(25.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(title,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(sub,color=FastMuted,fontSize=9.sp)};Text(if(ready)"READY" else "MISSING",color=if(ready)FastCyan else FastMuted,fontSize=8.sp,fontWeight=FontWeight.Bold)} } }

@Composable private fun FastSettings(onBack:()->Unit,onChat:()->Unit,onModels:()->Unit) { Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(18.dp)){ Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Color.White)};Text("Settings",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)}; FastSetting("Chat","Fast offline Qwen chat",Icons.Default.ChatBubble,onChat);FastSetting("AI Models","Qwen + VAD + Hindi STT",Icons.Default.Memory,onModels);FastSetting("Voice","Microphone → VAD → STT → Qwen",Icons.Default.Mic,null);FastSetting("Privacy","Local inference and local model storage",Icons.Default.Lock,null) } }
@Composable private fun FastSetting(title:String,sub:String,icon:androidx.compose.ui.graphics.vector.ImageVector,click:(()->Unit)?){Surface(Modifier.fillMaxWidth().padding(vertical=4.dp).then(if(click!=null)Modifier.clickable(onClick=click)else Modifier),RoundedCornerShape(16.dp),FastPanel,border=BorderStroke(1.dp,Color.White.copy(.05f))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=FastCyan,modifier=Modifier.size(25.dp));Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(title,color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold);Text(sub,color=FastMuted,fontSize=10.sp)};if(click!=null)Icon(Icons.Default.ChevronRight,null,tint=FastMuted)}}}
