package love.yinlin.compose.ui

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import org.libpag.PAGFile
import org.libpag.PAGImageView

@Stable
private class PAGImageAnimationWrapper : PlatformView<PAGImageView>() {
    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): PAGImageView = PAGImageView(context)
}

@Composable
actual fun PAGImageAnimation(
    source: PAGSource?,
    modifier: Modifier,
    repeatCount: Int,
    scaleMode: PAGConfig.ScaleMode,
    renderScale: Float,
    cacheAllFramesInMemory: Boolean,
) {
    val wrapper = rememberPlatformView { PAGImageAnimationWrapper() }

    wrapper.HostView(modifier = modifier)

    wrapper.Monitor(repeatCount, scaleMode, renderScale, cacheAllFramesInMemory) { view ->
        if (view.repeatCount() != repeatCount) view.setRepeatCount(repeatCount)
        scaleMode.asPAGScaleMode.let { if (view.scaleMode() != it) view.setScaleMode(it) }
        if (view.renderScale() != renderScale) view.setRenderScale(renderScale)
        if (view.cacheAllFramesInMemory() != cacheAllFramesInMemory) view.setCacheAllFramesInMemory(cacheAllFramesInMemory)
    }

    val assetManager = LocalContext.current.assets

    wrapper.Monitor(source) { view ->
        val layer = when (source) {
            null -> null
            is PAGSource.File -> PAGFile.Load(source.path)
            is PAGSource.Data -> PAGFile.Load(source.data)
            is PAGSource.Asset -> PAGFile.Load(assetManager, source.path)
        }

        val layerBlock = source?.block
        if (source != null && layer != null && layerBlock != null) PAGSourceScope(layer).layerBlock()

        view.composition = layer
    }
}