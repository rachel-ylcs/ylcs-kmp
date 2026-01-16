package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PAGAnimation(
    state: PAGState,
    modifier: Modifier,
    isPlaying: Boolean,
    repeatCount: Int,
    scaleMode: PAGConfig.ScaleMode,
    cachedEnabled: Boolean?,
    cacheScale: Float?,
    maxFrameRate: Float?,
    isSync: Boolean?,
    videoEnabled: Boolean?,
    useDiskCache: Boolean?,
) {
    state.HostView(modifier = modifier)

    state.Monitor(repeatCount, scaleMode, cachedEnabled, cacheScale, maxFrameRate, isSync, videoEnabled, useDiskCache) { view ->
        if (view.repeatCount() != repeatCount) view.setRepeatCount(repeatCount)
        scaleMode.asPAGScaleMode.let { if (view.scaleMode() != it) view.setScaleMode(it) }
        cachedEnabled?.let { if (view.cacheEnabled() != it) view.setCacheEnabled(it) }
        cacheScale?.let { if (view.cacheScale() != it) view.setCacheScale(it) }
        maxFrameRate?.let { if (view.maxFrameRate() != it) view.setMaxFrameRate(it) }
        isSync?.let { if (view.isSync != it) view.isSync = it }
        videoEnabled?.let { if (view.videoEnabled() != it) view.setVideoEnabled(it) }
        useDiskCache?.let { if (view.useDiskCache() != it) view.setUseDiskCache(it) }
    }

    val assetManager = LocalContext.current.assets

    state.Monitor(state.composition) { view ->
        state.updateComposition(assetManager, view)
    }

    state.Monitor(isPlaying) { view ->
        if (isPlaying) {
            if (!view.isPlaying) view.play()
        }
        else {
            if (view.isPlaying) view.pause()
        }
    }
}