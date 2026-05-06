@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.extension.then

@Composable
actual fun PAGView(
    state: PAGState,
    modifier: Modifier,
    composition: PAGComposition?,
    isPlaying: Boolean,
    config: PAGConfig,
) {
    state.HostView(modifier = modifier)

    state.Monitor(config) { view ->
        config.repeatCount.then { if (view.repeatCount() != it) view.setRepeatCount(it) }
        config.scaleMode.ordinal.toUInt().then { if (view.scaleMode() != it) view.setScaleMode(it) }
        config.cachedEnabled?.then { if (view.cacheEnabled() != it) view.setCacheEnabled(it) }
        config.cacheScale?.then { if (view.cacheScale() != it) view.setCacheScale(it) }
        config.maxFrameRate?.then { if (view.maxFrameRate() != it) view.setMaxFrameRate(it) }
        config.isSync?.then { if (view.sync() != it) view.setSync(it) }
        config.videoEnabled?.then { if (view.videoEnabled() != it) view.setVideoEnabled(it) }
        config.useDiskCache?.then { if (view.useDiskCache() != it) view.setUseDiskCache(it) }
    }

    state.Monitor(composition) { view ->
        view.setComposition(composition?.delegate)
    }

    state.Monitor(state.flushFlag) { view ->
        view.flush()
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