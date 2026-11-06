package com.jetbrains.kmpapp.screens.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
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
 * Паттерн сессии подсмотрен у рабочих KMP-сканеров (AndroidAPS):
 * startRunning блокирует и уходит с главного потока; делегат удерживается
 * сильно (session его не держит weak'ом по факту).
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrCameraView(
    modifier: Modifier,
    paused: Boolean,
    torchEnabled: Boolean,
    onCodeScanned: (String) -> Unit
) {
    val currentOnScanned = rememberUpdatedState(onCodeScanned)
    val currentPaused = rememberUpdatedState(paused)

    // Статусы AVAuthorizationStatus в Kotlin/Native — Long, не enum.
    val hasDevice = remember { AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null }
    var authorized by remember {
        mutableStateOf(
            hasDevice && AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                AVAuthorizationStatusAuthorized
        )
    }
    // Разрешение спрашивается снаружи (экраном), здесь — только чтение
    // статуса и запрос, если ещё не решали.
    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { allowed ->
                    dispatch_async(dispatch_get_main_queue()) {
                        authorized = allowed
                    }
                }
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> authorized = false
            else -> authorized = hasDevice
        }
    }

    if (!authorized) {
        // Пустая подложка: экран сканера поверх рисует своё сообщение
        // про разрешение камеры.
        androidx.compose.foundation.layout.Box(modifier = modifier)
        return
    }

    val holder = remember { ScannerDelegateHolder() }
    val session = remember { AVCaptureSession() }
    DisposableEffect(Unit) {
        holder.onScanned = { value -> if (!currentPaused.value) currentOnScanned.value(value) }
        onDispose {
            holder.onScanned = null
            dispatch_async(sessionQueue) { session.stopRunning() }
        }
    }

    UIKitView(
        factory = {
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null) {
                val input = memScoped {
                    val error = alloc<ObjCObjectVar<NSError?>>()
                    AVCaptureDeviceInput.deviceInputWithDevice(device = device, error = error.ptr)
                }
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
                val output = AVCaptureMetadataOutput()
                if (session.canAddOutput(output)) {
                    session.addOutput(output)
                    output.setMetadataObjectsDelegate(
                        objectsDelegate = holder.delegate, queue = dispatch_get_main_queue()
                    )
                    // Типы спрашиваем: неподдерживаемый тип роняет процесс
                    // NSInvalidArgumentException'ом без Kotlin-стека.
                    if (output.availableMetadataObjectTypes.any { it == AVMetadataObjectTypeQRCode }) {
                        output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
                    }
                }
            }
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            PreviewHostView(layer).apply {
                // startRunning блокирует: уходим с главного потока.
                dispatch_async(sessionQueue) { session.startRunning() }
            }
        },
        update = { _ ->
            // Фонарик: блики/тусклая доска лечатся подсветкой; идемпотентно.
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                device.lockForConfiguration(null)
                device.torchMode = if (torchEnabled) {
                    platform.AVFoundation.AVCaptureTorchModeOn
                } else {
                    platform.AVFoundation.AVCaptureTorchModeOff
                }
                device.unlockForConfiguration()
            }
        },
        modifier = modifier
    )
}

/** Серийная очередь сессии: старт/стоп камеры не блокируют UI-поток. */
private val sessionQueue = dispatch_queue_create("ru.vibemoments.krasava.qrscan", null)

/**
 * Хост-вью слоя превью: пересчитывает frame при каждом лэйауте (update
 * у UIKitView не для этого) и гасит анимацию первого кадра.
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

/** Мост колбэка метаданных в Kotlin-лямбду (удерживается сильно). */
private class ScannerDelegateHolder {
    var onScanned: ((String) -> Unit)? = null

    val delegate: NSObject = object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputMetadataObjects: List<*>,
            fromConnection: AVCaptureConnection
        ) {
            for (found in didOutputMetadataObjects) {
                (found as? AVMetadataMachineReadableCodeObject)?.stringValue
                    ?.let { value -> onScanned?.invoke(value) }
            }
        }
    }
}
