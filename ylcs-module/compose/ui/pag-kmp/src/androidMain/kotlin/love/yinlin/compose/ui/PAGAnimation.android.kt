package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
actual fun PAGAnimation(state: PAGState, modifier: Modifier) {
    state.Content(modifier)

    LaunchedEffect(repeatCount, scaleMode) {
        pagView.value?.let { view ->
            view.setRepeatCount(repeatCount)
            view.setScaleMode(when (scaleMode) {
                PAGConfig.ScaleMode.None -> PAGScaleMode.None
                PAGConfig.ScaleMode.Stretch -> PAGScaleMode.Stretch
                PAGConfig.ScaleMode.LetterBox -> PAGScaleMode.LetterBox
                PAGConfig.ScaleMode.Zoom -> PAGScaleMode.Zoom
            })
        }
    }

    LaunchedEffect(listener) {
        if (listener == pagViewListener.value.listener) {
            return@LaunchedEffect
        }
        pagView.value?.let { view ->
            view.removeListener(pagViewListener.value)
            pagViewListener.value = PAGViewListenerImpl(listener)
            view.addListener(pagViewListener.value)
        }
    }
}