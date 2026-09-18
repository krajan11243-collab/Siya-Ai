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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.siya.ai.llm.LlmModelInstaller
import com.siya.ai.llm.LlmModelStore
import com.siya.ai.llm.LocalLlmEngine
import com.siya.ai.service.VoiceSessionState
import com.siya.ai.service.TtsModelStore
import com.siya.ai.service.VoiceModelInstaller
import com.siya.ai.stt.SttModelStore
import com.siya.ai.vad.VadModelStore
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private val Bg = Color(0xFF020611)
private val Deep = Color(0xFF030A17)
private val Panel = Color(0xFF071426)
private val Panel2 = Color(0xFF0B1B31)
private val Purple = Color(0xFF9B4DFF)
private val Violet = Color(0xFF6E35FF)
private val Blue = Color(0xFF148CFF)
private val Cyan = Color(0xFF13D9FF)
private val Pink = Color(0xFFDB43FF)
private val Green = Color(0xFF18F5B1)
private val Red = Color(0xFFFF4B73)
private val White = Color(0xFFF4F8FF)
private val Muted = Color(0xFF93A7C2)

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
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val baseWidth = 390.dp
                val baseHeight = 820.dp
                val scale = minOf(maxWidth / baseWidth, maxHeight / baseHeight)
                    .coerceIn(0.82f, 1.10f)
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density.density * scale,
                        density.fontScale
                    )
                ) {
                    when (page) {
                        "chat" -> PixelChat(onBack = { page = "home" }, onModels = { page = "models" })
                        "models" -> ModelHome(onBack = { page = "home" }, onAllModels = { page = "allModels" }, onSpeech = { page = "speech" }, onImport = { page = "import" })
                        "allModels" -> AllModels(onBack = { page = "models" })
                        "speech" -> SpeechModels(onBack = { page = "models" })
                        "import" -> ImportModelPage(onBack = { page = "models" })
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
    Box(Modifier.fillMaxSize().background(Color(0xFF00030C))) {
        ReferenceHomeBackground()
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 7.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(91.dp), verticalAlignment = Alignment.Top) {
                HomeHeaderButton(Icons.Default.ChatBubble, "CHAT", Cyan, onChat)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Siya", color = White, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text(" Ai", color = Cyan, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Y O U R   A I   C O M P A N I O N", color = Muted, fontSize = 8.sp, letterSpacing = 2.8.sp)
                    Box(Modifier.padding(top = 8.dp).width(185.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, Cyan, Color.Transparent))))
                }
                Spacer(Modifier.weight(1f))
                HomeHeaderButton(Icons.Default.Settings, "SETTINGS", Purple, onSettings)
            }
            Text("L I S T E N S     •     U N D E R S T A N D S     •     C O N T R O L S",
                Modifier.fillMaxWidth().padding(top = 2.dp), color = Cyan, fontSize = 9.sp, letterSpacing = 1.55.sp, textAlign = TextAlign.Center)
            Box(Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp), contentAlignment = Alignment.Center) {
                ReferenceHologram(active = voice.active, modifier = Modifier.fillMaxSize())
                Column(Modifier.fillMaxSize().padding(top = 32.dp, bottom = 15.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReferenceSideText(listOf("FAST", "SMART", "SECURE", "ALWAYS WITH YOU"), Alignment.Start)
                        ReferenceSideText(listOf("MORE", "THAN AI", "A REAL", "COMPANION"), Alignment.End)
                    }
                    ReferenceSideText(listOf("SIMPLE", "NATURAL", "POWERFUL", "IN YOUR LANGUAGE"), Alignment.End, Modifier.align(Alignment.End).padding(end = 2.dp, bottom = 3.dp))
                }
            }
            Text("Tap to speak", Modifier.fillMaxWidth().padding(top = 1.dp), color = Muted, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(1.dp))
            ReferenceMicButton(active = voice.active, enabled = microphoneGranted, onClick = onVoice)
            Text(if (!microphoneGranted) "Tap to allow microphone" else if (voice.active) "Tap to stop" else "Tap to speak",
                Modifier.fillMaxWidth().padding(top = 0.dp), color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
            Text("—   A L W A Y S   W I T H   Y O U   —", Modifier.fillMaxWidth().padding(top = 8.dp), color = Blue, fontSize = 8.sp, letterSpacing = 2.35.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReferenceSideText(lines: List<String>, alignment: Alignment.Horizontal, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = alignment) {
        Box(Modifier.width(37.dp).height(1.dp).background(Brush.horizontalGradient(listOf(Cyan.copy(.65f), Color.Transparent))))
        lines.forEach { Text(it, color = Muted.copy(.82f), fontSize = 7.sp, letterSpacing = 1.55.sp) }
    }
}

@Composable
private fun ReferenceHologram(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "reference-hologram")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(if (active) 1500 else 4800, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "phase")
    val breathe by transition.animateFloat(.96f, 1.045f, infiniteRepeatable(tween(if (active) 850 else 1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier) {
        val w = size.width; val h = size.height; val cx = w * .50f; val cy = h * .46f
        val r = minOf(w, h) * .235f * breathe; val stroke = 1.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(if (active) .25f else .18f), Blue.copy(.10f), Purple.copy(.09f), Color.Transparent), Offset(cx, cy), r * 2.35f), r * 2.35f, Offset(cx, cy))
        drawLine(Cyan.copy(.18f), Offset(cx, cy-r*2.25f), Offset(cx, cy+r*2.25f), stroke)
        drawLine(Cyan.copy(.16f), Offset(cx-r*2.25f, cy), Offset(cx+r*2.25f, cy), stroke)
        listOf(1.28f, 1.55f, 1.82f).forEachIndexed { i, s -> drawCircle(if (i == 1) Purple.copy(.24f) else Cyan.copy(.18f), r*s, Offset(cx,cy), style=Stroke((if(i==1) 1.5f else 1f).dp.toPx())) }
        val sweep = phase * 360f
        drawArc(Cyan.copy(.92f), sweep, 112f, false, Offset(cx-r*1.82f,cy-r*1.82f), androidx.compose.ui.geometry.Size(r*3.64f,r*3.64f), style=Stroke(3.6.dp.toPx(),cap=StrokeCap.Round))
        drawArc(Purple.copy(.88f), sweep+150f, 96f, false, Offset(cx-r*1.96f,cy-r*1.96f), androidx.compose.ui.geometry.Size(r*3.92f,r*3.92f), style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
        drawArc(Blue.copy(.62f), sweep+285f, 54f, false, Offset(cx-r*2.08f,cy-r*2.08f), androidx.compose.ui.geometry.Size(r*4.16f,r*4.16f), style=Stroke(1.6.dp.toPx(),cap=StrokeCap.Round))
        drawCircle(Brush.radialGradient(listOf(White.copy(.20f), Cyan.copy(.17f), Blue.copy(.11f), Purple.copy(.10f), Color.Transparent), Offset(cx-r*.10f,cy-r*.13f), r*1.06f), r*1.06f, Offset(cx,cy))
        drawCircle(Cyan.copy(.28f),r*1.00f,Offset(cx,cy),style=Stroke(1.2.dp.toPx()))
        drawCircle(White.copy(.15f),r*.78f,Offset(cx,cy),style=Stroke(1.dp.toPx()))
        listOf(.56f,.46f,.38f,.32f).forEachIndexed { i, oh ->
            val ow = listOf(2.05f,1.92f,1.70f,1.50f)[i]
            drawOval(Brush.horizontalGradient(listOf(Color.Transparent, if(i%2==0) Cyan.copy(.76f) else Purple.copy(.70f), Color.Transparent)), Offset(cx-r*ow/2f,cy-r*oh/2f), androidx.compose.ui.geometry.Size(r*ow,r*oh), style=Stroke((if(i<2) 2f else 1f).dp.toPx()))
        }
        repeat(9) { i ->
            val a=(i/9f)*6.28318f+phase*6.28318f*(if(i%2==0) 1f else -.65f)
            val rx=r*(.78f+(i%3)*.23f); val ry=r*(.33f+(i%2)*.15f)
            drawCircle(if(i%3==0) Purple.copy(.95f) else Cyan.copy(.9f), if(i%4==0) 3.dp.toPx() else 1.5.dp.toPx(), Offset(cx+kotlin.math.cos(a)*rx,cy+kotlin.math.sin(a)*ry))
        }
        repeat(48) { i ->
            val a=i/48f*6.28318f; val inner=r*1.88f; val outer=inner+if(i%4==0) r*.10f else r*.045f
            drawLine(if(i%4==0) Cyan.copy(.55f) else Muted.copy(.23f), Offset(cx+kotlin.math.cos(a)*inner,cy+kotlin.math.sin(a)*inner), Offset(cx+kotlin.math.cos(a)*outer,cy+kotlin.math.sin(a)*outer), stroke)
        }
        val beamX=cx+(phase-.5f)*r*1.15f
        drawLine(Cyan.copy(.22f),Offset(beamX,cy-r*2.18f),Offset(beamX,cy+r*2.18f),stroke)
        repeat(22) { i -> val a=i/22f*6.28318f+phase*6.28318f*.35f; val rr=r*(1.35f+(i%5)*.16f); drawCircle(if(i%4==0) Purple.copy(.9f) else Cyan.copy(.72f),if(i%7==0) 2.4.dp.toPx() else 1.dp.toPx(),Offset(cx+kotlin.math.cos(a)*rr,cy+kotlin.math.sin(a)*rr)) }
        drawCircle(Cyan.copy(.18f),r*.22f,Offset(cx,cy)); drawCircle(White.copy(.98f),r*.075f,Offset(cx,cy)); drawCircle(Cyan.copy(.95f),r*.13f,Offset(cx,cy),style=Stroke(2.dp.toPx()))
        val py=cy+r*1.92f
        drawOval(Brush.horizontalGradient(listOf(Color.Transparent,Purple.copy(.48f),Cyan.copy(.72f),Purple.copy(.48f),Color.Transparent)),Offset(cx-r*1.70f,py-r*.08f),androidx.compose.ui.geometry.Size(r*3.40f,r*.16f),style=Stroke(1.5.dp.toPx()))
        drawOval(Brush.horizontalGradient(listOf(Color.Transparent,Cyan.copy(.34f),Purple.copy(.30f),Color.Transparent)),Offset(cx-r*1.42f,py),androidx.compose.ui.geometry.Size(r*2.84f,r*.07f),style=Stroke(1.dp.toPx()))
    }
}

