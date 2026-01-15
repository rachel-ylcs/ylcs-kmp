package love.yinlin.compose.ui

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import love.yinlin.compose.extension.mutableRefStateOf
import org.libpag.PAGFile
import org.libpag.PAGScaleMode
import org.libpag.PAGView

@Stable
actual open class PAGState actual constructor(
    initSource: PAGSource?,
    initIsPlaying: Boolean,
    initProgress: Double,
    initRepeatCount: Int,
    initScaleMode: PAGConfig.ScaleMode,
    private val listener: PAGAnimationListener
) : PlatformView<PAGView>() {
    var source: PAGSource? by mutableRefStateOf(initSource)
    var isPlaying: Boolean by mutableStateOf(initIsPlaying)
    var progress: Double by mutableDoubleStateOf(initProgress)
    var repeatCount: Int by mutableIntStateOf(initRepeatCount)
    var scaleMode: PAGConfig.ScaleMode by mutableStateOf(initScaleMode)

    private val pagListener = object : PAGView.PAGViewListener {
        override fun onAnimationStart(p0: PAGView?) = listener.onAnimationStart()
        override fun onAnimationEnd(p0: PAGView?) = listener.onAnimationEnd()
        override fun onAnimationCancel(p0: PAGView?) = listener.onAnimationCancel()
        override fun onAnimationRepeat(p0: PAGView?) = listener.onAnimationRepeat()
        override fun onAnimationUpdate(p0: PAGView?) = listener.onAnimationUpdate(progress)
    }

    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): PAGView {
        val view = PAGView(context)
        view.addListener(pagListener)
        return view
    }

    override fun update(view: PAGView) {
        view.setRepeatCount(repeatCount)
        view.setScaleMode(when (scaleMode) {
            PAGConfig.ScaleMode.None -> PAGScaleMode.None
            PAGConfig.ScaleMode.Stretch -> PAGScaleMode.Stretch
            PAGConfig.ScaleMode.LetterBox -> PAGScaleMode.LetterBox
            PAGConfig.ScaleMode.Zoom -> PAGScaleMode.Zoom
        })
    }

    override fun release(view: PAGView) {
        view.removeListener(pagListener)
    }

    @Composable
    override fun Monitor() {
        val assetManager = LocalContext.current.assets

        LaunchedEffect(source) {
            view?.composition = when (val rawSource = source) {
                null -> null
                is PAGSource.File -> PAGFile.Load(rawSource.path)
                is PAGSource.Data -> PAGFile.Load(rawSource.data)
                is PAGSource.Asset -> PAGFile.Load(assetManager, rawSource.path)
            }
        }

        LaunchedEffect(isPlaying) {
            view?.let {
                if (isPlaying) it.play() else it.pause()
            }
        }

        LaunchedEffect(progress) {
            view?.progress = progress
            view?.flush()
        }

        LaunchedEffect()
    }
}