@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import cocoapods.SGQRCode.*
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.graphics.colorWithHex
import love.yinlin.compose.ui.PlatformView
import love.yinlin.compose.ui.Releasable
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.rememberPlatformView
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.ioContext
import love.yinlin.extension.toNSData
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.UIKit.*
import platform.darwin.NSObject

private class QrcodeView(private val onRectOfInterest: (CValue<CGRect>) -> Unit) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val scanView: SGScanView = SGScanView(frame, SGScanViewConfigure().apply {
        scanline = "scan_scanline_qq"
        scanlineStep = 2.0
        isFromTop = true
        color = UIColor.blackColor.colorWithAlphaComponent(0.375)
        isShowBorder = true
        borderColor = UIColor.colorWithHex(0x7F1FB3E2U)
        cornerColor = UIColor.colorWithHex(0xFF1FB3E2U)
        cornerLocation = SGCornerLoaction.SGCornerLoactionInside
        cornerWidth = 4.0
        cornerLength = 16.0
    })

    init {
        addSubview(scanView)
        updateLayerOrientation()
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        scanView.setFrame(bounds)
        val w = 0.625 * CGRectGetWidth(bounds)
        val h = w
        val x = (CGRectGetWidth(bounds) - w) / 2.0
        val y = (CGRectGetHeight(bounds) - h) / 2.0
        scanView.setBorderFrame(CGRectMake(x, y, w, h))
        scanView.setScanFrame(CGRectMake(x, y, w, h))
        updateLayerOrientation()
    }

    fun updateLayerOrientation() {
        val previewLayer = layer.sublayers?.getOrNull(0) as? AVCaptureVideoPreviewLayer
        previewLayer?.frame = bounds
        val screenOrientation = UIDevice.currentDevice.orientation
        previewLayer?.connection?.videoOrientation = when (screenOrientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            else -> AVCaptureVideoOrientationLandscapeRight
        }

        if (CGRectGetWidth(scanView.scanFrame) == 0.0 && CGRectGetHeight(scanView.scanFrame) == 0.0)
            onRectOfInterest(CGRectMake(0.0, 0.0, 1.0, 1.0))
        else previewLayer?.metadataOutputRectOfInterestForRect(scanView.scanFrame)?.let(onRectOfInterest)
    }
}

@Stable
private class QrcodeScannerWrapper : PlatformView<QrcodeView>(), Releasable<QrcodeView> {
    var scanResult: String? by mutableStateOf(null)
    var isStart: Boolean by mutableStateOf(false)

    val scanCode = SGScanCode().apply {
        delegate = object : SGScanCodeDelegateProtocol, NSObject() {
            override fun scanCode(scanCode: SGScanCode?, result: String?) {
                scanCode?.stopRunning()
                scanCode?.playSoundEffect("SGQRCode.bundle/scan_end_sound.caf")
                scanResult = result!!
            }
        }
    }

    override fun build(): QrcodeView {
        val qrcodeView = QrcodeView { scanCode.setRectOfInterest(it) }
        scanCode.preview = qrcodeView
        return qrcodeView
    }

    override fun release(view: QrcodeView) {
        view.scanView.stopScanning()
        scanCode.stopRunning()
    }

    fun parseByteArray(data: ByteArray?) {
        if (data == null) return
        val image = UIImage(data.toNSData())
        scanCode.readQRCode(image) { text: String? ->
            if (text != null) {
                scanCode.playSoundEffect("SGQRCode.bundle/scan_end_sound.caf")
                scanResult = text
            }
        }
    }
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val wrapper = rememberPlatformView { QrcodeScannerWrapper() }

    val onDataUpdate by rememberUpdatedState(onData)
    val onResultUpdate by rememberUpdatedState(onResult)

    wrapper.Monitor(wrapper.isStart) {
        if (wrapper.isStart) {
            it.scanView.startScanning()
            Coroutines.io { it.scanCode.startRunning() }
        }
    }

    LaunchedEffect(wrapper.scanResult) {
        wrapper.scanResult?.let(onResultUpdate)
    }

    Box(modifier = modifier) {
        wrapper.HostView(Modifier.fillMaxSize().zIndex(1f))
        Row(
            modifier = Modifier.matchParentSize()
                .align(Alignment.BottomCenter)
                .padding(Theme.padding.v1)
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeContainer(Colors.White) {
                ColorIcon(
                    icon = Icons.AddPhotoAlternate,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable {
                        scope.launch(ioContext) { wrapper.parseByteArray(onDataUpdate()) }
                    },
                )

                var flashEnabled by rememberFalse()
                ColorIcon(
                    icon = if (flashEnabled) Icons.FlashOn else Icons.FlashOff,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable {
                        if (flashEnabled) SGTorch.turnOffTorch()
                        else SGTorch.turnOnTorch()
                        flashEnabled = !flashEnabled
                    }
                )
            }
        }
    }
}