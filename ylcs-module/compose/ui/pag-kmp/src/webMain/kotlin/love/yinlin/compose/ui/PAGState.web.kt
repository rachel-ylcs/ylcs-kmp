@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.util.fastForEach
import kotlinx.browser.document
import love.yinlin.compatible.ByteArrayCompatible
import love.yinlin.compatible.await
import love.yinlin.compose.extension.mutableRefStateOf
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
actual class PAGState actual constructor(
    initComposition: PAGSourceComposition?,
    initProgress: Double,
    private val listener: PAGAnimationListener?,
) : PlatformView<HTMLCanvasElement>(), Releasable<HTMLCanvasElement> {
    internal var stateProgress: Double by mutableDoubleStateOf(initProgress)
    internal var pagView: PAGView? by mutableRefStateOf(null)

    override fun build(): HTMLCanvasElement {
        val view = document.createElement("canvas") as HTMLCanvasElement
        view.style.setProperty("pointer-events", "none")
        (view.parentElement as? HTMLDivElement)?.style?.setProperty("pointer-events", "none")
        return view
    }

    override fun release(view: HTMLCanvasElement) {
        pagView?.destroy()
        pagView = null
    }

    actual var composition: PAGSourceComposition? by mutableRefStateOf(initComposition)

    internal suspend fun updateComposition(config: PAGConfig, view: HTMLCanvasElement) {
        val sources = composition?.sources?.ifEmpty { null }
        var width = composition?.width
        var height = composition?.height
        println("ok1")
        val pagInstance = pag() ?: return
        println("ok2")
        val layers = mutableListOf<PAGFile>()
        sources?.fastForEach { source ->
            val layer: PAGFile? = when (source) {
                is PAGSource.Data -> {
                    val data = ByteArrayCompatible(source.data).toInt8Array()
                    pagInstance.PAGFile.loadFromBuffer(data.buffer).await()
                }
                else -> null
            }
            println("ok4")
            if (layer != null) {
                val layerBlock = source.block
                if (layerBlock != null) PAGSourceScope(layer).layerBlock()

                if (width == null || height == null) {
                    width = layer.width()
                    height = layer.height()
                }

                layers += layer
            }
            println("ok5")
        }
        println("ok6 ${layers.size}")
        val newComposition = when (layers.size) {
            0 -> null
            1 -> {
                println("so 2?")
                println("so ${layers.size}")
                val a = layers.firstOrNull()
                println("so $a ?")
                layers.first()
            }
            else -> {
                val w = width
                val h = height
                if (w != null && h != null) {
                    val multiComposition = pagInstance.PAGComposition.make(w, h)
                    for (layer in layers) multiComposition.addLayer(layer)
                    multiComposition
                } else null
            }
        }
        println("ok7")
        if (newComposition != null) {
            println("ok8")
            pagView?.destroy()
            println("ok9")
            val newPagView = pagInstance.PAGView.init(
                composition = newComposition,
                canvas = view,
                initOptions = defaultPAGViewOptions.apply {
                    useCanvas2D = config.useCanvas2D
                }
            ).await()
            println("ok10")
            if (newPagView != null) {
                newPagView.removeListener("onAnimationStart", null)
                newPagView.removeListener("onAnimationEnd", null)
                newPagView.removeListener("onAnimationCancel", null)
                newPagView.removeListener("onAnimationRepeat", null)
                newPagView.removeListener("onAnimationUpdate", null)
                newPagView.addListener("onAnimationStart") { listener?.onAnimationStart() }
                newPagView.addListener("onAnimationEnd") { listener?.onAnimationEnd() }
                newPagView.addListener("onAnimationCancel") { listener?.onAnimationCancel() }
                newPagView.addListener("onAnimationRepeat") { listener?.onAnimationRepeat() }
                newPagView.addListener("onAnimationUpdate") { stateProgress = newPagView.getProgress() }
            }
            println("ok11")
            pagView = newPagView
            println("ok12")
        }
    }

    actual var progress: Double get() = stateProgress
        set(value) {
            pagView?.let {
                it.setProgress(value)
                it.flush()
            }
        }

    actual fun freeCache() { pagView?.freeCache() }
    actual fun makeSnapshot(): ImageBitmap? = null
}