package com.jetbrains.kmpapp.screens.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Сканер QR-кодов — вид живой камеры.
 *
 * Платформенная реализация:
 *  - Android: CameraX PreviewView + ML Kit Barcode Scanning (bundled).
 *  - iOS: AVFoundation AVCaptureSession + метаданные QR.
 *
 * Поиск кода идёт ПО ВСЕМУ КАДРУ камеры, рамка на экране — только
 * визуальный ориентир (где держать телефон). Дешифровки шлются в
 * [onCodeScanned]; пауза [paused] глушит выдачу (камера живёт).
 */
@Composable
expect fun QrCameraView(
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    /** Задняя фонарик: true — включён (кнопка в шапке сканера). */
    torchEnabled: Boolean = false,
    onCodeScanned: (String) -> Unit
)