@Composable
private fun ReferenceHomeBackground() {
    val t=rememberInfiniteTransition(label="reference-bg")
    val phase by t.animateFloat(0f,1f,infiniteRepeatable(tween(5000,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="bg")
    Canvas(Modifier.fillMaxSize()) {
        val w=size.width; val h=size.height
        drawCircle(Purple.copy(.065f),w*.72f,Offset(w*.98f,h*.08f)); drawCircle(Blue.copy(.045f),w*.55f,Offset(w*.02f,h*.70f)); drawCircle(Cyan.copy(.025f),w*.70f,Offset(w*.54f,h*.46f))
        val y=h*.90f
        val path=androidx.compose.ui.graphics.Path().apply{moveTo(-20f,y);cubicTo(w*.12f,y-h*.045f,w*.28f,y+h*.045f,w*.43f,y);cubicTo(w*.60f,y-h*.050f,w*.75f,y+h*.045f,w*.94f,y-h*.018f);cubicTo(w*1.02f,y-h*.035f,w*1.04f,y-h*.020f,w+20f,y-h*.055f)}
        drawPath(path,Brush.horizontalGradient(listOf(Color.Transparent,Cyan.copy(.42f),Purple.copy(.46f),Color.Transparent)),style=Stroke(2.4.dp.toPx()))
        val y2=y+h*.026f
        val path2=androidx.compose.ui.graphics.Path().apply{moveTo(-20f,y2);cubicTo(w*.16f,y2+h*.035f,w*.28f,y2-h*.035f,w*.47f,y2);cubicTo(w*.64f,y2+h*.040f,w*.80f,y2-h*.040f,w+20f,y2-h*.02f)}
        drawPath(path2,Brush.horizontalGradient(listOf(Color.Transparent,Blue.copy(.22f),Purple.copy(.28f),Color.Transparent)),style=Stroke(1.2.dp.toPx()))
        repeat(14){i->val x=((i*73)%100)/100f*w;val yy=((i*47)%100)/100f*h;drawCircle(if(i%2==0)Cyan.copy(.24f)else Purple.copy(.22f),1.dp.toPx(),Offset(x,yy+(phase-.5f)*3f))}
    }
}

@Composable
private fun ReferenceMicButton(active:Boolean, enabled:Boolean, onClick:()->Unit) {
    val t=rememberInfiniteTransition(label="reference-mic")
    val pulse by t.animateFloat(1f,1.08f,infiniteRepeatable(tween(if(active)600 else 1400),RepeatMode.Reverse),label="pulse")
    Box(Modifier.fillMaxWidth().height(112.dp).clickable(enabled=enabled,onClick=onClick),contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
            val cx=size.width/2f; val cy=size.height*.50f; val r=38.dp.toPx()*pulse
            repeat(13){i->val x=cx-145.dp.toPx()+i*11.dp.toPx();val amp=(10+((i*7)%18)).dp.toPx();val alpha=if(enabled).55f else .18f;drawLine(Cyan.copy(alpha),Offset(x,cy-amp),Offset(x,cy+amp),1.dp.toPx(),cap=StrokeCap.Round);val xr=cx+145.dp.toPx()-i*11.dp.toPx();drawLine(Purple.copy(alpha),Offset(xr,cy-amp*.8f),Offset(xr,cy+amp*.8f),1.dp.toPx(),cap=StrokeCap.Round)}
            drawCircle(Brush.radialGradient(listOf(Cyan.copy(.25f),Purple.copy(.13f),Color.Transparent),Offset(cx,cy),r*2.1f),r*2.1f,Offset(cx,cy));drawCircle(Panel2,r,Offset(cx,cy));drawCircle(Cyan.copy(.95f),r,Offset(cx,cy),style=Stroke(2.8.dp.toPx()));drawCircle(Purple.copy(.92f),r*.88f,Offset(cx,cy),style=Stroke(1.8.dp.toPx()));drawCircle(Blue.copy(.72f),r*.76f,Offset(cx,cy),style=Stroke(1.dp.toPx()))
        }
        Icon(if(active)Icons.Default.Stop else Icons.Default.Mic,"Microphone",tint=White.copy(if(enabled)1f else .45f),modifier=Modifier.size(39.dp))
    }
}
@Composable
private fun ModelHome(onBack: () -> Unit, onAllModels: () -> Unit, onSpeech: () -> Unit, onImport: () -> Unit) {
    var showRequirements by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
                NeonSurface(Modifier.size(48.dp).clickable(onClick = onBack), Cyan, 15.dp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI", color = Purple, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text("Models", color = White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("Download, Select & Use On-Device Models", color = Muted, fontSize = 11.sp)
                }
                NeonSurface(Modifier.size(48.dp).clickable { showRequirements = true }, Green, 15.dp) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhoneAndroid, "Device requirements", tint = Green, modifier = Modifier.size(24.dp))
                        Box(Modifier.size(7.dp).align(Alignment.TopEnd).offset((-7).dp, 7.dp).background(Green, CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            ModelHomeCard("AI All Model Download Select", "Download all required models for complete AI", "Choose and download AI models for offline use", Purple, Icons.Default.Memory, onAllModels)
            Spacer(Modifier.height(14.dp))
            ModelHomeCard("Download Speech Models", "Download VAD, STT and all related models", "(One Click)", Cyan, Icons.Default.GraphicEq, onSpeech)
            Spacer(Modifier.height(14.dp))
            ModelHomeCard("Import GGUF", "Select and import your own model file", "Verified local GGUF • SHA-256 integrity check", Purple, Icons.Default.FolderOpen, onImport)
            Spacer(Modifier.height(18.dp))
            NeonSurface(Modifier.fillMaxWidth(), Blue, 16.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Cyan, modifier = Modifier.size(31.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Model downloads run in a foreground service and continue while Siya Ai is closed. Progress stays in the notification.", color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
        }
        if (showRequirements) {
            DeviceRequirementsDialog(onDismiss = { showRequirements = false })
        }
    }
}

@Composable
private fun DeviceRequirementsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Deep,
        titleContentColor = White,
        textContentColor = Muted,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeonIconBox(Icons.Default.PhoneAndroid, Green, 48.dp)
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Mobile Requirements", color = White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Siya Ai • On-Device Runtime", color = Green, fontSize = 10.sp)
                }
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RequirementRow(Icons.Default.Memory, "RAM", "Minimum 6 GB • 8 GB recommended for the complete local AI stack.", Purple)
                RequirementRow(Icons.Default.DeveloperBoard, "Processor / CPU", "Snapdragon 7-series / 8-series or MediaTek Dimensity class processor recommended.", Cyan)
                RequirementRow(Icons.Default.Speed, "GPU / Acceleration", "Vulkan-capable GPU is required/recommended for supported mobile acceleration; actual performance depends on device/runtime.", Blue)
                RequirementRow(Icons.Default.Storage, "Storage", "Plan roughly 1.4 GB for the current core model set: Qwen ~1.1 GB + STT ~80 MB + VAD ~2 MB + TTS ~180 MB. Keep additional free space for downloads and updates.", Green)
                RequirementRow(Icons.Default.Android, "Android", "Android 11+ is the target for background microphone behavior, subject to Android, foreground-service and OEM restrictions.", Cyan)
                RequirementRow(Icons.Default.WifiOff, "Offline", "After required models are installed, core conversation is designed to work without internet. Cloud services are not mandatory for the core runtime.", Blue)
                RequirementRow(Icons.Default.GraphicEq, "Audio", "16 kHz mono PCM is the canonical internal audio format. AEC/Noise Suppression, headset and Bluetooth behavior are device-dependent.", Pink)
                RequirementRow(Icons.Default.BatteryChargingFull, "Battery / RAM", "RAM, latency and battery figures are targets, not guarantees. Heavy models may be unloaded under memory pressure.", Green)
                RequirementRow(Icons.Default.Security, "Model Safety", "Model files use local integrity/checksum validation; invalid or missing files should produce a recoverable error.", Purple)
                RequirementRow(Icons.Default.Info, "Testing", "Real-device testing is required for screen-off, Doze, low-memory, noisy-room, headset/Bluetooth, barge-in and long-run behavior.", Blue)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Green, fontWeight = FontWeight.Bold)
            }
        }
    )
}


