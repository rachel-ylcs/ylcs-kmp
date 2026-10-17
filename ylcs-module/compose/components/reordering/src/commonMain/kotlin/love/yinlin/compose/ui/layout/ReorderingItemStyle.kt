package love.yinlin.compose.ui.layout

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.staticLocalComposition

/**
 * 默认提供拖动态的状态层和抬升阴影。
 * 通常无需设置；可通过 CompositionLocalProvider 覆盖样式，返回 Modifier 可关闭默认效果。
 * 样式不应改变 item 的测量尺寸或位置，位移和回落动画由排序容器管理。
 */
val LocalReorderingItemStyle = staticLocalComposition<@Composable (isDragging: Boolean) -> Modifier> {
    { reorderingItemStyle(it) }
}

private val ReorderingExitEasing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)

@Suppress("ModifierFactoryExtensionFunction")
@Composable
fun reorderingItemStyle(isDragging: Boolean, shape: Shape = Theme.shape.v8): Modifier {
    val elevation = animateDpAsState(
        if (isDragging) Theme.shadow.v2 else Dp.Hairline,
        tween(if (isDragging) 120 else 150, easing = if (isDragging) FastOutSlowInEasing else ReorderingExitEasing),
    )
    val stateLayerAlpha = animateFloatAsState(if (isDragging) 0.16f else 0f, tween(if (isDragging) 45 else 150, easing = LinearEasing))
    val surfaceColor = Theme.color.surface
    val stateLayerColor = Theme.color.onSurface

    return Modifier.graphicsLayer {
        shadowElevation = elevation.value.toPx()
        this.shape = shape
    }.clip(shape).drawWithContent {
        val alpha = stateLayerAlpha.value
        if (alpha > 0f) drawRect(surfaceColor)
        drawContent()
        if (alpha > 0f) drawRect(stateLayerColor.copy(alpha = alpha))
    }
}
