package com.jetbrains.kmpapp.screens.qr

import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import kotlinx.coroutines.delay

/**
 * Сканер QR — полноценная страница-раздел (для дока).
 *
 * Единый экран: открыта как раздел дока — просто страница с шапкой
 * «Сканер QR»; открыта из шапки расписания / «Сервисов» / «Другого» —
 * с «назад» ([onBack]).
 *
 * Поиск кода идёт ПО ВСЕМУ КАДРУ камеры (не только в рамке): рамка —
 * статичный квадрат-ориентир по центру, автонаведение убрано по
 * фидбеку владельца. Антидребезг: код засчитывается после
 * [STABLE_FRAMES] стабильных кадров; ссылка открывается сразу
 * (коды «Пульса» меняются на ходу — подтверждать некогда). Пока
 * открыт браузер, анализ приостановлен; вернулся — сканируй дальше.
 */
@Composable
fun QrScannerScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) PlatformBackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    // Пауза анализа после успешного скана: пока открыт браузер, камера
    // не должна дёргать открытием повторно (код всё ещё в кадре).
    var paused by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }

    // ---- Антидребезг: счётчик стабильных кадров одного и того же кода.
    var candidateValue by remember { mutableStateOf<String?>(null) }
    var candidateStability by remember { mutableStateOf(0) }

    fun handleCode(raw: String) {
        if (paused) return
        if (raw == candidateValue) {
            candidateStability += 1
        } else {
            candidateValue = raw
            candidateStability = 1
        }
        if (candidateStability < STABLE_FRAMES) return
        // Стабильно: замораживаем и открываем.
        paused = true
        val url = raw.trim()
        runCatching {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                uriHandler.openUri(url)
            } else {
                // Не ссылка (просто текст): отдаём в поиск — лучше видно,
                // чем молча проигнорировать. Кодирование — ручное, без
                // java.net (его нет на iOS).
                val encoded = buildString {
                    url.forEach { ch ->
                        when {
                            ch.isLetterOrDigit() -> append(ch)
                            else -> {
                                ch.code.toString(16).uppercase().padStart(2, '0')
                                    .let { hex -> append('%').append(hex) }
                            }
                        }
                    }
                }
                uriHandler.openUri("https://duckduckgo.com/?q=$encoded")
            }
        }.onFailure {
            // Открытие не удалось: даём повторить — молчать нельзя.
            paused = false
            candidateValue = null
            candidateStability = 0
        }
    }

    // Возврат из браузера: разморозка через полсекунды — чтобы кадр
    // «устаканился» и код не перечитался мгновенно по старым данным.
    LaunchedEffect(paused) {
        if (paused) {
            delay(600)
            paused = false
            candidateValue = null
            candidateStability = 0
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Black,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            QrCameraView(
                paused = paused,
                torchEnabled = torch,
                onCodeScanned = { handleCode(it) },
                modifier = Modifier.fillMaxSize()
            )

            // Рамка-ориентир: статичный квадрат по центру (без
            // автонаведения — по решению владельца).
            StaticCornerSights(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Шапка страницы: слева «назад» (для подстраниц) и
                // заголовок, справа фонарик.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }
                    }
                    GlitchText(
                        text = "Сканер QR",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(start = if (onBack != null) 0.dp else 16.dp)
                            .weight(1f)
                    )
                    IconButton(onClick = { torch = !torch }) {
                        Icon(
                            imageVector = if (torch) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = if (torch) "Выключить фонарик" else "Включить фонарик",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** Сколько стабильных кадров подряд засчитывают код (антидребезг). */
private const val STABLE_FRAMES = 3

/**
 * Статичная рамка-ориентир: четыре белых уголка по центру экрана,
 * квадрат ~2/3 ширины. Только подсказка, где держать телефон: сам
 * поиск кода идёт по всему кадру камеры.
 */
@Composable
private fun StaticCornerSights(modifier: Modifier = Modifier) {
    val cornerColor = Color.White
    val cornerWidth = 4.dp
    val cornerLen = 22.dp

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.66f)
                .aspectRatio(1f)
        ) {
            val w = size.width
            val left = 0f
            val top = 0f
            val right = w
            val bottom = w

            val lenPx = cornerLen.toPx().coerceAtMost(w / 2.5f)
            val strokePx = cornerWidth.toPx()

            // Четыре уголка: каждый — два отрезка от вершины.
            val path = Path()
            // Левый верхний
            path.moveTo(left, top + lenPx)
            path.lineTo(left, top)
            path.lineTo(left + lenPx, top)
            // Правый верхний
            path.moveTo(right - lenPx, top)
            path.lineTo(right, top)
            path.lineTo(right, top + lenPx)
            // Правый нижний
            path.moveTo(right, bottom - lenPx)
            path.lineTo(right, bottom)
            path.lineTo(right - lenPx, bottom)
            // Левый нижний
            path.moveTo(left + lenPx, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, bottom - lenPx)
            drawPath(
                path,
                color = cornerColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
    }
}
