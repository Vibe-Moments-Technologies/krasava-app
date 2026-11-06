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
 * Поведение: дешифровки шлются в [onCodeScanned]; пауза [paused] глушит
 * выдачу (камера живёт). Позиция кода в кадре (автонаведение рамки) —
 * [onCodeBounds]: нормализованные x, y, w, h (0..1) в координатах вью;
 * null — код не виден. На iOS координаты конвертируются через
 * previewLayer (учитывает ориентацию), на Android пока не реализовано —
 * рамка остаётся в центре (заглушка, см. actual).
 */
@Composable
expect fun QrCameraView(
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    /** Задняя фонарик: true — включён (кнопка на экране сканера). */
    torchEnabled: Boolean = false,
    onCodeScanned: (String) -> Unit,
    /** Позиция кода в кадре: нормализованные x/y/w/h; null — не виден. */
    onCodeBounds: ((Float, Float, Float, Float) -> Unit)? = null
)
