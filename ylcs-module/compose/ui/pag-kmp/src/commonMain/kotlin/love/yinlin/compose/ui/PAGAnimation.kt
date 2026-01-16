package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 原生 PAGView 控件封装, 每个 PAGAnimation 都会创建一个 GPU 渲染上下文。
 * 适用于只渲染单个 pag 动画的场景。
 */
@Composable
expect fun PAGAnimation(
    state: PAGState,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    repeatCount: Int = PAGConfig.INFINITY,
    scaleMode: PAGConfig.ScaleMode = PAGConfig.ScaleMode.LetterBox,
    // 下面参数不确定初始值是什么, 后续看 native 源码再改, 请勿主动使用 null 以防后续不兼容
    cachedEnabled: Boolean? = null,
    cacheScale: Float? = null,
    maxFrameRate: Float? = null,
    isSync: Boolean? = null,
    videoEnabled: Boolean? = null,
    useDiskCache: Boolean? = null,
)