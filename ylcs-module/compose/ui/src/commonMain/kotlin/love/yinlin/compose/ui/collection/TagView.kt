package love.yinlin.compose.ui.collection

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.TextStyle
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter

/**
 * 标签组
 *
 * @param size 标签数
 * @param titleProvider 标题
 * @param iconProvider 图标
 * @param onClick 单击事件
 * @param onDelete 长按删除事件
 * @param containerColor 容器色
 * @param contentColor 内容色
 * @param style 文字样式
 * @param maxItemsInEachRow 最大行内标签数
 * @param maxLines 最大行
 */
@Composable
fun TagView(
    size: Int,
    titleProvider: (Int) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((Int) -> ImageVector?)? = null,
    readonly: Boolean = false,
    onClick: ((Int) -> Unit)? = null,
    onDelete: ((Int) -> Unit)? = null,
    containerColor: Color = Theme.color.backgroundVariant,
    contentColor: Color = Theme.color.onBackground,
    style: TextStyle = LocalStyle.current,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    maxLines: Int = Int.MAX_VALUE,
) {
    LookaheadScope {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
            maxItemsInEachRow = maxItemsInEachRow,
            maxLines = maxLines
        ) {
            // 确保任何时刻只能最多有一个元素处于删除状态
            var deletingIndex by rememberValueState(-1)
            repeat(size) { index ->
                val isDeleting = index == deletingIndex
                val backgroundColor = when {
                    isDeleting -> Theme.color.error
                    deletingIndex == -1 -> containerColor
                    else -> Theme.color.disabledContainer
                }
                val color = when {
                    isDeleting -> Theme.color.onError
                    deletingIndex == -1 -> contentColor
                    else -> Theme.color.disabledContent
                }
                val title = titleProvider(index)
                val icon = if (isDeleting) Icons.Delete else iconProvider?.invoke(index)

                ThemeContainer(color) {
                    TextIconAdapter(modifier = Modifier
                        .animateBounds(this@LookaheadScope)
                        .clip(Theme.shape.v5)
                        .background(backgroundColor)
                        .condition(!readonly) {
                            combinedClickable(
                                onLongClick = {
                                    if (onDelete != null) deletingIndex = if (isDeleting) -1 else index
                                },
                                onClick = {
                                    if (isDeleting) {
                                        deletingIndex = -1
                                        onDelete?.invoke(index)
                                    }
                                    else if (deletingIndex == -1) onClick?.invoke(index)
                                    else deletingIndex = -1
                                }
                            )
                        }.padding(Theme.padding.value)
                    ) { idIcon, idText ->
                        if (icon != null) Icon(icon = icon, modifier = Modifier.idIcon())
                        Text(text = title, style = style, modifier = Modifier.idText())
                    }
                }
            }
        }
    }
}