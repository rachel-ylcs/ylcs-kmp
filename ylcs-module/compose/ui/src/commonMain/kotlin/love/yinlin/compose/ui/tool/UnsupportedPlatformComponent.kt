package love.yinlin.compose.ui.tool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text
import love.yinlin.platform.UnsupportedPlatformText

@Composable
fun UnsupportedPlatformComponent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v, Alignment.CenterVertically)
    ) {
        Icon(icon = Icons.NotificationImportant, modifier = Modifier.size(Theme.size.image9))
        Text(text = UnsupportedPlatformText)
    }
}