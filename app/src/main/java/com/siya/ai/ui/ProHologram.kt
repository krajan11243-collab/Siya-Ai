package com.siya.ai.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

private val HoloBlue = Color(0xFF159BFF)
private val HoloCyan = Color(0xFF21E6FF)
private val HoloPurple = Color(0xFFC05CFF)

/** Code-rendered holographic orb tuned to the supplied Siya Ai mobile reference. */
@Composable
fun ProHologram(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "siya-hologram")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "rotation")
    val pulse by transition.animateFloat(.92f, 1.04f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse")
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val orbR = min(w * .31f, h * .30f).coerceAtLeast(70f)
        val cy = h * .47f
        val platformY = (cy + orbR * 1.30f).coerceAtMost(h * .78f)
        val glowR = orbR * pulse * if (active) 1.07f else 1f

        drawCircle(brush = Brush.radialGradient(listOf(HoloCyan.copy(.20f), HoloBlue.copy(.08f), Color.Transparent), center = Offset(cx, cy), radius = glowR * 1.65f), radius = glowR * 1.65f, center = Offset(cx, cy))
        drawCircle(brush = Brush.radialGradient(listOf(HoloBlue.copy(.30f), Color.Transparent), center = Offset(cx, cy), radius = glowR), radius = glowR, center = Offset(cx, cy))

        for (i in 0..8) {
            val y = cy - orbR * .72f + i * orbR * .18f
            drawLine(HoloBlue.copy(alpha = .10f), Offset(cx - orbR * .95f, y), Offset(cx + orbR * .95f, y), strokeWidth = 1f)
        }
        for (i in -5..5) {
            val x = cx + i * orbR * .16f
            drawLine(HoloCyan.copy(alpha = .09f), Offset(x, cy - orbR * 1.1f), Offset(x, cy + orbR * 1.1f), strokeWidth = 1f)
        }

        drawCircle(brush = Brush.radialGradient(listOf(Color(0xFF082B62), Color(0xFF031225), Color.Transparent), center = Offset(cx, cy), radius = orbR), radius = orbR, center = Offset(cx, cy))
        drawCircle(color = HoloCyan, radius = orbR * .93f, center = Offset(cx, cy), style = Stroke(width = 5f))
        drawCircle(color = HoloBlue.copy(.75f), radius = orbR * .78f, center = Offset(cx, cy), style = Stroke(width = 2f))
        drawCircle(color = HoloPurple.copy(.60f), radius = orbR * .60f, center = Offset(cx, cy), style = Stroke(width = 2f))

        drawOval(
            brush = Brush.sweepGradient(listOf(HoloPurple, HoloCyan, HoloBlue, HoloPurple)),
            topLeft = Offset(cx - orbR * 1.16f, cy - orbR * .28f),
            size = Size(orbR * 2.32f, orbR * .56f),
            style = Stroke(width = 2f),
        )
        drawOval(
            color = HoloCyan.copy(.40f),
            topLeft = Offset(cx - orbR * 1.03f, cy - orbR * .67f),
            size = Size(orbR * 2.06f, orbR * 1.34f),
            style = Stroke(width = 1.2f),
        )
        drawArc(
            color = HoloCyan,
            startAngle = rotation,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = Offset(cx - orbR * 1.12f, cy - orbR * 1.12f),
            size = Size(orbR * 2.24f, orbR * 2.24f),
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )
        drawArc(
            color = HoloPurple,
            startAngle = rotation + 180f,
            sweepAngle = 65f,
            useCenter = false,
            topLeft = Offset(cx - orbR * 1.12f, cy - orbR * 1.12f),
            size = Size(orbR * 2.24f, orbR * 2.24f),
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )

        drawCircle(HoloCyan, radius = 4.5f, center = Offset(cx, cy))
        drawLine(HoloCyan.copy(.55f), Offset(cx, cy - orbR * 1.55f), Offset(cx, cy - orbR * 1.02f), strokeWidth = 2f)
        drawLine(HoloCyan.copy(.55f), Offset(cx, cy + orbR * 1.02f), Offset(cx, platformY - 6f), strokeWidth = 2f)
        for (i in -2..2) drawLine(HoloBlue.copy(.20f), Offset(cx + i * orbR * .18f, cy + orbR), Offset(cx + i * orbR * .18f, platformY), strokeWidth = 1f)

        drawOval(
            brush = Brush.sweepGradient(listOf(HoloBlue, HoloPurple, HoloCyan, HoloBlue)),
            topLeft = Offset(cx - orbR * 1.55f, platformY - orbR * .14f),
            size = Size(orbR * 3.10f, orbR * .28f),
            style = Stroke(width = 3f),
        )
        drawOval(
            color = HoloCyan.copy(.55f),
            topLeft = Offset(cx - orbR * 1.30f, platformY - orbR * .07f),
            size = Size(orbR * 2.60f, orbR * .14f),
            style = Stroke(width = 2f),
        )
        drawLine(HoloBlue.copy(.18f), Offset(cx - orbR * 1.5f, platformY), Offset(cx + orbR * 1.5f, platformY), strokeWidth = orbR * .08f)
    }
}
