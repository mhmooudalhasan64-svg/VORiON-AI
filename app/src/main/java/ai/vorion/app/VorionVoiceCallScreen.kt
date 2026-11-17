package ai.vorion.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ai.vorion.hologram.HologramState
import ai.vorion.hologram.VorionHologram
import java.util.Locale

private val CallGold = Color(0xFFE0BD70)
private val CallPanel = Color(0xFF171717)
private val CallMuted = Color(0xFFAAA394)

@Composable
fun VorionVoiceCallScreen(
    onSendMessage: (String, (String?) -> Unit) -> Unit,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf("اضغط على الميكروفون وابدأ الحديث") }
    var recognizedText by remember { mutableStateOf("") }
    var assistantText by remember { mutableStateOf("أنا VORION. تفضل، أستمع إليك.") }
    var listening by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val latestSend by rememberUpdatedState(onSendMessage)

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث إلى VORION")
        }
    }

    val tts = remember {
        TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                // Arabic voice availability depends on the speech engine installed on the device.
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) {
            status = "الميكروفون جاهز"
            speechRecognizer?.startListening(speechIntent)
        } else {
            status = "يلزم السماح باستخدام الميكروفون لإجراء المكالمة"
        }
    }

    DisposableEffect(speechRecognizer, tts) {
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    status = "أستمع إليك الآن..."
                }
                override fun onBeginningOfSpeech() { status = "تابع حديثك..." }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    listening = false
                    status = "أحلل كلامك..."
                }
                override fun onError(error: Int) {
                    listening = false
                    status = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "لم ألتقط كلاماً واضحاً. حاول مرة أخرى."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "إذن الميكروفون غير متاح."
                        else -> "توقف التعرف على الصوت. حاول مجدداً."
                    }
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val phrase = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (phrase.isNotBlank()) {
                        recognizedText = phrase
                        busy = true
                        status = "VORION يفكر..."
                        latestSend(phrase) { answer ->
                            busy = false
                            if (answer.isNullOrBlank()) {
                                status = "تعذر الحصول على إجابة"
                            } else {
                                assistantText = answer
                                status = "انتهى الرد — يمكنك التحدث مجدداً"
                                val speakResult = tts.setLanguage(Locale("ar"))
                                tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "vorion-answer")
                            }
                        }
                    } else {
                        status = "لم أسمع سؤالاً واضحاً. حاول مجدداً."
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    recognizedText = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        onDispose {
            speechRecognizer?.destroy()
            tts.stop()
            tts.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CallPanel, RoundedCornerShape(22.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VorionHologram(
            state = when {
                listening -> HologramState.LISTENING
                busy -> HologramState.THINKING
                else -> HologramState.IDLE
            },
            modifier = Modifier.padding(bottom = 16.dp),
            size = 210.dp
        )
        Text("VORION AI", color = CallGold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("مكالمة صوتية", color = CallMuted, fontSize = 13.sp,
            modifier = Modifier.padding(top = 5.dp))
        Text(status, color = CallGold, fontSize = 14.sp,
            modifier = Modifier.padding(top = 18.dp))
        if (recognizedText.isNotBlank()) {
            Text("أنت: $recognizedText", color = Color.White, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        }
        Text("VORION: $assistantText", color = CallMuted, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        Spacer(Modifier.size(26.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (busy) return@Button
                    if (!permissionGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (speechRecognizer == null) {
                        status = "خدمة التعرف على الكلام غير متاحة على هذا الجهاز"
                    } else {
                        recognizedText = ""
                        status = "جارٍ تشغيل الميكروفون..."
                        speechRecognizer.startListening(speechIntent)
                    }
                },
                enabled = !busy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CallGold,
                    contentColor = Color(0xFF090909)
                ),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (listening) "استمع..." else "تحدث", fontWeight = FontWeight.Bold) }
            Button(
                onClick = {
                    speechRecognizer?.stopListening()
                    listening = false
                    tts.stop()
                    status = "تم إيقاف الصوت"
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF393939),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) { Text("إيقاف الصوت") }
        }
        Button(
            onClick = {
                speechRecognizer?.cancel()
                tts.stop()
                onEndCall()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8F2929),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) { Text("إنهاء المكالمة") }
        Text(
            "الصوت يعمل بنظام سؤال وجواب متتابع حالياً، وليس بثاً صوتياً متزامناً مستمراً.",
            color = CallMuted, fontSize = 10.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}
