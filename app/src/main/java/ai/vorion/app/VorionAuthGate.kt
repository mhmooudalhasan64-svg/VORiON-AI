package ai.vorion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val AuthBackground = Color(0xFF070707)
private val AuthGold = Color(0xFFE0BD70)
private val AuthText = Color(0xFFE8E0D0)
private val AuthMuted = Color(0xFF898477)

@Composable
internal fun VorionAuthGate(content: @Composable () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }

    androidx.compose.runtime.DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { signedIn = it.currentUser != null }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (signedIn) content() else VorionSignInScreen(auth)
}

@Composable
private fun VorionSignInScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = AuthBackground) {
        Column(
            modifier = Modifier.fillMaxSize().background(AuthBackground).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("VORION AI", color = AuthGold, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text(
                if (createAccount) "إنشاء حساب جديد" else "تسجيل الدخول",
                color = AuthText,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 22.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("البريد الإلكتروني") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("كلمة المرور") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            if (error.isNotBlank()) {
                Text(error, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Button(
                onClick = {
                    error = ""
                    busy = true
                    val task = if (createAccount) {
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                    } else {
                        auth.signInWithEmailAndPassword(email.trim(), password)
                    }
                    task.addOnCompleteListener { result ->
                        busy = false
                        if (!result.isSuccessful) {
                            error = result.exception?.localizedMessage ?: "تعذر إكمال العملية."
                        }
                    }
                },
                enabled = !busy && email.contains("@") && password.length >= 6,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (busy) "يرجى الانتظار..." else if (createAccount) "إنشاء الحساب" else "دخول")
            }
            TextButton(onClick = {
                createAccount = !createAccount
                error = ""
            }) {
                Text(
                    if (createAccount) "لديك حساب؟ سجّل الدخول" else "ليس لديك حساب؟ أنشئ حساباً",
                    color = AuthGold
                )
            }
            Text(
                "تسجيل آمن عبر Firebase Authentication",
                color = AuthMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
