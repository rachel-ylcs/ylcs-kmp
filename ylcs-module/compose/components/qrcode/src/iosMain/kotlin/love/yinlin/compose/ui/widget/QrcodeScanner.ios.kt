@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.cinterop.*
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.graphics.colorWithHex
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.rememberPlatformViewState
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.extension.toNSData
import platform.AVFoundation.*
import platform.CoreImage.*
import platform.CoreGraphics.*
import platform.QuartzCore.*
import platform.UIKit.*
import platform.darwin.*

private class QrcodeView(val environment: PlatformViewEnvironment) : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val captureQueue = dispatch_queue_create("love.yinlin.qrcode.capture", null)
    private val captureSession = AVCaptureSession()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
    private val borderLayer = CAShapeLayer()
    private var scanRect = CGRectMake(0.0, 0.0, 0.0, 0.0)

    private var metadataOutput: AVCaptureMetadataOutput? = null
    private var metadataDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null
    var lifecycleObserver: LifecycleEventObserver? = null
    var onRectOfInterestChanged: ((CValue<CGRect>) -> Unit)? = null
    var detected = false

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
        scanRect = CGRectMake(x, y, side, side)
        borderLayer.frame = bounds
        borderLayer.path = UIBezierPath.bezierPathWithRect(scanRect).CGPath

        previewLayer.connection?.videoOrientation = when (UIDevice.currentDevice.orientation) {
            UIDeviceOrientation.UIDeviceOrientationPortrait -> AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> AVCaptureVideoOrientationPortraitUpsideDown
            else -> AVCaptureVideoOrientationPortrait
        }
        onRectOfInterestChanged?.invoke(rectOfInterest())
    }

    fun rectOfInterest(): CValue<CGRect> {
        val converted = previewLayer.metadataOutputRectOfInterestForRect(scanRect)
        return if (CGRectIsEmpty(converted)) CGRectMake(0.0, 0.0, 1.0, 1.0) else converted
    }

    fun configure(delegate: AVCaptureMetadataOutputObjectsDelegateProtocol) = dispatch_async(captureQueue) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return@dispatch_async
        val input = catchingNull { AVCaptureDeviceInput.deviceInputWithDevice(device, error = null) } ?: return@dispatch_async
        captureSession.beginConfiguration()
        cleaning({
            if (!captureSession.canAddInput(input)) return@cleaning
            captureSession.addInput(input)
            val output = AVCaptureMetadataOutput()
            if (captureSession.canAddOutput(output)) {
                captureSession.addOutput(output)
                metadataDelegate = delegate
                metadataOutput = output
                output.setMetadataObjectsDelegate(delegate, queue = dispatch_get_main_queue())
                if (AVMetadataObjectTypeQRCode in output.availableMetadataObjectTypes) output.metadataObjectTypes = [AVMetadataObjectTypeQRCode]
            }
        }, captureSession::commitConfiguration)
    }

    fun updateRect(rect: CValue<CGRect>) = dispatch_async(captureQueue) {
        metadataOutput?.rectOfInterest = rect
    }

    fun setRunning(running: Boolean) = dispatch_async(captureQueue) {
        if (running && captureSession.inputs.isNotEmpty() && captureSession.outputs.isNotEmpty()) {
            if (!captureSession.isRunning()) captureSession.startRunning()
        }
        else if (captureSession.isRunning()) captureSession.stopRunning()
    }

    fun toggleTorch(enabled: Boolean) = dispatch_async(captureQueue) {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return@dispatch_async
        if (device.hasTorch && device.lockForConfiguration(null)) {
            cleaning({ device.torchMode = if (enabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff }, device::unlockForConfiguration)
        }
    }

    fun dispose() = dispatch_async(captureQueue) {
        metadataOutput?.setMetadataObjectsDelegate(null, queue = null)
        if (captureSession.isRunning()) captureSession.stopRunning()
        captureSession.inputs.toList().forEach { captureSession.removeInput(it as AVCaptureInput) }
        captureSession.outputs.toList().forEach { captureSession.removeOutput(it as AVCaptureOutput) }
        metadataOutput = null
        metadataDelegate = null
    }
}

@Stable
private data class ScannerParameters(val onResult: (String) -> Unit)

@Stable
private class QrcodeScannerState : PlatformViewState<QrcodeView, ScannerParameters>() {
    var flashEnabled by mutableStateOf(false)
        private set

    override val properties = UIKitInteropProperties(interactionMode = null)

    override fun factory(environment: PlatformViewEnvironment): QrcodeView = QrcodeView(environment)

    override fun attach(view: QrcodeView) {
        flashEnabled = false
        view.configure(object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
            override fun captureOutput(output: AVCaptureOutput, didOutputMetadataObjects: List<*>, fromConnection: AVCaptureConnection) {
                onView(view) {
                    if (!it.detected) {
                        val text = (didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject)?.stringValue
                        if (text != null) {
                            it.detected = true
                            it.setRunning(false)
                            parameters.onResult(text)
                        }
                    }
                }
            }
        })
        view.onRectOfInterestChanged = view::updateRect
        view.updateRect(view.rectOfInterest())
        val lifecycle = view.environment.lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, _ ->
            onView(view) { it.setRunning(!it.detected && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
        }
        view.lifecycleObserver = observer
        lifecycle.addObserver(observer)
        view.setRunning(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    override fun detach(view: QrcodeView) {
        val observer = view.lifecycleObserver
        view.lifecycleObserver = null
        view.onRectOfInterestChanged = null
        cleanup({ observer?.let(view.environment.lifecycleOwner.lifecycle::removeObserver) }, view::dispose)
    }

    @OptIn(BetaInteropApi::class)
    fun scanPicture(onData: suspend () -> ByteArray?) = withView { view ->
        launch(view) {
            val data = onData() ?: return@launch
            val result = Coroutines.io {
                autoreleasepool {
                    catchingNull {
                        val image = UIImage(data = data.toNSData())
                        val ciImage = image.CIImage ?: image.CGImage?.let { CIImage(cGImage = it) } ?: return@catchingNull null
                        val detector = CIDetector.detectorOfType(CIDetectorTypeQRCode, context = null, options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh))
                        (detector?.featuresInImage(ciImage)?.firstOrNull() as? CIQRCodeFeature)?.messageString
                    }
                }
            }
            if (result != null) onView(view) { parameters.onResult(result) }
        }
    }

    fun toggleTorch() = withView {
        val enabled = !flashEnabled
        it.toggleTorch(enabled)
        flashEnabled = enabled
    }
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    val state = rememberPlatformViewState { QrcodeScannerState() }

    Box(modifier = modifier) {
        PlatformViewHost(state, ScannerParameters(onResult), Modifier.fillMaxSize().zIndex(1f))
        Row(
            modifier = Modifier.matchParentSize()
                .align(Alignment.BottomCenter)
                .padding(Theme.padding.v1)
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeContainer(Colors.White) {
                ColorIcon(
                    icon = Icons.AddPhotoAlternate,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable { state.scanPicture(onData) },
                )
                ColorIcon(
                    icon = if (state.flashEnabled) Icons.FlashOn else Icons.FlashOff,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable { state.toggleTorch() },
                )
            }
        }
    }
}
