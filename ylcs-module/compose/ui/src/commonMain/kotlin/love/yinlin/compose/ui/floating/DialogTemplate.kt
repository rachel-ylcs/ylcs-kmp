package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
abstract class DialogTemplate<R : Any> : Dialog<R>() {
    /**
     * 显示标题和图标
     */
    protected open val showTitle: Boolean = true
    /**
     * 图标
     */
    protected open val icon: ImageVector? = null
    /**
     * 支持内容滚动
     */
    protected open val scrollable: Boolean = true
    /**
     * 底部功能组
     */
    protected open val actions: @Composable (RowScope.() -> Unit)? = null
    /**
     * 内容边距
     */
    protected open val contentPadding: PaddingValues @Composable get() = Theme.padding.value7
    /**
     * 内容对齐方式
     */
    protected open val contentAlignment: Alignment get() = Alignment.TopStart
    /**
     * 内容最小宽度
     */
    protected inline val minContentWidth: Dp @Composable get() = Theme.size.cell1
    /**
     * 内容最小高度
     */
    protected inline val minContentHeight: Dp @Composable get() = Theme.size.cell9

    @Composable
    protected fun LandDialogTemplate(title: String, block: @Composable () -> Unit) {
        LandDialog {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)
            ) {
                if (showTitle) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon?.let { Icon(icon = it, color = Theme.color.primary) }
                        SimpleEllipsisText(text = title, color = Theme.color.primary, style = Theme.typography.v6.bold)
                    }
                }

                Box(
                    modifier = Modifier.sizeIn(
                        minWidth = minContentWidth,
                        maxWidth = minContentWidth * 1.75f,
                        minHeight = minContentHeight,
                        maxHeight = minContentWidth * 2f
                    ).condition(scrollable) { verticalScroll(state = rememberScrollState()) },
                    contentAlignment = contentAlignment
                ) {
                    block()
                }

                actions?.let { ActionScope.Right.Container(modifier = Modifier.align(Alignment.End), content = it) }
            }
        }
    }
}