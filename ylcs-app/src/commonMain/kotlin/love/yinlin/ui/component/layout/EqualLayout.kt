package love.yinlin.ui.component.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

typealias EqualRowScope = RowScope

@Composable
fun EqualRowScope.EqualItem(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Center,
        content = content
    )
}

@Composable
fun EqualRow(
    modifier: Modifier = Modifier,
    content: @Composable EqualRowScope.() -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}


typealias EqualColumnScope = ColumnScope

@Composable
fun EqualColumnScope.EqualItem(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Center,
        content = content
    )
}

@Composable
fun EqualColumn(
    modifier: Modifier = Modifier,
    content: @Composable EqualColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}