@Composable
private fun RequirementRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, accent: Color) {
    NeonSurface(Modifier.fillMaxWidth(), accent, 13.dp) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = Muted, fontSize = 9.sp, lineHeight = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun ModelHomeCard(title: String, subtitle: String, detail: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    NeonSurface(Modifier.fillMaxWidth().height(151.dp).clickable(onClick = onClick), accent, 24.dp) {
        Box(Modifier.fillMaxSize()){
            Canvas(Modifier.matchParentSize()){
                val w=size.width; val h=size.height
                val p=androidx.compose.ui.graphics.Path().apply{moveTo(w*.35f,h*.98f);cubicTo(w*.54f,h*.42f,w*.72f,h*.94f,w*.98f,h*.18f)}
                drawPath(p,Brush.horizontalGradient(listOf(Color.Transparent,accent.copy(.24f),Cyan.copy(.16f))),style=Stroke(1.4.dp.toPx()))
                drawCircle(accent.copy(.09f),h*.72f,Offset(w*.90f,h*.88f))
            }
        Row(Modifier.fillMaxSize().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonIconBox(icon, accent, 72.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(text=title, color=White, fontSize=20.sp, fontWeight=FontWeight.Bold)
                Text(text=subtitle, color=Muted, fontSize=13.sp, modifier=Modifier.padding(top=5.dp))
                if (detail.isNotBlank()) Text(text=detail, color=Muted, fontSize=12.sp, modifier=Modifier.padding(top=2.dp))
            }
            NeonArrow(accent)
        }
        }
    }
}

@Composable
private fun AllModels(onBack: () -> Unit) {
    var showRequirements by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LlmModelStore(context) }
    var installed by remember { mutableStateOf(store.isInstalled()) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf<String?>(null) }
    // Only the verified, locally actionable runtime model is shown here.
    // Cloud/demo placeholders are intentionally omitted.
    val rows = listOf(
        Triple("Qwen 2.5 1.5B", "Balanced local chat model • GGUF Q4_K_M", "~1.1 GB")
    )
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 15.dp, vertical = 8.dp)) {
        NeonHeader("AI All Model Download Select", "Choose and download AI models for offline use", onBack, Icons.Default.PhoneAndroid, Green, onIconClick = { showRequirements = true })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(rows) { row ->
                val isQwen = row.first == "Qwen 2.5 1.5B"
                ModelRow(row.first, row.second, row.third, if (row.first.contains("Gemini")) Blue else if (row.first.contains("GPT")) Cyan else if (row.first.contains("Llama") || row.first.contains("DeepSeek")) Cyan else Purple, isQwen && installed, isQwen && downloading, if (isQwen) progress else 0f, isQwen,
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
        if (error != null) Text(text=error!!, color=Red, fontSize=11.sp, modifier=Modifier.padding(4.dp))
        NeonSurface(Modifier.fillMaxWidth(), Blue, 14.dp) {
            Text(text="Only real local runtime/download paths are actionable. Cloud-only models are not presented as fake offline downloads.", modifier=Modifier.padding(11.dp), color=Muted, fontSize=10.sp)
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
        error?.let { Text(text=it, color=Red, fontSize=11.sp, modifier=Modifier.padding(bottom=7.dp)) }
        NeonSurface(Modifier.fillMaxWidth(), Cyan, 14.dp) { Text(text="Pipeline: MIC  →  VAD  →  Hindi STT  →  Local LLM  →  TTS", modifier=Modifier.padding(12.dp), color=Muted, fontSize=11.sp) }
    }
}

@Composable
private fun SpeechRow(title:String, subtitle:String, size:String, accent:Color, installed:Boolean, busy:Boolean, progress:Float, onClick:()->Unit) {
    NeonSurface(Modifier.fillMaxWidth(), accent, 18.dp) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                NeonIconBox(Icons.Default.GraphicEq, accent, 47.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) { Text(text=title, color=White, fontSize=14.sp, fontWeight=FontWeight.Bold); Text(text=subtitle, color=Muted, fontSize=10.sp) }
                Text(text=size, color=Muted, fontSize=10.sp, textAlign=TextAlign.End)
            }
            Spacer(Modifier.height(9.dp))
            if (busy) {
                LinearProgressIndicator(progress={progress}, modifier=Modifier.fillMaxWidth().height(5.dp), color=accent, trackColor=Panel2)
                Text(text=(progress*100).toInt().toString()+"%  Downloading…", color=accent, fontSize=9.sp, modifier=Modifier.padding(top=4.dp))
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
private fun ModelLogo(title:String,accent:Color){
    val symbol=when{
        title.startsWith("GPT")->"✦"
        title.startsWith("Gemini")->"G"
        title.startsWith("Llama")->"∞"
        title.startsWith("Qwen")->"✥"
        title.startsWith("DeepSeek")->"◈"
        title.startsWith("Mistral")||title.startsWith("Mixtral")->"M"
        title.startsWith("Phi")->"✦"
        else->"◇"
    }
    NeonSurface(Modifier.size(48.dp),accent,14.dp){
        Text(symbol,color=accent,fontSize=25.sp,fontWeight=FontWeight.ExtraBold,textAlign=TextAlign.Center)
    }
}

@Composable
private fun ModelRow(title:String, subtitle:String, size:String, accent:Color, installed:Boolean, downloading:Boolean, progress:Float, enabled:Boolean, onDownload:()->Unit, onDelete:(()->Unit)?) {
    NeonSurface(Modifier.fillMaxWidth(), accent, 17.dp) {
        Row(Modifier.padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
            ModelLogo(title, accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(text=title,color=White,fontSize=13.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(7.dp))
                    Surface(shape=RoundedCornerShape(10.dp),color=accent.copy(.10f),border=BorderStroke(1.dp,accent.copy(.65f))) { Text(text="LOCAL",modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp),color=accent,fontSize=7.sp,fontWeight=FontWeight.Bold) }
                }
                Text(text=subtitle,color=Muted,fontSize=9.sp,modifier=Modifier.padding(top=2.dp))
                if(downloading){
                    LinearProgressIndicator(progress={progress},Modifier.fillMaxWidth().padding(top=6.dp).height(4.dp),color=accent,trackColor=Panel2)
                    Text(text=(progress*100).toInt().toString()+"%  Downloading…",color=accent,fontSize=8.sp,modifier=Modifier.padding(top=2.dp))
                }
            }
            Spacer(Modifier.width(7.dp))
            Column(horizontalAlignment=Alignment.End) {
                Text(text=size,color=Muted,fontSize=9.sp)
                Spacer(Modifier.height(5.dp))
                if(installed) Surface(shape=RoundedCornerShape(10.dp),color=Green.copy(.12f),border=BorderStroke(1.dp,Green.copy(.7f))) { Text(text="✓ READY",modifier=Modifier.padding(horizontal=8.dp,vertical=7.dp),color=Green,fontSize=8.sp,fontWeight=FontWeight.Bold) }
                else if(enabled) NeonSurface(Modifier.height(37.dp).width(126.dp).clickable(enabled=!downloading,onClick=onDownload),accent,11.dp) {
                    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.Center) {
                        Icon(imageVector=if(downloading) Icons.Default.Pause else Icons.Default.Download,contentDescription=null,tint=White,modifier=Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(text=if(downloading)"STOP" else "Download",color=White,fontSize=9.sp,fontWeight=FontWeight.Bold)
                    }
                }
                else NeonSurface(Modifier.height(37.dp).width(126.dp),accent.copy(.28f),11.dp) {
                    Text(text="Download",color=Muted,fontSize=9.sp,fontWeight=FontWeight.Bold)
                }
                onDelete?.let { IconButton(onClick=it,modifier=Modifier.size(29.dp)){Icon(imageVector=Icons.Default.Delete,contentDescription="Delete",tint=Red,modifier=Modifier.size(15.dp))} }
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
                Text(text="Qwen 2.5 1.5B Instruct Q4_K_M",color=White,fontSize=17.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Text(text="SHA-256 verified import • local storage",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=5.dp),textAlign=TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick={launcher.launch(arrayOf("application/octet-stream","application/*"))},modifier=Modifier.fillMaxWidth().height(48.dp),colors=ButtonDefaults.buttonColors(containerColor=Purple),shape=RoundedCornerShape(14.dp)){
                    Icon(Icons.Default.FolderOpen,null);Spacer(Modifier.width(7.dp));Text("Select GGUF")
                }
                Text(text=status,color=Muted,fontSize=10.sp,modifier=Modifier.padding(top=10.dp),textAlign=TextAlign.Center)
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
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        attached=uri?.lastPathSegment?.substringAfterLast('/') ?: uri?.toString()
    }
    DisposableEffect(Unit){onDispose{engine.close()}}
    LaunchedEffect(voice.phase,voice.transcript,voice.response){
        if(voice.phase==VoiceSessionState.Phase.THINKING && voice.transcript.isNotBlank() && messages.none{it.first&&it.second==voice.transcript})messages+=true to voice.transcript
        if(voice.phase==VoiceSessionState.Phase.READY && voice.response.isNotBlank() && messages.none{!it.first&&it.second==voice.response})messages+=false to voice.response
    }

    Box(Modifier.fillMaxSize().background(Bg)){
        ChatAmbientBackground()
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=14.dp,vertical=6.dp)){
            Row(Modifier.fillMaxWidth().height(76.dp),verticalAlignment=Alignment.CenterVertically){
                NeonSurface(Modifier.size(48.dp).clickable(onClick=onBack),Cyan,15.dp){
                    Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=White,modifier=Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text("Siya",color=White,fontSize=30.sp,fontWeight=FontWeight.ExtraBold)
                        Text(" Ai",color=Cyan,fontSize=30.sp,fontWeight=FontWeight.ExtraBold)
                    }
                    Text("Y O U R   A I   C O M P A N I O N",color=Muted,fontSize=7.sp,letterSpacing=2.6.sp)
                }
                NeonSurface(Modifier.size(48.dp).clickable{menu=!menu},Purple,15.dp){
                    Icon(Icons.Default.Menu,"Menu",tint=White,modifier=Modifier.padding(10.dp))
                }
            }
            NeonDivider()
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding=PaddingValues(top=14.dp,bottom=12.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                if(messages.isEmpty()) item {
                    ChatAssistantBubble("Hello! 👋\nमैं Siya Ai हूँ\nमैं आपकी कैसे मदद कर सकती हूँ?","10:24 PM")
                }
                items(messages){m->
                    if(m.first) ChatUserBubble(m.second,"10:25 PM")
                    else ChatAssistantBubble(m.second,"10:25 PM")
                }
            }
            attached?.let{
                NeonSurface(Modifier.fillMaxWidth().padding(bottom=6.dp),Cyan,11.dp){
                    Text("📎 $it",color=Cyan,fontSize=9.sp,maxLines=1,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))
                }
            }
            NeonSurface(Modifier.fillMaxWidth(),Purple,20.dp){
                Row(Modifier.padding(6.dp),verticalAlignment=Alignment.CenterVertically){
                    NeonSurface(Modifier.size(50.dp).clickable{picker.launch(arrayOf("*/*"))},Purple,15.dp){
                        Icon(Icons.Default.AttachFile,"Attach",tint=White,modifier=Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    BasicChatField(input,onValueChange={input=it})
                    Spacer(Modifier.width(5.dp))
                    NeonSurface(Modifier.size(46.dp),Blue,50.dp){
                        Icon(Icons.Default.Mic,"Voice",tint=White,modifier=Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.width(5.dp))
                    NeonSurface(
                        Modifier.size(54.dp).clickable(enabled=input.isNotBlank()&&!busy){
                            val prompt=input.trim();input="";messages+=true to prompt;busy=true
                            scope.launch{
                                try{
                                    check(store.isInstalled()){"Qwen model is not installed"}
                                    val result=engine.complete(prompt)
                                    messages+=false to result.text
                                    installed=true
                                }catch(e:Exception){
                                    messages+=false to "Local AI error: "+(e.message?:"model unavailable")
                                }finally{busy=false}
                            }
                        },Purple,50.dp
                    ){Icon(Icons.Default.Send,"Send",tint=White,modifier=Modifier.padding(14.dp))}
                }
            }
        }
        if(menu){
            NeonSurface(
                Modifier.align(Alignment.TopEnd).padding(top=86.dp,end=14.dp).width(190.dp),
                Purple,18.dp
            ){
                Column(Modifier.padding(7.dp)){
                    MenuItem("AI Models",Icons.Default.Memory){menu=false;onModels()}
                    MenuItem("Offline: "+if(installed)"READY" else "MODEL MISSING",Icons.Default.CloudOff){menu=false}
                }
            }
        }
    }
}

@Composable
private fun RowScope.BasicChatField(value:String,onValueChange:(String)->Unit){
    androidx.compose.foundation.text.BasicTextField(
        value=value,onValueChange=onValueChange,
        modifier=Modifier.weight(1f).padding(horizontal=4.dp,vertical=7.dp),
        textStyle=androidx.compose.ui.text.TextStyle(color=White,fontSize=15.sp),
        maxLines=4,
        decorationBox={inner->
            if(value.isEmpty())Text("Type your message…",color=Muted,fontSize=15.sp)
            inner()
        }
    )
}

@Composable
private fun ChatAssistantBubble(text:String,time:String){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
        NeonAvatar()
        Spacer(Modifier.width(8.dp))
        NeonSurface(Modifier.widthIn(max=330.dp),Cyan,18.dp){
            Column(Modifier.padding(horizontal=14.dp,vertical=10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("Siya Ai",color=Cyan,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(9.dp))
                    Text(time,color=Muted,fontSize=9.sp)
                }
                Text(text,color=White,fontSize=15.sp,modifier=Modifier.padding(top=6.dp))
            }
        }
    }
}

@Composable
private fun ChatUserBubble(text:String,time:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
        NeonSurface(Modifier.widthIn(max=355.dp),Purple,18.dp){
            Column(Modifier.padding(horizontal=14.dp,vertical=9.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                    Text(time,color=Muted,fontSize=9.sp)
                    Spacer(Modifier.width(5.dp))
                    Text("✓✓",color=Cyan,fontSize=9.sp)
                }
                Text(text,color=White,fontSize=15.sp,modifier=Modifier.padding(top=4.dp))
            }
        }
    }
}

