package love.yinlin.ui.component.platform

import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.king.camera.scan.BaseCameraScan
import com.king.view.viewfinderview.ViewfinderView
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.QRCodeAnalyzer
import love.yinlin.platform.appNative
import love.yinlin.ui.CustomUI
import java.util.UUID

@Stable
private class QrCodeScannerState {
    val previewView = mutableStateOf<PreviewView?>(null)
    val viewFinderView = mutableStateOf<ViewfinderView?>(null)
    var cameraScan by mutableStateOf<BaseCameraScan<com.google.zxing.Result>?>(null)
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { QrCodeScannerState() }

    Box(modifier = modifier) {
        CustomUI(
            view = state.previewView,
            modifier = Modifier.fillMaxSize().zIndex(1f),
            factory = { context ->
                val view = PreviewView(context)
                state.cameraScan = BaseCameraScan<com.google.zxing.Result>(context, lifecycleOwner, view).apply {
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
                        onResult(it.result.text)
                    }
                    appNative.activityResultRegistry?.register(
                        key = UUID.randomUUID().toString(),
                        contract = ActivityResultContracts.RequestPermission()
                    ) {
                        if (it) startCamera()
                    }?.launch(android.Manifest.permission.CAMERA)
                }
                view
            },
            release = { _, onRelease ->
                state.cameraScan?.release()
                state.cameraScan = null
                onRelease()
            }
        )
        CustomUI(
            view = state.viewFinderView,
            modifier = Modifier.fillMaxSize().zIndex(2f),
            factory = { context ->
                val view = ViewfinderView(context)
                view.setLaserStyle(ViewfinderView.LaserStyle.GRID)
                view.setLabelTextLocation(ViewfinderView.TextLocation.BOTTOM)
                view
            }
        )
    }
}