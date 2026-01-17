@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cocoapods.libpag.*
import kotlinx.cinterop.ExperimentalForeignApi

@Composable
actual fun PAGAnimation(
    state: PAGState,
    modifier: Modifier,
    isPlaying: Boolean,
    config: PAGConfig,
) {
    state.HostView(modifier = modifier)

    state.Monitor(config) { view ->
        config.repeatCount.let { if (view.repeatCount() != it) view.setRepeatCount(it) }
        config.scaleMode.ordinal.toUInt().let { if (view.scaleMode() != it) view.setScaleMode(it) }
        config.cachedEnabled?.let { if (view.cacheEnabled() != it) view.setCacheEnabled(it) }
        config.cacheScale?.let { if (view.cacheScale() != it) view.setCacheScale(it) }
        config.maxFrameRate?.let { if (view.maxFrameRate() != it) view.setMaxFrameRate(it) }
        config.isSync?.let { if (view.sync() != it) view.setSync(it) }
        config.videoEnabled?.let { if (view.videoEnabled() != it) view.setVideoEnabled(it) }
        config.useDiskCache?.let { if (view.useDiskCache() != it) view.setUseDiskCache(it) }
    }

    state.Monitor(state.composition) { view ->
        state.updateComposition(view)
    }

    state.Monitor(isPlaying) { view ->
        if (isPlaying) {
            if (!view.isPlaying()) view.play()
        }
        else {
            if (view.isPlaying()) view.pause()
        }
    }
}