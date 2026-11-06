package com.jetbrains.kmpapp.screens.qr

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/**
 * Камера-сканер (Android): CameraX Preview + ML Kit Barcode Scanning.
 *
 * Пауза ([paused]) — внутри analyzer-колбэка через rememberUpdatedState:
 * preview и сессия живут, результаты глушатся; возврат на экран сканера
 * возобновляет дешифровку без пересборки камеры.
 *
 * Автонаведение рамки: ML Kit даёт boundingBox кода в пикселях кадра;
 * переводим в нормализованные координаты вью (FILL_CENTER: кадр
 * масштабируется до покрытия, лишнее кропится по центру) и шлём в
 * [onCodeBounds] через main-поток. Нет кода в кадре — шлём null.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
actual fun QrCameraView(
    modifier: Modifier,
    paused: Boolean,
    torchEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    onCodeBounds: ((Float, Float, Float, Float) -> Unit)?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    // Analyzer живёт в camera-потоке: колбэки обязаны читаться через
    // rememberUpdatedState, чтобы всегда видеть актуальные значения.
    val currentOnScanned by rememberUpdatedState(onCodeScanned)
    val currentOnBounds by rememberUpdatedState(onCodeBounds)
    val currentPaused by rememberUpdatedState(paused)

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    if (!hasCameraPermission) {
        androidx.compose.foundation.layout.Box(modifier = modifier)
        return
    }

    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    // Размеры вью для нормализации координат (обновляются в update).
    var viewSizeRef by remember { mutableStateOf(1 to 1) }
    // Ссылка на previewView: post() из analyzer-потока в main.
    val previewViewRef = remember { arrayOf<PreviewView?>(null) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            previewViewRef[0] = previewView
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                // Нормализация бокса кода в координаты вью (FILL_CENTER).
                // Размеры кадра берём прямо из imageProxy (sensor-кадр),
                // rotationDegrees — из imageInfo.
                fun reportBounds(box: Rect?, frameW: Int, frameH: Int, rotation: Int) {
                    val pv = previewViewRef[0] ?: return
                    val callback = currentOnBounds ?: return
                    if (box == null || frameW <= 0 || frameH <= 0) {
                        pv.post { callback(-1f, -1f, -1f, -1f) }
                        return
                    }
                    // Экранная система: кадр повёрнут к ориентации вью.
                    val rotated = rotation == 90 || rotation == 270
                    val fw = if (rotated) frameH else frameW
                    val fh = if (rotated) frameW else frameH
                    // boundingBox ML Kit — в координатах повёрнутого кадра,
                    // переводим в sensor-систему при 90/270.
                    val bx: Int; val by: Int; val bw: Int; val bh: Int
                    if (rotated) {
                        bx = box.top; by = box.left; bw = box.height(); bh = box.width()
                    } else {
                        bx = box.left; by = box.top; bw = box.width(); bh = box.height()
                    }
                    pv.post {
                        val vw = pv.width.coerceAtLeast(1)
                        val vh = pv.height.coerceAtLeast(1)
                        val scale = maxOf(vw.toFloat() / fw, vh.toFloat() / fh)
                        val dx = (vw - fw * scale) / 2f
                        val dy = (vh - fh * scale) / 2f
                        callback(
                            (bx * scale + dx) / vw,
                            (by * scale + dy) / vh,
                            (bw * scale) / vw,
                            (bh * scale) / vh
                        )
                    }
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    if (mediaImage != null) {
                        val input = InputImage.fromMediaImage(mediaImage, rotation)
                        scanner.process(input)
                            .addOnSuccessListener { barcodes ->
                                val qr = barcodes.firstOrNull { it.rawValue != null }
                                if (qr != null) {
                                    reportBounds(
                                        qr.boundingBox, imageProxy.width, imageProxy.height, rotation
                                    )
                                    if (!currentPaused) {
                                        qr.rawValue?.let { currentOnScanned(it) }
                                    }
                                } else {
                                    reportBounds(null, imageProxy.width, imageProxy.height, rotation)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    provider.unbindAll()
                    cameraRef = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                    // Камера занята/недоступна: экран сканера покажет своё
                    // сообщение поверх пустого preview.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        update = {
            cameraRef?.cameraControl?.enableTorch(torchEnabled)
            viewSizeRef = it.width to it.height
        },
        onRelease = {
            previewViewRef[0] = null
        },
        modifier = modifier
    )
}
