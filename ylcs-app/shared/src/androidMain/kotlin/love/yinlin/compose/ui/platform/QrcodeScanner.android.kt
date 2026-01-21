package love.yinlin.compose.ui.platform

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
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
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.PlatformView
import love.yinlin.compose.ui.Releasable
import love.yinlin.compose.ui.image.ColorfulIcon
import love.yinlin.compose.ui.image.colorfulImageVector
import love.yinlin.compose.ui.rememberPlatformView
import love.yinlin.coroutines.Coroutines
import java.util.UUID

@Stable
private class QrCodeScannerWrapper(onResultCallback: State<(String) -> Unit>) : PlatformView<PreviewView>(), Releasable<PreviewView> {
    private val onResult by onResultCallback

    var cameraScan: BaseCameraScan<Result>? = null

    val enableTorch: Boolean get() = cameraScan?.isTorchEnabled ?: false

    fun updateResult(result: String) { onResult(result) }

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
                updateResult(it.result.text)
            }
            activityResultRegistry?.register(
                key = UUID.randomUUID().toString(),
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
    onAlbumPick: suspend () -> ByteArray?,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scannerWrapper = rememberPlatformView(onResult) { QrCodeScannerWrapper(it) }
    val finderWrapper = rememberPlatformView { QrCodeScannerFinderWrapper() }

    Box(modifier = modifier) {
        scannerWrapper.HostView(Modifier.fillMaxSize().zIndex(1f))
        finderWrapper.HostView(Modifier.fillMaxSize().zIndex(2f))
        Row(
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(CustomTheme.padding.verticalExtraSpace * 4)
                .zIndex(3f),
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
                            onAlbumPick()?.let { picture ->
                                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                                CodeUtils.parseQRCode(bitmap).also { bitmap.recycle() }
                            }
                        }?.let { scannerWrapper.updateResult(it) }
                    }
                }
            )

            var flashEnabled by rememberState { scannerWrapper.enableTorch }
            ColorfulIcon(
                icon = colorfulImageVector(
                    icon = if (flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    color = Colors.White,
                    background = Colors.Dark
                ),
                size = CustomTheme.size.mediumIcon,
                onClick = {
                    scannerWrapper.cameraScan?.let { it.enableTorch(!it.isTorchEnabled) }
                    flashEnabled = scannerWrapper.enableTorch
                }
            )
        }
    }
}