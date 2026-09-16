package com.siya.ai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class SiyaChatItem(val user: Boolean, val text: String, val thinking: Boolean = false)
private val Black = Color(0xFF01040B)
private val Panel = Color(0xFF07111F)
private val Purple = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF21D4FF)
private val Blue = Color(0xFF168BFF)
private val Muted = Color(0xFF8995AB)

@Composable
fun SiyaFastApp2(microphoneGranted: Boolean, onRequestMicrophone: () -> Unit, onStartVoice: () -> Unit, onStopVoice: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LlmModelStore(context) }
    val engine = remember { LocalLlmEngine(store) }
    var ready by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("home") }
    val voice by VoiceSessionState.state.collectAsState()
    LaunchedEffect(Unit) { if (store.isInstalled()) runCatching { engine.load(); ready = true } }
    DisposableEffect(Unit) { onDispose { engine.close() } }
    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Black, surface = Panel)) {
        when (page) {
            "chat" -> FastChat2(engine, ready, { page = "home" }, { page = "models" })
            "models" -> FastModels2 { page = "home" }
            "settings" -> FastSettings2({ page = "home" }, { page = "chat" }, { page = "models" })
            else -> FastHome2(microphoneGranted, voice, { page = "chat" }, { page = "settings" }) { if (!microphoneGranted) onRequestMicrophone() else if (voice.active) onStopVoice() else onStartVoice() }
        }
    }
}

@Composable private fun FastHome2(mic:Boolean, voice:VoiceSessionState.State, chat:()->Unit, settings:()->Unit, speak:()->Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)) {
        val compact=maxHeight<700.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(if(compact)54.dp else 62.dp),verticalAlignment=Alignment.CenterVertically){
                TopIcon(Icons.Default.ChatBubble,Cyan,chat);Spacer(Modifier.weight(1f));Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Siya Ai",color=Color.White,fontSize=if(maxWidth<360.dp)27.sp else 31.sp,fontWeight=FontWeight.ExtraBold);Text("Y O U R   A I   C O M P A N I O N",color=Muted,fontSize=6.sp,letterSpacing=1.2.sp)};Spacer(Modifier.weight(1f));TopIcon(Icons.Default.Settings,Purple,settings)
            }
            Text("L I S T E N S   •   U N D E R S T A N D S   •   C O N T R O L S",Modifier.fillMaxWidth(),color=Cyan,fontSize=7.sp,textAlign=TextAlign.Center)
            Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){FastOrb(voice.active,Modifier.fillMaxSize())}
            Text(when(voice.phase){VoiceSessionState.Phase.LISTENING->"Listening";VoiceSessionState.Phase.TRANSCRIBING->"Understanding";VoiceSessionState.Phase.THINKING->"Thinking…";VoiceSessionState.Phase.READY->"Ready";VoiceSessionState.Phase.ERROR->"Needs attention";else->"Tap to Speak"},Modifier.fillMaxWidth(),color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            voice.error?.let{Text(it,Modifier.fillMaxWidth().padding(top=4.dp),color=Color(0xFFFF7187),fontSize=9.sp,textAlign=TextAlign.Center,maxLines=2)}
            Text("Hindi  •  Hinglish  •  English",Modifier.fillMaxWidth().padding(top=3.dp),color=Muted,fontSize=9.sp,textAlign=TextAlign.Center)
            Surface(Modifier.size(if(compact)80.dp else 88.dp).align(Alignment.CenterHorizontally).clickable(onClick=speak),CircleShape,if(voice.active)Purple.copy(.25f) else Panel,border=BorderStroke(2.dp,if(voice.active)Cyan else Purple)){Icon(if(voice.active)Icons.Default.Stop else Icons.Default.Mic,null,tint=Color.White,modifier=Modifier.padding(22.dp))}
            Text(if(!mic)"Tap to allow microphone" else if(voice.active)"Tap to stop" else "Tap to speak",Modifier.fillMaxWidth(),color=Muted,fontSize=8.sp,textAlign=TextAlign.Center)
            Text("—   A L W A Y S   W I T H   Y O U   —",Modifier.fillMaxWidth().padding(top=4.dp),color=Blue,fontSize=6.sp,textAlign=TextAlign.Center)
        }
    }
}

