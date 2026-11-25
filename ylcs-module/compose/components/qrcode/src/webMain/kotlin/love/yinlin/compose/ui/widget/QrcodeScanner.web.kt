@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.PlatformViewEnvironment
import love.yinlin.compose.ui.PlatformViewHost
import love.yinlin.compose.ui.PlatformViewState
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.ColorIcon
import love.yinlin.compose.ui.rememberPlatformViewState
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.cast
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLVideoElement
import kotlin.time.Duration.Companion.milliseconds

@Stable
private class WebQrcodeScanner : PlatformViewState<HTMLDivElement, (String) -> Unit>() {
    private val decoder = WebQrcodeDecoder()
    private var activePreview: HTMLDivElement? = null
    private var completed = false

    var picking by mutableStateOf(false)
        private set
    var torchAvailable by mutableStateOf(false)
        private set
    var torchEnabled by mutableStateOf(false)
        private set
    var cameraUnavailable by mutableStateOf(false)
        private set
    var pictureUnreadable by mutableStateOf(false)
        private set

    private val HTMLDivElement.video: HTMLVideoElement get() = firstElementChild as HTMLVideoElement

    override fun factory(environment: PlatformViewEnvironment): HTMLDivElement = createElement {
        style.cssText = "position:relative;width:100%;height:100%;overflow:hidden;container-type:size;pointer-events:none"
        appendChild(createElement<HTMLVideoElement> {
            muted = true
            setAttribute("playsinline", "")
            style.cssText = "display:block;width:100%;height:100%;object-fit:cover"
        })

        // Web 的 HTML 视图位于 Compose 画布上方，取景框与视频一起在 DOM 中叠放
        appendChild(createElement<HTMLDivElement> {
            style.cssText = "position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:min(80cqw,80cqh);aspect-ratio:1;box-sizing:border-box;border:2px solid white;box-shadow:0 0 0 100vmax rgba(0,0,0,0.55)"
        })
    }

    override fun attach(view: HTMLDivElement) {
        activePreview = view
        completed = false
        cameraUnavailable = false
        pictureUnreadable = false

        launch(view) {
            val video = view.video
            val started = catchingNull { openQrCamera(video) { activePreview === view && !completed }.await().cast }
            if (started != true) {
                closeQrCamera(video)
                if (activePreview === view && !completed) cameraUnavailable = true
                return@launch
            }

            torchAvailable = hasQrTorch(video)

            val canvas = createElement<HTMLCanvasElement>()
            while (!completed) {
                if (!picking) {
                    val result = catchingNull { decoder.decodeVideo(canvas, video) }
                    if (result != null && !picking) deliver(view, result)
                }
                delay(150.milliseconds)
            }
        }
    }

    override fun detach(view: HTMLDivElement) {
        if (activePreview === view) {
            activePreview = null
            torchAvailable = false
            torchEnabled = false
        }

        closeQrCamera(view.video)
    }

    fun selectPicture(onData: suspend () -> ByteArray?) = withView { view ->
        if (picking || completed) return@withView

        launch(view) {
            picking = true
            pictureUnreadable = false

            cleaning({
                val readError = catchingError {
                    val bytes = onData() ?: return@catchingError
                    val result = catchingNull { decoder.decodeImage(bytes) }
                    if (result == null) pictureUnreadable = true
                    else deliver(view, result)
                }

                if (readError != null) pictureUnreadable = true
            }) { picking = false }
        }
    }

    fun toggleTorch() = withView { view ->
        if (!torchAvailable || completed) return@withView

        launch(view) {
            val enabled = !torchEnabled
            if (catchingError { setQrTorch(view.video, enabled).await() } == null && !completed) torchEnabled = enabled
        }
    }

    private fun deliver(view: HTMLDivElement, result: String) = onView(view) {
        if (completed) return@onView

        completed = true
        closeQrCamera(view.video)
        torchAvailable = false
        torchEnabled = false
        parameters(result)
    }
}

@Composable
actual fun QrcodeScanner(
    modifier: Modifier,
    onData: suspend () -> ByteArray?,
    onResult: (String) -> Unit,
) {
    val scanner = rememberPlatformViewState { WebQrcodeScanner() }

    Column(
        modifier = modifier.background(Colors.Dark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlatformViewHost(scanner, onResult, Modifier.weight(1f).fillMaxWidth())

        val instruction = when {
            scanner.pictureUnreadable -> "未能从图片中识别二维码"
            scanner.cameraUnavailable -> "无法使用相机，可选择图片识别"
            else -> "将二维码放入取景框"
        }

        Text(instruction, color = Colors.White, modifier = Modifier.padding(Theme.padding.v1))

        Row(
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.v1),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorIcon(
                icon = Icons.AddPhotoAlternate,
                size = Theme.size.image9,
                color = Colors.White,
                background = Colors.Black,
                modifier = Modifier.clickable(enabled = !scanner.picking, role = Role.Button) { scanner.selectPicture(onData) }
            )

            if (scanner.torchAvailable) {
                ColorIcon(
                    icon = if (scanner.torchEnabled) Icons.FlashOff else Icons.FlashOn,
                    size = Theme.size.image9,
                    color = Colors.White,
                    background = Colors.Black,
                    modifier = Modifier.clickable(role = Role.Button) { scanner.toggleTorch() }
                )
            }
        }
    }
}
