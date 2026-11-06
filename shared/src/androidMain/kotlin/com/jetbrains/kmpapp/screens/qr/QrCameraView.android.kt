package com.jetbrains.kmpapp.screens.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
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
 * Поиск кода — по ВСЕМУ КАДРУ (баркоды ищутся во всём входном
 * изображении, не в зоне рамки). Пауза ([paused]) — внутри
 * analyzer-колбэка через rememberUpdatedState: preview и сессия
 * живут, результаты глушатся; возврат на экран сканера возобновляет
 * дешифровку без пересборки камеры.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
actual fun QrCameraView(
    modifier: Modifier,
    paused: Boolean,
    torchEnabled: Boolean,
    onCodeScanned: (String) -> Unit
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
    // Analyzer живёт в camera-потоке: колбэк обязан читаться через
    // rememberUpdatedState, чтобы всегда видеть актуальное значение.
    val currentOnScanned by rememberUpdatedState(onCodeScanned)
    val currentPaused by rememberUpdatedState(paused)

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    if (!hasCameraPermission) {
        // Пустая подложка: экран сканера поверх рисует своё сообщение
        // про разрешение камеры.
        androidx.compose.foundation.layout.Box(modifier = modifier)
        return
    }

    var cameraRef by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
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

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val input = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(input)
                            .addOnSuccessListener { barcodes ->
                                if (!currentPaused) {
                                    // Первый QR с содержимым: весь кадр
                                    // уже проанализирован декодером.
                                    barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                        ?.let { currentOnScanned(it) }
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
        },
        modifier = modifier
    )
}