@Composable private fun TopIcon(icon:androidx.compose.ui.graphics.vector.ImageVector,color:Color,click:()->Unit){Surface(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).clickable(onClick=click),RoundedCornerShape(15.dp),Panel,border=BorderStroke(1.dp,color.copy(.85f))){Icon(icon,null,tint=color,modifier=Modifier.padding(11.dp))}}

@Composable private fun FastOrb(active:Boolean,modifier:Modifier){val inf=rememberInfiniteTransition(label="orb");val scale by inf.animateFloat(.94f,1.04f,infiniteRepeatable(tween(1400),RepeatMode.Reverse),label="s");Canvas(modifier){val x=size.width/2;val y=size.height*.47f;val r=minOf(size.width,size.height)*.27f*scale;drawCircle(Color(0xFF0B4FFF).copy(.12f),r*1.35f,Offset(x,y));drawCircle(Cyan,r,Offset(x,y),style=Stroke(3.dp.toPx()));drawCircle(Purple,r*.76f,Offset(x,y),style=Stroke(2.dp.toPx()));drawCircle(Blue,r*.9f,Offset(x,y),style=Stroke(1.dp.toPx()));drawOval(Cyan.copy(.8f),Rect(x-r*1.45f,y-r*.18f,x+r*1.45f,y+r*.18f),style=Stroke(2.dp.toPx()));drawLine(Cyan.copy(.35f),Offset(x,y-r*1.7f),Offset(x,y+r*1.7f),1.dp.toPx());drawCircle(Color.White,4.dp.toPx(),Offset(x,y));drawOval(Purple,Rect(x-r*1.7f,y+r*1.5f,x+r*1.7f,y+r*1.85f),style=Stroke(2.dp.toPx()));drawOval(Cyan,Rect(x-r*1.5f,y+r*1.58f,x+r*1.5f,y+r*1.78f),style=Stroke(1.dp.toPx()));if(active)drawCircle(Cyan.copy(.8f),r*.12f,Offset(x,y))}}

private fun quickReply(text: String): String? = when (text.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")) {
    "hi", "hello", "hey", "hii", "hlo" -> "Hi! 👋"
    "namaste", "नमस्ते" -> "Namaste! 🙏"
    else -> null
}

