package com.siya.ai.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
                "chat" -> PixelChat(onBack = { page = "home" }, onModels = { page = "models" })
                "models" -> ModelHome(onBack = { page = "settings" }, onAllModels = { page = "allModels" }, onSpeech = { page = "speech" }, onImport = { page = "import" })
                "settings" -> ResponsiveSettings(onBack = { page = "home" }, onChat = { page = "chat" }, onModels = { page = "models" })
                else -> PixelHome(
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
private fun PixelHome(
    microphoneGranted: Boolean,
    voice: VoiceSessionState.State,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onVoice: () -> Unit
) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
            HomeHeaderButton(Icons.Default.ChatBubble, "CHAT", Cyan, onChat)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Siya", color = White, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" Ai", color = Cyan, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Y O U R   A I   C O M P A N I O N", color = Muted, fontSize = 8.sp, letterSpacing = 2.7.sp)
            }
            Spacer(Modifier.weight(1f))
            HomeHeaderButton(Icons.Default.Settings, "SETTINGS", Purple, onSettings)
        }
        Text("L I S T E N S     •     U N D E R S T A N D S     •     C O N T R O L S",
            Modifier.fillMaxWidth().padding(top = 7.dp), color = Cyan, fontSize = 9.sp, letterSpacing = 1.6.sp, textAlign = TextAlign.Center)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            ProHologram(active = voice.active, modifier = Modifier.fillMaxSize())
        }
        val status = when (voice.phase) {
            VoiceSessionState.Phase.LISTENING -> "Listening…"
            VoiceSessionState.Phase.TRANSCRIBING -> "Understanding…"
            VoiceSessionState.Phase.THINKING -> "Siya is thinking…"
            VoiceSessionState.Phase.SPEAKING -> "Siya is speaking…"
            VoiceSessionState.Phase.INTERRUPTING -> "Listening…"
            VoiceSessionState.Phase.READY -> "Ready"
            VoiceSessionState.Phase.ERROR -> "Something needs attention"
            VoiceSessionState.Phase.IDLE -> "Tap to Speak"
        }
        Text(status, Modifier.fillMaxWidth(), White, 21.sp, FontWeight.Bold, textAlign = TextAlign.Center)
        if (voice.response.isNotBlank() && voice.phase == VoiceSessionState.Phase.READY) {
            NeonSurface(Modifier.fillMaxWidth().padding(top = 8.dp), Cyan, 15.dp) {
                Text(voice.response, Modifier.padding(12.dp), White, 12.sp, maxLines = 2)
            }
        }
        Text("Hindi  •  Hinglish  •  English", Modifier.fillMaxWidth().padding(top = 7.dp), Muted, 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        NeonMicButton(active = voice.active, onClick = onVoice)
        Text(if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak",
            Modifier.fillMaxWidth().padding(top = 5.dp), Muted, 10.sp, textAlign = TextAlign.Center)
        Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth().padding(top = 8.dp), Blue, 8.sp, letterSpacing = 2.3.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HomeHeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NeonSurface(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick), accent, 18.dp) {
            Icon(icon, label, tint = accent, modifier = Modifier.padding(14.dp))
        }
        Text(label, Modifier.padding(top = 4.dp), Muted, 7.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun NeonMicButton(active: Boolean, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "mic")
    val pulse by t.animateFloat(1f, 1.10f, infiniteRepeatable(tween(if (active) 650 else 1300), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.size(116.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * .39f * pulse
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(.22f), Purple.copy(.10f), Color.Transparent), c, r * 2.2f), r * 2.2f, c)
            drawCircle(Panel2, r, c)
            drawCircle(Cyan.copy(.85f), r, c, style = Stroke(2.8f))
            drawCircle(Purple.copy(.9f), r * .86f, c, style = Stroke(1.6f))
            drawCircle(Blue.copy(.7f), r * .72f, c, style = Stroke(1f))
        }
        Icon(if (active) Icons.Default.Stop else Icons.Default.Mic, "Microphone", tint = White, modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun ModelHome(onBack: () -> Unit, onAllModels: () -> Unit, onSpeech: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp, vertical = 10.dp)) {
        NeonHeader("AI Models", "Download, Select & Use On-Device Models", onBack, Icons.Default.PhoneAndroid, Green)
        Spacer(Modifier.height(16.dp))
        ModelHomeCard("AI All Model Download Select", "Download all required models for complete AI", "Choose and download AI models for offline use", Purple, Icons.Default.Memory, onAllModels)
        Spacer(Modifier.height(14.dp))
        ModelHomeCard("Download Speech Models", "Download VAD, STT and all related models", "(One Click)", Cyan, Icons.Default.GraphicEq, onSpeech)
        Spacer(Modifier.height(14.dp))
        ModelHomeCard("Import GGUF", "Select and import your own model file", "", Purple, Icons.Default.FolderOpen, onImport)
        Spacer(Modifier.height(20.dp))
        NeonSurface(Modifier.fillMaxWidth(), Blue, 16.dp) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Cyan, Modifier.size(31.dp))
                Spacer(Modifier.width(12.dp))
                Text("Model downloads run in a foreground service and continue while Siya Ai is closed. Progress stays in the notification.", Muted, 12.sp)
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ModelHomeCard(title: String, subtitle: String, detail: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    NeonSurface(Modifier.fillMaxWidth().height(151.dp).clickable(onClick = onClick), accent, 24.dp) {
        Row(Modifier.fillMaxSize().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonIconBox(icon, accent, 72.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, White, 20.sp, FontWeight.Bold)
                Text(subtitle, Muted, 13.sp, Modifier.padding(top = 5.dp))
                if (detail.isNotBlank()) Text(detail, Muted, 12.sp, Modifier.padding(top = 2.dp))
            }
            NeonArrow(accent)
        }
    }
}