@Composable
private fun ChatAmbientBackground(){
    Canvas(Modifier.fillMaxSize()){
        val w=size.width
        val h=size.height
        drawCircle(Purple.copy(.10f),w*.42f,Offset(w*.52f,-h*.01f))
        drawCircle(Cyan.copy(.055f),w*.50f,Offset(w*.02f,h*.58f))
        drawCircle(Blue.copy(.045f),w*.52f,Offset(w*1.02f,h*.70f))
        val waveY=h*.83f
        val path=androidx.compose.ui.graphics.Path().apply{
            moveTo(0f,waveY)
            cubicTo(w*.18f,waveY-h*.035f,w*.30f,waveY+h*.035f,w*.48f,waveY)
            cubicTo(w*.66f,waveY-h*.035f,w*.80f,waveY+h*.035f,w,waveY-h*.008f)
        }
        drawPath(path,brush=Brush.horizontalGradient(listOf(Color.Transparent,Cyan.copy(.32f),Purple.copy(.32f),Color.Transparent)),style=Stroke(2.dp.toPx()))
    }
}

@Composable
private fun MenuItem(text:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick=onClick).padding(11.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(imageVector=icon,contentDescription=null,tint=Cyan,modifier=Modifier.size(20.dp));Spacer(Modifier.width(9.dp));Text(text=text,color=White,fontSize=11.sp)
    }
}