@Composable private fun FastChat2(engine:LocalLlmEngine,ready:Boolean,back:()->Unit,models:()->Unit){
    val scope=rememberCoroutineScope();val voice by VoiceSessionState.state.collectAsState();val list=rememberLazyListState();val messages=remember{mutableStateListOf<SiyaChatItem>()};var input by rememberSaveable{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    fun send(){if(!ready||busy||input.isBlank())return;val p=input.trim();input="";messages+=SiyaChatItem(true,p);busy=true;val instant=quickReply(p);if(instant!=null){messages+=SiyaChatItem(false,instant);busy=false;return};messages+=SiyaChatItem(false,"Thinking…",true);scope.launch{try{val r=engine.complete(p);val i=messages.indexOfLast{it.thinking};if(i>=0)messages[i]=SiyaChatItem(false,r.text.ifBlank{"I’m ready."})}catch(e:Exception){val i=messages.indexOfLast{it.thinking};if(i>=0)messages[i]=SiyaChatItem(false,"Local AI error: ${e.message?:"model unavailable"}")}finally{busy=false}}}
    LaunchedEffect(messages.size,busy){if(messages.isNotEmpty())list.animateScrollToItem(messages.lastIndex)}
    LaunchedEffect(voice.phase,voice.transcript,voice.response,voice.error){when(voice.phase){VoiceSessionState.Phase.THINKING->{val t=voice.transcript.trim();if(t.isNotBlank()&&messages.none{it.user&&it.text==t})messages+=SiyaChatItem(true,t);if(messages.none{it.thinking})messages+=SiyaChatItem(false,"Thinking…",true);busy=true};VoiceSessionState.Phase.READY->{val i=messages.indexOfLast{it.thinking};if(i>=0&&voice.response.isNotBlank())messages[i]=SiyaChatItem(false,voice.response.trim());busy=false};VoiceSessionState.Phase.ERROR->{val i=messages.indexOfLast{it.thinking};if(i>=0)messages[i]=SiyaChatItem(false,voice.error?:"Voice error");busy=false};else->Unit}}
    Scaffold(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),containerColor=Black,contentWindowInsets=WindowInsets(0,0,0,0),bottomBar={Row(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(8.dp),verticalAlignment=Alignment.Bottom){OutlinedTextField(input,{input=it},Modifier.weight(1f),enabled=ready&&!busy,placeholder={Text(if(ready)"Ask Siya…" else "Warming local AI…",color=Muted)},maxLines=4,shape=RoundedCornerShape(18.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={send()}),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Purple,unfocusedBorderColor=Panel,focusedTextColor=Color.White,unfocusedTextColor=Color.White,cursorColor=Cyan));IconButton(enabled=ready&&!busy&&input.isNotBlank(),onClick=::send){Icon(Icons.Default.Send,null,tint=if(ready&&!busy&&input.isNotBlank())Cyan else Muted)}}}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=10.dp)){Row(Modifier.fillMaxWidth().height(54.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Color.White)};Column(Modifier.weight(1f)){Text("Siya Chat",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold);Text(if(ready)"Qwen 2.5 1.5B • Offline • Ready" else "Preparing local model…",color=if(ready)Cyan else Muted,fontSize=9.sp)};TextButton(onClick=models){Text("MODEL",color=Cyan,fontSize=8.sp)}};LazyColumn(Modifier.weight(1f),state=list,contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){if(messages.isEmpty())item{Text(if(ready)"Fast private offline chat" else "Local model is warming up…",Modifier.fillMaxWidth().padding(top=45.dp),color=Muted,textAlign=TextAlign.Center,fontSize=11.sp)};items(messages){m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.user)Arrangement.End else Arrangement.Start){Surface(shape=RoundedCornerShape(17.dp),color=if(m.user)Purple.copy(.22f)else Panel,border=BorderStroke(1.dp,if(m.user)Purple.copy(.4f)else Color.White.copy(.04f))){Row(Modifier.padding(11.dp),verticalAlignment=Alignment.CenterVertically){if(m.thinking){CircularProgressIndicator(Modifier.size(14.dp),color=Cyan,strokeWidth=2.dp);Spacer(Modifier.width(8.dp))};Text(m.text,color=Color.White,fontSize=13.sp)}}}}}}}
}

