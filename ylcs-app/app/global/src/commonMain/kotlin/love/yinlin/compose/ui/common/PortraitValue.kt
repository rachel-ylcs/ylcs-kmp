package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SimpleClipText

@Composable
fun PortraitValue(
    value: String,
    title: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.condition(onClick != null) { silentClick(onClick = onClick).pointerIcon(PointerIcon.Hand) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
    ) {
        SimpleClipText(text = value, style = Theme.typography.v6.bold)
        SimpleClipText(text = title)
    }
}