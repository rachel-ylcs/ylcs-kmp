package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.compatible.await
import love.yinlin.compose.extension.mutableRefStateOf
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
private class PAGImageAnimationWrapper : PlatformView<HTMLCanvasElement>() {
    var pagView: PAGView? by mutableRefStateOf(null)

    override fun build(): HTMLCanvasElement {
        val view = document.createElement("canvas") as HTMLCanvasElement
        view.style.setProperty("pointer-events", "none")
        (view.parentElement as? HTMLDivElement)?.style?.setProperty("pointer-events", "none")
        return view
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PAGImageAnimation(
    source: PAGSource?,
    modifier: Modifier,
    isPlaying: Boolean,
    config: PAGConfig,
) {
    val wrapper = rememberPlatformView { PAGImageAnimationWrapper() }

    wrapper.HostView(modifier = modifier)

    wrapper.Monitor(config, wrapper.pagView) {
        wrapper.pagView?.let { pagView ->
            config.repeatCount.let { if (pagView.repeatCount != it) pagView.setRepeatCount(it) }
            config.scaleMode.ordinal.let { if (pagView.scaleMode() != it) pagView.setScaleMode(it) }
            config.cachedEnabled?.let { if (pagView.cacheEnabled() != it) pagView.setCacheEnabled(it) }
            config.cacheScale?.let { if (pagView.cacheScale() != it) pagView.setCacheScale(it) }
            config.maxFrameRate?.let { if (pagView.maxFrameRate() != it) pagView.setMaxFrameRate(it) }
        }
    }

    wrapper.Monitor(source) { view ->
        val pagInstance = pag() ?: return@Monitor
        println("ok1")
        val layer = when (source) {
            is PAGSource.Data -> {
                val data = ByteArrayCompatible(source.data).toInt8Array()
                println("ok2")
                pagInstance.PAGFile.loadFromBuffer(data.buffer).await().also { println(it.width()) }
            }
            else -> null
        }

        println("ok3")
        if (layer != null) {
            val layerBlock = source?.block
            if (source != null && layerBlock != null) PAGSourceScope(layer).layerBlock()
            println("ok4")
            wrapper.pagView?.destroy()
            println("ok5")
            wrapper.pagView = pagInstance.PAGView.init(
                composition = layer,
                canvas = view,
                initOptions = defaultPAGViewOptions.apply {
                    useCanvas2D = config.useCanvas2D
                }
            ).await()
            println("ok6")
        }
        println("ok7")
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