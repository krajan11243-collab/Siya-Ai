package com.siya.ai.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

private val HoloBlue = Color(0xFF16BFFF)
private val HoloCyan = Color(0xFF22E6FF)
private val HoloPurple = Color(0xFF9B5CFF)
private val HoloPink = Color(0xFFD85CFF)

@Composable
fun ProHologram(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "siya_pro_hologram")
    val rotation by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(10500, easing = FastOutSlowInEasing)),
        label = "orbit_rotation"
    )
    val reverseRotation by transition.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(14500, easing = FastOutSlowInEasing)),
        label = "reverse_rotation"
    )
    val pulse by transition.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(if (active) 850 else 1500), RepeatMode.Reverse),
        label = "core_pulse"
    )
    val beam by transition.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beam"
    )

    Canvas(modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.48f
        val base = minOf(size.width * 0.39f, size.height * 0.31f)
        val energy = if (active) 1f else 0.72f

        // Deep atmospheric glow.
        drawCircle(
            Brush.radialGradient(
                listOf(
                    HoloCyan.copy(alpha = 0.24f * energy),
                    HoloBlue.copy(alpha = 0.12f * energy),
                    HoloPurple.copy(alpha = 0.05f),
                    Color.Transparent
                )
            ),
            base * 1.85f,
            Offset(cx, cy)
        )

        // Vertical energy column and scan beam.
        val beamX = cx + beam * base * 0.82f
        drawLine(HoloBlue.copy(alpha = .18f), Offset(cx, cy - base * 1.65f), Offset(cx, cy + base * 1.65f), 1.2f)
        drawLine(HoloCyan.copy(alpha = .32f), Offset(beamX, cy - base * 1.48f), Offset(beamX, cy + base * 1.48f), 2f)
        drawLine(HoloPurple.copy(alpha = .18f), Offset(cx - base * 1.55f, cy), Offset(cx + base * 1.55f, cy), 1.3f)

        // Large 3D-looking orbital system.
        rotate(rotation, Offset(cx, cy)) {
            drawOval(HoloCyan.copy(alpha = .76f * energy), Offset(cx - base * 1.55f, cy - base * .28f), Size(base * 3.1f, base * .56f), style = Stroke(2.4f))
            drawOval(HoloPurple.copy(alpha = .68f), Offset(cx - base * 1.38f, cy - base * .55f), Size(base * 2.76f, base * 1.10f), style = Stroke(1.5f))
            drawOval(HoloBlue.copy(alpha = .48f), Offset(cx - base * 1.16f, cy - base * .83f), Size(base * 2.32f, base * 1.66f), style = Stroke(1.2f))
            drawArc(HoloPink.copy(alpha = .9f), 18f, 102f, false, Offset(cx - base, cy - base), Size(base * 2f, base * 2f), style = Stroke(6f, cap = StrokeCap.Round))
            drawArc(HoloCyan.copy(alpha = .9f), 198f, 116f, false, Offset(cx - base, cy - base), Size(base * 2f, base * 2f), style = Stroke(6f, cap = StrokeCap.Round))
            drawArc(HoloBlue.copy(alpha = .7f), 295f, 48f, false, Offset(cx - base * 1.13f, cy - base * 1.13f), Size(base * 2.26f, base * 2.26f), style = Stroke(3f, cap = StrokeCap.Round))
        }

        rotate(reverseRotation, Offset(cx, cy)) {
            drawOval(HoloBlue.copy(alpha = .34f), Offset(cx - base * 1.18f, cy - base * .92f), Size(base * 2.36f, base * 1.84f), style = Stroke(1.2f))
            drawOval(HoloPurple.copy(alpha = .42f), Offset(cx - base * 1.48f, cy - base * .40f), Size(base * 2.96f, base * .80f), style = Stroke(1.1f))
        }

        // Particle field. Deterministic positions avoid allocations while animating.
        for (i in 0 until 110) {
            val angle = Math.toRadians((i * 137.50776) + rotation * (0.10 + (i % 5) * 0.025))
            val radius = base * (0.48f + ((i * 29) % 100) / 100f * 1.12f)
            val x = cx + cos(angle).toFloat() * radius
            val y = cy + sin(angle).toFloat() * radius * (0.62f + (i % 4) * .10f)
            val alpha = (0.20f + (i % 7) * .09f).coerceAtMost(.82f) * energy
            val r = if (i % 17 == 0) 3.2f else if (i % 5 == 0) 2.1f else 1.25f
            drawCircle(if (i % 3 == 0) HoloCyan.copy(alpha = alpha) else HoloPurple.copy(alpha = alpha), r, Offset(x, y))
        }

        // Central glass/energy core, intentionally no text inside.
        val core = base * pulse * .92f
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(.22f), HoloCyan.copy(.16f), Color.Transparent)), core * .72f, Offset(cx, cy))
        drawCircle(HoloBlue.copy(.16f), core, Offset(cx, cy), style = Stroke(2f))
        drawCircle(HoloPurple.copy(.36f), core * .72f, Offset(cx, cy), style = Stroke(1.4f))
        drawCircle(HoloCyan.copy(.30f), core * .48f, Offset(cx, cy), style = Stroke(1.2f))
        drawCircle(Color.White.copy(.90f), 5f, Offset(cx, cy))
        drawCircle(HoloCyan.copy(.80f), 12f, Offset(cx, cy), style = Stroke(2f))

        // Holographic floor / emitter platform.
        val platformY = cy + base * 1.46f
        drawOval(HoloBlue.copy(.18f), Offset(cx - base * 1.52f, platformY - base * .12f), Size(base * 3.04f, base * .34f), style = Stroke(2f))
        drawOval(HoloPurple.copy(.48f), Offset(cx - base * 1.34f, platformY - base * .08f), Size(base * 2.68f, base * .24f), style = Stroke(3f))
        drawOval(HoloCyan.copy(.50f), Offset(cx - base * 1.05f, platformY - base * .045f), Size(base * 2.10f, base * .15f), style = Stroke(2f))
        drawLine(HoloCyan.copy(.22f), Offset(cx, cy + base * .55f), Offset(cx, platformY), 1.5f)
    }
}
