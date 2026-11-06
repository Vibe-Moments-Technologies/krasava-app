package com.jetbrains.kmpapp.screens.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
// Category-расширения Objective-C: в cinterop резолвятся ТОЛЬКО явным
// импортом — без него будет Unresolved reference даже на валидном устройстве.
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

/**
 * Камера-сканер (iOS): AVCaptureSession + QR-метаданные.
 *
 * Результаты глушатся при [paused] (как на Android): preview живёт,
 * дешифровка стопорится и мгновенно возобновляется при возврате на
 * экран сканера. Фонарик — через update при смене [torchEnabled].
 *
 * Имена cinterop-символов сверены компиляцией (konanc 2.4.10, ios_arm64)
 * и рабочими KMP-сканерами: animeko QrCodeScanner.ios.kt, KScan,
 * AndroidAPS — category-члены (torchMode, hasTorch,
 * authorizationStatusForMediaType…) требуют top-level импортов.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrCameraView(
    modifier: Modifier,
    paused: Boolean,
    torchEnabled: Boolean,
    onCodeScanned: (String) -> Unit,
    onCodeBounds: ((Float, Float, Float, Float) -> Unit)?
) {
    val currentOnScanned = rememberUpdatedState(onCodeScanned)
    val currentOnBounds = rememberUpdatedState(onCodeBounds)
    val currentPaused = rememberUpdatedState(paused)

    // AVAuthorizationStatus в Kotlin/Native — Long, не enum: сравнение
    // константами, в when нужен else.
    var authorized by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                AVAuthorizationStatusAuthorized
        )
    }
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { allowed ->
                    // Колбэк приходит на очереди системы: прыгаем в главный
                    // поток, прежде чем трогать состояние Compose.
                    dispatch_async(dispatch_get_main_queue()) { authorized = allowed }
                }
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> authorized = false
            else -> authorized = true
        }
    }

    if (!authorized) {
        // Пустая подложка: экран сканера поверх рисует своё сообщение
        // про разрешение камеры.
        androidx.compose.foundation.layout.Box(modifier = modifier)
        return
    }

    val session = remember { AVCaptureSession() }
    val holder = remember { ScannerDelegateHolder() }
    val previewLayerRef = remember { arrayOf<AVCaptureVideoPreviewLayer?>(null) }
    DisposableEffect(Unit) {
        holder.onScanned = { value ->
            if (!currentPaused.value) currentOnScanned.value(value)
        }
        holder.onBounds = { metadataObject ->
            // Нормализованные координаты через previewLayer:
            // transformedMetadataObjectForMetadataObject — метод превью-
            // слоя, конвертит bounds метаданных в координаты слоя (учитывая
            // ориентацию и AspectFill); делим на фрэйм слоя.
            // Существование метода проверено компиляцией (konanc 2.4.10,
            // ios_arm64).
            val layer = previewLayerRef[0]
            val onBoundsCallback = currentOnBounds.value
            if (layer != null && onBoundsCallback != null) {
                val transformed = layer.transformedMetadataObjectForMetadataObject(metadataObject)
                if (transformed != null) {
                    val size = layer.frame.useContents { this.size }
                    val vw = size.width.toFloat().coerceAtLeast(1f)
                    val vh = size.height.toFloat().coerceAtLeast(1f)
                    transformed.bounds.useContents {
                        onBoundsCallback(
                            origin.x.toFloat() / vw,
                            origin.y.toFloat() / vh,
                            size.width.toFloat() / vw,
                            size.height.toFloat() / vh
                        )
                    }
                }
            }
        }
        onDispose {
            holder.onScanned = null
            holder.onBounds = null
            // stopRunning блокирует: с серийной очереди, не с главного потока.
            dispatch_async(sessionQueue) { session.stopRunning() }
        }
    }

    UIKitView(
        factory = {
            session.sessionPreset = AVCaptureSessionPresetHigh
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                val input = memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    AVCaptureDeviceInput.deviceInputWithDevice(device, error = errorPtr.ptr)
                }
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
                val output = AVCaptureMetadataOutput()
                if (session.canAddOutput(output)) {
                    session.addOutput(output)
                    output.setMetadataObjectsDelegate(
                        holder.delegate, queue = dispatch_get_main_queue()
                    )
                    // Типы — строго после addOutput: до него список
                    // availableMetadataObjectTypes пуст; неподдерживаемый
                    // тип роняет процесс NSException'ом без Kotlin-стека.
                    if (output.availableMetadataObjectTypes.any { it == AVMetadataObjectTypeQRCode }) {
                        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
                    }
                }
            }
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            previewLayerRef[0] = layer
            PreviewHostView(layer).apply {
                // startRunning блокирует: не в главный поток.
                dispatch_async(sessionQueue) { session.startRunning() }
            }
        },
        update = { _ ->
            // Фонарик: блики/тусклая доска лечатся подсветкой; идемпотентно.
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                if (device.hasTorch && device.lockForConfiguration(null)) {
                    device.torchMode = if (torchEnabled) {
                        AVCaptureTorchModeOn
                    } else {
                        AVCaptureTorchModeOff
                    }
                    device.unlockForConfiguration()
                }
            }
        },
        modifier = modifier
    )
}

/** Серийная очередь сессии: старт/стоп камеры не блокируют UI-поток. */
private val sessionQueue = dispatch_queue_create("ru.vibemoments.krasava.qrscan", null)

/**
 * Хост-вью слоя превью: пересчитывает frame при каждом лэйауте (update
 * у UIKitView вызывается на смене состояния, не на лэйауте) и гасит
 * анимацию первого кадра.
 */
@OptIn(ExperimentalForeignApi::class)
private class PreviewHostView(
    private val previewLayer: AVCaptureVideoPreviewLayer
) : UIView(frame = CGRectZero.readValue()) {
    init {
        clipsToBounds = true
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        previewLayer.setFrame(bounds)
        CATransaction.commit()
    }
}

/**
 * Мост колбэка метаданных в Kotlin-лямбды. Держится сильно: output
 * удерживает делегата слабо — держатель-remember обязателен.
 */
private class ScannerDelegateHolder {
    var onScanned: ((String) -> Unit)? = null
    var onBounds: ((AVMetadataMachineReadableCodeObject) -> Unit)? = null

    // Тип — именно ПРОТОКОЛ: NSObject-поле не матчится с параметром
    // setMetadataObjectsDelegate (CI-репорт «argument type mismatch»).
    val delegate: AVCaptureMetadataOutputObjectsDelegateProtocol =
        object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
            override fun captureOutput(
                output: AVCaptureOutput,
                didOutputMetadataObjects: List<*>,
                fromConnection: AVCaptureConnection
            ) {
                if (onScanned == null && onBounds == null) return
                for (found in didOutputMetadataObjects) {
                    val code = found as? AVMetadataMachineReadableCodeObject ?: continue
                    code.stringValue?.let { value -> onScanned?.invoke(value) }
                    onBounds?.invoke(code)
                }
            }
        }
}
