@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import love.yinlin.compose.OffScreenEffect
import love.yinlin.compose.rememberRefState
import love.yinlin.data.MimeType
import love.yinlin.ui.CustomUI
import org.w3c.dom.HTMLSourceElement
import org.w3c.dom.HTMLVideoElement

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val view = rememberRefState<HTMLVideoElement?> { null }

    OffScreenEffect { isForeground ->
        view.value?.let { video ->
            if (isForeground) video.play()
            else video.pause()
        }
    }

    CustomUI(
        view = view,
        factory = {
            (document.createElement("video") as HTMLVideoElement).also { video ->
                video.autoplay = true
                video.controls = true
                video.loop = true
                video.muted = false
                (document.createElement("source") as HTMLSourceElement).also { source ->
                    source.type = MimeType.MP4
                    video.appendChild(source)
                }
            }
        },
        update = { view ->
            (view.firstElementChild as? HTMLSourceElement)?.src = url
        },
        modifier = modifier,
    )
}