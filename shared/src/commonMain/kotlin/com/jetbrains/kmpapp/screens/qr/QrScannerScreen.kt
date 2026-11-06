package com.jetbrains.kmpapp.screens.qr

import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.FilledIconButton
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
 * Экран сканера QR: отмечается пара на «Пульсе» — код с доски часто
 * мелкий/бликует, системная камера не справляется; наш декодер читает
 * тяжелее, а фонарик подсвечивает доску.
 *
 * Рамка-прицел — не фиксированный квадрат, а четыре уголка с
 * АВТОНАВЕДЕНИЕМ: следят за кодом в кадре (координаты от платформы),
 * при захвате «замораживаются» и подсвечиваются, дрожание сглаживается
 * анимацией. Антидребезг: код засчитывается, когда распознан
 * стабильно (порог [STABLE_FRAMES] подряд), — только после этого
 * открывается ссылка (сразу, без подтверждения: коды меняются на ходу).
 *
 * [embedded] — режим из шапки расписания: экран живёт внутри страницы
 * расписания (шапка расписания остаётся сверху), «назад» не нужен.
 */
@Composable
fun QrScannerScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
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

    // ---- Автонаведение рамки (нормализованные x/y/w/h, -1 — кода нет).
    var targetBounds by remember { mutableStateOf<FloatArray?>(null) }
    // Плавная рамка: к target через анимацию (глушит дрожание руки).
    val animX by animateFloatAsState(targetBounds?.get(0) ?: 0.5f, tween(120))
    val animY by animateFloatAsState(targetBounds?.get(1) ?: 0.4f, tween(120))
    val animW by animateFloatAsState(targetBounds?.get(2) ?: 0.42f, tween(120))
    val animH by animateFloatAsState(targetBounds?.get(3) ?: 0.42f, tween(120))
    // Захват: код стабильно распознан — рамка замирает и зеленеет.
    var locked by remember { mutableStateOf(false) }

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
        locked = true
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
            locked = false
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
            locked = false
            paused = false
            candidateValue = null
            candidateStability = 0
        }
    }

    // Код пропал из кадра (платформа шлёт -1/-1/-1/-1) — сбрасываем цель,
    // рамка мягко возвращается к исходной позиции.
    fun onBounds(x: Float, y: Float, w: Float, h: Float) {
        if (x < 0f) {
            targetBounds = null
        } else {
            targetBounds = floatArrayOf(x, y, w, h)
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
                onCodeBounds = { x, y, w, h -> onBounds(x, y, w, h) },
                modifier = Modifier.fillMaxSize()
            )

            // Рамка-прицел: четыре уголка, следящие за кодом.
            Box(modifier = Modifier.fillMaxSize()) {
                CornerSights(
                    x01 = animX, y01 = animY, w01 = animW, h01 = animH,
                    locked = locked,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                if (!embedded && onBack != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        GlitchText(
                            text = "Сканер QR",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Подсказка + фонарик: подняты над доком (запрос владельца:
                // «кнопка молнии прилипла к доку, надпись уходит под док»).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    FilledIconButton(
                        onClick = { torch = !torch },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (torch) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = if (torch) "Выключить фонарик" else "Включить фонарик",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    GlitchText(
                        text = if (locked) {
                            "Код распознан — открываю…"
                        } else {
                            "Наведите камеру на код — рамка сама найдёт его"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

/** Сколько стабильных кадров подряд засчитывают код (антидребезг). */
private const val STABLE_FRAMES = 3

/**
 * Четыре уголка-прицела вокруг (x01, y01, w01, h01) — нормализованных
 * координат кода. [locked]: зелёные, толще, с лёгким свечением.
 */
@Composable
private fun CornerSights(
    x01: Float,
    y01: Float,
    w01: Float,
    h01: Float,
    locked: Boolean,
    modifier: Modifier = Modifier
) {
    val cornerColor = if (locked) Color(0xFF34D399) else Color.White
    val cornerAlpha by animateFloatAsState(if (locked) 1f else 0.9f, tween(120))
    val cornerWidth = 4.dp
    val cornerLen = 22.dp

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Полу-прозрачное поле чуть больше рамки — «зона поиска».
        val rectW = w01 * w
        val rectH = h01 * h
        val left = (x01 * w).coerceIn(0f, w - rectW)
        val top = (y01 * h).coerceIn(0f, h - rectH)
        val right = (left + rectW).coerceAtMost(w)
        val bottom = (top + rectH).coerceAtMost(h)

        val lenPx = cornerLen.toPx()
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
            color = cornerColor.copy(alpha = cornerAlpha),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}
