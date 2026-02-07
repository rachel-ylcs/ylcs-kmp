package love.yinlin.compose.ui.image

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.floating.BalloonTip
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.node.pointerIcon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * @param painter 绘制
 * @param color 颜色
 * @param size 大小
 * @param tip 悬浮提示
 * @param enabled 启用点击
 * @param indication 点击指示器
 * @param onClick 点击事件
 */
@Composable
fun Icon(
    painter: Painter,
    color: Color = LocalColor.current,
    tip: String = "",
    enabled: Boolean = true,
    indication: Indication? = LocalIndication.current,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val minSize = Theme.size.icon
    val actualColor = if (enabled) color else Theme.color.disabledContent
    val colorFilter = remember(actualColor) {
        if (actualColor == Colors.Unspecified) null else ColorFilter.tint(actualColor)
    }
    val clickableModifier = if (onClick != null) Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = indication,
        enabled = enabled,
        onClick = onClick
    ) else Modifier

    BalloonTip(enabled = Theme.tool.enableBallonTip, text = tip) {
        Layout(
            modifier = Modifier.defaultMinSize(minSize, minSize)
                .then(modifier)
                .paint(painter = painter, colorFilter = colorFilter, contentScale = ContentScale.Fit)
                .then(clickableModifier)
                .pointerIcon(PointerIcon.Hand, enabled && onClick != null),
            measurePolicy = MeasurePolicies.Empty
        )
    }
}

/**
 * @param icon 图标
 * @param color 颜色
 * @param size 大小
 * @param tip 悬浮提示
 * @param enabled 启用点击
 * @param indication 点击指示器
 * @param onClick 点击事件
 */
@Composable
fun Icon(
    icon: ImageVector,
    color: Color = LocalColor.current,
    tip: String = "",
    enabled: Boolean = true,
    indication: Indication? = LocalIndication.current,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Icon(painter = rememberVectorPainter(icon), color = color, tip = tip, enabled = enabled, indication = indication, modifier = modifier, onClick = onClick)
}

/**
 * @param icon 图标资源
 * @param size 大小
 * @param tip 悬浮提示
 * @param enabled 启用点击
 * @param indication 点击指示器
 * @param onClick 点击事件
 */
@Composable
fun Icon(
    icon: DrawableResource,
    tip: String = "",
    enabled: Boolean = true,
    indication: Indication? = LocalIndication.current,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Icon(painter = painterResource(icon), color = Colors.Unspecified, tip = tip, enabled = enabled, indication = indication, modifier = modifier, onClick = onClick)
}