@Composable
private fun AssistantBubble(text:String){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top){
        NeonAvatar();Spacer(Modifier.width(8.dp))
        NeonSurface(Modifier.widthIn(max=310.dp),Cyan,19.dp){
            Column(Modifier.padding(13.dp)){Text(text="Siya Ai",color=Cyan,fontSize=12.sp,fontWeight=FontWeight.Bold);Text(text=text,color=White,fontSize=15.sp,modifier=Modifier.padding(top=6.dp))}
        }
    }
}

@Composable
private fun UserBubble(text:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
        NeonSurface(Modifier.widthIn(max=330.dp),Purple,19.dp){Text(text=text,color=White,fontSize=15.sp,modifier=Modifier.padding(14.dp))}
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
        drawLine(color=Cyan.copy(.45f),start=Offset(c.x-size.minDimension*.20f,c.y+size.minDimension*.20f),end=Offset(c.x+size.minDimension*.20f,c.y+size.minDimension*.20f),strokeWidth=1.4f)
    }
}

@Composable
private fun NeonHeader(title:String,subtitle:String,onBack:()->Unit,icon:androidx.compose.ui.graphics.vector.ImageVector,accent:Color,onIconClick:(()->Unit)?=null){
    Row(Modifier.fillMaxWidth().height(66.dp),verticalAlignment=Alignment.CenterVertically){
        NeonSurface(Modifier.size(48.dp).clickable(onClick=onBack),Cyan,15.dp){Icon(imageVector=Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Back",tint=White,modifier=Modifier.padding(10.dp))}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){Text(text=title,color=White,fontSize=25.sp,fontWeight=FontWeight.ExtraBold);Text(text=subtitle,color=Muted,fontSize=11.sp)}
        NeonSurface(Modifier.size(48.dp).then(if(onIconClick!=null) Modifier.clickable(onClick=onIconClick) else Modifier),accent,15.dp){Icon(imageVector=icon,contentDescription="Device requirements",tint=accent,modifier=Modifier.padding(11.dp))}
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
private fun NeonSurface(modifier:Modifier,accent:Color,shapeDp:androidx.compose.ui.unit.Dp,content: @Composable () -> Unit){
    val transition = rememberInfiniteTransition(label = "neon-surface")
    val glow by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val shape = RoundedCornerShape(shapeDp)
    Box(
        modifier = modifier
            .drawBehind {
                val inset = 2.dp.toPx()
                drawRoundRect(
                    color = accent.copy(alpha = glow * 0.20f),
                    topLeft = Offset(-inset, -inset),
                    size = androidx.compose.ui.geometry.Size(size.width + inset * 2f, size.height + inset * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shapeDp.toPx() + inset),
                    style = Stroke(width = 6.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(accent.copy(.92f), Cyan.copy(.62f), accent.copy(.92f))),
                    topLeft = Offset(0f, 0f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(shapeDp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = .13f),
                        Panel.copy(alpha = .97f),
                        Deep.copy(alpha = .98f),
                        accent.copy(alpha = .08f)
                    )
                ),
                shape
            )
            .border(BorderStroke(0.8.dp, accent.copy(.42f)), shape),
        contentAlignment = Alignment.Center
    ) { content() }
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
