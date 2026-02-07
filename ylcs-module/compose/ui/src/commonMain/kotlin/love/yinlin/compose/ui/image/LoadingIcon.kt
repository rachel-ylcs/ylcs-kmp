package love.yinlin.compose.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.animation.IndeterminateLoadingAnimation
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * @param painter 绘制
 * @param color 颜色
 * @param size 大小
 * @param animation 动画
 * @param tip 悬浮提示
 * @param enabled 启用点击
 * @param onClick 点击事件
 */
@Composable
fun LoadingIcon(
    painter: Painter,
    color: Color = LocalColor.current,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    tip: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()

    if (isLoading) animation.Content(color = color)
    else {
        Icon(painter = painter, color = color, tip = tip, enabled = enabled, modifier = modifier, onClick = {
            if (!isLoading) {
                scope.launch {
                    isLoading = true
                    onClick()
                    isLoading = false
                }
            }
        })
    }
}

/**
 * @param icon 图标
 * @param color 颜色
 * @param size 大小
 * @param animation 动画
 * @param tip 悬浮提示
 * @param enabled 启用点击
 * @param onClick 点击事件
 */
@Composable
fun LoadingIcon(
    icon: ImageVector,
    color: Color = LocalColor.current,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    tip: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit,
) {
    LoadingIcon(painter = rememberVectorPainter(icon), color = color, animation = animation, tip = tip, enabled = enabled, modifier = modifier, onClick = onClick)
}

/**
 * @param icon 图标资源
 * @param size 大小
 * @param animation 动画
 * @param tip 悬浮提示
 * @param enabled 启用点击
 * @param onClick 点击事件
 */
@Composable
fun LoadingIcon(
    icon: DrawableResource,
    animation: IndeterminateLoadingAnimation = CircleLoading,
    tip: String = "",
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit,
) {
    LoadingIcon(painter = painterResource(icon), color = Colors.Unspecified, animation = animation, tip = tip, enabled = enabled, modifier = modifier, onClick = onClick)
}