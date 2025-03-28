package love.yinlin.ui.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.ui.component.screen.ActionScope

@Composable
fun SplitLayout(
    modifier: Modifier = Modifier,
    gap: Dp = 10.dp,
    left: @Composable BoxScope.() -> Unit = {},
    right: @Composable BoxScope.() -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
            content = left
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
            content = right
        )
    }
}

@Composable
fun SplitActionLayout(
    modifier: Modifier = Modifier,
    space: Dp = 10.dp,
    gap: Dp = 10.dp,
    left: @Composable ActionScope.() -> Unit = {},
    right: @Composable ActionScope.() -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(space, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            content = { ActionScope.Left.actions(block = left) }
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(space, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = { ActionScope.Right.actions(block = right) }
        )
    }
}