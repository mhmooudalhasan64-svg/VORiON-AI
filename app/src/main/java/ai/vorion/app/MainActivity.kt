package ai.vorion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vorion.hologram.HologramState
import ai.vorion.hologram.VorionHologram
import ai.vorion.network.VorionApiClient
import ai.vorion.network.VorionMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val Background = Color(0xFF070707)
private val Panel = Color(0xFF121212)
private val Gold = Color(0xFFE0BD70)
private val SoftText = Color(0xFFE8E0D0)
private val Muted = Color(0xFF898477)

private data class ConversationLine(val role: String, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VorionAuthGate { VorionApp() } }
    }
}

@Composable
private fun VorionApp() {
    val colors = darkColorScheme(
        primary = Gold,
        background = Background,
        surface = Panel,
        onBackground = SoftText,
        onSurface = SoftText
    )
    MaterialTheme(colorScheme = colors) {
        VorionChatScreen()
    }
}

@Composable
private fun VorionChatScreen() {
    val conversation = remember {
        mutableStateListOf(
            ConversationLine(
                role = "assistant",
                text = "أنا VORION AI. واجهتي جاهزة، وبانتظار إكمال إعداد الاتصال الآمن."
            )
        )
    }
    var draft by remember { mutableStateOf("") }
    var hologramState by remember { mutableStateOf(HologramState.IDLE) }
    var status by remember { mutableStateOf("الاتصال بالخادم غير مُعدّ بعد") }
    var sending by remember { mutableStateOf(false) }
    var showCall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val gatewayUrl = BuildConfig.VORION_GATEWAY_URL
    val apiClient = remember(gatewayUrl) {
        VorionApiClient(
            gatewayBaseUrl = gatewayUrl,
            // Authentication is intentionally not faked or hardcoded.
            // Connect this to the real sign-in/session flow before enabling live inference.
            sessionTokenProvider = {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)?.let { it.await().token }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VORION AI",
                color = Gold,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "YOUR INTELLIGENT ASSISTANT",
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )

            VorionHologram(
                state = hologramState,
                modifier = Modifier.padding(top = 4.dp),
                size = 190.dp
            )

            Text(
                text = when (hologramState) {
                    HologramState.IDLE -> "أنا جاهز للمساعدة"
                    HologramState.THINKING -> "أحلل طلبك..."
                    HologramState.SPEAKING -> "VORION يرد"
                    HologramState.ERROR -> "تعذر إكمال الطلب"
                    else -> "VORION AI"
                },
                color = SoftText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = status,
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showCall = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showCall) Gold else Panel,
                        contentColor = if (!showCall) Background else SoftText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("الدردشة") }
                Button(
                    onClick = { showCall = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCall) Gold else Panel,
                        contentColor = if (showCall) Background else SoftText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("اتصال صوتي") }
            }

            if (showCall) {
                VorionVoiceCallScreen(
                    onSendMessage = { spokenText, onReply ->
                        conversation.add(ConversationLine("user", spokenText))
                        scope.launch {
                            try {
                                if (gatewayUrl.isBlank()) {
                                    throw IllegalStateException("لم يتم إعداد خادم VORION بعد.")
                                }
                                val history = conversation.takeLast(20)
                                    .map { VorionMessage(it.role, it.text) }
                                val reply = apiClient.sendMessage(history)
                                conversation.add(ConversationLine("assistant", reply.text))
                                onReply(reply.text)
                            } catch (error: Exception) {
                                status = error.message ?: "تعذر الاتصال بخادم VORION."
                                onReply("تعذر الاتصال بالمساعد حالياً. يلزم إكمال إعداد الخادم وتسجيل الدخول.")
                            }
                        }
                    },
                    onEndCall = { showCall = false }
                )
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Panel, RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversation) { line ->
                    val isAssistant = line.role == "assistant"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isAssistant) Color(0xFF1A1813) else Color(0xFF202020),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isAssistant) "VORION" else "أنت",
                            color = if (isAssistant) Gold else Muted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = line.text, color = SoftText, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    enabled = !sending,
                    textStyle = TextStyle(color = SoftText, fontSize = 14.sp),
                    decorationBox = { innerTextField ->
                        if (draft.isEmpty()) {
                            Text("اكتب رسالتك إلى VORION...", color = Muted, fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                )
                Button(
                    onClick = {
                        val message = draft.trim()
                        if (message.isNotEmpty() && !sending) {
                            conversation.add(ConversationLine("user", message))
                            draft = ""
                            sending = true
                            hologramState = HologramState.THINKING
                            status = "جارٍ تجهيز الطلب..."
                            scope.launch {
                                try {
                                    if (gatewayUrl.isBlank()) {
                                        throw IllegalStateException(
                                            "لم يتم إعداد عنوان خادم VORION بعد."
                                        )
                                    }
                                    val history = conversation
                                        .takeLast(20)
                                        .map { VorionMessage(it.role, it.text) }
                                    val reply = apiClient.sendMessage(history)
                                    conversation.add(ConversationLine("assistant", reply.text))
                                    hologramState = HologramState.SPEAKING
                                    status = "اكتمل الرد"
                                } catch (error: Exception) {
                                    hologramState = HologramState.ERROR
                                    status = error.message ?: "تعذر الاتصال بخادم VORION."
                                    conversation.add(
                                        ConversationLine(
                                            "assistant",
                                            "لم أتمكن من إرسال طلبك بعد. لم يتم تفعيل الاتصال المباشر حتى الآن."
                                        )
                                    )
                                } finally {
                                    sending = false
                                }
                            }
                        }
                    },
                    enabled = draft.isNotBlank() && !sending,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Background,
                        disabledContainerColor = Color(0xFF51472F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (sending) "..." else "إرسال", fontWeight = FontWeight.Bold)
                }
            }
            }
            Text(
                text = "الإصدار التأسيسي 0.1.0",
                color = Color(0xFF5E5A52),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
