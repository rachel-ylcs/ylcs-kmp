package love.yinlin.compose.ui

import android.content.Context
import android.content.res.AssetManager
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.LifecycleOwner
import love.yinlin.compose.extension.mutableRefStateOf
import org.libpag.PAGComposition
import org.libpag.PAGFile
import org.libpag.PAGView

@Stable
actual class PAGState actual constructor(
    initComposition: PAGSourceComposition?,
    initIsPlaying: Boolean,
    initProgress: Double,
    listener: PAGAnimationListener?,
) : PlatformView<PAGView>(), Releasable<PAGView> {
    internal var stateComposition: PAGSourceComposition? by mutableRefStateOf(initComposition)
    internal var stateProgress: Double by mutableDoubleStateOf(initProgress)

    private val pagListener = listener?.let {
        object : PAGView.PAGViewListener {
            override fun onAnimationStart(view: PAGView) = it.onAnimationStart()
            override fun onAnimationEnd(view: PAGView) = it.onAnimationEnd()
            override fun onAnimationCancel(view: PAGView) = it.onAnimationCancel()
            override fun onAnimationRepeat(view: PAGView) = it.onAnimationRepeat()
            override fun onAnimationUpdate(view: PAGView) {
                stateProgress = view.progress
                it.onAnimationUpdate(view.progress)
            }
        }
    }

    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): PAGView {
        val view = PAGView(context)
        pagListener?.let { view.addListener(it) }
        return view
    }

    override fun release(view: PAGView) {
        pagListener?.let { view.removeListener(it) }
    }

    var progress: Double get() = stateProgress
        set(value) {
            host?.let {
                it.progress = value
                it.flush()
            }
        }

    fun play() { host?.play() }
    fun pause() { host?.pause() }
    val isPlaying: Boolean get() = host?.isPlaying ?: false

    fun freeCache() { host?.freeCache() }

    fun makeSnapshot(): ImageBitmap? = host?.makeSnapshot()?.asImageBitmap()

    internal fun updateComposition(assetManager: AssetManager, view: PAGView) {
        val sources = stateComposition?.sources?.ifEmpty { null }
        var width = stateComposition?.width
        var height = stateComposition?.height

        val layers = mutableListOf<PAGFile>()
        sources?.fastForEach { source ->
            val layer = when (source) {
                is PAGSource.File -> PAGFile.Load(source.path)
                is PAGSource.Data -> PAGFile.Load(source.data)
                is PAGSource.Asset -> PAGFile.Load(assetManager, source.path)
            }
            with(source) { PAGSourceScope(layer).block() }
            if (width == null || height == null) {
                width = layer.width()
                height = layer.height()
            }
            layers += layer
        }

        view.composition = when (layers.size) {
            0 -> null
            1 -> layers.first()
            else -> {
                val w = width
                val h = height
                if (w != null && h != null) {
                    val multiComposition = PAGComposition.Make(w, h)
                    for (layer in layers) multiComposition.addLayer(layer)
                    multiComposition
                } else null
            }
        }
    }
}