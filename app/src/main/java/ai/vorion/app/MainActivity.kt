package ai.vorion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.vorion.hologram.HologramState
import ai.vorion.hologram.VorionHologram

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VorionApp() }
    }
}

@Composable
private fun VorionApp() {
    val colors = darkColorScheme(
        primary = Color(0xFFD7B56D),
        background = Color(0xFF070707),
        surface = Color(0xFF101010)
    )
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070707)) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF070707)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("VORION AI", color = Color(0xFFE0C27A), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("INTELLIGENCE INTERFACE", color = Color(0xFF8C877C), fontSize = 10.sp, letterSpacing = 3.sp)
                VorionHologram(
                    state = HologramState.IDLE,
                    modifier = Modifier.padding(top = 28.dp),
                    size = 280.dp
                )
                Text("أنا جاهز للمساعدة", color = Color(0xFFE5DCC8), fontSize = 16.sp)
                Text("الإصدار التأسيسي 0.1.0", color = Color(0xFF77736B), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
