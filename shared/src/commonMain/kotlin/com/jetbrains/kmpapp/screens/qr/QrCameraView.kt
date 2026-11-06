package com.jetbrains.kmpapp.screens.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Сканер QR-кодов — вид живой камеры с рамкой-прицелом.
 *
 * Платформенная реализация:
 *  - Android: CameraX PreviewView + ML Kit Barcode Scanning (GMS-free,
 *    bundled) — выбор сильного распознавания в тяжёлых условиях
 *    (блики, мелкий код, плохой свет; запрос владельца).
 *  - iOS: AVFoundation AVCaptureSession + метаданные QR.
 *
 * Поведение: одна успешная дешифровка -> [onCodeScanned] и пауза камеры
 * (управляется вызовом [paused]); перезапуск — снятием паузы. Коды
 * меняются на ходу (запрос владельца), поэтому экран сканера НЕ просит
 * подтверждения: распознали -> открыли ссылку.
 */
@Composable
expect fun QrCameraView(
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    /** Задняя фонарик: true — включён (кнопка на экране сканера). */
    torchEnabled: Boolean = false,
    onCodeScanned: (String) -> Unit
)
