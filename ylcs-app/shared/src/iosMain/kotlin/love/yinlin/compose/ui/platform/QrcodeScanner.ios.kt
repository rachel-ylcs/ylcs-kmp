@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui.platform

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import cocoapods.SGQRCode.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import love.yinlin.extension.colorWithHex
import love.yinlin.compose.*
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.PlatformView
import love.yinlin.compose.ui.Releasable
import love.yinlin.extension.toNSData
import love.yinlin.compose.ui.image.ColorfulIcon
import love.yinlin.compose.ui.image.colorfulImageVector
import love.yinlin.compose.ui.rememberPlatformView
import love.yinlin.coroutines.Coroutines
import platform.AVFoundation.*
import platform.darwin.NSObject
import platform.CoreGraphics.*
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImage
import platform.UIKit.UIView

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

        if (CGRectGetWidth(scanView.scanFrame) == 0.0 && CGRectGetHeight(scanView.scanFrame) == 0.0) {
            onRectOfInterest(CGRectMake(0.0, 0.0, 1.0, 1.0))
        } else {
            previewLayer?.metadataOutputRectOfInterestForRect(scanView.scanFrame)?.let {
                onRectOfInterest(it)
            }
        }
    }
}

@Stable
private class QrcodeScannerWrapper(onResultCallback: State<(String) -> Unit>) : PlatformView<QrcodeView>(), Releasable<QrcodeView> {
    val onResult by onResultCallback

    val scanCode = SGScanCode().apply {
        delegate = object : SGScanCodeDelegateProtocol, NSObject() {
            override fun scanCode(scanCode: SGScanCode?, result: String?) {
                scanCode?.stopRunning()
                scanCode?.playSoundEffect("SGQRCode.bundle/scan_end_sound.caf")
                onResult(result!!)
            }
        }
    }

    override fun build(): QrcodeView {
        val qrcodeView = QrcodeView { scanCode.setRectOfInterest(it) }
        scanCode.preview = qrcodeView
        Coroutines.startIO { scanCode.startRunning() }
        qrcodeView.scanView.startScanning()
        return qrcodeView
    }

    override fun release(view: QrcodeView) {
        view.scanView.stopScanning()
        scanCode.stopRunning()
    }

    suspend fun parseByteArray(data: ByteArray) {
        val image = UIImage(data.toNSData())
        scanCode.readQRCode(image) { text ->
            if (text != null) {
                scanCode.playSoundEffect("SGQRCode.bundle/scan_end_sound.caf")
                Coroutines.startMain { onResult(text) }
            }
        }
    }
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onAlbumPick: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val wrapper = rememberPlatformView(onResult) { QrcodeScannerWrapper(it) }

    Box(modifier = modifier) {
        wrapper.HostView(Modifier.fillMaxSize().zIndex(1f))
        Row(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(CustomTheme.padding.verticalExtraSpace * 4)
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorfulIcon(
                icon = colorfulImageVector(
                    icon = Icons.Outlined.AddPhotoAlternate,
                    color = Colors.White,
                    background = Colors.Dark
                ),
                size = CustomTheme.size.mediumIcon,
                onClick = {
                    scope.launch {
                        Coroutines.io {
                            onAlbumPick()?.let { wrapper.parseByteArray(it) }
                        }
                    }
                }
            )

            var flashEnabled by rememberFalse()
            ColorfulIcon(
                icon = colorfulImageVector(
                    icon = if (flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    color = Colors.White,
                    background = Colors.Dark
                ),
                size = CustomTheme.size.mediumIcon,
                onClick = {
                    if (flashEnabled) SGTorch.turnOffTorch()
                    else SGTorch.turnOnTorch()
                    flashEnabled = !flashEnabled
                }
            )
        }
    }
}