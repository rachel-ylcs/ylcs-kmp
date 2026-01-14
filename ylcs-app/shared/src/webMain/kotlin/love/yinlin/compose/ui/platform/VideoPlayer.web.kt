package love.yinlin.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import love.yinlin.compose.OffScreenEffect
import love.yinlin.compose.ui.PlatformView
import love.yinlin.data.MimeType
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLVideoElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
private class VideoPlayerWrapper : PlatformView<HTMLVideoElement>() {
    override fun build(): HTMLVideoElement {
        val video = document.createElement("video") as HTMLVideoElement
        val source = document.createElement("source") as HTMLSourceElement
        video.autoplay = true
        video.controls = true
        video.loop = true
        video.muted = false
        source.type = MimeType.MP4
        video.appendChild(source)
        return video
    }

    fun load(url: String) {
        (view?.firstElementChild as? HTMLSourceElement)?.src = url
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    fun playOrPause(isForeground: Boolean) {
        view?.let {
            if (isForeground) it.play()
            else it.pause()
        }
    }
}

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val wrapper = remember { VideoPlayerWrapper() }

    wrapper.Content(modifier)

    LaunchedEffect(url) { wrapper.load(url) }

    OffScreenEffect { wrapper.playOrPause(it) }
}