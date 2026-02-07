package love.yinlin.compose.ui.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.TextIconAdapter

/**
 * provider参数均为索引, 0 <= index < size
 * @param size Filter数量
 * @param onClick Int是索引, Boolean为true表示选中, false表示取消选中
 */
@Composable
fun Filter(
    size: Int,
    selectedProvider: (Int) -> Boolean,
    titleProvider: (Int) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((Int) -> ImageVector?)? = null,
    enabledProvider: ((Int) -> Boolean)? = null,
    padding: PaddingValues = Theme.padding.value,
    style: TextStyle = LocalStyle.current,
    shape: Shape = Theme.shape.v7,
    border: Dp = Theme.border.v7,
    activeIcon: ImageVector = Icons.Check,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Theme.padding.h),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Theme.padding.v),
    onClick: (Int, Boolean) -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        repeat(size) { index ->
            val selected = selectedProvider(index)
            val title = titleProvider(index)
            val icon = iconProvider?.invoke(index)
            val enabled = enabledProvider?.invoke(index) ?: true

            val backgroundColor by animateColorAsState(
                targetValue = if (selected) Theme.color.primaryContainer else Theme.color.backgroundVariant,
                animationSpec = tween(Theme.animation.duration.default)
            )

            val contentColor = when {
                !enabled -> Theme.color.disabledContent
                selected -> Theme.color.onContainer
                else -> Theme.color.onBackground
            }

            val contentIcon = if (selected) activeIcon else icon

            TextIconAdapter(modifier = Modifier
                .clip(shape)
                .semantics(Role.RadioButton)
                .background(if (enabled) backgroundColor else Theme.color.disabledContainer)
                .border(border, Theme.color.outline, shape)
                .clickable(enabled = enabled) { onClick(index, !selected) }
                .padding(padding)
                .animateContentSize(),
            ) { iconId, textId ->
                contentIcon?.let { Icon(icon = it, color = contentColor, modifier = Modifier.iconId()) }
                SimpleClipText(text = title, modifier = Modifier.textId(), color = contentColor, style = style)
            }
        }
    }
}