package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Theme

@Stable
sealed class ActionScope(@PublishedApi internal val ltr: Boolean) {
    @Stable
    data object Left : ActionScope(ltr = true)
    @Stable
    data object Right : ActionScope(ltr = false)

    @Composable
    inline fun Container(
        modifier: Modifier = Modifier,
        padding: Dp = Theme.padding.h,
        content: @Composable RowScope.() -> Unit
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(
                space = padding,
                alignment = if (ltr) Alignment.Start else Alignment.End
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }

    companion object {
        @Composable
        inline fun SplitContainer(
            modifier: Modifier = Modifier,
            padding: Dp = Theme.padding.h,
            left: @Composable RowScope.() -> Unit,
            right: @Composable RowScope.() -> Unit,
        ) {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Left.Container(padding = padding, content = left)
                Right.Container(padding = padding, content = right)
            }
        }
    }
}