package love.yinlin.compose.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleClipText

@Composable
fun GameCommonButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v5,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        contentPadding = Theme.padding.value,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = icon)
            SimpleClipText(text = text)
        }
    }
}