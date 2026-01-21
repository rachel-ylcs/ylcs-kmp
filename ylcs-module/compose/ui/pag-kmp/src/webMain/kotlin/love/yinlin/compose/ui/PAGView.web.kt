package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.browser.window
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.round

@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
@Composable
actual fun PAGView(
    state: PAGState,
    modifier: Modifier,
    composition: PAGComposition?,
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

    state.Monitor(composition) { view ->
        state.pagView?.let {
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