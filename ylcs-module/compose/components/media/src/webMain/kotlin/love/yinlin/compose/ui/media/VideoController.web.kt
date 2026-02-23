package love.yinlin.compose.ui.media

import androidx.compose.runtime.Stable
import kotlinx.browser.document
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import love.yinlin.compose.ui.PlatformView
import love.yinlin.data.MimeType
import love.yinlin.extension.createElement
import love.yinlin.extension.raw
import love.yinlin.foundation.Context
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLVideoElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual abstract class VideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoState(context, topBar, bottomBar) {
    abstract val view: PlatformView<HTMLVideoElement>

    actual override fun release() { }
}

@Stable
@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
internal class WebVideoController(context: Context, topBar: VideoActionBar?, bottomBar: VideoActionBar?) : VideoController(context, topBar, bottomBar) {
    override val view = object : PlatformView<HTMLVideoElement>() {
        override fun build(): HTMLVideoElement = createElement {
            autoplay = true
            controls = true
            loop = true
            muted = false
            appendChild(createElement<HTMLSourceElement> {
                type = MimeType.MP4
            })
            onplay = { this@WebVideoController.isPlaying = true }
            onpause = { this@WebVideoController.isPlaying = false }
            ondurationchange = { this@WebVideoController.duration = (duration * 1000).toLong() }
            ontimeupdate = { this@WebVideoController.position = (currentTime * 1000).toLong() }
            onerror = { message, _, _, _, error ->
                this@WebVideoController.error = ThrowableCompatible(error).build() ?: Throwable(message?.toString() ?: "error")
                false.raw
            }
        }
    }

    override fun load(path: String) {
        view.host {
            (it.firstElementChild as? HTMLSourceElement)?.src = path
        }
    }

    override fun play() {
        view.host?.play()
    }

    override fun pause() {
        view.host?.pause()
    }

    override fun stop() {
        view.host {
            it.pause()
            (it.firstElementChild as? HTMLSourceElement)?.src = ""
        }
    }

    override fun seek(position: Long) {
        view.host?.currentTime = position / 1000.0
    }

    override fun release() {
        view.host?.let { document.removeChild(it) }
    }
}