@Composable private fun FastModels2(back:()->Unit){val c=LocalContext.current;val scope=rememberCoroutineScope();val llm=remember{LlmModelStore(c)};val vad=remember{VadModelStore(c)};val stt=remember{SttModelStore(c)};val vi=remember{VoiceModelInstaller(vad,stt)};var q by remember{mutableStateOf(llm.isInstalled())};var v by remember{mutableStateOf(vad.isInstalled())};var s by remember{mutableStateOf(stt.isInstalled())};var busy by remember{mutableStateOf(false)};var done by remember{mutableLongStateOf(0)};var total by remember{mutableLongStateOf(0)};var msg by remember{mutableStateOf("")};var err by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(18.dp)){Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Color.White)};Text("AI Models",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)};ModelRow("Qwen 2.5 1.5B","Q4_K_M • ~1.12 GB",q,Purple);Spacer(Modifier.height(8.dp));ModelRow("Silero VAD","16 kHz",v,Cyan);Spacer(Modifier.height(8.dp));ModelRow("Hindi STT","IndicConformer",s,Blue);Spacer(Modifier.height(14.dp));if(busy){val p=if(total>0)done.toDouble()/total*100 else 0.0;LinearProgressIndicator({(p/100).toFloat()},Modifier.fillMaxWidth(),color=Cyan);Text(String.format(Locale.US,"%.1f%% • %d / %d MB",p,done/1048576,if(total>0)total/1048576 else 0),Modifier.fillMaxWidth().padding(top=5.dp),color=Cyan,fontSize=10.sp,textAlign=TextAlign.Center)};Button(enabled=!busy&&!q,onClick={scope.launch{busy=true;msg="";err="";try{LlmModelInstaller(llm).download{d,t->done=d;total=t};q=true;msg="Qwen downloaded and verified"}catch(e:Exception){err=e.message?:"Download failed"}finally{busy=false}}},Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Purple)){Icon(Icons.Default.Download,null);Spacer(Modifier.width(7.dp));Text(if(q)"Qwen Installed" else "Download Qwen")};Spacer(Modifier.height(8.dp));OutlinedButton(enabled=!busy&&(!v||!s),onClick={scope.launch{busy=true;msg="";err="";try{if(!v){vi.downloadVad{d,t->done=d;total=t};v=true};if(!s){vi.downloadHindiStt{d,t->done=d;total=t};s=true};msg="Voice models downloaded and verified"}catch(e:Exception){err=e.message?:"Voice download failed"}finally{busy=false}}},Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(15.dp)){Icon(Icons.Default.RecordVoiceOver,null);Spacer(Modifier.width(7.dp));Text(if(v&&s)"Voice Models Installed" else "Download VAD + Hindi STT")};if(msg.isNotBlank())Text(msg,color=Cyan,fontSize=10.sp,modifier=Modifier.padding(top=8.dp));if(err.isNotBlank())Text(err,color=Color(0xFFFF7187),fontSize=10.sp,modifier=Modifier.padding(top=6.dp));Text("Model folder: ${llm.modelDirectory().absolutePath}",color=Muted,fontSize=9.sp,modifier=Modifier.padding(top=15.dp));Text("Normal APK updates keep this model folder. Broad all-files permission is not required.",color=Muted,fontSize=9.sp,modifier=Modifier.padding(top=5.dp))}}
@Composable private fun ModelRow(title:String,sub:String,ready:Boolean,color:Color){Surface(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),Panel,border=BorderStroke(1.dp,color.copy(.3f))){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Memory,null,tint=color);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp);Text(sub,color=Muted,fontSize=9.sp)};Text(if(ready)"READY"else"MISSING",color=if(ready)Cyan else Muted,fontSize=8.sp)}}}
@Composable private fun FastSettings2(back:()->Unit,chat:()->Unit,models:()->Unit){Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(18.dp)){Row(Modifier.fillMaxWidth().height(52.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Color.White)};Text("Settings",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)};Setting("Chat","Fast offline Qwen",Icons.Default.ChatBubble,chat);Setting("AI Models","Qwen + VAD + Hindi STT",Icons.Default.Memory,models);Setting("Voice","Microphone → VAD → STT → Qwen",Icons.Default.Mic,null);Setting("Privacy","Local model and inference",Icons.Default.Lock,null)}}
@Composable private fun Setting(title:String,sub:String,icon:androidx.compose.ui.graphics.vector.ImageVector,click:(()->Unit)?){Surface(Modifier.fillMaxWidth().padding(vertical=4.dp).then(if(click!=null)Modifier.clickable(onClick=click)else Modifier),RoundedCornerShape(16.dp),Panel,border=BorderStroke(1.dp,Color.White.copy(.05f))){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Cyan);Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(title,color=Color.White,fontWeight=FontWeight.Bold,fontSize=13.sp);Text(sub,color=Muted,fontSize=10.sp)}}}}
