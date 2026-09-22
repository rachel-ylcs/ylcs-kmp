package love.yinlin.compose.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Composable
fun BoxText(text: String, color: Color) {
    SimpleEllipsisText(
        text = text,
        style = Theme.typography.v7.bold,
        color = color,
        modifier = Modifier.wrapContentSize().border(Theme.border.v7, color = color).padding(Theme.padding.g9),
    )
}