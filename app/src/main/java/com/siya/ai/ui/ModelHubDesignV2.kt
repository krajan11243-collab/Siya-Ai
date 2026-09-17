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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.stt.SttModelSource
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.launch

private val V2Bg = Color(0xFF010713)
private val V2Panel = Color(0xFF061426)
private val V2Purple = Color(0xFF8B35FF)
private val V2Blue = Color(0xFF087BFF)
private val V2Cyan = Color(0xFF18D9FF)
private val V2Green = Color(0xFF16F0B1)
private val V2Text = Color(0xFFF6F8FF)
private val V2Muted = Color(0xFFA9B8D4)

@Composable
fun PixelModelHub(
    onBack: () -> Unit,
    onSpeech: () -> Unit,
    onImport: () -> Unit,
    onAllModels: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "hub-pulse")
    val glow by pulse.animateFloat(0.72f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(V2Bg, Color(0xFF020B1B), V2Bg))).verticalScroll(rememberScrollState()).padding(horizontal = 27.dp, vertical = 9.dp)
    ) {
        PixelHeader("Models", "Download, Select & Use On-Device Models", onBack, V2Purple)
        Spacer(Modifier.height(15.dp))
        PixelActionCard("AI All Model Download Select", "Download all required models for complete AI voice assistant experience", V2Purple, Icons.Default.Memory, glow, onAllModels)
        Spacer(Modifier.height(22.dp))
        PixelActionCard("Download Speech Models", "Download VAD, STT and all related models (One Click)", V2Cyan, Icons.Default.GraphicEq, glow, onSpeech)
        Spacer(Modifier.height(22.dp))
        PixelActionCard("Import GGUF", "Select and import your own model file", V2Purple, Icons.Default.FolderOpen, glow, onImport)
        Spacer(Modifier.height(21.dp))
        Surface(Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(17.dp), clip = true), RoundedCornerShape(17.dp), Color.Transparent, BorderStroke(1.dp, V2Blue.copy(.75f))) {
            Row(Modifier.background(Brush.linearGradient(listOf(V2Blue.copy(.09f), V2Panel.copy(.88f)))).padding(15.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = V2Blue, modifier = Modifier.size(31.dp).alpha(glow))
                Spacer(Modifier.width(11.dp))
                Text("Model downloads run in a foreground service and continue while Siya Ai is closed. Progress stays in the notification.", color = V2Text.copy(.88f), fontSize = 11.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun PixelHeader(title: String, subtitle: String, onBack: () -> Unit, accent: Color) {
    Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(50.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(34.dp)) }
        Column(Modifier.weight(1f).padding(start = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI", color = V2Cyan, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(7.dp))
                Text(title, color = Color.White, fontSize = 35.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(subtitle, color = V2Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(Modifier.size(48.dp).shadow(12.dp, RoundedCornerShape(16.dp), clip = true), RoundedCornerShape(16.dp), Color.Transparent, BorderStroke(1.dp, accent.copy(.78f))) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.15f), Color.Transparent))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Smartphone, null, tint = Color(0xFFFF4FE7), modifier = Modifier.size(27.dp))
                Box(Modifier.size(8.dp).align(Alignment.TopEnd).offset((-7).dp, 7.dp).background(V2Green, CircleShape))
            }
        }
    }
}

@Composable
private fun PixelActionCard(title: String, subtitle: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, glow: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Surface(Modifier.fillMaxWidth().height(169.dp).shadow(22.dp, shape, clip = true).clickable(onClick = onClick), shape, Color.Transparent, BorderStroke(1.8.dp, accent.copy(alpha = .98f))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(.28f), Color(0xFF07132A).copy(.96f), accent.copy(.08f))))) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.17f), Color.Transparent))))
            Box(Modifier.fillMaxSize().graphicsLayer { translationX = 90f * glow }.background(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(.05f), Color.Transparent))).alpha(.8f))
            Row(Modifier.fillMaxSize().padding(horizontal = 23.dp, vertical = 19.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(91.dp).shadow(16.dp, CircleShape, clip = true), CircleShape, Color.Transparent, BorderStroke(1.8.dp, accent.copy(.9f))) {
                    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.22f), Color.Transparent))), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(52.dp).alpha(glow)) }
                }
                Spacer(Modifier.width(25.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, color = Color.White.copy(.82f), fontSize = 12.sp, lineHeight = 18.sp, maxLines = 2)
                }
                Surface(Modifier.size(58.dp).shadow(12.dp, CircleShape, clip = true), CircleShape, accent.copy(.09f), BorderStroke(1.5.dp, accent.copy(.9f))) { Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.padding(11.dp).alpha(glow)) }
            }
        }
    }
}

