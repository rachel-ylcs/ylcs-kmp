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

// 简单动画
// data: 动画源
// repeatCount: 重复次数
// renderScale: 缩放比例
// scaleMode: 缩放模式
// cacheAllFramesInMemory: 启用内存缓存

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

// 帧动画
// 通过PAGState控制动画源data和当前进度progress(0 ~ 1)
// 动画初始状态不播放, 在协程中变化进度来控制播放

@Composable
expect fun PAGAnimation(
    state: PAGState,
    modifier: Modifier = Modifier,
)