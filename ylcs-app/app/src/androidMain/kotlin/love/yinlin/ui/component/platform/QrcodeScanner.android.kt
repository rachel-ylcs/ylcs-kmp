package love.yinlin.ui.component.platform

import android.graphics.BitmapFactory
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.Result
import com.king.camera.scan.BaseCameraScan
import com.king.view.viewfinderview.ViewfinderView
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.QRCodeAnalyzer
import com.king.zxing.util.CodeUtils
import kotlinx.coroutines.launch
import kotlinx.io.asInputStream
import love.yinlin.compose.*
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Picker
import love.yinlin.platform.appNative
import love.yinlin.ui.CustomUI
import love.yinlin.ui.component.image.ColorfulIcon
import love.yinlin.ui.component.image.colorfulImageVector
import java.util.*

@Stable
private class QrCodeScannerState {
    val previewView = mutableRefStateOf<PreviewView?>(null)
    val viewFinderView = mutableRefStateOf<ViewfinderView?>(null)
    var cameraScan by mutableRefStateOf<BaseCameraScan<Result>?>(null)
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
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
                            Picker.pickPicture()?.use { picture ->
                                val bitmap = BitmapFactory.decodeStream(picture.asInputStream())
                                val text = CodeUtils.parseQRCode(bitmap)
                                bitmap.recycle()
                                if (text != null) Coroutines.main { onResult(text) }
                            }
                        }
                    }
                }
            )

            var flashEnabled by rememberState { state.cameraScan?.isTorchEnabled == true }
            ColorfulIcon(
                icon = colorfulImageVector(
                    icon = if (flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    color = Colors.White,
                    background = Colors.Dark
                ),
                size = CustomTheme.size.mediumIcon,
                onClick = {
                    state.cameraScan?.let { cameraScan ->
                        if (cameraScan.isTorchEnabled) cameraScan.enableTorch(false)
                        else cameraScan.enableTorch(true)
                        flashEnabled = cameraScan.isTorchEnabled
                    }
                }
            )
        }
    }
}