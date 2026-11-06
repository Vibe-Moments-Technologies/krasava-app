package com.jetbrains.kmpapp.screens.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureMetadataObjectTypeQRCode
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.sessionPresetHigh
import platform.AVFoundation.torchModeAlwaysOn
import platform.AVFoundation.torchModeOff
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CATransaction
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/**
 * Камера-сканер (iOS): AVCaptureSession + QR-метаданные.
 *
 * Делегат-объект живёт дольше композиции (session его удерживает);
 * результаты глушатся при [paused] так же, как на Android: preview
 * живёт, дешифровка стопорится и мгновенно возобновляется.
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

    // Статус разрешения: стартуем без камеры, если не выдано — экран
    // сканера рисует сообщение поверх.
    var authorized by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                platform.AVFoundation.AVAuthorizationStatusAuthorized
        )
    }

    val session = remember { AVCaptureSession() }
    // Делегат-мост: session удерживает слабую ссылку, поэтому обычный
    // remember с retention через DelegateHolder.
    val delegateHolder = remember { MetadataDelegateHolder() }
    DisposableEffect(delegateHolder) {
        delegateHolder.onScanned = { value -> if (!currentPaused) currentOnScanned(value) }
        onDispose {
            delegateHolder.onScanned = null
            session.stopRunning()
        }
    }

    // Запрос разрешения, если ещё не решали.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            platform.AVFoundation.AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    authorized = granted
                }
            }
            platform.AVFoundation.AVAuthorizationStatusDenied,
            platform.AVFoundation.AVAuthorizationStatusRestricted -> authorized = false
            else -> authorized = true
        }
    }

    if (!authorized) {
        androidx.compose.foundation.layout.Box(modifier = modifier)
        return
    }

    UIKitView(
        factory = {
            session.sessionPreset = sessionPresetHigh
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null) {
                val input = platform.AVFoundation.AVCaptureDeviceInput
                    .deviceInputWithDevice(device, error = null)
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
                val output = AVCaptureMetadataOutput()
                if (session.canAddOutput(output)) {
                    session.addOutput(output)
                    output.setMetadataObjectsDelegate(
                        delegateHolder.delegate, queue = dispatch_get_main_queue()
                    )
                    output.metadataObjectTypes = listOf(AVCaptureMetadataObjectTypeQRCode)
                }
            }
            val layer = AVCaptureVideoPreviewLayer(session = session)
            layer.videoGravity = platform.AVFoundation.AVCaptureVideoGravityResizeAspectFill
            platform.UIKit.UIView().apply {
                layer.frame = bounds
                this.layer.addSublayer(layer)
                session.startRunning()
            }
        },
        update = { _ ->
            // Фонарик: без транзакции, режим идемпотентен.
            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                device.lockForConfiguration(null)
                device.torchMode = if (torchEnabled) torchModeAlwaysOn else torchModeOff
                device.unlockForConfiguration()
            }
        },
        modifier = modifier
    )
}

/** Удержание делегата + актуальный колбэк (rememberUpdatedState для ObjC). */
private class MetadataDelegateHolder {
    var onScanned: ((String) -> Unit)? = null

    val delegate: NSObject =
        object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
            override fun captureOutput(
                output: platform.AVFoundation.AVCaptureOutput,
                didOutputMetadataObjects: List<*>,
                fromConnection: platform.AVFoundation.AVCaptureConnection
            ) {
                didOutputMetadataObjects.forEach { obj ->
                    (obj as? platform.AVFoundation.AVMetadataMachineReadableCodeObject)
                        ?.stringValue
                        ?.let { onScanned?.invoke(it) }
                }
            }
        }
}