@Composable
fun PixelSpeechModelsScreen(onBack: () -> Unit, vadStore: VadModelStore, sttStore: SttModelStore, installer: VoiceModelInstaller) {
    val scope = rememberCoroutineScope()
    var vadInstalled by remember { mutableStateOf(vadStore.isInstalled()) }
    var sttInstalled by remember { mutableStateOf(sttStore.isInstalled()) }
    var active by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableLongStateOf(0L) }
    var total by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val pulse = rememberInfiniteTransition(label = "speech-pulse")
    val glow by pulse.animateFloat(.65f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "speech-glow")

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(V2Bg, Color(0xFF020A18), V2Bg))).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 7.dp)) {
        PixelHeader("Speech Models", "VAD • STT • Tokens • Offline voice pipeline", onBack, V2Cyan)
        Spacer(Modifier.height(12.dp))
        SpeechPipeline(glow)
        Spacer(Modifier.height(13.dp))
        SpeechAsset("Silero VAD", "VAD", "Detects speech start/stop before STT runs.", V2Cyan, vadInstalled, active == "VAD", done, total) {
            scope.launch { active = "VAD"; message = null; error = null; try { installer.downloadVad { d,t -> done=d; total=t }; vadInstalled=true; message="Silero VAD is installed and ready." } catch(e:Exception){ error=e.message ?: "VAD download failed" } finally { active=null } }
        }
        Spacer(Modifier.height(12.dp))
        SpeechAsset("Hindi STT", "STT", "The Hindi speech-to-text model wired into Siya's offline voice path.", V2Blue, sttInstalled, active == "STT", done, total) {
            scope.launch { active = "STT"; message = null; error = null; try { installer.downloadHindiStt { d,t -> done=d; total=t }; sttInstalled=true; message="Hindi STT model and tokenizer are installed." } catch(e:Exception){ error=e.message ?: "STT download failed" } finally { active=null } }
        }
        Spacer(Modifier.height(12.dp))
        SpeechDetailsCard()
        Spacer(Modifier.height(12.dp))
        message?.let { Text(it, color = V2Green, fontSize = 10.sp) }
        error?.let { Text(it, color = Color(0xFFFF5D78), fontSize = 10.sp) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SpeechPipeline(glow: Float) {
    Surface(Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(21.dp), clip=true), RoundedCornerShape(21.dp), Color.Transparent, BorderStroke(1.4.dp, V2Cyan.copy(.7f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(V2Cyan.copy(.13f), V2Panel.copy(.96f), V2Purple.copy(.07f)))).padding(15.dp)) {
            Text("ACTIVE OFFLINE SPEECH PIPELINE", color=V2Cyan, fontSize=9.sp, fontWeight=FontWeight.ExtraBold, letterSpacing=1.4.sp)
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                listOf("MIC" to V2Cyan, "VAD" to V2Cyan, "STT" to V2Blue, "LLM" to V2Purple).forEachIndexed { i, pair ->
                    if (i > 0) Text("→", color=V2Muted, fontSize=17.sp)
                    Surface(Modifier.size(51.dp), RoundedCornerShape(14.dp), pair.second.copy(.08f), BorderStroke(1.dp, pair.second.copy(.65f))) { Box(contentAlignment=Alignment.Center){ Text(pair.first, color=pair.second, fontSize=8.sp, fontWeight=FontWeight.ExtraBold, modifier=Modifier.alpha(glow)) } }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text("Only real assets used by the current Siya offline speech implementation are shown below.", color=V2Muted, fontSize=9.sp, lineHeight=14.sp)
        }
    }
}

@Composable
private fun SpeechAsset(title:String, tag:String, subtitle:String, accent:Color, installed:Boolean, downloading:Boolean, done:Long, total:Long, onDownload:()->Unit) {
    Surface(Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(21.dp), clip=true), RoundedCornerShape(21.dp), Color.Transparent, BorderStroke(1.4.dp, accent.copy(.82f))) {
        Column(Modifier.background(Brush.linearGradient(listOf(accent.copy(.15f), V2Panel.copy(.97f), accent.copy(.04f)))).padding(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(Modifier.size(61.dp), RoundedCornerShape(16.dp), accent.copy(.07f), BorderStroke(1.2.dp, accent.copy(.72f))) { Icon(if(tag=="STT") Icons.Default.GraphicEq else Icons.Default.Speed, null, tint=accent, modifier=Modifier.padding(14.dp)) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) { Row(verticalAlignment=Alignment.CenterVertically){ Text(title,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.ExtraBold); Spacer(Modifier.width(7.dp)); Surface(RoundedCornerShape(7.dp),Color.Transparent,BorderStroke(1.dp,accent.copy(.75f))){Text(tag,color=accent,fontSize=8.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=7.dp,vertical=2.dp))} }; Text(subtitle,color=Color.White.copy(.73f),fontSize=10.sp,lineHeight=14.sp,modifier=Modifier.padding(top=4.dp)) }
                if(installed) Icon(Icons.Default.CheckCircle,null,tint=V2Green,modifier=Modifier.size(25.dp))
            }
            Spacer(Modifier.height(12.dp))
            if(tag=="VAD") {
                DetailLine("Engine","Silero Voice Activity Detection"); DetailLine("Input","16 kHz audio"); DetailLine("Runtime","On-device / offline"); DetailLine("File","onnx/model.onnx")
            } else {
                DetailLine("Architecture",SttModelSource.MODEL_ARCHITECTURE); DetailLine("Quantization",SttModelSource.QUANTIZATION); DetailLine("Language","Hindi (hi)"); DetailLine("Sample rate","16 kHz"); DetailLine("Model","model.int8.onnx"); DetailLine("Tokenizer","tokens.txt")
            }
            if(downloading && total>0L){ val p=(done.toFloat()/total.toFloat()).coerceIn(0f,1f); Spacer(Modifier.height(8.dp)); Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Downloading…",color=V2Muted,fontSize=9.sp);Text("${(p*100).toInt()}%",color=V2Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold)}; LinearProgressIndicator(progress={p},Modifier.fillMaxWidth().padding(top=5.dp),color=accent) }
            Spacer(Modifier.height(11.dp))
            Button(onClick=onDownload,enabled=!installed&&!downloading,modifier=Modifier.fillMaxWidth().height(49.dp).shadow(10.dp,RoundedCornerShape(14.dp),clip=true),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color.Black,disabledContainerColor=Color(0xFF12302D),disabledContentColor=V2Green)){Icon(if(installed)Icons.Default.CheckCircle else Icons.Default.Download,null);Spacer(Modifier.width(7.dp));Text(if(installed)"Downloaded & Ready" else if(downloading)"Downloading…" else "Download This Model",fontWeight=FontWeight.Bold)}
        }
    }
}

@Composable private fun DetailLine(key:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.Top){Text(key,color=V2Muted,fontSize=9.sp,modifier=Modifier.width(91.dp));Text(value,color=Color.White.copy(.86f),fontSize=9.sp,modifier=Modifier.weight(1f))}}

@Composable private fun SpeechDetailsCard(){Surface(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),Color.Transparent,BorderStroke(1.dp,V2Purple.copy(.58f))){Column(Modifier.background(Brush.linearGradient(listOf(V2Purple.copy(.09f),V2Panel.copy(.93f)))).padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Info,null,tint=V2Purple,modifier=Modifier.size(25.dp));Spacer(Modifier.width(9.dp));Text("What Siya actually uses",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(7.dp));Text("Microphone → Silero VAD → Hindi IndicConformer STT → Qwen Local LLM → Siya response. VAD is the speech gate; STT converts Hindi speech into text. The assets remain on-device after installation.",color=V2Muted,fontSize=9.sp,lineHeight=14.sp)}}}
