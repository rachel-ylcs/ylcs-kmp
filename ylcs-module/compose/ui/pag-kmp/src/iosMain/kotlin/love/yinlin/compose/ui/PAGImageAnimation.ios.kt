@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import cocoapods.libpag.*
import kotlinx.cinterop.ExperimentalForeignApi

@Stable
private class PAGImageAnimationWrapper : PlatformView<PAGImageView>() {
    override fun build(): PAGImageView = PAGImageView()
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
        config.repeatCount.toUInt().let { if (view.repeatCount() != it) view.setRepeatCount(it) }
        config.scaleMode.ordinal.let { if (view.scaleMode() != it) view.setScaleMode(it) }
        config.renderScale.let { if (view.renderScale() != it) view.setRenderScale(it) }
        config.cacheAllFramesInMemory.let { if (view.cacheAllFramesInMemory() != it) view.setCacheAllFramesInMemory(it) }
    }

    wrapper.Monitor(source) { view ->
        val layer: PAGFile? = when (source) {
            null -> null
            is PAGSource.File -> PAGFile.Load(source.path)
            is PAGSource.Data -> PAGFile.Load(source.data)
            else -> null
        }

        val layerBlock = source?.block
        if (source != null && layer != null && layerBlock != null) PAGSourceScope(layer).layerBlock()

        view.composition = layer
    }

    wrapper.Monitor(isPlaying) { view ->
        if (isPlaying) {
            if (!view.isPlaying()) view.play()
        }
        else {
            if (view.isPlaying()) view.pause()
        }
    }
}