package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * 原生 PAGImageView 控件封装, 会将渲染结果缓存到磁盘或内存中, 渲染完毕或缓存命中则销毁渲染上下文, 直接绘制缓存
 *
 * 适合列表等页面中含有多个 pag 动画同时渲染的场景
 *
 * @param source PAG输入源
 * @param modifier
 * @param isPlaying 是否播放
 * @param config PAG配置
 */
@Composable
expect fun PAGImageAnimation(
    source: PAGSource? = null,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    config: PAGConfig = remember { PAGConfig() }
)