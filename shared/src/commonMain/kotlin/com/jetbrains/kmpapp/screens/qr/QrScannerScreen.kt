package com.jetbrains.kmpapp.screens.qr

import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler

/**
 * Сканер QR — полноценная страница-раздел (для дока).
 *
 * Единый экран: открыта как раздел дока — просто страница с шапкой
 * «Сканер QR»; открыта из шапки расписания / «Сервисов» / «Другого» —
 * с «назад» ([onBack]).
 *
 * Поиск кода идёт по всему кадру, рамка — статичный ориентир.
 * Распознавание НЕПРЕРЫВНОЕ: код сменился/камера наведена на другой —
 * работаем с новым, не зависаем на предыдущем.
 *
 * Доменный барьер (жёсткий): в браузер автоматически уходит только
 * ссылка с доменом *.mirea.ru (miro-поддомены mirea.ru считаются
 * своими). Всё остальное — мини-виджет домена под рамкой; тап по
 * нему открывает предупреждение с полной ссылкой и кнопкой перехода
 * (ссылка не связана с МИРЭА — открыть может только сам студент).
 * Фонарика нет: в аудитории вспышкой не пользуются, а блики она
 * только усиливает (решение владельца).
 */
@Composable
fun QrScannerScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBack != null) PlatformBackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    // Код, распознанный прямо сейчас (обновляется каждый кадр —
    // непрерывное распознавание, без фиксации на первом).
    var currentCode by remember { mutableStateOf<String?>(null) }
    // Показываем предупреждение по тапу на мини-виджет.
    var showForeignDialog by remember { mutableStateOf(false) }

    fun open(url: String) {
        uriHandler.openUri(url)
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
                paused = false,
                onCodeScanned = { raw ->
                    // Непрерывно: каждый распознанный кадр — текущий код.
                    // Антидребезг не нужен: критичное действие (переход)
                    // всё равно за доменным барьером.
                    val url = raw.trim()
                    if (url != currentCode) {
                        currentCode = url
                    }
                    if (isMireaUrl(url)) {
                        // Домен МИРЭА: открываем сразу, без подтверждений
                        // (коды «Пульса» меняются на ходу).
                        open(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Рамка-ориентир: статичный квадрат по центру.
            StaticCornerSights(modifier = Modifier.fillMaxSize())

            // Мини-виджет распознанного домена — под рамкой: только для
            // ссылок НЕ из mirea.ru (свои открываются сами).
            val foreign = currentCode?.let { code ->
                extractUrl(code)?.takeIf { !isMireaUrl(it) }
            }
            if (foreign != null) {
                DomainMiniBadge(
                    url = foreign,
                    onClick = { showForeignDialog = true },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 220.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Шапка страницы: слева «назад» (для подстраниц) и
                // заголовок.
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
                        modifier = Modifier.padding(start = if (onBack != null) 0.dp else 16.dp)
                    )
                }
            }
        }
    }

    // Предупреждение для чужого домена: полная ссылка, кнопка перехода
    // и явное «не связано с МИРЭА».
    val foreignUrl = currentCode?.let { extractUrl(it) }?.takeIf { !isMireaUrl(it) }
    if (showForeignDialog && foreignUrl != null) {
        AlertDialog(
            onDismissRequest = { showForeignDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                GlitchText(
                    text = "Переход по ссылке",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    GlitchText(
                        text = "Распознанная ссылка не связана с МИРЭА:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    GlitchText(
                        text = foreignUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    GlitchText(
                        text = "Открывайте её, только если доверяете источнику кода.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForeignDialog = false
                        uriHandler.openUri(foreignUrl)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    GlitchText("Перейти", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForeignDialog = false }) {
                    GlitchText("Отмена")
                }
            }
        )
    }
}

/**
 * Жёсткий доменный барьер: только *.mirea.ru (включая сам mirea.ru)
 * открывается автоматически. http/https обязателен — голый текст
 * не пропускаем никогда.
 */
private fun isMireaUrl(raw: String): Boolean {
    val url = extractUrl(raw) ?: return false
    val lower = url.lowercase()
    if (!lower.startsWith("https://") && !lower.startsWith("http://")) return false
    // Хост — между схемой и первым /, ? или #.
    val rest = lower.substringAfter("://")
    val host = rest.substringBefore('/').substringBefore('?').substringBefore('#')
    // Хост-суффикс: «mirea.ru» или «*.mirea.ru». Точку перед mirea
    // требуем явно, чтобы «notmirea.ru» не прошло.
    return host == "mirea.ru" || host.endsWith(".mirea.ru")
}

/** Вытащить URL из распознанного текста (null — это не ссылка). */
private fun extractUrl(raw: String): String? {
    val trimmed = raw.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else null
}

/**
 * Мини-виджет распознанного домена — в стиле UpdateMiniBadge из
 * «Другого»: капсула с иконкой и доменом. Тёмная, подкладка под
 * чёрным фоном камеры, тап — предупреждение.
 */
@Composable
private fun DomainMiniBadge(
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val host = url.lowercase()
        .substringAfter("://")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xEE1C1B20))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.TravelExplore,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        GlitchText(
            text = host,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
