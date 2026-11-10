package com.jetbrains.kmpapp.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Медленный снегопад поверх интерфейса в новогодней теме.
 *
 * Три слоя глубины дают параллакс: дальние flakes мелкие, тусклые и плывут
 * медленно, передние — крупнее и быстрее. Лёгкое покачивание по синусу
 * убирает ощущение «падающих точек». Несколько крупных снежинок рисуются
 * четырёхлучевыми звёздочками, остальные — мягкими круглыми хлопьями.
 *
 * Canvas без pointerInput не перехватывает жесты: под снегом можно листать
 * расписание и нажимать кнопки. Цвет хлопьев зависит от светлоты фона:
 * на снежной светлой теме белый был бы невидим, поэтому там работает
 * голубовато-серебристый.
 */
@Composable
fun NewYearSnowfall(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val flakes = remember { createFlakes() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now = it }
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val core = if (darkTheme) Color(0xFFF2FBFF) else Color(0xFF5E8FB8)
        val halo = if (darkTheme) Color(0xFF9BD8FF) else Color(0xFFB9D8EE)
        val seconds = now / 1_000_000_000.0
        val alphaScale = if (darkTheme) 1f else 0.78f

        for (flake in flakes) {
            val y = ((flake.y + flake.speed * seconds) % 1.08).toFloat()
            val x = flake.x +
                sin((seconds * flake.swaySpeed + flake.swayPhase).toFloat()) * flake.sway
            val px = x * w
            val py = (y - 0.04f) * h
            val radius = flake.radius * density
            if (px < -radius * 4f || px > w + radius * 4f ||
                py < -radius * 4f || py > h + radius * 4f
            ) {
                continue
            }

            val alpha = (flake.alpha * alphaScale).coerceIn(0f, 1f)
            drawCircle(
                color = halo.copy(alpha = alpha * 0.32f),
                radius = radius * 2.3f,
                center = Offset(px, py)
            )
            drawCircle(
                color = core.copy(alpha = alpha),
                radius = radius,
                center = Offset(px, py)
            )

            if (flake.sparkle) {
                val arm = radius * 2.9f
                val stroke = max(0.7f * density, radius * 0.34f)
                val lineColor = core.copy(alpha = alpha * 0.72f)
                drawLine(
                    color = lineColor,
                    start = Offset(px - arm, py),
                    end = Offset(px + arm, py),
                    strokeWidth = stroke
                )
                drawLine(
                    color = lineColor,
                    start = Offset(px, py - arm),
                    end = Offset(px, py + arm),
                    strokeWidth = stroke
                )
            }
        }
    }
}

private data class NewYearFlake(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val sway: Float,
    val swayPhase: Float,
    val swaySpeed: Float,
    val alpha: Float,
    val sparkle: Boolean
)

private fun createFlakes(): List<NewYearFlake> {
    val random = Random(20261231)
    return List(64) {
        val depthRoll = random.nextFloat()
        val depth = when {
            depthRoll < 0.22f -> 0
            depthRoll < 0.7f -> 1
            else -> 2
        }
        val radius = when (depth) {
            0 -> 0.9f + random.nextFloat() * 0.7f
            1 -> 1.6f + random.nextFloat() * 1.0f
            else -> 2.4f + random.nextFloat() * 1.4f
        }
        val speed = when (depth) {
            0 -> 0.006f + random.nextFloat() * 0.006f
            1 -> 0.014f + random.nextFloat() * 0.010f
            else -> 0.026f + random.nextFloat() * 0.016f
        }
        val sway = when (depth) {
            0 -> 0.006f + random.nextFloat() * 0.012f
            1 -> 0.012f + random.nextFloat() * 0.016f
            else -> 0.018f + random.nextFloat() * 0.022f
        }
        val alpha = when (depth) {
            0 -> 0.18f + random.nextFloat() * 0.12f
            1 -> 0.28f + random.nextFloat() * 0.18f
            else -> 0.45f + random.nextFloat() * 0.27f
        }
        NewYearFlake(
            x = random.nextFloat() * 1.06f - 0.03f,
            y = random.nextFloat() * 1.08f - 0.08f,
            radius = radius,
            speed = speed,
            sway = sway,
            swayPhase = random.nextFloat() * 6.2831855f,
            swaySpeed = 0.15f + random.nextFloat() * 0.4f,
            alpha = alpha,
            sparkle = depth == 2 && random.nextFloat() < 0.28f
        )
    }
}