@Composable
private fun AllModels(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    val rows = listOf(
        Triple("Qwen 2.5 1.5B", "Balanced local assistant model", "~1.12 GB"),
        Triple("Llama 3.2 1B", "Lightweight mobile model", "Not configured"),
        Triple("DeepSeek Coder 1.3B", "Specialized coding model", "Not configured"),
        Triple("Kokoro TTS", "Local speech synthesis backend", "Import package")
    )
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 15.dp, vertical = 8.dp)) {
        NeonHeader("AI All Model Download Select", "Choose and download AI models for offline use", onBack, Icons.Default.PhoneAndroid, Green)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(rows) { row ->
                val isQwen = row.first.startsWith("Qwen")
                ModelRow(row.first, row.second, row.third, if (isQwen) Purple else Cyan, isQwen && installed, isQwen && downloading, if (isQwen) progress else 0f, isQwen,
                    {
                        scope.launch {
                            downloading = true; error = null; progress = 0f
                            try {
                                LlmModelInstaller(store).download { d, t -> progress = if (t > 0) (d.toFloat() / t).coerceIn(0f, 1f) else 0f }
                                installed = true
                            } catch (e: Exception) { error = e.message ?: "Download failed" }
                            finally { downloading = false }
                        }
                    },
                    if (isQwen) { { installed = false; store.delete() } } else null)
            }
        }
        if (error != null) Text(error!!, Red, 11.sp, Modifier.padding(4.dp))
        NeonSurface(Modifier.fillMaxWidth(), Blue, 14.dp) {
            Text("Only real local runtime/download paths are actionable. Cloud-only models are not presented as fake offline downloads.", Modifier.padding(11.dp), Muted, 10.sp)
        }
    }
}

