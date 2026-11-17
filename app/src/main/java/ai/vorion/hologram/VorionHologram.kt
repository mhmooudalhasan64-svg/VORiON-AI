package ai.vorion.hologram

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Gold = Color(0xFFE0BD70)
private val GoldBright = Color(0xFFFFE7A8)
private val Void = Color(0xFF080808)

/**
 * A visual-only holographic presence. It does not request microphone permission
 * or listen in the background. Feed normalized audio amplitude (0f..1f) from an
 * explicitly started audio session to animate the response while speaking.
 */
@Composable
fun VorionHologram(
    state: HologramState,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    audioLevel: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "vorion-hologram")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == HologramState.THINKING) 2200 else 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "orbital-phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "core-pulse"
    )

    val level = audioLevel.coerceIn(0f, 1f)
    val accent = when (state) {
        HologramState.LISTENING -> Color(0xFFFFD98A)
        HologramState.THINKING -> Color(0xFFC7A7FF)
        HologramState.SPEAKING -> Color(0xFFFFE7A8)
        HologramState.EXECUTING -> Color(0xFF9ED8D0)
        HologramState.SUCCESS -> Color(0xFF9AD6A0)
        HologramState.WARNING -> Color(0xFFFFB45C)
        HologramState.ERROR -> Color(0xFFFF7777)
        HologramState.IDLE -> Gold
    }

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val unit = this.size.minDimension
        val dynamic = 1f + level * 0.12f
        val coreRadius = unit * 0.105f * pulse * dynamic

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.07f), Color.Transparent),
                center = center,
                radius = unit * 0.43f
            ),
            radius = unit * 0.43f,
            center = center
        )

        // Three orbital rings with state-dependent rotation and subtle depth.
        for (index in 0..2) {
            val radius = unit * (0.22f + index * 0.075f)
            drawCircle(
                color = accent.copy(alpha = 0.18f + index * 0.035f),
                radius = radius,
                center = center,
                style = Stroke(width = unit * 0.0028f)
            )
            val angle = phase * (if (index == 1) -0.72f else 0.45f + index * 0.18f) + index
            val orbitPoint = Offset(
                center.x + cos(angle) * radius,
                center.y + sin(angle) * radius * 0.42f
            )
            drawCircle(color = accent.copy(alpha = 0.8f), radius = unit * 0.012f, center = orbitPoint)
        }

        // Fine holographic meridians.
        for (index in -2..2) {
            val y = center.y + index * unit * 0.035f
            drawLine(
                color = accent.copy(alpha = 0.12f),
                start = Offset(center.x - unit * 0.17f, y),
                end = Offset(center.x + unit * 0.17f, y),
                strokeWidth = unit * 0.0018f,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, GoldBright, accent, Void),
                center = Offset(center.x - coreRadius * 0.25f, center.y - coreRadius * 0.3f),
                radius = coreRadius * 1.8f
            ),
            radius = coreRadius,
            center = center
        )
        drawCircle(color = accent.copy(alpha = 0.65f), radius = coreRadius * 1.55f, center = center, style = Stroke(width = unit * 0.003f))
    }
}
