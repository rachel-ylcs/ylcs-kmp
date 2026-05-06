package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.browser.window
import kotlinx.coroutines.await
import love.yinlin.extension.then
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.round

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun PAGView(
    state: PAGState,
    modifier: Modifier,
    composition: PAGComposition?,
    isPlaying: Boolean,
    config: PAGConfig,
) {
    state.HostView(modifier = modifier.onSizeChanged { size ->
        state.host?.then {
            it.width = size.width
            it.height = size.height
            it.style.width = "${round(size.width / window.devicePixelRatio)}px"
            it.style.height = "${round(size.height / window.devicePixelRatio)}px"
        }
        state.pagView?.updateSize()
    })

    state.Monitor(config, state.pagView) {
        state.pagView?.then { pagView ->
            config.repeatCount.then { if (pagView.repeatCount != it) pagView.setRepeatCount(it) }
            config.scaleMode.ordinal.then { if (pagView.scaleMode() != it) pagView.setScaleMode(it) }
            config.cachedEnabled?.then { if (pagView.cacheEnabled() != it) pagView.setCacheEnabled(it) }
            config.cacheScale?.then { if (pagView.cacheScale() != it) pagView.setCacheScale(it) }
            config.maxFrameRate?.then { if (pagView.maxFrameRate() != it) pagView.setMaxFrameRate(it) }
        }
    }

    state.Monitor(composition) { view ->
        state.pagView?.then {
            it.removeListener("onAnimationStart", null)
            it.removeListener("onAnimationEnd", null)
            it.removeListener("onAnimationCancel", null)
            it.removeListener("onAnimationRepeat", null)
            it.removeListener("onAnimationUpdate", null)
            it.destroy()
        }
        if (composition == null) state.pagView = null
        else {
            state.pagView = PAG.pagInstance?.PAGView?.init(
                composition = composition.delegate,
                canvas = view,
                initOptions = defaultPAGViewOptions.apply { useCanvas2D = config.useCanvas2D }
            )?.await()?.also { pagView ->
                pagView.addListener("onAnimationStart") { state.onAnimationStart() }
                pagView.addListener("onAnimationEnd") { state.onAnimationEnd() }
                pagView.addListener("onAnimationCancel") { state.onAnimationCancel() }
                pagView.addListener("onAnimationRepeat") { state.onAnimationRepeat() }
                pagView.addListener("onAnimationUpdate") { state.stateProgress = pagView.getProgress() }
            }
        }
    }

    state.Monitor(state.flushFlag) {
        state.pagView?.flush()
    }

    state.Monitor(isPlaying, state.pagView) {
        state.pagView?.then { pagView ->
            if (isPlaying) {
                if (!pagView.isPlaying) pagView.play()
            }
            else {
                if (pagView.isPlaying) pagView.pause()
            }
        }
    }
}