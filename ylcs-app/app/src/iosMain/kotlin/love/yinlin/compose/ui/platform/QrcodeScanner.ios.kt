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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import love.yinlin.app
import love.yinlin.extension.colorWithHex
import love.yinlin.compose.*
import love.yinlin.extension.toNSData
import love.yinlin.platform.Coroutines
import love.yinlin.compose.ui.image.ColorfulIcon
import love.yinlin.compose.ui.image.colorfulImageVector
import love.yinlin.platform.PlatformView
import platform.AVFoundation.*
import platform.darwin.NSObject
import platform.CoreGraphics.*
import platform.UIKit.UIColor
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIImage
import platform.UIKit.UIView

@ExperimentalForeignApi
private class QrScanView(val scanCode: SGScanCode) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val scanView = SGScanView(frame, SGScanViewConfigure().apply {
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
            scanCode.setRectOfInterest(CGRectMake(0.0, 0.0, 1.0, 1.0))
        } else {
            previewLayer?.metadataOutputRectOfInterestForRect(scanView.scanFrame)?.let {
                scanCode.setRectOfInterest(it)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scanCode = remember {
        SGScanCode().apply {
            delegate = object : SGScanCodeDelegateProtocol, NSObject() {
                override fun scanCode(scanCode: SGScanCode?, result: String?) {
                    scanCode?.stopRunning()
                    scanCode?.playSoundEffect("SGQRCode.bundle/scan_end_sound.caf")
                    onResult(result!!)
                }
            }
        }
    }
    val state: MutableState<QrScanView?> = rememberRefState { null }

    Box(modifier = modifier) {
        PlatformView(
            view = state,
            modifier = Modifier.fillMaxSize().zIndex(1f),
            factory = {
                val view = QrScanView(scanCode)
                scanCode.preview = view
                Coroutines.startIO { scanCode.startRunning() }
                view.scanView.startScanning()
                view
            },
            release = { view, onRelease ->
                view.scanView.stopScanning()
                scanCode.stopRunning()
                onRelease()
            }
        )
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
                            app.picker.pickPicture()?.use { picture ->
                                val data = picture.readByteArray().toNSData()
                                val image = UIImage(data)
                                scanCode.readQRCode(image) { text ->
                                    if (text != null) {
                                        scanCode.playSoundEffect("SGQRCode.bundle/scan_end_sound.caf")
                                        Coroutines.startMain { onResult(text) }
                                    }
                                }
                            }
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