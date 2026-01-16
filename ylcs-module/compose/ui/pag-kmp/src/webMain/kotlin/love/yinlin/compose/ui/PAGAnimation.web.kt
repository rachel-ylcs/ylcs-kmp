package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.round

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PAGAnimation(
    state: PAGState,
    modifier: Modifier,
    isPlaying: Boolean,
    config: PAGConfig,
) {
    state.HostView(modifier = modifier.onSizeChanged { size ->
        state.host?.let {
            it.width = size.width
            it.height = size.height
            it.style.width = "${round(size.width / window.devicePixelRatio)}px"
            it.style.height = "${round(size.height / window.devicePixelRatio)}px"
        }
        state.pagView?.updateSize()
    })

    state.Monitor(config, state.pagView) {
        state.pagView?.let { pagView ->
            config.repeatCount.let { if (pagView.repeatCount != it) pagView.setRepeatCount(it) }
            config.scaleMode.ordinal.let { if (pagView.scaleMode() != it) pagView.setScaleMode(it) }
            config.cachedEnabled?.let { if (pagView.cacheEnabled() != it) pagView.setCacheEnabled(it) }
            config.cacheScale?.let { if (pagView.cacheScale() != it) pagView.setCacheScale(it) }
            config.maxFrameRate?.let { if (pagView.maxFrameRate() != it) pagView.setMaxFrameRate(it) }
        }
    }

    state.Monitor(state.composition) { view ->
        state.updateComposition(config, view)
    }

    state.Monitor(isPlaying, state.pagView) {
        state.pagView?.let { pagView ->
            if (isPlaying) {
                if (!pagView.isPlaying) pagView.play()
            }
            else {
                if (pagView.isPlaying) pagView.pause()
            }
        }
    }
}