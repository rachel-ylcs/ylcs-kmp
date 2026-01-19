package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.OffScreenEffect
import love.yinlin.compose.ui.PlatformView
import love.yinlin.compose.ui.rememberPlatformView
import love.yinlin.data.MimeType
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLVideoElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
private class VideoPlayerWrapper : PlatformView<HTMLVideoElement>() {
    override fun build(): HTMLVideoElement = createElement {
        autoplay = true
        controls = true
        loop = true
        muted = false
        appendChild(createElement<HTMLSourceElement> {
            type = MimeType.MP4
        })
    }
}

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val wrapper = rememberPlatformView { VideoPlayerWrapper() }

    wrapper.HostView(modifier)

    wrapper.Monitor(url) {
        (it.firstElementChild as? HTMLSourceElement)?.src = url
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    OffScreenEffect { isForeground ->
        wrapper.host { if (isForeground) it.play() else it.pause() }
    }
}