@Composable
private fun SpeechModels(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vad = remember { VadModelStore(context) }
    val stt = remember { SttModelStore(context) }
    val tts = remember { TtsModelStore(context) }
    var vadInstalled by remember { mutableStateOf(vad.isInstalled()) }
    var sttInstalled by remember { mutableStateOf(stt.isInstalled()) }
    var vadBusy by remember { mutableStateOf(false) }
    var sttBusy by remember { mutableStateOf(false) }
    var vadProgress by remember { mutableFloatStateOf(0f) }
    var sttProgress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 15.dp, vertical = 8.dp)) {
        NeonHeader("Download Speech Models", "VAD, STT and local voice components", onBack, Icons.Default.GraphicEq, Cyan)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item {
                SpeechRow("Silero VAD", "Voice activity detection • 16 kHz", if (vadInstalled) "READY" else "~1–3 MB", Cyan, vadInstalled, vadBusy, vadProgress) {
                    scope.launch {
                        vadBusy = true; error = null
                        try { VoiceModelInstaller(vad, stt).downloadVad { d,t -> vadProgress = if (t > 0) (d.toFloat()/t).coerceIn(0f,1f) else 0f }; vadInstalled = true }
                        catch (e: Exception) { error = e.message ?: "VAD download failed" }
                        finally { vadBusy = false }
                    }
                }
            }
            item {
                SpeechRow("AI4Bharat IndicConformer Hindi STT", "Hindi / Hinglish speech recognition", if (sttInstalled) "READY" else "~100+ MB", Blue, sttInstalled, sttBusy, sttProgress) {
                    scope.launch {
                        sttBusy = true; error = null
                        try { VoiceModelInstaller(vad, stt).downloadHindiStt { d,t -> sttProgress = if (t > 0) (d.toFloat()/t).coerceIn(0f,1f) else 0f }; sttInstalled = true }
                        catch (e: Exception) { error = e.message ?: "STT download failed" }
                        finally { sttBusy = false }
                    }
                }
            }
            item { SpeechRow("Kokoro TTS", "Sherpa-ONNX local neural voice adapter", if (tts.isInstalled()) "READY" else "IMPORT", Purple, tts.isInstalled(), false, 0f) {} }
        }
        error?.let { Text(it, Red, 11.sp, Modifier.padding(bottom = 7.dp)) }
        NeonSurface(Modifier.fillMaxWidth(), Cyan, 14.dp) { Text("Pipeline: MIC  →  VAD  →  Hindi STT  →  Local LLM  →  TTS", Modifier.padding(12.dp), Muted, 11.sp) }
    }
}

