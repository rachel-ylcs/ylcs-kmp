package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.icon.M3Icons
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.platform.UnsupportedPlatformText

@Composable
fun UnsupportedPlatformComponent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace, Alignment.CenterVertically)
    ) {
        MiniIcon(
            icon = M3Icons.NotificationImportant,
            size = CustomTheme.size.image
        )
        Text(text = UnsupportedPlatformText)
    }
}