@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.util.fastForEach
import cocoapods.libpag.*
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.compose.extension.mutableRefStateOf
import platform.CoreGraphics.CGSizeMake
import platform.darwin.NSObject

@Stable
actual class PAGState actual constructor(
    initComposition: PAGSourceComposition?,
    initProgress: Double,
    listener: PAGAnimationListener?,
) : PlatformView<PAGView>(), Releasable<PAGView> {
    internal var stateProgress: Double by mutableDoubleStateOf(initProgress)

    private val pagListener = listener?.let {
        object : NSObject(), PAGViewListenerProtocol {
            override fun onAnimationStart(view: PAGView?) = it.onAnimationStart()
            override fun onAnimationEnd(view: PAGView?) = it.onAnimationEnd()
            override fun onAnimationCancel(view: PAGView?) = it.onAnimationCancel()
            override fun onAnimationRepeat(view: PAGView?) = it.onAnimationRepeat()
            override fun onAnimationUpdate(view: PAGView?) { stateProgress = view?.getProgress() ?: 0.0 }
        }
    }

    override fun build(): PAGView {
        val view = PAGView()
        view.addListener(pagListener)
        return view
    }

    override fun release(view: PAGView) {
        view.removeListener(pagListener)
    }

    actual var composition: PAGSourceComposition? by mutableRefStateOf(initComposition)

    internal fun updateComposition(view: PAGView) {
        val sources = composition?.sources?.ifEmpty { null }
        var width = composition?.width
        var height = composition?.height

        val layers = mutableListOf<PAGFile>()
        sources?.fastForEach { source ->
            val layer: PAGFile? = when (source) {
                is PAGSource.File -> PAGFile.Load(source.path)
                is PAGSource.Data -> PAGFile.Load(source.data)
                else -> null
            }

            if (layer != null) {
                val layerBlock = source.block
                if (layerBlock != null) PAGSourceScope(layer).layerBlock()

                if (width == null || height == null) {
                    width = layer.width().toInt()
                    height = layer.height().toInt()
                }

                layers += layer
            }
        }

        view.setComposition(when (layers.size) {
            0 -> null
            1 -> layers.first()
            else -> {
                val w = width
                val h = height
                if (w != null && h != null) {
                    PAGComposition.Make(CGSizeMake(w.toDouble(), h.toDouble()))?.also { multiComposition ->
                        for (layer in layers) multiComposition.addLayer(layer)
                    }
                } else null
            }
        })
    }

    actual var progress: Double get() = stateProgress
        set(value) {
            host?.let {
                it.setProgress(value)
                it.flush()
            }
        }

    actual fun freeCache() { host?.freeCache() }

    actual fun makeSnapshot(): ImageBitmap? = host?.makeSnapshot()?.let(::createImageFromPixelBuffer)
}