package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.browser.window
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.round

private class PAGImageViewWrapper : PlatformView<HTMLCanvasElement>(), Releasable<HTMLCanvasElement> {
    var pagView: PlatformPAGView? by mutableRefStateOf(null)

    override fun build(): HTMLCanvasElement {
        return createElement<HTMLCanvasElement> {
            style.setProperty("pointer-events", "none")
            (parentElement as? HTMLDivElement)?.style?.setProperty("pointer-events", "none")
        }
    }

    override fun release(view: HTMLCanvasElement) {
        pagView = null
    }
}

@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
@Composable
actual fun PAGImageView(
    composition: PAGComposition?,
    modifier: Modifier,
    isPlaying: Boolean,
    config: PAGConfig
) {
    val wrapper = rememberPlatformView { PAGImageViewWrapper() }

    wrapper.HostView(modifier = modifier.onSizeChanged { size ->
        wrapper.host?.let {
            it.width = size.width
            it.height = size.height
            it.style.width = "${round(size.width / window.devicePixelRatio)}px"
            it.style.height = "${round(size.height / window.devicePixelRatio)}px"
        }
        wrapper.pagView?.updateSize()
    })

    wrapper.Monitor(config, wrapper.pagView) {
        wrapper.pagView?.let { pagView ->
            config.repeatCount.let { if (pagView.repeatCount != it) pagView.setRepeatCount(it) }
            config.scaleMode.ordinal.let { if (pagView.scaleMode() != it) pagView.setScaleMode(it) }
            config.cachedEnabled?.let { if (pagView.cacheEnabled() != it) pagView.setCacheEnabled(it) }
            config.cacheScale?.let { if (pagView.cacheScale() != it) pagView.setCacheScale(it) }
            config.maxFrameRate?.let { if (pagView.maxFrameRate() != it) pagView.setMaxFrameRate(it) }
        }
    }

    wrapper.Monitor(composition) { view ->
        wrapper.pagView?.destroy()
        if (composition == null) wrapper.pagView = null
        else {
            wrapper.pagView = PAG.pagInstance?.PAGView?.init(
                composition = composition.delegate,
                canvas = view,
                initOptions = defaultPAGViewOptions.apply { useCanvas2D = config.useCanvas2D }
            )?.await()
        }
    }

    wrapper.Monitor(isPlaying, wrapper.pagView) {
        wrapper.pagView?.let { pagView ->
            if (isPlaying) {
                if (!pagView.isPlaying) pagView.play()
            }
            else {
                if (pagView.isPlaying) pagView.pause()
            }
        }
    }
}