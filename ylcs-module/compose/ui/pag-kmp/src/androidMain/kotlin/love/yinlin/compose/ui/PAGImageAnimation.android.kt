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
    isPlaying: Boolean,
    config: PAGConfig,
) {
    val wrapper = rememberPlatformView { PAGImageAnimationWrapper() }

    wrapper.HostView(modifier = modifier)

    wrapper.Monitor(config) { view ->
        config.repeatCount.let { if (view.repeatCount() != it) view.setRepeatCount(it) }
        config.scaleMode.ordinal.let { if (view.scaleMode() != it) view.setScaleMode(it) }
        config.renderScale.let { if (view.renderScale() != it) view.setRenderScale(it) }
        config.cacheAllFramesInMemory.let { if (view.cacheAllFramesInMemory() != it) view.setCacheAllFramesInMemory(it) }
    }

    val assetManager = LocalContext.current.assets

    wrapper.Monitor(source) { view ->
        val layer: PAGFile? = when (source) {
            null -> null
            is PAGSource.File -> PAGFile.Load(source.path)
            is PAGSource.Data -> PAGFile.Load(source.data)
            is PAGSource.Asset -> PAGFile.Load(assetManager, source.path)
        }

        val layerBlock = source?.block
        if (source != null && layer != null && layerBlock != null) PAGSourceScope(layer).layerBlock()

        view.composition = layer
    }

    wrapper.Monitor(isPlaying) { view ->
        if (isPlaying) {
            if (!view.isPlaying) view.play()
        }
        else {
            if (view.isPlaying) view.pause()
        }
    }
}