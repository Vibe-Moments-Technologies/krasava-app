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
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
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
import platform.AVFoundation.AVCaptureVideoGravityResizeAspectFill
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.avMetadataObjectTypeQRCode
import platform.Foundation.NSErrorVar
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/**
 * Камера-сканер (iOS): AVCaptureSession + QR-метаданные.
 *
 * Делегат-мост удерживается remember-холдером; результаты глушатся при
 * [paused] (как на Android): preview живёт, дешифровка стопорится и
 * мгновенно возобновляется при возврате на экран сканера.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun QrCameraView(
    modifier: Modifier,
    paused: Boolean,
    torchEnabled: Boolean,
    onCodeScanned: (String) -> Unit
) {
    val currentOnScanned by rememberUpdatedState(onCodeScanned)
    val currentPaused by rememberUpdatedState(paused)

    // Статус разрешения: без камеры стартуем только если отказано —
    // экран сканера рисует сообщение поверх пустой подложки.
    var authorized by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                AVAuthorizationStatusAuthorized
        )
    }

    val session = remember { AVCaptureSession() }
    val holder = remember { ScannerDelegateHolder() }
    DisposableEffect(holder) {
        holder.onScanned = { value -> if (!currentPaused) currentOnScanned(value) }
        onDispose {
            holder.onScanned = null
            session.stopRunning()
        }
    }

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    authorized = granted
                }
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> authorized = false
            else -> authorized = true
        }
    }

    if (!authorized) {
        androidx.compose.foundation.layout.Box(modifier = modifier)
        return
    }

    UIKitView(
        factory = {
            session.sessionPreset = AVCaptureSessionPresetHigh
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                val errorPtr = NSErrorVar()
                val input = AVCaptureDeviceInput.deviceInputWithDevice(device, error = errorPtr)
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
                val output = AVCaptureMetadataOutput()
                if (session.canAddOutput(output)) {
                    session.addOutput(output)
                    output.setMetadataObjectsDelegate(
                        holder.delegate, queue = dispatch_get_main_queue()
                    )
                    output.metadataObjectTypes = listOf(avMetadataObjectTypeQRCode)
                }
            }
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = AVCaptureVideoGravityResizeAspectFill
            UIView().apply {
                layer.frame = bounds
                this.layer.addSublayer(layer)
                session.startRunning()
            }
        },
        update = { _ ->
            // Фонарик: блики/тусклая доска лечатся подсветкой; режимы
            // идемпотентны, транзакция не нужна.
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                if (device.hasTorch()) {
                    device.lockForConfiguration(null)
                    device.torchMode = if (torchEnabled) {
                        platform.AVFoundation.AVCaptureTorchModeOn
                    } else {
                        platform.AVFoundation.AVCaptureTorchModeOff
                    }
                    device.unlockForConfiguration()
                }
            }
        },
        modifier = modifier
    )
}

/** Мост колбэка метаданных в Kotlin-лямбду. */
private class ScannerDelegateHolder {
    var onScanned: ((String) -> Unit)? = null

    val delegate: NSObject = object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputMetadataObjects: List<*>,
            fromConnection: AVCaptureConnection
        ) {
            didOutputMetadataObjects.forEach { obj ->
                (obj as? AVMetadataMachineReadableCodeObject)?.stringValue
                    ?.let { value -> onScanned?.invoke(value) }
            }
        }
    }
}
