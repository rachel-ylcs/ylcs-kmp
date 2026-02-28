package love.yinlin.compose.ui.widget

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultRegistry
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
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.Result
import com.king.camera.scan.BaseCameraScan
import com.king.view.viewfinderview.ViewfinderView
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.QRCodeAnalyzer
import com.king.zxing.util.CodeUtils
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.PlatformView
import love.yinlin.compose.ui.Releasable
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.rememberPlatformView
import love.yinlin.coroutines.ioContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Stable
private class QrCodeScannerWrapper : PlatformView<PreviewView>(), Releasable<PreviewView> {
    var scanResult: String? by mutableStateOf(null)

    var cameraScan: BaseCameraScan<Result>? = null

    @OptIn(ExperimentalUuidApi::class)
    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): PreviewView {
        val previewView = PreviewView(context)
        cameraScan = BaseCameraScan<Result>(context, lifecycleOwner, previewView).apply {
            setAnalyzer(QRCodeAnalyzer(DecodeConfig().apply {
                hints = DecodeFormatManager.QR_CODE_HINTS
                isFullAreaScan = false
                areaRectRatio = 0.8f
                areaRectHorizontalOffset = 0
                areaRectVerticalOffset = 0
            }))
            setPlayBeep(true)
            setOnScanResultCallback {
                setAnalyzeImage(false)
                scanResult = it.result.text
            }
            activityResultRegistry?.register(
                key = Uuid.generateV4().toHexString(),
                contract = ActivityResultContracts.RequestPermission()
            ) {
                if (it) startCamera()
            }?.launch(Manifest.permission.CAMERA)
        }
        return previewView
    }

    override fun release(view: PreviewView) {
        cameraScan?.release()
        cameraScan = null
    }

    fun parseByteArray(data: ByteArray?) {
        if (data == null) return
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return
        val result = CodeUtils.parseQRCode(bitmap)
        if (result != null) scanResult = result
        bitmap.recycle()
    }
}

@Stable
private class QrCodeScannerFinderWrapper : PlatformView<ViewfinderView>() {
    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): ViewfinderView {
        val finderView = ViewfinderView(context)
        finderView.setLaserStyle(ViewfinderView.LaserStyle.GRID)
        finderView.setLabelTextLocation(ViewfinderView.TextLocation.BOTTOM)
        return finderView
    }
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scannerWrapper = rememberPlatformView { QrCodeScannerWrapper() }
    val finderWrapper = rememberPlatformView { QrCodeScannerFinderWrapper() }

    val onDataUpdate by rememberUpdatedState(onData)
    val onResultUpdate by rememberUpdatedState(onResult)

    LaunchedEffect(scannerWrapper.scanResult) {
        scannerWrapper.scanResult?.let(onResultUpdate)
    }

    Box(modifier = modifier) {
        scannerWrapper.HostView(Modifier.matchParentSize().zIndex(1f))
        finderWrapper.HostView(Modifier.matchParentSize().zIndex(2f))
        Row(
            modifier = Modifier.matchParentSize()
                .align(Alignment.BottomCenter)
                .padding(Theme.padding.v1)
                .zIndex(3f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeContainer(Colors.White) {
                ColorIcon(
                    icon = Icons.AddPhotoAlternate,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable {
                        scope.launch(ioContext) { scannerWrapper.parseByteArray(onDataUpdate()) }
                    },
                )

                ColorIcon(
                    icon = Icons.FlashOn,
                    background = Colors.Dark,
                    modifier = Modifier.size(Theme.size.image9).clickable {
                        scannerWrapper.cameraScan?.let { it.enableTorch(!it.isTorchEnabled) }
                    }
                )
            }
        }
    }
}