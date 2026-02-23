package love.yinlin.compose.ui.input

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.ThemeContainer
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
    activeIcon: ImageVector? = Icons.Check,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Theme.padding.h),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Theme.padding.v),
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
    onClick: (Int, Boolean) -> Unit,
) {
    LookaheadScope {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
            itemVerticalAlignment = Alignment.CenterVertically,
            maxItemsInEachRow = maxItemsInEachRow,
            maxLines = maxLines
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

                ThemeContainer(contentColor) {
                    TextIconAdapter(modifier = Modifier
                        .animateBounds(this@LookaheadScope)
                        .clip(shape)
                        .semantics(Role.RadioButton)
                        .background(if (enabled) backgroundColor else Theme.color.disabledContainer)
                        .border(border, Theme.color.outline, shape)
                        .clickable(enabled = enabled) { onClick(index, !selected) }
                        .padding(padding),
                    ) { iconId, textId ->
                        if (contentIcon != null) Icon(icon = contentIcon, modifier = Modifier.iconId())
                        SimpleClipText(text = title, modifier = Modifier.textId(), style = style)
                    }
                }
            }
        }
    }
}