package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 原生 PAGImageView 控件封装, 会将渲染结果缓存到磁盘或内存中, 渲染完毕或缓存命中则销毁渲染上下文, 直接绘制缓存
 *
 * 适合列表等页面中含有多个 pag 动画同时渲染的场景
 *
 * @param state PAG状态
 * @param modifier
 * @param renderScale 缩放比例
 * @param cacheAllFramesInMemory 在内存中缓存所有帧 (会极大增加内存占用量, 仅在必要时开启)
 */
@Composable
expect fun PAGImageAnimation(
    state: PAGState,
    modifier: Modifier = Modifier,
    renderScale: Float = 1.0f,
    cacheAllFramesInMemory: Boolean = false,
)