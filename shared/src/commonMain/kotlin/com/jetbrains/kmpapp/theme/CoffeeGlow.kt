package com.jetbrains.kmpapp.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun CoffeeGlow(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "coffee-glow")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coffee-glow-pulse"
    )
    val glow = if (dark) Color(0xFFFFC07A) else Color(0xFFB26A3C)
    val alpha = if (dark) 0.05f + pulse * 0.035f else 0.04f + pulse * 0.03f
    Canvas(modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas
        val leftCenter = Offset(width * 0.08f, height * 0.92f)
        val leftRadius = width * 0.9f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = alpha), Color.Transparent),
                center = leftCenter,
                radius = leftRadius
            ),
            radius = leftRadius,
            center = leftCenter
        )
        val rightCenter = Offset(width * 0.94f, height * 0.78f)
        val rightRadius = width * 0.72f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = alpha * 0.8f), Color.Transparent),
                center = rightCenter,
                radius = rightRadius
            ),
            radius = rightRadius,
            center = rightCenter
        )
    }
}