@Composable
private fun SpeechRow(title:String, subtitle:String, size:String, accent:Color, installed:Boolean, busy:Boolean, progress:Float, onClick:()->Unit) {
    NeonSurface(Modifier.fillMaxWidth(), accent, 18.dp) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                NeonIconBox(Icons.Default.GraphicEq, accent, 47.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) { Text(title, White, 14.sp, FontWeight.Bold); Text(subtitle, Muted, 10.sp) }
                Text(size, Muted, 10.sp, textAlign=TextAlign.End)
            }
            Spacer(Modifier.height(9.dp))
            if (busy) {
                LinearProgressIndicator(progress={progress}, modifier=Modifier.fillMaxWidth().height(5.dp), color=accent, trackColor=Panel2)
                Text((progress*100).toInt().toString()+"%  Downloading…", accent, 9.sp, Modifier.padding(top=4.dp))
            } else {
                OutlinedButton(onClick=onClick, enabled=!installed, modifier=Modifier.fillMaxWidth().height(40.dp), shape=RoundedCornerShape(12.dp), border=BorderStroke(1.dp,accent.copy(.7f)), colors=ButtonDefaults.outlinedButtonColors(contentColor=accent)) {
                    Icon(if(installed) Icons.Default.Check else Icons.Default.Download, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if(installed) "Downloaded • Ready" else "Download", fontSize=11.sp, fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModelRow(title:String, subtitle:String, size:String, accent:Color, installed:Boolean, downloading:Boolean, progress:Float, enabled:Boolean, onDownload:()->Unit, onDelete:(()->Unit)?) {
    NeonSurface(Modifier.fillMaxWidth(), accent, 17.dp) {
        Row(Modifier.padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
            NeonIconBox(Icons.Default.Memory, accent, 48.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(title,White,13.sp,FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Surface(shape=RoundedCornerShape(10.dp),color=accent.copy(.10f),border=BorderStroke(1.dp,accent.copy(.65f))) { Text("LOCAL",Modifier.padding(horizontal=6.dp,vertical=2.dp),accent,7.sp,FontWeight.Bold) }
                }
                Text(subtitle,Muted,9.sp,Modifier.padding(top=2.dp))
                if(downloading){
                    LinearProgressIndicator(progress={progress},Modifier.fillMaxWidth().padding(top=6.dp).height(4.dp),color=accent,trackColor=Panel2)
                    Text((progress*100).toInt().toString()+"%  Downloading…",accent,8.sp,Modifier.padding(top=2.dp))
                }
            }
            Spacer(Modifier.width(7.dp))
            Column(horizontalAlignment=Alignment.End) {
                Text(size,Muted,9.sp)
                Spacer(Modifier.height(5.dp))
                if(installed) Surface(shape=RoundedCornerShape(10.dp),color=Green.copy(.12f),border=BorderStroke(1.dp,Green.copy(.7f))) { Text("✓ READY",Modifier.padding(horizontal=8.dp,vertical=7.dp),Green,8.sp,FontWeight.Bold) }
                else if(enabled) Button(onClick=onDownload,enabled=!downloading,modifier=Modifier.height(37.dp),contentPadding=PaddingValues(horizontal=10.dp),shape=RoundedCornerShape(11.dp),colors=ButtonDefaults.buttonColors(containerColor=accent.copy(.18f),contentColor=White)) { Icon(Icons.Default.Download,null,Modifier.size(15.dp));Spacer(Modifier.width(4.dp));Text(if(downloading)"STOP" else "Download",9.sp,fontWeight=FontWeight.Bold) }
                else Surface(shape=RoundedCornerShape(10.dp),color=Panel2,border=BorderStroke(1.dp,Color.White.copy(.08f))) { Text("NOT CONFIGURED",Modifier.padding(horizontal=7.dp,vertical=7.dp),Muted,7.sp,FontWeight.Bold) }
                onDelete?.let { IconButton(onClick=it,modifier=Modifier.size(29.dp)){Icon(Icons.Default.Delete,"Delete",Red,Modifier.size(15.dp))} }
            }
        }
    }
}

@Composable
private fun ImportModelPage(onBack:()->Unit) {
    val context=LocalContext.current
    val store=remember{LlmModelStore(context)}
    val scope=rememberCoroutineScope()
    var status by remember{mutableStateOf(if(store.isInstalled())"Qwen model is installed." else "Select a verified Qwen GGUF file.")}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->
        if(uri==null)return@rememberLauncherForActivityResult
        status="Importing…"
        scope.launch { try { importModel(context,uri,store); status="Import complete • Qwen model is READY." } catch(e:Exception) { status="Import failed: "+(e.message ?: "invalid GGUF") } }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(18.dp)) {
        NeonHeader("Import GGUF","Select and import your own model file",onBack,Icons.Default.FolderOpen,Purple)
        Spacer(Modifier.height(20.dp))
        NeonSurface(Modifier.fillMaxWidth(),Purple,22.dp){
            Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
                NeonIconBox(Icons.Default.UploadFile,Purple,76.dp)
                Spacer(Modifier.height(13.dp))
                Text("Qwen 2.5 1.5B Instruct Q4_K_M",White,17.sp,FontWeight.Bold,textAlign=TextAlign.Center)
                Text("SHA-256 verified import • local storage",Muted,11.sp,Modifier.padding(top=5.dp),textAlign=TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick={launcher.launch(arrayOf("application/octet-stream","application/*"))},modifier=Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=Purple),shape=RoundedCornerShape(14.dp)){
                    Icon(Icons.Default.FolderOpen,null);Spacer(Modifier.width(7.dp));Text("Select GGUF")
                }
                Text(status,Muted,10.sp,Modifier.padding(top=10.dp),textAlign=TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PixelChat(onBack:()->Unit,onModels:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val store=remember{LlmModelStore(context)}
    val engine=remember{LocalLlmEngine(store)}
    val voice by VoiceSessionState.state.collectAsState()
    val messages=remember{mutableStateListOf<Pair<Boolean,String>>() }
    var input by rememberSaveable{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var attached by rememberSaveable{mutableStateOf<String?>(null)}
    var menu by remember{mutableStateOf(false)}
    var installed by remember{mutableStateOf(store.isInstalled())}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->attached=uri?.lastPathSegment?.substringAfterLast('/') ?: uri?.toString()}
    DisposableEffect(Unit){onDispose{engine.close()}}
    LaunchedEffect(voice.phase,voice.transcript,voice.response){
        if(voice.phase==VoiceSessionState.Phase.THINKING && voice.transcript.isNotBlank() && messages.none{it.first&&it.second==voice.transcript})messages+=true to voice.transcript
        if(voice.phase==VoiceSessionState.Phase.READY && voice.response.isNotBlank() && messages.none{!it.first&&it.second==voice.response})messages+=false to voice.response
    }
    Box(Modifier.fillMaxSize()){
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=12.dp,vertical=7.dp)){
            Row(Modifier.fillMaxWidth().height(68.dp),verticalAlignment=Alignment.CenterVertically){
                NeonSurface(Modifier.size(48.dp).clickable(onClick=onBack),Cyan,15.dp){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",White,Modifier.padding(11.dp))}
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                    Row(verticalAlignment=Alignment.CenterVertically){Text("Siya",White,28.sp,FontWeight.Bold);Text(" Ai",Cyan,28.sp,FontWeight.Bold)}
                    Text("Y O U R   A I   C O M P A N I O N",Muted,7.sp,letterSpacing=2.2.sp)
                }
                NeonSurface(Modifier.size(48.dp).clickable{menu=true},Purple,15.dp){Icon(Icons.Default.Menu,"Menu",Purple,Modifier.padding(11.dp))}
            }
            NeonDivider()
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(top=13.dp,bottom=13.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                if(messages.isEmpty())item{AssistantBubble("Hello! 👋\nमैं Siya Ai हूँ\nमैं आपकी कैसे मदद कर सकती हूँ?")}
                items(messages){m->if(m.first)UserBubble(m.second)else AssistantBubble(m.second)}
            }
            if(attached!=null){Surface(shape=RoundedCornerShape(10.dp),color=Panel2,border=BorderStroke(1.dp,Cyan.copy(.45f))){Text("📎 "+attached!!,Modifier.padding(7.dp),Cyan,9.sp,maxLines=1)};Spacer(Modifier.height(5.dp))}
            Row(Modifier.fillMaxWidth().padding(bottom=5.dp),verticalAlignment=Alignment.Bottom){
                NeonSurface(Modifier.size(48.dp).clickable{picker.launch(arrayOf("*/*"))},Purple,15.dp){Icon(Icons.Default.AttachFile,"Attach",White,Modifier.padding(12.dp))}
                Spacer(Modifier.width(7.dp))
                OutlinedTextField(value=input,onValueChange={input=it},modifier=Modifier.weight(1f),placeholder={Text("Type your message…",color=Muted)},shape=RoundedCornerShape(18.dp),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Text),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Purple,unfocusedBorderColor=Panel2,focusedTextColor=White,unfocusedTextColor=White,cursorColor=Cyan),maxLines=4)
                Spacer(Modifier.width(5.dp))
                NeonSurface(Modifier.size(48.dp),Blue,15.dp){Icon(Icons.Default.Mic,"Voice",White,Modifier.padding(12.dp))}
                Spacer(Modifier.width(5.dp))
                NeonSurface(Modifier.size(53.dp).clickable(enabled=input.isNotBlank()&&!busy){
                    val prompt=input.trim();input="";messages+=true to prompt;busy=true
                    scope.launch{
                        try{check(store.isInstalled()){"Qwen model is not installed"};val result=engine.complete(prompt);messages+=false to result.text;installed=true}
                        catch(e:Exception){messages+=false to "Local AI error: "+(e.message ?: "model unavailable")}
                        finally{busy=false}
                    }
                },Purple,17.dp){Icon(Icons.Default.Send,"Send",White,Modifier.padding(14.dp))}
            }
        }
        if(menu)Box(Modifier.fillMaxSize().background(Color.Black.copy(.42f)).clickable{menu=false}){
            NeonSurface(Modifier.align(Alignment.TopEnd).padding(top=72.dp,end=13.dp).width(190.dp),Purple,18.dp){
                Column(Modifier.padding(8.dp)){
                    MenuItem("AI Models",Icons.Default.Memory){menu=false;onModels()}
                    MenuItem("Offline: "+if(installed)"READY" else "MODEL MISSING",Icons.Default.CloudOff){menu=false}
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(11.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(icon,null,Cyan,Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text(text,White,11.sp)
    }
}

@Composable
private fun AssistantBubble(text:String){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
        NeonAvatar();Spacer(Modifier.width(8.dp))
        NeonSurface(Modifier.widthIn(max=310.dp),Cyan,19.dp){
            Column(Modifier.padding(13.dp)){Text("Siya Ai",Cyan,12.sp,FontWeight.Bold);Text(text,White,15.sp,Modifier.padding(top=6.dp))}
        }
    }
}

@Composable
private fun UserBubble(text:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
        NeonSurface(Modifier.widthIn(max=330.dp),Purple,19.dp){Text(text,White,15.sp,Modifier.padding(14.dp))}
    }
}

@Composable
private fun NeonAvatar(){
    Canvas(Modifier.size(57.dp)){
        val c=Offset(size.width/2,size.height/2)
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(.4f),Purple.copy(.18f),Color.Transparent),c,size.minDimension*.7f),size.minDimension*.7f,c)
        drawCircle(Panel2,size.minDimension*.40f,c)
        drawCircle(Cyan.copy(.85f),size.minDimension*.40f,c,style=Stroke(1.8f))
        drawCircle(Purple.copy(.75f),size.minDimension*.34f,c,style=Stroke(1.2f))
        drawCircle(Cyan.copy(.75f),size.minDimension*.10f,Offset(c.x,c.y-size.minDimension*.06f))
        drawArc(Cyan.copy(.65f),200f,140f,false,Offset(c.x-size.minDimension*.18f,c.y-size.minDimension*.03f),androidx.compose.ui.geometry.Size(size.minDimension*.36f,size.minDimension*.30f),style=Stroke(1.7f))
        drawLine(Cyan.copy(.45f),Offset(c.x-size.minDimension*.20f,c.y+size.minDimension*.20f),Offset(c.x+size.minDimension*.20f,c.y+size.minDimension*.20f),1.4f)
    }
}

@Composable
private fun NeonHeader(title:String,subtitle:String,onBack:()->Unit,icon:androidx.compose.ui.graphics.vector.ImageVector,accent:Color){
    Row(Modifier.fillMaxWidth().height(66.dp),verticalAlignment=Alignment.CenterVertically){
        NeonSurface(Modifier.size(48.dp).clickable(onClick=onBack),Cyan,15.dp){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",White,Modifier.padding(10.dp))}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){Text(title,White,25.sp,FontWeight.ExtraBold);Text(subtitle,Muted,11.sp)}
        NeonSurface(Modifier.size(48.dp),accent,15.dp){Icon(imageVector=icon,contentDescription=null,tint=accent,modifier=Modifier.padding(11.dp))}
    }
}

@Composable
private fun NeonIconBox(icon:androidx.compose.ui.graphics.vector.ImageVector,accent:Color,size:androidx.compose.ui.unit.Dp){
    NeonSurface(Modifier.size(size),accent,16.dp){Icon(imageVector=icon,contentDescription=null,tint=accent,modifier=Modifier.padding(size/4))}
}

@Composable
private fun NeonArrow(accent:Color){
    NeonSurface(Modifier.size(49.dp),accent,50.dp){Icon(imageVector=Icons.Default.ChevronRight,contentDescription=null,tint=White,modifier=Modifier.padding(9.dp))}
}

@Composable
private fun NeonSurface(modifier:Modifier,accent:Color,shapeDp:androidx.compose.ui.unit.Dp,content:@Composable()->Unit){
    Surface(modifier=modifier,shape=RoundedCornerShape(shapeDp),color=Panel.copy(.92f),border=BorderStroke(1.dp,accent.copy(.60f)),shadowElevation=0.dp,content=content)
}

@Composable
private fun NeonDivider(){
    Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,Cyan.copy(.7f),Purple.copy(.7f),Color.Transparent))))
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
            VoiceSessionState.Phase.SPEAKING -> "Siya is speaking…"
            VoiceSessionState.Phase.INTERRUPTING -> "Listening…"
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
                    Surface(shape = RoundedCornerShape(18.dp), color = if (message.first) Purple.copy(.22f) else Panel, border = BorderStroke(1.dp, if (message.first) Purple.copy(.4f) else Color.White.copy(.04f))) {
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
