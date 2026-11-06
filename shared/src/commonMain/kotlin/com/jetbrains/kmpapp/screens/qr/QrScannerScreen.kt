package com.jetbrains.kmpapp.screens.qr

import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler

/**
 * Экран сканера QR: отмечается пара на «Пульсе» — код с доски часто
 * мелкий/бликует, системная камера не справляется; наш декодер
 * (ML Kit/AVFoundation) читает тяжелее, а фонарик подсвечивает доску.
 *
 * Коды меняются на ходу (запрос владельца): подтверждения нет —
 * распознали ссылку -> сразу открыли в браузере. Пока браузер поверх,
 * анализ приостановлен (paused); возврат — мгновенное возобновление.
 * Ссылки без схемы (просто текст) открываются поиском, чтобы не
 * проглатить не-URL молча.
 */
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    // Пауза анализа после успешного скана: пока открыт браузер, камера
    // не должен дёргать открытием повторно (код всё ещё в кадре).
    var paused by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    // Гашение «дребезга»: один и тот же код в нескольких кадрах подряд
    // не должен переоткрывать ссылку.
    var lastHandled by remember { mutableStateOf<String?>(null) }

    fun handleCode(raw: String) {
        if (paused || raw == lastHandled) return
        lastHandled = raw
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
            // Открытие не удалось: даём повторить через 1.5 с — иногда
            // браузер занят или ссылка битая; молчать нельзя.
            lastHandled = null
            paused = false
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

            // Затемнение сверху и снизу: шапка и подсказки читаются на
            // любом фоне.
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
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

                Spacer(modifier = Modifier.weight(1f))

                // Подсказка + фонарик
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Наведите камеру на код — ссылка откроется сама",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Рамка-прицел: тонкий скруглённый контур по центру.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(0.dp)
            )
        }
    }
}
