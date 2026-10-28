package love.yinlin.compose.ui.widget

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.google.zxing.Result
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.BaseCameraScan
import com.king.camera.scan.CameraScan
import com.king.view.viewfinderview.ViewfinderView
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.QRCodeAnalyzer
import com.king.zxing.util.CodeUtils
import kotlinx.coroutines.awaitCancellation
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
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
import kotlin.uuid.Uuid

private class ScannerPreviewResources(val environment: PlatformViewEnvironment) {
    var cameraScan: BaseCameraScan<Result>? = null
    var permission: ActivityResultLauncher<String>? = null
}

@Stable
private data class ScannerParameters(val onResult: (String) -> Unit)

@Stable
private class QrCodeScannerState : PlatformViewState<PreviewView, ScannerParameters>() {
    val finder = QrCodeScannerFinderState()
    private val previews = mutableMapOf<PreviewView, ScannerPreviewResources>()

    override fun factory(environment: PlatformViewEnvironment): PreviewView = PreviewView(environment.context).also {
        previews[it] = ScannerPreviewResources(environment)
    }

    override fun attach(view: PreviewView) {
        val resources = previews.getValue(view)
        val scan = BaseCameraScan<Result>(view.context, resources.environment.lifecycleOwner, view)
        resources.cameraScan = scan
        scan.setAnalyzer(QRCodeAnalyzer(DecodeConfig().apply {
            setHints(DecodeFormatManager.QR_CODE_HINTS)
            setFullAreaScan(false)
            setAreaRectRatio(0.8f)
            setAreaRectHorizontalOffset(0)
            setAreaRectVerticalOffset(0)
        }))
        scan.setPlayBeep(true)
        scan.setOnScanResultCallback(object : CameraScan.OnScanResultCallback<Result> {
            override fun onScanResultCallback(result: AnalyzeResult<Result>) = onView(view) {
                previews[it]?.cameraScan?.setAnalyzeImage(false)
                parameters.onResult(result.result.text)
            }
        })
        resources.permission = resources.environment.activityResultRegistry?.register(
            key = Uuid.generateV7().toHexString(),
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            onView(view) { if (granted) previews[it]?.cameraScan?.startCamera() }
        }
        resources.permission?.launch(Manifest.permission.CAMERA)
        launch(view) {
            cleaning({
                awaitCancellation()
            }) {
                stopPreview(resources)
            }
        }
    }

    override fun detach(view: PreviewView) {
        val resources = previews.remove(view) ?: return
        stopPreview(resources)
    }

    override fun releaseResources() {
        finder.release()
    }

    private fun stopPreview(resources: ScannerPreviewResources) {
        val scan = resources.cameraScan
        val permission = resources.permission
        resources.cameraScan = null
        resources.permission = null

        cleanup({ permission?.unregister() }, {
            scan?.setOnScanResultCallback(object : CameraScan.OnScanResultCallback<Result> {
                override fun onScanResultCallback(result: AnalyzeResult<Result>) {}
            })
        }, { scan?.release() })
    }

    fun scanPicture(onData: suspend () -> ByteArray?) = withView { view ->
        launch(view) {
            val data = onData() ?: return@launch
            val result = Coroutines.io {
                catchingNull {
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return@catchingNull null
                    cleaning({ CodeUtils.parseQRCode(bitmap) }, bitmap::recycle)
                }
            }
            if (result != null) onView(view) { parameters.onResult(result) }
        }
    }

    fun toggleTorch() = withView { view ->
        previews[view]?.cameraScan?.let { it.enableTorch(!it.isTorchEnabled()) }
    }
}

@Stable
private class QrCodeScannerFinderState : PlatformViewState<ViewfinderView, Unit>() {
    override fun factory(environment: PlatformViewEnvironment): ViewfinderView = ViewfinderView(environment.context).apply {
        setLaserStyle(ViewfinderView.LaserStyle.GRID)
        setLabelTextLocation(ViewfinderView.TextLocation.BOTTOM)
    }

    override fun detach(view: ViewfinderView) {}
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit,
) {
    val scanner = rememberPlatformViewState { QrCodeScannerState() }

    Box(modifier = modifier) {
        PlatformViewHost(scanner, ScannerParameters(onResult), Modifier.matchParentSize().zIndex(1f))
        PlatformViewHost(scanner.finder, Unit, Modifier.matchParentSize().zIndex(2f))
        Row(
            modifier = Modifier.matchParentSize()
                .align(Alignment.BottomCenter)
                .padding(Theme.padding.v1)
                .zIndex(3f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeContainer(Colors.White) {
                ColorIcon(
                    icon = Icons.AddPhotoAlternate,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable { scanner.scanPicture(onData) },
                )
                ColorIcon(
                    icon = Icons.FlashOn,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable { scanner.toggleTorch() },
                )
            }
        }
    }
}
