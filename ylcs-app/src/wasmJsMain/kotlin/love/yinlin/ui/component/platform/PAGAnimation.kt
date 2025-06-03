package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

@Composable
actual fun PAGImageAnimation(
    data: ByteArray,
    repeatCount: Int,
    renderScale: Float,
    scaleMode: PAGConfig.ScaleMode,
    cacheAllFramesInMemory: Boolean,
    modifier: Modifier,
) {

}

@Stable
actual class PAGState {
    actual var data: ByteArray = ByteArray(0)
    actual var progress: Double = 0.0
}

@Composable
actual fun PAGAnimation(
    state: PAGState,
    modifier: Modifier,
) {

}