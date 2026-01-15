package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 原生 PAGView 控件封装, 每个 PAGAnimation 都会创建一个 GPU 渲染上下文
 *
 * 适用于只渲染单个 pag 动画的场景
 *
 * @param state PAG状态
 * @param modifier
 */
@Composable
expect fun PAGAnimation(state: PAGState, modifier: Modifier = Modifier)