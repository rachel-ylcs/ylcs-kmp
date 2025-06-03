package love.yinlin.ui.component.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

object PAGConfig {
    enum class ScaleMode {
        None, Stretch, LetterBox, Zoom
    }

    const val INFINITY = -1
}

@Composable
expect fun PAGImageAnimation(
    data: ByteArray,
    repeatCount: Int = PAGConfig.INFINITY,
    renderScale: Float = 1f,
    scaleMode: PAGConfig.ScaleMode = PAGConfig.ScaleMode.Zoom,
    cacheAllFramesInMemory: Boolean = true,
    modifier: Modifier = Modifier,
)

@Stable
expect class PAGState() {
    var data: ByteArray
    var progress: Double
}

@Composable
expect fun PAGAnimation(
    state: PAGState,
    modifier: Modifier = Modifier,
)