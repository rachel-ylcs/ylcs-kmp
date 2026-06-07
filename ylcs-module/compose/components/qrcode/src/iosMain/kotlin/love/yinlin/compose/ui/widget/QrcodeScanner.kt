@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.graphics.colorWithHex
import love.yinlin.compose.ui.PlatformView
import love.yinlin.compose.ui.Releasable
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.rememberPlatformView
import love.yinlin.coroutines.ioContext
import love.yinlin.extension.toNSData
import platform.AVFoundation.*
import platform.CoreImage.*
import platform.CoreGraphics.*
import platform.QuartzCore.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

private class QrcodeView : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val captureSession = AVCaptureSession()
    val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
    private val borderLayer = CAShapeLayer()
    private var scanRect: CGRect = CGRectZero
    var onRectOfInterestChanged: ((CGRect) -> Unit)? = null

    init {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)

        borderLayer.fillColor = UIColor.clearColor.CGColor
        borderLayer.strokeColor = UIColor.colorWithHex(0xFF1FB3E2U).CGColor
        borderLayer.lineWidth = 4.0
        layer.addSublayer(borderLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds

        val side = 0.625 * minOf(CGRectGetWidth(bounds), CGRectGetHeight(bounds))
        val x = (CGRectGetWidth(bounds) - side) / 2.0
        val y = (CGRectGetHeight(bounds) - side) / 2.0
        scanRect = CGRectMake(x, y, side, side).useContents { this }

        borderLayer.frame = bounds
        borderLayer.path = UIBezierPath.bezierPathWithRect(scanRect.readValue()).CGPath
        onRectOfInterestChanged?.invoke(rectOfInterest())

        previewLayer.connection?.videoOrientation = when (UIDevice.currentDevice.orientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            else -> AVCaptureVideoOrientationPortrait
        }
    }

    fun rectOfInterest(): CGRect {
        val convertedRect = previewLayer.metadataOutputRectOfInterestForRect(scanRect.readValue())
        val result = if (CGRectIsEmpty(convertedRect)) CGRectMake(0.0, 0.0, 1.0, 1.0) else convertedRect
        return result.useContents { this }
    }
}

@Stable
private class QrcodeScannerWrapper : PlatformView<QrcodeView>(), Releasable<QrcodeView> {
    var scanResult: String? by mutableStateOf(null)
    private var scannerView: QrcodeView? = null
    private var isStarted = false
    private var torchEnabled = false
    private val metadataObjectsDelegate = object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputMetadataObjects: List<*>,
            fromConnection: AVCaptureConnection
        ) {
            val qrObject = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject ?: return
            val text = qrObject.stringValue ?: return
            scanResult = text
            scannerView?.captureSession?.stopRunning()
            isStarted = false
        }
    }
    private val detector: CIDetector? = CIDetector.detectorOfType(
        CIDetectorTypeQRCode,
        context = null,
        options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)
    )

    private fun createCaptureInput(): AVCaptureDeviceInput? {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return null
        return runCatching {
            AVCaptureDeviceInput.deviceInputWithDevice(device, error = null)
        }.getOrNull()
    }

    override fun build(): QrcodeView {
        val qrcodeView = QrcodeView()
        scannerView = qrcodeView

        val input = createCaptureInput() ?: return qrcodeView
        val metadataOutput = AVCaptureMetadataOutput()

        qrcodeView.captureSession.beginConfiguration()
        if (qrcodeView.captureSession.canAddInput(input)) {
            qrcodeView.captureSession.addInput(input)
        }
        if (qrcodeView.captureSession.canAddOutput(metadataOutput)) {
            qrcodeView.captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(metadataObjectsDelegate, queue = dispatch_get_main_queue())
            metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
            qrcodeView.onRectOfInterestChanged = { metadataOutput.rectOfInterest = it.readValue() }
            metadataOutput.rectOfInterest = qrcodeView.rectOfInterest().readValue()
        }
        qrcodeView.captureSession.commitConfiguration()

        return qrcodeView
    }

    override fun release(view: QrcodeView) {
        isStarted = false
        view.captureSession.stopRunning()
        scannerView = null
    }

    fun parseByteArray(data: ByteArray?) {
        if (data == null) return
        val image = UIImage(data = data.toNSData())
        val ciImage = image.CIImage ?: CIImage(cGImage = image.CGImage)
        val features = detector?.featuresInImage(ciImage) ?: return
        val qrFeature = features.firstOrNull() as? CIQRCodeFeature
        val text = qrFeature?.messageString ?: return
        scanResult = text
    }

    fun start() {
        val view = scannerView ?: return
        if (isStarted) return
        view.captureSession.startRunning()
        isStarted = true
    }

    fun toggleTorch(enabled: Boolean) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        if (!device.hasTorch) return
        runCatching {
            device.lockForConfiguration(null)
            device.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            device.unlockForConfiguration()
            torchEnabled = enabled
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

    DisposableEffect(Unit) {
        wrapper.start()
        onDispose { }
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

                var flashEnabled by remember { mutableStateOf(false) }
                ColorIcon(
                    icon = if (flashEnabled) Icons.FlashOn else Icons.FlashOff,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable {
                        wrapper.toggleTorch(!flashEnabled)
                        flashEnabled = !flashEnabled
                    }
                )
            }
        }
    }
}